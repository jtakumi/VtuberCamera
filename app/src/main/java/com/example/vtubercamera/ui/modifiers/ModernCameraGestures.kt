package com.example.vtubercamera.ui.modifiers

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import kotlin.math.abs
import kotlin.math.min

/**
 * Modern camera gestures using Compose's built-in gesture detection
 *
 * @param onScale ズーム比率が変更されたときのコールバック
 * @param onDoubleTap ダブルタップ時のコールバック
 * @param currentZoom 現在のズーム比率
 * @param minZoom 最小ズーム比率
 * @param maxZoom 最大ズーム比率
 * @param enableHapticFeedback ハプティックフィードバックを有効にするか
 * @param zoomSensitivity ズームの感度 (デフォルト: 1.0f)
 */
@Composable
fun Modifier.modernCameraGestures(
    onScale: (Float) -> Unit,
    onDoubleTap: () -> Unit = {},
    onTap: (Offset) -> Unit = {},
    currentZoom: Float,
    minZoom: Float = 1.0f,
    maxZoom: Float = 10.0f,
    enableHapticFeedback: Boolean = true,
    zoomSensitivity: Float = 1.0f
): Modifier {
    val view = LocalView.current

    return this
        // ピンチズーム検出
        .pointerInput(currentZoom, minZoom, maxZoom) {
            detectTransformGestures(
                panZoomLock = false
            ) { _, _, zoom, _ ->
                // ズーム感度を適用
                val adjustedZoom = 1f + (zoom - 1f) * zoomSensitivity
                val newZoom = (currentZoom * adjustedZoom).coerceIn(minZoom, maxZoom)

                // ハプティックフィードバック（最小/最大ズーム時）
                if (enableHapticFeedback) {
                    val wasAtLimit = currentZoom == minZoom || currentZoom == maxZoom
                    val isAtLimit = newZoom == minZoom || newZoom == maxZoom

                    if (isAtLimit && !wasAtLimit) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }
                onScale(newZoom)
            }
        }
        // ダブルタップ検出
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset -> onTap(offset) },
                onDoubleTap = { onDoubleTap() }
            )
        }
}

/**
 * より詳細な制御が可能なカメラジェスチャー
 * フォーカスタップやスワイプジェスチャーも含む
 */
@Composable
fun Modifier.advancedCameraGestures(
    onScale: (Float) -> Unit,
    onDoubleTap: () -> Unit = {},
    onTap: (Offset) -> Unit = {},
    onFocusTap: (Offset) -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    currentZoom: Float,
    minZoom: Float = 1.0f,
    maxZoom: Float = 10.0f,
    enableHapticFeedback: Boolean = true,
    zoomSensitivity: Float = 1.0f,
    swipeThreshold: Float = 100f
): Modifier {
    val view = LocalView.current
    var lastZoom by remember { mutableFloatStateOf(currentZoom) }

    return this
        // ピンチズーム検出
        .pointerInput(currentZoom, minZoom, maxZoom) {
            detectTransformGestures(
                panZoomLock = false
            ) { _, _, zoom, _ ->
                val adjustedZoom = 1f + (zoom - 1f) * zoomSensitivity
                val newZoom = (lastZoom * adjustedZoom).coerceIn(minZoom, maxZoom)

                if (enableHapticFeedback) {
                    val wasAtLimit = lastZoom == minZoom || lastZoom == maxZoom
                    val isAtLimit = newZoom == minZoom || newZoom == maxZoom

                    if (isAtLimit && !wasAtLimit) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                }

                lastZoom = newZoom
                onScale(newZoom)
            }
        }
        // タップとダブルタップ検出
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    onTap(offset)
                    onFocusTap(offset) // フォーカス用
                },
                onDoubleTap = { onDoubleTap() }
            )
        }
        // スワイプ検出
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    // ドラッグ終了時の処理
                }
            ) { _, dragAmount ->
                // 縦方向のスワイプ検出
                if (abs(dragAmount.y) > abs(dragAmount.x)) {
                    if (dragAmount.y < -swipeThreshold) {
                        onSwipeUp()
                    } else if (dragAmount.y > swipeThreshold) {
                        onSwipeDown()
                    }
                }
            }
        }
}