package com.example.vtubercamera.ui.viewmodels

import android.animation.ValueAnimator
import android.net.Uri
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.PhotoItem
import com.example.vtubercamera.utils.CameraCapabilityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val mediaRepository: MediaRepository,
    private val cameraCapabilityManager: CameraCapabilityManager,
) : ViewModel() {

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

    // ギャラリー機能の状態管理
    private val _allPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val allPhotos: StateFlow<List<PhotoItem>> = _allPhotos.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.getAllPhotos().collect { photos ->
                _allPhotos.value = photos
            }
        }
        
        // Initialize camera capabilities
        initializeCameraCapabilities()
    }

    private val _isLoadingPhotos = MutableStateFlow(false)
    val isLoadingPhotos: StateFlow<Boolean> = _isLoadingPhotos.asStateFlow()

    private val _selectedPhotos = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedPhotos: StateFlow<Set<Uri>> = _selectedPhotos.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _currentViewingPhoto = MutableStateFlow<PhotoItem?>(null)
    val currentViewingPhoto: StateFlow<PhotoItem?> = _currentViewingPhoto.asStateFlow()

    // Lens switching capabilities
    private val _canSwitchLens = MutableStateFlow(false)
    val canSwitchLens: StateFlow<Boolean> = _canSwitchLens.asStateFlow()

    private val _currentLensType = MutableStateFlow(CameraCapabilityManager.LensType.NORMAL)
    val currentLensType: StateFlow<CameraCapabilityManager.LensType> = _currentLensType.asStateFlow()

    private val _lensDisplayName = MutableStateFlow("Camera")
    val lensDisplayName: StateFlow<String> = _lensDisplayName.asStateFlow()

    private var _camera: Camera? = null

    fun setCamera(camera: Camera?) {
        _camera = camera
        
        // Update zoom range based on actual camera capabilities
        camera?.let { cam ->
            try {
                val zoomState = cam.cameraInfo.zoomState.value
                zoomState?.let { state ->
                    val actualMinZoom = state.minZoomRatio
                    val actualMaxZoom = state.maxZoomRatio
                    
                    _minZoomRatio.value = actualMinZoom
                    _maxZoomRatio.value = actualMaxZoom
                    
                    // Adjust current zoom to fit within the new range
                    val currentZoom = _zoomRatio.value
                    val adjustedZoom = when {
                        currentZoom < actualMinZoom -> actualMinZoom
                        currentZoom > actualMaxZoom -> actualMaxZoom
                        else -> currentZoom
                    }
                    
                    if (adjustedZoom != currentZoom) {
                        _zoomRatio.value = adjustedZoom
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

    fun switchCamera() {
        _cameraSelector.value = when (_cameraSelector.value) {
            CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        // カメラ切り替え時に再バインドが必要
        _needsCameraRebind.value = true
    }

    fun toggleFlash() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_ON
        }
    }

    fun setZoom(zoom: Float) {
        val minZoom = _minZoomRatio.value
        val maxZoom = _camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: _maxZoomRatio.value
        _zoomRatio.value = zoom.coerceIn(minZoom, maxZoom)
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
        onPhotoSaved: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            cameraRepository.capturePhoto(
                imageCapture = imageCapture,
                onPhotoSaved = { uri ->
                    val msg = "写真を保存しました: $uri"
                    _lastCapturedImageUri.value = uri
                    onPhotoSaved(msg)
                    refreshPhotos()
                },
                onError = onError
            )
        }
    }

    fun enterPreviewMode() {
        _isPreviewMode.value = true
    }

    fun exitPreviewMode() {
        _isPreviewMode.value = false
        // カメラ再バインドフラグを設定
        _needsCameraRebind.value = true
    }

    fun clearLastCapturedImage() {
        _lastCapturedImageUri.value?.let { uri ->
            viewModelScope.launch {
                try {
                    mediaRepository.deletePhoto(uri)
                    Log.d("CameraViewModel", "写真を削除しました: $uri")
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "写真の削除に失敗しました", e)
                }
            }
        }
        _lastCapturedImageUri.value = null
        _isPreviewMode.value = false
        _needsCameraRebind.value = true
    }

    /**
     * 指定されたURIの写真をストレージから削除
     * @param uri 削除する写真のURI
     * @return 削除が成功したかどうか
     */
    fun deletePhoto(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = mediaRepository.deletePhoto(uri)
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
                onResult(success)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "写真の削除中にエラーが発生しました", e)
                onResult(false)
            }
        }
    }

    /**
     * 複数の写真を一括削除
     * @param uris 削除する写真のURIリスト
     * @return 削除に成功した写真の数
     */
    fun deleteMultiplePhotos(uris: List<Uri>, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val successCount = mediaRepository.deleteMultiplePhotos(uris)
                if (successCount > 0) {
                    clearSelection()
                }
                onResult(successCount)
            } catch (e: Exception) {
                Log.e("CameraViewModel", "複数写真の削除中にエラーが発生しました", e)
                onResult(0)
            }
        }
    }

    /**
     * 端末内の全ての写真を取得
     */
    fun refreshPhotos() {
        viewModelScope.launch {
            _isLoadingPhotos.value = true
            try {
                mediaRepository.refreshPhotos()
                // Update last captured image URI with the latest photo
                val photos = allPhotos.value
                _lastCapturedImageUri.value = photos.firstOrNull()?.uri
            } catch (_: Exception) {
            } finally {
                _isLoadingPhotos.value = false
            }
        }
    }

    /**
     * MediaStoreから全ての写真を取得
     */

    /**
     * 写真の選択状態を切り替え
     */
    fun togglePhotoSelection(uri: Uri) {
        val currentSelection = _selectedPhotos.value.toMutableSet()
        if (currentSelection.contains(uri)) {
            currentSelection.remove(uri)
        } else {
            currentSelection.add(uri)
        }
        _selectedPhotos.value = currentSelection

        // 選択がなくなったら選択モードを終了
        if (currentSelection.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    /**
     * 選択モードを開始
     */
    fun startSelectionMode() {
        _isSelectionMode.value = true
        _selectedPhotos.value = emptySet()
    }

    /**
     * 選択モードを終了
     */
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedPhotos.value = emptySet()
        // ギャラリーから戻った時にカメラを再バインド
        _needsCameraRebind.value = true
    }

    /**
     * 全選択/全選択解除
     */
    fun toggleSelectAll() {
        val photosList = allPhotos.value
        if (_selectedPhotos.value.size == photosList.size) {
            // 全選択されている場合は全選択解除
            _selectedPhotos.value = emptySet()
        } else {
            // そうでなければ全選択
            _selectedPhotos.value = photosList.map { it.uri }.toSet()
        }
    }

    /**
     * 選択をクリア
     */
    fun clearSelection() {
        _selectedPhotos.value = emptySet()
        _isSelectionMode.value = false
    }

    /**
     * 選択された写真を削除
     */
    fun deleteSelectedPhotos(onResult: (Int) -> Unit) {
        val selectedUris = _selectedPhotos.value.toList()
        deleteMultiplePhotos(selectedUris, onResult)
    }

    /**
     * 特定の写真を拡大表示用に設定
     */
    fun setCurrentViewingPhoto(photo: PhotoItem?) {
        _currentViewingPhoto.value = photo
        // 写真詳細ビューから戻った時にカメラを再バインド
        if (photo == null) {
            _needsCameraRebind.value = true
        }
    }

    /**
     * 次の写真に移動
     */
    fun goToNextPhoto() {
        val currentPhoto = _currentViewingPhoto.value ?: return
        val photosList = allPhotos.value
        val currentIndex = photosList.indexOfFirst { it.id == currentPhoto.id }
        if (currentIndex >= 0 && currentIndex < photosList.size - 1) {
            _currentViewingPhoto.value = photosList[currentIndex + 1]
        }
    }

    /**
     * 前の写真に移動
     */
    fun goToPreviousPhoto() {
        val currentPhoto = _currentViewingPhoto.value ?: return
        val photosList = allPhotos.value
        val currentIndex = photosList.indexOfFirst { it.id == currentPhoto.id }
        if (currentIndex > 0) {
            _currentViewingPhoto.value = photosList[currentIndex - 1]
        }
    }

    fun onCameraRebound() {
        // カメラが再バインドされたらフラグをリセット
        _needsCameraRebind.value = false
    }

    fun initializePhotos() {
        refreshPhotos()
    }

    private fun initializeCameraCapabilities() {
        viewModelScope.launch {
            try {
                val success = cameraCapabilityManager.detectCameraCapabilities()
                if (success) {
                    _canSwitchLens.value = cameraCapabilityManager.canSwitchLens()
                    updateLensDisplayInfo()
                    Log.d("CameraViewModel", "Camera capabilities initialized. Can switch lens: ${_canSwitchLens.value}")
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to initialize camera capabilities", e)
            }
        }
    }

    private fun updateLensDisplayInfo() {
        _currentLensType.value = cameraCapabilityManager.getLensType(_cameraSelector.value)
        _lensDisplayName.value = cameraCapabilityManager.getLensDisplayName(_cameraSelector.value)
    }

    /**
     * Switch between normal and wide-angle lenses using pinch gesture with error handling
     */
    fun switchLens() {
        try {
            if (!_canSwitchLens.value) {
                Log.w("CameraViewModel", "Lens switching not supported on this device")
                return
            }

            val currentSelector = _cameraSelector.value
            val alternateSelector = cameraCapabilityManager.getAlternateRearCamera(currentSelector)
            
            if (alternateSelector != null && alternateSelector != currentSelector) {
                val previousLensName = _lensDisplayName.value
                _cameraSelector.value = alternateSelector
                updateLensDisplayInfo()
                _needsCameraRebind.value = true
                
                Log.d("CameraViewModel", "Switched from '$previousLensName' to '${_lensDisplayName.value}'")
            } else {
                Log.w("CameraViewModel", "No alternate camera available or already using the alternate camera")
            }
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Failed to switch lens", e)
        }
    }

    /**
     * Check if device supports multiple rear cameras and lens switching
     */
    fun isLensSwitchingAvailable(): Boolean = _canSwitchLens.value

    /**
     * Get current lens information for debugging
     */
    fun getCurrentLensInfo(): String {
        return "${_lensDisplayName.value} (${_currentLensType.value})"
    }

    /**
     * Get camera switch callback for use in camera binding
     */
    fun getCameraSwitchCallback(): (
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        cameraProvider: androidx.camera.lifecycle.ProcessCameraProvider,
        imageCapture: androidx.camera.core.ImageCapture,
        onCamera: (Camera) -> Unit
    ) -> Camera? {
        return { lifecycleOwner, cameraProvider, imageCapture, onCamera ->
            cameraRepository.switchToCamera(
                lifecycleOwner = lifecycleOwner,
                cameraProvider = cameraProvider,
                newCameraSelector = _cameraSelector.value,
                flashMode = _flashMode.value,
                onImageCaptureCreated = { imageCapture },
                onCameraCreated = onCamera
            )
        }
    }
}
