package com.example.vtubercamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.example.vtubercamera.databinding.ActivityMainBinding

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraView: TextureView
    private var cameraDevice: CameraDevice? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        cameraView = binding.cameraTextureView
    }

    override fun onResume() {
        super.onResume()
        if (cameraView.isAvailable) {
            openCamera()
        } else {
            cameraView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}
                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {}
                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean = true

            }
        }
    }

    private fun openCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        cameraManager.openCamera("0", object : CameraDevice.StateCallback() {
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0
                createCameraPreview()
            }

            override fun onDisconnected(p0: CameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)

    }

    private fun createCameraPreview() {
        cameraDevice?.let {
            val texture = cameraView.surfaceTexture
            texture?.setDefaultBufferSize(640, 480)
            val surface = Surface(texture)
            val previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)
            it.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(p0: CameraCaptureSession) {
                    p0.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {
                }
            }, null)
        }
    }

}
