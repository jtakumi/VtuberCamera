package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.data.vrm.math.Quaternion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for applying VRM pose data and bone transformations to avatars
 * Handles pose transitions, bone manipulation, and hierarchical transformations
 */
@Singleton
class PoseController @Inject constructor() {
    
    private val _currentPose = MutableStateFlow<Pose?>(null)
    val currentPose: StateFlow<Pose?> = _currentPose.asStateFlow()
    
    private val _activeBoneTransforms = MutableStateFlow<Map<String, Transform>>(emptyMap())
    val activeBoneTransforms: StateFlow<Map<String, Transform>> = _activeBoneTransforms.asStateFlow()
    
    private val _boneLocks = MutableStateFlow<Set<String>>(emptySet())
    val boneLocks: StateFlow<Set<String>> = _boneLocks.asStateFlow()
    
    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()
    
    private val _transitionProgress = MutableStateFlow(0f)
    val transitionProgress: StateFlow<Float> = _transitionProgress.asStateFlow()
    
    // Configuration
    private var transitionDuration: Float = 0.5f // seconds
    private var currentTransitionTime: Float = 0f
    private var fromPose: Pose? = null
    private var toPose: Pose? = null
    private var boneMapping: BoneMapping? = null
    
    // Bone constraints
    private val boneConstraints = mutableMapOf<String, BoneConstraint>()
    
    /**
     * Set bone mapping for the current VRM model
     * @param mapping Bone mapping from VRM data
     */
    fun setBoneMapping(mapping: BoneMapping) {
        this.boneMapping = mapping
        initializeDefaultConstraints(mapping)
    }
    
    /**
     * Apply pose immediately without transition
     * @param pose Pose to apply, null to reset to default
     */
    fun applyPose(pose: Pose?) {
        _currentPose.value = pose
        _isTransitioning.value = false
        _transitionProgress.value = 1f
        
        if (pose != null) {
            updateActiveBoneTransforms(pose.boneTransforms)
        } else {
            clearActiveBoneTransforms()
        }
    }
    
    /**
     * Apply pose with smooth transition
     * @param pose Pose to transition to
     * @param duration Transition duration in seconds
     */
    fun transitionToPose(pose: Pose?, duration: Float = transitionDuration) {
        if (pose == _currentPose.value) return
        
        fromPose = _currentPose.value
        toPose = pose
        transitionDuration = duration
        currentTransitionTime = 0f
        
        _isTransitioning.value = true
        _transitionProgress.value = 0f
    }
    
    /**
     * Update transition progress (should be called from render loop)
     * @param deltaTime Time elapsed since last update in seconds
     */
    fun updateTransition(deltaTime: Float) {
        if (!_isTransitioning.value) return
        
        currentTransitionTime += deltaTime
        val progress = (currentTransitionTime / transitionDuration).coerceIn(0f, 1f)
        _transitionProgress.value = progress
        
        // Apply interpolated pose
        val interpolatedTransforms = interpolatePoses(fromPose, toPose, progress)
        updateActiveBoneTransforms(interpolatedTransforms)
        
        // Complete transition
        if (progress >= 1f) {
            _isTransitioning.value = false
            _currentPose.value = toPose
            fromPose = null
            toPose = null
        }
    }
    
    /**
     * Blend multiple poses with weights
     * @param poseWeights Map of poses to their weights (0.0 to 1.0)
     */
    fun blendPoses(poseWeights: Map<Pose, Float>) {
        if (poseWeights.isEmpty()) {
            clearActiveBoneTransforms()
            _currentPose.value = null
            return
        }
        
        // Normalize weights
        val totalWeight = poseWeights.values.sum()
        val normalizedWeights = if (totalWeight > 0f) {
            poseWeights.mapValues { (_, weight) -> weight / totalWeight }
        } else {
            poseWeights
        }
        
        // Blend bone transforms
        val blendedTransforms = mutableMapOf<String, Transform>()
        val allBoneNames = normalizedWeights.keys.flatMap { it.getAffectedBones() }.toSet()
        
        for (boneName in allBoneNames) {
            val transforms = mutableListOf<Pair<Transform, Float>>()
            
            for ((pose, weight) in normalizedWeights) {
                val boneTransform = pose.getBoneTransform(boneName)
                if (boneTransform != null) {
                    transforms.add(boneTransform to weight)
                }
            }
            
            if (transforms.isNotEmpty()) {
                blendedTransforms[boneName] = blendBoneTransforms(transforms)
            }
        }
        
        updateActiveBoneTransforms(blendedTransforms)
        
        // Set representative pose
        val dominantPose = normalizedWeights.maxByOrNull { it.value }?.key
        _currentPose.value = dominantPose
    }
    
