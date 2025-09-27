package com.example.vtubercamera.data.vrm

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.data.vrm.math.Quaternion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Loads pose and animation data from VRM/glTF files
 */
class VRMPoseLoader {
    
    companion object {
        // Standard humanoid bone names
        private val HUMANOID_BONES = setOf(
            "hips", "spine", "chest", "upperChest", "neck", "head",
            "leftShoulder", "leftUpperArm", "leftLowerArm", "leftHand",
            "rightShoulder", "rightUpperArm", "rightLowerArm", "rightHand",
            "leftUpperLeg", "leftLowerLeg", "leftFoot", "leftToes",
            "rightUpperLeg", "rightLowerLeg", "rightFoot", "rightToes",
            "leftEye", "rightEye", "jaw",
            "leftThumbProximal", "leftThumbIntermediate", "leftThumbDistal",
            "leftIndexProximal", "leftIndexIntermediate", "leftIndexDistal",
            "leftMiddleProximal", "leftMiddleIntermediate", "leftMiddleDistal",
            "leftRingProximal", "leftRingIntermediate", "leftRingDistal",
            "leftLittleProximal", "leftLittleIntermediate", "leftLittleDistal",
            "rightThumbProximal", "rightThumbIntermediate", "rightThumbDistal",
            "rightIndexProximal", "rightIndexIntermediate", "rightIndexDistal",
            "rightMiddleProximal", "rightMiddleIntermediate", "rightMiddleDistal",
            "rightRingProximal", "rightRingIntermediate", "rightRingDistal",
            "rightLittleProximal", "rightLittleIntermediate", "rightLittleDistal"
        )
        
        // Animation interpolation types
        private const val INTERPOLATION_LINEAR = "LINEAR"
        private const val INTERPOLATION_STEP = "STEP"
        private const val INTERPOLATION_CUBICSPLINE = "CUBICSPLINE"
    }
    
    /**
     * Load poses and animations from glTF data
     */
    suspend fun loadPosesAndAnimations(
        json: JsonObject,
        binaryData: ByteArray,
        vrmExtension: JsonObject?
    ): PoseData = withContext(Dispatchers.IO) {
        val poses = mutableListOf<Pose>()
        val animations = mutableListOf<AnimationData>()
        val boneMapping = extractBoneMapping(json, vrmExtension)
        
        try {
            // Load animations from glTF
            val animationsArray = json.getAsJsonArray("animations")
            if (animationsArray != null) {
                for (i in 0 until animationsArray.size()) {
                    val animationJson = animationsArray[i].asJsonObject
                    val animationData = parseAnimation(animationJson, json, binaryData, boneMapping)
                    if (animationData != null) {
                        animations.add(animationData)
                        
                        // Create static poses from animation keyframes
                        val staticPoses = extractStaticPoses(animationData, boneMapping)
                        poses.addAll(staticPoses)
                    }
                }
            }
            
            // Load VRM-specific poses
            val vrmPoses = loadVRMPoses(vrmExtension, boneMapping)
            poses.addAll(vrmPoses)
            
            // Add default poses if none exist
            if (poses.isEmpty()) {
                poses.addAll(createDefaultPoses(boneMapping))
            }
            
        } catch (e: Exception) {
            // Log error but continue with what we have
        }
        
        PoseData(
            poses = poses,
            animations = animations,
            boneMapping = boneMapping,
            staticPoses = poses.filter { it.isLooping.not() },
            loopingAnimations = poses.filter { it.isLooping }
        )
    }
    
