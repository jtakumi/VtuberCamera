package com.example.vtubercamera.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import com.example.vtubercamera.data.CameraRepository
import com.example.vtubercamera.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockCameraRepository: CameraRepository

    @Mock
    private lateinit var mockMediaRepository: MediaRepository
    

    private lateinit var viewModel: CameraViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup mock returns
        whenever(mockMediaRepository.getAllPhotos()).thenReturn(flowOf(emptyList()))

        viewModel = CameraViewModel(
            cameraRepository = mockCameraRepository,
            mediaRepository = mockMediaRepository,
        )
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

    @Test
    fun `refreshPhotos should call mediaRepository refreshPhotos`() = runTest {
        // When - Refresh photos is called
        viewModel.refreshPhotos()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - MediaRepository refresh should be called
        verify(mockMediaRepository).refreshPhotos()
    }
}
