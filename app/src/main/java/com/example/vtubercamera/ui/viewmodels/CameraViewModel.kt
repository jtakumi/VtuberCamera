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
    private val arRepository: ARRepository,
    private val vrmRepository: VRMRepository,
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
        
        // Initialize AR state observation
        initializeARStateObservation()
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
                        
                        // Reset expression and pose to defaults if available
                        if (vrmModel.expressions.isNotEmpty()) {
                            setAvatarExpression(vrmModel.expressions.first())
                        }
                        if (vrmModel.poses.isNotEmpty()) {
                            setAvatarPose(vrmModel.poses.first())
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
        _avatarTransform.value = transform
        _avatarState.value = _avatarState.value.copy(transform = transform)
        Log.d("CameraViewModel", "Avatar transform updated: $transform")
    }

    /**
     * Set avatar expression
     */
    fun setAvatarExpression(expression: Expression?) {
        _currentExpression.value = expression
        _avatarState.value = _avatarState.value.copy(currentExpression = expression)
        Log.d("CameraViewModel", "Avatar expression set: ${expression?.name ?: "none"}")
    }

    /**
     * Set avatar pose
     */
    fun setAvatarPose(pose: Pose?) {
        _currentPose.value = pose
        _avatarState.value = _avatarState.value.copy(currentPose = pose)
        Log.d("CameraViewModel", "Avatar pose set: ${pose?.name ?: "none"}")
    }

    /**
     * Toggle avatar visibility
     */
    fun toggleAvatarVisibility() {
        val newVisibility = !_avatarState.value.isVisible
        _avatarState.value = _avatarState.value.copy(isVisible = newVisibility)
        Log.d("CameraViewModel", "Avatar visibility toggled: $newVisibility")
    }

    /**
     * Reset avatar transform to default
     */
    fun resetAvatarTransform() {
        val defaultTransform = Transform.identity()
        updateAvatarTransform(defaultTransform)
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
                
                // Use existing photo capture functionality
                // The AR rendering will be handled by the AR renderer during capture
                cameraRepository.capturePhoto(
                    imageCapture = imageCapture,
                    onPhotoSaved = { uri ->
                        val msg = "AR写真を保存しました: $uri"
                        _lastCapturedImageUri.value = uri
                        onPhotoSaved(msg)
                        refreshPhotos()
                        Log.d("CameraViewModel", "AR photo captured successfully")
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
}
