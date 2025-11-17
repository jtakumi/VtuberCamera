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
data class CameraUiState(
    // カメラ基本設定
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

    // ギャラリー機能
    val allPhotos: List<PhotoItem> = emptyList(),
    val arPhotos: List<PhotoItem> = emptyList(),
    val normalPhotos: List<PhotoItem> = emptyList(),
    val photoFilterMode: PhotoFilterMode = PhotoFilterMode.ALL,
    val isLoadingPhotos: Boolean = false,
    val selectedPhotos: Set<Uri> = emptySet(),
    val isSelectionMode: Boolean = false,
    val currentViewingPhoto: PhotoItem? = null,

    // レンズ切り替え機能
    val canSwitchLens: Boolean = false,
    val currentLensType: CameraCapabilityManager.LensType = CameraCapabilityManager.LensType.NORMAL,
    val lensDisplayName: String = "Camera",

    // AR Mode
    val isARMode: Boolean = false,

    // Avatar State Management
    val avatarState: AvatarState = AvatarState(),
    val arSessionState: ARSessionState = ARSessionState(),
    val arCameraState: ARCameraState = ARCameraState.default(),
    val arError: ARError? = null,
    val avatarTransform: Transform = Transform.identity(),
    val currentAvatar: VRMModel? = null,
    val currentExpression: Expression? = null,
    val currentPose: Pose? = null,

    // Avatar Library Management
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

    // Avatar Control State
    val activeBlendShapes: Map<String, Float> = emptyMap(),
    val isExpressionTransitioning: Boolean = false,
    val expressionTransitionProgress: Float = 0f,
    val activeBoneTransforms: Map<String, Transform> = emptyMap(),
    val boneLocks: Set<String> = emptySet(),
    val isPoseTransitioning: Boolean = false,
    val poseTransitionProgress: Float = 0f,
    val smoothTransitions: Boolean = true,
    val autoResetOnAvatarChange: Boolean = true,

    // Lighting System
    val lightingSettings: LightingSettings = LightingSettings(),
    val environmentLighting: EnvironmentLighting? = null,
    val lightingPresets: List<LightingPreset> = emptyList()
)
