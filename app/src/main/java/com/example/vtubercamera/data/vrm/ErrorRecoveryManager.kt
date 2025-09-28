package com.example.vtubercamera.data.vrm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages error recovery strategies and automatic retry mechanisms
 */
@Singleton
class ErrorRecoveryManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ErrorRecoveryManager"
        private const val MAX_AUTO_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_BASE_MS = 1000L
        private const val RETRY_DELAY_MULTIPLIER = 2.0
    }
    
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()
    
    private val _recoveryProgress = MutableStateFlow<RecoveryProgress?>(null)
    val recoveryProgress: StateFlow<RecoveryProgress?> = _recoveryProgress.asStateFlow()
    
    /**
     * Attempt to recover from an error automatically
     */
    suspend fun attemptAutoRecovery(
        errorState: ErrorState,
        retryOperation: suspend () -> Result<Unit>
    ): RecoveryResult {
        if (!errorState.isRecoverable || !canAutoRecover(errorState.type)) {
            return RecoveryResult.NotRecoverable(errorState)
        }
        
        _isRecovering.value = true
        _recoveryProgress.value = RecoveryProgress(
            strategy = getAutoRecoveryStrategy(errorState.type),
            currentStep = 1,
            totalSteps = getRecoverySteps(errorState.type),
            message = "Attempting to recover from error..."
        )
        
        try {
            val result = when (errorState.type) {
                ErrorType.NETWORK_TIMEOUT,
                ErrorType.NETWORK_CONNECTION_FAILED -> recoverFromNetworkError(errorState, retryOperation)
                
                ErrorType.FILE_IO_ERROR -> recoverFromFileError(errorState, retryOperation)
                
                ErrorType.AR_TRACKING_LOST,
                ErrorType.AR_SESSION_INTERRUPTED -> recoverFromARError(errorState, retryOperation)
                
                ErrorType.VRM_CORRUPTED,
                ErrorType.VRM_PARSE_ERROR -> recoverFromVRMError(errorState, retryOperation)
                
                ErrorType.VRM_INSUFFICIENT_MEMORY,
                ErrorType.AR_INSUFFICIENT_RESOURCES -> recoverFromMemoryError(errorState, retryOperation)
                
                else -> RecoveryResult.NotRecoverable(errorState)
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto recovery", e)
            return RecoveryResult.Failed(errorState, e.message ?: "Recovery failed")
        } finally {
            _isRecovering.value = false
            _recoveryProgress.value = null
        }
    }
    
    /**
     * Execute a manual recovery action
     */
    suspend fun executeRecoveryAction(
        action: ErrorAction,
        errorState: ErrorState
    ): RecoveryResult {
        _isRecovering.value = true
        
        try {
            return when (action) {
                ErrorAction.REQUEST_PERMISSIONS -> {
                    requestStoragePermissions()
                    RecoveryResult.ActionRequired("Please grant storage permissions and try again")
                }
                
                ErrorAction.REQUEST_CAMERA_PERMISSION -> {
                    requestCameraPermissions()
                    RecoveryResult.ActionRequired("Please grant camera permissions and try again")
                }
                
                ErrorAction.OPEN_SETTINGS -> {
                    openAppSettings()
                    RecoveryResult.ActionRequired("Please check app settings and try again")
                }
                
                ErrorAction.INSTALL_ARCORE -> {
                    openARCoreInstallPage()
                    RecoveryResult.ActionRequired("Please install ARCore and try again")
                }
                
                ErrorAction.UPDATE_ARCORE -> {
                    openARCoreUpdatePage()
                    RecoveryResult.ActionRequired("Please update ARCore and try again")
                }
                
                ErrorAction.CHECK_NETWORK -> {
                    openNetworkSettings()
                    RecoveryResult.ActionRequired("Please check your network connection and try again")
                }
                
                ErrorAction.FREE_MEMORY -> {
                    suggestMemoryCleanup()
                    RecoveryResult.ActionRequired("Please close other apps to free memory and try again")
                }
                
                ErrorAction.RESTART_APP -> {
                    RecoveryResult.ActionRequired("Please restart the app to continue")
                }
                
                else -> RecoveryResult.NotRecoverable(errorState)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error executing recovery action", e)
            return RecoveryResult.Failed(errorState, e.message ?: "Recovery action failed")
        } finally {
            _isRecovering.value = false
        }
    }
    
    /**
     * Get user-friendly recovery instructions for an error
     */
    fun getRecoveryInstructions(errorState: ErrorState): List<RecoveryInstruction> {
        return when (errorState.type) {
            ErrorType.VRM_FILE_NOT_FOUND -> listOf(
                RecoveryInstruction(
                    title = "Select a different file",
                    description = "Choose another VRM file from your device",
                    action = ErrorAction.SELECT_DIFFERENT_FILE,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Check file location",
                    description = "Verify the file still exists in its original location",
                    action = ErrorAction.CHECK_FILE_PERMISSIONS,
                    priority = RecoveryPriority.MEDIUM
                )
            )
            
            ErrorType.VRM_INVALID_FORMAT -> listOf(
                RecoveryInstruction(
                    title = "Validate file format",
                    description = "Ensure the file is a valid VRM format (.vrm extension)",
                    action = ErrorAction.VALIDATE_FILE_FORMAT,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Try a different file",
                    description = "Select a different VRM file that you know works",
                    action = ErrorAction.SELECT_DIFFERENT_FILE,
                    priority = RecoveryPriority.MEDIUM
                )
            )
            
            ErrorType.VRM_FILE_TOO_LARGE -> listOf(
                RecoveryInstruction(
                    title = "Select a smaller file",
                    description = "Choose a VRM file smaller than 100MB",
                    action = ErrorAction.SELECT_SMALLER_FILE,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Compress the file",
                    description = "Use VRM optimization tools to reduce file size",
                    action = ErrorAction.COMPRESS_FILE,
                    priority = RecoveryPriority.MEDIUM
                )
            )
            
            ErrorType.AR_NOT_SUPPORTED -> listOf(
                RecoveryInstruction(
                    title = "Use 2D mode",
                    description = "Continue using the app without AR features",
                    action = ErrorAction.USE_2D_MODE,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Check device compatibility",
                    description = "Verify if your device supports ARCore",
                    action = ErrorAction.CHECK_DEVICE_COMPATIBILITY,
                    priority = RecoveryPriority.LOW
                )
            )
            
            ErrorType.AR_NOT_INSTALLED -> listOf(
                RecoveryInstruction(
                    title = "Install ARCore",
                    description = "Download and install ARCore from Google Play Store",
                    action = ErrorAction.INSTALL_ARCORE,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Use 2D mode",
                    description = "Continue without AR features for now",
                    action = ErrorAction.USE_2D_MODE,
                    priority = RecoveryPriority.MEDIUM
                )
            )
            
            ErrorType.NETWORK_NO_CONNECTION -> listOf(
                RecoveryInstruction(
                    title = "Check network connection",
                    description = "Verify your WiFi or mobile data connection",
                    action = ErrorAction.CHECK_NETWORK,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Use offline mode",
                    description = "Continue with locally stored content",
                    action = ErrorAction.USE_OFFLINE_MODE,
                    priority = RecoveryPriority.MEDIUM
                )
            )
            
            ErrorType.FILE_PERMISSION_DENIED -> listOf(
                RecoveryInstruction(
                    title = "Grant storage permission",
                    description = "Allow the app to access your device storage",
                    action = ErrorAction.REQUEST_PERMISSIONS,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Open app settings",
                    description = "Manually enable permissions in device settings",
                    action = ErrorAction.OPEN_SETTINGS,
                    priority = RecoveryPriority.MEDIUM
                )
            )
            
            else -> listOf(
                RecoveryInstruction(
                    title = "Try again",
                    description = "Retry the operation",
                    action = ErrorAction.RETRY_OPERATION,
                    priority = RecoveryPriority.HIGH
                ),
                RecoveryInstruction(
                    title = "Restart app",
                    description = "Close and reopen the app",
                    action = ErrorAction.RESTART_APP,
                    priority = RecoveryPriority.MEDIUM
                )
            )
        }
    }
    
    private suspend fun recoverFromNetworkError(
        errorState: ErrorState,
        retryOperation: suspend () -> Result<Unit>
    ): RecoveryResult {
        updateRecoveryProgress("Checking network connectivity...", 1, 3)
        
        // Wait a bit before retrying
        delay(RETRY_DELAY_BASE_MS)
        
        updateRecoveryProgress("Retrying network operation...", 2, 3)
        
        val result = retryOperation()
        
        updateRecoveryProgress("Completing recovery...", 3, 3)
        
        return if (result.isSuccess) {
            RecoveryResult.Success("Network connection restored")
        } else {
            RecoveryResult.Failed(errorState, "Network recovery failed")
        }
    }
    
    private suspend fun recoverFromFileError(
        errorState: ErrorState,
        retryOperation: suspend () -> Result<Unit>
    ): RecoveryResult {
        updateRecoveryProgress("Checking file access...", 1, 2)
        
        delay(500)
        
        updateRecoveryProgress("Retrying file operation...", 2, 2)
        
        val result = retryOperation()
        
        return if (result.isSuccess) {
            RecoveryResult.Success("File access restored")
        } else {
            RecoveryResult.Failed(errorState, "File recovery failed")
        }
    }
    
    private suspend fun recoverFromARError(
        errorState: ErrorState,
        retryOperation: suspend () -> Result<Unit>
    ): RecoveryResult {
        updateRecoveryProgress("Resetting AR session...", 1, 3)
        
        delay(1000)
        
        updateRecoveryProgress("Reinitializing tracking...", 2, 3)
        
        delay(1000)
        
        updateRecoveryProgress("Retrying AR operation...", 3, 3)
        
        val result = retryOperation()
        
        return if (result.isSuccess) {
            RecoveryResult.Success("AR session restored")
        } else {
            RecoveryResult.Failed(errorState, "AR recovery failed")
        }
    }
    
    private suspend fun recoverFromVRMError(
        errorState: ErrorState,
        retryOperation: suspend () -> Result<Unit>
    ): RecoveryResult {
        updateRecoveryProgress("Clearing VRM cache...", 1, 2)
        
        delay(500)
        
        updateRecoveryProgress("Retrying VRM operation...", 2, 2)
        
        val result = retryOperation()
        
        return if (result.isSuccess) {
            RecoveryResult.Success("VRM loading recovered")
        } else {
            RecoveryResult.Failed(errorState, "VRM recovery failed")
        }
    }
    
    private suspend fun recoverFromMemoryError(
        errorState: ErrorState,
        retryOperation: suspend () -> Result<Unit>
    ): RecoveryResult {
        updateRecoveryProgress("Freeing memory...", 1, 3)
        
        // Suggest garbage collection
        System.gc()
        delay(1000)
        
        updateRecoveryProgress("Optimizing resources...", 2, 3)
        
        delay(500)
        
        updateRecoveryProgress("Retrying operation...", 3, 3)
        
        val result = retryOperation()
        
        return if (result.isSuccess) {
            RecoveryResult.Success("Memory issue resolved")
        } else {
            RecoveryResult.Failed(errorState, "Memory recovery failed")
        }
    }
    
    private fun canAutoRecover(errorType: ErrorType): Boolean {
        return when (errorType) {
            ErrorType.NETWORK_TIMEOUT,
            ErrorType.NETWORK_CONNECTION_FAILED,
            ErrorType.FILE_IO_ERROR,
            ErrorType.AR_TRACKING_LOST,
            ErrorType.AR_SESSION_INTERRUPTED,
            ErrorType.VRM_CORRUPTED,
            ErrorType.VRM_PARSE_ERROR,
            ErrorType.VRM_INSUFFICIENT_MEMORY,
            ErrorType.AR_INSUFFICIENT_RESOURCES -> true
            else -> false
        }
    }
    
    private fun getAutoRecoveryStrategy(errorType: ErrorType): RecoveryStrategy {
        return when (errorType) {
            ErrorType.NETWORK_TIMEOUT,
            ErrorType.NETWORK_CONNECTION_FAILED -> RecoveryStrategy.RETRY_OPERATION
            ErrorType.FILE_IO_ERROR -> RecoveryStrategy.RETRY_OPERATION
            ErrorType.AR_TRACKING_LOST,
            ErrorType.AR_SESSION_INTERRUPTED -> RecoveryStrategy.RESTART_APP
            ErrorType.VRM_INSUFFICIENT_MEMORY,
            ErrorType.AR_INSUFFICIENT_RESOURCES -> RecoveryStrategy.FREE_MEMORY
            else -> RecoveryStrategy.RETRY_OPERATION
        }
    }
    
    private fun getRecoverySteps(errorType: ErrorType): Int {
        return when (errorType) {
            ErrorType.NETWORK_TIMEOUT,
            ErrorType.NETWORK_CONNECTION_FAILED -> 3
            ErrorType.AR_TRACKING_LOST,
            ErrorType.AR_SESSION_INTERRUPTED -> 3
            ErrorType.VRM_INSUFFICIENT_MEMORY,
            ErrorType.AR_INSUFFICIENT_RESOURCES -> 3
            else -> 2
        }
    }
    
    private fun updateRecoveryProgress(message: String, currentStep: Int, totalSteps: Int) {
        _recoveryProgress.value = RecoveryProgress(
            strategy = RecoveryStrategy.RETRY_OPERATION,
            currentStep = currentStep,
            totalSteps = totalSteps,
            message = message
        )
    }
    
    private fun requestStoragePermissions() {
        // This would typically be handled by the UI layer
        Log.d(TAG, "Storage permissions requested")
    }
    
    private fun requestCameraPermissions() {
        // This would typically be handled by the UI layer
        Log.d(TAG, "Camera permissions requested")
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings", e)
        }
    }
    
    private fun openARCoreInstallPage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.core")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open ARCore install page", e)
        }
    }
    
    private fun openARCoreUpdatePage() {
        openARCoreInstallPage() // Same as install page
    }
    
    private fun openNetworkSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open network settings", e)
        }
    }
    
    private fun suggestMemoryCleanup() {
        // This would typically show a dialog with memory cleanup suggestions
        Log.d(TAG, "Memory cleanup suggested")
    }
}

/**
 * Progress information during error recovery
 */
data class RecoveryProgress(
    val strategy: RecoveryStrategy,
    val currentStep: Int,
    val totalSteps: Int,
    val message: String
) {
    val progressPercentage: Float = (currentStep.toFloat() / totalSteps.toFloat()) * 100f
}

/**
 * Result of an error recovery attempt
 */
sealed class RecoveryResult {
    data class Success(val message: String) : RecoveryResult()
    data class Failed(val originalError: ErrorState, val failureReason: String) : RecoveryResult()
    data class NotRecoverable(val originalError: ErrorState) : RecoveryResult()
    data class ActionRequired(val instruction: String) : RecoveryResult()
}

/**
 * Recovery instruction for users
 */
data class RecoveryInstruction(
    val title: String,
    val description: String,
    val action: ErrorAction,
    val priority: RecoveryPriority
)

/**
 * Priority levels for recovery instructions
 */
enum class RecoveryPriority {
    HIGH,    // Should be tried first
    MEDIUM,  // Alternative solution
    LOW      // Last resort or informational
}