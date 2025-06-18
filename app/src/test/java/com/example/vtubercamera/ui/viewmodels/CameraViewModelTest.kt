package com.example.vtubercamera.ui.viewmodels

import android.content.ContentResolver
import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.ZoomState
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class CameraViewModelTest {

    @get:org.junit.Rule
    val instantExecutorRule = androidx.arch.core.executor.testing.InstantTaskExecutorRule()

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

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        cameraViewModel = CameraViewModel()
        cameraViewModel.setCamera(mockCamera)
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        closeable.close()
    }

    @org.junit.jupiter.api.Test
    fun getCameraSelector() {
    }

    @org.junit.jupiter.api.Test
    fun getLastCapturedImageUri() {
    }

    @org.junit.jupiter.api.Test
    fun isPreviewMode() {
    }

    @org.junit.jupiter.api.Test
    fun getFlashMode() {
    }

    @org.junit.jupiter.api.Test
    fun getZoomRatio() {
    }

    @org.junit.jupiter.api.Test
    fun getNeedsCameraRebind() {
    }

    @org.junit.jupiter.api.Test
    fun setCamera() {
    }

    @org.junit.jupiter.api.Test
    fun switchCamera() {
    }

    @org.junit.jupiter.api.Test
    fun toggleFlash() {
    }

    @org.junit.jupiter.api.Test
    fun setZoom() {
    }

    @org.junit.jupiter.api.Test
    fun takePhoto() {
    }

    @org.junit.jupiter.api.Test
    fun enterPreviewMode() {
    }

    @org.junit.jupiter.api.Test
    fun exitPreviewMode() {
    }

    @org.junit.jupiter.api.Test
    fun clearLastCapturedImage() {
    }

    @org.junit.jupiter.api.Test
    fun onCameraRebound() {
    }

}