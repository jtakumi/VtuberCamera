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
import android.icu.text.SimpleDateFormat
import android.media.ImageReader
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.MenuItem
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.vtubercamera.databinding.ActivityMainBinding
import com.example.vtubercamera.extentions.playSound
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale

class MainActivity<T : Any?> : AppCompatActivity(), View.OnClickListener {
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraView: TextureView
    private lateinit var shutter: ImageView
    private lateinit var settingIcon: ImageView
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler
    private var switchCameraValue = 0
    private var cameraIsOpen = false
    private var cameraDevice: CameraDevice? = null
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private val cameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surefaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(
            surefaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surefaceTexture: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surefaceTexure: SurfaceTexture) {
        }
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
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    //when start app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val toolbar = binding.toolbarMain
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        cameraView = binding.cameraTextureView
        shutter = binding.cameraButton
        settingIcon = binding.settingIcon
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //起動時に権限の確認を行い、付与されなければリクエストを送る
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
        binding.switchCamera.setOnClickListener(this)
        shutter.setOnClickListener(this)
        settingIcon.setOnClickListener(this)

    }

    override fun onClick(view: View) {
        when (view) {
            binding.switchCamera -> {
                changeSPCamera()
            }

            binding.cameraButton -> {
                saveImage()
                playSound(MediaPlayer.create(this, R.raw.camera_shutter))
            }

            binding.settingIcon -> {
                moveActivities(SettingActivity::class.java)
            }
        }
    }

    //back button's function in toolbar.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                moveActivities(OpeningActivity::class.java)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun moveActivities(calledActivity: Class<*>) {
        val intent = Intent(this, calledActivity)
        startActivity(intent)
        finish()
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

    //getFrontCameraId and getBackCameraId function get own camera's id.
    private fun getCameraId(wannaId: Int): String? {
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            when (wannaId) {
                0 -> {
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        return cameraId
                    }
                }

                1 -> {
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        return cameraId
                    }
                }
            }
        }
        return null
    }

    private fun openCamera() {
        val backCameraId = getCameraId(0)
        val frontCameraId = getCameraId(1)
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
        }, handler)
        //isOpen flag turn into true
        cameraIsOpen = true
    }

    private fun createCameraPreview() {
        cameraDevice?.let {
            val texture =
                cameraView.surfaceTexture ?: throw NullPointerException("texture has not found.")
            val viewSize = Point(
                getString(R.string.image_view_width).toInt(),
                getString(R.string.image_view_height).toInt()
            )
            texture.setDefaultBufferSize(viewSize.x, viewSize.y)
            val surface = Surface(texture)
            //need converting to getCameraCharacteristics
            val previewRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)
            it.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    cameraCaptureSession.setRepeatingRequest(
                        previewRequestBuilder.build(),
                        null,
                        null
                    )
                }

                override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                }
            }, null)
        }
    }

    private fun saveImage() {
        cameraDevice?.let {
            val saveRequestBuilder = it.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            val texture = cameraView.surfaceTexture ?: return
            val viewSize = Point(
                getString(R.string.image_view_width).toInt(),
                getString(R.string.image_view_height).toInt()
            )
            texture.setDefaultBufferSize(viewSize.x, viewSize.y)
            val surface = Surface(texture)
            val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN).format(Date())
            //missing taken photo but the path could get
            //need to modifying path can view taken photos on android phone
            val imageReader = ImageReader.newInstance(viewSize.x, viewSize.y, ImageFormat.JPEG, 1)
            saveRequestBuilder.addTarget(surface)
            saveRequestBuilder.addTarget(imageReader.surface)
            val imageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            imageDir?.mkdir()
            val imageFile = File(imageDir, "IMG$timeStamp.jpg")
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val fileStream = FileOutputStream(imageFile)
                //save the captured image to the file
                fileStream.write(bytes)
                fileStream.close()
                image.close()
                Toast.makeText(this, "image captured", Toast.LENGTH_SHORT).show()
            }, handler)
            //need converting to getCameraCharacteristics
            it.createCaptureSession(
                listOf(surface, imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(saveRequestBuilder.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }
                }, handler
            )
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
            0 -> getCameraId(0)
            1 -> getCameraId(1)
            else -> null
        }
        cameraId?.apply {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        }
        return 0
    }
}
