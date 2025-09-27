package com.example.vtubercamera.data.vrm

import android.util.Log
import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.data.vrm.math.Transform
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts VRM model data to Filament-compatible format
 * Handles mesh data conversion, material setup, and texture processing
 */
@Singleton
class VRMFilamentConverter @Inject constructor() {
    
    companion object {
        private const val TAG = "VRMFilamentConverter"
        private const val BYTES_PER_FLOAT = 4
        private const val BYTES_PER_INT = 4
        private const val POSITION_COMPONENTS = 3
        private const val NORMAL_COMPONENTS = 3
        private const val UV_COMPONENTS = 2
        private const val COLOR_COMPONENTS = 4
        private const val BONE_WEIGHT_COMPONENTS = 4
        private const val BONE_INDEX_COMPONENTS = 4
    }
    
    /**
     * Convert VRM model to Filament-compatible mesh data
     */
    fun convertVRMToFilamentMesh(vrmModel: VRMModel): FilamentMeshData {
        Log.d(TAG, "Converting VRM model to Filament mesh: ${vrmModel.name}")
        
        try {
            // Extract mesh data from VRM
            val meshData = extractMeshData(vrmModel.meshData)
            
            // Convert to Filament format
            val filamentMeshes = meshData.meshes.map { mesh ->
                convertMeshToFilament(mesh)
            }
            
            // Process materials
            val materials = processMaterials(vrmModel)
            
            // Process textures
            val textures = processTextures(vrmModel.textureData)
            
            Log.d(TAG, "Successfully converted VRM to Filament format - ${filamentMeshes.size} meshes, ${materials.size} materials")
            
            return FilamentMeshData(
                meshes = filamentMeshes,
                materials = materials,
                textures = textures,
                boundingBox = meshData.boundingBox,
                totalVertices = meshData.totalVertices,
                totalTriangles = meshData.totalTriangles
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert VRM to Filament mesh", e)
            throw VRMLoadingError.ParseError("Failed to convert VRM to Filament format: ${e.message}")
        }
    }
    
    /**
     * Convert a single mesh to Filament format
     */
    private fun convertMeshToFilament(mesh: Mesh): FilamentMesh {
        Log.d(TAG, "Converting mesh: ${mesh.name} (${mesh.vertices.size} vertices)")
        
        // Create vertex buffer
        val vertexBuffer = createVertexBuffer(mesh.vertices)
        
        // Create index buffer
        val indexBuffer = createIndexBuffer(mesh.indices)
        
        // Calculate vertex attributes
        val attributes = calculateVertexAttributes(mesh.vertices)
        
        return FilamentMesh(
            name = mesh.name,
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            vertexCount = mesh.vertices.size,
            indexCount = mesh.indices.size,
            attributes = attributes,
            materials = mesh.materials,
            boundingBox = mesh.boundingBox
        )
    }
    
    /**
     * Create vertex buffer in Filament format
     */
    private fun createVertexBuffer(vertices: List<Vertex>): ByteBuffer {
        val vertexSize = calculateVertexSize()
        val bufferSize = vertices.size * vertexSize
        
        val buffer = ByteBuffer.allocateDirect(bufferSize)
            .order(ByteOrder.nativeOrder())
        
        vertices.forEach { vertex ->
            // Position (3 floats)
            buffer.putFloat(vertex.position.x)
            buffer.putFloat(vertex.position.y)
            buffer.putFloat(vertex.position.z)
            
            // Normal (3 floats)
            buffer.putFloat(vertex.normal.x)
            buffer.putFloat(vertex.normal.y)
            buffer.putFloat(vertex.normal.z)
            
            // UV coordinates (2 floats)
            buffer.putFloat(vertex.uv.first)
            buffer.putFloat(vertex.uv.second)
            
            // Color (4 floats)
            buffer.putFloat(vertex.color[0])
            buffer.putFloat(vertex.color[1])
            buffer.putFloat(vertex.color[2])
            buffer.putFloat(vertex.color[3])
            
            // Bone weights (4 floats)
            buffer.putFloat(vertex.boneWeights[0])
            buffer.putFloat(vertex.boneWeights[1])
            buffer.putFloat(vertex.boneWeights[2])
            buffer.putFloat(vertex.boneWeights[3])
            
            // Bone indices (4 ints)
            buffer.putInt(vertex.boneIndices[0])
            buffer.putInt(vertex.boneIndices[1])
            buffer.putInt(vertex.boneIndices[2])
            buffer.putInt(vertex.boneIndices[3])
        }
        
        buffer.rewind()
        return buffer
    }
    
    /**
     * Create index buffer
     */
    private fun createIndexBuffer(indices: List<Int>): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(indices.size * BYTES_PER_INT)
            .order(ByteOrder.nativeOrder())
        
        indices.forEach { index ->
            buffer.putInt(index)
        }
        
        buffer.rewind()
        return buffer
    }
    
