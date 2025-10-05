package com.example.vtubercamera.data.vrm

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VRM file parser that handles VRM 1.0 format
 */
class VRMParser(private val context: Context) {

    private val gson = Gson()

    companion object {
        private const val VRM_MAGIC = "glTF"
        private const val VRM_BINARY_MAGIC = 0x46546C67 // "glTF" in little-endian
        private const val JSON_CHUNK_TYPE = 0x4E4F534A // "JSON" in little-endian
        private const val BIN_CHUNK_TYPE = 0x004E4942  // "BIN\0" in little-endian

        // Maximum file size: 100MB
        private const val MAX_FILE_SIZE = 100 * 1024 * 1024

        // Supported VRM versions
        private val SUPPORTED_VERSIONS = setOf("1.0", "0.0")
    }

    /**
     * Parse VRM file from URI
     */
    suspend fun parseVRM(uri: Uri): Result<VRMModel> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)

            parseVRMFromStream(inputStream)
        } catch (_: SecurityException) {
            Result.failure(VRMLoadingError.PermissionDenied)
        } catch (e: IOException) {
            Result.failure(VRMLoadingError.IOError(e.message ?: "Unknown IO error"))
        } catch (e: Exception) {
            Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown parsing error"))
        }
    }

    /**
     * Parse VRM file from input stream
     */
    suspend fun parseVRMFromStream(inputStream: InputStream): Result<VRMModel> =
        withContext(Dispatchers.IO) {
            try {
                val data = inputStream.readBytes()

                // Check file size
                if (data.size > MAX_FILE_SIZE) {
                    return@withContext Result.failure(VRMLoadingError.FileSizeExceeded)
                }

                // Parse the VRM data
                parseVRMData(data)
            } catch (_: OutOfMemoryError) {
                Result.failure(VRMLoadingError.InsufficientMemory)
            } catch (e: Exception) {
                Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown parsing error"))
            } finally {
                inputStream.close()
            }
        }

    /**
     * Parse VRM from byte array
     */
    private suspend fun parseVRMData(data: ByteArray): Result<VRMModel> =
        withContext(Dispatchers.IO) {
            try {
                val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

                // Check if it's a binary glTF (VRM)
                val magic = buffer.int
                if (magic != VRM_BINARY_MAGIC) {
                    return@withContext Result.failure(VRMLoadingError.InvalidFormat)
                }

                val version = buffer.int
                if (version != 2) {
                    return@withContext Result.failure(VRMLoadingError.UnsupportedVersion)
                }

                val totalLength = buffer.int
                if (totalLength != data.size) {
                    return@withContext Result.failure(VRMLoadingError.CorruptedData)
                }

                // Parse chunks
                val parseResult = parseChunks(buffer, data)
                parseResult
            } catch (e: Exception) {
                Result.failure(VRMLoadingError.ParseError(e.message ?: "Failed to parse VRM data"))
            }
        }

    /**
     * Parse glTF chunks (JSON and BIN)
     */
    private suspend fun parseChunks(buffer: ByteBuffer, fullData: ByteArray): Result<VRMModel> =
        withContext(Dispatchers.IO) {
            var jsonData: JsonObject?
            var binaryData: ByteArray? = null

            try {
                // Parse JSON chunk
                val jsonChunkLength = buffer.int
                val jsonChunkType = buffer.int

                if (jsonChunkType != JSON_CHUNK_TYPE) {
                    return@withContext Result.failure(VRMLoadingError.InvalidFormat)
                }

                val jsonBytes = ByteArray(jsonChunkLength)
                buffer.get(jsonBytes)
                val jsonString = String(jsonBytes, Charsets.UTF_8)
                jsonData = JsonParser.parseString(jsonString).asJsonObject

                // Parse binary chunk (if exists)
                if (buffer.remaining() >= 8) {
                    val binChunkLength = buffer.int
                    val binChunkType = buffer.int

                    if (binChunkType == BIN_CHUNK_TYPE && binChunkLength > 0) {
                        binaryData = ByteArray(binChunkLength)
                        buffer.get(binaryData)
                    }
                }

                // Parse the VRM model from JSON and binary data
                parseVRMFromJson(jsonData, binaryData ?: ByteArray(0), fullData)
            } catch (e: Exception) {
                Result.failure(VRMLoadingError.ParseError("Failed to parse chunks: ${e.message}"))
            }
        }

    /**
     * Parse VRM model from JSON data
     */
    private suspend fun parseVRMFromJson(
        json: JsonObject,
        binaryData: ByteArray,
        fullData: ByteArray
    ): Result<VRMModel> = withContext(Dispatchers.IO) {
        try {
            // Validate VRM format
            if (!json.has("asset")) {
                return@withContext Result.failure(VRMLoadingError.InvalidFormat)
            }

            val asset = json.getAsJsonObject("asset")
            asset.get("generator")?.asString ?: ""

            // Check for VRM extension
            val extensions = json.getAsJsonObject("extensions")
            val vrmExtension = extensions?.getAsJsonObject("VRM")
                ?: extensions?.getAsJsonObject("VRMC_vrm")

            if (vrmExtension == null) {
                return@withContext Result.failure(VRMLoadingError.InvalidFormat)
            }

            // Parse VRM metadata
            val metadata = parseVRMMetadata(vrmExtension)

            // Generate unique ID
            val id = generateModelId(metadata)

            // Parse mesh data
            val meshData = parseMeshData(json, binaryData)

            // Parse textures
            val textureData = parseTextureData(json, binaryData, fullData)

            // Parse expressions
            val expressions = parseExpressions(vrmExtension)

            // Parse poses/animations
            val poses = parsePoses(json)

            // Parse additional model info
            val modelInfo = parseModelInfo(json, binaryData)

            val vrmModel = VRMModel(
                id = id,
                name = metadata.title.ifEmpty { "Unnamed VRM" },
                meshData = meshData,
                textureData = textureData,
                expressions = expressions,
                poses = poses,
                metadata = metadata,
                version = asset.get("version")?.asString ?: "1.0",
                boneNames = modelInfo.boneNames,
                materialNames = modelInfo.materialNames,
                animationClips = modelInfo.animationClips,
                boundingBox = modelInfo.boundingBox,
                polyCount = modelInfo.polyCount,
                textureResolution = modelInfo.textureResolution
            )

            Result.success(vrmModel)
        } catch (e: Exception) {
            Result.failure(VRMLoadingError.ParseError("Failed to parse VRM JSON: ${e.message}"))
        }
    }

    /**
     * Parse VRM metadata from extension
     */
    private fun parseVRMMetadata(vrmExtension: JsonObject): VRMMetadata {
        val meta = vrmExtension.getAsJsonObject("meta") ?: JsonObject()

        return VRMMetadata(
            title = meta.get("title")?.asString ?: "",
            version = meta.get("version")?.asString ?: "",
            author = meta.get("author")?.asString ?: "",
            contactInformation = meta.get("contactInformation")?.asString ?: "",
            reference = meta.get("reference")?.asString ?: "",
            texture = meta.get("texture")?.asString ?: "",
            allowedUserName = parseAllowedUser(meta.get("allowedUserName")?.asString),
            violentUsage = parseUsage(meta.get("violentUssageName")?.asString), // Note: typo in VRM spec
            sexualUsage = parseUsage(meta.get("sexualUssageName")?.asString), // Note: typo in VRM spec
            commercialUsage = parseUsage(meta.get("commercialUssageName")?.asString), // Note: typo in VRM spec
            otherPermissionUrl = meta.get("otherPermissionUrl")?.asString ?: "",
            licenseName = parseLicenseType(meta.get("licenseName")?.asString),
            otherLicenseUrl = meta.get("otherLicenseUrl")?.asString ?: ""
        )
    }

    /**
     * Parse allowed user type
     */
    private fun parseAllowedUser(value: String?): VRMMetadata.AllowedUser {
        return when (value?.lowercase()) {
            "onlyauthor" -> VRMMetadata.AllowedUser.ONLY_AUTHOR
            "explicitlylicensedperson" -> VRMMetadata.AllowedUser.EXPLICITLY_LICENSED_PERSON
            "everyone" -> VRMMetadata.AllowedUser.EVERYONE
            else -> VRMMetadata.AllowedUser.ONLY_AUTHOR
        }
    }

    /**
     * Parse usage permission
     */
    private fun parseUsage(value: String?): VRMMetadata.Usage {
        return when (value?.lowercase()) {
            "allow" -> VRMMetadata.Usage.ALLOW
            "disallow" -> VRMMetadata.Usage.DISALLOW
            else -> VRMMetadata.Usage.DISALLOW
        }
    }

    /**
     * Parse license type
     */
    private fun parseLicenseType(value: String?): VRMMetadata.LicenseType {
        return when (value?.lowercase()) {
            "redistribution_prohibited" -> VRMMetadata.LicenseType.REDISTRIBUTION_PROHIBITED
            "cc0" -> VRMMetadata.LicenseType.CC0
            "cc_by" -> VRMMetadata.LicenseType.CC_BY
            "cc_by_nc" -> VRMMetadata.LicenseType.CC_BY_NC
            "cc_by_sa" -> VRMMetadata.LicenseType.CC_BY_SA
            "cc_by_nc_sa" -> VRMMetadata.LicenseType.CC_BY_NC_SA
            "cc_by_nd" -> VRMMetadata.LicenseType.CC_BY_ND
            "cc_by_nc_nd" -> VRMMetadata.LicenseType.CC_BY_NC_ND
            "other" -> VRMMetadata.LicenseType.OTHER
            else -> VRMMetadata.LicenseType.REDISTRIBUTION_PROHIBITED
        }
    }

    /**
     * Generate unique model ID
     */
    private fun generateModelId(metadata: VRMMetadata): String {
        val baseString = "${metadata.title}_${metadata.author}_${System.currentTimeMillis()}"
        return baseString.hashCode().toString()
    }

    /**
     * Parse mesh data from glTF
     */
    private suspend fun parseMeshData(json: JsonObject, binaryData: ByteArray): ByteArray =
        withContext(Dispatchers.IO) {
            // For now, return the binary data as-is
            // In a full implementation, this would extract and process the mesh geometry
            binaryData
        }

    /**
     * Parse texture data from glTF
     */
    private suspend fun parseTextureData(
        json: JsonObject,
        binaryData: ByteArray,
        fullData: ByteArray
    ): Map<String, ByteArray> = withContext(Dispatchers.IO) {
        val textureMap = mutableMapOf<String, ByteArray>()

        try {
            val images = json.getAsJsonArray("images")
            val bufferViews = json.getAsJsonArray("bufferViews")

            if (images != null && bufferViews != null) {
                for (i in 0 until images.size()) {
                    val image = images[i].asJsonObject
                    val bufferViewIndex = image.get("bufferView")?.asInt

                    if (bufferViewIndex != null && bufferViewIndex < bufferViews.size()) {
                        val bufferView = bufferViews[bufferViewIndex].asJsonObject
                        val byteOffset = bufferView.get("byteOffset")?.asInt ?: 0
                        val byteLength = bufferView.get("byteLength")?.asInt ?: 0

                        if (byteLength > 0 && byteOffset + byteLength <= binaryData.size) {
                            val textureData =
                                binaryData.copyOfRange(byteOffset, byteOffset + byteLength)
                            val textureName = image.get("name")?.asString ?: "texture_$i"
                            textureMap[textureName] = textureData
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Log error but don't fail the entire parsing process
        }

        textureMap
    }

    /**
     * Parse expressions from VRM extension
     */
    private suspend fun parseExpressions(vrmExtension: JsonObject): List<Expression> =
        withContext(Dispatchers.IO) {
            val expressions = mutableListOf<Expression>()

            try {
                val expressionExtension = vrmExtension.getAsJsonObject("expressions")
                    ?: vrmExtension.getAsJsonObject("blendShape")

                if (expressionExtension != null) {
                    val presets = expressionExtension.getAsJsonArray("preset")
                    val customs = expressionExtension.getAsJsonArray("custom")

                    // Parse preset expressions
                    presets?.forEach { presetElement ->
                        val preset = presetElement.asJsonObject
                        expressions.add(parseExpression(preset, true))
                    }

                    // Parse custom expressions
                    customs?.forEach { customElement ->
                        val custom = customElement.asJsonObject
                        expressions.add(parseExpression(custom, false))
                    }
                }
            } catch (_: Exception) {
                // Log error but continue with empty expressions
            }

            expressions
        }

    /**
     * Parse single expression
     */
    private fun parseExpression(expressionJson: JsonObject, isPreset: Boolean): Expression {
        val name = expressionJson.get("name")?.asString ?: "unknown"
        val displayName = expressionJson.get("displayName")?.asString ?: name
        val isBinary = expressionJson.get("isBinary")?.asBoolean ?: false

        // Parse blend shape bindings
        val blendShapeKeys = mutableMapOf<String, Float>()
        val binds = expressionJson.getAsJsonArray("binds")

        binds?.forEach { bindElement ->
            val bind = bindElement.asJsonObject
            val meshIndex = bind.get("mesh")?.asInt
            val index = bind.get("index")?.asInt
            val weight = bind.get("weight")?.asFloat ?: 0f

            if (meshIndex != null && index != null) {
                blendShapeKeys["mesh_${meshIndex}_shape_${index}"] = weight
            }
        }

        // Parse override settings
        val overrideBlink = parseOverrideType(expressionJson.get("overrideBlink")?.asString)
        val overrideLookAt = parseOverrideType(expressionJson.get("overrideLookAt")?.asString)
        val overrideMouth = parseOverrideType(expressionJson.get("overrideMouth")?.asString)

        return Expression(
            name = name,
            displayName = displayName,
            blendShapeKeys = blendShapeKeys,
            isBinary = isBinary,
            overrideBlink = overrideBlink,
            overrideLookAt = overrideLookAt,
            overrideMouth = overrideMouth
        )
    }

    /**
     * Parse override type
     */
    private fun parseOverrideType(value: String?): Expression.OverrideType {
        return when (value?.lowercase()) {
            "block" -> Expression.OverrideType.BLOCK
            "blend" -> Expression.OverrideType.BLEND
            else -> Expression.OverrideType.NONE
        }
    }

    /**
     * Parse poses/animations from glTF
     */
    private suspend fun parsePoses(json: JsonObject): List<Pose> = withContext(Dispatchers.IO) {
        val poses = mutableListOf<Pose>()

        try {
            val animations = json.getAsJsonArray("animations")

            animations?.forEach { animationElement ->
                val animation = animationElement.asJsonObject
                val name = animation.get("name")?.asString ?: "pose_${poses.size}"

                // For now, create a basic pose structure
                // In a full implementation, this would parse the animation channels and samplers
                poses.add(
                    Pose(
                        name = name,
                        displayName = name.replace("_", " ").replaceFirstChar { it.uppercase() },
                        boneTransforms = emptyMap(),
                        category = Pose.PoseCategory.GENERAL
                    )
                )
            }
        } catch (_: Exception) {
            // Log error but continue with empty poses
        }

        poses
    }

    /**
     * Parse additional model information
     */
    private suspend fun parseModelInfo(json: JsonObject, binaryData: ByteArray): ModelInfo =
        withContext(Dispatchers.IO) {
            val boneNames = mutableListOf<String>()
            val materialNames = mutableListOf<String>()
            val animationClips = mutableListOf<VRMModel.AnimationClip>()
            var polyCount = 0

            try {
                // Parse nodes for bone names
                val nodes = json.getAsJsonArray("nodes")
                nodes?.forEach { nodeElement ->
                    val node = nodeElement.asJsonObject
                    val name = node.get("name")?.asString
                    if (name != null) {
                        boneNames.add(name)
                    }
                }

                // Parse materials
                val materials = json.getAsJsonArray("materials")
                materials?.forEach { materialElement ->
                    val material = materialElement.asJsonObject
                    val name = material.get("name")?.asString ?: "material_${materialNames.size}"
                    materialNames.add(name)
                }

                // Parse meshes for poly count
                val meshes = json.getAsJsonArray("meshes")
                meshes?.forEach { meshElement ->
                    val mesh = meshElement.asJsonObject
                    val primitives = mesh.getAsJsonArray("primitives")
                    primitives?.forEach { primitiveElement ->
                        val primitive = primitiveElement.asJsonObject
                        val indices = primitive.get("indices")?.asInt
                        if (indices != null) {
                            // Rough estimation - in a full implementation, this would read the actual index data
                            polyCount += 1000 // Placeholder
                        }
                    }
                }

                // Parse animations for clips
                val animations = json.getAsJsonArray("animations")
                animations?.forEach { animationElement ->
                    val animation = animationElement.asJsonObject
                    val name = animation.get("name")?.asString ?: "animation_${animationClips.size}"

                    animationClips.add(
                        VRMModel.AnimationClip(
                            name = name,
                            duration = 1.0f, // Placeholder - would calculate from samplers
                            isLooping = false,
                            frameRate = 30f
                        )
                    )
                }
            } catch (_: Exception) {
                // Log error but continue with default values
            }

            ModelInfo(
                boneNames = boneNames,
                materialNames = materialNames,
                animationClips = animationClips,
                boundingBox = null, // Would calculate from mesh data
                polyCount = polyCount,
                textureResolution = null // Would calculate from texture data
            )
        }

    /**
     * Data class for model information
     */
    private data class ModelInfo(
        val boneNames: List<String>,
        val materialNames: List<String>,
        val animationClips: List<VRMModel.AnimationClip>,
        val boundingBox: VRMModel.BoundingBox?,
        val polyCount: Int,
        val textureResolution: VRMModel.TextureResolution?
    )
}