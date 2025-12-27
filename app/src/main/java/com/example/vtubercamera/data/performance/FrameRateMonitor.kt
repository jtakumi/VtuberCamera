package com.example.vtubercamera.data.performance

import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * フレームレートを監視し、品質自動調整を行うクラス
 */
@Singleton
class FrameRateMonitor @Inject constructor() : Choreographer.FrameCallback {
    
    private val _frameRateData = MutableStateFlow(FrameRateData())
    val frameRateData: StateFlow<FrameRateData> = _frameRateData.asStateFlow()
    
    private val _qualityAdjustment = MutableStateFlow(QualityAdjustment())
    val qualityAdjustment: StateFlow<QualityAdjustment> = _qualityAdjustment.asStateFlow()
    
    private var isMonitoring = false
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var startTime = 0L
    private val frameTimes = mutableListOf<Long>()
    private val maxFrameTimeHistory = 60 // 60フレーム分の履歴を保持
    
    private var targetFrameRate = 30f
    private var qualityLevel = QualityLevel.HIGH
    
    /**
     * フレームレート監視を開始
     */
    fun startMonitoring(targetFps: Float = 30f) {
        if (isMonitoring) return
        
        targetFrameRate = targetFps
        isMonitoring = true
        frameCount = 0
        startTime = System.nanoTime()
        lastFrameTime = startTime
        frameTimes.clear()
        
        Choreographer.getInstance().postFrameCallback(this)
    }
    
    /**
     * フレームレート監視を停止
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        Choreographer.getInstance().removeFrameCallback(this)
    }
    
    override fun doFrame(frameTimeNanos: Long) {
        if (!isMonitoring) return
        
        frameCount++
        
        // フレーム時間を記録
        if (lastFrameTime != 0L) {
            val frameTime = frameTimeNanos - lastFrameTime
            frameTimes.add(frameTime)
            
            // 履歴サイズを制限
            if (frameTimes.size > maxFrameTimeHistory) {
                frameTimes.removeAt(0)
            }
        }
        
        lastFrameTime = frameTimeNanos
        
        // 1秒ごとにフレームレートを計算
        val elapsedTime = frameTimeNanos - startTime
        if (elapsedTime >= 1_000_000_000L) { // 1秒 = 1,000,000,000ナノ秒
            calculateFrameRateMetrics(elapsedTime)
            
            // 次の測定期間を開始
            startTime = frameTimeNanos
            frameCount = 0
        }
        
        // 次のフレームコールバックを登録
        Choreographer.getInstance().postFrameCallback(this)
    }
    
    /**
     * フレームレートメトリクスを計算
     */
    private fun calculateFrameRateMetrics(elapsedTimeNanos: Long) {
        val elapsedSeconds = elapsedTimeNanos / 1_000_000_000.0
        val currentFps = frameCount / elapsedSeconds
        
        // フレーム時間の統計を計算
        val frameTimeStats = calculateFrameTimeStatistics()
        
        // フレームドロップを検出
        val frameDrops = frameTimes.count { it > (1_000_000_000L / targetFrameRate) * 1.5 }
        val frameDropRate = if (frameTimes.isNotEmpty()) frameDrops.toFloat() / frameTimes.size else 0f
        
        // ジッターを計算（フレーム時間の標準偏差）
        val jitter = calculateJitter()
        
        val currentData = _frameRateData.value
        val newData = currentData.copy(
            currentFps = currentFps.toFloat(),
            averageFps = (currentData.averageFps * 0.8f + currentFps * 0.2f).toFloat(),
            minFps = minOf(currentData.minFps, currentFps.toFloat()),
            maxFps = maxOf(currentData.maxFps, currentFps.toFloat()),
            frameDrops = currentData.frameDrops + frameDrops,
            frameDropRate = frameDropRate,
            jitter = jitter,
            isStable = frameDropRate < 0.1f && jitter < 5f,
            frameTimeStats = frameTimeStats
        )
        
        _frameRateData.value = newData
        
        // 品質調整の判定
        evaluateQualityAdjustment(newData)
    }
    
    /**
     * フレーム時間の統計を計算
     */
    private fun calculateFrameTimeStatistics(): FrameTimeStatistics {
        if (frameTimes.isEmpty()) {
            return FrameTimeStatistics()
        }
        
        val sortedTimes = frameTimes.sorted()
        val average = frameTimes.average()
        val median = if (sortedTimes.size % 2 == 0) {
            (sortedTimes[sortedTimes.size / 2 - 1] + sortedTimes[sortedTimes.size / 2]) / 2.0
        } else {
            sortedTimes[sortedTimes.size / 2].toDouble()
        }
        
        val p95Index = (sortedTimes.size * 0.95).toInt().coerceAtMost(sortedTimes.size - 1)
        val p99Index = (sortedTimes.size * 0.99).toInt().coerceAtMost(sortedTimes.size - 1)
        
        return FrameTimeStatistics(
            averageMs = (average / 1_000_000).toFloat(),
            medianMs = (median / 1_000_000).toFloat(),
            p95Ms = (sortedTimes[p95Index] / 1_000_000).toFloat(),
            p99Ms = (sortedTimes[p99Index] / 1_000_000).toFloat(),
            minMs = (sortedTimes.first() / 1_000_000).toFloat(),
            maxMs = (sortedTimes.last() / 1_000_000).toFloat()
        )
    }
    
