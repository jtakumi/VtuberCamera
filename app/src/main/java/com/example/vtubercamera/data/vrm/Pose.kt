package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Transform

/**
 * VRM pose data containing bone transformations
 */
data class Pose(
    val name: String,
    val displayName: String = name,
    val boneTransforms: Map<String, Transform> = emptyMap(),
    val description: String = "",
    val category: PoseCategory = PoseCategory.GENERAL,
    val isLooping: Boolean = false,
    val duration: Float = 0f
) {
    /**
     * Categories for organizing poses
     */
    enum class PoseCategory {
        GENERAL,
        GREETING,
        EMOTION,
        ACTION,
        DANCE,
        IDLE,
        CUSTOM
    }

    /**
     * Check if this pose has bone transform data
     */
    fun hasBoneTransforms(): Boolean = boneTransforms.isNotEmpty()

    /**
     * Get the transform for a specific bone
     */
    fun getBoneTransform(boneName: String): Transform? = boneTransforms[boneName]

    /**
     * Check if a specific bone is affected by this pose
     */
    fun affectsBone(boneName: String): Boolean = boneTransforms.containsKey(boneName)

    /**
     * Get all bone names affected by this pose
     */
    fun getAffectedBones(): Set<String> = boneTransforms.keys

    /**
     * Create a copy with modified bone transform
     */
    fun withBoneTransform(boneName: String, transform: Transform): Pose {
        val newTransforms = boneTransforms.toMutableMap()
        newTransforms[boneName] = transform
        return copy(boneTransforms = newTransforms)
    }

    /**
     * Create a copy without a specific bone transform
     */
    fun withoutBone(boneName: String): Pose {
        val newTransforms = boneTransforms.toMutableMap()
        newTransforms.remove(boneName)
        return copy(boneTransforms = newTransforms)
    }

    /**
     * Blend this pose with another pose
     */
    fun blendWith(other: Pose, weight: Float): Pose {
        val clampedWeight = weight.coerceIn(0f, 1f)
        
        val blendedTransforms = mutableMapOf<String, Transform>()
        val allBoneNames = (boneTransforms.keys + other.boneTransforms.keys).distinct()
        
        for (boneName in allBoneNames) {
            val thisTransform = boneTransforms[boneName] ?: Transform.identity()
            val otherTransform = other.boneTransforms[boneName] ?: Transform.identity()
            blendedTransforms[boneName] = thisTransform.lerp(otherTransform, clampedWeight)
        }
        
        return copy(
            name = "${name}_blend_${other.name}",
            displayName = "$displayName + ${other.displayName}",
            boneTransforms = blendedTransforms,
            category = PoseCategory.CUSTOM
        )
    }

    /**
     * Scale all transforms in this pose by a factor
     */
    fun withScaledTransforms(scaleFactor: Float): Pose {
        val scaledTransforms = boneTransforms.mapValues { (_, transform) ->
            transform.scaleBy(scaleFactor)
        }
        return copy(boneTransforms = scaledTransforms)
    }

    /**
     * Apply an offset to all position transforms in this pose
     */
    fun withPositionOffset(offset: com.example.vtubercamera.data.vrm.math.Vector3): Pose {
        val offsetTransforms = boneTransforms.mapValues { (_, transform) ->
            transform.translate(offset)
        }
        return copy(boneTransforms = offsetTransforms)
    }

    /**
     * Get pose statistics
     */
    fun getStatistics(): PoseStatistics = PoseStatistics(
        boneCount = boneTransforms.size,
        hasRootMotion = boneTransforms.containsKey("root") || boneTransforms.containsKey("hips"),
        affectsUpperBody = boneTransforms.keys.any { it.contains("spine") || it.contains("arm") || it.contains("hand") },
        affectsLowerBody = boneTransforms.keys.any { it.contains("leg") || it.contains("foot") || it.contains("hip") },
        affectsFace = boneTransforms.keys.any { it.contains("head") || it.contains("neck") || it.contains("eye") }
    )

    companion object {
        /**
         * Create a T-pose (default humanoid pose)
         */
        fun tPose(): Pose = Pose(
            name = "t_pose",
            displayName = "T-Pose",
            boneTransforms = mapOf(
                "left_upper_arm" to Transform.rotate(
                    com.example.vtubercamera.data.vrm.math.Quaternion.fromEuler(0f, 0f, -90f * kotlin.math.PI.toFloat() / 180f)
                ),
                "right_upper_arm" to Transform.rotate(
                    com.example.vtubercamera.data.vrm.math.Quaternion.fromEuler(0f, 0f, 90f * kotlin.math.PI.toFloat() / 180f)
                )
            ),
            category = PoseCategory.GENERAL,
            description = "Standard T-pose for calibration"
        )

        /**
         * Create a neutral standing pose
         */
        fun neutralStanding(): Pose = Pose(
            name = "neutral_standing",
            displayName = "Neutral Standing",
            boneTransforms = emptyMap(),
            category = PoseCategory.IDLE,
            description = "Neutral standing position"
        )

        /**
         * Create a waving pose
         */
        fun wave(): Pose = Pose(
            name = "wave",
            displayName = "Wave",
            boneTransforms = mapOf(
                "right_upper_arm" to Transform.rotate(
                    com.example.vtubercamera.data.vrm.math.Quaternion.fromEuler(0f, 0f, 45f * kotlin.math.PI.toFloat() / 180f)
                ),
                "right_lower_arm" to Transform.rotate(
                    com.example.vtubercamera.data.vrm.math.Quaternion.fromEuler(0f, 0f, -30f * kotlin.math.PI.toFloat() / 180f)
                )
            ),
            category = PoseCategory.GREETING,
            description = "Friendly waving gesture"
        )

        /**
         * Create a pointing pose
         */
        fun point(): Pose = Pose(
            name = "point",
            displayName = "Point",
            boneTransforms = mapOf(
                "right_upper_arm" to Transform.rotate(
                    com.example.vtubercamera.data.vrm.math.Quaternion.fromEuler(0f, 0f, 0f)
                ),
                "right_lower_arm" to Transform.rotate(
                    com.example.vtubercamera.data.vrm.math.Quaternion.fromEuler(0f, 0f, 0f)
                )
            ),
            category = PoseCategory.ACTION,
            description = "Pointing gesture"
        )
    }
}

/**
 * Statistics about a pose
 */
data class PoseStatistics(
    val boneCount: Int,
    val hasRootMotion: Boolean,
    val affectsUpperBody: Boolean,
    val affectsLowerBody: Boolean,
    val affectsFace: Boolean
) {
    /**
     * Check if this is a full-body pose
     */
    fun isFullBody(): Boolean = affectsUpperBody && affectsLowerBody

    /**
     * Check if this is an upper-body only pose
     */
    fun isUpperBodyOnly(): Boolean = affectsUpperBody && !affectsLowerBody

    /**
     * Check if this is a facial pose
     */
    fun isFacialPose(): Boolean = affectsFace && !affectsUpperBody && !affectsLowerBody

    /**
     * Get a complexity rating (0-5)
     */
    fun getComplexityRating(): Int = when {
        boneCount == 0 -> 0
        boneCount <= 5 -> 1
        boneCount <= 10 -> 2
        boneCount <= 20 -> 3
        boneCount <= 40 -> 4
        else -> 5
    }
}