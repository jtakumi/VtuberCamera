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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vtubercamera.BuildConfig
import com.example.vtubercamera.R
import com.example.vtubercamera.ui.components.AsyncImage
import com.example.vtubercamera.ui.components.DeleteConfirmDialog
import com.example.vtubercamera.ui.components.GalleryView
import com.example.vtubercamera.ui.components.PartialAccessDialog
import com.example.vtubercamera.ui.components.PermissionRequestComponent
import com.example.vtubercamera.ui.components.PhotoPreviewComponent
import com.example.vtubercamera.ui.components.PhotoDetailView
import com.example.vtubercamera.ui.modifiers.modernCameraGestures
import com.example.vtubercamera.ui.viewmodels.CameraViewModel
import com.example.vtubercamera.ui.viewmodels.PhotoItem
import com.example.vtubercamera.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // デバイス設定の読み取り
    val configuration = LocalConfiguration.current
    val language = configuration.locales[0].language
    val country = configuration.locales[0].country
    val orientation = configuration.orientation
    val density = configuration.densityDpi
    val screenDp = LocalWindowInfo.current.containerSize
    val uiMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val isNightMode = uiMode == Configuration.UI_MODE_NIGHT_YES
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

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var showPartialAccessDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showGalleryView by remember { mutableStateOf(false) }

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
            viewModel.loadAllPhotos(context)
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
                    val deletedCount = viewModel.deleteSelectedPhotos(context)
                    Toast.makeText(
                        context,
                        context.resources.getQuantityString(
                            R.plurals.photos_deleted_count,
                            deletedCount,
                            deletedCount
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    lastCapturedImageUri?.let { uri ->
                        val success = viewModel.deletePhoto(context, uri)
                        if (success) {
                            Toast.makeText(
                                context,
                                R.string.photo_deleted_successfully,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearLastCapturedImage(context)
                        } else {
                            Toast.makeText(
                                context,
                                R.string.photo_deletion_failed,
                                Toast.LENGTH_SHORT
                            ).show()
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
                            val success = viewModel.deletePhoto(context, photo.uri)
                            if (success) {
                                Toast.makeText(context, "写真を削除しました", Toast.LENGTH_SHORT)
                                    .show()
                                viewModel.setCurrentViewingPhoto(null)
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

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = when (context.resources.configuration.orientation) {
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
                                    currentZoom = zoomRatio,
                                    minZoom = minZoomRatio,
                                    maxZoom = maxZoomRatio,
                                    enableHapticFeedback = true,
                                    zoomSensitivity = 2.4f
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

                        // 設定情報のデバッグ表示
                        if (BuildConfig.DEBUG) {
                            ConfigurationDebugPanel(
                                language = language,
                                country = country,
                                orientation = orientation,
                                density = density,
                                screenWidthDp = screenDp.width,
                                screenHeightDp = screenDp.height,
                                isNightMode = isNightMode,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            )
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
                                        context = context,
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

@Composable
private fun ConfigurationDebugPanel(
    language: String,
    country: String,
    orientation: Int,
    density: Int,
    screenWidthDp: Int,
    screenHeightDp: Int,
    isNightMode: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "設定情報",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Text(
                text = "言語: $language-$country",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Text(
                text = "向き: ${if (orientation == Configuration.ORIENTATION_LANDSCAPE) "横" else "縦"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Text(
                text = "密度: ${density}dpi",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Text(
                text = "画面: ${screenWidthDp}×${screenHeightDp}dp",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Text(
                text = "テーマ: ${if (isNightMode) "ダーク" else "ライト"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}