    /**
     * Set individual bone transform
     * @param boneName Name of the bone
     * @param transform Transform to apply
     */
    fun setBoneTransform(boneName: String, transform: Transform) {
        if (_boneLocks.value.contains(boneName)) return
        
        val constraints = boneConstraints[boneName]
        val constrainedTransform = if (constraints != null) {
            applyBoneConstraints(transform, constraints)
        } else {
            transform
        }
        
        val currentTransforms = _activeBoneTransforms.value.toMutableMap()
        currentTransforms[boneName] = constrainedTransform
        _activeBoneTransforms.value = currentTransforms
        _currentPose.value = null // Mark as custom pose
    }
    
    /**
     * Get current bone transform
     * @param boneName Name of the bone
     * @return Current transform or identity if not found
     */
    fun getBoneTransform(boneName: String): Transform {
        return _activeBoneTransforms.value[boneName] ?: Transform.identity()
    }
    
    /**
     * Lock bone to prevent modification
     * @param boneName Name of the bone to lock
     */
    fun lockBone(boneName: String) {
        val currentLocks = _boneLocks.value.toMutableSet()
        currentLocks.add(boneName)
        _boneLocks.value = currentLocks
    }
    
    /**
     * Unlock bone to allow modification
     * @param boneName Name of the bone to unlock
     */
    fun unlockBone(boneName: String) {
        val currentLocks = _boneLocks.value.toMutableSet()
        currentLocks.remove(boneName)
        _boneLocks.value = currentLocks
    }
    
    /**
     * Check if bone is locked
     * @param boneName Name of the bone
     * @return True if bone is locked
     */
    fun isBoneLocked(boneName: String): Boolean {
        return _boneLocks.value.contains(boneName)
    }
    
    /**
     * Add bone constraint
     * @param boneName Name of the bone
     * @param constraint Constraint to apply
     */
    fun addBoneConstraint(boneName: String, constraint: BoneConstraint) {
        boneConstraints[boneName] = constraint
    }
    
    /**
     * Remove bone constraint
     * @param boneName Name of the bone
     */
    fun removeBoneConstraint(boneName: String) {
        boneConstraints.remove(boneName)
    }
    
    /**
     * Clear all bone constraints
     */
    fun clearBoneConstraints() {
        boneConstraints.clear()
    }
    
    /**
     * Reset to default pose (T-pose or bind pose)
     */
    fun resetToDefaultPose() {
        val defaultPose = Pose.tPose()
        applyPose(defaultPose)
    }
    
    /**
     * Clear all active poses and return to neutral
     */
    fun clearPose() {
        applyPose(null)
    }
    
    /**
     * Get pose application data for rendering engine
     */
    fun getPoseRenderData(): PoseRenderData {
        return PoseRenderData(
            boneTransforms = _activeBoneTransforms.value,
            boneLocks = _boneLocks.value,
            isTransitioning = _isTransitioning.value,
            transitionProgress = _transitionProgress.value,
            boneMapping = boneMapping
        )
    }
    
    /**
     * Set transition duration
     * @param duration Duration in seconds
     */
    fun setTransitionDuration(duration: Float) {
        this.transitionDuration = duration.coerceAtLeast(0.01f)
    }
    
    /**
     * Get available bone names from current model
     */
    fun getAvailableBones(): Set<String> {
        return boneMapping?.getAllBoneNames() ?: emptySet()
    }
    
    /**
     * Get bone hierarchy information
     */
    fun getBoneHierarchy(): Map<String, List<String>> {
        return boneMapping?.getBoneHierarchy() ?: emptyMap()
    }
    
    /**
     * Apply inverse kinematics to reach target position
     * @param endEffectorBone Name of the end effector bone
     * @param targetPosition Target position in world space
     * @param iterations Number of IK iterations
     */
    fun applyInverseKinematics(
        endEffectorBone: String, 
        targetPosition: Vector3, 
        iterations: Int = 10
    ) {
        val boneChain = getBoneChain(endEffectorBone)
        if (boneChain.isEmpty()) return
        
        // Simple CCD (Cyclic Coordinate Descent) IK
        for (i in 0 until iterations) {
            for (boneName in boneChain.reversed()) {
                val boneTransform = getBoneTransform(boneName)
                val endEffectorPos = calculateEndEffectorPosition(endEffectorBone)
                
                val toTarget = (targetPosition - boneTransform.position).normalized()
                val toEnd = (endEffectorPos - boneTransform.position).normalized()
                
                val rotationAxis = toEnd.cross(toTarget)
                val angle = kotlin.math.acos(toEnd.dot(toTarget).coerceIn(-1f, 1f))
                
                if (rotationAxis.magnitude() > 0.001f && angle > 0.001f) {
                    val rotation = Quaternion.fromAxisAngle(rotationAxis.normalized(), angle)
                    val newTransform = boneTransform.rotate(rotation)
                    setBoneTransform(boneName, newTransform)
                }
            }
        }
    }
    
