package com.example.vtubercamera.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vtubercamera.ui.components.AsyncImage
import com.example.vtubercamera.ui.viewmodels.CameraViewModel

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
    val cameraRebindTrigger by viewModel.cameraRebindTrigger.collectAsStateWithLifecycle()

    // カメラ状態管理
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // カメラプロバイダーの初期化
    LaunchedEffect(Unit) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    // カメラの再バインド処理（プレビューモードからの復帰時も含む）
    LaunchedEffect(cameraSelector, flashMode, cameraRebindTrigger) {
        if (cameraProvider != null && previewView != null && !isPreviewMode) {
            Log.d(
                "CameraScreen",
                "Rebinding camera - Selector: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}, Flash: $flashMode, Trigger: $cameraRebindTrigger"
            )

            try {
                // 既存のバインディングを解除
                cameraProvider!!.unbindAll()

                // 新しいプレビューとImageCaptureを作成
                val preview = Preview.Builder().build()
                val newImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(flashMode)
                    .build()

                // SurfaceProviderを設定
                preview.surfaceProvider = previewView!!.surfaceProvider

                // カメラをバインド
                val newCamera = cameraProvider!!.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newImageCapture
                )

                imageCapture = newImageCapture
                camera = newCamera
                viewModel.setCamera(newCamera)

                Log.d("CameraScreen", "Camera rebound successfully")
            } catch (e: Exception) {
                Log.e("CameraScreen", "Failed to rebind camera", e)
            }
        }
    }

    // クリーンアップ
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("カメラ") },
                actions = {
                    IconButton(onClick = { viewModel.toggleFlash() }) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            },
                            contentDescription = "フラッシュモード切り替え"
                        )
                    }
                    IconButton(onClick = { viewModel.switchCamera() }) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "カメラ切り替え"
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
            if (hasCameraPermission) {
                if (isPreviewMode && lastCapturedImageUri != null) {
                    // 写真プレビューモード
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = lastCapturedImageUri!!,
                            contentDescription = "撮影した写真",
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
                                onClick = { viewModel.clearLastCapturedImage() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("削除")
                            }
                            Button(
                                onClick = { viewModel.exitPreviewMode() }
                            ) {
                                Text("戻る")
                            }
                        }
                    }
                } else {
                    // カメラプレビューモード
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                previewView = this
                                Log.d("CameraScreen", "PreviewView created")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // コントロールUI
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setZoom(zoomRatio - 0.5f) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "ズームアウト"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setZoom(zoomRatio + 0.5f) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "ズームイン"
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            imageCapture?.let { capture ->
                                viewModel.takePhoto(
                                    imageCapture = capture,
                                    context = context,
                                    onPhotoSaved = { /* トーストメッセージを削除 */ },
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
                            contentDescription = "写真を撮影"
                        )
                    }

                    lastCapturedImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.enterPreviewMode() }
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "最後に撮影した写真",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("カメラの使用許可が必要です")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("許可をリクエスト")
                    }
                }
            }
        }
    }
}