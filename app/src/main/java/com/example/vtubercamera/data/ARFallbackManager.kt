package com.example.vtubercamera.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.*
import com.example.vtubercamera.data.vrm.*
import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.data.vrm.math.Quaternion
import com.example.vtubercamera.utils.ARDeviceCompatibility
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ARFallbackManager @Inject constructor() {
    
    private companion object {
        private const val TAG = "ARFallbackManager"
    }
    
    enum class FallbackMode {
        NONE,
        LIMITED_AR,
        VIRTUAL_CAMERA,
        CAMERA_ONLY
    }
    
    data class FallbackConfig(
        val mode: FallbackMode,
        val supportedFeatures: Set<ARDeviceCompatibility.ARFeature>,
        val limitations: List<String>,
        val recommendations: List<String>
    )
    
    private val _fallbackConfig = MutableStateFlow<FallbackConfig?>(null)
    val fallbackConfig: StateFlow<FallbackConfig?> = _fallbackConfig.asStateFlow()
    
    private val _isFallbackActive = MutableStateFlow(false)
    val isFallbackActive: StateFlow<Boolean> = _isFallbackActive.asStateFlow()
    
    private val virtualARRepository = VirtualARRepository()
    
    suspend fun determineFallbackMode(
        context: Context,
        compatibilityResult: ARDeviceCompatibility.CompatibilityResult
    ): FallbackConfig {
        Log.d(TAG, "Determining fallback mode for device compatibility")
        
        val compatibilityLevel = ARDeviceCompatibility.getCompatibilityLevel(compatibilityResult)
        
        val fallbackConfig = when (compatibilityLevel) {
            ARDeviceCompatibility.CompatibilityLevel.NOT_SUPPORTED -> {
                createCameraOnlyFallback(compatibilityResult)
            }
            ARDeviceCompatibility.CompatibilityLevel.MINIMAL_SUPPORT -> {
                createVirtualCameraFallback(compatibilityResult)
            }
            ARDeviceCompatibility.CompatibilityLevel.PARTIALLY_SUPPORTED -> {
                createLimitedARFallback(compatibilityResult)
            }
            ARDeviceCompatibility.CompatibilityLevel.FULLY_SUPPORTED -> {
                FallbackConfig(
                    mode = FallbackMode.NONE,
                    supportedFeatures = compatibilityResult.supportedFeatures,
                    limitations = emptyList(),
                    recommendations = emptyList()
                )
            }
        }
        
        _fallbackConfig.value = fallbackConfig
        return fallbackConfig
    }
    
    private fun createCameraOnlyFallback(
        compatibilityResult: ARDeviceCompatibility.CompatibilityResult
    ): FallbackConfig {
        Log.i(TAG, "Creating camera-only fallback mode")
        
        val limitations = listOf(
            "ARCore機能は利用できません",
            "3D仮想オブジェクトの配置はできません",
            "平面検出機能は利用できません",
            "光推定機能は利用できません",
            "カメラ機能のみ使用可能です"
        )
        
        val recommendations = listOf(
            "ARCore対応デバイスへのアップグレードを検討してください",
            "基本的なカメラ撮影機能をご利用ください",
            "写真編集機能で仮想エフェクトを追加できます"
        )
        
        return FallbackConfig(
            mode = FallbackMode.CAMERA_ONLY,
            supportedFeatures = emptySet(),
            limitations = limitations,
            recommendations = recommendations
        )
    }
    
    private fun createVirtualCameraFallback(
        compatibilityResult: ARDeviceCompatibility.CompatibilityResult
    ): FallbackConfig {
        Log.i(TAG, "Creating virtual camera fallback mode")
        
        val limitations = listOf(
            "リアルタイムAR機能は制限されます",
            "トラッキング精度が低下する可能性があります",
            "一部の高度なAR機能は利用できません"
        )
        
        val recommendations = listOf(
            "デバイスを安定して保持してください",
            "十分な照明がある環境で使用してください",
            "バッテリー消費を抑えるため短時間でご利用ください"
        )
        
        return FallbackConfig(
            mode = FallbackMode.VIRTUAL_CAMERA,
            supportedFeatures = setOf(
                ARDeviceCompatibility.ARFeature.PLANE_DETECTION,
                ARDeviceCompatibility.ARFeature.LIGHT_ESTIMATION
            ),
            limitations = limitations,
            recommendations = recommendations
        )
    }
    
    private fun createLimitedARFallback(
        compatibilityResult: ARDeviceCompatibility.CompatibilityResult
    ): FallbackConfig {
        Log.i(TAG, "Creating limited AR fallback mode")
        
        val limitations = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        if (!compatibilityResult.supportedFeatures.contains(ARDeviceCompatibility.ARFeature.ENVIRONMENTAL_HDR)) {
            limitations.add("環境HDR機能は利用できません")
            recommendations.add("基本的な光推定モードを使用します")
        }
        
        if (compatibilityResult.deviceInfo.totalRAM < 3L * 1024 * 1024 * 1024) {
            limitations.add("メモリ容量により一部機能が制限されます")
            recommendations.add("他のアプリを終了してメモリを確保してください")
        }
        
        return FallbackConfig(
            mode = FallbackMode.LIMITED_AR,
            supportedFeatures = compatibilityResult.supportedFeatures,
            limitations = limitations,
            recommendations = recommendations
        )
    }
    
    fun activateFallback(config: FallbackConfig) {
        Log.i(TAG, "Activating fallback mode: ${config.mode}")
        _isFallbackActive.value = true
        
        when (config.mode) {
            FallbackMode.VIRTUAL_CAMERA -> {
                virtualARRepository.initialize()
            }
            FallbackMode.LIMITED_AR -> {
                // 制限モードでの初期化
            }
            FallbackMode.CAMERA_ONLY -> {
                // カメラのみモードでの初期化
            }
            FallbackMode.NONE -> {
                // フォールバック無効化
            }
        }
    }
    
    fun deactivateFallback() {
        Log.i(TAG, "Deactivating fallback mode")
        _isFallbackActive.value = false
        virtualARRepository.shutdown()
    }
    
    fun getFallbackARRepository(): ARRepository? {
        return if (_isFallbackActive.value) {
            when (_fallbackConfig.value?.mode) {
                FallbackMode.VIRTUAL_CAMERA -> virtualARRepository
                else -> null
            }
        } else {
            null
        }
    }
    
    fun createCompatibilityReport(
        compatibilityResult: ARDeviceCompatibility.CompatibilityResult,
        fallbackConfig: FallbackConfig
    ): CompatibilityReport {
        return CompatibilityReport(
            deviceModel = "${compatibilityResult.deviceInfo.manufacturer} ${compatibilityResult.deviceInfo.model}",
            androidVersion = compatibilityResult.deviceInfo.androidVersion,
            arCoreSupported = compatibilityResult.isARCoreSupported,
            fallbackMode = fallbackConfig.mode,
            supportedFeatures = fallbackConfig.supportedFeatures.map { it.name },
            limitations = fallbackConfig.limitations,
            recommendations = fallbackConfig.recommendations,
            performanceLevel = determinePerformanceLevel(compatibilityResult)
        )
    }
    
    private fun determinePerformanceLevel(
        compatibilityResult: ARDeviceCompatibility.CompatibilityResult
    ): PerformanceLevel {
        val deviceInfo = compatibilityResult.deviceInfo
        
        return when {
            deviceInfo.totalRAM >= 6L * 1024 * 1024 * 1024 && 
            deviceInfo.androidVersion >= 28 && 
            compatibilityResult.supportedFeatures.contains(ARDeviceCompatibility.ARFeature.ENVIRONMENTAL_HDR) -> 
                PerformanceLevel.HIGH
            
            deviceInfo.totalRAM >= 3L * 1024 * 1024 * 1024 && 
            deviceInfo.androidVersion >= 26 -> 
                PerformanceLevel.MEDIUM
            
            deviceInfo.totalRAM >= 2L * 1024 * 1024 * 1024 -> 
                PerformanceLevel.LOW
            
            else -> PerformanceLevel.MINIMAL
        }
    }
    
    data class CompatibilityReport(
        val deviceModel: String,
        val androidVersion: Int,
        val arCoreSupported: Boolean,
        val fallbackMode: FallbackMode,
        val supportedFeatures: List<String>,
        val limitations: List<String>,
        val recommendations: List<String>,
        val performanceLevel: PerformanceLevel
    )
    
    enum class PerformanceLevel {
        HIGH,
        MEDIUM, 
        LOW,
        MINIMAL
    }
    
    private inner class VirtualARRepository : ARRepository {
        private val _sessionState = MutableStateFlow(ARSessionState())
        private val _cameraState = MutableStateFlow(ARCameraState.default())
        private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
        private val _lightEstimate = MutableStateFlow<LightEstimate?>(null)
        
        private var isInitialized = false
        
        fun initialize() {
            isInitialized = true
            _sessionState.value = ARSessionState(isInitialized = true, trackingState = TrackingState.TRACKING)
            _trackingState.value = TrackingState.TRACKING
            _cameraState.value = ARCameraState.default().copy(
                isTracking = true,
                trackingQuality = TrackingQuality.ADEQUATE
            )
            Log.d(TAG, "Virtual AR Repository initialized")
        }
        
        fun shutdown() {
            isInitialized = false
            _sessionState.value = ARSessionState()
            _trackingState.value = TrackingState.STOPPED
            _cameraState.value = ARCameraState.default()
            _lightEstimate.value = null
            Log.d(TAG, "Virtual AR Repository shutdown")
        }
        
        override fun initializeSession(
            context: Context,
            lifecycleOwner: androidx.lifecycle.LifecycleOwner,
            onSessionReady: () -> Unit,
            onError: (ARError) -> Unit
        ) {
            Log.d(TAG, "Virtual session initialization")
            initialize()
            onSessionReady()
        }
        
        override fun pauseSession() {
            _sessionState.value = _sessionState.value.copy(trackingState = TrackingState.PAUSED)
            _trackingState.value = TrackingState.PAUSED
        }
        
        override fun resumeSession() {
            if (isInitialized) {
                _sessionState.value = _sessionState.value.copy(trackingState = TrackingState.TRACKING)
                _trackingState.value = TrackingState.TRACKING
            }
        }
        
        override fun destroySession() {
            shutdown()
        }
        
        override val sessionState = _sessionState.asStateFlow()
        override val cameraState = _cameraState.asStateFlow()
        override val trackingState = _trackingState.asStateFlow()
        override val lightEstimate: Flow<LightEstimate?> = _lightEstimate.asStateFlow()
        
        override fun enablePlaneDetection(enabled: Boolean) {
            _sessionState.value = _sessionState.value.copy(planeDetection = enabled)
        }
        
        override fun enableEnvironmentalHDR(enabled: Boolean) {
            _sessionState.value = _sessionState.value.copy(environmentalHDR = false) // 常にfalse
        }
        
        override fun enableLightEstimation(enabled: Boolean) {
            if (enabled) {
                _lightEstimate.value = LightEstimate(pixelIntensity = 0.7f)
                _sessionState.value = _sessionState.value.copy(lightEstimate = _lightEstimate.value)
            }
        }
        
        override fun isSessionInitialized(): Boolean = isInitialized
        override fun getCurrentTrackingState(): TrackingState = _trackingState.value
        override fun getCurrentCameraState(): ARCameraState = _cameraState.value
        override fun getCurrentLightEstimate(): LightEstimate? = _lightEstimate.value
        
        override fun setTrackingStateListener(listener: (TrackingState) -> Unit) {
            // 仮想実装では無効
        }
        
        override fun removeTrackingStateListener() {
            // 仮想実装では無効
        }
        
        override fun configureSession(
            planeDetectionEnabled: Boolean,
            lightEstimationEnabled: Boolean,
            environmentalHDREnabled: Boolean
        ) {
            enablePlaneDetection(planeDetectionEnabled)
            enableLightEstimation(lightEstimationEnabled)
            enableEnvironmentalHDR(environmentalHDREnabled)
        }
        
        override suspend fun waitForTracking(timeoutMs: Long): Boolean {
            return isInitialized
        }
    }
}