    private fun updateActiveBoneTransforms(transforms: Map<String, Transform>) {
        val filteredTransforms = transforms.filterKeys { boneName ->
            !_boneLocks.value.contains(boneName)
        }
        _activeBoneTransforms.value = filteredTransforms
    }
    
    private fun clearActiveBoneTransforms() {
        _activeBoneTransforms.value = emptyMap()
    }
    
    private fun interpolatePoses(from: Pose?, to: Pose?, progress: Float): Map<String, Transform> {
        if (from == null && to == null) return emptyMap()
        if (from == null) return to?.boneTransforms?.mapValues { (_, transform) ->
            Transform.identity().lerp(transform, progress)
        } ?: emptyMap()
        if (to == null) return from.boneTransforms.mapValues { (_, transform) ->
            transform.lerp(Transform.identity(), progress)
        }
        
        val interpolatedTransforms = mutableMapOf<String, Transform>()
        val allBones = (from.getAffectedBones() + to.getAffectedBones()).distinct()
        
        for (boneName in allBones) {
            val fromTransform = from.getBoneTransform(boneName) ?: Transform.identity()
            val toTransform = to.getBoneTransform(boneName) ?: Transform.identity()
            interpolatedTransforms[boneName] = fromTransform.lerp(toTransform, progress)
        }
        
        return interpolatedTransforms
    }
    
    private fun blendBoneTransforms(transforms: List<Pair<Transform, Float>>): Transform {
        if (transforms.isEmpty()) return Transform.identity()
        if (transforms.size == 1) return transforms[0].first
        
        var blendedPosition = Vector3.ZERO
        var blendedScale = Vector3.ZERO
        val rotations = mutableListOf<Pair<Quaternion, Float>>()
        
        for ((transform, weight) in transforms) {
            blendedPosition += transform.position * weight
            blendedScale += transform.scale * weight
            rotations.add(transform.rotation to weight)
        }
        
        // Blend quaternions using SLERP
        var blendedRotation = rotations[0].first
        var remainingWeight = 1f - rotations[0].second
        
        for (i in 1 until rotations.size) {
            val (rotation, weight) = rotations[i]
            val t = if (remainingWeight > 0f) weight / (weight + remainingWeight) else 0f
            blendedRotation = Quaternion.slerp(blendedRotation, rotation, t)
            remainingWeight -= weight
        }
        
        return Transform(blendedPosition, blendedRotation, blendedScale)
    }
    
    private fun applyBoneConstraints(transform: Transform, constraint: BoneConstraint): Transform {
        var constrainedTransform = transform
        
        // Apply position constraints
        if (constraint.positionConstraints != null) {
            val pos = constraint.positionConstraints.clamp(transform.position)
            constrainedTransform = constrainedTransform.withPosition(pos)
        }
        
        // Apply rotation constraints
        if (constraint.rotationConstraints != null) {
            val euler = transform.rotation.toEuler()
            val constrainedEuler = constraint.rotationConstraints.clampEuler(euler)
            val constrainedRotation = Quaternion.fromEuler(constrainedEuler.x, constrainedEuler.y, constrainedEuler.z)
            constrainedTransform = constrainedTransform.withRotation(constrainedRotation)
        }
        
        // Apply scale constraints
        if (constraint.scaleConstraints != null) {
            val scale = Vector3(
                transform.scale.x.coerceIn(constraint.scaleConstraints.minScale, constraint.scaleConstraints.maxScale),
                transform.scale.y.coerceIn(constraint.scaleConstraints.minScale, constraint.scaleConstraints.maxScale),
                transform.scale.z.coerceIn(constraint.scaleConstraints.minScale, constraint.scaleConstraints.maxScale)
            )
            constrainedTransform = constrainedTransform.withScale(scale)
        }
        
        return constrainedTransform
    }
    
