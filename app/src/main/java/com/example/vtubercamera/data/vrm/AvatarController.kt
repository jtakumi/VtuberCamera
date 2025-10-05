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
 * Controller for managing avatar transformations, expressions, and poses in AR space.
 * 
 * This class provides a comprehensive interface for manipulating VRM avatars including:
 * - 3D transformations (position, rotation, scale) with safety limits
 * - Expression and pose management
 * - Gesture-based manipulation support
 * - Smooth interpolation and blending
 * - State management with reactive updates
 * - Loading state tracking
 * 
 * The controller maintains avatar state through StateFlow for reactive UI updates
 * and applies safety constraints to prevent invalid transformations.
 * 
 * All transformation operations are applied with appropriate limits:
 * - Position: ±5 units in each axis
 * - Scale: 0.1x to 3.0x
 * - Rotation: No limits (full 360° rotation)
 * 
 * Usage example:
 * ```kotlin
 * // Load an avatar
 * avatarController.loadModel(vrmModel)
 * 
 * // Apply transformations
 * avatarController.updatePosition(1.0f, 0.0f, -2.0f)
 * avatarController.setScale(1.5f)
 * 
 * // Apply expressions
 * avatarController.setExpression(happyExpression)
 * 
 * // Observe state changes
 * avatarController.avatarState.collect { state ->
 *     // Update UI or renderer
 * }
 * ```
 * 
 * @author VTuber Camera Team
 * @since 1.0.0
 * 
 * @see AvatarState
 * @see Transform
 * @see Expression
 * @see Pose
 */
@Singleton
class AvatarController @Inject constructor() {
    
    private val _avatarState = MutableStateFlow(AvatarState())
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()
    
    // Transform limits for safe avatar manipulation
    private val positionLimits = Vector3(5f, 5f, 5f) // ±5 units in each direction
    private val minScale = 0.1f
    private val maxScale = 3.0f
    
    /**
     * Updates avatar position by delta values with safety constraints.
     * 
     * The position is updated incrementally and clamped to safe limits
     * to prevent the avatar from moving too far from the origin.
     * 
     * @param deltaX Movement delta in X axis (right/left)
     * @param deltaY Movement delta in Y axis (up/down)  
     * @param deltaZ Movement delta in Z axis (forward/backward)
     * 
     * @see positionLimits for the applied constraints
     */
    fun updatePosition(deltaX: Float, deltaY: Float, deltaZ: Float) {
        val currentState = _avatarState.value
        val currentPosition = currentState.transform.position
        
        val newPosition = Vector3(
            (currentPosition.x + deltaX).coerceIn(-positionLimits.x, positionLimits.x),
            (currentPosition.y + deltaY).coerceIn(-positionLimits.y, positionLimits.y),
            (currentPosition.z + deltaZ).coerceIn(-positionLimits.z, positionLimits.z)
        )
        
        val newTransform = currentState.transform.withPosition(newPosition)
        _avatarState.value = currentState.copy(transform = newTransform)
    }
    
    /**
     * Set absolute position
     * @param position New position vector
     */
    fun setPosition(position: Vector3) {
        val currentState = _avatarState.value
        val clampedPosition = Vector3(
            position.x.coerceIn(-positionLimits.x, positionLimits.x),
            position.y.coerceIn(-positionLimits.y, positionLimits.y),
            position.z.coerceIn(-positionLimits.z, positionLimits.z)
        )
        
        val newTransform = currentState.transform.withPosition(clampedPosition)
        _avatarState.value = currentState.copy(transform = newTransform)
    }
    
    /**
     * Update avatar rotation by delta values (in radians)
     * @param deltaYaw Rotation around Y axis
     * @param deltaPitch Rotation around X axis
     * @param deltaRoll Rotation around Z axis
     */
    fun updateRotation(deltaYaw: Float, deltaPitch: Float, deltaRoll: Float) {
        val currentState = _avatarState.value
        val deltaRotation = Quaternion.fromEuler(deltaPitch, deltaYaw, deltaRoll)
        val newRotation = currentState.transform.rotation * deltaRotation
        
        val newTransform = currentState.transform.withRotation(newRotation)
        _avatarState.value = currentState.copy(transform = newTransform)
    }
    
