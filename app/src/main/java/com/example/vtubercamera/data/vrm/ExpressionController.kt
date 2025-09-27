package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Vector3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller for applying VRM expression data to avatars
 * Handles blend shape application, material property changes, and expression transitions
 */
@Singleton
class ExpressionController @Inject constructor() {
    
    private val _currentExpression = MutableStateFlow<Expression?>(null)
    val currentExpression: StateFlow<Expression?> = _currentExpression.asStateFlow()
    
    private val _activeBlendShapes = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeBlendShapes: StateFlow<Map<String, Float>> = _activeBlendShapes.asStateFlow()
    
    private val _activeMaterialBindings = MutableStateFlow<Map<String, Expression.MaterialColorBinding>>(emptyMap())
    val activeMaterialBindings: StateFlow<Map<String, Expression.MaterialColorBinding>> = _activeMaterialBindings.asStateFlow()
    
    private val _activeTextureBindings = MutableStateFlow<Map<String, Expression.TextureTransformBinding>>(emptyMap())
    val activeTextureBindings: StateFlow<Map<String, Expression.TextureTransformBinding>> = _activeTextureBindings.asStateFlow()
    
    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()
    
    private val _transitionProgress = MutableStateFlow(0f)
    val transitionProgress: StateFlow<Float> = _transitionProgress.asStateFlow()
    
    // Configuration
    private var transitionDuration: Float = 0.3f // seconds
    private var currentTransitionTime: Float = 0f
    private var fromExpression: Expression? = null
    private var toExpression: Expression? = null
    
    /**
     * Apply expression immediately without transition
     * @param expression Expression to apply, null to clear
     */
    fun applyExpression(expression: Expression?) {
        _currentExpression.value = expression
        _isTransitioning.value = false
        _transitionProgress.value = 1f
        
        if (expression != null) {
            updateActiveStates(expression)
        } else {
            clearActiveStates()
        }
    }
    
    /**
     * Apply expression with smooth transition
     * @param expression Expression to transition to
     * @param duration Transition duration in seconds
     */
    fun transitionToExpression(expression: Expression?, duration: Float = transitionDuration) {
        if (expression == _currentExpression.value) return
        
        fromExpression = _currentExpression.value
        toExpression = expression
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
        
        // Apply interpolated expression
        val interpolatedExpression = interpolateExpressions(fromExpression, toExpression, progress)
        if (interpolatedExpression != null) {
            updateActiveStates(interpolatedExpression)
        } else {
            clearActiveStates()
        }
        
        // Complete transition
        if (progress >= 1f) {
            _isTransitioning.value = false
            _currentExpression.value = toExpression
            fromExpression = null
            toExpression = null
        }
    }
    
    /**
     * Blend multiple expressions with weights
     * @param expressionWeights Map of expressions to their weights (0.0 to 1.0)
     */
    fun blendExpressions(expressionWeights: Map<Expression, Float>) {
        if (expressionWeights.isEmpty()) {
            clearActiveStates()
            _currentExpression.value = null
            return
        }
        
        // Normalize weights
        val totalWeight = expressionWeights.values.sum()
        val normalizedWeights = if (totalWeight > 0f) {
            expressionWeights.mapValues { (_, weight) -> weight / totalWeight }
        } else {
            expressionWeights
        }
        
        // Blend blend shapes
        val blendedShapes = mutableMapOf<String, Float>()
        val blendedMaterialBindings = mutableMapOf<String, Expression.MaterialColorBinding>()
        val blendedTextureBindings = mutableMapOf<String, Expression.TextureTransformBinding>()
        
        for ((expression, weight) in normalizedWeights) {
            // Blend shape keys
            for ((shapeName, shapeWeight) in expression.blendShapeKeys) {
                blendedShapes[shapeName] = (blendedShapes[shapeName] ?: 0f) + (shapeWeight * weight)
            }
            
            // Material bindings (use highest weight for non-additive properties)
            for ((materialName, binding) in expression.materialColorBindings) {
                val existingBinding = blendedMaterialBindings[materialName]
                if (existingBinding == null || weight > (normalizedWeights.entries.find { 
                    it.key.materialColorBindings.containsKey(materialName) 
                }?.value ?: 0f)) {
                    blendedMaterialBindings[materialName] = binding
                }
            }
            
            // Texture bindings (use highest weight)
            for ((materialName, binding) in expression.textureTransformBindings) {
                val existingBinding = blendedTextureBindings[materialName]
                if (existingBinding == null || weight > (normalizedWeights.entries.find { 
                    it.key.textureTransformBindings.containsKey(materialName) 
                }?.value ?: 0f)) {
                    blendedTextureBindings[materialName] = binding
                }
            }
        }
        
        // Clamp blend shape values
        val clampedShapes = blendedShapes.mapValues { (_, value) -> value.coerceIn(0f, 1f) }
        
        // Update active states
        _activeBlendShapes.value = clampedShapes
        _activeMaterialBindings.value = blendedMaterialBindings
        _activeTextureBindings.value = blendedTextureBindings
        
        // Create representative expression for current state
        val dominantExpression = normalizedWeights.maxByOrNull { it.value }?.key
        _currentExpression.value = dominantExpression
    }
    
