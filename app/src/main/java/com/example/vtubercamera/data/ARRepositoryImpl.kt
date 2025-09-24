package com.example.vtubercamera.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import com.example.vtubercamera.data.vrm.*
import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.data.vrm.math.Quaternion
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ARRepositoryImpl @Inject constructor() : ARRepository {
    
    private companion object {
        private const val TAG = "ARRepositoryImpl"
    }
    
    private var arSession: Session? = null
    private var config: Config? = null
    private var lifecycleOwner: LifecycleOwner? = null
    
    private val _sessionState = MutableStateFlow(ARSessionState())
    override val sessionState: StateFlow<ARSessionState> = _sessionState.asStateFlow()
    
    private val _cameraState = MutableStateFlow(ARCameraState.default())
    override val cameraState: StateFlow<ARCameraState> = _cameraState.asStateFlow()
    
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    override val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    private val _lightEstimate = MutableStateFlow<LightEstimate?>(null)
    override val lightEstimate: StateFlow<LightEstimate?> = _lightEstimate.asStateFlow()
    
    private var trackingStateListener: ((TrackingState) -> Unit)? = null
    
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> pauseSession()
            Lifecycle.Event.ON_RESUME -> resumeSession()
            Lifecycle.Event.ON_DESTROY -> destroySession()
            else -> {}
        }
    }
    
    override fun initializeSession(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onSessionReady: () -> Unit,
        onError: (ARError) -> Unit
    ) {
        try {
            Log.d(TAG, "Initializing AR session...")
            
            this.lifecycleOwner = lifecycleOwner
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            
            if (!isARCoreSupportedAndUpToDate(context)) {
                onError(ARError.UnsupportedDevice("ARCore is not supported on this device"))
                return
            }
            
            arSession = Session(context, setOf()).apply {
                config = createSessionConfig(this)
                configure(config)
            }
            
            _sessionState.value = _sessionState.value.copy(
                isInitialized = true,
                trackingState = TrackingState.PAUSED
            )
            
            Log.d(TAG, "AR session initialized successfully")
            onSessionReady()
            
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore not installed", e)
            onError(ARError.UnsupportedDevice("ARCore is not installed"))
        } catch (e: UnavailableApkTooOldException) {
            Log.e(TAG, "ARCore APK is too old", e)
            onError(ARError.UnsupportedDevice("ARCore APK is too old"))
        } catch (e: UnavailableSdkTooOldException) {
            Log.e(TAG, "SDK is too old", e)
            onError(ARError.UnsupportedDevice("SDK is too old for ARCore"))
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device not compatible with ARCore", e)
            onError(ARError.UnsupportedDevice("Device not compatible with ARCore"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AR session", e)
            onError(ARError.SessionError("Failed to initialize AR session: ${e.message}"))
        }
    }
    
    override fun pauseSession() {
        try {
            arSession?.pause()
            _sessionState.value = _sessionState.value.copy(trackingState = TrackingState.PAUSED)
            _trackingState.value = TrackingState.PAUSED
            Log.d(TAG, "AR session paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing AR session", e)
        }
    }
    
    override fun resumeSession() {
        try {
            arSession?.resume()
            if (_sessionState.value.isInitialized) {
                _sessionState.value = _sessionState.value.copy(trackingState = TrackingState.TRACKING)
                _trackingState.value = TrackingState.TRACKING
            }
            Log.d(TAG, "AR session resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming AR session", e)
        }
    }
    
    override fun destroySession() {
        try {
            lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
            arSession?.close()
            arSession = null
            config = null
            lifecycleOwner = null
            
            _sessionState.value = ARSessionState()
            _cameraState.value = ARCameraState.default()
            _trackingState.value = TrackingState.STOPPED
            _lightEstimate.value = null
            
            Log.d(TAG, "AR session destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying AR session", e)
        }
    }
    
    override fun enablePlaneDetection(enabled: Boolean) {
        config?.let { config ->
            config.planeFindingMode = if (enabled) {
                Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            } else {
                Config.PlaneFindingMode.DISABLED
            }
            arSession?.configure(config)
            
            _sessionState.value = _sessionState.value.copy(planeDetection = enabled)
            Log.d(TAG, "Plane detection ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    override fun enableEnvironmentalHDR(enabled: Boolean) {
        config?.let { config ->
            if (arSession?.isSupported(Config.LightEstimationMode.ENVIRONMENTAL_HDR) == true) {
                config.lightEstimationMode = if (enabled) {
                    Config.LightEstimationMode.ENVIRONMENTAL_HDR
                } else {
                    Config.LightEstimationMode.AMBIENT_INTENSITY
                }
                arSession?.configure(config)
                
                _sessionState.value = _sessionState.value.copy(environmentalHDR = enabled)
                Log.d(TAG, "Environmental HDR ${if (enabled) "enabled" else "disabled"}")
            } else {
                Log.w(TAG, "Environmental HDR not supported on this device")
            }
        }
    }
    
    override fun enableLightEstimation(enabled: Boolean) {
        config?.let { config ->
            config.lightEstimationMode = if (enabled) {
                if (_sessionState.value.environmentalHDR && 
                    arSession?.isSupported(Config.LightEstimationMode.ENVIRONMENTAL_HDR) == true) {
                    Config.LightEstimationMode.ENVIRONMENTAL_HDR
                } else {
                    Config.LightEstimationMode.AMBIENT_INTENSITY
                }
            } else {
                Config.LightEstimationMode.DISABLED
            }
            arSession?.configure(config)
            Log.d(TAG, "Light estimation ${if (enabled) "enabled" else "disabled"}")
        }
    }
    
    override fun isSessionInitialized(): Boolean = _sessionState.value.isInitialized
    
    override fun getCurrentTrackingState(): TrackingState = _trackingState.value
    
    override fun getCurrentCameraState(): ARCameraState = _cameraState.value
    
    override fun getCurrentLightEstimate(): LightEstimate? = _lightEstimate.value
    
    override fun setTrackingStateListener(listener: (TrackingState) -> Unit) {
        trackingStateListener = listener
    }
    
    override fun removeTrackingStateListener() {
        trackingStateListener = null
    }
    
    override fun configureSession(
        planeDetectionEnabled: Boolean,
        lightEstimationEnabled: Boolean,
        environmentalHDREnabled: Boolean
    ) {
        enablePlaneDetection(planeDetectionEnabled)
        enableEnvironmentalHDR(environmentalHDREnabled)
        enableLightEstimation(lightEstimationEnabled)
        Log.d(TAG, "Session configured - Plane: $planeDetectionEnabled, Light: $lightEstimationEnabled, HDR: $environmentalHDREnabled")
    }
    
    override suspend fun waitForTracking(timeoutMs: Long): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                if (_trackingState.value == TrackingState.TRACKING) {
                    continuation.resume(true)
                    return@suspendCancellableCoroutine
                }
                
                var resumed = false
                trackingState.collect { state ->
                    if (state == TrackingState.TRACKING && !resumed) {
                        resumed = true
                        continuation.resume(true)
                    }
                }
            }
        } ?: false
    }
    
    fun updateSessionState(frame: Frame) {
        try {
            val camera = frame.camera
            val trackingState = camera.trackingState
            
            updateTrackingState(trackingState)
            updateCameraState(camera)
            updateLightEstimate(frame)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating session state", e)
        }
    }
    
    private fun updateTrackingState(cameraTrackingState: Camera.TrackingState) {
        val newState = when (cameraTrackingState) {
            Camera.TrackingState.PAUSED -> TrackingState.PAUSED
            Camera.TrackingState.STOPPED -> TrackingState.STOPPED
            Camera.TrackingState.TRACKING -> TrackingState.TRACKING
        }
        
        if (_trackingState.value != newState) {
            _trackingState.value = newState
            _sessionState.value = _sessionState.value.copy(trackingState = newState)
            trackingStateListener?.invoke(newState)
        }
    }
    
    private fun updateCameraState(camera: Camera) {
        val pose = camera.pose
        val position = Vector3(pose.tx(), pose.ty(), pose.tz())
        val rotation = Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw())
        
        val intrinsics = camera.imageIntrinsics
        val focalLength = (intrinsics[0] + intrinsics[4]) / 2.0f
        val imageWidth = intrinsics[2] * 2
        val fovRadians = 2.0f * kotlin.math.atan(imageWidth / (2.0f * focalLength))
        val fovDegrees = kotlin.math.toDegrees(fovRadians.toDouble()).toFloat()
        
        val trackingQuality = when (camera.trackingState) {
            Camera.TrackingState.TRACKING -> TrackingQuality.GOOD
            Camera.TrackingState.PAUSED -> TrackingQuality.POOR
            Camera.TrackingState.STOPPED -> TrackingQuality.UNKNOWN
        }
        
        val newCameraState = _cameraState.value.copy(
            position = position,
            rotation = rotation,
            fieldOfView = fovDegrees,
            isTracking = camera.trackingState == Camera.TrackingState.TRACKING,
            trackingQuality = trackingQuality,
            lastUpdateTimestamp = System.currentTimeMillis()
        )
        
        _cameraState.value = newCameraState
    }
    
    private fun updateLightEstimate(frame: Frame) {
        try {
            val lightEstimate = frame.lightEstimate
            val pixelIntensity = lightEstimate.pixelIntensity
            
            if (pixelIntensity > 0) {
                val colorCorrection = lightEstimate.colorCorrectionRgba
                val estimate = LightEstimate(
                    pixelIntensity = pixelIntensity,
                    colorCorrection = colorCorrection,
                    timestamp = System.currentTimeMillis()
                )
                
                _lightEstimate.value = estimate
                _sessionState.value = _sessionState.value.copy(lightEstimate = estimate)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get light estimate", e)
        }
    }
    
    private fun isARCoreSupportedAndUpToDate(context: Context): Boolean {
        return when (ArCoreApk.getInstance().checkAvailability(context)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                Log.i(TAG, "ARCore is supported but needs installation/update")
                false
            }
            else -> {
                Log.w(TAG, "ARCore is not supported on this device")
                false
            }
        }
    }
    
    private fun createSessionConfig(session: Session): Config {
        return Config(session).apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        }
    }
}