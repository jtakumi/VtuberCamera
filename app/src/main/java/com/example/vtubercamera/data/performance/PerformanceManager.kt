package com.example.vtubercamera.data.performance

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * パフォーマンス監視と最適化を統合管理するクラス
 */
@Singleton
class PerformanceManager @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
    private val performanceOptimizer: PerformanceOptimizer,
    private val batteryMonitor: BatteryMonitor,
    private val frameRateMonitor: FrameRateMonitor
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitoringJob: Job? = null
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _overallStatus = MutableStateFlow(OverallPerformanceStatus())
    val overallStatus: StateFlow<OverallPerformanceStatus> = _overallStatus.asStateFlow()
    
    // 最適化コールバック
    private var optimizationCallbacks: OptimizationCallbacks? = null
    
    /**
     * パフォーマンス監視を開始
     */
    fun startMonitoring(
        targetFrameRate: Float = 30f,
        callbacks: OptimizationCallbacks
    ) {
        if (_isMonitoring.value) return
        
        optimizationCallbacks = callbacks
        _isMonitoring.value = true
        
        // 各監視システムを開始
        batteryMonitor.startMonitoring()
        frameRateMonitor.startMonitoring(targetFrameRate)
        
        // 定期的な監視ジョブを開始
        monitoringJob = scope.launch {
            while (_isMonitoring.value) {
                updatePerformanceStatus()
                delay(1000) // 1秒間隔で更新
            }
        }
        
        // 状態変化の監視
        scope.launch {
            combine(
                performanceMonitor.memoryState,
                frameRateMonitor.frameRateData,
                batteryMonitor.batteryInfo,
                frameRateMonitor.qualityAdjustment
            ) { memory, frameRate, battery, qualityAdjustment ->
                
                // 自動最適化の実行
                if (performanceOptimizer.settings.value.autoOptimization) {
                    handleAutomaticOptimization(memory, frameRate, battery, qualityAdjustment)
                }
                
                // 総合ステータスの更新
                updateOverallStatus(memory, frameRate, battery)
            }
        }
    }
    
    /**
     * パフォーマンス監視を停止
     */
    fun stopMonitoring() {
        if (!_isMonitoring.value) return
        
        _isMonitoring.value = false
        monitoringJob?.cancel()
        
        batteryMonitor.stopMonitoring()
        frameRateMonitor.stopMonitoring()
    }
    
    /**
     * パフォーマンス状態を更新
     */
    private suspend fun updatePerformanceStatus() {
        performanceMonitor.updateMemoryState()
        
        val batteryInfo = batteryMonitor.batteryInfo.value
        performanceMonitor.updateBatteryState(
            batteryInfo.level,
            batteryInfo.isCharging,
            batteryInfo.temperature
        )
        
        val frameRateData = frameRateMonitor.frameRateData.value
        performanceMonitor.updateFrameRate(frameRateData.currentFps)
    }
    
    /**
     * 自動最適化を処理
     */
    private fun handleAutomaticOptimization(
        memory: MemoryState,
        frameRate: FrameRateData,
        battery: BatteryInfo,
        qualityAdjustment: QualityAdjustment
    ) {
        val callbacks = optimizationCallbacks ?: return
        
        // フレームレート基準の品質調整
        if (qualityAdjustment.shouldReduceQuality) {
            callbacks.onQualityReductionNeeded(qualityAdjustment.reason)
        } else if (qualityAdjustment.shouldIncreaseQuality) {
            callbacks.onQualityImprovementPossible()
        }
        
        // メモリ基準の最適化
        if (memory.shouldOptimize) {
            callbacks.onMemoryOptimizationNeeded(memory.usagePercent)
        }
        
        // バッテリー基準の最適化
        if (battery.isOverheating) {
            callbacks.onThermalThrottlingNeeded(battery.temperature)
        }
        
        if (battery.isLowBattery && !battery.isCharging) {
            callbacks.onBatteryOptimizationNeeded(battery.level)
        }
        
        // 総合的な最適化判定
        val performanceState = performanceMonitor.getOverallPerformanceState()
        if (performanceState.shouldOptimize) {
            performanceOptimizer.applyOptimizations(
                performanceState,
                onTextureQualityChange = callbacks::onTextureQualityChange,
                onRenderQualityChange = callbacks::onRenderQualityChange,
                onFrameRateChange = callbacks::onFrameRateChange,
                onCacheClear = callbacks::onCacheClear,
                onEffectsToggle = callbacks::onEffectsToggle,
                onThermalThrottling = callbacks::onThermalThrottling
            )
        } else if (performanceState.overallHealth == PerformanceHealth.EXCELLENT) {
            // パフォーマンスが良好な場合は品質を復元
            performanceOptimizer.restoreQualityIfPossible(
                performanceState,
                onTextureQualityChange = callbacks::onTextureQualityChange,
                onRenderQualityChange = callbacks::onRenderQualityChange,
                onFrameRateChange = callbacks::onFrameRateChange
            )
        }
    }
    
    /**
     * 総合ステータスを更新
     */
    private fun updateOverallStatus(
        memory: MemoryState,
        frameRate: FrameRateData,
        battery: BatteryInfo
    ) {
        val performanceScore = calculatePerformanceScore(memory, frameRate, battery)
        val recommendations = generateRecommendations(memory, frameRate, battery)
        val alerts = generateAlerts(memory, frameRate, battery)
        
        _overallStatus.value = OverallPerformanceStatus(
            performanceScore = performanceScore,
            memoryUsagePercent = memory.usagePercent,
            currentFps = frameRate.currentFps,
            batteryLevel = battery.level,
            batteryTemperature = battery.temperature,
            isOptimizationActive = performanceOptimizer.settings.value.autoOptimization,
            recommendations = recommendations,
            alerts = alerts,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * パフォーマンススコアを計算
     */
    private fun calculatePerformanceScore(
        memory: MemoryState,
        frameRate: FrameRateData,
        battery: BatteryInfo
    ): Int {
        val memoryScore = when {
            memory.usagePercent < 50 -> 100
            memory.usagePercent < 70 -> 80
            memory.usagePercent < 85 -> 60
            else -> 30
        }
        
        val frameRateScore = when {
            frameRate.averageFps >= 28f -> 100
            frameRate.averageFps >= 24f -> 80
            frameRate.averageFps >= 20f -> 60
            else -> 30
        }
        
        val batteryScore = when {
            battery.level > 50 && !battery.isOverheating -> 100
            battery.level > 30 && !battery.isOverheating -> 80
            battery.level > 15 || battery.isCharging -> 60
            else -> 30
        }
        
        return (memoryScore + frameRateScore + batteryScore) / 3
    }
    
    /**
     * 推奨事項を生成
     */
    private fun generateRecommendations(
        memory: MemoryState,
        frameRate: FrameRateData,
        battery: BatteryInfo
    ): List<PerformanceRecommendation> {
        val recommendations = mutableListOf<PerformanceRecommendation>()
        
        if (memory.usagePercent > 80) {
            recommendations.add(PerformanceRecommendation.REDUCE_MEMORY_USAGE)
        }
        
        if (frameRate.averageFps < 25f) {
            recommendations.add(PerformanceRecommendation.IMPROVE_FRAME_RATE)
        }
        
        if (battery.isOverheating) {
            recommendations.add(PerformanceRecommendation.COOL_DOWN_DEVICE)
        }
        
        if (battery.isLowBattery) {
            recommendations.add(PerformanceRecommendation.SAVE_BATTERY)
        }
        
        return recommendations
    }
    
    /**
     * アラートを生成
     */
    private fun generateAlerts(
        memory: MemoryState,
        frameRate: FrameRateData,
        battery: BatteryInfo
    ): List<PerformanceAlert> {
        val alerts = mutableListOf<PerformanceAlert>()
        
        if (memory.isLowMemory) {
            alerts.add(PerformanceAlert.LOW_MEMORY)
        }
        
        if (frameRate.frameDropRate > 0.2f) {
            alerts.add(PerformanceAlert.HIGH_FRAME_DROPS)
        }
        
        if (battery.isOverheating) {
            alerts.add(PerformanceAlert.OVERHEATING)
        }
        
        if (battery.level < 10) {
            alerts.add(PerformanceAlert.CRITICAL_BATTERY)
        }
        
        return alerts
    }
    
    /**
     * 手動最適化を実行
     */
    fun performManualOptimization() {
        val callbacks = optimizationCallbacks ?: return
        val performanceState = performanceMonitor.getOverallPerformanceState()
        
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = callbacks::onTextureQualityChange,
            onRenderQualityChange = callbacks::onRenderQualityChange,
            onFrameRateChange = callbacks::onFrameRateChange,
            onCacheClear = callbacks::onCacheClear,
            onEffectsToggle = callbacks::onEffectsToggle,
            onThermalThrottling = callbacks::onThermalThrottling
        )
    }
    
    /**
     * 設定を更新
     */
    fun updateSettings(settings: PerformanceSettings) {
        performanceOptimizer.updateSettings(settings)
        frameRateMonitor.setTargetFrameRate(settings.targetFrameRate)
    }
}

