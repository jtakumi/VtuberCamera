package com.example.vtubercamera.managers

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.vtubercamera.data.*
import com.example.vtubercamera.data.vrm.*
import com.example.vtubercamera.utils.ARDeviceCompatibility
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ARSessionManager @Inject constructor(
    private val arRepository: ARRepository,
    private val arPermissionManager: ARPermissionManager,
    private val arFallbackManager: ARFallbackManager,
    private val arTrackingMonitor: ARTrackingMonitor
) {
    
    private companion object {
        private const val TAG = "ARSessionManager"
    }
    
    enum class SessionState {
        UNINITIALIZED,
        CHECKING_COMPATIBILITY,
        REQUESTING_PERMISSIONS,
        INSTALLING_ARCORE,
        INITIALIZING,
        READY,
        FALLBACK_ACTIVE,
        ERROR,
        DESTROYED
    }
    
    data class ARSessionInfo(
        val state: SessionState,
        val compatibilityResult: ARDeviceCompatibility.CompatibilityResult? = null,
        val fallbackConfig: ARFallbackManager.FallbackConfig? = null,
        val permissionsGranted: Boolean = false,
        val arCoreInstalled: Boolean = false,
        val errorMessage: String? = null,
        val activeRepository: ARRepository? = null
    )
    
    private val _sessionInfo = MutableStateFlow(
        ARSessionInfo(state = SessionState.UNINITIALIZED)
    )
    val sessionInfo: StateFlow<ARSessionInfo> = _sessionInfo.asStateFlow()
    
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()
    
    private var currentScope: CoroutineScope? = null
    private var compatibilityResult: ARDeviceCompatibility.CompatibilityResult? = null
    
    suspend fun initializeARSession(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        scope: CoroutineScope
    ): Result<ARRepository> {
        currentScope = scope
        
        try {
            Log.d(TAG, "Starting AR session initialization")
            
            updateSessionState(SessionState.CHECKING_COMPATIBILITY)
            val compatibility = checkDeviceCompatibility(context)
            
            updateSessionState(SessionState.REQUESTING_PERMISSIONS)
            val permissionsGranted = requestPermissions()
            
            if (!permissionsGranted) {
                return Result.failure(Exception("Required permissions not granted"))
            }
            
            val repository = when {
                compatibility.isARCoreSupported -> {
                    initializeARCore(context, lifecycleOwner, compatibility)
                }
                else -> {
                    initializeFallback(context, compatibility)
                }
            }
            
            startTrackingMonitor(scope)
            updateSessionState(SessionState.READY)
            _isSessionActive.value = true
            
            Log.i(TAG, "AR session initialized successfully")
            return Result.success(repository)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AR session", e)
            updateSessionState(SessionState.ERROR, errorMessage = e.message)
            return Result.failure(e)
        }
    }
    
    private suspend fun checkDeviceCompatibility(context: Context): ARDeviceCompatibility.CompatibilityResult {
        Log.d(TAG, "Checking device compatibility")
        
        val result = ARDeviceCompatibility.checkDeviceCompatibility(context)
        compatibilityResult = result
        
        _sessionInfo.value = _sessionInfo.value.copy(compatibilityResult = result)
        
        val compatibilityLevel = ARDeviceCompatibility.getCompatibilityLevel(result)
        Log.i(TAG, "Device compatibility level: $compatibilityLevel")
        
        return result
    }
    
    private suspend fun requestPermissions(): Boolean {
        Log.d(TAG, "Requesting AR permissions")
        
        val permissionResult = arPermissionManager.requestARPermissionsAsync()
        val permissionsGranted = permissionResult.granted
        
        _sessionInfo.value = _sessionInfo.value.copy(permissionsGranted = permissionsGranted)
        
        if (!permissionsGranted) {
            Log.w(TAG, "AR permissions not granted: ${permissionResult.deniedPermissions}")
        }
        
        return permissionsGranted
    }
    
    private suspend fun initializeARCore(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        compatibility: ARDeviceCompatibility.CompatibilityResult
    ): ARRepository {
        Log.d(TAG, "Initializing ARCore")
        
        if (!compatibility.isARCoreInstalled) {
            updateSessionState(SessionState.INSTALLING_ARCORE)
            val installResult = arPermissionManager.checkAndInstallARCoreAsync()
            
            if (!installResult.installed) {
                throw Exception("ARCore installation failed: ${installResult.error}")
            }
        }
        
        updateSessionState(SessionState.INITIALIZING)
        
        return suspendCancellableCoroutine { continuation ->
            arRepository.initializeSession(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onSessionReady = {
                    Log.d(TAG, "ARCore session ready")
                    _sessionInfo.value = _sessionInfo.value.copy(
                        arCoreInstalled = true,
                        activeRepository = arRepository
                    )
                    continuation.resume(arRepository)
                },
                onError = { error ->
                    Log.e(TAG, "ARCore initialization failed: $error")
                    continuation.resumeWithException(Exception("ARCore initialization failed: $error"))
                }
            )
        }
    }
    
    private suspend fun initializeFallback(
        context: Context,
        compatibility: ARDeviceCompatibility.CompatibilityResult
    ): ARRepository {
        Log.d(TAG, "Initializing fallback mode")
        
        updateSessionState(SessionState.FALLBACK_ACTIVE)
        
        val fallbackConfig = arFallbackManager.determineFallbackMode(context, compatibility)
        arFallbackManager.activateFallback(fallbackConfig)
        
        val fallbackRepository = arFallbackManager.getFallbackARRepository()
            ?: throw Exception("Failed to create fallback AR repository")
        
        _sessionInfo.value = _sessionInfo.value.copy(
            fallbackConfig = fallbackConfig,
            activeRepository = fallbackRepository
        )
        
        return fallbackRepository
    }
    
    private fun startTrackingMonitor(scope: CoroutineScope) {
        Log.d(TAG, "Starting AR tracking monitor")
        arTrackingMonitor.startMonitoring(scope)
        
        scope.launch {
            arTrackingMonitor.trackingEvents.collect { event ->
                handleTrackingEvent(event)
            }
        }
    }
    
    private fun handleTrackingEvent(event: ARTrackingMonitor.TrackingEvent) {
        when (event) {
            is ARTrackingMonitor.TrackingEvent.TrackingLost -> {
                Log.w(TAG, "AR tracking lost")
            }
            is ARTrackingMonitor.TrackingEvent.TrackingRecovered -> {
                Log.i(TAG, "AR tracking recovered")
            }
            is ARTrackingMonitor.TrackingEvent.TrackingTimeout -> {
                Log.e(TAG, "AR tracking timeout after ${event.durationMs}ms")
            }
            is ARTrackingMonitor.TrackingEvent.TrackingError -> {
                Log.e(TAG, "AR tracking error: ${event.error}")
            }
            else -> {
                Log.d(TAG, "AR tracking event: $event")
            }
        }
    }
    
    fun pauseSession() {
        Log.d(TAG, "Pausing AR session")
        _sessionInfo.value.activeRepository?.pauseSession()
        arTrackingMonitor.stopMonitoring()
    }
    
    fun resumeSession() {
        Log.d(TAG, "Resuming AR session")
        _sessionInfo.value.activeRepository?.resumeSession()
        currentScope?.let { arTrackingMonitor.startMonitoring(it) }
    }
    
    fun destroySession() {
        Log.d(TAG, "Destroying AR session")
        
        _sessionInfo.value.activeRepository?.destroySession()
        arTrackingMonitor.stopMonitoring()
        arFallbackManager.deactivateFallback()
        
        updateSessionState(SessionState.DESTROYED)
        _isSessionActive.value = false
        currentScope = null
        compatibilityResult = null
    }
    
    fun getCurrentRepository(): ARRepository? = _sessionInfo.value.activeRepository
    
    fun getCompatibilityReport(): ARFallbackManager.CompatibilityReport? {
        val currentInfo = _sessionInfo.value
        val compatibility = currentInfo.compatibilityResult
        val fallbackConfig = currentInfo.fallbackConfig
        
        return if (compatibility != null && fallbackConfig != null) {
            arFallbackManager.createCompatibilityReport(compatibility, fallbackConfig)
        } else {
            null
        }
    }
    
    fun isARModeActive(): Boolean {
        return _sessionInfo.value.state == SessionState.READY && 
               !arFallbackManager.isFallbackActive.value
    }
    
    fun isFallbackModeActive(): Boolean {
        return _sessionInfo.value.state == SessionState.FALLBACK_ACTIVE || 
               arFallbackManager.isFallbackActive.value
    }
    
    fun getRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        compatibilityResult?.let { compatibility ->
            recommendations.addAll(
                ARDeviceCompatibility.getPerformanceOptimizationSuggestions(compatibility)
            )
        }
        
        _sessionInfo.value.fallbackConfig?.let { fallbackConfig ->
            recommendations.addAll(fallbackConfig.recommendations)
        }
        
        return recommendations
    }
    
    fun getLimitations(): List<String> {
        return _sessionInfo.value.fallbackConfig?.limitations ?: emptyList()
    }
    
    private fun updateSessionState(
        newState: SessionState,
        errorMessage: String? = null
    ) {
        Log.d(TAG, "Session state: ${_sessionInfo.value.state} -> $newState")
        
        _sessionInfo.value = _sessionInfo.value.copy(
            state = newState,
            errorMessage = errorMessage
        )
    }
    
    fun handlePermissionResult(result: ARPermissionManager.PermissionResult) {
        if (!result.granted) {
            val message = if (result.permanentlyDeniedPermissions.isNotEmpty()) {
                "権限が永続的に拒否されました。設定から手動で許可してください。"
            } else {
                "AR機能に必要な権限が許可されませんでした。"
            }
            updateSessionState(SessionState.ERROR, errorMessage = message)
        }
    }
    
    fun handleARCoreInstallResult(result: ARPermissionManager.ARInstallResult) {
        if (!result.installed) {
            updateSessionState(SessionState.ERROR, errorMessage = "ARCoreのインストールに失敗しました: ${result.error}")
        }
    }
}