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
 * Controller for managing avatar transformations, expressions, and poses
 * Provides basic operations for manipulating VRM avatars in AR space
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
     * Update avatar position by delta values
     * @param deltaX Movement in X axis
     * @param deltaY Movement in Y axis  
     * @param deltaZ Movement in Z axis
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
     * Update avatar scale by factor
     * @param scaleFactor Multiplication factor for current scale
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
            blendedExpression = if (blendedExpression == null) {
                expression.withScaledWeights(weight)
            } else {
                blendedExpression.blendWith(expression, weight)
            }
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