    private fun initializeDefaultConstraints(mapping: BoneMapping) {
        // Add typical constraints for humanoid bones
        val headBones = setOf("head", "neck")
        val armBones = setOf("leftUpperArm", "rightUpperArm", "leftLowerArm", "rightLowerArm")
        val legBones = setOf("leftUpperLeg", "rightUpperLeg", "leftLowerLeg", "rightLowerLeg")
        
        for (boneName in headBones) {
            if (mapping.hasBone(boneName)) {
                addBoneConstraint(boneName, BoneConstraint.createHeadConstraint())
            }
        }
        
        for (boneName in armBones) {
            if (mapping.hasBone(boneName)) {
                addBoneConstraint(boneName, BoneConstraint.createArmConstraint())
            }
        }
        
        for (boneName in legBones) {
            if (mapping.hasBone(boneName)) {
                addBoneConstraint(boneName, BoneConstraint.createLegConstraint())
            }
        }
    }
    
    private fun getBoneChain(endEffectorBone: String): List<String> {
        val hierarchy = getBoneHierarchy()
        val chain = mutableListOf<String>()
        var currentBone: String? = endEffectorBone
        
        while (currentBone != null) {
            chain.add(currentBone)
            currentBone = hierarchy.entries.find { 
                it.value.contains(currentBone) 
            }?.key
        }
        
        return chain
    }
    
    private fun calculateEndEffectorPosition(boneName: String): Vector3 {
        val transform = getBoneTransform(boneName)
        return transform.position
    }
}

/**
 * Data class for pose render data
 */
data class PoseRenderData(
    val boneTransforms: Map<String, Transform>,
    val boneLocks: Set<String>,
    val isTransitioning: Boolean,
    val transitionProgress: Float,
    val boneMapping: BoneMapping?
) {
    companion object {
        fun empty() = PoseRenderData(
            boneTransforms = emptyMap(),
            boneLocks = emptySet(),
            isTransitioning = false,
            transitionProgress = 0f,
            boneMapping = null
        )
    }
    
    /**
     * Check if any pose data is active
     */
    fun hasActivePose(): Boolean = boneTransforms.isNotEmpty()
    
    /**
     * Get affected bone count
     */
    fun getAffectedBoneCount(): Int = boneTransforms.size
    
    /**
     * Check if bone is affected
     */
    fun isBoneAffected(boneName: String): Boolean = boneTransforms.containsKey(boneName)
}

/**
 * Bone constraint definition
 */
data class BoneConstraint(
    val positionConstraints: PositionConstraints? = null,
    val rotationConstraints: RotationConstraints? = null,
    val scaleConstraints: ScaleConstraints? = null
) {
    companion object {
        fun createHeadConstraint() = BoneConstraint(
            rotationConstraints = RotationConstraints(
                minPitch = -45f, maxPitch = 45f,
                minYaw = -90f, maxYaw = 90f,
                minRoll = -30f, maxRoll = 30f
            )
        )
        
        fun createArmConstraint() = BoneConstraint(
            rotationConstraints = RotationConstraints(
                minPitch = -180f, maxPitch = 180f,
                minYaw = -90f, maxYaw = 90f,
                minRoll = -180f, maxRoll = 180f
            )
        )
        
        fun createLegConstraint() = BoneConstraint(
            rotationConstraints = RotationConstraints(
                minPitch = -120f, maxPitch = 30f,
                minYaw = -45f, maxYaw = 45f,
                minRoll = -30f, maxRoll = 30f
            )
        )
    }
}

/**
 * Position constraints
 */
data class PositionConstraints(
    val minPosition: Vector3,
    val maxPosition: Vector3
) {
    fun clamp(position: Vector3): Vector3 = Vector3(
        position.x.coerceIn(minPosition.x, maxPosition.x),
        position.y.coerceIn(minPosition.y, maxPosition.y),
        position.z.coerceIn(minPosition.z, maxPosition.z)
    )
}

/**
 * Rotation constraints (in degrees)
 */
data class RotationConstraints(
    val minPitch: Float = -180f,
    val maxPitch: Float = 180f,
    val minYaw: Float = -180f,
    val maxYaw: Float = 180f,
    val minRoll: Float = -180f,
    val maxRoll: Float = 180f
) {
    fun clampEuler(euler: Vector3): Vector3 = Vector3(
        euler.x.coerceIn(minPitch, maxPitch),
        euler.y.coerceIn(minYaw, maxYaw),
        euler.z.coerceIn(minRoll, maxRoll)
    )
}

/**
 * Scale constraints
 */
data class ScaleConstraints(
    val minScale: Float = 0.1f,
    val maxScale: Float = 3.0f
)