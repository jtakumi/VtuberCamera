package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Transform

/**
 * Represents the current state of an avatar in the AR scene
 * 
 * @param model The loaded VRM model, null if no avatar is loaded
 * @param transform Current position, rotation, and scale of the avatar
 * @param currentExpression Currently applied facial expression
 * @param currentPose Currently applied pose
 * @param isVisible Whether the avatar is currently visible in the scene
 * @param isLoading Whether an avatar is currently being loaded
 * @param loadingProgress Loading progress from 0.0 to 1.0
 */
data class AvatarState(
    val model: VRMModel? = null,
    val transform: Transform = Transform.identity(),
    val currentExpression: Expression? = null,
    val currentPose: Pose? = null,
    val isVisible: Boolean = true,
    val isLoading: Boolean = false,
    val loadingProgress: Float = 0.0f
) {
    /**
     * Returns true if an avatar is loaded and ready for display
     */
    val isReady: Boolean
        get() = model != null && !isLoading
    
    /**
     * Returns true if the avatar should be rendered
     */
    val shouldRender: Boolean
        get() = isReady && isVisible
}