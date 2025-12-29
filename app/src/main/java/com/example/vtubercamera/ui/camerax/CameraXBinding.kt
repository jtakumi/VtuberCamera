package com.example.vtubercamera.ui.camerax

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner

internal fun bindCameraWithPreview(
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    preview: Preview,
    cameraSelector: CameraSelector,
    flashMode: Int,
    initialZoomRatio: Float? = null,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCameraCreated: (Camera) -> Unit
): Camera? {
    return try {
        Log.d(
            "CameraBinding",
            "Binding camera with flash mode: $flashMode, camera: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}"
        )

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()

        cameraProvider.unbindAll()

        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        onImageCaptureCreated(imageCapture)
        onCameraCreated(camera)

        initialZoomRatio?.let { targetZoom ->
            try {
                val minZoom = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f
                val maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 10.0f
                val coerced = targetZoom.coerceIn(minZoom, maxZoom)
                camera.cameraControl.setZoomRatio(coerced)
                Log.d("CameraBinding", "Restored zoom ratio to $coerced after rebind")
            } catch (e: Exception) {
                Log.w("CameraBinding", "ズーム復元に失敗しました", e)
            }
        }

        Log.d("CameraBinding", "Camera bound successfully")

        camera
    } catch (e: Exception) {
        Log.e("CameraBinding", "カメラのバインドに失敗しました", e)
        null
    }
}
