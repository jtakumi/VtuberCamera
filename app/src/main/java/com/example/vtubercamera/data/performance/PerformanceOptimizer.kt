package com.example.vtubercamera.data.performance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * パフォーマンス最適化を自動実行するクラス
 */
@Singleton
class PerformanceOptimizer @Inject constructor() {
    
    private val _settings = MutableStateFlow(PerformanceSettings())
    val settings: StateFlow<PerformanceSettings> = _settings.asStateFlow()
    
    private val _optimizationHistory = MutableStateFlow<List<OptimizationHistoryEntry>>(emptyList())
    val optimizationHistory: StateFlow<List<OptimizationHistoryEntry>> = _optimizationHistory.asStateFlow()
    
    /**
     * パフォーマンス状態に基づいて自動最適化を実行
     */
    fun applyOptimizations(
        performanceState: PerformanceState,
        onTextureQualityChange: (QualityLevel) -> Unit,
        onRenderQualityChange: (QualityLevel) -> Unit,
        onFrameRateChange: (Float) -> Unit,
        onCacheClear: () -> Unit,
        onEffectsToggle: (Boolean) -> Unit,
        onThermalThrottling: (Boolean) -> Unit
    ) {
        if (!_settings.value.autoOptimization) return
        
        val actions = mutableListOf<OptimizationAction>()
        
        performanceState.recommendedOptimizations.forEach { recommendation ->
            when (recommendation) {
                OptimizationRecommendation.REDUCE_TEXTURE_QUALITY -> {
                    val newQuality = reduceQualityLevel(_settings.value.qualityLevel)
                    onTextureQualityChange(newQuality)
                    actions.add(OptimizationAction.TEXTURE_QUALITY_REDUCED)
                }
                
                OptimizationRecommendation.REDUCE_RENDER_QUALITY -> {
                    val newQuality = reduceQualityLevel(_settings.value.qualityLevel)
                    onRenderQualityChange(newQuality)
                    actions.add(OptimizationAction.RENDER_QUALITY_REDUCED)
                }
                
                OptimizationRecommendation.CLEAR_CACHE -> {
                    onCacheClear()
                    actions.add(OptimizationAction.CACHE_CLEARED)
                }
                
                OptimizationRecommendation.LOWER_FRAME_RATE -> {
                    val newFrameRate = maxOf(15f, _settings.value.targetFrameRate - 5f)
                    onFrameRateChange(newFrameRate)
                    updateSettings(_settings.value.copy(targetFrameRate = newFrameRate))
                    actions.add(OptimizationAction.FRAME_RATE_LOWERED)
                }
                
                OptimizationRecommendation.DISABLE_EFFECTS -> {
                    onEffectsToggle(false)
                    actions.add(OptimizationAction.EFFECTS_DISABLED)
                }
                
                OptimizationRecommendation.THERMAL_THROTTLING -> {
                    if (_settings.value.thermalThrottlingEnabled) {
                        onThermalThrottling(true)
                        actions.add(OptimizationAction.THERMAL_THROTTLING_ENABLED)
                    }
                }
                
                OptimizationRecommendation.REDUCE_BRIGHTNESS -> {
                    // ブライトネス調整は外部で処理
                    actions.add(OptimizationAction.BRIGHTNESS_SUGGESTION)
                }
            }
        }
        
        if (actions.isNotEmpty()) {
            addOptimizationHistory(actions)
        }
    }
    
    /**
     * 品質レベルを一段階下げる
     */
    private fun reduceQualityLevel(currentLevel: QualityLevel): QualityLevel {
        return when (currentLevel) {
            QualityLevel.ULTRA -> QualityLevel.HIGH
            QualityLevel.HIGH -> QualityLevel.MEDIUM
            QualityLevel.MEDIUM -> QualityLevel.LOW
            QualityLevel.LOW -> QualityLevel.LOW
        }
    }
    
    /**
     * 品質レベルを一段階上げる
     */
    private fun increaseQualityLevel(currentLevel: QualityLevel): QualityLevel {
        return when (currentLevel) {
            QualityLevel.LOW -> QualityLevel.MEDIUM
            QualityLevel.MEDIUM -> QualityLevel.HIGH
            QualityLevel.HIGH -> QualityLevel.ULTRA
            QualityLevel.ULTRA -> QualityLevel.ULTRA
        }
    }
    
    /**
     * パフォーマンスが改善された場合の品質復元
     */
    fun restoreQualityIfPossible(
        performanceState: PerformanceState,
        onTextureQualityChange: (QualityLevel) -> Unit,
        onRenderQualityChange: (QualityLevel) -> Unit,
        onFrameRateChange: (Float) -> Unit
    ) {
        if (!_settings.value.autoOptimization) return
        
        // パフォーマンスが良好な場合、品質を段階的に復元
        if (performanceState.overallHealth == PerformanceHealth.EXCELLENT ||
            performanceState.overallHealth == PerformanceHealth.GOOD) {
            
            val currentQuality = _settings.value.qualityLevel
            if (currentQuality != QualityLevel.ULTRA) {
                val improvedQuality = increaseQualityLevel(currentQuality)
                onTextureQualityChange(improvedQuality)
                onRenderQualityChange(improvedQuality)
                updateSettings(_settings.value.copy(qualityLevel = improvedQuality))
            }
            
            // フレームレートも復元
            if (_settings.value.targetFrameRate < 30f) {
                val improvedFrameRate = minOf(30f, _settings.value.targetFrameRate + 5f)
                onFrameRateChange(improvedFrameRate)
                updateSettings(_settings.value.copy(targetFrameRate = improvedFrameRate))
            }
        }
    }
    
    /**
     * 設定を更新
     */
    fun updateSettings(newSettings: PerformanceSettings) {
        _settings.value = newSettings
    }
    
    /**
     * 最適化履歴を追加
     */
    private fun addOptimizationHistory(actions: List<OptimizationAction>) {
        val currentHistory = _optimizationHistory.value.toMutableList()
        val timestamp = System.currentTimeMillis()
        
        actions.forEach { action ->
            currentHistory.add(0, OptimizationHistoryEntry(action, timestamp))
        }
        
        // 履歴は最新100件まで保持
        _optimizationHistory.value = currentHistory.take(100)
    }
    
    /**
     * 最適化履歴をクリア
     */
    fun clearOptimizationHistory() {
        _optimizationHistory.value = emptyList()
    }
}

/**
 * 最適化アクション
 */
enum class OptimizationAction {
    TEXTURE_QUALITY_REDUCED,
    RENDER_QUALITY_REDUCED,
    CACHE_CLEARED,
    FRAME_RATE_LOWERED,
    EFFECTS_DISABLED,
    THERMAL_THROTTLING_ENABLED,
    BRIGHTNESS_SUGGESTION
}

/**
 * 最適化履歴エントリ
 */
data class OptimizationHistoryEntry(
    val action: OptimizationAction,
    val timestamp: Long
)