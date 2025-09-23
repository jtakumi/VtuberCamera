package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Vector3

/**
 * Represents extracted mesh data
 */
data class MeshData(
    val meshes: List<Mesh>,
    val totalVertices: Int,
    val totalTriangles: Int,
    val boundingBox: VRMModel.BoundingBox?
) {
    /**
     * Get mesh by name
     */
    fun getMesh(name: String): Mesh? = meshes.find { it.name == name }
    
    /**
     * Check if mesh data is empty
     */
    fun isEmpty(): Boolean = meshes.isEmpty()
    
    /**
     * Get total memory usage estimate
     */
    fun getMemoryUsage(): Long {
        return meshes.sumOf { mesh ->
            mesh.vertices.size * VERTEX_SIZE_BYTES + mesh.indices.size * 4L
        }
    }
    
    companion object {
        private const val VERTEX_SIZE_BYTES = 32 + 12 + 8 + 16 + 16 + 16 // Rough estimate
        
        fun empty() = MeshData(
            meshes = emptyList(),
            totalVertices = 0,
            totalTriangles = 0,
            boundingBox = null
        )
    }
}

/**
 * Represents a single mesh
 */
data class Mesh(
    val name: String,
    val vertices: List<Vertex>,
    val indices: List<Int>,
    val materials: List<String>,
    val boundingBox: VRMModel.BoundingBox?
) {
    /**
     * Get triangle count
     */
    fun getTriangleCount(): Int = indices.size / 3
    
    /**
     * Check if mesh has bone weights
     */
    fun hasBoneWeights(): Boolean = vertices.any { vertex ->
        vertex.boneWeights.any { it > 0f }
    }
}

/**
 * Represents a single vertex
 */
data class Vertex(
    val position: Vector3,
    val normal: Vector3 = Vector3.UP,
    val uv: Pair<Float, Float> = Pair(0f, 0f),
    val color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val boneWeights: FloatArray = floatArrayOf(1f, 0f, 0f, 0f),
    val boneIndices: IntArray = intArrayOf(0, 0, 0, 0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vertex

        if (position != other.position) return false
        if (normal != other.normal) return false
        if (uv != other.uv) return false
        if (!color.contentEquals(other.color)) return false
        if (!boneWeights.contentEquals(other.boneWeights)) return false
        if (!boneIndices.contentEquals(other.boneIndices)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + normal.hashCode()
        result = 31 * result + uv.hashCode()
        result = 31 * result + color.contentHashCode()
        result = 31 * result + boneWeights.contentHashCode()
        result = 31 * result + boneIndices.contentHashCode()
        return result
    }
}

/**
 * Represents primitive data during extraction
 */
internal data class PrimitiveData(
    val vertices: List<Vertex>,
    val indices: List<Int>
)