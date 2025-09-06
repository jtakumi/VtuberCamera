package com.example.vtubercamera.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraRepository {

    override suspend fun getCameraProvider(): ProcessCameraProvider {
        return withContext(Dispatchers.Main) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.get()
        }
    }

    override fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        cameraSelector: CameraSelector,
        flashMode: Int,
        onImageCaptureCreated: (ImageCapture) -> Unit,
        onCameraCreated: (Camera) -> Unit
    ): Camera? {
        return try {
            Log.d(
                "CameraRepository",
                "Binding camera with flash mode: $flashMode, camera: ${if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) "BACK" else "FRONT"}"
            )

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(flashMode)
                .build()

            val preview = Preview.Builder().build()

            cameraProvider.unbindAll()

            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            onImageCaptureCreated(imageCapture)
            onCameraCreated(camera)

            Log.d("CameraRepository", "Camera bound successfully")
            camera
        } catch (e: Exception) {
            Log.e("CameraRepository", "Failed to bind camera", e)
            null
        }
    }

    override suspend fun capturePhoto(
        imageCapture: ImageCapture,
        onPhotoSaved: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VTuberCamera")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "写真を保存しました: ${output.savedUri}"
                    Log.d("CameraRepository", msg)
                    output.savedUri?.let { onPhotoSaved(it) }
                }

                override fun onError(exc: ImageCaptureException) {
                    val msg = "写真の保存に失敗しました"
                    Log.e("CameraRepository", msg, exc)
                    onError(msg)
                }
            }
        )
    }

    override fun switchToCamera(
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        newCameraSelector: CameraSelector,
        flashMode: Int,
        onImageCaptureCreated: (ImageCapture) -> Unit,
        onCameraCreated: (Camera) -> Unit
    ): Camera? {
        return try {
            Log.d("CameraRepository", "Switching to new camera lens")
            
            // Unbind current camera
            cameraProvider.unbindAll()
            
            // Bind to new camera with same logic as bindCamera
            bindCamera(
                lifecycleOwner,
                cameraProvider,
                newCameraSelector,
                flashMode,
                onImageCaptureCreated,
                onCameraCreated
            )
        } catch (e: Exception) {
            Log.e("CameraRepository", "Failed to switch camera", e)
            null
        }
    }
}