package com.example.vtubercamera.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.vtubercamera.data.vrm.TrackingState
import com.example.vtubercamera.data.vrm.TrackingQuality
import com.example.vtubercamera.data.vrm.ARError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ARTrackingMonitor @Inject constructor(
    private val arRepository: ARRepository
) {
    
    private companion object {
        private const val TAG = "ARTrackingMonitor"
        private const val TRACKING_TIMEOUT_MS = 10000L
        private const val TRACKING_LOST_THRESHOLD_MS = 3000L
        private const val QUALITY_CHECK_INTERVAL_MS = 500L
    }
    
    private var monitoringScope: CoroutineScope? = null
    private var trackingTimeoutJob: Job? = null
    private var qualityMonitorJob: Job? = null
    
    private val _trackingEvents = MutableSharedFlow<TrackingEvent>()
    val trackingEvents: SharedFlow<TrackingEvent> = _trackingEvents.asSharedFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private var lastTrackingLostTime = 0L
    private var trackingStartTime = 0L
    
    data class TrackingStats(
        val totalTrackingTime: Long = 0L,
        val trackingLostCount: Int = 0,
        val averageTrackingQuality: Float = 0f,
        val longestTrackingSession: Long = 0L,
        val currentSessionDuration: Long = 0L
    )
    
    private val _trackingStats = MutableStateFlow(TrackingStats())
    val trackingStats: StateFlow<TrackingStats> = _trackingStats.asStateFlow()
    
    sealed class TrackingEvent {
        object TrackingStarted : TrackingEvent()
        object TrackingLost : TrackingEvent()
        object TrackingRecovered : TrackingEvent()
        data class TrackingTimeout(val durationMs: Long) : TrackingEvent()
        data class QualityChanged(val quality: TrackingQuality, val isReliable: Boolean) : TrackingEvent()
        data class TrackingError(val error: ARError) : TrackingEvent()
    }
    
    fun startMonitoring(scope: CoroutineScope) {
        if (_isMonitoring.value) {
            Log.w(TAG, "Tracking monitor is already running")
            return
        }
        
        monitoringScope = scope
        _isMonitoring.value = true
        
        Log.d(TAG, "Starting AR tracking monitoring...")
        
        scope.launch {
            arRepository.trackingState.collect { trackingState ->
                handleTrackingStateChange(trackingState)
            }
        }
        
        startQualityMonitoring(scope)
    }
    
    fun stopMonitoring() {
        if (!_isMonitoring.value) {
            return
        }
        
        trackingTimeoutJob?.cancel()
        qualityMonitorJob?.cancel()
        _isMonitoring.value = false
        
        Log.d(TAG, "AR tracking monitoring stopped")
    }
    
    private fun startQualityMonitoring(scope: CoroutineScope) {
        qualityMonitorJob = scope.launch {
            while (_isMonitoring.value) {
                val cameraState = arRepository.getCurrentCameraState()
                
                _trackingEvents.emit(
                    TrackingEvent.QualityChanged(
                        quality = cameraState.trackingQuality,
                        isReliable = cameraState.isTrackingReliable
                    )
                )
                
                updateTrackingStats(cameraState.trackingQuality)
                
                delay(QUALITY_CHECK_INTERVAL_MS)
            }
        }
    }
    
    private suspend fun handleTrackingStateChange(trackingState: TrackingState) {
        when (trackingState) {
            TrackingState.TRACKING -> {
                handleTrackingStarted()
            }
            TrackingState.TRACKING_LOST -> {
                handleTrackingLost()
            }
            TrackingState.PAUSED -> {
                trackingTimeoutJob?.cancel()
            }
            TrackingState.STOPPED -> {
                trackingTimeoutJob?.cancel()
                resetTrackingStats()
            }
        }
    }
    
    private suspend fun handleTrackingStarted() {
        trackingTimeoutJob?.cancel()
        
        if (lastTrackingLostTime > 0) {
            val recoveryTime = System.currentTimeMillis() - lastTrackingLostTime
            Log.d(TAG, "Tracking recovered after ${recoveryTime}ms")
            _trackingEvents.emit(TrackingEvent.TrackingRecovered)
        } else {
            Log.d(TAG, "Tracking started")
            _trackingEvents.emit(TrackingEvent.TrackingStarted)
            trackingStartTime = System.currentTimeMillis()
        }
        
        lastTrackingLostTime = 0L
    }
    
    private suspend fun handleTrackingLost() {
        lastTrackingLostTime = System.currentTimeMillis()
        Log.w(TAG, "Tracking lost")
        _trackingEvents.emit(TrackingEvent.TrackingLost)
        
        startTrackingTimeout()
    }
    
    private fun startTrackingTimeout() {
        trackingTimeoutJob?.cancel()
        
        trackingTimeoutJob = monitoringScope?.launch {
            delay(TRACKING_TIMEOUT_MS)
            
            val timeoutDuration = System.currentTimeMillis() - lastTrackingLostTime
            Log.e(TAG, "Tracking timeout after ${timeoutDuration}ms")
            
            _trackingEvents.emit(TrackingEvent.TrackingTimeout(timeoutDuration))
            _trackingEvents.emit(
                TrackingEvent.TrackingError(
                    ARError.TrackingError("Tracking lost for more than ${TRACKING_TIMEOUT_MS}ms")
                )
            )
        }
    }
    
    private fun updateTrackingStats(quality: TrackingQuality) {
        val currentStats = _trackingStats.value
        val currentTime = System.currentTimeMillis()
        
        val sessionDuration = if (trackingStartTime > 0) {
            currentTime - trackingStartTime
        } else 0L
        
        val qualityScore = when (quality) {
            TrackingQuality.EXCELLENT -> 5f
            TrackingQuality.GOOD -> 4f
            TrackingQuality.ADEQUATE -> 3f
            TrackingQuality.POOR -> 2f
            TrackingQuality.UNKNOWN -> 1f
        }
        
        _trackingStats.value = currentStats.copy(
            currentSessionDuration = sessionDuration,
            averageTrackingQuality = (currentStats.averageTrackingQuality + qualityScore) / 2f,
            longestTrackingSession = maxOf(currentStats.longestTrackingSession, sessionDuration)
        )
    }
    
    private fun resetTrackingStats() {
        val currentStats = _trackingStats.value
        val sessionDuration = if (trackingStartTime > 0) {
            System.currentTimeMillis() - trackingStartTime
        } else 0L
        
        _trackingStats.value = TrackingStats(
            totalTrackingTime = currentStats.totalTrackingTime + sessionDuration,
            trackingLostCount = currentStats.trackingLostCount,
            averageTrackingQuality = currentStats.averageTrackingQuality,
            longestTrackingSession = maxOf(currentStats.longestTrackingSession, sessionDuration),
            currentSessionDuration = 0L
        )
        
        trackingStartTime = 0L
    }
    
    fun addTrackingLostEvent() {
        val currentStats = _trackingStats.value
        _trackingStats.value = currentStats.copy(
            trackingLostCount = currentStats.trackingLostCount + 1
        )
    }
    
    fun getTrackingRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val stats = _trackingStats.value
        
        if (stats.trackingLostCount > 5) {
            recommendations.add("環境の照明を改善してください")
            recommendations.add("テクスチャのある表面を探してください")
        }
        
        if (stats.averageTrackingQuality < 3.0f) {
            recommendations.add("デバイスをゆっくりと動かしてください")
            recommendations.add("カメラレンズをクリーニングしてください")
        }
        
        if (stats.currentSessionDuration < 5000L && stats.trackingLostCount == 0) {
            recommendations.add("デバイスを安定して持ってください")
            recommendations.add("十分な特徴点がある環境で使用してください")
        }
        
        return recommendations
    }
    
    suspend fun waitForStableTracking(
        requiredQuality: TrackingQuality = TrackingQuality.GOOD,
        stabilityDurationMs: Long = 2000L
    ): Boolean {
        var stableStartTime = 0L
        
        return try {
            arRepository.cameraState
                .map { it.trackingQuality }
                .collect { quality ->
                    if (quality >= requiredQuality) {
                        if (stableStartTime == 0L) {
                            stableStartTime = System.currentTimeMillis()
                        } else if (System.currentTimeMillis() - stableStartTime >= stabilityDurationMs) {
                            return@collect
                        }
                    } else {
                        stableStartTime = 0L
                    }
                }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error waiting for stable tracking", e)
            false
        }
    }
    
    fun getCurrentTrackingInfo(): TrackingInfo {
        val state = arRepository.getCurrentTrackingState()
        val cameraState = arRepository.getCurrentCameraState()
        val stats = _trackingStats.value
        
        return TrackingInfo(
            currentState = state,
            quality = cameraState.trackingQuality,
            isReliable = cameraState.isTrackingReliable,
            sessionDuration = stats.currentSessionDuration,
            recommendations = getTrackingRecommendations()
        )
    }
    
    data class TrackingInfo(
        val currentState: TrackingState,
        val quality: TrackingQuality,
        val isReliable: Boolean,
        val sessionDuration: Long,
        val recommendations: List<String>
    )
}