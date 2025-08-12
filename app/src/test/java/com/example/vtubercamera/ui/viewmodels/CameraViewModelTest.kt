package com.example.vtubercamera.ui.viewmodels

import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ZoomState
import androidx.lifecycle.LiveData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class CameraViewModelTest {

    @Test
    fun toggleFlash_cyclesThroughModes() {
        val viewModel = CameraViewModel()

        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)

        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_ON, viewModel.flashMode.value)

        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_AUTO, viewModel.flashMode.value)

        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
    }

    @Test
    fun setZoom_clampsToSupportedRange() {
        val viewModel = CameraViewModel()

        val cameraControl = Mockito.mock(CameraControl::class.java)
        val cameraInfo = Mockito.mock(CameraInfo::class.java)
        val zoomState = Mockito.mock(ZoomState::class.java)
        Mockito.`when`(zoomState.maxZoomRatio).thenReturn(4.0f)
        @Suppress("UNCHECKED_CAST")
        val zoomStateLiveData = Mockito.mock(LiveData::class.java) as LiveData<ZoomState>
        Mockito.`when`(zoomStateLiveData.value).thenReturn(zoomState)
        Mockito.`when`(cameraInfo.zoomState).thenReturn(zoomStateLiveData)

        val camera = Mockito.mock(Camera::class.java)
        Mockito.`when`(camera.cameraControl).thenReturn(cameraControl)
        Mockito.`when`(camera.cameraInfo).thenReturn(cameraInfo)

        viewModel.setCamera(camera)

        viewModel.setZoom(0.5f)
        assertEquals(1.0f, viewModel.zoomRatio.value)
        Mockito.verify(cameraControl).setZoomRatio(1.0f)

        viewModel.setZoom(2.0f)
        assertEquals(2.0f, viewModel.zoomRatio.value)
        Mockito.verify(cameraControl).setZoomRatio(2.0f)

        viewModel.setZoom(10.0f)
        assertEquals(4.0f, viewModel.zoomRatio.value)
        Mockito.verify(cameraControl).setZoomRatio(4.0f)
    }
}

