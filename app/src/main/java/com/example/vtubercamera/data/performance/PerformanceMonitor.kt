package com.example.vtubercamera.data.performance

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * システムパフォーマンスを監視し、最適化を提案するクラス
 */
@Singleton
class PerformanceMonitor @Inject constructor(
    context: Context
) {
    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()
    
    private val _frameRateState = MutableStateFlow(FrameRateState())
    val frameRateState: StateFlow<FrameRateState> = _frameRateState.asStateFlow()
    
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    /**
     * メモリ使用量を監視し、状態を更新
     */
    fun updateMemoryState() {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = memoryInfo.availMem
        
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat() * 100).toInt()
        val isLowMemory = memoryInfo.lowMemory
        
        _memoryState.value = MemoryState(
            usedMemoryMB = (usedMemory / (1024 * 1024)).toInt(),
            maxMemoryMB = (maxMemory / (1024 * 1024)).toInt(),
            availableMemoryMB = (availableMemory / (1024 * 1024)).toInt(),
            usagePercent = memoryUsagePercent,
            isLowMemory = isLowMemory,
            shouldOptimize = memoryUsagePercent > 80 || isLowMemory
        )
    }
    
    /**
     * フレームレートを監視し、状態を更新
     */
    fun updateFrameRate(currentFps: Float, targetFps: Float = 30f) {
        val frameDrops = if (currentFps < targetFps * 0.9f) 1 else 0
        val currentState = _frameRateState.value
        
        _frameRateState.value = currentState.copy(
            currentFps = currentFps,
            targetFps = targetFps,
            averageFps = (currentState.averageFps * 0.9f + currentFps * 0.1f),
            frameDrops = currentState.frameDrops + frameDrops,
            isStable = currentFps >= targetFps * 0.9f,
            shouldReduceQuality = currentFps < targetFps * 0.8f
        )
    }
    
    /**
     * バッテリー状態を更新
     */
    fun updateBatteryState(batteryLevel: Int, isCharging: Boolean, temperature: Float) {
        val isOverheating = temperature > 40f // 40度以上で過熱とみなす
        val isLowBattery = batteryLevel < 20
        
        _batteryState.value = BatteryState(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            temperature = temperature,
            isOverheating = isOverheating,
            isLowBattery = isLowBattery,
            shouldOptimize = isOverheating || (isLowBattery && !isCharging)
        )
    }
    
    /**
     * 総合的なパフォーマンス状態を取得
     */
    fun getOverallPerformanceState(): PerformanceState {
        val memory = _memoryState.value
        val frameRate = _frameRateState.value
        val battery = _batteryState.value
        
        val shouldOptimize = memory.shouldOptimize || 
                           frameRate.shouldReduceQuality || 
                           battery.shouldOptimize
        
        return PerformanceState(
            memoryState = memory,
            frameRateState = frameRate,
            batteryState = battery,
            overallHealth = calculateOverallHealth(memory, frameRate, battery),
            shouldOptimize = shouldOptimize,
            recommendedOptimizations = generateOptimizationRecommendations(memory, frameRate, battery)
        )
    }
    
    private fun calculateOverallHealth(
        memory: MemoryState,
        frameRate: FrameRateState,
        battery: BatteryState
    ): PerformanceHealth {
        val memoryScore = when {
            memory.usagePercent < 50 -> 100
            memory.usagePercent < 70 -> 80
            memory.usagePercent < 85 -> 60
            else -> 30
        }
        
        val frameRateScore = when {
            frameRate.averageFps >= frameRate.targetFps * 0.95f -> 100
            frameRate.averageFps >= frameRate.targetFps * 0.85f -> 80
            frameRate.averageFps >= frameRate.targetFps * 0.70f -> 60
            else -> 30
        }
        
        val batteryScore = when {
            battery.batteryLevel > 50 && !battery.isOverheating -> 100
            battery.batteryLevel > 30 && !battery.isOverheating -> 80
            battery.batteryLevel > 15 || battery.isCharging -> 60
            else -> 30
        }
        
        val overallScore = (memoryScore + frameRateScore + batteryScore) / 3
        
        return when {
            overallScore >= 85 -> PerformanceHealth.EXCELLENT
            overallScore >= 70 -> PerformanceHealth.GOOD
            overallScore >= 50 -> PerformanceHealth.FAIR
            else -> PerformanceHealth.POOR
        }
    }
    
    private fun generateOptimizationRecommendations(
        memory: MemoryState,
        frameRate: FrameRateState,
        battery: BatteryState
    ): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        if (memory.shouldOptimize) {
            recommendations.add(OptimizationRecommendation.REDUCE_TEXTURE_QUALITY)
            recommendations.add(OptimizationRecommendation.CLEAR_CACHE)
        }
        
        if (frameRate.shouldReduceQuality) {
            recommendations.add(OptimizationRecommendation.REDUCE_RENDER_QUALITY)
            recommendations.add(OptimizationRecommendation.LOWER_FRAME_RATE)
        }
        
        if (battery.shouldOptimize) {
            recommendations.add(OptimizationRecommendation.REDUCE_BRIGHTNESS)
            recommendations.add(OptimizationRecommendation.DISABLE_EFFECTS)
            if (battery.isOverheating) {
                recommendations.add(OptimizationRecommendation.THERMAL_THROTTLING)
            }
        }
        
        return recommendations
    }
}