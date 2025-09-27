package com.example.vtubercamera.data.vrm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Base64

/**
 * Extracts texture data from VRM/glTF files
 */
class VRMTextureExtractor {
    
    companion object {
        private const val MAX_TEXTURE_SIZE = 4096
        private const val MAX_TEXTURE_MEMORY = 256 * 1024 * 1024 // 256MB
    }
    
    /**
     * Extract all textures from glTF JSON and binary data
     */
    suspend fun extractTextures(
        json: JsonObject,
        binaryData: ByteArray,
        fullFileData: ByteArray
    ): TextureData = withContext(Dispatchers.IO) {
        val images = json.getAsJsonArray("images")
        val textures = json.getAsJsonArray("textures")
        val materials = json.getAsJsonArray("materials")
        val bufferViews = json.getAsJsonArray("bufferViews")
        
        val extractedTextures = mutableMapOf<String, Texture>()
        val materialTextures = mutableMapOf<String, MaterialTextureMapping>()
        var totalMemoryUsage = 0L
        
        // Extract image data
        if (images != null) {
            for (i in 0 until images.size()) {
                val imageJson = images[i].asJsonObject
                val texture = extractImage(imageJson, bufferViews, binaryData, i)
                
                if (texture != null) {
                    extractedTextures[texture.name] = texture
                    totalMemoryUsage += texture.memoryUsage
                    
                    // Stop if we exceed memory limit
                    if (totalMemoryUsage > MAX_TEXTURE_MEMORY) {
                        break
                    }
                }
            }
        }
        
        // Map textures to materials
        if (materials != null && textures != null) {
            for (i in 0 until materials.size()) {
                val materialJson = materials[i].asJsonObject
                val materialName = materialJson.get("name")?.asString ?: "material_$i"
                val mapping = extractMaterialTextureMapping(materialJson, textures, extractedTextures)
                materialTextures[materialName] = mapping
            }
        }
        
        TextureData(
            textures = extractedTextures,
            materialMappings = materialTextures,
            totalMemoryUsage = totalMemoryUsage
        )
    }
    
