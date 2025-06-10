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
import androidx.lifecycle.LifecycleOwner
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
    
    // カメラ状態管理の改善
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var preview: Preview? by remember { mutableStateOf(null) }
    
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
                    // AndroidViewの改善
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
                        modifier = Modifier.fillMaxSize()
                    ) { view ->
                        // 初回のみカメラプロバイダーを初期化
                        if (cameraProvider == null) {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                cameraProvider = cameraProviderFuture.get()
                                
                                // プレビューを一度だけ作成してSurfaceProviderを設定
                                preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(view.surfaceProvider)
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
                    LaunchedEffect(cameraSelector, flashMode, isPreviewMode) {
                        // カメラプロバイダーとプレビューが準備できている場合のみ再バインド
                        if (cameraProvider != null && preview != null) {
                            Log.d("CameraScreen", "Rebinding camera due to state change")
                            bindCameraWithPreview(
                                lifecycleOwner = lifecycleOwner,
                                cameraProvider = cameraProvider!!,
                                preview = preview!!, // 既存のプレビューを再利用
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
        Log.d("CameraBinding", "Binding camera with flash mode: $flashMode, camera: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}")

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