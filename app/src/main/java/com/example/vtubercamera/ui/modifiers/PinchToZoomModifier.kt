package com.example.vtubercamera.ui.modifiers

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext

/**
 * カメラのピンチズーム機能を提供するModifier
 * 
 * @param onScale ズーム比率が変更されたときのコールバック
 * @param currentZoom 現在のズーム比率
 * @param minZoom 最小ズーム比率
 * @param maxZoom 最大ズーム比率
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.pinchToZoom(
    onScale: (Float) -> Unit,
    currentZoom: Float,
    minZoom: Float = 1.0f,
    maxZoom: Float = 10.0f
): Modifier {
    val context = LocalContext.current
    
    val scaleGestureDetector = remember {
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    val newZoom = (currentZoom * scaleFactor).coerceIn(minZoom, maxZoom)
                    onScale(newZoom)
                    return true
                }
            }
        )
    }
    
    return this.pointerInteropFilter { motionEvent ->
        scaleGestureDetector.onTouchEvent(motionEvent)
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> true
            else -> false
        }
    }
}
