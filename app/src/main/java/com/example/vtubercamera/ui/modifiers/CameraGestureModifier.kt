package com.example.vtubercamera.ui.modifiers

import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * カメラのピンチズームとダブルタップ機能を提供するModifier
 * 
 * @param onScale ズーム比率が変更されたときのコールバック
 * @param onDoubleTap ダブルタップ時のコールバック
 * @param currentZoom 現在のズーム比率
 * @param minZoom 最小ズーム比率
 * @param maxZoom 最大ズーム比率
 * @param enableHapticFeedback ハプティックフィードバックを有効にするか
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.cameraGestures(
    onScale: (Float) -> Unit,
    onDoubleTap: () -> Unit = {},
    currentZoom: Float,
    minZoom: Float = 1.0f,
    maxZoom: Float = 10.0f,
    enableHapticFeedback: Boolean = true
): Modifier {
    val context = LocalContext.current
    val view = LocalView.current
    
    // ScaleGestureDetector for pinch-to-zoom
    val scaleGestureDetector = remember {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    val newZoom = (currentZoom * scaleFactor).coerceIn(minZoom, maxZoom)
                    
                    // ハプティックフィードバック（最小/最大ズーム時）
                    if (enableHapticFeedback) {
                        if ((newZoom == minZoom || newZoom == maxZoom) && 
                            (currentZoom != minZoom && currentZoom != maxZoom)) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                    }
                    
                    onScale(newZoom)
                    return true
                }
            }
        )
    }
    
    // GestureDetector for double-tap
    val gestureDetector = remember {
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onDoubleTap()
                    return true
                }
            }
        )
    }
    
    return this.pointerInteropFilter { motionEvent ->
        var handled = scaleGestureDetector.onTouchEvent(motionEvent)
        
        // ScaleGestureDetectorが処理していない場合のみGestureDetectorで処理
        if (!scaleGestureDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(motionEvent) || handled
        }
        
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> true
            else -> handled
        }
    }
}

/**
 * より簡単なピンチズームのみのModifier（後方互換性のため）
 */
//@Composable
//fun Modifier.pinchToZoom(
//    onScale: (Float) -> Unit,
//    currentZoom: Float,
//    minZoom: Float = 1.0f,
//    maxZoom: Float = 10.0f
//): Modifier = cameraGestures(
//    onScale = onScale,
//    onDoubleTap = {},
//    currentZoom = currentZoom,
//    minZoom = minZoom,
//    maxZoom = maxZoom,
//    enableHapticFeedback = false
//)
