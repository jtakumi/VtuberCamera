package com.example.vtubercamera.domain.ar

import android.util.Log
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.TrackingState
import com.example.vtubercamera.domain.camera.UiStateProvider
import com.example.vtubercamera.domain.camera.UiStateUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ARFeature @Inject constructor(
    private val arRepository: ARRepository,
) {
    
    companion object{
        private const val TAG = "ARFeature"
    }

    fun enableARMode(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
        onTrackingStateChanged: (TrackingState) -> Unit,
        startSession: suspend (
            onSessionReady: () -> Unit,
            onError: (ARError) -> Unit,
        ) -> Unit,
    ) {
        if (uiStateProvider().isARMode) {
            Log.d(TAG, "AR mode already enabled")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Enabling AR mode...")
                updateUiState {
                    copy(
                        ar = ar.copy(
                            isARMode = true,
                        ),
                        camera = camera.copy(
                            needsCameraRebind = true,
                        )
                    )
                }

                startSession(
                    {
                        Log.d(TAG, "AR session ready")
                        observeARStates(
                            scope = scope,
                            updateUiState = updateUiState,
                            onTrackingStateChanged = onTrackingStateChanged,
                        )
                    },
                    { error ->
                        Log.e(TAG, "AR session initialization failed: $error")
                        updateUiState { copy(ar = ar.copy(arError = error)) }
                        disableARMode(
                            scope = scope,
                            updateUiState = updateUiState,
                            uiStateProvider = uiStateProvider,
                            resetAvatarState = {},
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable AR mode", e)
                updateUiState {
                    copy(ar = ar.copy(arError = ARError.SessionError("Failed to enable AR mode: ${e.message}")))
                }
                disableARMode(
                    scope = scope,
                    updateUiState = updateUiState,
                    uiStateProvider = uiStateProvider,
                    resetAvatarState = {},
                )
            }
        }
    }

    fun disableARMode(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
        resetAvatarState: () -> Unit,
    ) {
        if (!uiStateProvider().isARMode) {
            Log.d(TAG, "AR mode already disabled")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Disabling AR mode...")

                arRepository.destroySession()

                updateUiState {
                    copy(
                        ar = ar.copy(
                            isARMode = false,
                            arSessionState = ARSessionState(),
                            arCameraState = ARCameraState.default(),
                            arError = null,
                        ),
                        camera = camera.copy(
                            needsCameraRebind = true,
                        )
                    )
                }
                resetAvatarState()

                Log.d(TAG, "AR mode disabled")
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling AR mode", e)
            }
        }
    }

    fun toggleARMode(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
        onTrackingStateChanged: (TrackingState) -> Unit,
        resetAvatarState: () -> Unit,
        startSession: suspend (
            onSessionReady: () -> Unit,
            onError: (ARError) -> Unit,
        ) -> Unit,
    ) {
        if (uiStateProvider().isARMode) {
            disableARMode(
                scope = scope,
                updateUiState = updateUiState,
                uiStateProvider = uiStateProvider,
                resetAvatarState = resetAvatarState,
            )
        } else {
            enableARMode(
                scope = scope,
                updateUiState = updateUiState,
                uiStateProvider = uiStateProvider,
                onTrackingStateChanged = onTrackingStateChanged,
                startSession = startSession,
            )
        }
    }

    private fun observeARStates(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        onTrackingStateChanged: (TrackingState) -> Unit,
    ) {
        scope.launch {
            arRepository.sessionState.collect { sessionState ->
                updateUiState { copy(ar = ar.copy(arSessionState = sessionState)) }
            }
        }

        scope.launch {
            arRepository.cameraState.collect { cameraState ->
                updateUiState { copy(ar = ar.copy(arCameraState = cameraState)) }
            }
        }

        scope.launch {
            arRepository.trackingState.collect { trackingState ->
                Log.d(TAG, "AR tracking state changed: $trackingState")
                onTrackingStateChanged(trackingState)
            }
        }
    }
}
