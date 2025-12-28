package com.example.vtubercamera.domain.camera

import android.util.Log
import com.example.vtubercamera.ui.viewmodels.CameraUiState
import com.example.vtubercamera.utils.CameraCapabilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class LensSwitchFeature @Inject constructor(
    private val cameraCapabilityManager: CameraCapabilityManager,
) {

    fun initializeCameraCapabilities(
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        scope.launch {
            try {
                val success = cameraCapabilityManager.detectCameraCapabilities()
                if (success) {
                    val canSwitch = cameraCapabilityManager.canSwitchLens()
                    updateUiState { copy(lens = lens.copy(canSwitchLens = canSwitch)) }
                    updateLensDisplayInfo(updateUiState, uiStateProvider)
                    Log.d("CameraViewModel", "Camera capabilities initialized. Can switch lens: $canSwitch")
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to initialize camera capabilities", e)
            }
        }
    }

    fun updateLensDisplayInfo(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val currentSelector = uiStateProvider().cameraSelector
        val lensType = cameraCapabilityManager.getLensType(currentSelector)
        val lensName = cameraCapabilityManager.getLensDisplayName(currentSelector)
        updateUiState {
            copy(
                lens = lens.copy(
                    currentLensType = lensType,
                    lensDisplayName = lensName,
                )
            )
        }
    }

    fun switchLens(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        try {
            if (!uiStateProvider().canSwitchLens) {
                Log.w("CameraViewModel", "Lens switching not supported on this device")
                return
            }

            val currentSelector = uiStateProvider().cameraSelector
            val alternateSelector = cameraCapabilityManager.getAlternateRearCamera(currentSelector)

            if (alternateSelector != null && alternateSelector != currentSelector) {
                val previousLensName = uiStateProvider().lensDisplayName
                updateUiState { copy(camera = camera.copy(cameraSelector = alternateSelector)) }
                updateLensDisplayInfo(updateUiState, uiStateProvider)
                updateUiState { copy(camera = camera.copy(needsCameraRebind = true)) }

                val newLensName = uiStateProvider().lensDisplayName
                Log.d("CameraViewModel", "Switched from '$previousLensName' to '$newLensName'")
            } else {
                Log.w("CameraViewModel", "No alternate camera available or already using the alternate camera")
            }
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Failed to switch lens", e)
        }
    }

    fun isLensSwitchingAvailable(uiStateProvider: UiStateProvider): Boolean = uiStateProvider().canSwitchLens

    fun getCurrentLensInfo(uiStateProvider: UiStateProvider): String {
        val state = uiStateProvider()
        return "${state.lensDisplayName} (${state.currentLensType})"
    }
}
