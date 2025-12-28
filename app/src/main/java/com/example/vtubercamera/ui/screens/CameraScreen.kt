package com.example.vtubercamera.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vtubercamera.R
import com.example.vtubercamera.data.PhotoItem
import com.example.vtubercamera.ui.components.AsyncImage
import com.example.vtubercamera.ui.components.DeleteConfirmDialog
import com.example.vtubercamera.ui.components.GalleryView
import com.example.vtubercamera.ui.components.LensIndicator
import com.example.vtubercamera.ui.components.LensSwitchFeedback
import com.example.vtubercamera.ui.components.LensSwitchHint
import com.example.vtubercamera.ui.components.PartialAccessDialog
import com.example.vtubercamera.ui.components.PermissionRequestComponent
import com.example.vtubercamera.ui.components.PhotoDetailView
import com.example.vtubercamera.ui.components.PhotoPreviewComponent
import com.example.vtubercamera.ui.modifiers.modernCameraGestures
import com.example.vtubercamera.ui.camerax.bindCameraWithPreview
import com.example.vtubercamera.ui.viewmodels.CameraViewModel
import com.example.vtubercamera.utils.CameraCapabilityManager
import com.example.vtubercamera.utils.PermissionUtils

@SuppressLint("LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val resource = context.resources
    val lifecycleOwner = LocalLifecycleOwner.current

    // デバイス設定の読み取り
    val configuration = LocalConfiguration.current
    configuration.locales[0].language
    configuration.locales[0].country
    configuration.orientation
    configuration.densityDpi
    LocalWindowInfo.current.containerSize
    val uiMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    uiMode == Configuration.UI_MODE_NIGHT_YES

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraSelector = uiState.cameraSelector
    val latestCapturedImageUri = uiState.lastCapturedImageUri
    val isPreviewMode = uiState.isPreviewMode
    val flashMode = uiState.flashMode
    val zoomRatio = uiState.zoomRatio
    val maxZoomRatio = uiState.maxZoomRatio
    val minZoomRatio = uiState.minZoomRatio
    val needsCameraRebind = uiState.needsCameraRebind
    val allPhotos = uiState.allPhotos
    val isLoadingPhotos = uiState.isLoadingPhotos
    val selectedPhotos = uiState.selectedPhotos
    val isSelectionMode = uiState.isSelectionMode
    val currentViewingPhoto = uiState.currentViewingPhoto
    val canSwitchLens = uiState.canSwitchLens
    val currentLensType = uiState.currentLensType
    val lensDisplayName = uiState.lensDisplayName

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var showPartialAccessDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showGalleryView by remember { mutableStateOf(false) }
    var showLensSwitchFeedback by remember { mutableStateOf(false) }
    var lensSwitchFeedbackName by remember { mutableStateOf("") }

    var hasCameraPermission by remember {
        mutableStateOf(
            PermissionUtils.hasCameraPermission(context)
        )
    }
    var hasMediaPermissions by remember {
        mutableStateOf(
            PermissionUtils.hasMediaPermissions(context)
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )
    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasMediaPermissions = permissions.values.all { it }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasPartialAccess = PermissionUtils.hasPartialMediaAccess(context)
                if (hasPartialAccess && !permissions[Manifest.permission.READ_MEDIA_IMAGES]!!) {
                    showPartialAccessDialog = true
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasMediaPermissions) {
            mediaPermissionsLauncher.launch(PermissionUtils.getRequiredMediaPermissions())
        }
    }

    LaunchedEffect(hasMediaPermissions) {
        if (hasMediaPermissions) {
            viewModel.initializePhotos()
        }
    }

    CameraDialogs(
        showPartialAccessDialog = showPartialAccessDialog,
        onDismissPartialAccessDialog = { showPartialAccessDialog = false },
        showDeleteConfirmDialog = showDeleteConfirmDialog,
        onDismissDeleteConfirmDialog = { showDeleteConfirmDialog = false },
        onConfirmDelete = {
            if (isSelectionMode && selectedPhotos.isNotEmpty()) {
                viewModel.deleteSelectedPhotos { deletedCount ->
                    Toast.makeText(
                        context,
                        resource.getQuantityString(
                            R.plurals.photos_deleted_count,
                            deletedCount,
                            deletedCount
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                latestCapturedImageUri?.let { uri ->
                    viewModel.deletePhoto(uri) { success ->
                        if (success) {
                            Toast.makeText(
                                context,
                                R.string.photo_deleted_successfully,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.exitPreviewMode()
                        } else {
                            Toast.makeText(
                                context,
                                R.string.photo_deletion_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            showDeleteConfirmDialog = false
        },
        isSelectionMode = isSelectionMode,
        selectedPhotos = selectedPhotos,
        context = context
    )

    Scaffold(
        topBar = {
            CameraTopBar(
                showGalleryView = showGalleryView,
                isSelectionMode = isSelectionMode,
                selectedPhotosCount = selectedPhotos.size,
                allPhotosCount = allPhotos.size,
                onBackFromGallery = {
                    showGalleryView = false
                    viewModel.exitSelectionMode()
                },
                onToggleSelectAll = { viewModel.toggleSelectAll() },
                onRequestDelete = { showDeleteConfirmDialog = true },
                onStartSelectionMode = { viewModel.startSelectionMode() },
                showGalleryAction = allPhotos.isNotEmpty(),
                onOpenGallery = { showGalleryView = true },
                flashMode = flashMode,
                onToggleFlash = { viewModel.toggleFlash() },
                onSwitchCamera = { viewModel.switchCamera() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !hasCameraPermission -> {
                    PermissionGateContent(
                        title = stringResource(R.string.camera_permission_required),
                        buttonText = stringResource(R.string.grant_camera_permission),
                        onButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }

                !hasMediaPermissions -> {
                    PermissionGateContent(
                        title = stringResource(R.string.storage_permission_required),
                        buttonText = stringResource(R.string.grant_storage_permission),
                        onButtonClick = {
                            mediaPermissionsLauncher.launch(PermissionUtils.getRequiredMediaPermissions())
                        }
                    )
                }

                currentViewingPhoto != null -> {
                    PhotoDetailContent(
                        currentViewingPhoto = currentViewingPhoto,
                        allPhotos = allPhotos,
                        onBack = { viewModel.setCurrentViewingPhoto(null) },
                        onDelete = { photo ->
                            viewModel.deletePhoto(photo.uri) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "写真を削除しました",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    viewModel.setCurrentViewingPhoto(null)
                                }
                            }
                        },
                        onNext = { viewModel.goToNextPhoto() },
                        onPrevious = { viewModel.goToPreviousPhoto() }
                    )
                }

                showGalleryView -> {
                    GalleryContent(
                        allPhotos = allPhotos,
                        isLoadingPhotos = isLoadingPhotos,
                        selectedPhotos = selectedPhotos,
                        isSelectionMode = isSelectionMode,
                        onPhotoClick = { photo ->
                            if (isSelectionMode) {
                                viewModel.togglePhotoSelection(photo.uri)
                            } else {
                                viewModel.setCurrentViewingPhoto(photo)
                            }
                        },
                        onPhotoLongClick = { photo ->
                            if (!isSelectionMode) {
                                viewModel.startSelectionMode()
                                viewModel.togglePhotoSelection(photo.uri)
                            }
                        }
                    )
                }

                isPreviewMode && latestCapturedImageUri != null -> {
                    PhotoPreviewContent(
                        imageUri = latestCapturedImageUri.toString(),
                        onDelete = { showDeleteConfirmDialog = true },
                        onBack = { viewModel.exitPreviewMode() }
                    )
                }

                else -> {
                    CameraCaptureContent(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        viewModel = viewModel,
                        cameraSelector = cameraSelector,
                        flashMode = flashMode,
                        zoomRatio = zoomRatio,
                        minZoomRatio = minZoomRatio,
                        maxZoomRatio = maxZoomRatio,
                        needsCameraRebind = needsCameraRebind,
                        hasMediaPermissions = hasMediaPermissions,
                        latestCapturedImageUri = latestCapturedImageUri,
                        canSwitchLens = canSwitchLens,
                        currentLensType = currentLensType,
                        lensDisplayName = lensDisplayName,
                        showLensSwitchFeedback = showLensSwitchFeedback,
                        lensSwitchFeedbackName = lensSwitchFeedbackName,
                        onShowLensSwitchFeedback = { visible -> showLensSwitchFeedback = visible },
                        onLensSwitchFeedbackNameChanged = { name -> lensSwitchFeedbackName = name },
                        imageCapture = imageCapture,
                        onImageCaptureChanged = { imageCapture = it },
                        camera = camera,
                        onCameraChanged = { camera = it },
                        cameraProvider = cameraProvider,
                        onCameraProviderChanged = { cameraProvider = it },
                        previewView = previewView,
                        onPreviewViewChanged = { previewView = it },
                        preview = preview,
                        onPreviewChanged = { preview = it }
                    )
                }

                // Auto-hide lens switch feedback after delay
//                     LaunchedEffect(key1 = showLensSwitchFeedback) {
//                         if (showLensSwitchFeedback) {
//                             kotlinx.coroutines.delay(1500)
//                             showLensSwitchFeedback = false
//                         }
//                     }
            }
        }
    }
}

// end of CameraScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraTopBar(
    showGalleryView: Boolean,
    isSelectionMode: Boolean,
    selectedPhotosCount: Int,
    allPhotosCount: Int,
    onBackFromGallery: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onRequestDelete: () -> Unit,
    onStartSelectionMode: () -> Unit,
    showGalleryAction: Boolean,
    onOpenGallery: () -> Unit,
    flashMode: Int,
    onToggleFlash: () -> Unit,
    onSwitchCamera: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                if (showGalleryView) "ギャラリー" else stringResource(R.string.camera_title)
            )
        },
        navigationIcon = {
            if (showGalleryView) {
                IconButton(onClick = onBackFromGallery) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る"
                    )
                }
            }
        },
        actions = {
            if (showGalleryView) {
                if (isSelectionMode) {
                    IconButton(onClick = onToggleSelectAll) {
                        Icon(
                            imageVector = if (selectedPhotosCount == allPhotosCount && allPhotosCount > 0)
                                Icons.Default.SelectAll else Icons.Default.CheckBox,
                            contentDescription = "全選択"
                        )
                    }
                    IconButton(
                        onClick = onRequestDelete,
                        enabled = selectedPhotosCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "削除"
                        )
                    }
                } else {
                    IconButton(onClick = onStartSelectionMode) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "選択"
                        )
                    }
                }
            } else {
                if (showGalleryAction) {
                    IconButton(onClick = onOpenGallery) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "ギャラリー"
                        )
                    }
                }
                IconButton(onClick = onToggleFlash) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = stringResource(R.string.flash_mode_toggle)
                    )
                }
                IconButton(onClick = onSwitchCamera) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.switch_camera)
                    )
                }
            }
        }
    )
}