/**
 * 最適化コールバック
 */
interface OptimizationCallbacks {
    fun onQualityReductionNeeded(reason: QualityAdjustmentReason)
    fun onQualityImprovementPossible()
    fun onMemoryOptimizationNeeded(memoryUsagePercent: Int)
    fun onThermalThrottlingNeeded(temperature: Float)
    fun onBatteryOptimizationNeeded(batteryLevel: Int)
    fun onTextureQualityChange(quality: QualityLevel)
    fun onRenderQualityChange(quality: QualityLevel)
    fun onFrameRateChange(frameRate: Float)
    fun onCacheClear()
    fun onEffectsToggle(enabled: Boolean)
    fun onThermalThrottling(enabled: Boolean)
}

/**
 * 総合パフォーマンス状態
 */
data class OverallPerformanceStatus(
    val performanceScore: Int = 100,
    val memoryUsagePercent: Int = 0,
    val currentFps: Float = 30f,
    val batteryLevel: Int = 100,
    val batteryTemperature: Float = 25f,
    val isOptimizationActive: Boolean = true,
    val recommendations: List<PerformanceRecommendation> = emptyList(),
    val alerts: List<PerformanceAlert> = emptyList(),
    val lastUpdateTime: Long = 0L
)

/**
 * パフォーマンス推奨事項
 */
enum class PerformanceRecommendation {
    REDUCE_MEMORY_USAGE,
    IMPROVE_FRAME_RATE,
    COOL_DOWN_DEVICE,
    SAVE_BATTERY
}

/**
 * パフォーマンスアラート
 */
enum class PerformanceAlert {
    LOW_MEMORY,
    HIGH_FRAME_DROPS,
    OVERHEATING,
    CRITICAL_BATTERY
}