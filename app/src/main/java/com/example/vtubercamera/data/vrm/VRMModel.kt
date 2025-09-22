package com.example.vtubercamera.data.vrm

/**
 * Main VRM model data class containing all VRM data
 */
data class VRMModel(
    val id: String,
    val name: String,
    val meshData: ByteArray,
    val textureData: Map<String, ByteArray> = emptyMap(),
    val expressions: List<Expression> = emptyList(),
    val poses: List<Pose> = emptyList(),
    val metadata: VRMMetadata,
    val version: String = "1.0",
    val boneNames: List<String> = emptyList(),
    val materialNames: List<String> = emptyList(),
    val animationClips: List<AnimationClip> = emptyList(),
    val boundingBox: BoundingBox? = null,
    val polyCount: Int = 0,
    val textureResolution: TextureResolution? = null
) {
    /**
     * Animation clip data
     */
    data class AnimationClip(
        val name: String,
        val duration: Float,
        val isLooping: Boolean = false,
        val frameRate: Float = 30f
    )

    /**
     * Bounding box for the model
     */
    data class BoundingBox(
        val min: com.example.vtubercamera.data.vrm.math.Vector3,
        val max: com.example.vtubercamera.data.vrm.math.Vector3
    ) {
        /**
         * Get the center of the bounding box
         */
        fun getCenter(): com.example.vtubercamera.data.vrm.math.Vector3 = 
            com.example.vtubercamera.data.vrm.math.Vector3(
                (min.x + max.x) * 0.5f,
                (min.y + max.y) * 0.5f,
                (min.z + max.z) * 0.5f
            )

        /**
         * Get the size of the bounding box
         */
        fun getSize(): com.example.vtubercamera.data.vrm.math.Vector3 = 
            com.example.vtubercamera.data.vrm.math.Vector3(
                max.x - min.x,
                max.y - min.y,
                max.z - min.z
            )

        /**
         * Get the largest dimension
         */
        fun getMaxDimension(): Float {
            val size = getSize()
            return maxOf(size.x, size.y, size.z)
        }
    }

    /**
     * Texture resolution information
     */
    data class TextureResolution(
        val mainTexture: Pair<Int, Int>? = null,
        val normalTexture: Pair<Int, Int>? = null,
        val emissionTexture: Pair<Int, Int>? = null,
        val occlusionTexture: Pair<Int, Int>? = null
    ) {
        /**
         * Get the maximum texture resolution
         */
        fun getMaxResolution(): Pair<Int, Int>? {
            val resolutions = listOfNotNull(mainTexture, normalTexture, emissionTexture, occlusionTexture)
            return if (resolutions.isNotEmpty()) {
                resolutions.maxByOrNull { it.first * it.second }
            } else null
        }

        /**
         * Get total texture memory usage estimate (in bytes)
         */
        fun getEstimatedMemoryUsage(): Long {
            var totalMemory = 0L
            mainTexture?.let { totalMemory += it.first * it.second * 4 } // RGBA
            normalTexture?.let { totalMemory += it.first * it.second * 4 }
            emissionTexture?.let { totalMemory += it.first * it.second * 4 }
            occlusionTexture?.let { totalMemory += it.first * it.second * 4 }
            return totalMemory
        }
    }

    /**
     * Check if the model has expressions
     */
    fun hasExpressions(): Boolean = expressions.isNotEmpty()

    /**
     * Check if the model has poses
     */
    fun hasPoses(): Boolean = poses.isNotEmpty()

    /**
     * Check if the model has textures
     */
    fun hasTextures(): Boolean = textureData.isNotEmpty()

    /**
     * Check if the model has animation clips
     */
    fun hasAnimations(): Boolean = animationClips.isNotEmpty()

    /**
     * Get expression by name
     */
    fun getExpression(name: String): Expression? = expressions.find { it.name == name }

    /**
     * Get pose by name
     */
    fun getPose(name: String): Pose? = poses.find { it.name == name }

    /**
     * Get animation clip by name
     */
    fun getAnimationClip(name: String): AnimationClip? = animationClips.find { it.name == name }

    /**
     * Get all expression names
     */
    fun getExpressionNames(): List<String> = expressions.map { it.name }

    /**
     * Get all pose names
     */
    fun getPoseNames(): List<String> = poses.map { it.name }

    /**
     * Get all animation clip names
     */
    fun getAnimationClipNames(): List<String> = animationClips.map { it.name }

    /**
     * Get texture data by name
     */
    fun getTextureData(textureName: String): ByteArray? = textureData[textureName]

    /**
     * Get all texture names
     */
    fun getTextureNames(): List<String> = textureData.keys.toList()

    /**
     * Check if a specific bone exists
     */
    fun hasBone(boneName: String): Boolean = boneNames.contains(boneName)

    /**
     * Check if a specific material exists
     */
    fun hasMaterial(materialName: String): Boolean = materialNames.contains(materialName)

    /**
     * Get estimated memory usage for the entire model
     */
    fun getEstimatedMemoryUsage(): Long {
        var totalMemory = meshData.size.toLong()
        totalMemory += textureData.values.sumOf { it.size.toLong() }
        totalMemory += textureResolution?.getEstimatedMemoryUsage() ?: 0L
        return totalMemory
    }

    /**
     * Get model complexity rating (1-5)
     */
    fun getComplexityRating(): Int = when {
        polyCount < 1000 -> 1
        polyCount < 5000 -> 2
        polyCount < 15000 -> 3
        polyCount < 30000 -> 4
        else -> 5
    }

    /**
     * Get model statistics
     */
    fun getStatistics(): ModelStatistics = ModelStatistics(
        polyCount = polyCount,
        boneCount = boneNames.size,
        materialCount = materialNames.size,
        textureCount = textureData.size,
        expressionCount = expressions.size,
        poseCount = poses.size,
        animationCount = animationClips.size,
        memoryUsage = getEstimatedMemoryUsage(),
        complexityRating = getComplexityRating(),
        hasBlendShapes = expressions.any { it.hasBlendShapes() },
        hasBoneAnimations = poses.isNotEmpty() || animationClips.isNotEmpty()
    )

    /**
     * Create a copy with updated metadata
     */
    fun withMetadata(newMetadata: VRMMetadata): VRMModel = copy(metadata = newMetadata)

    /**
     * Create a copy with updated name
     */
    fun withName(newName: String): VRMModel = copy(name = newName)

    /**
     * Create a copy with additional expression
     */
    fun withExpression(expression: Expression): VRMModel = copy(expressions = expressions + expression)

    /**
     * Create a copy with additional pose
     */
    fun withPose(pose: Pose): VRMModel = copy(poses = poses + pose)

    /**
     * Create a copy without a specific expression
     */
    fun withoutExpression(expressionName: String): VRMModel = 
        copy(expressions = expressions.filter { it.name != expressionName })

    /**
     * Create a copy without a specific pose
     */
    fun withoutPose(poseName: String): VRMModel = 
        copy(poses = poses.filter { it.name != poseName })

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VRMModel

        if (id != other.id) return false
        if (name != other.name) return false
        if (!meshData.contentEquals(other.meshData)) return false
        if (textureData.size != other.textureData.size) return false
        if (expressions != other.expressions) return false
        if (poses != other.poses) return false
        if (metadata != other.metadata) return false
        if (version != other.version) return false

        // Check texture data equality
        for ((key, value) in textureData) {
            val otherValue = other.textureData[key]
            if (otherValue == null || !value.contentEquals(otherValue)) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + meshData.contentHashCode()
        result = 31 * result + textureData.hashCode()
        result = 31 * result + expressions.hashCode()
        result = 31 * result + poses.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    companion object {
        /**
         * Create a minimal VRM model for testing
         */
        fun createMinimal(
            id: String,
            name: String,
            meshData: ByteArray,
            metadata: VRMMetadata
        ): VRMModel = VRMModel(
            id = id,
            name = name,
            meshData = meshData,
            metadata = metadata
        )

        /**
         * Create a VRM model with basic expressions
         */
        fun createWithBasicExpressions(
            id: String,
            name: String,
            meshData: ByteArray,
            metadata: VRMMetadata
        ): VRMModel = VRMModel(
            id = id,
            name = name,
            meshData = meshData,
            metadata = metadata,
            expressions = listOf(
                Expression.neutral(),
                Expression.happy(),
                Expression.sad(),
                Expression.angry(),
                Expression.surprised()
            )
        )
    }
}

/**
 * Statistics about a VRM model
 */
data class ModelStatistics(
    val polyCount: Int,
    val boneCount: Int,
    val materialCount: Int,
    val textureCount: Int,
    val expressionCount: Int,
    val poseCount: Int,
    val animationCount: Int,
    val memoryUsage: Long,
    val complexityRating: Int,
    val hasBlendShapes: Boolean,
    val hasBoneAnimations: Boolean
) {
    /**
     * Get formatted memory usage
     */
    fun getFormattedMemoryUsage(): String {
        return when {
            memoryUsage < 1024 -> "${memoryUsage}B"
            memoryUsage < 1024 * 1024 -> "${memoryUsage / 1024}KB"
            memoryUsage < 1024 * 1024 * 1024 -> "${memoryUsage / (1024 * 1024)}MB"
            else -> "${memoryUsage / (1024 * 1024 * 1024)}GB"
        }
    }

    /**
     * Check if this is a high-poly model
     */
    fun isHighPoly(): Boolean = polyCount > 20000

    /**
     * Check if this is a complex model
     */
    fun isComplex(): Boolean = complexityRating >= 4

    /**
     * Get a summary description
     */
    fun getSummary(): String {
        val features = mutableListOf<String>()
        
        if (hasBlendShapes) features.add("Expressions")
        if (hasBoneAnimations) features.add("Animations")
        if (isHighPoly()) features.add("High-poly")
        
        val featureText = if (features.isNotEmpty()) {
            " (${features.joinToString(", ")})"
        } else ""
        
        return "${getFormattedMemoryUsage()}, ${polyCount} polys$featureText"
    }
}