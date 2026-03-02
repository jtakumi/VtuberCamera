package com.example.vtubercamera.domain.camera

import androidx.camera.core.ImageCapture
import com.example.vtubercamera.data.ARPhotoMetadata
import com.example.vtubercamera.data.CameraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class CameraCaptureFeature @Inject constructor(
    private val cameraRepository: CameraRepository,
) {

    fun capturePhoto(
        imageCapture: ImageCapture,
        scope: CoroutineScope,
        onPhotoSaved: (android.net.Uri) -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            try {
                cameraRepository.capturePhoto(
                    imageCapture = imageCapture,
                    onPhotoSaved = onPhotoSaved,
                    onError = onError,
                )
            } catch (e: Exception) {
                onError(e.message ?: "Failed to capture photo. Please try again.")
            }
        }
    }

    fun captureARPhoto(
        imageCapture: ImageCapture,
        arMetadata: ARPhotoMetadata,
        scope: CoroutineScope,
        onPhotoSaved: (android.net.Uri) -> Unit,
        onError: (String) -> Unit,
    ) {
        scope.launch {
            try {
                cameraRepository.captureARPhoto(
                    imageCapture = imageCapture,
                    arMetadata = arMetadata,
                    onPhotoSaved = onPhotoSaved,
                    onError = onError,
                )
            } catch (e: Exception) {
                onError(e.message ?: "Failed to capture AR photo. Please try again.")
            }
        }
    }
}
