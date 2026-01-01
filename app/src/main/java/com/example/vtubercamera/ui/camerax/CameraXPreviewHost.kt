package com.example.vtubercamera.ui.camerax

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Owns the full CameraX wiring:
 * - ProcessCameraProvider acquisition
 * - Preview instance creation
 * - bindCameraWithPreview invocation (including rebind)
 *
 * No UX is added here; this composable only hosts the camera preview.
 */
@Composable
internal fun CameraXPreviewHost(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraSelector: CameraSelector,
    flashMode: Int,
    zoomRatio: Float,
    needsCameraRebind: Boolean,
    modifier: Modifier = Modifier,
    onCameraReboundHandled: () -> Unit,
    onImageCaptureCreated: (ImageCapture) -> Unit,
    onCameraCreated: (Camera) -> Unit,
    onCameraProviderChanged: (ProcessCameraProvider?) -> Unit,
    onPreviewViewChanged: (PreviewView?) -> Unit,
    onPreviewChanged: (Preview?) -> Unit
) {
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var providerListenerRegistered by remember { mutableStateOf(false) }

    fun bindIfReady(forceNewPreview: Boolean) {
        val provider = cameraProvider ?: return
        val view = previewView ?: return

        val effectivePreview: Preview = if (forceNewPreview || preview == null) {
            Preview.Builder().build().also { createdPreview ->
                createdPreview.surfaceProvider = view.surfaceProvider
            }
        } else {
            preview!!
        }

        preview = effectivePreview
        onPreviewChanged(effectivePreview)

        bindCameraWithPreview(
            lifecycleOwner = lifecycleOwner,
            cameraProvider = provider,
            preview = effectivePreview,
            cameraSelector = cameraSelector,
            flashMode = flashMode,
            initialZoomRatio = zoomRatio,
            onImageCaptureCreated = onImageCaptureCreated,
            onCameraCreated = { camera ->
                boundCamera = camera
                onCameraCreated(camera)
            }
        )
    }

    val unbindAllSafelyState = rememberUpdatedState {
        val provider = cameraProvider
        if (provider == null) return@rememberUpdatedState

        try {
            provider.unbindAll()
        } catch (e: Exception) {
            Log.w("CameraXPreviewHost", "Failed to unbindAll()", e)
        } finally {
            boundCamera = null
            preview = null
            onPreviewChanged(null)
        }
    }

    val rebindIfReadyState = rememberUpdatedState {
        val providerReady = cameraProvider != null
        val viewReady = previewView != null
        if (!providerReady || !viewReady) return@rememberUpdatedState

        // Recreate Preview after stopping to ensure a valid surface provider.
        bindIfReady(forceNewPreview = true)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> unbindAllSafelyState.value.invoke()
                Lifecycle.Event.ON_START -> rebindIfReadyState.value.invoke()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unbindAllSafelyState.value.invoke()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                previewView = this
                onPreviewViewChanged(this)
            }
        },
        modifier = modifier
    ) {
        // Intentionally empty: binding is driven by LaunchedEffect below.
    }

    LaunchedEffect(cameraProviderFuture) {
        if (providerListenerRegistered) return@LaunchedEffect
        providerListenerRegistered = true

        cameraProviderFuture.addListener(
            {
                val provider = try {
                    cameraProviderFuture.get()
                } catch (e: Exception) {
                    Log.e("CameraXPreviewHost", "Failed to get ProcessCameraProvider", e)
                    null
                }

                cameraProvider = provider
                onCameraProviderChanged(provider)

                if (provider != null) {
                    // Initial bind creates a Preview.
                    bindIfReady(forceNewPreview = true)
                }
            },
            mainExecutor
        )
    }

    LaunchedEffect(
        cameraSelector,
        flashMode,
        needsCameraRebind,
        cameraProvider,
        previewView
    ) {
        val providerReady = cameraProvider != null
        val viewReady = previewView != null
        if (!providerReady || !viewReady) return@LaunchedEffect

        if (needsCameraRebind) {
            Log.d("CameraXPreviewHost", "Rebinding camera (requested)")
            bindIfReady(forceNewPreview = true)
            onCameraReboundHandled()
        } else {
            // Re-bind for selector/flash changes without forcing a new Preview.
            bindIfReady(forceNewPreview = false)
        }
    }

    LaunchedEffect(zoomRatio, boundCamera) {
        val camera = boundCamera ?: return@LaunchedEffect
        try {
            camera.cameraControl.setZoomRatio(zoomRatio)
        } catch (e: Exception) {
            Log.w("CameraXPreviewHost", "Failed to apply zoomRatio=$zoomRatio", e)
        }
    }
}
