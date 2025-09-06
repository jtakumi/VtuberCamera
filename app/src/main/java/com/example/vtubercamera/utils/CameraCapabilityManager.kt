package com.example.vtubercamera.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages camera capability detection for multiple lenses
 * Detects available cameras including wide-angle and normal lenses
 */
@Suppress("UnsafeOptInUsageError")
@OptIn(androidx.camera.core.ExperimentalCameraInfo::class, androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
class CameraCapabilityManager(private val context: Context) {

    data class CameraCapability(
        val cameraSelector: CameraSelector,
        val lensType: LensType,
        val focalLength: Float? = null,
        val displayName: String
    )

    enum class LensType {
        NORMAL,
        WIDE_ANGLE,
        TELEPHOTO,
        FRONT
    }

    private var _availableCameras = emptyList<CameraCapability>()
    val availableCameras: List<CameraCapability> get() = _availableCameras

    private var _hasMultipleRearCameras = false
    val hasMultipleRearCameras: Boolean get() = _hasMultipleRearCameras

    private var _normalCamera: CameraCapability? = null
    val normalCamera: CameraCapability? get() = _normalCamera

    private var _wideAngleCamera: CameraCapability? = null
    val wideAngleCamera: CameraCapability? get() = _wideAngleCamera

    /**
     * Detect all available cameras and their capabilities with enhanced compatibility checks
     */
    @OptIn(androidx.camera.core.ExperimentalCameraInfo::class, androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    suspend fun detectCameraCapabilities(): Boolean {
        return withContext(Dispatchers.IO) {
            // Check camera permissions first
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Camera permission not granted, cannot detect camera capabilities")
                return@withContext false
            }

            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                val detectedCameras = mutableListOf<CameraCapability>()

                // Enhanced camera detection with fallback strategies
                var hasWideAngle = false
                var hasNormal = false

                // Strategy 1: Try to detect using Camera2 API for detailed camera characteristics
                try {
                    val cameraManager = context.getSystemService(android.content.Context.CAMERA_SERVICE) 
                        as android.hardware.camera2.CameraManager
                        
                        for (cameraId in cameraManager.cameraIdList) {
                            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                            val lensFacing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                            
                            if (lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                                val focalLengths = characteristics.get(
                                    android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                                )
                                
                                val focalLength = focalLengths?.firstOrNull()
                                
                                // Build CameraSelector for this specific camera using CameraFilter
                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .addCameraFilter { cameraInfos ->
                                        cameraInfos.filter { cameraInfo ->
                                            try {
                                                val camera2Info = Camera2CameraInfo.from(cameraInfo)
                                                camera2Info.cameraId == cameraId
                                            } catch (e: Exception) {
                                                false
                                            }
                                        }
                                    }
                                    .build()

                                // Enhanced lens type detection with multiple criteria
                                val lensType = determineLensType(characteristics, focalLength)
                                
                                if (cameraProvider.hasCamera(selector)) {
                                    val capability = CameraCapability(
                                        cameraSelector = selector,
                                        lensType = lensType,
                                        focalLength = focalLength,
                                        displayName = getLensDisplayName(lensType, focalLength)
                                    )

                                    detectedCameras.add(capability)

                                    when (lensType) {
                                        LensType.NORMAL -> {
                                            hasNormal = true
                                            if (_normalCamera == null) _normalCamera = capability
                                        }
                                        LensType.WIDE_ANGLE -> {
                                            hasWideAngle = true
                                            if (_wideAngleCamera == null) _wideAngleCamera = capability
                                        }
                                        else -> {}
                                    }

                                    Log.d(TAG, "Detected camera (Camera2): ${capability.displayName} (focal length: ${focalLength}mm, ID: $cameraId)")
                                }
                            }
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Camera2 API detection failed, falling back to CameraX detection: ${e.message}")
                }

                // Strategy 2: Fallback to CameraX detection using available cameras if Camera2 failed or insufficient cameras found
                if (!hasWideAngle || !hasNormal) {
                    try {
                        // Get all available cameras that face back
                        val availableCameras = cameraProvider.availableCameraInfos.filter { cameraInfo ->
                            try {
                                @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
                                val camera2Info = Camera2CameraInfo.from(cameraInfo)
                                val characteristics = camera2Info.getCameraCharacteristic(
                                    android.hardware.camera2.CameraCharacteristics.LENS_FACING
                                )
                                characteristics == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
                            } catch (e: Exception) {
                                false
                            }
                        }

                        for ((index, cameraInfo) in availableCameras.withIndex()) {
                            try {
                                @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
                                val camera2Info = Camera2CameraInfo.from(cameraInfo)
                                val cameraId = camera2Info.cameraId
                                
                                // Create specific camera selector using camera ID
                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .addCameraFilter { cameraInfos ->
                                        cameraInfos.filter { cameraInfoFilter ->
                                            try {
                                                val camera2InfoFilter = Camera2CameraInfo.from(cameraInfoFilter)
                                                camera2InfoFilter.cameraId == cameraId
                                            } catch (e: Exception) {
                                                false
                                            }
                                        }
                                    }
                                    .build()

                                // Skip if we already have this camera
                                if (detectedCameras.any { 
                                    try {
                                        val existingCamera2Info = Camera2CameraInfo.from(cameraProvider.getCameraInfo(it.cameraSelector))
                                        existingCamera2Info.cameraId == cameraId
                                    } catch (e: Exception) { false }
                                }) continue
                                
                                var focalLength: Float? = null
                                var lensType = if (index == 0) LensType.NORMAL else LensType.WIDE_ANGLE
                                
                                try {
                                    @OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
                                    val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)
                                    val characteristics = camera2CameraInfo.getCameraCharacteristic(
                                        android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                                    )
                                    
                                    val focalLengths = characteristics as? FloatArray
                                    focalLength = focalLengths?.firstOrNull()
                                    
                                    if (focalLength != null) {
                                        lensType = determineLensTypeFromFocalLength(focalLength)
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Could not get camera characteristics for fallback detection: ${e.message}")
                                }

                                val capability = CameraCapability(
                                    cameraSelector = selector,
                                    lensType = lensType,
                                    focalLength = focalLength,
                                    displayName = getLensDisplayName(lensType, focalLength)
                                )

                                detectedCameras.add(capability)

                                when (lensType) {
                                    LensType.NORMAL -> {
                                        hasNormal = true
                                        if (_normalCamera == null) _normalCamera = capability
                                    }
                                    LensType.WIDE_ANGLE -> {
                                        hasWideAngle = true
                                        if (_wideAngleCamera == null) _wideAngleCamera = capability
                                    }
                                    else -> {}
                                }

                                Log.d(TAG, "Detected camera (CameraX fallback): ${capability.displayName} (focal length: ${focalLength}mm, ID: $cameraId)")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to analyze camera in fallback detection: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Fallback camera detection failed: ${e.message}")
                    }
                }

                // Add front camera if available
                try {
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        val frontCapability = CameraCapability(
                            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
                            lensType = LensType.FRONT,
                            displayName = "Front"
                        )
                        detectedCameras.add(frontCapability)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to detect front camera: ${e.message}")
                }

                // Final validation and assignment
                _availableCameras = detectedCameras
                _hasMultipleRearCameras = hasWideAngle && hasNormal

                Log.d(TAG, "Camera detection completed. Found ${detectedCameras.size} cameras")
                Log.d(TAG, "Multiple rear cameras available: $_hasMultipleRearCameras")
                Log.d(TAG, "Normal camera available: ${_normalCamera != null}")
                Log.d(TAG, "Wide-angle camera available: ${_wideAngleCamera != null}")

                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to detect camera capabilities", e)
                // Provide minimal fallback - at least one camera should be available
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        val fallbackCapability = CameraCapability(
                            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
                            lensType = LensType.NORMAL,
                            displayName = "Camera"
                        )
                        _availableCameras = listOf(fallbackCapability)
                        _normalCamera = fallbackCapability
                        _hasMultipleRearCameras = false
                        Log.d(TAG, "Using fallback single camera configuration")
                        return@withContext true
                    }
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Even fallback camera detection failed", fallbackException)
                }
                false
            }
        }
    }

    /**
     * Enhanced lens type determination using multiple camera characteristics
     */
    @Suppress("DEPRECATION")
    private fun determineLensType(
        characteristics: android.hardware.camera2.CameraCharacteristics, 
        focalLength: Float?
    ): LensType {
        return try {
            // Method 1: Check lens intrinsic calibration for field of view indicators
            val lensPoseRotation = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_POSE_ROTATION)
            val lensIntrinsicCalibration = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                
                // Wide-angle cameras often have different intrinsic calibration parameters
            if (lensIntrinsicCalibration != null && lensIntrinsicCalibration.size >= 2) {
                val fx = lensIntrinsicCalibration[0] // Focal length in pixels (x-direction)
                val fy = lensIntrinsicCalibration[1] // Focal length in pixels (y-direction)
                
                // Lower focal length in pixels often indicates wide-angle lens
                if (fx < 1000f || fy < 1000f) {
                    return LensType.WIDE_ANGLE
                }
            }
            
            // Method 2: Use physical focal length if available
            if (focalLength != null) {
                return determineLensTypeFromFocalLength(focalLength)
            }

            // Method 3: Additional checks for wide-angle detection can be added here in the future
            // For now, we rely on focal length and intrinsic calibration

            // Default to normal lens type
            LensType.NORMAL
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine lens type from characteristics, defaulting to NORMAL: ${e.message}")
            LensType.NORMAL
        }
    }

    /**
     * Determine lens type based on focal length with enhanced thresholds
     */
    private fun determineLensTypeFromFocalLength(focalLength: Float): LensType {
        return when {
            // Wide-angle: typically < 3.5mm on mobile devices
            focalLength < 3.5f -> LensType.WIDE_ANGLE
            // Normal: typically 3.5-7mm on mobile devices  
            focalLength < 7.0f -> LensType.NORMAL
            // Telephoto: > 7mm
            else -> LensType.TELEPHOTO
        }
    }

    /**
     * Get display name with enhanced information
     */
    private fun getLensDisplayName(lensType: LensType, focalLength: Float?): String {
        val baseName = when (lensType) {
            LensType.WIDE_ANGLE -> "Wide"
            LensType.NORMAL -> "1×"
            LensType.TELEPHOTO -> "Tele"
            LensType.FRONT -> "Front"
        }
        
        return if (focalLength != null && lensType != LensType.FRONT) {
            "$baseName (${String.format(java.util.Locale.US, "%.1f", focalLength)}mm)"
        } else {
            baseName
        }
    }

    /**
     * Get camera selector for switching between normal and wide-angle
     */
    fun getAlternateRearCamera(currentSelector: CameraSelector): CameraSelector? {
        if (!_hasMultipleRearCameras) return null

        return when {
            currentSelector == _normalCamera?.cameraSelector -> _wideAngleCamera?.cameraSelector
            currentSelector == _wideAngleCamera?.cameraSelector -> _normalCamera?.cameraSelector
            else -> _wideAngleCamera?.cameraSelector ?: _normalCamera?.cameraSelector
        }
    }

    /**
     * Get current lens type based on camera selector
     */
    fun getLensType(cameraSelector: CameraSelector): LensType {
        return _availableCameras.find { it.cameraSelector == cameraSelector }?.lensType 
            ?: LensType.NORMAL
    }

    /**
     * Get display name for current lens
     */
    fun getLensDisplayName(cameraSelector: CameraSelector): String {
        return _availableCameras.find { it.cameraSelector == cameraSelector }?.displayName 
            ?: "Camera"
    }

    /**
     * Check if lens switching is supported
     */
    fun canSwitchLens(): Boolean = _hasMultipleRearCameras

    companion object {
        private const val TAG = "CameraCapabilityManager"
    }
}