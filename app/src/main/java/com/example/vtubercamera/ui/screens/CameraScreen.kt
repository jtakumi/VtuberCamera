package com.example.vtubercamera.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vtubercamera.R
import com.example.vtubercamera.ui.components.AsyncImage
import com.example.vtubercamera.ui.modifiers.modernCameraGestures
import com.example.vtubercamera.ui.viewmodels.CameraViewModel
import com.example.vtubercamera.utils.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraSelector by viewModel.cameraSelector.collectAsStateWithLifecycle()
    val lastCapturedImageUri by viewModel.lastCapturedImageUri.collectAsStateWithLifecycle()
    val isPreviewMode by viewModel.isPreviewMode.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val needsCameraRebind by viewModel.needsCameraRebind.collectAsStateWithLifecycle()

    // カメラ状態管理の改善
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    var showPartialAccessDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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

    // カメラ権限リクエスト用ランチャー
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )
    // メディアアクセス権限リクエスト用ランチャー
    val mediaPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasMediaPermissions = permissions.values.all { it }

            // Android 15: パーシャルアクセスの確認
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasPartialAccess = PermissionUtils.hasPartialMediaAccess(context)
                if (hasPartialAccess && !permissions[Manifest.permission.READ_MEDIA_IMAGES]!!) {
                    showPartialAccessDialog = true
                }
            }
        }
    )

    // 初期権限チェック
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (!hasMediaPermissions) {
            mediaPermissionsLauncher.launch(PermissionUtils.getRequiredMediaPermissions())
        }
    }

    // パーシャルアクセスダイアログ（Android 15対応）
    if (showPartialAccessDialog) {
        AlertDialog(
            onDismissRequest = { showPartialAccessDialog = false },
            title = { Text(stringResource(R.string.photo_access)) },
            text = { Text(stringResource(R.string.partial_access_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPartialAccessDialog = false
                        // 設定画面を開く
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPartialAccessDialog = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }

    // 削除確認ダイアログ
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_photo_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
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
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_title)) },
                actions = {
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.camera_permission_required))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text(stringResource(R.string.grant_camera_permission))
                        }
                    }
                }

                !hasMediaPermissions -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.storage_permission_required))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            mediaPermissionsLauncher.launch(PermissionUtils.getRequiredMediaPermissions())
                        }) {
                            Text(stringResource(R.string.grant_storage_permission))
                        }
                    }
                }

                else -> {
                    if (isPreviewMode && lastCapturedImageUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = lastCapturedImageUri!!,
                                contentDescription = stringResource(R.string.captured_photo),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { showDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(stringResource(R.string.delete))
                                }
                                Button(
                                    onClick = { viewModel.exitPreviewMode() }
                                ) {
                                    Text(stringResource(R.string.back))
                                }
                            }
                        }
                    } else {
                        // カメラプレビューとコントロールを回転可能なBoxでラップ
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    // 画面の向きに応じて回転
                                    rotationZ = when (context.resources.configuration.orientation) {
                                        android.content.res.Configuration.ORIENTATION_LANDSCAPE -> 90f
                                        else -> 0f
                                    }
                                }
                        ) {
                            // AndroidViewの改善（元のコードの構造を保持）
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        previewView = this // PreviewViewの参照を保存
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .modernCameraGestures(
                                        onScale = { newZoom ->
                                            viewModel.setZoom(newZoom)
                                        },
                                        onDoubleTap = {
                                            viewModel.resetZoom()
                                        },
                                        currentZoom = zoomRatio,
                                        minZoom = camera?.cameraInfo?.zoomState?.value?.minZoomRatio
                                            ?: 1.0f,
                                        maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio
                                            ?: 10.0f,
                                        enableHapticFeedback = true,
                                        zoomSensitivity = 2.4f
                                    )
                            ) { view ->
                                // 初回のみカメラプロバイダーを初期化
                                if (cameraProvider == null) {
                                    val cameraProviderFuture =
                                        ProcessCameraProvider.getInstance(context)
                                    cameraProviderFuture.addListener({
                                        cameraProvider = cameraProviderFuture.get()

                                        // プレビューを一度だけ作成してSurfaceProviderを設定
                                        preview = Preview.Builder().build().also {
                                            it.surfaceProvider = view.surfaceProvider
                                        }

                                        // 初回バインド
                                        bindCameraWithPreview(
                                            lifecycleOwner = lifecycleOwner,
                                            cameraProvider = cameraProvider!!,
                                            preview = preview!!,
                                            cameraSelector = cameraSelector,
                                            flashMode = flashMode,
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

                            // 状態変更の監視と賢い再バインド
                            LaunchedEffect(cameraSelector, flashMode, needsCameraRebind) {
                                // カメラプロバイダーとプレビューが準備できている場合のみ再バインド
                                if (cameraProvider != null && preview != null) {
                                    Log.d(
                                        "CameraScreen",
                                        "Rebinding camera - Selector: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}, Flash: $flashMode, NeedsRebind: $needsCameraRebind"
                                    )

                                    // needsCameraRebindがtrueの場合は、新しいPreviewを作成
                                    if (needsCameraRebind && previewView != null) {
                                        Log.d("CameraScreen", "Creating new Preview for rebind")
                                        preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView!!.surfaceProvider
                                        }
                                        viewModel.onCameraRebound() // フラグをリセット
                                    }

                                    bindCameraWithPreview(
                                        lifecycleOwner = lifecycleOwner,
                                        cameraProvider = cameraProvider!!,
                                        preview = preview!!, // 新しいまたは既存のプレビューを使用
                                        cameraSelector = cameraSelector,
                                        flashMode = flashMode,
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

                            // ズーム情報表示
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
                                val maxZoomRatio =
                                    camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 10.0f
                                val minZoomRatio =
                                    camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1.0f
                                Text(
                                    text = stringResource(
                                        R.string.zoom_info,
                                        zoomRatio, minZoomRatio, maxZoomRatio
                                    ),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            // シャッターボタン
                            FloatingActionButton(
                                onClick = {
                                    imageCapture?.let { capture ->
                                        viewModel.takePhoto(
                                            imageCapture = capture,
                                            context = context,
                                            onPhotoSaved = { /* トーストメッセージを削除 */ },
                                            onError = { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT)
                                                    .show()
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
}

// 既存のPreviewを再利用するバインド関数
private fun bindCameraWithPreview(
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    preview: Preview, // 既存のPreviewを受け取る
    cameraSelector: CameraSelector,
    flashMode: Int,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCameraCreated: (Camera) -> Unit
): Camera? {
    return try {
        Log.d(
            "CameraBinding",
            "Binding camera with flash mode: $flashMode, camera: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}"
        )

        // ImageCaptureのみ新しく作成（フラッシュモードを反映）
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()

        // 既存のバインディングを解除
        cameraProvider.unbindAll()

        // 既存のPreviewと新しいImageCaptureでバインド
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview, // 既存のPreviewを再利用
            imageCapture
        )

        onImageCaptureCreated(imageCapture)
        onCameraCreated(camera)

        Log.d("CameraBinding", "Camera bound successfully")

        camera
    } catch (e: Exception) {
        Log.e("CameraBinding", "カメラのバインドに失敗しました", e)
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun CameraScreenPreview() {
    // プレビュー用のモックUI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_title)) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.flash_mode_toggle)
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = stringResource(R.string.switch_camera)
                        )
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
            // カメラプレビューエリアのモック
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                // カメラプレビューのプレースホルダー
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.camera_preview_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.camera_preview_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // ズーム情報表示（右上）
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
                        2.0f, 1.0f, 10.0f
                    ),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // シャッターボタン（下部中央）
            FloatingActionButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = stringResource(R.string.take_photo)
                )
            }

            // 最後に撮影した写真のサムネイル（右下）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Preview,
                    contentDescription = stringResource(R.string.last_captured_photo),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
