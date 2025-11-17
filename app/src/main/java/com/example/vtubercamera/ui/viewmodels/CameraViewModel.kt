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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
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

    // UI状態の管理
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // 個別のStateFlow（後方互換性のため）
    val cameraSelector: StateFlow<CameraSelector> = _uiState.map { it.cameraSelector }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CameraSelector.DEFAULT_BACK_CAMERA
    )
    val lastCapturedImageUri: StateFlow<Uri?> = _uiState.map { it.lastCapturedImageUri }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val isPreviewMode: StateFlow<Boolean> = _uiState.map { it.isPreviewMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val flashMode: StateFlow<Int> = _uiState.map { it.flashMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ImageCapture.FLASH_MODE_OFF
    )
    val zoomRatio: StateFlow<Float> = _uiState.map { it.zoomRatio }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )
    val maxZoomRatio: StateFlow<Float> = _uiState.map { it.maxZoomRatio }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 10.0f
    )
    val minZoomRatio: StateFlow<Float> = _uiState.map { it.minZoomRatio }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )
    val needsCameraRebind: StateFlow<Boolean> = _uiState.map { it.needsCameraRebind }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val focusPoint: StateFlow<Pair<Float, Float>?> = _uiState.map { it.focusPoint }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val allPhotos: StateFlow<List<PhotoItem>> = _uiState.map { it.allPhotos }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val arPhotos: StateFlow<List<PhotoItem>> = _uiState.map { it.arPhotos }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val normalPhotos: StateFlow<List<PhotoItem>> = _uiState.map { it.normalPhotos }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val photoFilterMode: StateFlow<PhotoFilterMode> = _uiState.map { it.photoFilterMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotoFilterMode.ALL
    )

    // 残りの個別StateFlow（後方互換性のため）
    val isLoadingPhotos: StateFlow<Boolean> = _uiState.map { it.isLoadingPhotos }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val selectedPhotos: StateFlow<Set<Uri>> = _uiState.map { it.selectedPhotos }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )
    val isSelectionMode: StateFlow<Boolean> = _uiState.map { it.isSelectionMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val currentViewingPhoto: StateFlow<PhotoItem?> = _uiState.map { it.currentViewingPhoto }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val canSwitchLens: StateFlow<Boolean> = _uiState.map { it.canSwitchLens }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val currentLensType: StateFlow<CameraCapabilityManager.LensType> = _uiState.map { it.currentLensType }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CameraCapabilityManager.LensType.NORMAL
    )
    val lensDisplayName: StateFlow<String> = _uiState.map { it.lensDisplayName }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Camera"
    )
    val isARMode: StateFlow<Boolean> = _uiState.map { it.isARMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val avatarState: StateFlow<AvatarState> = _uiState.map { it.avatarState }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AvatarState()
    )
    val arSessionState: StateFlow<ARSessionState> = _uiState.map { it.arSessionState }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ARSessionState()
    )
    val arCameraState: StateFlow<ARCameraState> = _uiState.map { it.arCameraState }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ARCameraState.default()
    )
    val arError: StateFlow<ARError?> = _uiState.map { it.arError }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val avatarTransform: StateFlow<Transform> = _uiState.map { it.avatarTransform }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Transform.identity()
    )
    val currentAvatar: StateFlow<VRMModel?> = _uiState.map { it.currentAvatar }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val currentExpression: StateFlow<Expression?> = _uiState.map { it.currentExpression }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val currentPose: StateFlow<Pose?> = _uiState.map { it.currentPose }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val avatarLibrary: StateFlow<List<AvatarInfo>> = _uiState.map { it.avatarLibrary }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val avatarLibraryStats: StateFlow<AvatarLibraryStats?> = _uiState.map { it.avatarLibraryStats }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val isLoadingAvatarLibrary: StateFlow<Boolean> = _uiState.map { it.isLoadingAvatarLibrary }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val avatarLibraryError: StateFlow<String?> = _uiState.map { it.avatarLibraryError }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val selectedAvatarId: StateFlow<String?> = _uiState.map { it.selectedAvatarId }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val avatarSortBy: StateFlow<AvatarSortBy> = _uiState.map { it.avatarSortBy }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AvatarSortBy.DATE_ADDED_DESC
    )
    val showImportDialog: StateFlow<Boolean> = _uiState.map { it.showImportDialog }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val showRenameDialog: StateFlow<Boolean> = _uiState.map { it.showRenameDialog }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val renameAvatarId: StateFlow<String?> = _uiState.map { it.renameAvatarId }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val renameCurrentName: StateFlow<String> = _uiState.map { it.renameCurrentName }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )
    val activeBlendShapes: StateFlow<Map<String, Float>> = _uiState.map { it.activeBlendShapes }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
    val isExpressionTransitioning: StateFlow<Boolean> = _uiState.map { it.isExpressionTransitioning }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val expressionTransitionProgress: StateFlow<Float> = _uiState.map { it.expressionTransitionProgress }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )
    val activeBoneTransforms: StateFlow<Map<String, Transform>> = _uiState.map { it.activeBoneTransforms }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )
    val boneLocks: StateFlow<Set<String>> = _uiState.map { it.boneLocks }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )
    val isPoseTransitioning: StateFlow<Boolean> = _uiState.map { it.isPoseTransitioning }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val poseTransitionProgress: StateFlow<Float> = _uiState.map { it.poseTransitionProgress }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )
    val smoothTransitions: StateFlow<Boolean> = _uiState.map { it.smoothTransitions }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val autoResetOnAvatarChange: StateFlow<Boolean> = _uiState.map { it.autoResetOnAvatarChange }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val lightingSettings: StateFlow<LightingSettings> = lightingSystem.lightingSettings
    val environmentLighting: StateFlow<EnvironmentLighting?> = lightingSystem.environmentLighting
    val lightingPresets: StateFlow<List<LightingPreset>> = _uiState.map { it.lightingPresets }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var _camera: Camera? = null

    // UI状態を更新するヘルパー関数
    private fun updateUiState(update: CameraUiState.() -> CameraUiState) {
        _uiState.value = _uiState.value.update()
    }

    init {
        viewModelScope.launch {
            mediaRepository.getAllPhotos().collect { photos ->
                updateUiState { copy(allPhotos = photos) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getARPhotos().collect { photos ->
                updateUiState { copy(arPhotos = photos) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getNormalPhotos().collect { photos ->
                updateUiState { copy(normalPhotos = photos) }
            }
        }

        viewModelScope.launch {
            mediaRepository.getLatestPhotoUri().collect { latestUri ->
                updateUiState { copy(latestLibraryPhotoUri = latestUri) }
            }
        }

        // Initialize camera capabilities
        initializeCameraCapabilities()

        // Initialize AR state observation
        initializeARStateObservation()

        // Initialize avatar library (deferred to avoid initialization race conditions)
        viewModelScope.launch {
            try {
                initializeAvatarLibrary()
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to initialize avatar library", e)
                updateUiState { copy(avatarLibraryError = "Failed to initialize avatar library: ${e.message}") }
            }
        }

        // Initialize avatar control observers
        initializeAvatarControlObservers()
    }

    fun setCamera(camera: Camera?) {
        _camera = camera
        
        // Update zoom range based on actual camera capabilities
        camera?.let { cam ->
            try {
                val zoomState = cam.cameraInfo.zoomState.value
                zoomState?.let { state ->
                    val actualMinZoom = state.minZoomRatio
                    val actualMaxZoom = state.maxZoomRatio
                    
                    updateUiState { 
                        copy(
                            minZoomRatio = actualMinZoom,
                            maxZoomRatio = actualMaxZoom
                        )
                    }
                    
                    // Adjust current zoom to fit within the new range
                    val currentZoom = _uiState.value.zoomRatio
                    val adjustedZoom = when {
                        currentZoom < actualMinZoom -> actualMinZoom
                        currentZoom > actualMaxZoom -> actualMaxZoom
                        else -> currentZoom
                    }
                    
                    if (adjustedZoom != currentZoom) {
                        updateUiState { copy(zoomRatio = adjustedZoom) }
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
        val newSelector = when (_uiState.value.cameraSelector) {
            CameraSelector.DEFAULT_BACK_CAMERA -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.DEFAULT_FRONT_CAMERA -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
        updateUiState { 
            copy(
                cameraSelector = newSelector,
                needsCameraRebind = true
            )
        }
    }

    fun toggleFlash() {
        val newFlashMode = when (_uiState.value.flashMode) {
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_ON
        }
        updateUiState { copy(flashMode = newFlashMode) }
    }

    fun setZoom(zoom: Float) {
        val minZoom = _uiState.value.minZoomRatio
        val maxZoom = _camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: _uiState.value.maxZoomRatio
        val clampedZoom = zoom.coerceIn(minZoom, maxZoom)
        updateUiState { copy(zoomRatio = clampedZoom) }
        _camera?.cameraControl?.setZoomRatio(clampedZoom)
    }

    fun smoothZoomTo(targetZoom: Float, duration: Long = 300) {
        val currentZoom = _uiState.value.zoomRatio
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
        updateUiState { copy(focusPoint = Pair(x, y)) }
        viewModelScope.launch {
            delay(1000) // 1秒待機
            updateUiState { copy(focusPoint = null) }
        }
    }

    fun setMaxZoomRatio(maxZoomRatio: Float) {
        updateUiState { copy(maxZoomRatio = maxZoomRatio) }
    }

    fun setMinZoomRatio(minZoomRatio: Float) {
        updateUiState { copy(minZoomRatio = minZoomRatio) }
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
                    updateUiState {
                        copy(
                            lastCapturedImageUri = uri,
                            latestLibraryPhotoUri = uri
                        )
                    }
                    onPhotoSaved(msg)
                    refreshPhotos()
                },
                onError = onError
            )
        }
    }

    fun enterPreviewMode() {
        if (_uiState.value.latestLibraryPhotoUri != null) {
            updateUiState { copy(isPreviewMode = true) }
        }
    }

    fun exitPreviewMode() {
        updateUiState { 
            copy(
                isPreviewMode = false,
                needsCameraRebind = true
            )
        }
    }

    fun clearLastCapturedImage() {
        _uiState.value.lastCapturedImageUri?.let { uri ->
            viewModelScope.launch {
                try {
                    mediaRepository.deletePhoto(uri)
                    Log.d("CameraViewModel", "写真を削除しました: $uri")
                } catch (e: Exception) {
                    Log.e("CameraViewModel", "写真の削除に失敗しました", e)
                }
            }
        }
        updateUiState { 
            copy(
                lastCapturedImageUri = null,
                isPreviewMode = false,
                needsCameraRebind = true
            )
        }
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
                    val currentState = _uiState.value
                    val shouldClearLastCaptured = uri == currentState.lastCapturedImageUri
                    val shouldClearLatest = uri == currentState.latestLibraryPhotoUri
                    if (shouldClearLastCaptured || shouldClearLatest) {
                        updateUiState {
                            copy(
                                lastCapturedImageUri = if (shouldClearLastCaptured) null else lastCapturedImageUri,
                                latestLibraryPhotoUri = if (shouldClearLatest) null else latestLibraryPhotoUri,
                                isPreviewMode = if (shouldClearLatest) false else isPreviewMode,
                                needsCameraRebind = if (shouldClearLastCaptured) true else needsCameraRebind
                            )
                        }
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
            updateUiState { copy(isLoadingPhotos = true) }
            try {
                mediaRepository.refreshPhotos()
            } catch (_: Exception) {
            } finally {
                updateUiState { copy(isLoadingPhotos = false) }
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
        val currentSelection = _uiState.value.selectedPhotos.toMutableSet()
        if (currentSelection.contains(uri)) {
            currentSelection.remove(uri)
        } else {
            currentSelection.add(uri)
        }
        
        updateUiState { 
            copy(
                selectedPhotos = currentSelection,
                isSelectionMode = if (currentSelection.isEmpty()) false else isSelectionMode
            )
        }
    }

    /**
     * 選択モードを開始
     */
    fun startSelectionMode() {
        updateUiState { 
            copy(
                isSelectionMode = true,
                selectedPhotos = emptySet()
            )
        }
    }

    /**
     * 選択モードを終了
     */
    fun exitSelectionMode() {
        updateUiState { 
            copy(
                isSelectionMode = false,
                selectedPhotos = emptySet(),
                needsCameraRebind = true
            )
        }
    }

    /**
     * 全選択/全選択解除
     */
    fun toggleSelectAll() {
        val photosList = _uiState.value.allPhotos
        val currentSelection = _uiState.value.selectedPhotos
        val newSelection = if (currentSelection.size == photosList.size) {
            // 全選択されている場合は全選択解除
            emptySet()
        } else {
            // そうでなければ全選択
            photosList.map { it.uri }.toSet()
        }
        updateUiState { copy(selectedPhotos = newSelection) }
    }

    /**
     * 選択をクリア
     */
    fun clearSelection() {
        updateUiState { 
            copy(
                selectedPhotos = emptySet(),
                isSelectionMode = false
            )
        }
    }

    /**
     * 選択された写真を削除
     */
    fun deleteSelectedPhotos(onResult: (Int) -> Unit) {
        val selectedUris = _uiState.value.selectedPhotos.toList()
        deleteMultiplePhotos(selectedUris, onResult)
    }

    /**
     * 特定の写真を拡大表示用に設定
     */
    fun setCurrentViewingPhoto(photo: PhotoItem?) {
        updateUiState { 
            copy(
                currentViewingPhoto = photo,
                needsCameraRebind = photo == null
            )
        }
    }

    /**
     * 次の写真に移動
     */
    fun goToNextPhoto() {
        val currentPhoto = _uiState.value.currentViewingPhoto ?: return
        val photosList = _uiState.value.allPhotos
        val currentIndex = photosList.indexOfFirst { it.id == currentPhoto.id }
        if (currentIndex >= 0 && currentIndex < photosList.size - 1) {
            updateUiState { copy(currentViewingPhoto = photosList[currentIndex + 1]) }
        }
    }

    /**
     * 前の写真に移動
     */
    fun goToPreviousPhoto() {
        val currentPhoto = _uiState.value.currentViewingPhoto ?: return
        val photosList = _uiState.value.allPhotos
        val currentIndex = photosList.indexOfFirst { it.id == currentPhoto.id }
        if (currentIndex > 0) {
            updateUiState { copy(currentViewingPhoto = photosList[currentIndex - 1]) }
        }
    }

    fun onCameraRebound() {
        // カメラが再バインドされたらフラグをリセット
        updateUiState { copy(needsCameraRebind = false) }
    }

    fun initializePhotos() {
        refreshPhotos()
    }

    private fun initializeCameraCapabilities() {
        viewModelScope.launch {
            try {
                val success = cameraCapabilityManager.detectCameraCapabilities()
                if (success) {
                    val canSwitch = cameraCapabilityManager.canSwitchLens()
                    updateUiState { copy(canSwitchLens = canSwitch) }
                    updateLensDisplayInfo()
                    Log.d("CameraViewModel", "Camera capabilities initialized. Can switch lens: $canSwitch")
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to initialize camera capabilities", e)
            }
        }
    }

    private fun updateLensDisplayInfo() {
        val currentSelector = _uiState.value.cameraSelector
        val lensType = cameraCapabilityManager.getLensType(currentSelector)
        val lensName = cameraCapabilityManager.getLensDisplayName(currentSelector)
        updateUiState { 
            copy(
                currentLensType = lensType,
                lensDisplayName = lensName
            )
        }
    }

    /**
     * Switch between normal and wide-angle lenses using pinch gesture with error handling
     */
    fun switchLens() {
        try {
            if (!_uiState.value.canSwitchLens) {
                Log.w("CameraViewModel", "Lens switching not supported on this device")
                return
            }

            val currentSelector = _uiState.value.cameraSelector
            val alternateSelector = cameraCapabilityManager.getAlternateRearCamera(currentSelector)
            
            if (alternateSelector != null && alternateSelector != currentSelector) {
                val previousLensName = _uiState.value.lensDisplayName
                updateUiState { copy(cameraSelector = alternateSelector) }
                updateLensDisplayInfo()
                updateUiState { copy(needsCameraRebind = true) }
                
                val newLensName = _uiState.value.lensDisplayName
                Log.d("CameraViewModel", "Switched from '$previousLensName' to '$newLensName'")
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
    fun isLensSwitchingAvailable(): Boolean = _uiState.value.canSwitchLens

    /**
     * Get current lens information for debugging
     */
    fun getCurrentLensInfo(): String {
        val state = _uiState.value
        return "${state.lensDisplayName} (${state.currentLensType})"
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
                newCameraSelector = cameraSelector.value,
                flashMode = flashMode.value,
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
        if (uiState.value.isARMode) {
            Log.d("CameraViewModel", "AR mode already enabled")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Enabling AR mode...")
                updateUiState { 
                    copy(
                        isARMode = true,
                        needsCameraRebind = true
                    )
                }

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
                        updateUiState { copy(arError = error) }
                        // Fallback to normal camera mode
                        disableARMode()
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to enable AR mode", e)
                updateUiState { copy(arError = ARError.SessionError("Failed to enable AR mode: ${e.message}")) }
                disableARMode()
            }
        }
    }

    /**
     * Disable AR mode and return to normal camera
     */
    fun disableARMode() {
        if (!uiState.value.isARMode) {
            Log.d("CameraViewModel", "AR mode already disabled")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Disabling AR mode...")
                
                // Destroy AR session
                arRepository.destroySession()
                
                // Reset AR states
                val currentAvatarState = uiState.value.avatarState
                updateUiState { 
                    copy(
                        isARMode = false,
                        arSessionState = ARSessionState(),
                        arCameraState = ARCameraState.default(),
                        arError = null,
                        avatarState = currentAvatarState.copy(
                    transform = Transform.identity(),
                    isVisible = false
                        ),
                        avatarTransform = Transform.identity(),
                        needsCameraRebind = true
                )
                }
                
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
        if (uiState.value.isARMode) {
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
                val currentState = uiState.value
                updateUiState { 
                    copy(
                        avatarState = currentState.avatarState.copy(
                    isLoading = true,
                    loadingProgress = 0.0f
                )
                    )
                }

                // Load VRM model
                val result = vrmRepository.loadVRMFromUri(uri)
                
                result.fold(
                    onSuccess = { vrmModel ->
                        Log.d("CameraViewModel", "Avatar loaded successfully: ${vrmModel.name}")

                        // Update avatar state
                        updateUiState { 
                            copy(
                                currentAvatar = vrmModel,
                                avatarState = AvatarState(
                                    model = vrmModel,
                                    transform = currentState.avatarTransform,
                                    currentExpression = currentState.currentExpression,
                                    currentPose = currentState.currentPose,
                                    isVisible = currentState.isARMode,
                                    isLoading = false,
                                    loadingProgress = 1.0f
                                )
                            )
                        }

                        // Load avatar into controllers
                        avatarController.loadModel(vrmModel)

                        // Auto-reset if enabled
                        if (currentState.autoResetOnAvatarChange) {
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
                        updateUiState { 
                            copy(
                                avatarState = currentState.avatarState.copy(
                                    isLoading = false,
                                    loadingProgress = 0.0f
                                ),
                                arError = ARError.AvatarError("Failed to load avatar: ${error.message}")
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error loading avatar", e)
                updateUiState { 
                    copy(
                        avatarState = avatarState.copy(
                            isLoading = false,
                            loadingProgress = 0.0f
                        ),
                        arError = ARError.AvatarError("Error loading avatar: ${e.message}")
                    )
                }
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
        val currentState = uiState.value
        val newVisibility = !currentState.avatarState.isVisible

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
        if (!uiState.value.isARMode) {
            onError("AR mode is not enabled")
            return
        }

        if (!avatarState.value.shouldRender) {
            onError("No avatar is loaded or visible")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("CameraViewModel", "Capturing AR photo...")

                // Prepare AR metadata for the photo
                val arMetadata = com.example.vtubercamera.data.ARPhotoMetadata(
                    avatarName = currentAvatar.value?.name,
                    poseName = currentPose.value?.name,
                    expressionName = currentExpression.value?.name,
                    lightingPreset = getCurrentLightingPresetName()
                )

                // Use new AR photo capture functionality
                cameraRepository.captureARPhoto(
                    imageCapture = imageCapture,
                    arMetadata = arMetadata,
                    onPhotoSaved = { uri ->
                        val msg = "AR写真を保存しました: $uri"
                        _uiState.value = _uiState.value.copy(
                            lastCapturedImageUri = uri
                        )
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
                updateUiState { copy(arSessionState = sessionState) }
            }
        }

        viewModelScope.launch {
            // Observe AR camera state
            arRepository.cameraState.collect { cameraState ->
                updateUiState { copy(arCameraState = cameraState) }
            }
        }

        viewModelScope.launch {
            // Observe tracking state changes
            arRepository.trackingState.collect { trackingState ->
                Log.d("CameraViewModel", "AR tracking state changed: $trackingState")
                // Update avatar visibility based on tracking state
                val currentState = _uiState.value
                if (currentState.avatarState.model != null) {
                    val shouldShow = trackingState == com.example.vtubercamera.data.vrm.TrackingState.TRACKING
                    if (currentState.avatarState.isVisible != shouldShow) {
                        updateUiState { 
                            copy(
                                avatarState = currentState.avatarState.copy(isVisible = shouldShow)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear AR error state
     */
    fun clearARError() {
        updateUiState { copy(arError = null) }
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
        val avatar = currentAvatar.value
        val state = avatarState.value

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
            try {
                updateUiState { 
                    copy(
                        isLoadingAvatarLibrary = true,
                        avatarLibraryError = null
                    )
                }

                avatarLibraryManager.getAvatarsSortedBy(_uiState.value.avatarSortBy)
                    .catch { error: Throwable ->
                        updateUiState { 
                            copy(
                                avatarLibraryError = error.message ?: "Failed to load avatars",
                                isLoadingAvatarLibrary = false
                            )
                        }
                    }
                    .collect { avatars: List<AvatarInfo> ->
                        updateUiState { 
                            copy(
                                avatarLibrary = avatars,
                                isLoadingAvatarLibrary = false,
                                avatarLibraryError = null
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error in loadAvatarsFromLibrary", e)
                updateUiState { 
                    copy(
                        avatarLibraryError = e.message ?: "Unknown error occurred",
                        isLoadingAvatarLibrary = false
                    )
                }
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
                updateUiState { copy(avatarLibraryStats = stats) }
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
                updateUiState { copy(selectedAvatarId = avatarId) }

                // Find avatar info and load the model
                val avatarInfo = _uiState.value.avatarLibrary.find { it.id == avatarId }
                if (avatarInfo != null) {
                    // Load the avatar from its file path
                    loadAvatar(Uri.fromFile(java.io.File(avatarInfo.filePath)))
                    Log.d("CameraViewModel", "Selected and loading avatar: ${avatarInfo.name}")
                } else {
                    updateUiState { copy(avatarLibraryError = "Avatar not found in library") }
                }

                // Refresh to show updated usage
                refreshAvatarLibrary()
            } catch (e: Exception) {
                updateUiState { copy(avatarLibraryError = "Failed to select avatar: ${e.message}") }
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
                val avatar = _uiState.value.avatarLibrary.find { it.id == avatarId }
                if (avatar != null) {
                    val result = vrmRepository.setAvatarFavorite(avatarId, !avatar.isFavorite)
                    if (result.isFailure) {
                        updateUiState { copy(avatarLibraryError = "Failed to update favorite: ${result.exceptionOrNull()?.message}") }
                    } else {
                        refreshAvatarLibrary()
                        Log.d("CameraViewModel", "Toggled favorite for avatar: ${avatar.name}")
                    }
                }
            } catch (e: Exception) {
                updateUiState { copy(avatarLibraryError = "Failed to toggle favorite: ${e.message}") }
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
                updateUiState { copy(isLoadingAvatarLibrary = true) }

                val result = vrmRepository.deleteAvatar(avatarId)
                if (result.isFailure) {
                    updateUiState { 
                        copy(
                            avatarLibraryError = "Failed to delete avatar: ${result.exceptionOrNull()?.message}",
                            isLoadingAvatarLibrary = false
                        )
                    }
                } else {
                    // If the deleted avatar was the current one, clear it
                    if (_uiState.value.selectedAvatarId == avatarId) {
                        updateUiState { 
                            copy(
                                selectedAvatarId = null,
                                currentAvatar = null,
                                avatarState = AvatarState()
                            )
                        }
                    }
                    refreshAvatarLibrary()
                    Log.d("CameraViewModel", "Deleted avatar: $avatarId")
                }
            } catch (e: Exception) {
                updateUiState { 
                    copy(
                        avatarLibraryError = "Failed to delete avatar: ${e.message}",
                        isLoadingAvatarLibrary = false
                    )
                }
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
                    updateUiState { copy(avatarLibraryError = "Failed to rename avatar: ${result.exceptionOrNull()?.message}") }
                } else {
                    hideRenameDialog()
                    refreshAvatarLibrary()
                    Log.d("CameraViewModel", "Renamed avatar $avatarId to: $newName")
                }
            } catch (e: Exception) {
                updateUiState { copy(avatarLibraryError = "Failed to rename avatar: ${e.message}") }
                Log.e("CameraViewModel", "Failed to rename avatar", e)
            }
        }
    }

    /**
     * Change avatar library sort order
     */
    fun setAvatarSortBy(sortBy: AvatarSortBy) {
        updateUiState { copy(avatarSortBy = sortBy) }
        loadAvatarsFromLibrary()
    }

    /**
     * Show import dialog
     */
    fun showAvatarImportDialog() {
        updateUiState { copy(showImportDialog = true) }
    }

    /**
     * Hide import dialog
     */
    fun hideAvatarImportDialog() {
        updateUiState { copy(showImportDialog = false) }
    }

    /**
     * Show rename dialog for avatar
     */
    fun showAvatarRenameDialog(avatarId: String) {
        val avatar = _uiState.value.avatarLibrary.find { it.id == avatarId }
        if (avatar != null) {
            updateUiState { 
                copy(
                    showRenameDialog = true,
                    renameAvatarId = avatarId,
                    renameCurrentName = avatar.name
                )
            }
        }
    }

    /**
     * Hide rename dialog
     */
    fun hideRenameDialog() {
        updateUiState { 
            copy(
                showRenameDialog = false,
                renameAvatarId = null,
                renameCurrentName = ""
            )
        }
    }

    /**
     * Cleanup avatar library (remove orphaned files, etc.)
     */
    fun cleanupAvatarLibrary() {
        viewModelScope.launch {
            try {
               _uiState.value = _uiState.value.copy(
                   isLoadingAvatarLibrary = true
               )


                val result = vrmRepository.cleanupLibrary()
                _uiState.value = _uiState.value.copy(
                    isLoadingAvatarLibrary = false
                )

                if (result.success) {
                    refreshAvatarLibrary()
                    Log.d("CameraViewModel", "Avatar library cleanup completed")
                } else {
                    _uiState.value = _uiState.value.copy(
                        avatarLibraryError = result.error
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    avatarLibraryError = "Failed to cleanup library: ${e.message}",
                    isLoadingAvatarLibrary = false
                )
                Log.e("CameraViewModel", "Failed to cleanup avatar library", e)
            }
        }
    }

    /**
     * Clear avatar library error
     */
    fun clearAvatarLibraryError() {
        _uiState.value = _uiState.value.copy(
            avatarLibraryError = null
        )
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
                updateUiState { 
                    copy(
                        avatarState = avatarControllerState,
                        avatarTransform = avatarControllerState.transform,
                        currentAvatar = avatarControllerState.model,
                        currentExpression = avatarControllerState.currentExpression,
                        currentPose = avatarControllerState.currentPose
                    )
                }
            }
        }

        // Observe expression controller state
        viewModelScope.launch {
            expressionController.currentExpression.collect { expression ->
                updateUiState { copy(currentExpression = expression) }
            }
        }

        viewModelScope.launch {
            expressionController.activeBlendShapes.collect { blendShapes ->
                updateUiState { copy(activeBlendShapes = blendShapes) }
            }
        }

        viewModelScope.launch {
            expressionController.isTransitioning.collect { isTransitioning ->
                updateUiState { copy(isExpressionTransitioning = isTransitioning) }
            }
        }

        viewModelScope.launch {
            expressionController.transitionProgress.collect { progress ->
                updateUiState { copy(expressionTransitionProgress = progress) }
            }
        }

        // Observe pose controller state
        viewModelScope.launch {
            poseController.currentPose.collect { pose ->
                updateUiState { copy(currentPose = pose) }
            }
        }

        viewModelScope.launch {
            poseController.activeBoneTransforms.collect { transforms ->
                updateUiState { copy(activeBoneTransforms = transforms) }
            }
        }

        viewModelScope.launch {
            poseController.boneLocks.collect { locks ->
                updateUiState { copy(boneLocks = locks) }
            }
        }

        viewModelScope.launch {
            poseController.isTransitioning.collect { isTransitioning ->
                updateUiState { copy(isPoseTransitioning = isTransitioning) }
            }
        }

        viewModelScope.launch {
            poseController.transitionProgress.collect { progress ->
                updateUiState { copy(poseTransitionProgress = progress) }
            }
        }
    }

    // Expression Control Methods

    /**
     * Select expression for current avatar
     */
    fun selectExpression(expression: Expression?) {
        if (_uiState.value.smoothTransitions && expression != null) {
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
        if (_uiState.value.smoothTransitions && pose != null) {
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
        updateUiState { copy(smoothTransitions = enabled) }
        Log.d("CameraViewModel", "Smooth transitions: $enabled")
    }

    /**
     * Enable/disable auto reset on avatar change
     */
    fun setAutoResetOnAvatarChange(enabled: Boolean) {
        updateUiState { copy(autoResetOnAvatarChange = enabled) }
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
        _uiState.value = _uiState.value.copy(
            photoFilterMode = mode
        )
    }

    /**
     * Get photos based on current filter mode
     */
    fun getFilteredPhotos(): List<PhotoItem> {
        return when (uiState.value.photoFilterMode) {
            PhotoFilterMode.ALL -> uiState.value.allPhotos
            PhotoFilterMode.AR_ONLY -> uiState.value.arPhotos
            PhotoFilterMode.NORMAL_ONLY -> uiState.value.normalPhotos
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
        val arPhotos = uiState.value.arPhotos
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
