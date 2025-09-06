package com.example.vtubercamera.ui.screens

import android.Manifest
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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vtubercamera.R
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
import com.example.vtubercamera.ui.viewmodels.CameraViewModel
import com.example.vtubercamera.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // デバイス設定の読み取り
    val configuration = LocalConfiguration.current
    configuration.locales[0].language
    configuration.locales[0].country
    configuration.orientation
    configuration.densityDpi
    LocalWindowInfo.current.containerSize
    val uiMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    uiMode == Configuration.UI_MODE_NIGHT_YES
    val cameraSelector by viewModel.cameraSelector.collectAsStateWithLifecycle()
    val lastCapturedImageUri by viewModel.lastCapturedImageUri.collectAsStateWithLifecycle()
    val isPreviewMode by viewModel.isPreviewMode.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val maxZoomRatio by viewModel.maxZoomRatio.collectAsStateWithLifecycle()
    val minZoomRatio by viewModel.minZoomRatio.collectAsStateWithLifecycle()
    val needsCameraRebind by viewModel.needsCameraRebind.collectAsStateWithLifecycle()
    val focusPoint by viewModel.focusPoint.collectAsStateWithLifecycle()
    val allPhotos by viewModel.allPhotos.collectAsStateWithLifecycle()
    val isLoadingPhotos by viewModel.isLoadingPhotos.collectAsStateWithLifecycle()
    val selectedPhotos by viewModel.selectedPhotos.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val currentViewingPhoto by viewModel.currentViewingPhoto.collectAsStateWithLifecycle()
    val canSwitchLens by viewModel.canSwitchLens.collectAsStateWithLifecycle()
    val currentLensType by viewModel.currentLensType.collectAsStateWithLifecycle()
    val lensDisplayName by viewModel.lensDisplayName.collectAsStateWithLifecycle()

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

    if (showPartialAccessDialog) {
        PartialAccessDialog(
            onDismiss = { showPartialAccessDialog = false },
            context = context
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                if (isSelectionMode && selectedPhotos.isNotEmpty()) {
                    viewModel.deleteSelectedPhotos { deletedCount ->
                        Toast.makeText(
                            context,
                            context.resources.getQuantityString(
                                R.plurals.photos_deleted_count,
                                deletedCount,
                                deletedCount
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    lastCapturedImageUri?.let { uri ->
                        viewModel.deletePhoto(uri) { success ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    R.string.photo_deleted_successfully,
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.clearLastCapturedImage()
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
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showGalleryView) "ギャラリー" else stringResource(R.string.camera_title)
                    )
                },
                navigationIcon = {
                    if (showGalleryView) {
                        IconButton(onClick = {
                            showGalleryView = false
                            viewModel.exitSelectionMode()
                        }) {
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
                            IconButton(onClick = { viewModel.toggleSelectAll() }) {
                                Icon(
                                    imageVector = if (selectedPhotos.size == allPhotos.size && allPhotos.isNotEmpty())
                                        Icons.Default.SelectAll else Icons.Default.CheckBox,
                                    contentDescription = "全選択"
                                )
                            }
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                enabled = selectedPhotos.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "削除"
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.startSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "選択"
                                )
                            }
                        }
                    } else {
                        if (allPhotos.isNotEmpty()) {
                            IconButton(onClick = { showGalleryView = true }) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = "ギャラリー"
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleFlash() }) {
                            Icon(
                                imageVector = when (flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = stringResource(R.string.flash_mode_toggle)
                            )
                        }
                        IconButton(onClick = { viewModel.switchCamera() }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = stringResource(R.string.switch_camera)
                            )
                        }
                    }
                }
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
                    PermissionRequestComponent(
                        title = stringResource(R.string.camera_permission_required),
                        buttonText = stringResource(R.string.grant_camera_permission),
                        onButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }

                !hasMediaPermissions -> {
                    PermissionRequestComponent(
                        title = stringResource(R.string.storage_permission_required),
                        buttonText = stringResource(R.string.grant_storage_permission),
                        onButtonClick = {
                            mediaPermissionsLauncher.launch(PermissionUtils.getRequiredMediaPermissions())
                        }
                    )
                }

                currentViewingPhoto != null -> {
                    PhotoDetailView(
                        photo = currentViewingPhoto!!,
                        onBack = { viewModel.setCurrentViewingPhoto(null) },
                        onDelete = { photo ->
                            viewModel.deletePhoto(photo.uri) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "写真を削除しました",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                    viewModel.setCurrentViewingPhoto(null)
                                }
                            }
                        },
                        onNext = { viewModel.goToNextPhoto() },
                        onPrevious = { viewModel.goToPreviousPhoto() },
                        hasNext = allPhotos.isNotEmpty() &&
                                allPhotos.indexOfFirst { it.id == currentViewingPhoto!!.id } < allPhotos.size - 1,
                        hasPrevious = allPhotos.isNotEmpty() &&
                                allPhotos.indexOfFirst { it.id == currentViewingPhoto!!.id } > 0
                    )
                }

                showGalleryView -> {
                    GalleryView(
                        photos = allPhotos,
                        isLoading = isLoadingPhotos,
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

                isPreviewMode -> {
                    PhotoPreviewComponent(
                        imageUri = lastCapturedImageUri.toString(),
                        onDelete = { showDeleteConfirmDialog = true },
                        onBack = { viewModel.exitPreviewMode() }
                    )
                }

                else -> {
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
                                    previewView = this
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
                                        lensSwitchFeedbackName = lensDisplayName
                                        showLensSwitchFeedback = true
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
                                    cameraProvider = cameraProviderFuture.get()

                                    preview = Preview.Builder().build().also {
                                        it.surfaceProvider = view.surfaceProvider
                                    }

                                    bindCameraWithPreview(
                                        lifecycleOwner = lifecycleOwner,
                                        cameraProvider = cameraProvider!!,
                                        preview = preview!!,
                                        cameraSelector = cameraSelector,
                                        flashMode = flashMode,
                                        initialZoomRatio = zoomRatio,
                                        onImageCaptureCreated = { capture ->
                                            imageCapture = capture
                                        },
                                        onCameraCreated = { cam ->
                                            camera = cam
                                            viewModel.setCamera(cam)
                                        }
                                    )
                                }, ContextCompat.getMainExecutor(context))
                            }
                        }


                        focusPoint?.let { (x, y) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = ((x - 5f).dp).coerceAtLeast(0.dp),
                                            y = ((y - 5f).dp).coerceAtLeast(0.dp)
                                        )
                                        .background(
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color.Black,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        LaunchedEffect(cameraSelector, needsCameraRebind) {
                            if (cameraProvider != null && preview != null) {
                                Log.d(
                                    "CameraScreen",
                                    "Rebinding camera - Selector: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}, Flash: $flashMode, NeedsRebind: $needsCameraRebind"
                                )

                                if (needsCameraRebind && previewView != null) {
                                    Log.d("CameraScreen", "Creating new Preview for rebind")
                                    preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView!!.surfaceProvider
                                    }
                                    viewModel.onCameraRebound()
                                }

                                bindCameraWithPreview(
                                    lifecycleOwner = lifecycleOwner,
                                    cameraProvider = cameraProvider!!,
                                    preview = preview!!,
                                    cameraSelector = cameraSelector,
                                    flashMode = flashMode,
                                    initialZoomRatio = zoomRatio,
                                    onImageCaptureCreated = { capture ->
                                        imageCapture = capture
                                    },
                                    onCameraCreated = { cam ->
                                        camera = cam
                                        viewModel.setCamera(cam)
                                    }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
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

                        FloatingActionButton(
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
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = stringResource(R.string.take_photo)
                            )
                        }
                        // 最後に撮影した写真のサムネイル
                        lastCapturedImageUri?.let { uri ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                showDeleteConfirmDialog = true
                                            },
                                            onTap = {
                                                viewModel.enterPreviewMode()
                                            }
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = stringResource(R.string.last_captured_photo),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Lens switching indicators and feedback
                        LensIndicator(
                            lensType = currentLensType,
                            lensDisplayName = lensDisplayName,
                            canSwitchLens = canSwitchLens,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        )

                        // Lens switch hint (shown when switching is available)
                        if (canSwitchLens) {
                            LensSwitchHint(
                                canSwitchLens = canSwitchLens,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 60.dp)
                            )
                        }

                        // Lens switch feedback overlay
                        LensSwitchFeedback(
                            isVisible = showLensSwitchFeedback,
                            newLensName = lensSwitchFeedbackName,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Auto-hide lens switch feedback after delay
                    LaunchedEffect(showLensSwitchFeedback) {
                        if (showLensSwitchFeedback) {
                            kotlinx.coroutines.delay(1500)
                            showLensSwitchFeedback = false
                        }
                    }
                }
            }
        }
    }
}


private fun bindCameraWithPreview(
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    preview: Preview,
    cameraSelector: CameraSelector,
    flashMode: Int,
    initialZoomRatio: Float? = null,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCameraCreated: (Camera) -> Unit
): Camera? {
    return try {
        Log.d(
            "CameraBinding",
            "Binding camera with flash mode: $flashMode, camera: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}"
        )

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()

        cameraProvider.unbindAll()

        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        onImageCaptureCreated(imageCapture)
        onCameraCreated(camera)

        initialZoomRatio?.let { targetZoom ->
            try {
                val minZoom = camera.cameraInfo.zoomState.value?.minZoomRatio ?: 1.0f
                val maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 10.0f
                val coerced = targetZoom.coerceIn(minZoom, maxZoom)
                camera.cameraControl.setZoomRatio(coerced)
                Log.d("CameraBinding", "Restored zoom ratio to $coerced after rebind")
            } catch (e: Exception) {
                Log.w("CameraBinding", "ズーム復元に失敗しました", e)
            }
        }

        Log.d("CameraBinding", "Camera bound successfully")

        camera
    } catch (e: Exception) {
        Log.e("CameraBinding", "カメラのバインドに失敗しました", e)
        null
    }
}