    /**
     * Extract a single image/texture
     */
    private suspend fun extractImage(
        imageJson: JsonObject,
        bufferViews: JsonArray?,
        binaryData: ByteArray,
        index: Int
    ): Texture? = withContext(Dispatchers.IO) {
        try {
            val name = imageJson.get("name")?.asString ?: "texture_$index"
            val mimeType = imageJson.get("mimeType")?.asString ?: "image/png"
            
            val imageData = when {
                // Image is embedded as base64 URI
                imageJson.has("uri") -> {
                    val uri = imageJson.get("uri").asString
                    if (uri.startsWith("data:")) {
                        extractDataUri(uri)
                    } else {
                        null // External file reference - not supported in this implementation
                    }
                }
                
                // Image is in binary buffer
                imageJson.has("bufferView") && bufferViews != null -> {
                    val bufferViewIndex = imageJson.get("bufferView").asInt
                    if (bufferViewIndex < bufferViews.size()) {
                        extractFromBufferView(bufferViews[bufferViewIndex].asJsonObject, binaryData)
                    } else {
                        null
                    }
                }
                
                else -> null
            }
            
            if (imageData != null) {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                if (bitmap != null) {
                    // Resize if too large
                    val resizedBitmap = if (bitmap.width > MAX_TEXTURE_SIZE || bitmap.height > MAX_TEXTURE_SIZE) {
                        resizeBitmap(bitmap, MAX_TEXTURE_SIZE)
                    } else {
                        bitmap
                    }
                    
                    val textureInfo = analyzeTexture(resizedBitmap, name, mimeType)
                    
                    Texture(
                        name = name,
                        data = imageData,
                        bitmap = resizedBitmap,
                        width = resizedBitmap.width,
                        height = resizedBitmap.height,
                        format = getTextureFormat(mimeType),
                        type = textureInfo.type,
                        hasAlpha = textureInfo.hasAlpha,
                        memoryUsage = calculateMemoryUsage(resizedBitmap),
                        mipLevels = calculateMipLevels(resizedBitmap.width, resizedBitmap.height)
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null // Skip corrupted textures
        }
    }
    
    /**
     * Extract image data from data URI
     */
    private fun extractDataUri(uri: String): ByteArray? {
        return try {
            val base64Data = uri.substringAfter("base64,")
            Base64.decode(base64Data, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract image data from buffer view
     */
    private fun extractFromBufferView(bufferView: JsonObject, binaryData: ByteArray): ByteArray? {
        return try {
            val byteOffset = bufferView.get("byteOffset")?.asInt ?: 0
            val byteLength = bufferView.get("byteLength")?.asInt ?: return null
            
            if (byteOffset + byteLength <= binaryData.size) {
                binaryData.copyOfRange(byteOffset, byteOffset + byteLength)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract material texture mapping
     */
    private fun extractMaterialTextureMapping(
        materialJson: JsonObject,
        textures: JsonArray,
        extractedTextures: Map<String, Texture>
    ): MaterialTextureMapping {
        val pbrMetallicRoughness = materialJson.getAsJsonObject("pbrMetallicRoughness")
        val normalTexture = materialJson.getAsJsonObject("normalTexture")
        val occlusionTexture = materialJson.getAsJsonObject("occlusionTexture")
        val emissiveTexture = materialJson.getAsJsonObject("emissiveTexture")
        
        return MaterialTextureMapping(
            baseColorTexture = getTextureReference(pbrMetallicRoughness?.getAsJsonObject("baseColorTexture"), textures, extractedTextures),
            metallicRoughnessTexture = getTextureReference(pbrMetallicRoughness?.getAsJsonObject("metallicRoughnessTexture"), textures, extractedTextures),
            normalTexture = getTextureReference(normalTexture, textures, extractedTextures),
            occlusionTexture = getTextureReference(occlusionTexture, textures, extractedTextures),
            emissiveTexture = getTextureReference(emissiveTexture, textures, extractedTextures),
            baseColorFactor = pbrMetallicRoughness?.getAsJsonArray("baseColorFactor")?.let { 
                floatArrayOf(
                    it[0].asFloat,
                    it[1].asFloat,
                    it[2].asFloat,
                    it[3].asFloat
                )
            } ?: floatArrayOf(1f, 1f, 1f, 1f),
            metallicFactor = pbrMetallicRoughness?.get("metallicFactor")?.asFloat ?: 1f,
            roughnessFactor = pbrMetallicRoughness?.get("roughnessFactor")?.asFloat ?: 1f,
            emissiveFactor = materialJson.getAsJsonArray("emissiveFactor")?.let {
                floatArrayOf(it[0].asFloat, it[1].asFloat, it[2].asFloat)
            } ?: floatArrayOf(0f, 0f, 0f),
            alphaCutoff = materialJson.get("alphaCutoff")?.asFloat ?: 0.5f,
            alphaMode = parseAlphaMode(materialJson.get("alphaMode")?.asString),
            doubleSided = materialJson.get("doubleSided")?.asBoolean ?: false
        )
    }
    
    /**
     * Get texture reference from texture info object
     */
    private fun getTextureReference(
        textureInfo: JsonObject?,
        textures: JsonArray,
        extractedTextures: Map<String, Texture>
    ): String? {
        if (textureInfo == null) return null
        
        val textureIndex = textureInfo.get("index")?.asInt ?: return null
        if (textureIndex >= textures.size()) return null
        
        val textureJson = textures[textureIndex].asJsonObject
        val imageIndex = textureJson.get("source")?.asInt ?: return null
        val textureName = "texture_$imageIndex"
        
        return if (extractedTextures.containsKey(textureName)) textureName else null
    }
    
    /**
     * Parse alpha mode from string
     */
    private fun parseAlphaMode(alphaMode: String?): AlphaMode {
        return when (alphaMode?.uppercase()) {
            "OPAQUE" -> AlphaMode.OPAQUE
            "MASK" -> AlphaMode.MASK
            "BLEND" -> AlphaMode.BLEND
            else -> AlphaMode.OPAQUE
        }
    }
    
    /**
     * Analyze texture properties
     */
    private fun analyzeTexture(bitmap: Bitmap, name: String, mimeType: String): TextureAnalysis {
        val hasAlpha = bitmap.hasAlpha()
        
        val type = when {
            name.contains("normal", ignoreCase = true) -> TextureType.NORMAL
            name.contains("roughness", ignoreCase = true) -> TextureType.ROUGHNESS
            name.contains("metallic", ignoreCase = true) -> TextureType.METALLIC
            name.contains("emission", ignoreCase = true) || name.contains("emissive", ignoreCase = true) -> TextureType.EMISSIVE
            name.contains("occlusion", ignoreCase = true) || name.contains("ao", ignoreCase = true) -> TextureType.OCCLUSION
            hasAlpha -> TextureType.ALBEDO_ALPHA
            else -> TextureType.ALBEDO
        }
        
        return TextureAnalysis(type, hasAlpha)
    }
    
    /**
     * Get texture format from MIME type
     */
    private fun getTextureFormat(mimeType: String): TextureFormat {
        return when (mimeType.lowercase()) {
            "image/png" -> TextureFormat.PNG
            "image/jpeg", "image/jpg" -> TextureFormat.JPEG
            "image/webp" -> TextureFormat.PNG // WEBP not supported yet, fallback to PNG
            "image/ktx2" -> TextureFormat.PNG // KTX2 not supported yet, fallback to PNG
            "image/basis" -> TextureFormat.PNG // BASIS not supported yet, fallback to PNG
            else -> TextureFormat.PNG
        }
    }
    
    /**
     * Resize bitmap to fit within max dimensions
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / aspectRatio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * aspectRatio).toInt()
        }
        
        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (resized != bitmap) {
            bitmap.recycle()
        }
        
        return resized
    }
    
    /**
     * Calculate memory usage of bitmap
     */
    private fun calculateMemoryUsage(bitmap: Bitmap): Long {
        return (bitmap.width * bitmap.height * 4).toLong() // Assuming ARGB_8888
    }
    
    /**
     * Calculate number of mip levels
     */
    private fun calculateMipLevels(width: Int, height: Int): Int {
        val maxDimension = maxOf(width, height)
        var levels = 0
        var size = maxDimension
        
        while (size > 0) {
            levels++
            size /= 2
        }
        
        return levels
    }
}

/**
 * Contains all extracted texture data
 */
data class TextureData(
    val textures: Map<String, Texture>,
    val materialMappings: Map<String, MaterialTextureMapping>,
    val totalMemoryUsage: Long
) {
    companion object {
        fun empty() = TextureData(
            textures = emptyMap(),
            materialMappings = emptyMap(),
            totalMemoryUsage = 0L
        )
    }
    
    /**
     * Get texture by name
     */
    fun getTexture(name: String): Texture? = textures[name]
    
    /**
     * Get material mapping by name
     */
    fun getMaterialMapping(materialName: String): MaterialTextureMapping? = materialMappings[materialName]
    
    /**
     * Check if texture data is empty
     */
    fun isEmpty(): Boolean = textures.isEmpty()
    
    /**
     * Get formatted memory usage
     */
    fun getFormattedMemoryUsage(): String {
        return when {
            totalMemoryUsage < 1024 * 1024 -> "${totalMemoryUsage / 1024}KB"
            totalMemoryUsage < 1024 * 1024 * 1024 -> "${totalMemoryUsage / (1024 * 1024)}MB"
            else -> "${totalMemoryUsage / (1024 * 1024 * 1024)}GB"
        }
    }
}

/**
 * Represents a single texture
 */
data class Texture(
    val name: String,
    val data: ByteArray,
    val bitmap: Bitmap?,
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val type: TextureType,
    val hasAlpha: Boolean,
    val memoryUsage: Long,
    val mipLevels: Int
) {
    /**
     * Get aspect ratio
     */
    fun getAspectRatio(): Float = width.toFloat() / height.toFloat()
    
    /**
     * Check if texture is square
     */
    fun isSquare(): Boolean = width == height
    
    /**
     * Check if texture is power of two
     */
    fun isPowerOfTwo(): Boolean = isPowerOfTwo(width) && isPowerOfTwo(height)
    
    private fun isPowerOfTwo(n: Int): Boolean = n > 0 && (n and (n - 1)) == 0
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Texture

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (format != other.format) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + format.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

/**
 * Material texture mapping
 */
data class MaterialTextureMapping(
    val baseColorTexture: String? = null,
    val metallicRoughnessTexture: String? = null,
    val normalTexture: String? = null,
    val occlusionTexture: String? = null,
    val emissiveTexture: String? = null,
    val baseColorFactor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val metallicFactor: Float = 1f,
    val roughnessFactor: Float = 1f,
    val emissiveFactor: FloatArray = floatArrayOf(0f, 0f, 0f),
    val alphaCutoff: Float = 0.5f,
    val alphaMode: AlphaMode = AlphaMode.OPAQUE,
    val doubleSided: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MaterialTextureMapping

        if (baseColorTexture != other.baseColorTexture) return false
        if (metallicRoughnessTexture != other.metallicRoughnessTexture) return false
        if (normalTexture != other.normalTexture) return false
        if (occlusionTexture != other.occlusionTexture) return false
        if (emissiveTexture != other.emissiveTexture) return false
        if (!baseColorFactor.contentEquals(other.baseColorFactor)) return false
        if (metallicFactor != other.metallicFactor) return false
        if (roughnessFactor != other.roughnessFactor) return false
        if (!emissiveFactor.contentEquals(other.emissiveFactor)) return false
        if (alphaCutoff != other.alphaCutoff) return false
        if (alphaMode != other.alphaMode) return false
        if (doubleSided != other.doubleSided) return false

        return true
    }

    override fun hashCode(): Int {
        var result = baseColorTexture?.hashCode() ?: 0
        result = 31 * result + (metallicRoughnessTexture?.hashCode() ?: 0)
        result = 31 * result + (normalTexture?.hashCode() ?: 0)
        result = 31 * result + (occlusionTexture?.hashCode() ?: 0)
        result = 31 * result + (emissiveTexture?.hashCode() ?: 0)
        result = 31 * result + baseColorFactor.contentHashCode()
        result = 31 * result + metallicFactor.hashCode()
        result = 31 * result + roughnessFactor.hashCode()
        result = 31 * result + emissiveFactor.contentHashCode()
        result = 31 * result + alphaCutoff.hashCode()
        result = 31 * result + alphaMode.hashCode()
        result = 31 * result + doubleSided.hashCode()
        return result
    }
}

/**
 * Texture formats
 */
enum class TextureFormat {
    PNG,
    JPEG,
    UNKNOWN
}

/**
 * Texture types
 */
enum class TextureType {
    ALBEDO,
    ALBEDO_ALPHA,
    NORMAL,
    ROUGHNESS,
    METALLIC,
    METALLIC_ROUGHNESS,
    EMISSIVE,
    OCCLUSION,
    HEIGHT,
    SUBSURFACE,
    UNKNOWN
}

/**
 * Alpha modes for materials
 */
enum class AlphaMode {
    OPAQUE,
    MASK,
    BLEND
}

/**
 * Texture analysis result
 */
private data class TextureAnalysis(
    val type: TextureType,
    val hasAlpha: Boolean
)