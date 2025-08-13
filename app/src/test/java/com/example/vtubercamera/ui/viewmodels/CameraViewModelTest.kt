package com.example.vtubercamera.ui.viewmodels

import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ZoomState
import androidx.lifecycle.LiveData
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CameraViewModelTest {

    @Mock
    private lateinit var cameraControl: CameraControl

    @Mock
    private lateinit var cameraInfo: CameraInfo

    @Mock
    private lateinit var zoomState: ZoomState

    @Mock
    private lateinit var zoomStateLiveData: LiveData<ZoomState>

    @Mock
    private lateinit var camera: Camera

    private lateinit var viewModel: CameraViewModel

    @Before
    fun setUp() {
        viewModel = CameraViewModel()

        // Mock setup
        Mockito.`when`(zoomState.maxZoomRatio).thenReturn(4.0f)
        Mockito.`when`(zoomStateLiveData.value).thenReturn(zoomState)
        Mockito.`when`(cameraInfo.zoomState).thenReturn(zoomStateLiveData)
        Mockito.`when`(camera.cameraControl).thenReturn(cameraControl)
        Mockito.`when`(camera.cameraInfo).thenReturn(cameraInfo)
    }

    @Test
    fun `toggleFlash should cycle through flash modes correctly`() {
        // Given: Initial state
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)

        // When & Then: First toggle
        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_ON, viewModel.flashMode.value)

        // When & Then: Second toggle
        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_AUTO, viewModel.flashMode.value)

        // When & Then: Third toggle (back to OFF)
        viewModel.toggleFlash()
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
    }

    @Test
    fun `setZoom should clamp to supported range correctly`() {
        // Given: Camera is set
        viewModel.setCamera(camera)

        // When & Then: Zoom below minimum (should clamp to 1.0f)
        viewModel.setZoom(0.5f)
        assertEquals(1.0f, viewModel.zoomRatio.value)
        Mockito.verify(cameraControl).setZoomRatio(1.0f)

        // When & Then: Zoom within range (should set to 2.0f)
        viewModel.setZoom(2.0f)
        assertEquals(2.0f, viewModel.zoomRatio.value)
        Mockito.verify(cameraControl).setZoomRatio(2.0f)

        // When & Then: Zoom above maximum (should clamp to 4.0f)
        viewModel.setZoom(10.0f)
        assertEquals(4.0f, viewModel.zoomRatio.value)
        Mockito.verify(cameraControl).setZoomRatio(4.0f)
    }

    @Test
    fun `setCamera should update camera references`() {
        // When
        viewModel.setCamera(camera)

        // Then: Camera should be set and zoom operations should work
        viewModel.setZoom(2.0f)
        Mockito.verify(cameraControl).setZoomRatio(2.0f)
    }
}