@Composable
private fun PermissionGateContent(
    title: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    PermissionRequestComponent(
        title = title,
        buttonText = buttonText,
        onButtonClick = onButtonClick
    )
}


@Composable
private fun PhotoDetailContent(
    currentViewingPhoto: PhotoItem?,
    allPhotos: List<PhotoItem>,
    onBack: () -> Unit,
    onDelete: (PhotoItem) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    if (currentViewingPhoto == null) return

    PhotoDetailView(
        photo = currentViewingPhoto,
        onBack = onBack,
        onDelete = onDelete,
        onNext = onNext,
        onPrevious = onPrevious,
        hasNext = allPhotos.isNotEmpty() &&
            allPhotos.indexOfFirst { it.id == currentViewingPhoto.id } < allPhotos.size - 1,
        hasPrevious = allPhotos.isNotEmpty() &&
            allPhotos.indexOfFirst { it.id == currentViewingPhoto.id } > 0
    )
}


@Composable
private fun GalleryContent(
    allPhotos: List<PhotoItem>,
    isLoadingPhotos: Boolean,
    selectedPhotos: Set<android.net.Uri>,
    isSelectionMode: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    onPhotoLongClick: (PhotoItem) -> Unit
) {
    GalleryView(
        photos = allPhotos,
        isLoading = isLoadingPhotos,
        selectedPhotos = selectedPhotos,
        isSelectionMode = isSelectionMode,
        onPhotoClick = onPhotoClick,
        onPhotoLongClick = onPhotoLongClick
    )
}


