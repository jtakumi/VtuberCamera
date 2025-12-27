package com.example.vtubercamera.utils

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ARDeviceCompatibility {
    
    private const val TAG = "ARDeviceCompatibility"
    
    data class CompatibilityResult(
        val isARCoreSupported: Boolean,
        val isARCoreInstalled: Boolean,
        val needsARCoreUpdate: Boolean,
        val hasRequiredCameras: Boolean,
        val supportedFeatures: Set<ARFeature>,
        val deviceInfo: DeviceInfo,
        val recommendedConfig: ARConfig?
    )
    
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: Int,
        val buildVersionRelease: String,
        val totalRAM: Long,
        val availableRAM: Long,
        val cpuArchitecture: String,
        val hasGyroscope: Boolean,
        val hasAccelerometer: Boolean,
        val hasMagnetometer: Boolean
    )
    
    data class ARConfig(
        val recommendedResolution: String,
        val maxFPS: Int,
        val supportedLightEstimation: List<Config.LightEstimationMode>,
        val supportedPlaneFinding: List<Config.PlaneFindingMode>,
        val recommendedUpdateMode: Config.UpdateMode,
        val supportsEnvironmentalHDR: Boolean,
        val supportsInstantPlacement: Boolean
    )
    
    enum class ARFeature {
        PLANE_DETECTION,
        LIGHT_ESTIMATION,
        ENVIRONMENTAL_HDR,
        INSTANT_PLACEMENT,
        AUGMENTED_FACES,
        AUGMENTED_IMAGES,
        CLOUD_ANCHORS,
        OCCLUSION,
        RECORDING_AND_PLAYBACK,
        PERSISTENT_CLOUD_ANCHORS,
        GEOSPATIAL_API
    }
    
    enum class CompatibilityLevel {
        FULLY_SUPPORTED,
        PARTIALLY_SUPPORTED,
        MINIMAL_SUPPORT,
        NOT_SUPPORTED
    }
    
    suspend fun checkDeviceCompatibility(context: Context): CompatibilityResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Checking AR device compatibility...")
        
        val deviceInfo = getDeviceInfo(context)
        val arCoreAvailability = ArCoreApk.getInstance().checkAvailability(context)
        
        val isARCoreSupported = when (arCoreAvailability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED,
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> true
            else -> false
        }
        
        val isARCoreInstalled = arCoreAvailability == ArCoreApk.Availability.SUPPORTED_INSTALLED
        val needsUpdate = arCoreAvailability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD
        
        val hasRequiredCameras = checkCameraRequirements(context)
        val supportedFeatures = if (isARCoreSupported) {
            checkSupportedFeatures(context)
        } else {
            emptySet()
        }
        
        val recommendedConfig = if (isARCoreSupported) {
            generateRecommendedConfig(context, deviceInfo)
        } else {
            null
        }
        
        CompatibilityResult(
            isARCoreSupported = isARCoreSupported,
            isARCoreInstalled = isARCoreInstalled,
            needsARCoreUpdate = needsUpdate,
            hasRequiredCameras = hasRequiredCameras,
            supportedFeatures = supportedFeatures,
            deviceInfo = deviceInfo,
            recommendedConfig = recommendedConfig
        )
    }
    
    fun getCompatibilityLevel(result: CompatibilityResult): CompatibilityLevel {
        return when {
            !result.isARCoreSupported || !result.hasRequiredCameras -> CompatibilityLevel.NOT_SUPPORTED
            
            result.isARCoreInstalled && 
            result.supportedFeatures.containsAll(listOf(
                ARFeature.PLANE_DETECTION,
                ARFeature.LIGHT_ESTIMATION
            )) && 
            result.deviceInfo.totalRAM >= 3L * 1024 * 1024 * 1024 -> // 3GB以上
                CompatibilityLevel.FULLY_SUPPORTED
            
            result.isARCoreSupported && result.hasRequiredCameras -> 
                CompatibilityLevel.PARTIALLY_SUPPORTED
            
            else -> CompatibilityLevel.MINIMAL_SUPPORT
        }
    }
    
    private suspend fun getDeviceInfo(context: Context): DeviceInfo = withContext(Dispatchers.IO) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        
        DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.SDK_INT,
            buildVersionRelease = Build.VERSION.RELEASE,
            totalRAM = memInfo.totalMem,
            availableRAM = memInfo.availMem,
            cpuArchitecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            hasGyroscope = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE) != null,
            hasAccelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) != null,
            hasMagnetometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD) != null
        )
    }
    
    private fun checkCameraRequirements(context: Context): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            
            // 少なくとも1つのカメラが必要
            if (cameraIds.isEmpty()) {
                Log.w(TAG, "No cameras found on device")
                return false
            }
            
            // 背面カメラの存在確認
            val hasBackCamera = cameraIds.any { cameraId ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    facing == CameraCharacteristics.LENS_FACING_BACK
                } catch (e: CameraAccessException) {
                    Log.w(TAG, "Could not access camera $cameraId", e)
                    false
                }
            }
            
            if (!hasBackCamera) {
                Log.w(TAG, "No back-facing camera found")
                return false
            }
            
            // カメラ権限が必要
            val hasCameraFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            
            Log.d(TAG, "Camera requirements check: hasBackCamera=$hasBackCamera, hasCameraFeature=$hasCameraFeature")
            hasBackCamera && hasCameraFeature
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera requirements", e)
            false
        }
    }
    
    private suspend fun checkSupportedFeatures(context: Context): Set<ARFeature> = withContext(Dispatchers.IO) {
        val supportedFeatures = mutableSetOf<ARFeature>()
        
        try {
            // ARCoreセッションを一時的に作成して機能をテスト
            val session = Session(context, setOf())
            
            // 基本機能は常にサポート
            supportedFeatures.add(ARFeature.PLANE_DETECTION)
            supportedFeatures.add(ARFeature.LIGHT_ESTIMATION)
            
            // 環境HDRのサポート確認
            try {
                val config = Config(session)
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                if (session.isSupported(config)) {
                    supportedFeatures.add(ARFeature.ENVIRONMENTAL_HDR)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not check Environmental HDR support", e)
            }
            
            // インスタント配置のサポート確認
            try {
                val config = Config(session)
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                if (session.isSupported(config)) {
                    supportedFeatures.add(ARFeature.INSTANT_PLACEMENT)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not check Instant Placement support", e)
            }
            
            // その他の高度な機能
            try {
                val config = Config(session)
                
                // Augmented Facesのサポート確認（フロントカメラが必要）
                if (hasFrontCamera(context)) {
                    supportedFeatures.add(ARFeature.AUGMENTED_FACES)
                }
                
                // 追加機能のサポート
                supportedFeatures.add(ARFeature.AUGMENTED_IMAGES)
                supportedFeatures.add(ARFeature.CLOUD_ANCHORS)
                
                // Android API レベルに基づく機能
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    supportedFeatures.add(ARFeature.RECORDING_AND_PLAYBACK)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    supportedFeatures.add(ARFeature.OCCLUSION)
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Could not check advanced AR features", e)
            }
            
            session.close()
            
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.w(TAG, "Device not compatible with ARCore", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking AR features", e)
        }
        
        Log.d(TAG, "Supported AR features: $supportedFeatures")
        supportedFeatures
    }
    
    private fun hasFrontCamera(context: Context): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.cameraIdList.any { cameraId ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    facing == CameraCharacteristics.LENS_FACING_FRONT
                } catch (e: CameraAccessException) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun generateRecommendedConfig(context: Context, deviceInfo: DeviceInfo): ARConfig {
        // デバイス性能に基づく推奨設定
        val isHighEndDevice = deviceInfo.totalRAM >= 6L * 1024 * 1024 * 1024 // 6GB以上
        val isMidRangeDevice = deviceInfo.totalRAM >= 3L * 1024 * 1024 * 1024 // 3GB以上
        
        val recommendedResolution = when {
            isHighEndDevice -> "1920x1080"
            isMidRangeDevice -> "1280x720" 
            else -> "854x480"
        }
        
        val maxFPS = when {
            isHighEndDevice -> 60
            isMidRangeDevice -> 30
            else -> 24
        }
        
        val supportedLightEstimation = mutableListOf<Config.LightEstimationMode>().apply {
            add(Config.LightEstimationMode.AMBIENT_INTENSITY)
            if (isHighEndDevice) {
                add(Config.LightEstimationMode.ENVIRONMENTAL_HDR)
            }
        }
        
        val supportedPlaneFinding = listOf(
            Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL,
            Config.PlaneFindingMode.HORIZONTAL,
            Config.PlaneFindingMode.VERTICAL
        )
        
        return ARConfig(
            recommendedResolution = recommendedResolution,
            maxFPS = maxFPS,
            supportedLightEstimation = supportedLightEstimation,
            supportedPlaneFinding = supportedPlaneFinding,
            recommendedUpdateMode = if (isHighEndDevice) Config.UpdateMode.LATEST_CAMERA_IMAGE else Config.UpdateMode.BLOCKING,
            supportsEnvironmentalHDR = isHighEndDevice,
            supportsInstantPlacement = isMidRangeDevice || isHighEndDevice
        )
    }
    
    fun getUnsupportedDeviceMessage(result: CompatibilityResult): String {
        return when {
            !result.isARCoreSupported -> "このデバイスはARCoreをサポートしていません。"
            !result.isARCoreInstalled -> "ARCoreがインストールされていません。Google Play Storeからインストールしてください。"
            result.needsARCoreUpdate -> "ARCoreのアップデートが必要です。Google Play Storeで更新してください。"
            !result.hasRequiredCameras -> "AR機能に必要なカメラが見つかりません。"
            result.deviceInfo.totalRAM < 1L * 1024 * 1024 * 1024 -> "RAM容量が不足しています。最低1GB必要です。"
            !result.deviceInfo.hasGyroscope -> "ジャイロスコープセンサーが必要です。"
            !result.deviceInfo.hasAccelerometer -> "加速度センサーが必要です。"
            else -> "デバイスでAR機能を使用できません。"
        }
    }
    
    fun getPerformanceOptimizationSuggestions(result: CompatibilityResult): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (result.deviceInfo.availableRAM < result.deviceInfo.totalRAM * 0.3) {
            suggestions.add("他のアプリを終了してメモリを解放してください")
        }
        
        if (result.deviceInfo.totalRAM < 3L * 1024 * 1024 * 1024) {
            suggestions.add("低解像度モードを使用することを推奨します")
        }
        
        if (!result.supportedFeatures.contains(ARFeature.ENVIRONMENTAL_HDR)) {
            suggestions.add("基本的な光推定モードを使用してください")
        }
        
        if (result.deviceInfo.androidVersion < Build.VERSION_CODES.N) {
            suggestions.add("Android 7.0以上にアップグレードすることを推奨します")
        }
        
        return suggestions
    }
}