    /**
     * ジッター（フレーム時間のばらつき）を計算
     */
    private fun calculateJitter(): Float {
        if (frameTimes.size < 2) return 0f
        
        val average = frameTimes.average()
        val variance = frameTimes.map { (it - average) * (it - average) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        return (standardDeviation / 1_000_000).toFloat() // ミリ秒単位
    }
    
    /**
     * 品質調整の必要性を評価
     */
    private fun evaluateQualityAdjustment(frameData: FrameRateData) {
        val currentAdjustment = _qualityAdjustment.value
        
        val shouldReduceQuality = frameData.averageFps < targetFrameRate * 0.8f ||
                                 frameData.frameDropRate > 0.15f ||
                                 frameData.jitter > 10f
        
        val shouldIncreaseQuality = frameData.averageFps > targetFrameRate * 0.95f &&
                                   frameData.frameDropRate < 0.05f &&
                                   frameData.jitter < 3f &&
                                   qualityLevel != QualityLevel.ULTRA
        
        val newAdjustment = when {
            shouldReduceQuality && !currentAdjustment.isReducing -> {
                currentAdjustment.copy(
                    shouldReduceQuality = true,
                    shouldIncreaseQuality = false,
                    isReducing = true,
                    lastAdjustmentTime = System.currentTimeMillis(),
                    reason = determineAdjustmentReason(frameData, true)
                )
            }
            shouldIncreaseQuality && !currentAdjustment.isIncreasing -> {
                currentAdjustment.copy(
                    shouldReduceQuality = false,
                    shouldIncreaseQuality = true,
                    isIncreasing = true,
                    lastAdjustmentTime = System.currentTimeMillis(),
                    reason = determineAdjustmentReason(frameData, false)
                )
            }
            else -> currentAdjustment.copy(
                shouldReduceQuality = false,
                shouldIncreaseQuality = false,
                isReducing = false,
                isIncreasing = false
            )
        }
        
        _qualityAdjustment.value = newAdjustment
    }
    
    /**
     * 調整理由を決定
     */
    private fun determineAdjustmentReason(frameData: FrameRateData, isReduction: Boolean): QualityAdjustmentReason {
        return if (isReduction) {
            when {
                frameData.frameDropRate > 0.2f -> QualityAdjustmentReason.HIGH_FRAME_DROPS
                frameData.jitter > 15f -> QualityAdjustmentReason.HIGH_JITTER
                frameData.averageFps < targetFrameRate * 0.7f -> QualityAdjustmentReason.LOW_FRAME_RATE
                else -> QualityAdjustmentReason.GENERAL_PERFORMANCE
            }
        } else {
            QualityAdjustmentReason.PERFORMANCE_IMPROVED
        }
    }
    
    /**
     * 品質レベルを設定
     */
    fun setQualityLevel(level: QualityLevel) {
        qualityLevel = level
    }
    
    /**
     * ターゲットフレームレートを設定
     */
    fun setTargetFrameRate(fps: Float) {
        targetFrameRate = fps
    }
    
    /**
     * 統計をリセット
     */
    fun resetStatistics() {
        _frameRateData.value = FrameRateData()
        _qualityAdjustment.value = QualityAdjustment()
        frameTimes.clear()
        frameCount = 0
    }
}

/**
 * フレームレートデータ
 */
data class FrameRateData(
    val currentFps: Float = 30f,
    val averageFps: Float = 30f,
    val minFps: Float = 30f,
    val maxFps: Float = 30f,
    val frameDrops: Int = 0,
    val frameDropRate: Float = 0f,
    val jitter: Float = 0f,
    val isStable: Boolean = true,
    val frameTimeStats: FrameTimeStatistics = FrameTimeStatistics()
)

/**
 * フレーム時間統計
 */
data class FrameTimeStatistics(
    val averageMs: Float = 33.3f, // 30fps = 33.3ms
    val medianMs: Float = 33.3f,
    val p95Ms: Float = 33.3f,
    val p99Ms: Float = 33.3f,
    val minMs: Float = 33.3f,
    val maxMs: Float = 33.3f
)

/**
 * 品質調整情報
 */
data class QualityAdjustment(
    val shouldReduceQuality: Boolean = false,
    val shouldIncreaseQuality: Boolean = false,
    val isReducing: Boolean = false,
    val isIncreasing: Boolean = false,
    val lastAdjustmentTime: Long = 0L,
    val reason: QualityAdjustmentReason = QualityAdjustmentReason.NONE
)

/**
 * 品質調整理由
 */
enum class QualityAdjustmentReason {
    NONE,
    LOW_FRAME_RATE,
    HIGH_FRAME_DROPS,
    HIGH_JITTER,
    GENERAL_PERFORMANCE,
    PERFORMANCE_IMPROVED
}