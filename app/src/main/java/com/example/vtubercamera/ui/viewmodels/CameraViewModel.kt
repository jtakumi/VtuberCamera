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
import com.example.vtubercamera.data.ARRepository
import com.example.vtubercamera.data.VRMRepository
import com.example.vtubercamera.data.vrm.AvatarState
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.VRMModel
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.data.vrm.AvatarLibraryManager
import com.example.vtubercamera.data.vrm.AvatarLibraryStats
import com.example.vtubercamera.data.vrm.AvatarSortBy
import com.example.vtubercamera.data.vrm.AvatarController
import com.example.vtubercamera.data.vrm.ExpressionController
import com.example.vtubercamera.data.vrm.PoseController
import com.example.vtubercamera.data.vrm.LightingSystem
import com.example.vtubercamera.data.vrm.LightingSettings
import com.example.vtubercamera.data.vrm.LightingPreset
import com.example.vtubercamera.data.vrm.EnvironmentLighting
import com.example.vtubercamera.utils.CameraCapabilityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PhotoFilterMode {
    ALL, AR_ONLY, NORMAL_ONLY
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val mediaRepository: MediaRepository,
    private val cameraCapabilityManager: CameraCapabilityManager,
    private val arRepository: ARRepository,
    private val vrmRepository: VRMRepository,
    private val avatarLibraryManager: AvatarLibraryManager,
    private val avatarController: AvatarController,
    private val expressionController: ExpressionController,
    private val poseController: PoseController,
    private val lightingSystem: LightingSystem,
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

    private val _arPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val arPhotos: StateFlow<List<PhotoItem>> = _arPhotos.asStateFlow()

    private val _normalPhotos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val normalPhotos: StateFlow<List<PhotoItem>> = _normalPhotos.asStateFlow()

    private val _photoFilterMode = MutableStateFlow(PhotoFilterMode.ALL)
    val photoFilterMode: StateFlow<PhotoFilterMode> = _photoFilterMode.asStateFlow()

    init {
        viewModelScope.launch {
            mediaRepository.getAllPhotos().collect { photos ->
                _allPhotos.value = photos
            }
        }

        viewModelScope.launch {
            mediaRepository.getARPhotos().collect { photos ->
                _arPhotos.value = photos
            }
        }

        viewModelScope.launch {
            mediaRepository.getNormalPhotos().collect { photos ->
                _normalPhotos.value = photos
            }
        }

        // Initialize camera capabilities
        initializeCameraCapabilities()

        // Initialize AR state observation
        initializeARStateObservation()

        // Initialize avatar library
        initializeAvatarLibrary()

        // Initialize avatar control observers
        initializeAvatarControlObservers()
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

    // AR Mode State
    private val _isARMode = MutableStateFlow(false)
    val isARMode: StateFlow<Boolean> = _isARMode.asStateFlow()

    // Avatar State Management
    private val _avatarState = MutableStateFlow(AvatarState())
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()

    // AR Session State
    private val _arSessionState = MutableStateFlow(ARSessionState())
    val arSessionState: StateFlow<ARSessionState> = _arSessionState.asStateFlow()

    // AR Camera State
    private val _arCameraState = MutableStateFlow(ARCameraState.default())
    val arCameraState: StateFlow<ARCameraState> = _arCameraState.asStateFlow()

    // AR Error State
    private val _arError = MutableStateFlow<ARError?>(null)
    val arError: StateFlow<ARError?> = _arError.asStateFlow()

    // Avatar Transform for user manipulation
    private val _avatarTransform = MutableStateFlow(Transform.identity())
    val avatarTransform: StateFlow<Transform> = _avatarTransform.asStateFlow()

    // Current Avatar Model
    private val _currentAvatar = MutableStateFlow<VRMModel?>(null)
    val currentAvatar: StateFlow<VRMModel?> = _currentAvatar.asStateFlow()

    // Avatar Expression State
    private val _currentExpression = MutableStateFlow<Expression?>(null)
    val currentExpression: StateFlow<Expression?> = _currentExpression.asStateFlow()

    // Avatar Pose State
    private val _currentPose = MutableStateFlow<Pose?>(null)
    val currentPose: StateFlow<Pose?> = _currentPose.asStateFlow()

    // Avatar Library Management State
    private val _avatarLibrary = MutableStateFlow<List<AvatarInfo>>(emptyList())
    val avatarLibrary: StateFlow<List<AvatarInfo>> = _avatarLibrary.asStateFlow()

    private val _avatarLibraryStats = MutableStateFlow<AvatarLibraryStats?>(null)
    val avatarLibraryStats: StateFlow<AvatarLibraryStats?> = _avatarLibraryStats.asStateFlow()

    private val _isLoadingAvatarLibrary = MutableStateFlow(false)
    val isLoadingAvatarLibrary: StateFlow<Boolean> = _isLoadingAvatarLibrary.asStateFlow()

    private val _avatarLibraryError = MutableStateFlow<String?>(null)
    val avatarLibraryError: StateFlow<String?> = _avatarLibraryError.asStateFlow()

    private val _selectedAvatarId = MutableStateFlow<String?>(null)
    val selectedAvatarId: StateFlow<String?> = _selectedAvatarId.asStateFlow()

    private val _avatarSortBy = MutableStateFlow(AvatarSortBy.DATE_ADDED_DESC)
    val avatarSortBy: StateFlow<AvatarSortBy> = _avatarSortBy.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog.asStateFlow()

    private val _renameAvatarId = MutableStateFlow<String?>(null)
    val renameAvatarId: StateFlow<String?> = _renameAvatarId.asStateFlow()

    private val _renameCurrentName = MutableStateFlow("")
    val renameCurrentName: StateFlow<String> = _renameCurrentName.asStateFlow()

    // Avatar Control State
    private val _activeBlendShapes = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeBlendShapes: StateFlow<Map<String, Float>> = _activeBlendShapes.asStateFlow()

    private val _isExpressionTransitioning = MutableStateFlow(false)
    val isExpressionTransitioning: StateFlow<Boolean> = _isExpressionTransitioning.asStateFlow()

    private val _expressionTransitionProgress = MutableStateFlow(0f)
    val expressionTransitionProgress: StateFlow<Float> = _expressionTransitionProgress.asStateFlow()

    private val _activeBoneTransforms = MutableStateFlow<Map<String, Transform>>(emptyMap())
    val activeBoneTransforms: StateFlow<Map<String, Transform>> = _activeBoneTransforms.asStateFlow()

    private val _boneLocks = MutableStateFlow<Set<String>>(emptySet())
    val boneLocks: StateFlow<Set<String>> = _boneLocks.asStateFlow()

    private val _isPoseTransitioning = MutableStateFlow(false)
    val isPoseTransitioning: StateFlow<Boolean> = _isPoseTransitioning.asStateFlow()

    private val _poseTransitionProgress = MutableStateFlow(0f)
    val poseTransitionProgress: StateFlow<Float> = _poseTransitionProgress.asStateFlow()

    private val _smoothTransitions = MutableStateFlow(true)
    val smoothTransitions: StateFlow<Boolean> = _smoothTransitions.asStateFlow()

    private val _autoResetOnAvatarChange = MutableStateFlow(true)
    val autoResetOnAvatarChange: StateFlow<Boolean> = _autoResetOnAvatarChange.asStateFlow()

    // Lighting System State
    val lightingSettings: StateFlow<LightingSettings> = lightingSystem.lightingSettings
    val environmentLighting: StateFlow<EnvironmentLighting?> = lightingSystem.environmentLighting

    private val _lightingPresets = MutableStateFlow(lightingSystem.getLightingPresets())
    val lightingPresets: StateFlow<List<LightingPreset>> = _lightingPresets.asStateFlow()

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

    /**
     * Initialize AR state observation
     */
    private fun initializeARStateObservation() {
        // This will be called when AR mode is enabled
        // The actual observation starts in observeARStates()
    }

    // ========== AR Mode Functions ==========

    /**
     * Enable AR mode and initialize AR session
     */
    fun enableARMode(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        if (_isARMode.value) {
            Log.d("CameraViewModel", "AR mode already enabled")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Enabling AR mode...")
                _isARMode.value = true
                _needsCameraRebind.value = true

                // Initialize AR session
                arRepository.initializeSession(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onSessionReady = {
                        Log.d("CameraViewModel", "AR session ready")
                        // Start observing AR state flows
                        observeARStates()
                    },
                    onError = { error ->
                        Log.e("CameraViewModel", "AR session initialization failed: $error")
                        _arError.value = error
                        // Fallback to normal camera mode
                        disableARMode()
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to enable AR mode", e)
                _arError.value = ARError.SessionError("Failed to enable AR mode: ${e.message}")
                disableARMode()
            }
        }
    }

    /**
     * Disable AR mode and return to normal camera
     */
    fun disableARMode() {
        if (!_isARMode.value) {
            Log.d("CameraViewModel", "AR mode already disabled")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Disabling AR mode...")
                
                // Destroy AR session
                arRepository.destroySession()
                
                // Reset AR states
                _isARMode.value = false
                _arSessionState.value = ARSessionState()
                _arCameraState.value = ARCameraState.default()
                _arError.value = null
                
                // Reset avatar state but keep the loaded model
                _avatarState.value = _avatarState.value.copy(
                    transform = Transform.identity(),
                    isVisible = false
                )
                _avatarTransform.value = Transform.identity()
                
                // Trigger camera rebind to return to normal mode
                _needsCameraRebind.value = true
                
                Log.d("CameraViewModel", "AR mode disabled")
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error disabling AR mode", e)
            }
        }
    }

    /**
     * Toggle between AR mode and normal camera mode
     */
    fun toggleARMode(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        if (_isARMode.value) {
            disableARMode()
        } else {
            enableARMode(context, lifecycleOwner)
        }
    }

    // ========== Avatar Management Functions ==========

    /**
     * Load VRM avatar from URI
     */
    fun loadAvatar(uri: Uri) {
        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Loading avatar from URI: $uri")
                
                // Set loading state
                _avatarState.value = _avatarState.value.copy(
                    isLoading = true,
                    loadingProgress = 0.0f
                )

                // Load VRM model
                val result = vrmRepository.loadVRMFromUri(uri)
                
                result.fold(
                    onSuccess = { vrmModel ->
                        Log.d("CameraViewModel", "Avatar loaded successfully: ${vrmModel.name}")

                        // Update avatar state
                        _currentAvatar.value = vrmModel
                        _avatarState.value = AvatarState(
                            model = vrmModel,
                            transform = _avatarTransform.value,
                            currentExpression = _currentExpression.value,
                            currentPose = _currentPose.value,
                            isVisible = _isARMode.value,
                            isLoading = false,
                            loadingProgress = 1.0f
                        )

                        // Load avatar into controllers
                        avatarController.loadModel(vrmModel)

                        // Auto-reset if enabled
                        if (_autoResetOnAvatarChange.value) {
                            clearExpression()
                            clearPose()
                        } else {
                            // Reset expression and pose to defaults if available
                            if (vrmModel.expressions.isNotEmpty()) {
                                selectExpression(vrmModel.expressions.first())
                            }
                            if (vrmModel.poses.isNotEmpty()) {
                                selectPose(vrmModel.poses.first())
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("CameraViewModel", "Failed to load avatar", error)
                        _avatarState.value = _avatarState.value.copy(
                            isLoading = false,
                            loadingProgress = 0.0f
                        )
                        _arError.value = ARError.AvatarError("Failed to load avatar: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error loading avatar", e)
                _avatarState.value = _avatarState.value.copy(
                    isLoading = false,
                    loadingProgress = 0.0f
                )
                _arError.value = ARError.AvatarError("Error loading avatar: ${e.message}")
            }
        }
    }

    /**
     * Update avatar transform (position, rotation, scale)
     */
    fun updateAvatarTransform(transform: Transform) {
        // Update AvatarController (single source of truth)
        // Local state will be automatically synchronized via the observer
        avatarController.setTransform(transform)

        Log.d("CameraViewModel", "Avatar transform updated: $transform")
    }

    /**
     * Set avatar expression (legacy method - redirects to new system)
     */
    fun setAvatarExpression(expression: Expression?) {
        selectExpression(expression)
    }

    /**
     * Set avatar pose (legacy method - redirects to new system)
     */
    fun setAvatarPose(pose: Pose?) {
        selectPose(pose)
    }

    /**
     * Toggle avatar visibility
     */
    fun toggleAvatarVisibility() {
        val newVisibility = !_avatarState.value.isVisible

        // Update AvatarController (single source of truth)
        // Local state will be automatically synchronized via the observer
        avatarController.setVisible(newVisibility)

        Log.d("CameraViewModel", "Avatar visibility toggled: $newVisibility")
    }

    /**
     * Reset avatar transform to default
     */
    fun resetAvatarTransform() {
        val defaultTransform = Transform.identity()

        // Update AvatarController (single source of truth)
        avatarController.setTransform(defaultTransform)

        Log.d("CameraViewModel", "Avatar transform reset to default")
    }

    // ========== AR Photo Capture Functions ==========

    /**
     * Capture AR photo with avatar composite
     */
    fun captureARPhoto(
        imageCapture: ImageCapture,
        onPhotoSaved: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!_isARMode.value) {
            onError("AR mode is not enabled")
            return
        }

        if (!_avatarState.value.shouldRender) {
            onError("No avatar is loaded or visible")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Capturing AR photo...")

                // Prepare AR metadata for the photo
                val arMetadata = com.example.vtubercamera.data.ARPhotoMetadata(
                    avatarName = _currentAvatar.value?.name,
                    poseName = _currentPose.value?.name,
                    expressionName = _currentExpression.value?.name,
                    lightingPreset = getCurrentLightingPresetName()
                )

                // Use new AR photo capture functionality
                cameraRepository.captureARPhoto(
                    imageCapture = imageCapture,
                    arMetadata = arMetadata,
                    onPhotoSaved = { uri ->
                        val msg = "AR写真を保存しました: $uri"
                        _lastCapturedImageUri.value = uri
                        onPhotoSaved(msg)
                        refreshPhotos()
                        Log.d("CameraViewModel", "AR photo captured successfully with metadata: $arMetadata")
                    },
                    onError = { errorMsg ->
                        Log.e("CameraViewModel", "AR photo capture failed: $errorMsg")
                        onError(errorMsg)
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error capturing AR photo", e)
                onError("AR photo capture error: ${e.message}")
            }
        }
    }

    // ========== AR State Observation ==========

    /**
     * Start observing AR repository state flows
     */
    private fun observeARStates() {
        viewModelScope.launch {
            // Observe AR session state
            arRepository.sessionState.collect { sessionState ->
                _arSessionState.value = sessionState
            }
        }

        viewModelScope.launch {
            // Observe AR camera state
            arRepository.cameraState.collect { cameraState ->
                _arCameraState.value = cameraState
            }
        }

        viewModelScope.launch {
            // Observe tracking state changes
            arRepository.trackingState.collect { trackingState ->
                Log.d("CameraViewModel", "AR tracking state changed: $trackingState")
                // Update avatar visibility based on tracking state
                if (_avatarState.value.model != null) {
                    val shouldShow = trackingState == com.example.vtubercamera.data.vrm.TrackingState.TRACKING
                    if (_avatarState.value.isVisible != shouldShow) {
                        _avatarState.value = _avatarState.value.copy(isVisible = shouldShow)
                    }
                }
            }
        }
    }

    /**
     * Clear AR error state
     */
    fun clearARError() {
        _arError.value = null
    }

    /**
     * Check if AR mode is available (device supports ARCore)
     */
    fun isARModeAvailable(context: android.content.Context): Boolean {
        return try {
            val availability = com.google.ar.core.ArCoreApk.getInstance().checkAvailability(context)
            availability == com.google.ar.core.ArCoreApk.Availability.SUPPORTED_INSTALLED
        } catch (e: Exception) {
            Log.w("CameraViewModel", "Could not check ARCore availability", e)
            false
        }
    }

    /**
     * Get current avatar statistics for debugging
     */
    fun getAvatarDebugInfo(): String {
        val avatar = _currentAvatar.value
        val state = _avatarState.value

        return if (avatar != null) {
            """
            Avatar: ${avatar.name}
            Expressions: ${avatar.expressions.size}
            Poses: ${avatar.poses.size}
            Visible: ${state.isVisible}
            Loading: ${state.isLoading}
            Transform: ${state.transform}
            """.trimIndent()
        } else {
            "No avatar loaded"
        }
    }

    // ========== Avatar Library Management Functions ==========

    /**
     * Initialize avatar library and load avatars
     */
    private fun initializeAvatarLibrary() {
        loadAvatarsFromLibrary()
        loadAvatarLibraryStats()
    }

    /**
     * Load avatars from the library
     */
    private fun loadAvatarsFromLibrary() {
        viewModelScope.launch {
            _isLoadingAvatarLibrary.value = true
            _avatarLibraryError.value = null

            try {
                avatarLibraryManager.getAvatarsSortedBy(_avatarSortBy.value)
                    .catch { error: Throwable ->
                        _avatarLibraryError.value = error.message ?: "Failed to load avatars"
                        _isLoadingAvatarLibrary.value = false
                    }
                    .collect { avatars: List<AvatarInfo> ->
                        _avatarLibrary.value = avatars
                        _isLoadingAvatarLibrary.value = false
                        _avatarLibraryError.value = null
                    }
            } catch (e: Exception) {
                _avatarLibraryError.value = e.message ?: "Unknown error occurred"
                _isLoadingAvatarLibrary.value = false
            }
        }
    }

    /**
     * Load library statistics
     */
    private fun loadAvatarLibraryStats() {
        viewModelScope.launch {
            try {
                val stats = vrmRepository.getLibraryStatistics()
                _avatarLibraryStats.value = stats
            } catch (e: Exception) {
                Log.w("CameraViewModel", "Failed to load avatar library statistics: ${e.message}")
            }
        }
    }

    /**
     * Refresh the avatar library
     */
    fun refreshAvatarLibrary() {
        loadAvatarsFromLibrary()
        loadAvatarLibraryStats()
    }

    /**
     * Select an avatar from the library and load it
     */
    fun selectAvatarFromLibrary(avatarId: String) {
        viewModelScope.launch {
            try {
                // Record usage
                vrmRepository.recordAvatarUsage(avatarId)

                // Update selected avatar
                _selectedAvatarId.value = avatarId

                // Find avatar info and load the model
                val avatarInfo = _avatarLibrary.value.find { it.id == avatarId }
                if (avatarInfo != null) {
                    // Load the avatar from its file path
                    loadAvatar(android.net.Uri.fromFile(java.io.File(avatarInfo.filePath)))
                    Log.d("CameraViewModel", "Selected and loading avatar: ${avatarInfo.name}")
                } else {
                    _avatarLibraryError.value = "Avatar not found in library"
                }

                // Refresh to show updated usage
                refreshAvatarLibrary()
            } catch (e: Exception) {
                _avatarLibraryError.value = "Failed to select avatar: ${e.message}"
                Log.e("CameraViewModel", "Failed to select avatar", e)
            }
        }
    }

    /**
     * Toggle favorite status of an avatar
     */
    fun toggleAvatarFavorite(avatarId: String) {
        viewModelScope.launch {
            try {
                val avatar = _avatarLibrary.value.find { it.id == avatarId }
                if (avatar != null) {
                    val result = vrmRepository.setAvatarFavorite(avatarId, !avatar.isFavorite)
                    if (result.isFailure) {
                        _avatarLibraryError.value = "Failed to update favorite: ${result.exceptionOrNull()?.message}"
                    } else {
                        refreshAvatarLibrary()
                        Log.d("CameraViewModel", "Toggled favorite for avatar: ${avatar.name}")
                    }
                }
            } catch (e: Exception) {
                _avatarLibraryError.value = "Failed to toggle favorite: ${e.message}"
                Log.e("CameraViewModel", "Failed to toggle favorite", e)
            }
        }
    }

    /**
     * Delete an avatar from the library
     */
    fun deleteAvatarFromLibrary(avatarId: String) {
        viewModelScope.launch {
            try {
                _isLoadingAvatarLibrary.value = true

                val result = vrmRepository.deleteAvatar(avatarId)
                if (result.isFailure) {
                    _avatarLibraryError.value = "Failed to delete avatar: ${result.exceptionOrNull()?.message}"
                    _isLoadingAvatarLibrary.value = false
                } else {
                    // If the deleted avatar was the current one, clear it
                    if (_selectedAvatarId.value == avatarId) {
                        _selectedAvatarId.value = null
                        _currentAvatar.value = null
                        _avatarState.value = AvatarState()
                    }
                    refreshAvatarLibrary()
                    Log.d("CameraViewModel", "Deleted avatar: $avatarId")
                }
            } catch (e: Exception) {
                _avatarLibraryError.value = "Failed to delete avatar: ${e.message}"
                _isLoadingAvatarLibrary.value = false
                Log.e("CameraViewModel", "Failed to delete avatar", e)
            }
        }
    }

    /**
     * Rename an avatar in the library
     */
    fun renameAvatarInLibrary(avatarId: String, newName: String) {
        viewModelScope.launch {
            try {
                val result = vrmRepository.renameAvatar(avatarId, newName)
                if (result.isFailure) {
                    _avatarLibraryError.value = "Failed to rename avatar: ${result.exceptionOrNull()?.message}"
                } else {
                    hideRenameDialog()
                    refreshAvatarLibrary()
                    Log.d("CameraViewModel", "Renamed avatar $avatarId to: $newName")
                }
            } catch (e: Exception) {
                _avatarLibraryError.value = "Failed to rename avatar: ${e.message}"
                Log.e("CameraViewModel", "Failed to rename avatar", e)
            }
        }
    }

    /**
     * Change avatar library sort order
     */
    fun setAvatarSortBy(sortBy: AvatarSortBy) {
        _avatarSortBy.value = sortBy
        loadAvatarsFromLibrary()
    }

    /**
     * Show import dialog
     */
    fun showAvatarImportDialog() {
        _showImportDialog.value = true
    }

    /**
     * Hide import dialog
     */
    fun hideAvatarImportDialog() {
        _showImportDialog.value = false
    }

    /**
     * Show rename dialog for avatar
     */
    fun showAvatarRenameDialog(avatarId: String) {
        val avatar = _avatarLibrary.value.find { it.id == avatarId }
        if (avatar != null) {
            _showRenameDialog.value = true
            _renameAvatarId.value = avatarId
            _renameCurrentName.value = avatar.name
        }
    }

    /**
     * Hide rename dialog
     */
    fun hideRenameDialog() {
        _showRenameDialog.value = false
        _renameAvatarId.value = null
        _renameCurrentName.value = ""
    }

    /**
     * Cleanup avatar library (remove orphaned files, etc.)
     */
    fun cleanupAvatarLibrary() {
        viewModelScope.launch {
            try {
                _isLoadingAvatarLibrary.value = true

                val result = vrmRepository.cleanupLibrary()
                _isLoadingAvatarLibrary.value = false

                if (result.success) {
                    refreshAvatarLibrary()
                    Log.d("CameraViewModel", "Avatar library cleanup completed")
                } else {
                    _avatarLibraryError.value = result.error
                }
            } catch (e: Exception) {
                _avatarLibraryError.value = "Failed to cleanup library: ${e.message}"
                _isLoadingAvatarLibrary.value = false
                Log.e("CameraViewModel", "Failed to cleanup avatar library", e)
            }
        }
    }

    /**
     * Clear avatar library error
     */
    fun clearAvatarLibraryError() {
        _avatarLibraryError.value = null
    }

    // ========== Avatar Control Functions ==========

    /**
     * Initialize avatar control observers
     */
    private fun initializeAvatarControlObservers() {
        // Observe avatar controller state (single source of truth for avatar state)
        viewModelScope.launch {
            avatarController.avatarState.collect { avatarControllerState ->
                // Sync local avatar state with controller state
                _avatarState.value = avatarControllerState
                _avatarTransform.value = avatarControllerState.transform
                _currentAvatar.value = avatarControllerState.model
                _currentExpression.value = avatarControllerState.currentExpression
                _currentPose.value = avatarControllerState.currentPose
            }
        }

        // Observe expression controller state
        viewModelScope.launch {
            expressionController.currentExpression.collect { expression ->
                _currentExpression.value = expression
            }
        }

        viewModelScope.launch {
            expressionController.activeBlendShapes.collect { blendShapes ->
                _activeBlendShapes.value = blendShapes
            }
        }

        viewModelScope.launch {
            expressionController.isTransitioning.collect { isTransitioning ->
                _isExpressionTransitioning.value = isTransitioning
            }
        }

        viewModelScope.launch {
            expressionController.transitionProgress.collect { progress ->
                _expressionTransitionProgress.value = progress
            }
        }

        // Observe pose controller state
        viewModelScope.launch {
            poseController.currentPose.collect { pose ->
                _currentPose.value = pose
            }
        }

        viewModelScope.launch {
            poseController.activeBoneTransforms.collect { transforms ->
                _activeBoneTransforms.value = transforms
            }
        }

        viewModelScope.launch {
            poseController.boneLocks.collect { locks ->
                _boneLocks.value = locks
            }
        }

        viewModelScope.launch {
            poseController.isTransitioning.collect { isTransitioning ->
                _isPoseTransitioning.value = isTransitioning
            }
        }

        viewModelScope.launch {
            poseController.transitionProgress.collect { progress ->
                _poseTransitionProgress.value = progress
            }
        }
    }

    // Expression Control Methods

    /**
     * Select expression for current avatar
     */
    fun selectExpression(expression: Expression?) {
        if (_smoothTransitions.value && expression != null) {
            expressionController.transitionToExpression(expression)
        } else {
            expressionController.applyExpression(expression)
        }
        Log.d("CameraViewModel", "Selected expression: ${expression?.name ?: "none"}")
    }

    /**
     * Set blend shape weight
     */
    fun setBlendShapeWeight(shapeName: String, weight: Float) {
        expressionController.setBlendShapeWeight(shapeName, weight)
    }

    /**
     * Clear current expression
     */
    fun clearExpression() {
        expressionController.clearExpression()
        Log.d("CameraViewModel", "Cleared expression")
    }

    /**
     * Set expression transition duration
     */
    fun setExpressionTransitionDuration(duration: Float) {
        expressionController.setTransitionDuration(duration)
    }

    /**
     * Blend multiple expressions
     */
    fun blendExpressions(expressionWeights: Map<Expression, Float>) {
        expressionController.blendExpressions(expressionWeights)
    }

    // Pose Control Methods

    /**
     * Select pose for current avatar
     */
    fun selectPose(pose: Pose?) {
        if (_smoothTransitions.value && pose != null) {
            poseController.transitionToPose(pose)
        } else {
            poseController.applyPose(pose)
        }
        Log.d("CameraViewModel", "Selected pose: ${pose?.name ?: "none"}")
    }

    /**
     * Set bone transform
     */
    fun setBoneTransform(boneName: String, transform: Transform) {
        poseController.setBoneTransform(boneName, transform)
    }

    /**
     * Toggle bone lock
     */
    fun toggleBoneLock(boneName: String) {
        if (poseController.isBoneLocked(boneName)) {
            poseController.unlockBone(boneName)
        } else {
            poseController.lockBone(boneName)
        }
        Log.d("CameraViewModel", "Toggled bone lock for: $boneName")
    }

    /**
     * Clear current pose
     */
    fun clearPose() {
        poseController.clearPose()
        Log.d("CameraViewModel", "Cleared pose")
    }

    /**
     * Reset to default pose
     */
    fun resetToDefaultPose() {
        poseController.resetToDefaultPose()
        Log.d("CameraViewModel", "Reset to default pose")
    }

    /**
     * Set pose transition duration
     */
    fun setPoseTransitionDuration(duration: Float) {
        poseController.setTransitionDuration(duration)
    }

    /**
     * Blend multiple poses
     */
    fun blendPoses(poseWeights: Map<Pose, Float>) {
        poseController.blendPoses(poseWeights)
    }

    // Avatar Control Settings

    /**
     * Enable/disable smooth transitions
     */
    fun setSmoothTransitions(enabled: Boolean) {
        _smoothTransitions.value = enabled
        Log.d("CameraViewModel", "Smooth transitions: $enabled")
    }

    /**
     * Enable/disable auto reset on avatar change
     */
    fun setAutoResetOnAvatarChange(enabled: Boolean) {
        _autoResetOnAvatarChange.value = enabled
        Log.d("CameraViewModel", "Auto reset on avatar change: $enabled")
    }

    /**
     * Update expression and pose transitions (called from render loop)
     */
    fun updateAvatarTransitions(deltaTime: Float) {
        expressionController.updateTransition(deltaTime)
        poseController.updateTransition(deltaTime)
    }

    // ========== Lighting Control Functions ==========

    /**
     * Update lighting settings
     */
    fun updateLightingSettings(settings: LightingSettings) {
        viewModelScope.launch {
            lightingSystem.updateLightingSettings(settings)
        }
    }

    /**
     * Select a lighting preset
     */
    fun selectLightingPreset(preset: LightingPreset) {
        viewModelScope.launch {
            lightingSystem.updateLightingSettings(preset.settings)
        }
    }

    /**
     * Reset lighting settings to defaults
     */
    fun resetLightingToDefaults() {
        viewModelScope.launch {
            lightingSystem.resetToDefaults()
        }
    }

    /**
     * Get available lighting presets
     */
    fun getLightingPresets(): List<LightingPreset> {
        return lightingSystem.getLightingPresets()
    }

    /**
     * Get current lighting preset name for AR photo metadata
     */
    private fun getCurrentLightingPresetName(): String? {
        val currentSettings = lightingSettings.value
        return lightingSystem.getLightingPresets().find { preset ->
            preset.settings == currentSettings
        }?.name
    }

    // ========== Photo Management and Filtering Functions ==========

    /**
     * Set photo filter mode
     */
    fun setPhotoFilterMode(mode: PhotoFilterMode) {
        _photoFilterMode.value = mode
    }

    /**
     * Get photos based on current filter mode
     */
    fun getFilteredPhotos(): List<PhotoItem> {
        return when (_photoFilterMode.value) {
            PhotoFilterMode.ALL -> _allPhotos.value
            PhotoFilterMode.AR_ONLY -> _arPhotos.value
            PhotoFilterMode.NORMAL_ONLY -> _normalPhotos.value
        }
    }

    /**
     * Get photos by specific avatar
     */
    suspend fun getPhotosByAvatar(avatarName: String): List<PhotoItem> {
        return mediaRepository.getPhotosByAvatar(avatarName)
    }

    /**
     * Get AR photo statistics
     */
    fun getARPhotoStats(): ARPhotoStats {
        val arPhotos = _arPhotos.value
        val totalCount = arPhotos.size
        val avatars = arPhotos.mapNotNull { it.avatarName }.distinct()
        val poses = arPhotos.mapNotNull { it.poseName }.distinct()
        val expressions = arPhotos.mapNotNull { it.expressionName }.distinct()

        return ARPhotoStats(
            totalARPhotos = totalCount,
            uniqueAvatars = avatars.size,
            uniquePoses = poses.size,
            uniqueExpressions = expressions.size,
            avatarNames = avatars,
            poseNames = poses,
            expressionNames = expressions
        )
    }

    /**
     * Check if a photo is an AR photo
     */
    fun isARPhoto(photo: PhotoItem): Boolean {
        return photo.isARPhoto
    }
}

data class ARPhotoStats(
    val totalARPhotos: Int,
    val uniqueAvatars: Int,
    val uniquePoses: Int,
    val uniqueExpressions: Int,
    val avatarNames: List<String>,
    val poseNames: List<String>,
    val expressionNames: List<String>
)