    /**
     * Calculate vertex size in bytes
     */
    private fun calculateVertexSize(): Int {
        return (POSITION_COMPONENTS + NORMAL_COMPONENTS + UV_COMPONENTS + COLOR_COMPONENTS + BONE_WEIGHT_COMPONENTS) * BYTES_PER_FLOAT +
                BONE_INDEX_COMPONENTS * BYTES_PER_INT
    }
    
    /**
     * Calculate vertex attributes for Filament
     */
    private fun calculateVertexAttributes(vertices: List<Vertex>): VertexAttributes {
        var hasNormals = false
        var hasUVs = false
        var hasColors = false
        var hasBoneWeights = false
        
        vertices.forEach { vertex ->
            if (vertex.normal != Vector3.ZERO) hasNormals = true
            if (vertex.uv.first != 0f || vertex.uv.second != 0f) hasUVs = true
            if (vertex.color.any { it != 1f }) hasColors = true
            if (vertex.boneWeights.any { it > 0f }) hasBoneWeights = true
        }
        
        return VertexAttributes(
            hasPositions = true, // Always true
            hasNormals = hasNormals,
            hasUVs = hasUVs,
            hasColors = hasColors,
            hasBoneWeights = hasBoneWeights,
            hasBoneIndices = hasBoneWeights
        )
    }
    
    /**
     * Process materials from VRM model
     */
    private fun processMaterials(vrmModel: VRMModel): List<FilamentMaterial> {
        Log.d(TAG, "Processing ${vrmModel.materialNames.size} materials")
        
        return vrmModel.materialNames.mapIndexed { index, materialName ->
            FilamentMaterial(
                name = materialName,
                index = index,
                baseColorFactor = floatArrayOf(1f, 1f, 1f, 1f),
                metallicFactor = 0f,
                roughnessFactor = 1f,
                emissiveFactor = floatArrayOf(0f, 0f, 0f),
                baseColorTexture = findTextureForMaterial(materialName, "baseColor", vrmModel),
                normalTexture = findTextureForMaterial(materialName, "normal", vrmModel),
                metallicRoughnessTexture = findTextureForMaterial(materialName, "metallicRoughness", vrmModel),
                emissiveTexture = findTextureForMaterial(materialName, "emissive", vrmModel),
                occlusionTexture = findTextureForMaterial(materialName, "occlusion", vrmModel),
                doubleSided = true,
                alphaMode = AlphaMode.OPAQUE,
                alphaCutoff = 0.5f
            )
        }
    }
    
    /**
     * Find texture for a specific material and type
     */
    private fun findTextureForMaterial(materialName: String, textureType: String, vrmModel: VRMModel): String? {
        // Look for texture with naming convention: materialName_textureType
        val possibleNames = listOf(
            "${materialName}_${textureType}",
            "${materialName}_${textureType.lowercase()}",
            "${materialName}.${textureType}",
            materialName // Fallback to material name
        )
        
        return possibleNames.firstOrNull { textureName ->
            vrmModel.textureData.containsKey(textureName)
        }
    }
    
    /**
     * Process textures from VRM model
     */
    private fun processTextures(textureData: Map<String, ByteArray>): List<FilamentTexture> {
        Log.d(TAG, "Processing ${textureData.size} textures")
        
        return textureData.map { (name, data) ->
            FilamentTexture(
                name = name,
                data = data,
                format = detectTextureFormat(data),
                width = 0, // Will be determined when loading
                height = 0, // Will be determined when loading
                mipLevels = 1,
                sRGB = isColorTexture(name)
            )
        }
    }
    
