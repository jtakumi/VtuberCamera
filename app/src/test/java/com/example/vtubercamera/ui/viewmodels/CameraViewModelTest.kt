package com.example.vtubercamera.ui.viewmodels

import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class CameraViewModelTest {

    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        viewModel = CameraViewModel()
    }

    @Test
    fun `viewModel should be initialized properly`() {
        assertNotNull(viewModel)
    }

    @Test
    fun `initial flash mode should be OFF`() {
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
    }

    @Test
    fun `initial zoom ratio should be 1_0f`() {
        assertEquals(1.0f, viewModel.zoomRatio.value, 0.001f)
    }

    @Test
    fun `initial preview mode should be false`() {
        assertEquals(false, viewModel.isPreviewMode.value)
    }

    @Test
    fun `initial camera selector should be back camera`() {
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)
    }

    @Test
    fun `toggleFlash should cycle through flash modes correctly`() {
        // 初期状態: OFF
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
        
        // 1回目のトグル: OFF -> ON
        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_ON, viewModel.flashMode.value)
        
        // 2回目のトグル: ON -> AUTO
        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_AUTO, viewModel.flashMode.value)
        
        // 3回目のトグル: AUTO -> OFF
        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
    }

    @Test
    fun `switchCamera should toggle between front and back camera`() {
        // 初期状態: BACK
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)
        
        // 1回目の切り替え: BACK -> FRONT
        viewModel.switchCamera()
        assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, viewModel.cameraSelector.value)
        
        // 2回目の切り替え: FRONT -> BACK
        viewModel.switchCamera()
        assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.cameraSelector.value)
    }

    @Test
    fun `enterPreviewMode should set preview mode to true`() {
        // 初期状態
        assertEquals(false, viewModel.isPreviewMode.value)
        
        // プレビューモード開始
        viewModel.enterPreviewMode()
        assertEquals(true, viewModel.isPreviewMode.value)
    }

    @Test
    fun `exitPreviewMode should set preview mode to false and trigger camera rebind`() {
        // プレビューモード開始
        viewModel.enterPreviewMode()
        assertEquals(true, viewModel.isPreviewMode.value)
        
        // プレビューモード終了
        viewModel.exitPreviewMode()
        assertEquals(false, viewModel.isPreviewMode.value)
        assertEquals(true, viewModel.needsCameraRebind.value)
    }

    @Test
    fun `onCameraRebound should reset camera rebind flag`() {
        // カメラ再バインドが必要な状態にする
        viewModel.exitPreviewMode()
        assertEquals(true, viewModel.needsCameraRebind.value)
        
        // カメラ再バインド完了
        viewModel.onCameraRebound()
        assertEquals(false, viewModel.needsCameraRebind.value)
    }
}
