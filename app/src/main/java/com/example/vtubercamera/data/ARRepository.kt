package com.example.vtubercamera.data

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.TrackingState
import com.example.vtubercamera.data.vrm.LightEstimate

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
    
    val trackingState: Flow<TrackingState>
    
    val lightEstimate: Flow<LightEstimate?>
    
    fun enablePlaneDetection(enabled: Boolean)
    
    fun enableEnvironmentalHDR(enabled: Boolean)
    
    fun enableLightEstimation(enabled: Boolean)
    
    fun isSessionInitialized(): Boolean
    
    fun getCurrentTrackingState(): TrackingState
    
    fun getCurrentCameraState(): ARCameraState
    
    fun getCurrentLightEstimate(): LightEstimate?
    
    fun setTrackingStateListener(listener: (TrackingState) -> Unit)
    
    fun removeTrackingStateListener()
    
    fun configureSession(
        planeDetectionEnabled: Boolean = true,
        lightEstimationEnabled: Boolean = true,
        environmentalHDREnabled: Boolean = false
    )
    
    suspend fun waitForTracking(timeoutMs: Long = 5000): Boolean
}