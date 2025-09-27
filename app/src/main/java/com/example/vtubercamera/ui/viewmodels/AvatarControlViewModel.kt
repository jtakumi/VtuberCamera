package com.example.vtubercamera.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtubercamera.data.vrm.*
import com.example.vtubercamera.data.vrm.math.Transform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for avatar control functionality
 * Manages expression and pose state, integrates with controllers
 */
@HiltViewModel
class AvatarControlViewModel @Inject constructor(
    private val avatarController: AvatarController,
    private val expressionController: ExpressionController,
    private val poseController: PoseController
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AvatarControlUiState())
    val uiState: StateFlow<AvatarControlUiState> = _uiState.asStateFlow()
    
    init {
        // Observe avatar state changes
        viewModelScope.launch {
            avatarController.avatarState.collect { avatarState ->
                _uiState.value = _uiState.value.copy(
                    avatarModel = avatarState.model,
                    isAvatarLoading = avatarState.isLoading,
                    avatarLoadingProgress = avatarState.loadingProgress
                )
            }
        }
        
        // Observe expression state changes
        viewModelScope.launch {
            combine(
                expressionController.currentExpression,
                expressionController.activeBlendShapes,
                expressionController.isTransitioning,
                expressionController.transitionProgress
            ) { expression, blendShapes, isTransitioning, progress ->
                _uiState.value = _uiState.value.copy(
                    currentExpression = expression,
                    activeBlendShapes = blendShapes,
                    isExpressionTransitioning = isTransitioning,
                    expressionTransitionProgress = progress
                )
            }.collect()
        }
        
        // Observe pose state changes
        viewModelScope.launch {
            combine(
                poseController.currentPose,
                poseController.activeBoneTransforms,
                poseController.boneLocks,
                poseController.isTransitioning,
                poseController.transitionProgress
            ) { pose, transforms, locks, isTransitioning, progress ->
                _uiState.value = _uiState.value.copy(
                    currentPose = pose,
                    activeBoneTransforms = transforms,
                    boneLocks = locks,
                    isPoseTransitioning = isTransitioning,
                    poseTransitionProgress = progress
                )
            }.collect()
        }
    }
    
    // Expression Control Methods
    
    fun selectExpression(expression: Expression?) {
        if (_uiState.value.smoothTransitions && expression != null) {
            expressionController.transitionToExpression(expression)
        } else {
            expressionController.applyExpression(expression)
        }
    }
    
    fun setBlendShapeWeight(shapeName: String, weight: Float) {
        expressionController.setBlendShapeWeight(shapeName, weight)
    }
    
    fun clearExpression() {
        expressionController.clearExpression()
    }
    
    fun setExpressionTransitionDuration(duration: Float) {
        expressionController.setTransitionDuration(duration)
    }
    
    fun blendExpressions(expressionWeights: Map<Expression, Float>) {
        expressionController.blendExpressions(expressionWeights)
    }
    
    // Pose Control Methods
    
    fun selectPose(pose: Pose?) {
        if (_uiState.value.smoothTransitions && pose != null) {
            poseController.transitionToPose(pose)
        } else {
            poseController.applyPose(pose)
        }
    }
    
    fun setBoneTransform(boneName: String, transform: Transform) {
        poseController.setBoneTransform(boneName, transform)
    }
    
    fun toggleBoneLock(boneName: String) {
        if (poseController.isBoneLocked(boneName)) {
            poseController.unlockBone(boneName)
        } else {
            poseController.lockBone(boneName)
        }
    }
    
    fun clearPose() {
        poseController.clearPose()
    }
    
    fun resetToDefaultPose() {
        poseController.resetToDefaultPose()
    }
    
    fun setPoseTransitionDuration(duration: Float) {
        poseController.setTransitionDuration(duration)
    }
    
    fun blendPoses(poseWeights: Map<Pose, Float>) {
        poseController.blendPoses(poseWeights)
    }
    
    // Avatar Management Methods
    
    fun loadAvatar(vrmModel: VRMModel) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isAvatarLoading = true)
                
                // Load avatar into controller
                avatarController.loadModel(vrmModel)
                
                // Load expression and pose data
                loadExpressionData(vrmModel)
                loadPoseData(vrmModel)
                
                // Auto-reset if enabled
                if (_uiState.value.autoResetOnAvatarChange) {
                    clearExpression()
                    clearPose()
                }
                
            } catch (e: Exception) {
                // Handle error
                _uiState.value = _uiState.value.copy(
                    isAvatarLoading = false,
                    error = "Failed to load avatar: ${e.message}"
                )
            }
        }
    }
    
    // Settings Methods
    
    fun setAutoResetOnAvatarChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoResetOnAvatarChange = enabled)
    }
    
    fun setSmoothTransitions(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(smoothTransitions = enabled)
    }
    
    fun setUpdateFrequency(frequency: Int) {
        _uiState.value = _uiState.value.copy(updateFrequency = frequency)
    }
    
    fun setQualitySetting(quality: String) {
        _uiState.value = _uiState.value.copy(qualitySetting = quality)
    }
    
    // Update Methods (called from render loop)
    
    fun updateExpressionTransition(deltaTime: Float) {
        expressionController.updateTransition(deltaTime)
    }
    
    fun updatePoseTransition(deltaTime: Float) {
        poseController.updateTransition(deltaTime)
    }
    
    // Data Loading Methods
    
    private suspend fun loadExpressionData(vrmModel: VRMModel) {
        try {
            // Create expression data from VRM model
            val expressionData = ExpressionData(
                expressions = vrmModel.expressions,
                blendShapeGroups = groupExpressionsByCategory(vrmModel.expressions),
                presetExpressions = vrmModel.expressions.filter { isPresetExpression(it.name) },
                customExpressions = vrmModel.expressions.filter { !isPresetExpression(it.name) }
            )
            
            _uiState.value = _uiState.value.copy(expressionData = expressionData)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load expression data: ${e.message}"
            )
        }
    }
    
    private suspend fun loadPoseData(vrmModel: VRMModel) {
        try {
            // Create pose data from VRM model
            val poseData = PoseData(
                poses = vrmModel.poses,
                animations = emptyList(), // Animations would be loaded separately
                boneMapping = createBoneMappingFromModel(vrmModel),
                staticPoses = vrmModel.poses.filter { !it.isLooping },
                loopingAnimations = vrmModel.poses.filter { it.isLooping }
            )
            
            // Set bone mapping in pose controller
            poseController.setBoneMapping(poseData.boneMapping)
            
            _uiState.value = _uiState.value.copy(poseData = poseData)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Failed to load pose data: ${e.message}"
            )
        }
    }
    
    // Helper Methods
    
    private fun groupExpressionsByCategory(expressions: List<Expression>): List<BlendShapeGroup> {
        val groups = mutableListOf<BlendShapeGroup>()
        
        val emotionExpressions = expressions.filter { expr ->
            listOf("happy", "sad", "angry", "surprised", "relaxed").any { 
                expr.name.contains(it, ignoreCase = true) 
            }
        }
        
        val eyeExpressions = expressions.filter { expr ->
            listOf("blink", "look", "eye").any { 
                expr.name.contains(it, ignoreCase = true) 
            }
        }
        
        val mouthExpressions = expressions.filter { expr ->
            listOf("aa", "ih", "ou", "ee", "oh", "mouth").any { 
                expr.name.contains(it, ignoreCase = true) 
            }
        }
        
        val customExpressions = expressions.filter { expr ->
            !emotionExpressions.contains(expr) && 
            !eyeExpressions.contains(expr) && 
            !mouthExpressions.contains(expr)
        }
        
        if (emotionExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Emotions",
                expressions = emotionExpressions,
                category = BlendShapeCategory.EMOTION
            ))
        }
        
        if (eyeExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Eyes",
                expressions = eyeExpressions,
                category = BlendShapeCategory.EYE
            ))
        }
        
        if (mouthExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Mouth",
                expressions = mouthExpressions,
                category = BlendShapeCategory.MOUTH
            ))
        }
        
        if (customExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Custom",
                expressions = customExpressions,
                category = BlendShapeCategory.CUSTOM
            ))
        }
        
        return groups
    }
    
    private fun isPresetExpression(name: String): Boolean {
        val presetNames = setOf(
            "happy", "angry", "sad", "relaxed", "surprised",
            "aa", "ih", "ou", "ee", "oh",
            "blink", "blinkLeft", "blinkRight",
            "lookUp", "lookDown", "lookLeft", "lookRight",
            "neutral"
        )
        return presetNames.contains(name.lowercase())
    }
    
    private fun createBoneMappingFromModel(vrmModel: VRMModel): BoneMapping {
        // Create a basic bone mapping from VRM model metadata
        // In a real implementation, this would extract bone information from the model
        return BoneMapping.empty()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI state for avatar control screen
 */
data class AvatarControlUiState(
    // Avatar state
    val avatarModel: VRMModel? = null,
    val isAvatarLoading: Boolean = false,
    val avatarLoadingProgress: Float = 0f,
    
    // Expression state
    val expressionData: ExpressionData = ExpressionData.empty(),
    val currentExpression: Expression? = null,
    val activeBlendShapes: Map<String, Float> = emptyMap(),
    val isExpressionTransitioning: Boolean = false,
    val expressionTransitionProgress: Float = 0f,
    
    // Pose state
    val poseData: PoseData = PoseData.empty(),
    val currentPose: Pose? = null,
    val activeBoneTransforms: Map<String, Transform> = emptyMap(),
    val boneLocks: Set<String> = emptySet(),
    val isPoseTransitioning: Boolean = false,
    val poseTransitionProgress: Float = 0f,
    
    // Settings
    val autoResetOnAvatarChange: Boolean = true,
    val smoothTransitions: Boolean = true,
    val updateFrequency: Int = 30,
    val qualitySetting: String = "Medium",
    
    // Error state
    val error: String? = null
) {
    val hasError: Boolean get() = error != null
    val isLoading: Boolean get() = isAvatarLoading
}