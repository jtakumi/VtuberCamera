package com.example.vtubercamera.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageCapture
import androidx.camera.core.ZoomState
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CameraViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock setup - より詳細な階層設定
        `when`(zoomState.maxZoomRatio).thenReturn(4.0f)
        `when`(zoomState.minZoomRatio).thenReturn(1.0f)
        `when`(zoomStateLiveData.value).thenReturn(zoomState)
        `when`(cameraInfo.zoomState).thenReturn(zoomStateLiveData)
        `when`(camera.cameraControl).thenReturn(cameraControl)
        `when`(camera.cameraInfo).thenReturn(cameraInfo)

        viewModel = CameraViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleFlash should cycle through flash modes correctly`() = runTest {
        // Given: Initial state
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)

        // When & Then: First toggle
        viewModel.toggleFlash()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImageCapture.FLASH_MODE_ON, viewModel.flashMode.value)

        // When & Then: Second toggle
        viewModel.toggleFlash()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImageCapture.FLASH_MODE_AUTO, viewModel.flashMode.value)

        // When & Then: Third toggle (back to OFF)
        viewModel.toggleFlash()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ImageCapture.FLASH_MODE_OFF, viewModel.flashMode.value)
    }

    @Test
    fun `setZoom should clamp to supported range correctly`() = runTest {
        // Given: Camera is set
        viewModel.setCamera(camera)

        // When & Then: Zoom below minimum (should clamp to 1.0f)
        viewModel.setZoom(0.5f)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1.0f, viewModel.zoomRatio.value)
        verify(cameraControl).setZoomRatio(1.0f)

        // When & Then: Zoom within range (should set to 2.0f)
        viewModel.setZoom(2.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2.0f, viewModel.zoomRatio.value)
        verify(cameraControl).setZoomRatio(2.0f)

        // When & Then: Zoom above maximum (should clamp to 4.0f)
        viewModel.setZoom(10.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(4.0f, viewModel.zoomRatio.value)
        verify(cameraControl).setZoomRatio(4.0f)
    }

    @Test
    fun `setCamera should update camera references`() = runTest {
        // When
        viewModel.setCamera(camera)

        // Then: Camera should be set and zoom operations should work
        viewModel.setZoom(2.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(cameraControl).setZoomRatio(2.0f)
    }

    @Test
    fun `switchCamera should toggle between front and back camera`() = runTest {
        // Given: Initial state (back camera)
        assertEquals(
            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            viewModel.cameraSelector.value
        )

        // When: Switch camera
        viewModel.switchCamera()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should switch to front camera
        assertEquals(
            androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA,
            viewModel.cameraSelector.value
        )

        // When: Switch camera again
        viewModel.switchCamera()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Should switch back to back camera
        assertEquals(
            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            viewModel.cameraSelector.value
        )
    }

    @Test
    fun `resetZoom should set zoom to 1_0f`() = runTest {
        // Given: Camera is set and zoom is changed
        viewModel.setCamera(camera)
        viewModel.setZoom(3.0f)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3.0f, viewModel.zoomRatio.value)

        // When: Reset zoom
        viewModel.resetZoom()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Zoom should be reset to 1.0f
        assertEquals(1.0f, viewModel.zoomRatio.value)
    }

    @Test
    fun `enterPreviewMode should set isPreviewMode to true`() = runTest {
        // Given: Initial state
        assertEquals(false, viewModel.isPreviewMode.value)

        // When: Enter preview mode
        viewModel.enterPreviewMode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Preview mode should be true
        assertEquals(true, viewModel.isPreviewMode.value)
    }

    @Test
    fun `exitPreviewMode should set isPreviewMode to false and trigger camera rebind`() = runTest {
        // Given: Preview mode is enabled
        viewModel.enterPreviewMode()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.isPreviewMode.value)

        // When: Exit preview mode
        viewModel.exitPreviewMode()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Preview mode should be false and camera rebind should be needed
        assertEquals(false, viewModel.isPreviewMode.value)
        assertEquals(true, viewModel.needsCameraRebind.value)
    }
}