@Composable
private fun PhotoPreviewContent(
    imageUri: String,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    PhotoPreviewComponent(
        imageUri = imageUri,
        onDelete = onDelete,
        onBack = onBack
    )
}


@Composable
private fun CameraDialogs(
    showPartialAccessDialog: Boolean,
    onDismissPartialAccessDialog: () -> Unit,
    showDeleteConfirmDialog: Boolean,
    onDismissDeleteConfirmDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    isSelectionMode: Boolean,
    selectedPhotos: Set<android.net.Uri>,
    context: android.content.Context
) {
    if (showPartialAccessDialog) {
        PartialAccessDialog(
            onDismiss = onDismissPartialAccessDialog,
            context = context
        )
    }

    if (showDeleteConfirmDialog) {
        val deleteDialogMessage = if (isSelectionMode && selectedPhotos.isNotEmpty()) {
            stringResource(R.string.delete_selected_library_photos_message)
        } else {
            stringResource(R.string.delete_library_photo_message)
        }
        DeleteConfirmDialog(
            onDismiss = onDismissDeleteConfirmDialog,
            onConfirm = onConfirmDelete,
            text = deleteDialogMessage
        )
    }
}


@Composable
private fun CameraCaptureContent(
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner,
    viewModel: CameraViewModel,
    cameraSelector: CameraSelector,
    flashMode: Int,
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    needsCameraRebind: Boolean,
    hasMediaPermissions: Boolean,
    latestCapturedImageUri: android.net.Uri?,
    canSwitchLens: Boolean,
    currentLensType: CameraCapabilityManager.LensType,
    lensDisplayName: String,
    showLensSwitchFeedback: Boolean,
    lensSwitchFeedbackName: String,
    onShowLensSwitchFeedback: (Boolean) -> Unit,
    onLensSwitchFeedbackNameChanged: (String) -> Unit,
    imageCapture: ImageCapture?,
    onImageCaptureChanged: (ImageCapture?) -> Unit,
    camera: Camera?,
    onCameraChanged: (Camera?) -> Unit,
    cameraProvider: ProcessCameraProvider?,
    onCameraProviderChanged: (ProcessCameraProvider?) -> Unit,
    previewView: PreviewView?,
    onPreviewViewChanged: (PreviewView?) -> Unit,
    preview: Preview?,
    onPreviewChanged: (Preview?) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        viewModel.apply {
            setMaxZoomRatio(camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 10.0f)
            setMinZoomRatio(camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f)
        }

        val currentOrientation = LocalConfiguration.current.orientation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = when (currentOrientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> 90f
                        else -> 0f
                    }
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        onPreviewViewChanged(this)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .modernCameraGestures(
                        onScale = { newZoom ->
                            viewModel.smoothZoomTo(
                                targetZoom = newZoom,
                                duration = 0
                            )
                        },
                        onDoubleTap = {
                            viewModel.resetZoom()
                        },
                        onTap = { offset ->
                            previewView?.let {
                                viewModel.focusOnPoint(
                                    it,
                                    offset.x,
                                    offset.y
                                )
                            }
                        },
                        onLensSwitch = {
                            viewModel.switchLens()
                            onLensSwitchFeedbackNameChanged(lensDisplayName)
                            onShowLensSwitchFeedback(true)
                        },
                        currentZoom = zoomRatio,
                        minZoom = minZoomRatio,
                        maxZoom = maxZoomRatio,
                        canSwitchLens = canSwitchLens,
                        enableHapticFeedback = true,
                        zoomSensitivity = 2.4f,
                        lensSwitchThreshold = 1.0f
                    )
            ) { view ->
                if (cameraProvider == null) {
                    val cameraProviderFuture =
                        ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        onCameraProviderChanged(cameraProviderFuture.get())

                        val createdPreview = Preview.Builder().build().also {
                            it.surfaceProvider = view.surfaceProvider
                        }
                        onPreviewChanged(createdPreview)

                        bindCameraWithPreview(
                            lifecycleOwner = lifecycleOwner,
                            cameraProvider = cameraProviderFuture.get(),
                            preview = createdPreview,
                            cameraSelector = cameraSelector,
                            flashMode = flashMode,
                            initialZoomRatio = zoomRatio,
                            onImageCaptureCreated = { capture ->
                                onImageCaptureChanged(capture)
                            },
                            onCameraCreated = { cam ->
                                onCameraChanged(cam)
                                viewModel.setCamera(cam)
                            }
                        )
                    }, ContextCompat.getMainExecutor(context))
                }
            }

            ZoomLevelButtons(
                zoomRatio = zoomRatio,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio,
                onZoomSelected = { level -> viewModel.smoothZoomTo(level) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
            )
        }

        LaunchedEffect(cameraSelector, needsCameraRebind) {
            if (cameraProvider != null && preview != null) {
                Log.d(
                    "CameraScreen",
                    "Rebinding camera - Selector: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}, Flash: $flashMode, NeedsRebind: $needsCameraRebind"
                )

                if (needsCameraRebind && previewView != null) {
                    Log.d("CameraScreen", "Creating new Preview for rebind")
                    val newPreview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    onPreviewChanged(newPreview)
                    viewModel.onCameraRebound()
                }

                bindCameraWithPreview(
                    lifecycleOwner = lifecycleOwner,
                    cameraProvider = cameraProvider,
                    preview = preview,
                    cameraSelector = cameraSelector,
                    flashMode = flashMode,
                    initialZoomRatio = zoomRatio,
                    onImageCaptureCreated = { capture ->
                        onImageCaptureChanged(capture)
                    },
                    onCameraCreated = { cam ->
                        onCameraChanged(cam)
                        viewModel.setCamera(cam)
                    }
                )
            }
        }

        ZoomInfoOverlay(
            zoomRatio = zoomRatio,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        CaptureFab(
            onClick = {
                imageCapture?.let { capture ->
                    viewModel.takePhoto(
                        imageCapture = capture,
                        onPhotoSaved = { },
                        onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        ThumbnailPreview(
            hasMediaPermissions = hasMediaPermissions,
            latestCapturedImageUri = latestCapturedImageUri,
            onClick = { viewModel.enterPreviewMode() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        LensIndicator(
            lensType = currentLensType,
            lensDisplayName = lensDisplayName,
            canSwitchLens = canSwitchLens,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        if (canSwitchLens) {
            LensSwitchHint(
                canSwitchLens = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
            )
        }

        LensSwitchFeedback(
            isVisible = showLensSwitchFeedback,
            newLensName = lensSwitchFeedbackName,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}


@Composable
private fun ZoomLevelButtons(
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val zoomLevels = listOf(0.5f, 1f, 2f)
        zoomLevels
            .filter { it in minZoomRatio..maxZoomRatio }
            .forEach { level ->
                val isSelected = kotlin.math.abs(zoomRatio - level) < 0.01f
                Button(
                    onClick = { onZoomSelected(level) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 4.dp
                    ),
                    shape = CircleShape
                ) {
                    val label = if (level % 1f == 0f) level.toInt().toString() else level.toString()
                    Text("${label}x")
                }
            }
    }
}


@Composable
private fun ZoomInfoOverlay(
    zoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(
                R.string.zoom_info,
                zoomRatio, minZoomRatio, maxZoomRatio
            ),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}


@Composable
private fun CaptureFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = stringResource(R.string.take_photo)
        )
    }
}


@Composable
private fun ThumbnailPreview(
    hasMediaPermissions: Boolean,
    latestCapturedImageUri: android.net.Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canInteractWithThumbnail = hasMediaPermissions && latestCapturedImageUri != null
    Box(
        modifier = modifier.then(
            if (canInteractWithThumbnail) {
                Modifier.pointerInput(latestCapturedImageUri) {
                    detectTapGestures(
                        onLongPress = {},
                        onTap = { onClick() }
                    )
                }
            } else {
                Modifier
            }
        )
    ) {
        if (canInteractWithThumbnail && latestCapturedImageUri != null) {
            AsyncImage(
                model = latestCapturedImageUri,
                contentDescription = stringResource(R.string.latest_library_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (!canInteractWithThumbnail) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = stringResource(R.string.latest_library_photo),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