    /**
     * Set absolute rotation
     * @param rotation New rotation quaternion
     */
    fun setRotation(rotation: Quaternion) {
        val currentState = _avatarState.value
        val newTransform = currentState.transform.withRotation(rotation)
        _avatarState.value = currentState.copy(transform = newTransform)
    }
    
    /**
     * Updates avatar scale by multiplication factor with safety constraints.
     * 
     * The scale is applied uniformly to all axes and clamped to prevent
     * the avatar from becoming too small or too large.
     * 
     * @param scaleFactor Multiplication factor for current scale (e.g., 1.1 for 10% larger)
     * 
     * @see minScale Minimum allowed scale (0.1x)
     * @see maxScale Maximum allowed scale (3.0x)
     */
    fun updateScale(scaleFactor: Float) {
        val currentState = _avatarState.value
        val currentScale = currentState.transform.scale
        val uniformCurrentScale = (currentScale.x + currentScale.y + currentScale.z) / 3f
        val newUniformScale = (uniformCurrentScale * scaleFactor).coerceIn(minScale, maxScale)
        val newScale = Vector3(newUniformScale, newUniformScale, newUniformScale)
        
        val newTransform = currentState.transform.withScale(newScale)
        _avatarState.value = currentState.copy(transform = newTransform)
    }
    
    /**
     * Set absolute uniform scale
     * @param scale New uniform scale value
     */
    fun setScale(scale: Float) {
        val currentState = _avatarState.value
        val clampedScale = scale.coerceIn(minScale, maxScale)
        val newScale = Vector3(clampedScale, clampedScale, clampedScale)
        
        val newTransform = currentState.transform.withScale(newScale)
        _avatarState.value = currentState.copy(transform = newTransform)
    }
    
    /**
     * Set non-uniform scale
     * @param scaleVector Scale values for X, Y, Z axes
     */
    fun setScale(scaleVector: Vector3) {
        val currentState = _avatarState.value
        val clampedScale = Vector3(
            scaleVector.x.coerceIn(minScale, maxScale),
            scaleVector.y.coerceIn(minScale, maxScale),
            scaleVector.z.coerceIn(minScale, maxScale)
        )

        val newTransform = currentState.transform.withScale(clampedScale)
        _avatarState.value = currentState.copy(transform = newTransform)
    }

    /**
     * Set complete transform
     * @param transform New transform to apply
     */
    fun setTransform(transform: Transform) {
        val currentState = _avatarState.value

        // Apply limits to the transform components
        val clampedPosition = Vector3(
            transform.position.x.coerceIn(-positionLimits.x, positionLimits.x),
            transform.position.y.coerceIn(-positionLimits.y, positionLimits.y),
            transform.position.z.coerceIn(-positionLimits.z, positionLimits.z)
        )

        val clampedScale = Vector3(
            transform.scale.x.coerceIn(minScale, maxScale),
            transform.scale.y.coerceIn(minScale, maxScale),
            transform.scale.z.coerceIn(minScale, maxScale)
        )

        val clampedTransform = Transform(
            position = clampedPosition,
            rotation = transform.rotation, // Rotation doesn't need clamping
            scale = clampedScale
        )

        _avatarState.value = currentState.copy(transform = clampedTransform)
    }
    
    /**
     * Apply expression to avatar
     * @param expression Expression to apply
     */
    fun setExpression(expression: Expression) {
        val currentState = _avatarState.value
        _avatarState.value = currentState.copy(currentExpression = expression)
    }
    
    /**
     * Apply pose to avatar
     * @param pose Pose to apply
     */
    fun setPose(pose: Pose) {
        val currentState = _avatarState.value
        _avatarState.value = currentState.copy(currentPose = pose)
    }
    
    /**
     * Clear current expression (return to neutral)
     */
    fun clearExpression() {
        val currentState = _avatarState.value
        _avatarState.value = currentState.copy(currentExpression = null)
    }
    
    /**
     * Clear current pose (return to default)
     */
    fun clearPose() {
        val currentState = _avatarState.value
        _avatarState.value = currentState.copy(currentPose = null)
    }
    
    /**
     * Set avatar visibility
     * @param visible Whether avatar should be visible
     */
    fun setVisible(visible: Boolean) {
        val currentState = _avatarState.value
        _avatarState.value = currentState.copy(isVisible = visible)
    }
    
