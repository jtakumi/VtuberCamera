package com.example.vtubercamera.domain.ar

import android.util.Log
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
    
    companion object{
        private const val TAG = "ARFeature"
    }

    fun enableARMode(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
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
                            uiStateProvider = uiStateProvider,
                        )
                    },
                    { error ->
                        Log.e(TAG, "AR session initialization failed: $error")
                        updateUiState { copy(ar = ar.copy(arError = error)) }
                        disableARMode(
                            scope = scope,
                            updateUiState = updateUiState,
                            uiStateProvider = uiStateProvider,
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
            Log.d(TAG, "AR mode already disabled")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Disabling AR mode...")

                arRepository.destroySession()

                val currentAvatarState = uiStateProvider().avatarState
                updateUiState {
                    copy(
                        ar = ar.copy(
                            isARMode = false,
                            arSessionState = ARSessionState(),
                            arCameraState = ARCameraState.default(),
                            arError = null,
                        ),
                        avatar = avatar.copy(
                            avatarState = currentAvatarState.copy(
                                transform = Transform.identity(),
                                isVisible = false,
                            ),
                            avatarTransform = Transform.identity(),
                        ),
                        camera = camera.copy(
                            needsCameraRebind = true,
                        )
                    )
                }

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
            )
        } else {
            enableARMode(
                scope = scope,
                updateUiState = updateUiState,
                uiStateProvider = uiStateProvider,
                startSession = startSession,
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
                val currentState = uiStateProvider()
                if (currentState.avatarState.model != null) {
                    val shouldShow = trackingState == TrackingState.TRACKING
                    if (currentState.avatarState.isVisible != shouldShow) {
                        updateUiState {
                            copy(
                                avatar = avatar.copy(
                                    avatarState = currentState.avatarState.copy(isVisible = shouldShow)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
