package com.example.vtubercamera.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class ARPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    
    private companion object {
        private const val TAG = "ARPermissionManager"
        private const val AR_CORE_INSTALL_REQUEST_CODE = 1001
    }
    
    data class PermissionResult(
        val granted: Boolean,
        val deniedPermissions: List<String> = emptyList(),
        val permanentlyDeniedPermissions: List<String> = emptyList(),
        val shouldShowRationale: List<String> = emptyList()
    )
    
    data class ARInstallResult(
        val installed: Boolean,
        val error: String? = null
    )
    
    private var currentActivity: ComponentActivity? = null
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var arCoreInstallLauncher: ActivityResultLauncher<Intent>? = null
    
    private var permissionCallback: ((PermissionResult) -> Unit)? = null
    private var arCoreInstallCallback: ((ARInstallResult) -> Unit)? = null
    
    private val requiredARPermissions = arrayOf(
        Manifest.permission.CAMERA
    )
    
    private val optionalARPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    fun initialize(activity: ComponentActivity) {
        currentActivity = activity
        activity.lifecycle.addObserver(this)
        
        setupPermissionLauncher(activity)
        setupARCoreInstallLauncher(activity)
        
        Log.d(TAG, "AR Permission Manager initialized")
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        currentActivity?.lifecycle?.removeObserver(this)
        currentActivity = null
        permissionLauncher = null
        arCoreInstallLauncher = null
        permissionCallback = null
        arCoreInstallCallback = null
    }
    
    private fun setupPermissionLauncher(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionResult(permissions)
        }
    }
    
    private fun setupARCoreInstallLauncher(activity: ComponentActivity) {
        arCoreInstallLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleARCoreInstallResult(result)
        }
    }
    
    fun checkARPermissions(): PermissionResult {
        val deniedPermissions = mutableListOf<String>()
        val permanentlyDeniedPermissions = mutableListOf<String>()
        val shouldShowRationale = mutableListOf<String>()
        
        requiredARPermissions.forEach { permission ->
            when {
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED -> {
                    deniedPermissions.add(permission)
                    
                    currentActivity?.let { activity ->
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            shouldShowRationale.add(permission)
                        } else if (hasPermissionBeenRequestedBefore(permission)) {
                            permanentlyDeniedPermissions.add(permission)
                        }
                    }
                }
            }
        }
        
        val granted = deniedPermissions.isEmpty()
        
        Log.d(TAG, "Permission check - Granted: $granted, Denied: $deniedPermissions")
        
        return PermissionResult(
            granted = granted,
            deniedPermissions = deniedPermissions,
            permanentlyDeniedPermissions = permanentlyDeniedPermissions,
            shouldShowRationale = shouldShowRationale
        )
    }
    
    fun requestARPermissions(callback: (PermissionResult) -> Unit) {
        val permissionResult = checkARPermissions()
        
        if (permissionResult.granted) {
            callback(permissionResult)
            return
        }
        
        permissionCallback = callback
        
        val permissionsToRequest = requiredARPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.contentToString()}")
            markPermissionsAsRequested(permissionsToRequest)
            permissionLauncher?.launch(permissionsToRequest)
        }
    }
    
    suspend fun requestARPermissionsAsync(): PermissionResult = suspendCancellableCoroutine { continuation ->
        requestARPermissions { result ->
            continuation.resume(result)
        }
    }
    
    fun checkOptionalPermissions(): Map<String, Boolean> {
        return optionalARPermissions.associateWith { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestOptionalPermissions(permissions: Array<String>, callback: (PermissionResult) -> Unit) {
        val validPermissions = permissions.filter { it in optionalARPermissions }.toTypedArray()
        
        if (validPermissions.isEmpty()) {
            callback(PermissionResult(granted = true))
            return
        }
        
        val deniedPermissions = validPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (deniedPermissions.isEmpty()) {
            callback(PermissionResult(granted = true))
            return
        }
        
        permissionCallback = callback
        permissionLauncher?.launch(validPermissions)
    }
    
    fun checkAndInstallARCore(callback: (ARInstallResult) -> Unit) {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            
            when (availability) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    Log.d(TAG, "ARCore is already installed")
                    callback(ARInstallResult(installed = true))
                }
                
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                    Log.d(TAG, "ARCore needs installation/update")
                    requestARCoreInstallation(callback)
                }
                
                ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                    Log.d(TAG, "ARCore availability is being checked...")
                    callback(ARInstallResult(installed = false, error = "ARCore availability check in progress"))
                }
                
                ArCoreApk.Availability.UNKNOWN_ERROR -> {
                    Log.e(TAG, "Unknown error checking ARCore availability")
                    callback(ARInstallResult(installed = false, error = "Unknown error checking ARCore"))
                }
                
                ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                    Log.e(TAG, "ARCore availability check timed out")
                    callback(ARInstallResult(installed = false, error = "ARCore check timed out"))
                }
                
                else -> {
                    Log.e(TAG, "ARCore is not supported on this device")
                    callback(ARInstallResult(installed = false, error = "ARCore not supported"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ARCore availability", e)
            callback(ARInstallResult(installed = false, error = e.message))
        }
    }
    
    suspend fun checkAndInstallARCoreAsync(): ARInstallResult = suspendCancellableCoroutine { continuation ->
        checkAndInstallARCore { result ->
            continuation.resume(result)
        }
    }
    
    private fun requestARCoreInstallation(callback: (ARInstallResult) -> Unit) {
        arCoreInstallCallback = callback
        
        try {
            currentActivity?.let { activity ->
                when (ArCoreApk.getInstance().requestInstall(activity, true)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        Log.d(TAG, "ARCore installation requested")
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        Log.d(TAG, "ARCore is already installed")
                        callback(ARInstallResult(installed = true))
                        arCoreInstallCallback = null
                    }
                }
            } ?: run {
                callback(ARInstallResult(installed = false, error = "Activity not available"))
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Log.w(TAG, "User declined ARCore installation", e)
            callback(ARInstallResult(installed = false, error = "User declined ARCore installation"))
            arCoreInstallCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting ARCore installation", e)
            callback(ARInstallResult(installed = false, error = e.message))
            arCoreInstallCallback = null
        }
    }
    
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filter { !it.value }.keys.toList()
        val permanentlyDeniedPermissions = mutableListOf<String>()
        val shouldShowRationale = mutableListOf<String>()
        
        deniedPermissions.forEach { permission ->
            currentActivity?.let { activity ->
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    permanentlyDeniedPermissions.add(permission)
                } else {
                    shouldShowRationale.add(permission)
                }
            }
        }
        
        val result = PermissionResult(
            granted = deniedPermissions.isEmpty(),
            deniedPermissions = deniedPermissions,
            permanentlyDeniedPermissions = permanentlyDeniedPermissions,
            shouldShowRationale = shouldShowRationale
        )
        
        Log.d(TAG, "Permission result: $result")
        permissionCallback?.invoke(result)
        permissionCallback = null
    }
    
    private fun handleARCoreInstallResult(result: ActivityResult) {
        val installResult = when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "ARCore installation completed successfully")
                ARInstallResult(installed = true)
            }
            Activity.RESULT_CANCELED -> {
                Log.w(TAG, "ARCore installation was cancelled")
                ARInstallResult(installed = false, error = "Installation cancelled")
            }
            else -> {
                Log.e(TAG, "ARCore installation failed with code: ${result.resultCode}")
                ARInstallResult(installed = false, error = "Installation failed")
            }
        }
        
        arCoreInstallCallback?.invoke(installResult)
        arCoreInstallCallback = null
    }
    
    fun openAppSettings() {
        currentActivity?.let { activity ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not open app settings", e)
            }
        }
    }
    
    fun getPermissionExplanation(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> 
                "カメラ権限は、AR機能でリアルタイム映像を表示するために必要です。"
            Manifest.permission.RECORD_AUDIO -> 
                "音声録音権限は、AR動画録画機能で音声を記録するために使用されます。"
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> 
                "位置情報権限は、位置ベースのAR機能やクラウドアンカーで使用されます。"
            else -> "この権限はAR機能の正常な動作に必要です。"
        }
    }
    
    fun getRationaleMessage(permissions: List<String>): String {
        if (permissions.size == 1) {
            return getPermissionExplanation(permissions.first())
        }
        
        val explanations = permissions.map { permission ->
            "・${getPermissionExplanation(permission)}"
        }
        
        return "以下の権限が必要です：\n${explanations.joinToString("\n")}"
    }
    
    private fun hasPermissionBeenRequestedBefore(permission: String): Boolean {
        val prefs = context.getSharedPreferences("ar_permissions", Context.MODE_PRIVATE)
        return prefs.getBoolean("requested_$permission", false)
    }
    
    private fun markPermissionsAsRequested(permissions: Array<String>) {
        val prefs = context.getSharedPreferences("ar_permissions", Context.MODE_PRIVATE)
        prefs.edit().apply {
            permissions.forEach { permission ->
                putBoolean("requested_$permission", true)
            }
            apply()
        }
    }
    
    fun hasAllRequiredPermissions(): Boolean = checkARPermissions().granted
    
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}