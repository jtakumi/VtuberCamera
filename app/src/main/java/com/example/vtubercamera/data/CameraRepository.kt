package com.example.vtubercamera.data

import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner

interface CameraRepository {
    suspend fun getCameraProvider(): ProcessCameraProvider
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        flashMode: Int,
        onImageCaptureCreated: (ImageCapture) -> Unit,
        onCameraCreated: (Camera) -> Unit
    ): Camera?
    
    suspend fun capturePhoto(
        imageCapture: ImageCapture,
        onPhotoSaved: (Uri) -> Unit,
        onError: (String) -> Unit
    )
}