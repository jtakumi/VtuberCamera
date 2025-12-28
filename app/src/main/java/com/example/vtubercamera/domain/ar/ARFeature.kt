package com.example.vtubercamera.domain.ar

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.TrackingState
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.domain.camera.UiStateProvider
import com.example.vtubercamera.domain.camera.UiStateUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ARFeature @Inject constructor(
    private val arRepository: ARRepository,
) {

    fun enableARMode(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        if (uiStateProvider().isARMode) {
            Log.d("CameraViewModel", "AR mode already enabled")
            return
        }

        scope.launch {
            try {
                Log.d("CameraViewModel", "Enabling AR mode...")
                updateUiState {
                    copy(
                        isARMode = true,
                        needsCameraRebind = true,
                    )
                }

                arRepository.initializeSession(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onSessionReady = {
                        Log.d("CameraViewModel", "AR session ready")
                        observeARStates(
                            scope = scope,
                            updateUiState = updateUiState,
                            uiStateProvider = uiStateProvider,
                        )
                    },
                    onError = { error ->
                        Log.e("CameraViewModel", "AR session initialization failed: $error")
                        updateUiState { copy(arError = error) }
                        disableARMode(
                            scope = scope,
                            updateUiState = updateUiState,
                            uiStateProvider = uiStateProvider,
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to enable AR mode", e)
                updateUiState {
                    copy(arError = ARError.SessionError("Failed to enable AR mode: ${e.message}"))
                }
                disableARMode(
                    scope = scope,
                    updateUiState = updateUiState,
                    uiStateProvider = uiStateProvider,
                )
            }
        }
    }

    fun disableARMode(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        if (!uiStateProvider().isARMode) {
            Log.d("CameraViewModel", "AR mode already disabled")
            return
        }

        scope.launch {
            try {
                Log.d("CameraViewModel", "Disabling AR mode...")

                arRepository.destroySession()

                val currentAvatarState = uiStateProvider().avatarState
                updateUiState {
                    copy(
                        isARMode = false,
                        arSessionState = ARSessionState(),
                        arCameraState = ARCameraState.default(),
                        arError = null,
                        avatarState = currentAvatarState.copy(
                            transform = Transform.identity(),
                            isVisible = false,
                        ),
                        avatarTransform = Transform.identity(),
                        needsCameraRebind = true,
                    )
                }

                Log.d("CameraViewModel", "AR mode disabled")
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error disabling AR mode", e)
            }
        }
    }

    fun toggleARMode(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        if (uiStateProvider().isARMode) {
            disableARMode(
                scope = scope,
                updateUiState = updateUiState,
                uiStateProvider = uiStateProvider,
            )
        } else {
            enableARMode(
                context = context,
                lifecycleOwner = lifecycleOwner,
                scope = scope,
                updateUiState = updateUiState,
                uiStateProvider = uiStateProvider,
            )
        }
    }

    private fun observeARStates(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            arRepository.sessionState.collect { sessionState ->
                updateUiState { copy(arSessionState = sessionState) }
            }
        }

        scope.launch {
            arRepository.cameraState.collect { cameraState ->
                updateUiState { copy(arCameraState = cameraState) }
            }
        }

        scope.launch {
            arRepository.trackingState.collect { trackingState ->
                Log.d("CameraViewModel", "AR tracking state changed: $trackingState")
                val currentState = uiStateProvider()
                if (currentState.avatarState.model != null) {
                    val shouldShow = trackingState == TrackingState.TRACKING
                    if (currentState.avatarState.isVisible != shouldShow) {
                        updateUiState {
                            copy(
                                avatarState = currentState.avatarState.copy(isVisible = shouldShow)
                            )
                        }
                    }
                }
            }
        }
    }
}