    /**
     * Extract bone mapping from VRM humanoid definition
     */
    private fun extractBoneMapping(json: JsonObject, vrmExtension: JsonObject?): BoneMapping {
        val boneMap = mutableMapOf<String, BoneInfo>()
        val nodes = json.getAsJsonArray("nodes")
        
        // Extract node names and transforms
        if (nodes != null) {
            for (i in 0 until nodes.size()) {
                val node = nodes[i].asJsonObject
                val name = node.get("name")?.asString ?: "node_$i"
                val transform = parseNodeTransform(node)
                
                boneMap[name] = BoneInfo(
                    name = name,
                    nodeIndex = i,
                    transform = transform,
                    isHumanoidBone = isHumanoidBone(name),
                    parentIndex = null // Would be calculated from scene hierarchy
                )
            }
        }
        
        // Apply VRM humanoid mapping if available
        vrmExtension?.let { vrm ->
            val humanoid = vrm.getAsJsonObject("humanoid")
            val humanBones = humanoid?.getAsJsonArray("humanBones")
            
            humanBones?.forEach { boneElement ->
                val bone = boneElement.asJsonObject
                val boneName = bone.get("bone")?.asString
                val nodeIndex = bone.get("node")?.asInt
                
                if (boneName != null && nodeIndex != null && nodeIndex < nodes.size()) {
                    val nodeName = nodes[nodeIndex].asJsonObject.get("name")?.asString ?: "node_$nodeIndex"
                    
                    boneMap[boneName] = boneMap[nodeName]?.copy(
                        humanoidName = boneName,
                        isHumanoidBone = true
                    ) ?: BoneInfo(
                        name = nodeName,
                        nodeIndex = nodeIndex,
                        transform = Transform.identity(),
                        isHumanoidBone = true,
                        humanoidName = boneName
                    )
                }
            }
        }
        
        return BoneMapping(
            bones = boneMap,
            humanoidBones = boneMap.filter { it.value.isHumanoidBone },
            rootBone = findRootBone(boneMap)
        )
    }
    
    /**
     * Parse node transform from glTF node
     */
    private fun parseNodeTransform(node: JsonObject): Transform {
        // Check for matrix
        val matrix = node.getAsJsonArray("matrix")
        if (matrix != null && matrix.size() == 16) {
            return parseMatrixTransform(matrix)
        }
        
        // Parse TRS
        val translation = node.getAsJsonArray("translation")
        val rotation = node.getAsJsonArray("rotation")
        val scale = node.getAsJsonArray("scale")
        
        val position = if (translation != null && translation.size() >= 3) {
            Vector3(translation[0].asFloat, translation[1].asFloat, translation[2].asFloat)
        } else {
            Vector3.ZERO
        }
        
        val rot = if (rotation != null && rotation.size() >= 4) {
            Quaternion(rotation[0].asFloat, rotation[1].asFloat, rotation[2].asFloat, rotation[3].asFloat)
        } else {
            Quaternion.IDENTITY
        }
        
        val scl = if (scale != null && scale.size() >= 3) {
            Vector3(scale[0].asFloat, scale[1].asFloat, scale[2].asFloat)
        } else {
            Vector3.ONE
        }
        
        return Transform(position, rot, scl)
    }
    
    /**
     * Parse 4x4 matrix transform
     */
    private fun parseMatrixTransform(matrix: JsonArray): Transform {
        // For simplicity, we'll extract position from the matrix
        // A full implementation would decompose the matrix into TRS
        val position = Vector3(
            matrix[12].asFloat,
            matrix[13].asFloat,
            matrix[14].asFloat
        )
        
        return Transform(position = position)
    }
    
