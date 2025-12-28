package com.example.vtubercamera.ui.viewmodels

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
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
import com.example.vtubercamera.utils.CameraCapabilityManager

/**
 * CameraViewModelのUI状態を管理するデータクラス
 */
data class CameraState(
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    val lastCapturedImageUri: Uri? = null,
    val latestLibraryPhotoUri: Uri? = null,
    val isPreviewMode: Boolean = false,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val zoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 10.0f,
    val minZoomRatio: Float = 1.0f,
    val needsCameraRebind: Boolean = false,
    val focusPoint: Pair<Float, Float>? = null,
)

data class GalleryState(
    val allPhotos: List<PhotoItem> = emptyList(),
    val arPhotos: List<PhotoItem> = emptyList(),
    val normalPhotos: List<PhotoItem> = emptyList(),
    val photoFilterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val isLoadingPhotos: Boolean = false,
    val selectedPhotos: Set<Uri> = emptySet(),
    val isSelectionMode: Boolean = false,
    val currentViewingPhoto: PhotoItem? = null,
)

data class LensState(
    val canSwitchLens: Boolean = false,
    val currentLensType: CameraCapabilityManager.LensType = CameraCapabilityManager.LensType.NORMAL,
    val lensDisplayName: String = "Camera",
)

data class ArState(
    val isARMode: Boolean = false,
    val arSessionState: ARSessionState = ARSessionState(),
    val arCameraState: ARCameraState = ARCameraState.default(),
    val arError: ARError? = null,
)

data class AvatarUiState(
    val avatarState: AvatarState = AvatarState(),
    val avatarTransform: Transform = Transform.identity(),
    val currentAvatar: VRMModel? = null,
    val currentExpression: Expression? = null,
    val currentPose: Pose? = null,
)

data class CameraAvatarLibraryState(
    val avatarLibrary: List<AvatarInfo> = emptyList(),
    val avatarLibraryStats: AvatarLibraryStats? = null,
    val isLoadingAvatarLibrary: Boolean = false,
    val avatarLibraryError: String? = null,
    val selectedAvatarId: String? = null,
    val avatarSortBy: AvatarSortBy = AvatarSortBy.DATE_ADDED_DESC,
    val showImportDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameAvatarId: String? = null,
    val renameCurrentName: String = "",
)

data class CameraAvatarControlState(
    val activeBlendShapes: Map<String, Float> = emptyMap(),
    val isExpressionTransitioning: Boolean = false,
    val expressionTransitionProgress: Float = 0f,
    val activeBoneTransforms: Map<String, Transform> = emptyMap(),
    val boneLocks: Set<String> = emptySet(),
    val isPoseTransitioning: Boolean = false,
    val poseTransitionProgress: Float = 0f,
    val smoothTransitions: Boolean = true,
    val autoResetOnAvatarChange: Boolean = true,
)

data class LightingUiState(
    val lightingSettings: LightingSettings = LightingSettings(),
    val environmentLighting: EnvironmentLighting? = null,
    val lightingPresets: List<LightingPreset> = emptyList(),
)

