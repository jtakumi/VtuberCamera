package com.example.vtubercamera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.icu.text.SimpleDateFormat
import android.media.ImageReader
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.vtubercamera.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.Date
import java.util.Locale
import kotlin.io.path.Path

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraView: TextureView
    private lateinit var shutter: ImageView
    private var cameraDevice: CameraDevice? = null
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private var switchCameraValue = 0
    private var cameraIsOpen = false

    //when start app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val toolbar = binding.toolbarMain
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        cameraView = binding.cameraTextureView
        shutter = binding.cameraButton
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

        shutter.setOnClickListener {
            saveImage()
            playSound()
        }
    }

    private fun saveImage() {
        cameraDevice?.let {
            val saveRequestBuilder = it.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            val texture = cameraView.surfaceTexture ?: return
            val viewSize = Point(cameraView.width, cameraView.height)
            texture.setDefaultBufferSize(viewSize.x, viewSize.y)
            val surface = Surface(texture)
            val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG${timeStamp}.jpg"
            //missing taken photo but the path could get
            //need to modifying path can view taken photos on android phone
            val imageFile =  Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val filePath = imageFile.absolutePath + fileName
            val imageReader = ImageReader.newInstance(viewSize.x, viewSize.y, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val outputFile = File(filePath)
                //save the captured image to the file
                FileOutputStream(outputFile).use { output ->
                    output.write(bytes)
                }
                image.close()
            }, null)
            saveRequestBuilder.addTarget(surface)
            saveRequestBuilder.addTarget(imageReader.surface)
            //need converting to getCameraCharacteristics
            val rotation = windowManager.defaultDisplay.rotation
            saveRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(rotation))
            it.createCaptureSession(
                listOf(surface, imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(saveRequestBuilder.build(), null, null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                }, null
            )
        }
    }


    private fun playSound() {
        //mp3 shutter sound
        val mMediaPlayer = MediaPlayer.create(this, R.raw.camera_shutter)
        mMediaPlayer.apply {
            isLooping = false
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        //if camera is close
        if (cameraIsOpen.not()) {
            if (cameraView.isAvailable) {
                openCamera()
            } else {
                cameraView.surfaceTextureListener = surfaceTextureListener
            }
        }
    }

    //this function run when move to other app or home screen.
    override fun onPause() {
        super.onPause()
        closeCamera()
    }

    private fun changeSPCamera() {
        //0 is back camera
        //1 is front camera
        switchCameraValue = when (switchCameraValue) {
            0 -> 1
            1 -> 0
            else -> {
                return
            }
        }
        openCamera()
    }

    //back button's function in toolbar.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                backOpeningScreen()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //go back to openingActivity
    private fun backOpeningScreen() {
        val intent = Intent(this, OpeningActivity::class.java)
        startActivity(intent)
        finish()
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
            //select use camera
            0 -> openCameraId = backCameraId
            1 -> openCameraId = frontCameraId
        }
        cameraDevice?.close()

        cameraManager.openCamera(openCameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(CameraDevice: CameraDevice) {
                cameraDevice = CameraDevice
                createCameraPreview()
            }

            //disconnect or error happen, close camera
            override fun onDisconnected(CameraDevice: CameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(CameraDevice: CameraDevice, p1: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)
        //isOpen flag turn into true
        cameraIsOpen = true
    }

    private fun startCamera() {
        if (cameraView.isAvailable) {
            openCamera()
        } else {
            cameraView.surfaceTextureListener = surfaceTextureListener
        }
    }

    private fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null
        cameraIsOpen = false
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
        }
    }


    //getFrontCameraId and getBackCameraId function get own camera's id.
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
            val texture =
                cameraView.surfaceTexture ?: throw NullPointerException("texture has not found.")
            val viewSize = Point(cameraView.width, cameraView.height)
            texture.setDefaultBufferSize(viewSize.x, viewSize.y)
            val surface = Surface(texture)
            //need converting to getCameraCharacteristics
            val rotation = windowManager.defaultDisplay.rotation
            val previewRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)
            previewRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(rotation))
            it.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(p0: CameraCaptureSession) {
                    p0.setRepeatingRequest(previewRequestBuilder.build(), null, null)
                }

                override fun onConfigureFailed(p0: CameraCaptureSession) {
                }
            }, null)
        }
    }

    private fun getJpegOrientation(rotation: Int): Int {
        val sensorOrientation = getCameraSensorOrientation()
        val isFrontFacing = switchCameraValue == 1

        return if (isFrontFacing) {
            (sensorOrientation + rotation) % 360
        } else {
            (sensorOrientation - rotation + 360) % 360
        }
    }

    private fun getCameraSensorOrientation(): Int {
        val cameraId = when (switchCameraValue) {
            0 -> getBackCameraId()
            1 -> getFrontCameraId()
            else -> null
        }
        cameraId?.let {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        }
        return 0
    }


    //verify permissions function
    //all permission are true, the function will return value
    private fun allPermissionsGranted() =
        requiredPermissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    //request permission for system
    //show popup menu
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }
}
