package com.example.vtubercamera.ui.viewmodels

import android.content.ContentResolver
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ZoomState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class CameraViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockCamera: Camera

    @Mock
    private lateinit var mockImageCapture: ImageCapture

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock
    private lateinit var mockCameraControl: CameraControl

    @Mock
    private lateinit var mockZoomState: ZoomState

    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var closeable: AutoCloseable

    @BeforeEach
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        cameraViewModel = CameraViewModel()
        cameraViewModel.setCamera(mockCamera)
    }

    @AfterEach
    fun tearDown() {
        closeable.close()
    }

    @Test
    fun getCameraSelector() {
    }

    @Test
    fun switchCamera_DEFAULT_BACK_CAMERA_to_DEFAULT_FRONT_CAMERA() {
        whenever(cameraViewModel.cameraSelector.value).thenReturn(CameraSelector.DEFAULT_BACK_CAMERA)
        cameraViewModel.switchCamera()
        Assertions.assertEquals(
            CameraSelector.DEFAULT_FRONT_CAMERA,
            cameraViewModel.cameraSelector.value
        )
    }

    @Test
    fun getLastCapturedImageUri() {
    }

    @Test
    fun isPreviewMode() {
    }

    @Test
    fun getFlashMode() {
    }

    @Test
    fun getZoomRatio() {
    }

    @Test
    fun getNeedsCameraRebind() {
    }

    @Test
    fun setCamera() {
    }

    @Test
    fun switchCamera() {
    }

    @Test
    fun toggleFlash() {
    }

    @Test
    fun setZoom() {
    }

    @Test
    fun takePhoto() {
    }

    @Test
    fun enterPreviewMode() {
    }

    @Test
    fun exitPreviewMode() {
    }

    @Test
    fun clearLastCapturedImage() {
    }

    @Test
    fun onCameraRebound() {
    }

}

class MainDispatcherRule @OptIn(ExperimentalCoroutinesApi::class) constructor(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun starting(description: Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun finished(description: Description?) {
        Dispatchers.resetMain()
    }
}