    /**
     * Detect texture format from data
     */
    private fun detectTextureFormat(data: ByteArray): TextureFormat {
        return when {
            data.size >= 4 && data[0] == 0x89.toByte() && data[1] == 0x50.toByte() && 
            data[2] == 0x4E.toByte() && data[3] == 0x47.toByte() -> TextureFormat.PNG
            
            data.size >= 2 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() -> TextureFormat.JPEG
            
            else -> TextureFormat.UNKNOWN
        }
    }
    
    /**
     * Check if texture is a color texture (should use sRGB)
     */
    private fun isColorTexture(textureName: String): Boolean {
        val colorKeywords = listOf("basecolor", "diffuse", "albedo", "color", "emissive")
        val lowerName = textureName.lowercase()
        return colorKeywords.any { keyword -> lowerName.contains(keyword) }
    }
    
    /**
     * Extract mesh data from VRM binary data
     */
    private fun extractMeshData(meshData: ByteArray): MeshData {
        // This is a placeholder implementation
        // In a real implementation, this would parse the VRM/GLTF binary data
        Log.d(TAG, "Extracting mesh data from ${meshData.size} bytes")
        
        // For now, return empty mesh data
        // TODO: Implement actual VRM/GLTF parsing
        return MeshData.empty()
    }
}

/**
 * Filament-compatible mesh data
 */
data class FilamentMeshData(
    val meshes: List<FilamentMesh>,
    val materials: List<FilamentMaterial>,
    val textures: List<FilamentTexture>,
    val boundingBox: VRMModel.BoundingBox?,
    val totalVertices: Int,
    val totalTriangles: Int
) {
    fun isEmpty(): Boolean = meshes.isEmpty()
    
    fun getMesh(name: String): FilamentMesh? = meshes.find { it.name == name }
    
    fun getMaterial(name: String): FilamentMaterial? = materials.find { it.name == name }
    
    fun getTexture(name: String): FilamentTexture? = textures.find { it.name == name }
}

/**
 * Filament-compatible mesh
 */
data class FilamentMesh(
    val name: String,
    val vertexBuffer: ByteBuffer,
    val indexBuffer: ByteBuffer,
    val vertexCount: Int,
    val indexCount: Int,
    val attributes: VertexAttributes,
    val materials: List<String>,
    val boundingBox: VRMModel.BoundingBox?
) {
    fun getTriangleCount(): Int = indexCount / 3
}

/**
 * Vertex attributes information
 */
data class VertexAttributes(
    val hasPositions: Boolean,
    val hasNormals: Boolean,
    val hasUVs: Boolean,
    val hasColors: Boolean,
    val hasBoneWeights: Boolean,
    val hasBoneIndices: Boolean
) {
    fun getAttributeCount(): Int {
        var count = 0
        if (hasPositions) count++
        if (hasNormals) count++
        if (hasUVs) count++
        if (hasColors) count++
        if (hasBoneWeights) count++
        if (hasBoneIndices) count++
        return count
    }
}

/**
 * Filament material definition
 */
data class FilamentMaterial(
    val name: String,
    val index: Int,
    val baseColorFactor: FloatArray,
    val metallicFactor: Float,
    val roughnessFactor: Float,
    val emissiveFactor: FloatArray,
    val baseColorTexture: String?,
    val normalTexture: String?,
    val metallicRoughnessTexture: String?,
    val emissiveTexture: String?,
    val occlusionTexture: String?,
    val doubleSided: Boolean,
    val alphaMode: AlphaMode,
    val alphaCutoff: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilamentMaterial

        if (name != other.name) return false
        if (index != other.index) return false
        if (!baseColorFactor.contentEquals(other.baseColorFactor)) return false
        if (metallicFactor != other.metallicFactor) return false
        if (roughnessFactor != other.roughnessFactor) return false
        if (!emissiveFactor.contentEquals(other.emissiveFactor)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + index
        result = 31 * result + baseColorFactor.contentHashCode()
        result = 31 * result + metallicFactor.hashCode()
        result = 31 * result + roughnessFactor.hashCode()
        result = 31 * result + emissiveFactor.contentHashCode()
        return result
    }
}



/**
 * Filament texture definition
 */
data class FilamentTexture(
    val name: String,
    val data: ByteArray,
    val format: TextureFormat,
    val width: Int,
    val height: Int,
    val mipLevels: Int,
    val sRGB: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilamentTexture

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false
        if (format != other.format) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

