package com.example.vtubercamera.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtubercamera.data.performance.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * パフォーマンス監視デモ画面のViewModel
 */
@HiltViewModel
class PerformanceMonitorDemoViewModel @Inject constructor(
    private val performanceManager: PerformanceManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PerformanceMonitorDemoUiState())
    val uiState: StateFlow<PerformanceMonitorDemoUiState> = _uiState.asStateFlow()
    
    // シミュレーション用の状態
    private val _simulatedMemoryState = MutableStateFlow<MemoryState?>(null)
    private val _simulatedFrameRateData = MutableStateFlow<FrameRateData?>(null)
    private val _simulatedBatteryInfo = MutableStateFlow<BatteryInfo?>(null)
    
    init {
        // パフォーマンス監視の状態を監視
        viewModelScope.launch {
            combine(
                performanceManager.isMonitoring,
                performanceManager.overallStatus,
                _simulatedMemoryState,
                _simulatedFrameRateData,
                _simulatedBatteryInfo
            ) { isMonitoring, overallStatus, simMemory, simFrameRate, simBattery ->
                
                _uiState.value = _uiState.value.copy(
                    isMonitoring = isMonitoring,
                    performanceStatus = overallStatus,
                    memoryState = simMemory ?: MemoryState(
                        usedMemoryMB = 150,
                        maxMemoryMB = 512,
                        availableMemoryMB = 362,
                        usagePercent = 30,
                        isLowMemory = false,
                        shouldOptimize = false
                    ),
                    frameRateData = simFrameRate ?: FrameRateData(
                        currentFps = 30f,
                        averageFps = 29.5f,
                        minFps = 28f,
                        maxFps = 30f,
                        frameDrops = 2,
                        frameDropRate = 0.03f,
                        jitter = 1.2f,
                        isStable = true
                    ),
                    batteryInfo = simBattery ?: BatteryInfo(
                        level = 75,
                        temperature = 32f,
                        voltage = 4.1f,
                        isCharging = false,
                        health = BatteryHealthStatus.GOOD,
                        isOverheating = false,
                        isLowBattery = false
                    ),
                    powerConsumption = PowerConsumption(
                        consumptionRate = 8.5f,
                        totalConsumed = 25,
                        elapsedTimeMinutes = 180,
                        estimatedRemainingMinutes = 520,
                        isHighConsumption = false,
                        optimizationNeeded = false
                    )
                )
            }
        }
    }
    
    /**
     * パフォーマンス監視の開始/停止を切り替え
     */
    fun toggleMonitoring() {
        if (performanceManager.isMonitoring.value) {
            performanceManager.stopMonitoring()
        } else {
            performanceManager.startMonitoring(
                targetFrameRate = _uiState.value.settings.targetFrameRate,
                callbacks = createOptimizationCallbacks()
            )
        }
    }
    
    /**
     * 手動最適化を実行
     */
    fun performManualOptimization() {
        performanceManager.performManualOptimization()
    }
    
    /**
     * パフォーマンス設定を更新
     */
    fun updateSettings(settings: PerformanceSettings) {
        _uiState.value = _uiState.value.copy(settings = settings)
        performanceManager.updateSettings(settings)
    }
    
    /**
     * 高メモリ使用量をシミュレート
     */
    fun simulateHighMemoryUsage() {
        _simulatedMemoryState.value = MemoryState(
            usedMemoryMB = 450,
            maxMemoryMB = 512,
            availableMemoryMB = 62,
            usagePercent = 88,
            isLowMemory = true,
            shouldOptimize = true
        )
    }
    
    /**
     * 低フレームレートをシミュレート
     */
    fun simulateLowFrameRate() {
        _simulatedFrameRateData.value = FrameRateData(
            currentFps = 18f,
            averageFps = 19.2f,
            minFps = 15f,
            maxFps = 22f,
            frameDrops = 45,
            frameDropRate = 0.25f,
            jitter = 8.5f,
            isStable = false
        )
    }
    
    /**
     * デバイス過熱をシミュレート
     */
    fun simulateOverheating() {
        _simulatedBatteryInfo.value = BatteryInfo(
            level = 45,
            temperature = 47f,
            voltage = 3.9f,
            isCharging = false,
            health = BatteryHealthStatus.OVERHEATING,
            isOverheating = true,
            isLowBattery = false
        )
    }
    
    /**
     * シミュレーションをリセット
     */
    fun resetSimulation() {
        _simulatedMemoryState.value = null
        _simulatedFrameRateData.value = null
        _simulatedBatteryInfo.value = null
    }
    
    /**
     * 最適化コールバックを作成
     */
    private fun createOptimizationCallbacks(): OptimizationCallbacks {
        return object : OptimizationCallbacks {
            override fun onQualityReductionNeeded(reason: QualityAdjustmentReason) {
                // 品質低下が必要な場合の処理
            }
            
            override fun onQualityImprovementPossible() {
                // 品質向上が可能な場合の処理
            }
            
            override fun onMemoryOptimizationNeeded(memoryUsagePercent: Int) {
                // メモリ最適化が必要な場合の処理
            }
            
            override fun onThermalThrottlingNeeded(temperature: Float) {
                // 熱制御が必要な場合の処理
            }
            
            override fun onBatteryOptimizationNeeded(batteryLevel: Int) {
                // バッテリー最適化が必要な場合の処理
            }
            
            override fun onTextureQualityChange(quality: QualityLevel) {
                // テクスチャ品質変更の処理
            }
            
            override fun onRenderQualityChange(quality: QualityLevel) {
                // レンダリング品質変更の処理
            }
            
            override fun onFrameRateChange(frameRate: Float) {
                // フレームレート変更の処理
            }
            
            override fun onCacheClear() {
                // キャッシュクリアの処理
            }
            
            override fun onEffectsToggle(enabled: Boolean) {
                // エフェクト切り替えの処理
            }
            
            override fun onThermalThrottling(enabled: Boolean) {
                // 熱制御切り替えの処理
            }
        }
    }
}

/**
 * パフォーマンス監視デモ画面のUI状態
 */
data class PerformanceMonitorDemoUiState(
    val isMonitoring: Boolean = false,
    val performanceStatus: OverallPerformanceStatus = OverallPerformanceStatus(),
    val memoryState: MemoryState = MemoryState(),
    val frameRateData: FrameRateData = FrameRateData(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val powerConsumption: PowerConsumption = PowerConsumption(),
    val settings: PerformanceSettings = PerformanceSettings()
)