    /**
     * Load a VRM model into the controller
     * @param model VRM model to load
     */
    fun loadModel(model: VRMModel) {
        _avatarState.value = AvatarState(
            model = model,
            transform = Transform.identity(),
            currentExpression = null,
            currentPose = null,
            isVisible = true,
            isLoading = false,
            loadingProgress = 1.0f
        )
    }
    
    /**
     * Update loading state
     * @param isLoading Whether model is currently loading
     * @param progress Loading progress (0.0 to 1.0)
     */
    fun updateLoadingState(isLoading: Boolean, progress: Float = 0.0f) {
        val currentState = _avatarState.value
        _avatarState.value = currentState.copy(
            isLoading = isLoading,
            loadingProgress = progress.coerceIn(0.0f, 1.0f)
        )
    }
    
    /**
     * Reset avatar to default state
     */
    fun resetToDefault() {
        val currentState = _avatarState.value
        _avatarState.value = currentState.copy(
            transform = Transform.identity(),
            currentExpression = null,
            currentPose = null,
            isVisible = true
        )
    }
    
    /**
     * Get current transform
     */
    fun getCurrentTransform(): Transform = _avatarState.value.transform
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Vector3 = _avatarState.value.transform.position
    
    /**
     * Get current rotation
     */
    fun getCurrentRotation(): Quaternion = _avatarState.value.transform.rotation
    
    /**
     * Get current scale
     */
    fun getCurrentScale(): Vector3 = _avatarState.value.transform.scale
    
    /**
     * Get current expression
     */
    fun getCurrentExpression(): Expression? = _avatarState.value.currentExpression
    
    /**
     * Get current pose
     */
    fun getCurrentPose(): Pose? = _avatarState.value.currentPose
    
    /**
     * Check if avatar is ready for rendering
     */
    fun isReady(): Boolean = _avatarState.value.isReady
    
    /**
     * Check if avatar should be rendered
     */
    fun shouldRender(): Boolean = _avatarState.value.shouldRender
    
    /**
     * Apply transform interpolation for smooth animations
     * @param targetTransform Target transform to interpolate to
     * @param speed Interpolation speed (0.0 to 1.0)
     */
    fun interpolateToTransform(targetTransform: Transform, speed: Float) {
        val currentState = _avatarState.value
        val clampedSpeed = speed.coerceIn(0.0f, 1.0f)
        val newTransform = currentState.transform.lerp(targetTransform, clampedSpeed)
        
        _avatarState.value = currentState.copy(transform = newTransform)
    }
    
    /**
     * Blend expressions with weights
     * @param expressions Map of expressions to their weights
     */
    fun blendExpressions(expressions: Map<Expression, Float>) {
        if (expressions.isEmpty()) {
            clearExpression()
            return
        }
        
        val totalWeight = expressions.values.sum()
        if (totalWeight == 0f) {
            clearExpression()
            return
        }
        
        // Normalize weights
        val normalizedExpressions = expressions.mapValues { (_, weight) -> weight / totalWeight }
        
        // Create blended expression
        var blendedExpression: Expression? = null
        for ((expression, weight) in normalizedExpressions) {
            blendedExpression = blendedExpression?.blendWith(expression, weight) ?: expression.withScaledWeights(weight)
        }
        
        blendedExpression?.let { setExpression(it) }
    }
    
    /**
     * Apply gesture-based manipulation
     * @param gestureType Type of gesture (pan, pinch, rotate)
     * @param deltaX X delta from gesture
     * @param deltaY Y delta from gesture
     * @param scale Scale factor for pinch gestures
     */
    fun applyGesture(gestureType: GestureType, deltaX: Float = 0f, deltaY: Float = 0f, scale: Float = 1f) {
        when (gestureType) {
            GestureType.PAN -> {
                // Convert screen coordinates to world coordinates
                val worldDelta = Vector3(deltaX * 0.01f, -deltaY * 0.01f, 0f)
                updatePosition(worldDelta.x, worldDelta.y, worldDelta.z)
            }
            GestureType.PINCH -> {
                updateScale(scale)
            }
            GestureType.ROTATE -> {
                // Convert screen delta to rotation
                val rotationSensitivity = 0.01f
                updateRotation(deltaX * rotationSensitivity, deltaY * rotationSensitivity, 0f)
            }
        }
    }
    
    /**
     * Types of gestures for avatar manipulation
     */
    enum class GestureType {
        PAN,    // Move avatar
        PINCH,  // Scale avatar
        ROTATE  // Rotate avatar
    }
}