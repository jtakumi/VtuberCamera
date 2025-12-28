package com.example.vtubercamera.domain.camera

import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import com.example.vtubercamera.ui.viewmodels.CameraUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias UiStateUpdater = (CameraUiState.() -> CameraUiState) -> Unit

typealias UiStateProvider = () -> CameraUiState

class CameraControlsFeature @Inject constructor() {

    private var boundCamera: Camera? = null

    fun setCamera(
        camera: Camera?,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        this.boundCamera = camera

        camera?.let { cam ->
            try {
                val zoomState = cam.cameraInfo.zoomState.value
                zoomState?.let { state ->
                    val actualMinZoom = state.minZoomRatio
                    val actualMaxZoom = state.maxZoomRatio

                    updateUiState {
                        copy(
                            camera = this.camera.copy(
                                minZoomRatio = actualMinZoom,
                                maxZoomRatio = actualMaxZoom,
                            ),
                        )
                    }

                    val currentZoom = uiStateProvider().zoomRatio
                    val adjustedZoom = when {
                        currentZoom < actualMinZoom -> actualMinZoom
                        currentZoom > actualMaxZoom -> actualMaxZoom
                        else -> currentZoom
                    }

                    if (adjustedZoom != currentZoom) {
                        updateUiState { copy(camera = this.camera.copy(zoomRatio = adjustedZoom)) }
                        cam.cameraControl.setZoomRatio(adjustedZoom)
                        Log.d("CameraViewModel", "Adjusted zoom from ${currentZoom}x to ${adjustedZoom}x")
                    }

                    Log.d("CameraViewModel", "Updated zoom range: ${actualMinZoom}x - ${actualMaxZoom}x")
                }
            } catch (e: Exception) {
                Log.w("CameraViewModel", "Failed to get zoom range from camera: ${e.message}")
            }
        }
    }

    fun switchCamera(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val newSelector = when (uiStateProvider().cameraSelector) {
            CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        updateUiState {
            copy(
                camera = this.camera.copy(
                    cameraSelector = newSelector,
                    needsCameraRebind = true,
                ),
            )
        }
    }

    fun toggleFlash(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val newFlashMode = when (uiStateProvider().flashMode) {
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_ON
        }
        updateUiState { copy(camera = this.camera.copy(flashMode = newFlashMode)) }
    }

    fun setZoom(
        zoom: Float,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val state = uiStateProvider()
        val minZoom = state.minZoomRatio
        val maxZoom = boundCamera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: state.maxZoomRatio
        val clampedZoom = zoom.coerceIn(minZoom, maxZoom)
        updateUiState { copy(camera = this.camera.copy(zoomRatio = clampedZoom)) }
        boundCamera?.cameraControl?.setZoomRatio(clampedZoom)
    }

    fun smoothZoomTo(
        targetZoom: Float,
        duration: Long,
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        val currentZoom = uiStateProvider().zoomRatio
        val animator = ValueAnimator.ofFloat(currentZoom, targetZoom)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val animatedZoom = animation.animatedValue as Float
            setZoom(
                zoom = animatedZoom,
                updateUiState = updateUiState,
                uiStateProvider = uiStateProvider,
            )
        }
        animator.start()
    }

    fun resetZoom(
        updateUiState: UiStateUpdater,
        uiStateProvider: UiStateProvider,
    ) {
        smoothZoomTo(
            targetZoom = 1.0f,
            duration = 300,
            updateUiState = updateUiState,
            uiStateProvider = uiStateProvider,
        )
    }

    fun focusOnPoint(
        previewView: PreviewView,
        x: Float,
        y: Float,
        scope: CoroutineScope,
        updateUiState: UiStateUpdater,
    ) {
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        boundCamera?.cameraControl?.startFocusAndMetering(action)

        updateUiState { copy(camera = this.camera.copy(focusPoint = Pair(x, y))) }
        scope.launch {
            delay(1000)
            updateUiState { copy(camera = this.camera.copy(focusPoint = null)) }
        }
    }
}
