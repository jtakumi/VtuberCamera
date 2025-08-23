package com.example.vtubercamera.ui.viewmodels

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.view.PreviewView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    private val _maxZoomRatio = MutableStateFlow(10.0f)
    val maxZoomRatio: StateFlow<Float> = _maxZoomRatio.asStateFlow()
    private val _minZoomRatio = MutableStateFlow(1.0f)
    val minZoomRatio: StateFlow<Float> = _minZoomRatio.asStateFlow()


    // カメラ再初期化フラグを追加
    private val _needsCameraRebind = MutableStateFlow(false)
    val needsCameraRebind: StateFlow<Boolean> = _needsCameraRebind.asStateFlow()

    // フォーカスポイントの状態管理
    private val _focusPoint = MutableStateFlow<Pair<Float, Float>?>(null)
    val focusPoint: StateFlow<Pair<Float, Float>?> = _focusPoint.asStateFlow()

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
    
    fun smoothZoomTo(targetZoom: Float, duration: Long = 300) {
        val currentZoom = _zoomRatio.value
        val animator = ValueAnimator.ofFloat(currentZoom, targetZoom)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val animatedZoom = animation.animatedValue as Float
            setZoom(animatedZoom)
        }
        animator.start()
    }
    
    fun resetZoom() {
        smoothZoomTo(1.0f)
    }

    fun focusOnPoint(previewView: PreviewView, x: Float, y: Float) {
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        _camera?.cameraControl?.startFocusAndMetering(action)
        
        // フォーカスポイントを設定し、1秒後に消す
        _focusPoint.value = Pair(x, y)
        viewModelScope.launch {
            delay(1000) // 1秒待機
            _focusPoint.value = null
        }
    }

    fun setMaxZoomRatio(maxZoomRatio: Float) {
        _maxZoomRatio.value = maxZoomRatio
    }
    fun setMinZoomRatio(minZoomRatio: Float) {
        _minZoomRatio.value = minZoomRatio
    }

    fun takePhoto(
        imageCapture: ImageCapture,
        context: Context,
        onPhotoSaved: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
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
    }

    fun clearLastCapturedImage(context: Context) {
        _lastCapturedImageUri.value?.let { uri ->
            context.let { ctx ->
                try {
                    ctx.contentResolver.delete(uri, null, null)
                    Log.d("CameraViewModel", "写真を削除しました: $uri")
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "写真の削除に失敗しました", e)
                }
            }
        }
        _lastCapturedImageUri.value = null
        _isPreviewMode.value = false
        // カメラ再バインドフラグを設定
        _needsCameraRebind.value = true
    }

    /**
     * 指定されたURIの写真をストレージから削除
     * @param context コンテキスト
     * @param uri 削除する写真のURI
     * @return 削除が成功したかどうか
     */
    fun deletePhoto(context: Context, uri: Uri): Boolean {
        return try {
            val deletedRows = context.contentResolver.delete(uri, null, null)
            val success = deletedRows > 0
            
            if (success) {
                Log.d("CameraViewModel", "写真を削除しました: $uri")
                // 削除した写真が現在表示中の写真と同じ場合は状態をクリア
                if (uri == _lastCapturedImageUri.value) {
                    _lastCapturedImageUri.value = null
                    _isPreviewMode.value = false
                    _needsCameraRebind.value = true
                }
            } else {
                Log.w("CameraViewModel", "写真の削除に失敗しました: $uri")
            }
            
            success
        } catch (e: Exception) {
            Log.e("CameraViewModel", "写真の削除中にエラーが発生しました", e)
            false
        }
    }

    /**
     * 複数の写真を一括削除
     * @param context コンテキスト
     * @param uris 削除する写真のURIリスト
     * @return 削除に成功した写真の数
     */
    fun deleteMultiplePhotos(context: Context, uris: List<Uri>): Int {
        var successCount = 0
        uris.forEach { uri ->
            if (deletePhoto(context, uri)) {
                successCount++
            }
        }
        return successCount
    }

    fun onCameraRebound() {
        // カメラが再バインドされたらフラグをリセット
        _needsCameraRebind.value = false
    }
}