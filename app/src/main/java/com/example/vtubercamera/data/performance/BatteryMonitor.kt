package com.example.vtubercamera.data.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * バッテリー状態を監視し、消費最適化を提案するクラス
 */
@Singleton
class BatteryMonitor @Inject constructor(
    private val context: Context
) {
    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()
    
    private val _powerConsumption = MutableStateFlow(PowerConsumption())
    val powerConsumption: StateFlow<PowerConsumption> = _powerConsumption.asStateFlow()
    
    private var isMonitoring = false
    private var startTime = 0L
    private var initialBatteryLevel = 0
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    updateBatteryInfo(intent)
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    updateChargingState(true)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    updateChargingState(false)
                }
            }
        }
    }
    
    /**
     * バッテリー監視を開始
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        
        context.registerReceiver(batteryReceiver, filter)
        isMonitoring = true
        startTime = System.currentTimeMillis()
        
        // 初期バッテリー情報を取得
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let { updateBatteryInfo(it) }
        initialBatteryLevel = _batteryInfo.value.level
    }
    
    /**
     * バッテリー監視を停止
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // レシーバーが既に登録解除されている場合
        }
        isMonitoring = false
        
        // 最終的な消費量を計算
        calculateFinalConsumption()
    }
    
    /**
     * バッテリー情報を更新
     */
    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        
        val batteryPercent = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            0
        }
        
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        val healthStatus = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthStatus.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthStatus.OVERHEATING
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthStatus.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthStatus.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealthStatus.FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthStatus.COLD
            else -> BatteryHealthStatus.UNKNOWN
        }
        
        _batteryInfo.value = BatteryInfo(
            level = batteryPercent,
            temperature = temperature,
            voltage = voltage,
            isCharging = isCharging,
            health = healthStatus,
            isOverheating = temperature > 40f,
            isLowBattery = batteryPercent < 20
        )
        
        // 消費量を更新
        updatePowerConsumption(batteryPercent)
    }
    
    /**
     * 充電状態を更新
     */
    private fun updateChargingState(isCharging: Boolean) {
        _batteryInfo.value = _batteryInfo.value.copy(isCharging = isCharging)
    }
    
    /**
     * 電力消費量を更新
     */
    private fun updatePowerConsumption(currentLevel: Int) {
        if (startTime == 0L) return
        
        val elapsedTime = System.currentTimeMillis() - startTime
        val elapsedHours = elapsedTime / (1000f * 60f * 60f)
        val batteryDrop = initialBatteryLevel - currentLevel
        
        if (elapsedHours > 0 && batteryDrop > 0) {
            val consumptionRate = batteryDrop / elapsedHours
            val estimatedRemainingTime = if (consumptionRate > 0) {
                (currentLevel / consumptionRate) * 60 // 分単位
            } else {
                Float.MAX_VALUE
            }
            
            _powerConsumption.value = PowerConsumption(
                consumptionRate = consumptionRate,
                totalConsumed = batteryDrop,
                elapsedTimeMinutes = (elapsedTime / (1000 * 60)).toInt(),
                estimatedRemainingMinutes = estimatedRemainingTime.toInt(),
                isHighConsumption = consumptionRate > 10f, // 10%/時間以上で高消費
                optimizationNeeded = consumptionRate > 15f || currentLevel < 30
            )
        }
    }
    
    /**
     * 最終的な消費量を計算
     */
    private fun calculateFinalConsumption() {
        val currentLevel = _batteryInfo.value.level
        updatePowerConsumption(currentLevel)
    }
    
    /**
     * バッテリー最適化の推奨事項を取得
     */
    fun getBatteryOptimizationRecommendations(): List<BatteryOptimizationRecommendation> {
        val recommendations = mutableListOf<BatteryOptimizationRecommendation>()
        val battery = _batteryInfo.value
        val consumption = _powerConsumption.value
        
        if (battery.isOverheating) {
            recommendations.add(BatteryOptimizationRecommendation.REDUCE_PROCESSING_LOAD)
            recommendations.add(BatteryOptimizationRecommendation.ENABLE_THERMAL_THROTTLING)
        }
        
        if (consumption.isHighConsumption) {
            recommendations.add(BatteryOptimizationRecommendation.LOWER_FRAME_RATE)
            recommendations.add(BatteryOptimizationRecommendation.REDUCE_SCREEN_BRIGHTNESS)
            recommendations.add(BatteryOptimizationRecommendation.DISABLE_UNNECESSARY_FEATURES)
        }
        
        if (battery.isLowBattery && !battery.isCharging) {
            recommendations.add(BatteryOptimizationRecommendation.ENABLE_POWER_SAVING_MODE)
            recommendations.add(BatteryOptimizationRecommendation.REDUCE_QUALITY_SETTINGS)
        }
        
        return recommendations
    }
}

/**
 * バッテリー情報
 */
data class BatteryInfo(
    val level: Int = 100,
    val temperature: Float = 25f,
    val voltage: Float = 4.0f,
    val isCharging: Boolean = false,
    val health: BatteryHealthStatus = BatteryHealthStatus.GOOD,
    val isOverheating: Boolean = false,
    val isLowBattery: Boolean = false
)

/**
 * 電力消費情報
 */
data class PowerConsumption(
    val consumptionRate: Float = 0f, // %/時間
    val totalConsumed: Int = 0, // %
    val elapsedTimeMinutes: Int = 0,
    val estimatedRemainingMinutes: Int = Int.MAX_VALUE,
    val isHighConsumption: Boolean = false,
    val optimizationNeeded: Boolean = false
)

/**
 * バッテリー健全性状態
 */
enum class BatteryHealthStatus {
    GOOD,
    OVERHEATING,
    DEAD,
    OVER_VOLTAGE,
    FAILURE,
    COLD,
    UNKNOWN
}

/**
 * バッテリー最適化推奨事項
 */
enum class BatteryOptimizationRecommendation {
    REDUCE_PROCESSING_LOAD,
    ENABLE_THERMAL_THROTTLING,
    LOWER_FRAME_RATE,
    REDUCE_SCREEN_BRIGHTNESS,
    DISABLE_UNNECESSARY_FEATURES,
    ENABLE_POWER_SAVING_MODE,
    REDUCE_QUALITY_SETTINGS
}