    /**
     * Parse animation from glTF animation object
     */
    private suspend fun parseAnimation(
        animationJson: JsonObject,
        gltfJson: JsonObject,
        binaryData: ByteArray,
        boneMapping: BoneMapping
    ): AnimationData? = withContext(Dispatchers.IO) {
        try {
            val name = animationJson.get("name")?.asString ?: "animation"
            val channels = animationJson.getAsJsonArray("channels")
            val samplers = animationJson.getAsJsonArray("samplers")
            val accessors = gltfJson.getAsJsonArray("accessors")
            val bufferViews = gltfJson.getAsJsonArray("bufferViews")
            
            if (channels == null || samplers == null) return@withContext null
            
            val animationChannels = mutableListOf<AnimationChannel>()
            var duration = 0f
            
            // Parse channels
            for (i in 0 until channels.size()) {
                val channel = channels[i].asJsonObject
                val samplerIndex = channel.get("sampler")?.asInt ?: continue
                val target = channel.getAsJsonObject("target")
                val nodeIndex = target.get("node")?.asInt ?: continue
                val path = target.get("path")?.asString ?: continue
                
                if (samplerIndex < samplers.size()) {
                    val sampler = samplers[samplerIndex].asJsonObject
                    val animationChannel = parseSampler(
                        sampler, nodeIndex, path, accessors, bufferViews, binaryData, boneMapping
                    )
                    
                    if (animationChannel != null) {
                        animationChannels.add(animationChannel)
                        duration = maxOf(duration, animationChannel.duration)
                    }
                }
            }
            
            AnimationData(
                name = name,
                channels = animationChannels,
                duration = duration,
                isLooping = true, // Assume looping by default
                frameRate = 30f
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse animation sampler
     */
    private suspend fun parseSampler(
        sampler: JsonObject,
        nodeIndex: Int,
        path: String,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray,
        boneMapping: BoneMapping
    ): AnimationChannel? = withContext(Dispatchers.IO) {
        try {
            val inputAccessorIndex = sampler.get("input")?.asInt ?: return@withContext null
            val outputAccessorIndex = sampler.get("output")?.asInt ?: return@withContext null
            val interpolation = sampler.get("interpolation")?.asString ?: INTERPOLATION_LINEAR
            
            // Extract time values
            val timeValues = extractFloatArray(inputAccessorIndex, accessors, bufferViews, binaryData)
            
            // Extract keyframe values based on path
            val keyframes = when (path) {
                "translation" -> {
                    val positions = extractVector3Array(outputAccessorIndex, accessors, bufferViews, binaryData)
                    positions.mapIndexed { index, position ->
                        AnimationKeyframe(
                            time = if (index < timeValues.size) timeValues[index] else 0f,
                            transform = Transform(position = position)
                        )
                    }
                }
                "rotation" -> {
                    val rotations = extractQuaternionArray(outputAccessorIndex, accessors, bufferViews, binaryData)
                    rotations.mapIndexed { index, rotation ->
                        AnimationKeyframe(
                            time = if (index < timeValues.size) timeValues[index] else 0f,
                            transform = Transform(rotation = rotation)
                        )
                    }
                }
                "scale" -> {
                    val scales = extractVector3Array(outputAccessorIndex, accessors, bufferViews, binaryData)
                    scales.mapIndexed { index, scale ->
                        AnimationKeyframe(
                            time = if (index < timeValues.size) timeValues[index] else 0f,
                            transform = Transform(scale = scale)
                        )
                    }
                }
                else -> emptyList()
            }
            
            val boneName = boneMapping.getBoneNameByNodeIndex(nodeIndex) ?: "node_$nodeIndex"
            val duration = if (timeValues.isNotEmpty()) timeValues.last() else 0f
            
            AnimationChannel(
                boneName = boneName,
                path = path,
                keyframes = keyframes,
                interpolation = parseInterpolationType(interpolation),
                duration = duration
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract float array from accessor
     */
    private suspend fun extractFloatArray(
        accessorIndex: Int,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): List<Float> = withContext(Dispatchers.IO) {
        try {
            val accessor = accessors[accessorIndex].asJsonObject
            val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
            val bufferView = bufferViews[bufferViewIndex].asJsonObject
            
            val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
            val count = accessor.get("count")?.asInt ?: 0
            val componentType = accessor.get("componentType")?.asInt ?: 5126 // FLOAT
            
            val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * 4)
                .order(ByteOrder.LITTLE_ENDIAN)
            
            val values = mutableListOf<Float>()
            for (i in 0 until count) {
                values.add(buffer.float)
            }
            
            values
        } catch (e: Exception) {
            emptyList()
        }
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
        try {
            val accessor = accessors[accessorIndex].asJsonObject
            val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
            val bufferView = bufferViews[bufferViewIndex].asJsonObject
            
            val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
            val count = accessor.get("count")?.asInt ?: 0
            
            val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * 12)
                .order(ByteOrder.LITTLE_ENDIAN)
            
            val vectors = mutableListOf<Vector3>()
            for (i in 0 until count) {
                val x = buffer.float
                val y = buffer.float
                val z = buffer.float
                vectors.add(Vector3(x, y, z))
            }
            
            vectors
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Extract Quaternion array from accessor
     */
    private suspend fun extractQuaternionArray(
        accessorIndex: Int,
        accessors: JsonArray,
        bufferViews: JsonArray,
        binaryData: ByteArray
    ): List<Quaternion> = withContext(Dispatchers.IO) {
        try {
            val accessor = accessors[accessorIndex].asJsonObject
            val bufferViewIndex = accessor.get("bufferView")?.asInt ?: return@withContext emptyList()
            val bufferView = bufferViews[bufferViewIndex].asJsonObject
            
            val byteOffset = (accessor.get("byteOffset")?.asInt ?: 0) + (bufferView.get("byteOffset")?.asInt ?: 0)
            val count = accessor.get("count")?.asInt ?: 0
            
            val buffer = ByteBuffer.wrap(binaryData, byteOffset, count * 16)
                .order(ByteOrder.LITTLE_ENDIAN)
            
            val quaternions = mutableListOf<Quaternion>()
            for (i in 0 until count) {
                val x = buffer.float
                val y = buffer.float
                val z = buffer.float
                val w = buffer.float
                quaternions.add(Quaternion(x, y, z, w))
            }
            
            quaternions
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Parse interpolation type
     */
    private fun parseInterpolationType(interpolation: String): InterpolationType {
        return when (interpolation) {
            INTERPOLATION_LINEAR -> InterpolationType.LINEAR
            INTERPOLATION_STEP -> InterpolationType.STEP
            INTERPOLATION_CUBICSPLINE -> InterpolationType.CUBIC_SPLINE
            else -> InterpolationType.LINEAR
        }
    }
    
    /**
     * Extract static poses from animation data
     */
    private fun extractStaticPoses(animationData: AnimationData, boneMapping: BoneMapping): List<Pose> {
        val poses = mutableListOf<Pose>()
        
        // Extract pose at time 0 (T-pose or bind pose)
        val bindPose = extractPoseAtTime(animationData, 0f, boneMapping)
        if (bindPose.hasBoneTransforms()) {
            poses.add(bindPose.copy(
                name = "${animationData.name}_bind",
                displayName = "${animationData.name} Bind Pose",
                category = Pose.PoseCategory.GENERAL
            ))
        }
        
        // Extract pose at mid-animation (if different from bind pose)
        val midTime = animationData.duration * 0.5f
        val midPose = extractPoseAtTime(animationData, midTime, boneMapping)
        if (midPose.hasBoneTransforms() && midPose != bindPose) {
            poses.add(midPose.copy(
                name = "${animationData.name}_mid",
                displayName = "${animationData.name} Mid Pose",
                category = Pose.PoseCategory.ACTION
            ))
        }
        
        return poses
    }
    
    /**
     * Extract pose at specific time from animation
     */
    private fun extractPoseAtTime(animationData: AnimationData, time: Float, boneMapping: BoneMapping): Pose {
        val boneTransforms = mutableMapOf<String, Transform>()
        
        animationData.channels.forEach { channel ->
            val transform = interpolateTransformAtTime(channel, time)
            if (transform != Transform.identity()) {
                boneTransforms[channel.boneName] = transform
            }
        }
        
        return Pose(
            name = "extracted_pose",
            boneTransforms = boneTransforms
        )
    }
    
    /**
     * Interpolate transform at specific time in animation channel
     */
    private fun interpolateTransformAtTime(channel: AnimationChannel, time: Float): Transform {
        if (channel.keyframes.isEmpty()) return Transform.identity()
        if (channel.keyframes.size == 1) return channel.keyframes[0].transform
        
        // Find keyframes to interpolate between
        var keyframe1: AnimationKeyframe? = null
        var keyframe2: AnimationKeyframe? = null
        
        for (i in 0 until channel.keyframes.size - 1) {
            val current = channel.keyframes[i]
            val next = channel.keyframes[i + 1]
            
            if (time >= current.time && time <= next.time) {
                keyframe1 = current
                keyframe2 = next
                break
            }
        }
        
        if (keyframe1 == null || keyframe2 == null) {
            // Return closest keyframe
            return channel.keyframes.minByOrNull { kotlin.math.abs(it.time - time) }?.transform ?: Transform.identity()
        }
        
        // Calculate interpolation factor
        val duration = keyframe2.time - keyframe1.time
        val t = if (duration > 0) (time - keyframe1.time) / duration else 0f
        
        // Interpolate based on channel type
        return when (channel.interpolation) {
            InterpolationType.STEP -> keyframe1.transform
            InterpolationType.LINEAR -> keyframe1.transform.lerp(keyframe2.transform, t)
            InterpolationType.CUBIC_SPLINE -> keyframe1.transform.lerp(keyframe2.transform, t) // Simplified
        }
    }
    
    /**
     * Load VRM-specific poses
     */
    private fun loadVRMPoses(vrmExtension: JsonObject?, boneMapping: BoneMapping): List<Pose> {
        // VRM doesn't typically define static poses, but we can create some defaults
        return emptyList()
    }
    
    /**
     * Create default poses
     */
    private fun createDefaultPoses(boneMapping: BoneMapping): List<Pose> {
        return listOf(
            Pose.tPose(),
            Pose.neutralStanding(),
            Pose.wave(),
            Pose.point()
        )
    }
    
    /**
     * Check if bone name is a humanoid bone
     */
    private fun isHumanoidBone(name: String): Boolean {
        return HUMANOID_BONES.any { humanoidBone ->
            name.contains(humanoidBone, ignoreCase = true)
        }
    }
    
    /**
     * Find root bone in bone hierarchy
     */
    private fun findRootBone(boneMap: Map<String, BoneInfo>): String? {
        // Look for common root bone names
        val rootCandidates = listOf("hips", "root", "pelvis", "spine")
        
        for (candidate in rootCandidates) {
            val found = boneMap.keys.find { it.contains(candidate, ignoreCase = true) }
            if (found != null) return found
        }
        
        // If no common root found, return first humanoid bone
        return boneMap.values.find { it.isHumanoidBone }?.name
    }
}

/**
 * Contains all pose and animation data
 */
data class PoseData(
    val poses: List<Pose>,
    val animations: List<AnimationData>,
    val boneMapping: BoneMapping,
    val staticPoses: List<Pose>,
    val loopingAnimations: List<Pose>
) {
    companion object {
        fun empty() = PoseData(
            poses = emptyList(),
            animations = emptyList(),
            boneMapping = BoneMapping.empty(),
            staticPoses = emptyList(),
            loopingAnimations = emptyList()
        )
    }
    
    /**
     * Get pose by name
     */
    fun getPose(name: String): Pose? = poses.find { it.name == name }
    
    /**
     * Get animation by name
     */
    fun getAnimation(name: String): AnimationData? = animations.find { it.name == name }
    
    /**
     * Check if pose data is empty
     */
    fun isEmpty(): Boolean = poses.isEmpty() && animations.isEmpty()
}

/**
 * Animation data
 */
data class AnimationData(
    val name: String,
    val channels: List<AnimationChannel>,
    val duration: Float,
    val isLooping: Boolean,
    val frameRate: Float
) {
    /**
     * Get channels for specific bone
     */
    fun getChannelsForBone(boneName: String): List<AnimationChannel> {
        return channels.filter { it.boneName == boneName }
    }
    
    /**
     * Get frame count
     */
    fun getFrameCount(): Int = (duration * frameRate).toInt()
}

/**
 * Animation channel
 */
data class AnimationChannel(
    val boneName: String,
    val path: String,
    val keyframes: List<AnimationKeyframe>,
    val interpolation: InterpolationType,
    val duration: Float
)

/**
 * Animation keyframe
 */
data class AnimationKeyframe(
    val time: Float,
    val transform: Transform
)

/**
 * Bone mapping information
 */
data class BoneMapping(
    val bones: Map<String, BoneInfo>,
    val humanoidBones: Map<String, BoneInfo>,
    val rootBone: String?
) {
    companion object {
        fun empty() = BoneMapping(
            bones = emptyMap(),
            humanoidBones = emptyMap(),
            rootBone = null
        )
    }
    
    /**
     * Get bone name by node index
     */
    fun getBoneNameByNodeIndex(nodeIndex: Int): String? {
        return bones.values.find { it.nodeIndex == nodeIndex }?.name
    }
    
    /**
     * Check if bone exists
     */
    fun hasBone(boneName: String): Boolean = bones.containsKey(boneName)
    
    /**
     * Get humanoid bone mapping
     */
    fun getHumanoidBone(humanoidName: String): BoneInfo? {
        return bones.values.find { it.humanoidName == humanoidName }
    }
    
    /**
     * Get all bone names
     */
    fun getAllBoneNames(): Set<String> = bones.keys
    
    /**
     * Get bone hierarchy (parent -> children mapping)
     */
    fun getBoneHierarchy(): Map<String, List<String>> {
        val hierarchy = mutableMapOf<String, MutableList<String>>()
        
        for (bone in bones.values) {
            val parentBone = bone.parentIndex?.let { parentIndex ->
                bones.values.find { it.nodeIndex == parentIndex }?.name
            }
            
            if (parentBone != null) {
                hierarchy.getOrPut(parentBone) { mutableListOf() }.add(bone.name)
            }
        }
        
        return hierarchy
    }
}

/**
 * Bone information
 */
data class BoneInfo(
    val name: String,
    val nodeIndex: Int,
    val transform: Transform,
    val isHumanoidBone: Boolean,
    val parentIndex: Int? = null,
    val humanoidName: String? = null
)

/**
 * Animation interpolation types
 */
enum class InterpolationType {
    LINEAR,
    STEP,
    CUBIC_SPLINE
}