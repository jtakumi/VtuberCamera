package com.example.vtubercamera.data.vrm

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.example.vtubercamera.data.vrm.math.Vector3

/**
 * Extracts 3D mesh data from VRM/glTF files
 */
class VRMMeshExtractor {
    
    companion object {
        // glTF component types
        private const val BYTE = 5120
        private const val UNSIGNED_BYTE = 5121
        private const val SHORT = 5122
        private const val UNSIGNED_SHORT = 5123
        private const val UNSIGNED_INT = 5125
        private const val FLOAT = 5126
        
        // glTF attribute types
        private const val SCALAR = 1
        private const val VEC2 = 2
        private const val VEC3 = 3
        private const val VEC4 = 4
        private const val MAT2 = 4
        private const val MAT3 = 9
        private const val MAT4 = 16
    }
    
    /**
     * Extract mesh data from glTF JSON and binary buffer
     */
    suspend fun extractMeshData(
        json: JsonObject,
        binaryData: ByteArray
    ): MeshData = withContext(Dispatchers.IO) {
        val meshes = json.getAsJsonArray("meshes")
        val accessors = json.getAsJsonArray("accessors")
        val bufferViews = json.getAsJsonArray("bufferViews")
        
        if (meshes == null || accessors == null || bufferViews == null) {
            return@withContext MeshData.empty()
        }
        
        val extractedMeshes = mutableListOf<Mesh>()
        var totalVertices = 0
        var totalTriangles = 0
        
        for (i in 0 until meshes.size()) {
            val meshJson = meshes[i].asJsonObject
            val mesh = extractSingleMesh(meshJson, accessors, bufferViews, binaryData)
            extractedMeshes.add(mesh)
            totalVertices += mesh.vertices.size
            totalTriangles += mesh.indices.size / 3
        }
        
        MeshData(
            meshes = extractedMeshes,
            totalVertices = totalVertices,
            totalTriangles = totalTriangles,
            boundingBox = calculateBoundingBox(extractedMeshes)
        )
    }
    
    /**
     * Extract a single mesh from glTF data
     */
    private suspend fun extractSingleMesh(
        meshJson: JsonObject,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): Mesh = withContext(Dispatchers.IO) {
        val name = meshJson.get("name")?.asString ?: "Unnamed Mesh"
        val primitives = meshJson.getAsJsonArray("primitives")
        
        val vertices = mutableListOf<Vertex>()
        val indices = mutableListOf<Int>()
        val materials = mutableListOf<String>()
        
        if (primitives != null) {
            for (i in 0 until primitives.size()) {
                val primitive = primitives[i].asJsonObject
                val primitiveData = extractPrimitive(primitive, accessors, bufferViews, binaryData)
                
                val baseIndex = vertices.size
                vertices.addAll(primitiveData.vertices)
                
                // Adjust indices to account for vertex offset
                primitiveData.indices.forEach { index ->
                    indices.add(baseIndex + index)
                }
                
                val materialIndex = primitive.get("material")?.asInt
                materials.add(materialIndex?.toString() ?: "default")
            }
        }
        
        Mesh(
            name = name,
            vertices = vertices,
            indices = indices,
            materials = materials,
            boundingBox = calculateVertexBoundingBox(vertices)
        )
    }
    
    /**
     * Extract primitive data (positions, normals, UVs, etc.)
     */
    private suspend fun extractPrimitive(
        primitive: JsonObject,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): PrimitiveData = withContext(Dispatchers.IO) {
        val attributes = primitive.getAsJsonObject("attributes")
        val indicesAccessorIndex = primitive.get("indices")?.asInt
        
        // Extract positions (required)
        val positionAccessorIndex = attributes.get("POSITION")?.asInt
        val positions = if (positionAccessorIndex != null) {
            extractVector3Array(positionAccessorIndex, accessors, bufferViews, binaryData)
        } else {
            emptyList()
        }
        
        // Extract normals (optional)
        val normalAccessorIndex = attributes.get("NORMAL")?.asInt
        val normals = if (normalAccessorIndex != null) {
            extractVector3Array(normalAccessorIndex, accessors, bufferViews, binaryData)
        } else {
            generateNormals(positions)
        }
        
        // Extract UVs (optional)
        val uvAccessorIndex = attributes.get("TEXCOORD_0")?.asInt
        val uvs = if (uvAccessorIndex != null) {
            extractVector2Array(uvAccessorIndex, accessors, bufferViews, binaryData)
        } else {
            positions.map { Pair(0f, 0f) }
        }
        
        // Extract vertex colors (optional)
        val colorAccessorIndex = attributes.get("COLOR_0")?.asInt
        val colors = if (colorAccessorIndex != null) {
            extractVector4Array(colorAccessorIndex, accessors, bufferViews, binaryData)
        } else {
            positions.map { floatArrayOf(1f, 1f, 1f, 1f) }
        }
        
        // Extract bone weights and joints for skinning (optional)
        val weightsAccessorIndex = attributes.get("WEIGHTS_0")?.asInt
        val jointsAccessorIndex = attributes.get("JOINTS_0")?.asInt
        
        val boneWeights = if (weightsAccessorIndex != null) {
            extractVector4Array(weightsAccessorIndex, accessors, bufferViews, binaryData)
        } else {
            positions.map { floatArrayOf(1f, 0f, 0f, 0f) }
        }
        
        val boneIndices = if (jointsAccessorIndex != null) {
            extractVector4IntArray(jointsAccessorIndex, accessors, bufferViews, binaryData)
        } else {
            positions.map { intArrayOf(0, 0, 0, 0) }
        }
        
        // Combine into vertices
        val vertices = mutableListOf<Vertex>()
        for (i in positions.indices) {
            vertices.add(Vertex(
                position = positions[i],
                normal = if (i < normals.size) normals[i] else Vector3.UP,
                uv = if (i < uvs.size) uvs[i] else Pair(0f, 0f),
                color = if (i < colors.size) colors[i] else floatArrayOf(1f, 1f, 1f, 1f),
                boneWeights = if (i < boneWeights.size) boneWeights[i] else floatArrayOf(1f, 0f, 0f, 0f),
                boneIndices = if (i < boneIndices.size) boneIndices[i] else intArrayOf(0, 0, 0, 0)
            ))
        }
        
        // Extract indices
        val indices = if (indicesAccessorIndex != null) {
            extractIntArray(indicesAccessorIndex, accessors, bufferViews, binaryData)
        } else {
            positions.indices.toList()
        }
        
        PrimitiveData(vertices, indices)
    }
    