data class CameraUiState(
    val camera: CameraState = CameraState(),
    val gallery: GalleryState = GalleryState(),
    val lens: LensState = LensState(),
    val ar: ArState = ArState(),
    val avatar: AvatarUiState = AvatarUiState(),
    val avatarLibraryState: CameraAvatarLibraryState = CameraAvatarLibraryState(),
    val avatarControl: CameraAvatarControlState = CameraAvatarControlState(),
    val lighting: LightingUiState = LightingUiState(),
) {
    // ---- Backward-compatible accessors (existing uiState.xxx references) ----

    // カメラ基本設定
    val cameraSelector: CameraSelector get() = camera.cameraSelector
    val lastCapturedImageUri: Uri? get() = camera.lastCapturedImageUri
    val latestLibraryPhotoUri: Uri? get() = camera.latestLibraryPhotoUri
    val isPreviewMode: Boolean get() = camera.isPreviewMode
    val flashMode: Int get() = camera.flashMode
    val zoomRatio: Float get() = camera.zoomRatio
    val maxZoomRatio: Float get() = camera.maxZoomRatio
    val minZoomRatio: Float get() = camera.minZoomRatio
    val needsCameraRebind: Boolean get() = camera.needsCameraRebind
    val focusPoint: Pair<Float, Float>? get() = camera.focusPoint

    // ギャラリー機能
    val allPhotos: List<PhotoItem> get() = gallery.allPhotos
    val arPhotos: List<PhotoItem> get() = gallery.arPhotos
    val normalPhotos: List<PhotoItem> get() = gallery.normalPhotos
    val photoFilterMode: PhotoFilterMode get() = gallery.photoFilterMode
    val isLoadingPhotos: Boolean get() = gallery.isLoadingPhotos
    val selectedPhotos: Set<Uri> get() = gallery.selectedPhotos
    val isSelectionMode: Boolean get() = gallery.isSelectionMode
    val currentViewingPhoto: PhotoItem? get() = gallery.currentViewingPhoto

    // レンズ切り替え機能
    val canSwitchLens: Boolean get() = lens.canSwitchLens
    val currentLensType: CameraCapabilityManager.LensType get() = lens.currentLensType
    val lensDisplayName: String get() = lens.lensDisplayName

    // AR
    val isARMode: Boolean get() = ar.isARMode
    val arSessionState: ARSessionState get() = ar.arSessionState
    val arCameraState: ARCameraState get() = ar.arCameraState
    val arError: ARError? get() = ar.arError

    // Avatar state management
    val avatarState: AvatarState get() = avatar.avatarState
    val avatarTransform: Transform get() = avatar.avatarTransform
    val currentAvatar: VRMModel? get() = avatar.currentAvatar
    val currentExpression: Expression? get() = avatar.currentExpression
    val currentPose: Pose? get() = avatar.currentPose

    // Avatar library management
    val avatarLibrary: List<AvatarInfo> get() = avatarLibraryState.avatarLibrary
    val avatarLibraryStats: AvatarLibraryStats? get() = avatarLibraryState.avatarLibraryStats
    val isLoadingAvatarLibrary: Boolean get() = avatarLibraryState.isLoadingAvatarLibrary
    val avatarLibraryError: String? get() = avatarLibraryState.avatarLibraryError
    val selectedAvatarId: String? get() = avatarLibraryState.selectedAvatarId
    val avatarSortBy: AvatarSortBy get() = avatarLibraryState.avatarSortBy
    val showImportDialog: Boolean get() = avatarLibraryState.showImportDialog
    val showRenameDialog: Boolean get() = avatarLibraryState.showRenameDialog
    val renameAvatarId: String? get() = avatarLibraryState.renameAvatarId
    val renameCurrentName: String get() = avatarLibraryState.renameCurrentName

    // Avatar control state
    val activeBlendShapes: Map<String, Float> get() = avatarControl.activeBlendShapes
    val isExpressionTransitioning: Boolean get() = avatarControl.isExpressionTransitioning
    val expressionTransitionProgress: Float get() = avatarControl.expressionTransitionProgress
    val activeBoneTransforms: Map<String, Transform> get() = avatarControl.activeBoneTransforms
    val boneLocks: Set<String> get() = avatarControl.boneLocks
    val isPoseTransitioning: Boolean get() = avatarControl.isPoseTransitioning
    val poseTransitionProgress: Float get() = avatarControl.poseTransitionProgress
    val smoothTransitions: Boolean get() = avatarControl.smoothTransitions
    val autoResetOnAvatarChange: Boolean get() = avatarControl.autoResetOnAvatarChange

    // Lighting system
    val lightingSettings: LightingSettings get() = lighting.lightingSettings
    val environmentLighting: EnvironmentLighting? get() = lighting.environmentLighting
    val lightingPresets: List<LightingPreset> get() = lighting.lightingPresets

}
