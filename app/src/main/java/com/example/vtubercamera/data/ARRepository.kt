package com.example.vtubercamera.data

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError

interface ARRepository {
    
    fun initializeSession(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        onSessionReady: () -> Unit,
        onError: (ARError) -> Unit
    )
    
    fun pauseSession()
    
    fun resumeSession()
    
    fun destroySession()
    
    val sessionState: Flow<ARSessionState>
    
    val cameraState: Flow<ARCameraState>
    
    val trackingState: Flow<com.example.vtubercamera.data.vrm.TrackingState>
    
    val lightEstimate: Flow<com.example.vtubercamera.data.vrm.LightEstimate?>
    
    fun enablePlaneDetection(enabled: Boolean)
    
    fun enableEnvironmentalHDR(enabled: Boolean)
    
    fun enableLightEstimation(enabled: Boolean)
    
    fun isSessionInitialized(): Boolean
    
    fun getCurrentTrackingState(): com.example.vtubercamera.data.vrm.TrackingState
    
    fun getCurrentCameraState(): ARCameraState
    
    fun getCurrentLightEstimate(): com.example.vtubercamera.data.vrm.LightEstimate?
    
    fun setTrackingStateListener(listener: (com.example.vtubercamera.data.vrm.TrackingState) -> Unit)
    
    fun removeTrackingStateListener()
    
    fun configureSession(
        planeDetectionEnabled: Boolean = true,
        lightEstimationEnabled: Boolean = true,
        environmentalHDREnabled: Boolean = false
    )
    
    suspend fun waitForTracking(timeoutMs: Long = 5000): Boolean

    // Phase 3 additions
    // Expose underlying ARCore session for renderer initialization
    fun getSession(): com.google.ar.core.Session?

    // Propagate per-frame updates into repository state
    fun updateFromFrame(frame: com.google.ar.core.Frame)
}