    /**
     * Apply specific blend shape weights
     * @param blendShapes Map of blend shape names to weights
     */
    fun applyBlendShapes(blendShapes: Map<String, Float>) {
        val clampedShapes = blendShapes.mapValues { (_, value) -> value.coerceIn(0f, 1f) }
        _activeBlendShapes.value = clampedShapes
        _currentExpression.value = null // Mark as custom blend
    }
    
    /**
     * Get current blend shape weight
     * @param shapeName Name of the blend shape
     * @return Current weight (0.0 to 1.0)
     */
    fun getBlendShapeWeight(shapeName: String): Float {
        return _activeBlendShapes.value[shapeName] ?: 0f
    }
    
    /**
     * Set individual blend shape weight
     * @param shapeName Name of the blend shape
     * @param weight Weight value (0.0 to 1.0)
     */
    fun setBlendShapeWeight(shapeName: String, weight: Float) {
        val currentShapes = _activeBlendShapes.value.toMutableMap()
        currentShapes[shapeName] = weight.coerceIn(0f, 1f)
        _activeBlendShapes.value = currentShapes
        _currentExpression.value = null // Mark as custom blend
    }
    
    /**
     * Clear all active expressions and return to neutral
     */
    fun clearExpression() {
        applyExpression(null)
    }
    
    /**
     * Check if expression has override conflicts
     * @param expression Expression to check
     * @return List of override conflicts
     */
    fun checkOverrideConflicts(expression: Expression): List<String> {
        val conflicts = mutableListOf<String>()
        
        val currentExpr = _currentExpression.value
        if (currentExpr != null) {
            if (expression.overrideBlink != Expression.OverrideType.NONE && 
                currentExpr.overrideBlink != Expression.OverrideType.NONE) {
                conflicts.add("Blink override conflict")
            }
            
            if (expression.overrideLookAt != Expression.OverrideType.NONE && 
                currentExpr.overrideLookAt != Expression.OverrideType.NONE) {
                conflicts.add("LookAt override conflict")
            }
            
            if (expression.overrideMouth != Expression.OverrideType.NONE && 
                currentExpr.overrideMouth != Expression.OverrideType.NONE) {
                conflicts.add("Mouth override conflict")
            }
        }
        
        return conflicts
    }
    
    /**
     * Get expression application data for rendering engine
     */
    fun getExpressionRenderData(): ExpressionRenderData {
        return ExpressionRenderData(
            blendShapes = _activeBlendShapes.value,
            materialBindings = _activeMaterialBindings.value,
            textureBindings = _activeTextureBindings.value,
            isTransitioning = _isTransitioning.value,
            transitionProgress = _transitionProgress.value
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
     * Get available blend shape names from current model
     */
    fun getAvailableBlendShapes(): Set<String> {
        return _activeBlendShapes.value.keys
    }
    
    private fun updateActiveStates(expression: Expression) {
        _activeBlendShapes.value = expression.blendShapeKeys
        _activeMaterialBindings.value = expression.materialColorBindings
        _activeTextureBindings.value = expression.textureTransformBindings
    }
    
    private fun clearActiveStates() {
        _activeBlendShapes.value = emptyMap()
        _activeMaterialBindings.value = emptyMap()
        _activeTextureBindings.value = emptyMap()
    }
    
    private fun interpolateExpressions(from: Expression?, to: Expression?, progress: Float): Expression? {
        if (from == null && to == null) return null
        if (from == null) return to?.withScaledWeights(progress)
        if (to == null) return from.withScaledWeights(1f - progress)
        
        return from.blendWith(to, progress)
    }
}

/**
 * Data class for expression render data
 */
data class ExpressionRenderData(
    val blendShapes: Map<String, Float>,
    val materialBindings: Map<String, Expression.MaterialColorBinding>,
    val textureBindings: Map<String, Expression.TextureTransformBinding>,
    val isTransitioning: Boolean,
    val transitionProgress: Float
) {
    companion object {
        fun empty() = ExpressionRenderData(
            blendShapes = emptyMap(),
            materialBindings = emptyMap(),
            textureBindings = emptyMap(),
            isTransitioning = false,
            transitionProgress = 0f
        )
    }
    
    /**
     * Check if any expression data is active
     */
    fun hasActiveExpression(): Boolean = blendShapes.isNotEmpty() || 
                                        materialBindings.isNotEmpty() || 
                                        textureBindings.isNotEmpty()
    
    /**
     * Get total blend shape intensity
     */
    fun getTotalIntensity(): Float = blendShapes.values.sum()
}