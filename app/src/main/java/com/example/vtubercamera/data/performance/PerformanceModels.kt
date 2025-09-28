package com.example.vtubercamera.data.performance

/**
 * メモリ使用状態を表すデータクラス
 */
data class MemoryState(
    val usedMemoryMB: Int = 0,
    val maxMemoryMB: Int = 0,
    val availableMemoryMB: Int = 0,
    val usagePercent: Int = 0,
    val isLowMemory: Boolean = false,
    val shouldOptimize: Boolean = false
)

/**
 * フレームレート状態を表すデータクラス
 */
data class FrameRateState(
    val currentFps: Float = 30f,
    val targetFps: Float = 30f,
    val averageFps: Float = 30f,
    val frameDrops: Int = 0,
    val isStable: Boolean = true,
    val shouldReduceQuality: Boolean = false
)

/**
 * バッテリー状態を表すデータクラス
 */
data class BatteryState(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val temperature: Float = 25f,
    val isOverheating: Boolean = false,
    val isLowBattery: Boolean = false,
    val shouldOptimize: Boolean = false
)

/**
 * 総合的なパフォーマンス状態
 */
data class PerformanceState(
    val memoryState: MemoryState,
    val frameRateState: FrameRateState,
    val batteryState: BatteryState,
    val overallHealth: PerformanceHealth,
    val shouldOptimize: Boolean,
    val recommendedOptimizations: List<OptimizationRecommendation>
)

/**
 * パフォーマンスの健全性レベル
 */
enum class PerformanceHealth {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}

/**
 * 最適化の推奨事項
 */
enum class OptimizationRecommendation {
    REDUCE_TEXTURE_QUALITY,
    REDUCE_RENDER_QUALITY,
    CLEAR_CACHE,
    LOWER_FRAME_RATE,
    REDUCE_BRIGHTNESS,
    DISABLE_EFFECTS,
    THERMAL_THROTTLING
}

/**
 * パフォーマンス設定
 */
data class PerformanceSettings(
    val autoOptimization: Boolean = true,
    val targetFrameRate: Float = 30f,
    val maxMemoryUsagePercent: Int = 80,
    val thermalThrottlingEnabled: Boolean = true,
    val batteryOptimizationEnabled: Boolean = true,
    val qualityLevel: QualityLevel = QualityLevel.HIGH
)

/**
 * 品質レベル設定
 */
enum class QualityLevel {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}