package com.example.vtubercamera.ui.ar

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vtubercamera.ui.viewmodels.CameraViewModel

/**
 * Hosts a TextureView that provides a Surface for AR rendering.
 * - Creates/destroys the Surface based on TextureView lifecycle
 * - Notifies ViewModel to initialize/cleanup AR renderer
 * - Drives a simple frame loop tied to display frames
 */
@Composable
fun ARRenderOverlay(
    isARMode: Boolean,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    var surface by remember { mutableStateOf<Surface?>(null) }
    var viewSize by remember { mutableStateOf(0 to 0) }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        surface = Surface(st)
                        viewSize = width to height
                        viewModel.onRenderSurfaceAvailable(surface!!, width, height)
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                        viewSize = width to height
                        viewModel.onRenderSurfaceSizeChanged(width, height)
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        surface?.release()
                        surface = null
                        viewModel.onRenderSurfaceDestroyed()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier,
        update = { /* no-op */ }
    )

    // Drive a simple per-frame update loop when AR is active and Surface is ready
    LaunchedEffect(isARMode, surface) {
        if (!isARMode) return@LaunchedEffect
        val surf = surface ?: return@LaunchedEffect

        var lastTimeNanos: Long? = null
        while (true) {
            // Sync to display frame cadence using Compose frame clock
            val now = androidx.compose.runtime.withFrameNanos { it }
            val dt = lastTimeNanos?.let { (now - it) / 1_000_000_000f } ?: 0f
            lastTimeNanos = now

            val session = viewModel.getARSession() ?: continue
            try {
                val frame = session.update()
                viewModel.onARFrame(frame, dt)
            } catch (_: Throwable) {
                // Ignore update errors in early/paused states
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Surface cleanup handled in listener
        }
    }
}
