package com.example.vtubercamera.ui.viewmodels

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

class CameraViewModel : ViewModel() {
    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector.asStateFlow()

    private val _lastCapturedImageUri = MutableStateFlow<Uri?>(null)
    val lastCapturedImageUri: StateFlow<Uri?> = _lastCapturedImageUri.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    // カメラ再初期化フラグを追加
    private val _needsCameraRebind = MutableStateFlow(false)
    val needsCameraRebind: StateFlow<Boolean> = _needsCameraRebind.asStateFlow()

    private var _camera: Camera? = null

    fun setCamera(camera: Camera?) {
        _camera = camera
    }

    fun switchCamera() {
        _cameraSelector.value = when (_cameraSelector.value) {
            CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun toggleFlash() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_ON
        }
    }

    fun setZoom(zoom: Float) {
        _zoomRatio.value =
            zoom.coerceIn(1.0f, _camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1.0f)
        _camera?.cameraControl?.setZoomRatio(_zoomRatio.value)
    }

    fun takePhoto(
        imageCapture: ImageCapture,
        context: Context,
        onPhotoSaved: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.JAPAN)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VTuberCamera")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "写真を保存しました: ${output.savedUri}"
                    Log.d("Camera", msg)
                    _lastCapturedImageUri.value = output.savedUri
                    onPhotoSaved(msg)
                }

                override fun onError(exc: ImageCaptureException) {
                    val msg = "写真の保存に失敗しました"
                    Log.e("Camera", msg, exc)
                    onError(msg)
                }
            }
        )
    }

    fun enterPreviewMode() {
        _isPreviewMode.value = true
    }

    fun exitPreviewMode() {
        _isPreviewMode.value = false
        // カメラ再バインドフラグを設定
        _needsCameraRebind.value = true
        Log.d("CameraViewModel", "Exiting preview mode, requesting camera rebind")
    }

    fun clearLastCapturedImage() {
        _lastCapturedImageUri.value = null
        _isPreviewMode.value = false
        // カメラ再バインドフラグを設定
        _needsCameraRebind.value = true
    }

    fun onCameraRebound() {
        // カメラが再バインドされたらフラグをリセット
        _needsCameraRebind.value = false
    }
}