    /**
     * Extract Vector3 array from accessor
     */
    private suspend fun extractVector3Array(
        accessorIndex: Int,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): List<Vector3> = withContext(Dispatchers.IO) {
        val accessor = accessors[accessorIndex].asJsonObject
        val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
        val bufferView = bufferViews[bufferViewIndex].asJsonObject
        
        val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
        val count = accessor.get("count")?.asInt ?: 0
        val componentType = accessor.get("componentType")?.asInt ?: FLOAT
        val type = accessor.get("type")?.asString
        
        if (type != "VEC3") return@withContext emptyList()
        
        val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * 3 * getComponentSize(componentType))
            .order(ByteOrder.LITTLE_ENDIAN)
        
        val vectors = mutableListOf<Vector3>()
        for (i in 0 until count) {
            val x = readFloat(buffer, componentType)
            val y = readFloat(buffer, componentType)
            val z = readFloat(buffer, componentType)
            vectors.add(Vector3(x, y, z))
        }
        
        vectors
    }
    
    /**
     * Extract Vector2 (UV) array from accessor
     */
    private suspend fun extractVector2Array(
        accessorIndex: Int,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): List<Pair<Float, Float>> = withContext(Dispatchers.IO) {
        val accessor = accessors[accessorIndex].asJsonObject
        val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
        val bufferView = bufferViews[bufferViewIndex].asJsonObject
        
        val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
        val count = accessor.get("count")?.asInt ?: 0
        val componentType = accessor.get("componentType")?.asInt ?: FLOAT
        val type = accessor.get("type")?.asString
        
        if (type != "VEC2") return@withContext emptyList()
        
        val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * 2 * getComponentSize(componentType))
            .order(ByteOrder.LITTLE_ENDIAN)
        
        val vectors = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until count) {
            val x = readFloat(buffer, componentType)
            val y = readFloat(buffer, componentType)
            vectors.add(Pair(x, y))
        }
        
        vectors
    }
    
    /**
     * Extract Vector4 array from accessor
     */
    private suspend fun extractVector4Array(
        accessorIndex: Int,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): List<FloatArray> = withContext(Dispatchers.IO) {
        val accessor = accessors[accessorIndex].asJsonObject
        val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
        val bufferView = bufferViews[bufferViewIndex].asJsonObject
        
        val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
        val count = accessor.get("count")?.asInt ?: 0
        val componentType = accessor.get("componentType")?.asInt ?: FLOAT
        val type = accessor.get("type")?.asString
        
        if (type != "VEC4") return@withContext emptyList()
        
        val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * 4 * getComponentSize(componentType))
            .order(ByteOrder.LITTLE_ENDIAN)
        
        val vectors = mutableListOf<FloatArray>()
        for (i in 0 until count) {
            val x = readFloat(buffer, componentType)
            val y = readFloat(buffer, componentType)
            val z = readFloat(buffer, componentType)
            val w = readFloat(buffer, componentType)
            vectors.add(floatArrayOf(x, y, z, w))
        }
        
        vectors
    }
    
    /**
     * Extract Vector4 integer array from accessor
     */
    private suspend fun extractVector4IntArray(
        accessorIndex: Int,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): List<IntArray> = withContext(Dispatchers.IO) {
        val accessor = accessors[accessorIndex].asJsonObject
        val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
        val bufferView = bufferViews[bufferViewIndex].asJsonObject
        
        val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
        val count = accessor.get("count")?.asInt ?: 0
        val componentType = accessor.get("componentType")?.asInt ?: UNSIGNED_SHORT
        val type = accessor.get("type")?.asString
        
        if (type != "VEC4") return@withContext emptyList()
        
        val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * 4 * getComponentSize(componentType))
            .order(ByteOrder.LITTLE_ENDIAN)
        
        val vectors = mutableListOf<IntArray>()
        for (i in 0 until count) {
            val x = readInt(buffer, componentType)
            val y = readInt(buffer, componentType)
            val z = readInt(buffer, componentType)
            val w = readInt(buffer, componentType)
            vectors.add(intArrayOf(x, y, z, w))
        }
        
        vectors
    }
    
    /**
     * Extract integer array from accessor (for indices)
     */
    private suspend fun extractIntArray(
        accessorIndex: Int,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): List<Int> = withContext(Dispatchers.IO) {
        val accessor = accessors[accessorIndex].asJsonObject
        val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
        val bufferView = bufferViews[bufferViewIndex].asJsonObject
        
        val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
        val count = accessor.get("count")?.asInt ?: 0
        val componentType = accessor.get("componentType")?.asInt ?: UNSIGNED_SHORT
        
        val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * getComponentSize(componentType))
            .order(ByteOrder.LITTLE_ENDIAN)
        
        val indices = mutableListOf<Int>()
        for (i in 0 until count) {
            indices.add(readInt(buffer, componentType))
        }
        
        indices
    }
    
    /**
     * Read float value from buffer based on component type
     */
    private fun readFloat(buffer: ByteBuffer, componentType: Int): Float {
        return when (componentType) {
            BYTE -> buffer.get().toFloat()
            UNSIGNED_BYTE -> (buffer.get().toInt() and 0xFF).toFloat()
            SHORT -> buffer.short.toFloat()
            UNSIGNED_SHORT -> (buffer.short.toInt() and 0xFFFF).toFloat()
            UNSIGNED_INT -> (buffer.int.toLong() and 0xFFFFFFFFL).toFloat()
            FLOAT -> buffer.float
            else -> 0f
        }
    }
    
    /**
     * Read integer value from buffer based on component type
     */
    private fun readInt(buffer: ByteBuffer, componentType: Int): Int {
        return when (componentType) {
            BYTE -> buffer.get().toInt()
            UNSIGNED_BYTE -> buffer.get().toInt() and 0xFF
            SHORT -> buffer.short.toInt()
            UNSIGNED_SHORT -> buffer.short.toInt() and 0xFFFF
            UNSIGNED_INT -> buffer.int
            FLOAT -> buffer.float.toInt()
            else -> 0
        }
    }
    
    /**
     * Get component size in bytes
     */
    private fun getComponentSize(componentType: Int): Int {
        return when (componentType) {
            BYTE, UNSIGNED_BYTE -> 1
            SHORT, UNSIGNED_SHORT -> 2
            UNSIGNED_INT, FLOAT -> 4
            else -> 4
        }
    }
    
    /**
     * Generate normals for vertices that don't have them
     */
    private fun generateNormals(positions: List<Vector3>): List<Vector3> {
        if (positions.size < 3) return positions.map { Vector3.UP }
        
        val normals = MutableList(positions.size) { Vector3.ZERO }
        
        // Calculate face normals and accumulate for vertices
        for (i in 0 until positions.size - 2 step 3) {
            val v0 = positions[i]
            val v1 = positions[i + 1]
            val v2 = positions[i + 2]
            
            val edge1 = v1 - v0
            val edge2 = v2 - v0
            val faceNormal = edge1.cross(edge2).normalized()
            
            normals[i] = normals[i] + faceNormal
            normals[i + 1] = normals[i + 1] + faceNormal
            normals[i + 2] = normals[i + 2] + faceNormal
        }
        
        return normals.map { it.normalized() }
    }
    
    /**
     * Calculate bounding box for a list of meshes
     */
    private fun calculateBoundingBox(meshes: List<Mesh>): VRMModel.BoundingBox? {
        if (meshes.isEmpty()) return null
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE
        
        meshes.forEach { mesh ->
            mesh.vertices.forEach { vertex ->
                val pos = vertex.position
                minX = minOf(minX, pos.x)
                minY = minOf(minY, pos.y)
                minZ = minOf(minZ, pos.z)
                maxX = maxOf(maxX, pos.x)
                maxY = maxOf(maxY, pos.y)
                maxZ = maxOf(maxZ, pos.z)
            }
        }
        
        return VRMModel.BoundingBox(
            min = Vector3(minX, minY, minZ),
            max = Vector3(maxX, maxY, maxZ)
        )
    }
    
    /**
     * Calculate bounding box for vertices
     */
    private fun calculateVertexBoundingBox(vertices: List<Vertex>): VRMModel.BoundingBox? {
        if (vertices.isEmpty()) return null
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE
        
        vertices.forEach { vertex ->
            val pos = vertex.position
            minX = minOf(minX, pos.x)
            minY = minOf(minY, pos.y)
            minZ = minOf(minZ, pos.z)
            maxX = maxOf(maxX, pos.x)
            maxY = maxOf(maxY, pos.y)
            maxZ = maxOf(maxZ, pos.z)
        }
        
        return VRMModel.BoundingBox(
            min = Vector3(minX, minY, minZ),
            max = Vector3(maxX, maxY, maxZ)
        )
    }
}

