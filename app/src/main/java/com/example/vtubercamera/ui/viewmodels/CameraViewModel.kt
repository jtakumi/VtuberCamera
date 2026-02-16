package com.example.vtubercamera.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.MediaRepository
import com.example.vtubercamera.data.PhotoItem
import com.example.vtubercamera.data.vrm.AvatarState
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARError
import com.example.vtubercamera.data.vrm.VRMModel
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.data.vrm.AvatarLibraryStats
import com.example.vtubercamera.data.vrm.AvatarSortBy
import com.example.vtubercamera.data.vrm.LightingSettings
import com.example.vtubercamera.data.vrm.LightingPreset
import com.example.vtubercamera.data.vrm.EnvironmentLighting
import com.example.vtubercamera.data.vrm.ErrorNotification
import com.example.vtubercamera.data.vrm.ErrorNotificationManager
import com.example.vtubercamera.domain.ar.ARFeature
import com.example.vtubercamera.domain.avatar.AvatarFeature
import com.example.vtubercamera.domain.camera.CameraControlsFeature
import com.example.vtubercamera.domain.camera.CameraCaptureFeature
import com.example.vtubercamera.domain.camera.GalleryFeature
import com.example.vtubercamera.domain.camera.LensSwitchFeature
import com.example.vtubercamera.domain.lighting.LightingFeature
import com.example.vtubercamera.utils.CameraCapabilityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val cameraControlsFeature: CameraControlsFeature,
    private val cameraCaptureFeature: CameraCaptureFeature,
    private val galleryFeature: GalleryFeature,
    private val lensSwitchFeature: LensSwitchFeature,
    private val arFeature: ARFeature,
    private val avatarFeature: AvatarFeature,
    private val lightingFeature: LightingFeature,
    private val bootstrapper: CameraViewModelBootstrapper,
    private val arSessionStarter: ARSessionStarter,
    private val arRepository: com.example.vtubercamera.data.ARRepository,
    private val arRenderer: com.example.vtubercamera.data.vrm.ARRenderer,
    private val errorNotificationManager: ErrorNotificationManager,
) : ViewModel() {

    // UI状態の管理
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // 個別のStateFlow（後方互換性のため）
    val cameraSelector: StateFlow<CameraSelector> = _uiState.map { it.cameraSelector }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CameraSelector.DEFAULT_BACK_CAMERA
    )
    val lastCapturedImageUri: StateFlow<Uri?> = _uiState.map { it.lastCapturedImageUri }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val isPreviewMode: StateFlow<Boolean> = _uiState.map { it.isPreviewMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )
    val flashMode: StateFlow<Int> = _uiState.map { it.flashMode }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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
        started = SharingStarted.Eagerly,
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
        started = SharingStarted.Eagerly,
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

    val activeErrorNotifications: StateFlow<List<ErrorNotification>> =
        errorNotificationManager.activeNotifications

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
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    val autoResetOnAvatarChange: StateFlow<Boolean> = _uiState.map { it.autoResetOnAvatarChange }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )
    val lightingSettings: StateFlow<LightingSettings> = lightingFeature.lightingSettings
    val environmentLighting: StateFlow<EnvironmentLighting?> = lightingFeature.environmentLighting
    val lightingPresets: StateFlow<List<LightingPreset>> = _uiState.map { it.lightingPresets }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI状態を更新するヘルパー関数
    private fun updateUiState(update: CameraUiState.() -> CameraUiState) {
        _uiState.value = _uiState.value.update()
    }

    init {
        bootstrapper.start(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    fun setCamera(camera: Camera?) {
        cameraControlsFeature.setCamera(
            camera = camera,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun switchCamera() {
        cameraControlsFeature.switchCamera(
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun toggleFlash() {
        cameraControlsFeature.toggleFlash(
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun setZoom(zoom: Float) {
        cameraControlsFeature.setZoom(
            zoom = zoom,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun smoothZoomTo(targetZoom: Float, duration: Long = 300) {
        cameraControlsFeature.smoothZoomTo(
            targetZoom = targetZoom,
            duration = duration,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun resetZoom() {
        cameraControlsFeature.resetZoom(
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun focusOnPoint(previewView: androidx.camera.view.PreviewView, x: Float, y: Float) {
        cameraControlsFeature.focusOnPoint(
            previewView = previewView,
            x = x,
            y = y,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
        )
    }

    fun setMaxZoomRatio(maxZoomRatio: Float) {
        updateUiState { copy(camera = camera.copy(maxZoomRatio = maxZoomRatio)) }
    }

    fun setMinZoomRatio(minZoomRatio: Float) {
        updateUiState { copy(camera = camera.copy(minZoomRatio = minZoomRatio)) }
    }

    fun takePhoto(
        imageCapture: ImageCapture,
        onPhotoSaved: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        cameraCaptureFeature.capturePhoto(
            imageCapture = imageCapture,
            scope = viewModelScope,
            onPhotoSaved = { uri ->
                val msg = "写真を保存しました: $uri"
                updateUiState {
                    copy(
                        camera = camera.copy(
                            lastCapturedImageUri = uri,
                        )
                    )
                }
                onPhotoSaved(msg)
                refreshPhotos()
            },
            onError = onError,
        )
    }

    fun enterPreviewMode() {
        if (_uiState.value.latestLibraryPhotoUri != null) {
            updateUiState { copy(camera = camera.copy(isPreviewMode = true)) }
        }
    }

    fun exitPreviewMode() {
        updateUiState { 
            copy(
                camera = camera.copy(
                    isPreviewMode = false,
                    needsCameraRebind = true
                )
            )
        }
    }

    fun clearLastCapturedImage() {
        galleryFeature.clearLastCapturedImage(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun dismissErrorNotification(notificationId: String) {
        errorNotificationManager.dismissNotification(notificationId)
    }

    /**
     * 指定されたURIの写真をストレージから削除
     * @param uri 削除する写真のURI
     * @return 削除が成功したかどうか
     */
    fun deletePhoto(uri: Uri, onResult: (Boolean) -> Unit) {
        galleryFeature.deletePhoto(
            uri = uri,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
            onResult = onResult
        )
    }

    /**
     * 複数の写真を一括削除
     * @param uris 削除する写真のURIリスト
     * @return 削除に成功した写真の数
     */
    fun deleteMultiplePhotos(uris: List<Uri>, onResult: (Int) -> Unit) {
        galleryFeature.deleteMultiplePhotos(
            uris = uris,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            onResult = onResult
        )
    }

    /**
     * 端末内の全ての写真を取得
     */
    fun refreshPhotos() {
        galleryFeature.refreshPhotos(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
        )
    }

    /**
     * MediaStoreから全ての写真を取得
     */

    /**
     * 写真の選択状態を切り替え
     */
    fun togglePhotoSelection(uri: Uri) {
        galleryFeature.togglePhotoSelection(
            uri = uri,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    /**
     * 選択モードを開始
     */
    fun startSelectionMode() {
        galleryFeature.startSelectionMode(updateUiState = this::updateUiState)
    }

    /**
     * 選択モードを終了
     */
    fun exitSelectionMode() {
        galleryFeature.exitSelectionMode(updateUiState = this::updateUiState)
    }

    /**
     * 全選択/全選択解除
     */
    fun toggleSelectAll() {
        galleryFeature.toggleSelectAll(
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    /**
     * 選択をクリア
     */
    fun clearSelection() {
        galleryFeature.clearSelection(updateUiState = this::updateUiState)
    }

    /**
     * 選択された写真を削除
     */
    fun deleteSelectedPhotos(onResult: (Int) -> Unit) {
        galleryFeature.deleteSelectedPhotos(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
            onResult = onResult
        )
    }

    /**
     * 特定の写真を拡大表示用に設定
     */
    fun setCurrentViewingPhoto(photo: PhotoItem?) {
        galleryFeature.setCurrentViewingPhoto(
            photo = photo,
            updateUiState = this::updateUiState
        )
    }

    /**
     * 次の写真に移動
     */
    fun goToNextPhoto() {
        galleryFeature.goToNextPhoto(
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    /**
     * 前の写真に移動
     */
    fun goToPreviousPhoto() {
        galleryFeature.goToPreviousPhoto(
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    fun onCameraRebound() {
        // カメラが再バインドされたらフラグをリセット
        updateUiState { copy(camera = camera.copy(needsCameraRebind = false)) }
    }

    fun initializePhotos() {
        refreshPhotos()
    }

    /**
     * Switch between normal and wide-angle lenses using pinch gesture with error handling
     */
    fun switchLens() {
        lensSwitchFeature.switchLens(
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value }
        )
    }

    /**
     * Check if device supports multiple rear cameras and lens switching
     */
    fun isLensSwitchingAvailable(): Boolean = _uiState.value.canSwitchLens

    /**
     * Get current lens information for debugging
     */
    fun getCurrentLensInfo(): String {
        return lensSwitchFeature.getCurrentLensInfo(uiStateProvider = { _uiState.value })
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

    // ========== AR Mode Functions ==========

    /**
     * Enable AR mode and initialize AR session
     */
    fun enableARMode(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        arFeature.enableARMode(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
            onTrackingStateChanged = this::onARTrackingStateChanged,
            startSession = { onSessionReady, onError ->
                arSessionStarter.start(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onSessionReady = onSessionReady,
                    onError = onError,
                )
            },
        )
    }

    /**
     * Disable AR mode and return to normal camera
     */
    fun disableARMode() {
        arFeature.disableARMode(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
            resetAvatarState = this::resetAvatarState,
        )
    }

    /**
     * Toggle between AR mode and normal camera mode
     */
    fun toggleARMode(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner
    ) {
        arFeature.toggleARMode(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
            onTrackingStateChanged = this::onARTrackingStateChanged,
            resetAvatarState = this::resetAvatarState,
            startSession = { onSessionReady, onError ->
                arSessionStarter.start(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onSessionReady = onSessionReady,
                    onError = onError,
                )
            },
        )
    }

    // ========== AR Rendering Surface Hooks (Phase 3) ==========

    fun onRenderSurfaceAvailable(surface: android.view.Surface, width: Int, height: Int) {
        try {
            val session = arRepository.getSession()
            if (session == null) {
                Log.w("CameraViewModel", "AR session not ready when surface became available")
                return
            }
            arRenderer.setViewport(width, height)
            arRenderer.initialize(surface, session)
            arRenderer.setAvatarRenderingEnabled(_uiState.value.currentAvatar != null)
            Log.d("CameraViewModel", "ARRenderer initialized with ${width}x${height}")
        } catch (t: Throwable) {
            Log.e("CameraViewModel", "Failed to init AR renderer", t)
            updateUiState { copy(ar = ar.copy(arError = com.example.vtubercamera.data.vrm.ARError.RenderingError("Renderer init failed: ${t.message}"))) }
        }
    }

    fun onRenderSurfaceSizeChanged(width: Int, height: Int) {
        try {
            if (arRenderer.isInitialized()) {
                arRenderer.setViewport(width, height)
                Log.d("CameraViewModel", "ARRenderer viewport updated to ${width}x${height}")
            }
        } catch (t: Throwable) {
            Log.w("CameraViewModel", "Failed to update viewport", t)
        }
    }

    fun onRenderSurfaceDestroyed() {
        try {
            if (arRenderer.isInitialized()) {
                arRenderer.cleanup()
            }
        } catch (t: Throwable) {
            Log.w("CameraViewModel", "ARRenderer cleanup error", t)
        }
    }

    fun onARFrame(frame: com.google.ar.core.Frame, deltaSeconds: Float) {
        try {
            // Update repository state from frame (tracking, camera, light)
            arRepository.updateFromFrame(frame)

            // Update transitions (blend/pose)
            updateAvatarTransitions(deltaSeconds)

            // Render/update
            if (arRenderer.isInitialized()) {
                arRenderer.updateFrame(frame, _uiState.value.avatarState)

                val avatar = _uiState.value.currentAvatar
                if (avatar != null) {
                    arRenderer.setAvatarRenderingEnabled(true)
                    arRenderer.renderAvatar(avatar, _uiState.value.avatarTransform)
                } else {
                    arRenderer.setAvatarRenderingEnabled(false)
                }
            }
        } catch (t: Throwable) {
            Log.w("CameraViewModel", "onARFrame error", t)
        }
    }

    // ========== Avatar Management Functions ==========

    // Internal AR session accessor for UI layer
    fun getARSession(): com.google.ar.core.Session? = arRepository.getSession()

    /**
     * Load VRM avatar from URI
     */
    fun loadAvatar(uri: Uri) {
        avatarFeature.loadAvatar(
            uri = uri,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Update avatar transform (position, rotation, scale)
     */
    fun updateAvatarTransform(transform: Transform) {
        avatarFeature.updateAvatarTransform(transform)
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
        avatarFeature.toggleAvatarVisibility(uiStateProvider = { _uiState.value })
    }

    /**
     * Reset avatar transform to default
     */
    fun resetAvatarTransform() {
        avatarFeature.resetAvatarTransform()
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

        Log.d("CameraViewModel", "Capturing AR photo...")

        val arMetadata = com.example.vtubercamera.data.ARPhotoMetadata(
            avatarName = currentAvatar.value?.name,
            poseName = currentPose.value?.name,
            expressionName = currentExpression.value?.name,
            lightingPreset = getCurrentLightingPresetName()
        )

        cameraCaptureFeature.captureARPhoto(
            imageCapture = imageCapture,
            arMetadata = arMetadata,
            scope = viewModelScope,
            onPhotoSaved = { uri ->
                val msg = "AR写真を保存しました: $uri"
                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    camera = currentState.camera.copy(
                        lastCapturedImageUri = uri
                    )
                )
                onPhotoSaved(msg)
                refreshPhotos()
                Log.d("CameraViewModel", "AR photo captured successfully with metadata: $arMetadata")
            },
            onError = { errorMsg ->
                Log.e("CameraViewModel", "AR photo capture failed: $errorMsg")
                onError(errorMsg)
            },
        )
    }

    // ========== AR State Observation ==========

    private fun onARTrackingStateChanged(trackingState: com.example.vtubercamera.data.vrm.TrackingState) {
        val currentState = _uiState.value
        if (currentState.avatarState.model == null) return

        val shouldShow = trackingState == com.example.vtubercamera.data.vrm.TrackingState.TRACKING
        if (currentState.avatarState.isVisible == shouldShow) return

        updateUiState {
            copy(
                avatar = avatar.copy(
                    avatarState = currentState.avatarState.copy(isVisible = shouldShow)
                )
            )
        }
    }

    private fun resetAvatarState() {
        val currentAvatarState = _uiState.value.avatarState
        updateUiState {
            copy(
                avatar = avatar.copy(
                    avatarState = currentAvatarState.copy(
                        transform = Transform.identity(),
                        isVisible = false,
                    ),
                    avatarTransform = Transform.identity(),
                ),
            )
        }
    }

    /**
     * Clear AR error state
     */
    fun clearARError() {
        updateUiState { copy(ar = ar.copy(arError = null)) }
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
    /**
     * Refresh the avatar library
     */
    fun refreshAvatarLibrary() {
        avatarFeature.refreshAvatarLibrary(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Select an avatar from the library and load it
     */
    fun selectAvatarFromLibrary(avatarId: String) {
        avatarFeature.selectAvatarFromLibrary(
            avatarId = avatarId,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Toggle favorite status of an avatar
     */
    fun toggleAvatarFavorite(avatarId: String) {
        avatarFeature.toggleAvatarFavorite(
            avatarId = avatarId,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Delete an avatar from the library
     */
    fun deleteAvatarFromLibrary(avatarId: String) {
        avatarFeature.deleteAvatarFromLibrary(
            avatarId = avatarId,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Rename an avatar in the library
     */
    fun renameAvatarInLibrary(avatarId: String, newName: String) {
        avatarFeature.renameAvatarInLibrary(
            avatarId = avatarId,
            newName = newName,
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Change avatar library sort order
     */
    fun setAvatarSortBy(sortBy: AvatarSortBy) {
        updateUiState { copy(avatarLibraryState = avatarLibraryState.copy(avatarSortBy = sortBy)) }
        avatarFeature.loadAvatarsFromLibrary(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Show import dialog
     */
    fun showAvatarImportDialog() {
        updateUiState { copy(avatarLibraryState = avatarLibraryState.copy(showImportDialog = true)) }
    }

    /**
     * Hide import dialog
     */
    fun hideAvatarImportDialog() {
        updateUiState { copy(avatarLibraryState = avatarLibraryState.copy(showImportDialog = false)) }
    }

    /**
     * Show rename dialog for avatar
     */
    fun showAvatarRenameDialog(avatarId: String) {
        val avatar = _uiState.value.avatarLibrary.find { it.id == avatarId }
        if (avatar != null) {
            updateUiState { 
                copy(
                    avatarLibraryState = avatarLibraryState.copy(
                        showRenameDialog = true,
                        renameAvatarId = avatarId,
                        renameCurrentName = avatar.name
                    )
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
                avatarLibraryState = avatarLibraryState.copy(
                    showRenameDialog = false,
                    renameAvatarId = null,
                    renameCurrentName = ""
                )
            )
        }
    }

    /**
     * Cleanup avatar library (remove orphaned files, etc.)
     */
    fun cleanupAvatarLibrary() {
        avatarFeature.cleanupAvatarLibrary(
            scope = viewModelScope,
            updateUiState = this::updateUiState,
            uiStateProvider = { _uiState.value },
        )
    }

    /**
     * Clear avatar library error
     */
    fun clearAvatarLibraryError() {
        avatarFeature.clearAvatarLibraryError(updateUiState = this::updateUiState)
    }

    // ========== Avatar Control Functions ==========

    /**
     * Initialize avatar control observers
     */
    // Expression Control Methods

    /**
     * Select expression for current avatar
     */
    fun selectExpression(expression: Expression?) {
        avatarFeature.selectExpression(
            expression = expression,
            smoothTransitions = _uiState.value.smoothTransitions,
        )
    }

    /**
     * Set blend shape weight
     */
    fun setBlendShapeWeight(shapeName: String, weight: Float) {
        avatarFeature.setBlendShapeWeight(shapeName, weight)
    }

    /**
     * Clear current expression
     */
    fun clearExpression() {
        avatarFeature.clearExpression()
    }

    /**
     * Set expression transition duration
     */
    fun setExpressionTransitionDuration(duration: Float) {
        avatarFeature.setExpressionTransitionDuration(duration)
    }

    /**
     * Blend multiple expressions
     */
    fun blendExpressions(expressionWeights: Map<Expression, Float>) {
        avatarFeature.blendExpressions(expressionWeights)
    }

    // Pose Control Methods

    /**
     * Select pose for current avatar
     */
    fun selectPose(pose: Pose?) {
        avatarFeature.selectPose(
            pose = pose,
            smoothTransitions = _uiState.value.smoothTransitions,
        )
    }

    /**
     * Set bone transform
     */
    fun setBoneTransform(boneName: String, transform: Transform) {
        avatarFeature.setBoneTransform(boneName, transform)
    }

    /**
     * Toggle bone lock
     */
    fun toggleBoneLock(boneName: String) {
        avatarFeature.toggleBoneLock(boneName)
    }

    /**
     * Clear current pose
     */
    fun clearPose() {
        avatarFeature.clearPose()
    }

    /**
     * Reset to default pose
     */
    fun resetToDefaultPose() {
        avatarFeature.resetToDefaultPose()
    }

    /**
     * Set pose transition duration
     */
    fun setPoseTransitionDuration(duration: Float) {
        avatarFeature.setPoseTransitionDuration(duration)
    }

    /**
     * Blend multiple poses
     */
    fun blendPoses(poseWeights: Map<Pose, Float>) {
        avatarFeature.blendPoses(poseWeights)
    }

    // Avatar Control Settings

    /**
     * Enable/disable smooth transitions
     */
    fun setSmoothTransitions(enabled: Boolean) {
        updateUiState { copy(avatarControl = avatarControl.copy(smoothTransitions = enabled)) }
        Log.d("CameraViewModel", "Smooth transitions: $enabled")
    }

    /**
     * Enable/disable auto reset on avatar change
     */
    fun setAutoResetOnAvatarChange(enabled: Boolean) {
        updateUiState { copy(avatarControl = avatarControl.copy(autoResetOnAvatarChange = enabled)) }
        Log.d("CameraViewModel", "Auto reset on avatar change: $enabled")
    }

    /**
     * Update expression and pose transitions (called from render loop)
     */
    fun updateAvatarTransitions(deltaTime: Float) {
        avatarFeature.updateAvatarTransitions(deltaTime)
    }

    // ========== Lighting Control Functions ==========

    /**
     * Update lighting settings
     */
    fun updateLightingSettings(settings: LightingSettings) {
        lightingFeature.updateLightingSettings(viewModelScope, settings)
    }

    /**
     * Select a lighting preset
     */
    fun selectLightingPreset(preset: LightingPreset) {
        lightingFeature.selectLightingPreset(viewModelScope, preset)
    }

    /**
     * Reset lighting settings to defaults
     */
    fun resetLightingToDefaults() {
        lightingFeature.resetLightingToDefaults(viewModelScope)
    }

    /**
     * Get available lighting presets
     */
    fun getLightingPresets(): List<LightingPreset> {
        return lightingFeature.getLightingPresets()
    }

    /**
     * Get current lighting preset name for AR photo metadata
     */
    private fun getCurrentLightingPresetName(): String? {
        val currentSettings = lightingSettings.value
        return lightingFeature.getCurrentLightingPresetName(currentSettings)
    }

    // ========== Photo Management and Filtering Functions ==========

    /**
     * Set photo filter mode
     */
    fun setPhotoFilterMode(mode: PhotoFilterMode) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            gallery = currentState.gallery.copy(
                photoFilterMode = mode
            )
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
