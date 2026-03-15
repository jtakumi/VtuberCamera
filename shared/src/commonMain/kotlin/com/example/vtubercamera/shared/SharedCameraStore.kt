package com.example.vtubercamera.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * PoC state store shared by Android/iOS.
 */
class SharedCameraStore {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun switchCamera() {
        _uiState.update { state ->
            state.copy(
                currentLens = if (state.currentLens == LensType.BACK) LensType.FRONT else LensType.BACK
            )
        }
    }

    fun setFlashEnabled(enabled: Boolean) {
        _uiState.update { state -> state.copy(isFlashEnabled = enabled) }
    }

    fun markCaptured() {
        _uiState.update { state -> state.copy(captureCount = state.captureCount + 1) }
    }
}

data class CameraUiState(
    val currentLens: LensType = LensType.BACK,
    val isFlashEnabled: Boolean = false,
    val captureCount: Int = 0
)

enum class LensType {
    FRONT,
    BACK
}
