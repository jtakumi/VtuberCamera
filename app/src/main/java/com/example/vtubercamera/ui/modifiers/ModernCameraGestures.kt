package com.example.vtubercamera.ui.modifiers

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import kotlin.math.abs

/**
 * Modern camera gestures using Compose's built-in gesture detection with lens switching
 *
 * @param onScale ズーム比率が変更されたときのコールバック
 * @param onDoubleTap ダブルタップ時のコールバック
 * @param onLensSwitch レンズ切り替え時のコールバック
 * @param currentZoom 現在のズーム比率
 * @param minZoom 最小ズーム比率
 * @param maxZoom 最大ズーム比率
 * @param canSwitchLens レンズ切り替えが可能かどうか
 * @param enableHapticFeedback ハプティックフィードバックを有効にするか
 * @param zoomSensitivity ズームの感度 (デフォルト: 1.0f)
 * @param lensSwitchThreshold レンズ切り替えのしきい値 (デフォルト: 2.0f)
 */
@Composable
fun Modifier.modernCameraGestures(
    onScale: (Float) -> Unit,
    onDoubleTap: () -> Unit = {},
    onTap: (Offset) -> Unit = {},
    onLensSwitch: () -> Unit = {},
    currentZoom: Float,
    minZoom: Float = 1.0f,
    maxZoom: Float = 10.0f,
    canSwitchLens: Boolean = false,
    enableHapticFeedback: Boolean = true,
    zoomSensitivity: Float = 1.0f,
    lensSwitchThreshold: Float = 2.0f
): Modifier {
    val view = LocalView.current
    var lastZoom by remember { mutableFloatStateOf(currentZoom) }
    var gestureScale by remember { mutableFloatStateOf(1.0f) }
    var hasTriggeredLensSwitch by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentZoom) { lastZoom = currentZoom }

    return this
        // ピンチズーム検出とレンズ切り替え
        .pointerInput(minZoom, maxZoom, canSwitchLens) {
            detectTransformGestures(
                panZoomLock = false
            ) { _, _, zoom, _ ->
                gestureScale *= zoom
                
                // レンズ切り替え判定（pinch out で wide-angle、pinch in で normal に戻る）
                if (canSwitchLens && !hasTriggeredLensSwitch) {
                    try {
                        when {
                            // Pinch out beyond threshold - switch to wide-angle
                            gestureScale > lensSwitchThreshold -> {
                                onLensSwitch()
                                hasTriggeredLensSwitch = true
                                if (enableHapticFeedback) {
                                    try {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    } catch (_: Exception) {
                                        // Ignore haptic feedback errors
                                    }
                                }
                                gestureScale = 1.0f // Reset gesture scale
                                return@detectTransformGestures
                            }
                            // Pinch in beyond threshold - switch to normal
                            gestureScale < (1.0f / lensSwitchThreshold) -> {
                                onLensSwitch()
                                hasTriggeredLensSwitch = true
                                if (enableHapticFeedback) {
                                    try {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    } catch (_: Exception) {
                                        // Ignore haptic feedback errors
                                    }
                                }
                                gestureScale = 1.0f // Reset gesture scale
                                return@detectTransformGestures
                            }
                        }
                    } catch (_: Exception) {
                        // Reset gesture state on any error to prevent stuck state
                        gestureScale = 1.0f
                        hasTriggeredLensSwitch = false
                    }
                }

                // Normal zoom handling
                val adjustedZoom = 1f + (zoom - 1f) * zoomSensitivity
                val newZoom = (lastZoom * adjustedZoom).coerceIn(minZoom, maxZoom)

                // ハプティックフィードバック（最小/最大ズーム時）
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
        // ジェスチャー終了時のリセット
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset -> 
                    onTap(offset)
                    // Reset gesture state on tap
                    gestureScale = 1.0f
                    hasTriggeredLensSwitch = false
                },
                onDoubleTap = { 
                    onDoubleTap()
                    // Reset gesture state on double tap
                    gestureScale = 1.0f
                    hasTriggeredLensSwitch = false
                }
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

    LaunchedEffect(currentZoom) { lastZoom = currentZoom }

    return this
        // ピンチズーム検出
        .pointerInput(minZoom, maxZoom) {
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