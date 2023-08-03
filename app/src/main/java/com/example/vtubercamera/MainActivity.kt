package com.example.vtubercamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.example.vtubercamera.databinding.ActivityMainBinding

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraView: TextureView
    private var cameraDevice: CameraDevice? = null
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private var switchCameraValue = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val toolbar = binding.toolbarMain
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        cameraView = binding.cameraTextureView
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
        binding.switchCamera.setOnClickListener {
            changeSPCamera()
        }
    }

    private fun changeSPCamera() {
        switchCameraValue = when (switchCameraValue) {
            0 -> 1
            1 -> 0
            else -> {
                return
            }
        }
        openCamera()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                backOpeningScreen()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun backOpeningScreen() {
        val intent = Intent(this, OpeningActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
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
        val backCameraId = getBackCameraId()
        val frontCameraId = getFrontCameraId()
        if (backCameraId.isNullOrEmpty() || frontCameraId.isNullOrEmpty()) {
            // Handle case where back camera is not available
            return
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        var openCameraId = ""
        when (switchCameraValue) {
            0 -> openCameraId = backCameraId
            1 -> openCameraId = frontCameraId
        }
        cameraDevice?.close()

        cameraManager.openCamera(openCameraId, object : CameraDevice.StateCallback() {
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

    private fun getFrontCameraId(): String? {
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId
            }
        }
        return null
    }

    private fun getBackCameraId(): String? {
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId
            }
        }
        return null
    }

    private fun createCameraPreview() {
        cameraDevice?.let {
            val texture = cameraView.surfaceTexture
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(it.id)
            val streamConfigurationMap =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val defaultPreviewSize = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)?.get(0)
            texture?.setDefaultBufferSize(640, 480)
            val surface = Surface(texture)
            val previewRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
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

    private fun allPermissionsGranted() =
        requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                // Handle the case when camera permission is denied by the user.
                // You might display a message or close the app gracefully.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}

