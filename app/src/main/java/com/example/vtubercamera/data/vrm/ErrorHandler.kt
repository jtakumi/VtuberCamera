package com.example.vtubercamera.data.vrm

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling system for VRM AR Avatar functionality
 * Handles VRM loading errors, ARCore errors, network errors, and file access errors
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ErrorHandler"
    }
    
    private val _currentError = MutableStateFlow<ErrorState?>(null)
    val currentError: StateFlow<ErrorState?> = _currentError.asStateFlow()
    
    private val _errorHistory = MutableStateFlow<List<ErrorRecord>>(emptyList())
    val errorHistory: StateFlow<List<ErrorRecord>> = _errorHistory.asStateFlow()
    
    /**
     * Handle VRM loading errors with appropriate recovery strategies
     */
    fun handleVRMError(error: VRMLoadingError, context: ErrorContext): ErrorState {
        Log.e(TAG, "VRM Error: ${error.message}", error)
        
        val errorState = when (error) {
            is VRMLoadingError.FileNotFound -> ErrorState(
                type = ErrorType.VRM_FILE_NOT_FOUND,
                message = "VRM file not found",
                userMessage = "The selected VRM file could not be found. Please check if the file still exists.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = false,
                suggestedActions = listOf(
                    ErrorAction.SELECT_DIFFERENT_FILE,
                    ErrorAction.CHECK_FILE_PERMISSIONS
                ),
                context = context
            )
            
            is VRMLoadingError.InvalidFormat -> ErrorState(
                type = ErrorType.VRM_INVALID_FORMAT,
                message = "Invalid VRM file format",
                userMessage = "The selected file is not a valid VRM format. Please select a proper VRM file.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = false,
                suggestedActions = listOf(
                    ErrorAction.SELECT_DIFFERENT_FILE,
                    ErrorAction.VALIDATE_FILE_FORMAT
                ),
                context = context
            )
            
            is VRMLoadingError.FileSizeExceeded -> ErrorState(
                type = ErrorType.VRM_FILE_TOO_LARGE,
                message = "VRM file size exceeds limit",
                userMessage = "The VRM file is too large. Please select a smaller file (max 100MB).",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = false,
                suggestedActions = listOf(
                    ErrorAction.SELECT_SMALLER_FILE,
                    ErrorAction.COMPRESS_FILE
                ),
                context = context
            )
            
            is VRMLoadingError.CorruptedData -> ErrorState(
                type = ErrorType.VRM_CORRUPTED,
                message = "VRM file data is corrupted",
                userMessage = "The VRM file appears to be corrupted. Please try a different file.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.SELECT_DIFFERENT_FILE,
                    ErrorAction.REDOWNLOAD_FILE
                ),
                context = context
            )
            
            is VRMLoadingError.UnsupportedVersion -> ErrorState(
                type = ErrorType.VRM_UNSUPPORTED_VERSION,
                message = "Unsupported VRM version",
                userMessage = "This VRM file version is not supported. Please use VRM 1.0 or 0.0 format.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = false,
                suggestedActions = listOf(
                    ErrorAction.CONVERT_FILE_VERSION,
                    ErrorAction.SELECT_DIFFERENT_FILE
                ),
                context = context
            )
            
            is VRMLoadingError.InsufficientMemory -> ErrorState(
                type = ErrorType.VRM_INSUFFICIENT_MEMORY,
                message = "Insufficient memory to load VRM",
                userMessage = "Not enough memory to load this VRM file. Try closing other apps or select a smaller file.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.FREE_MEMORY,
                    ErrorAction.RESTART_APP,
                    ErrorAction.SELECT_SMALLER_FILE
                ),
                context = context
            )
            
            is VRMLoadingError.PermissionDenied -> ErrorState(
                type = ErrorType.VRM_PERMISSION_DENIED,
                message = "Permission denied to access VRM file",
                userMessage = "Permission denied to access the file. Please grant storage permission.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.REQUEST_PERMISSIONS,
                    ErrorAction.SELECT_DIFFERENT_FILE
                ),
                context = context
            )
            
            is VRMLoadingError.NetworkError -> ErrorState(
                type = ErrorType.NETWORK_ERROR,
                message = "Network error loading VRM",
                userMessage = "Network error occurred while loading the VRM file. Check your internet connection.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.CHECK_NETWORK,
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.USE_OFFLINE_MODE
                ),
                context = context
            )
            
            is VRMLoadingError.IOError -> ErrorState(
                type = ErrorType.FILE_IO_ERROR,
                message = "File I/O error: ${error.details}",
                userMessage = "File access error occurred. Please try again or select a different file.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.CHECK_FILE_PERMISSIONS,
                    ErrorAction.SELECT_DIFFERENT_FILE
                ),
                context = context
            )
            
            is VRMLoadingError.ParseError -> ErrorState(
                type = ErrorType.VRM_PARSE_ERROR,
                message = "VRM parsing error: ${error.details}",
                userMessage = "Error parsing the VRM file. The file may be corrupted or incompatible.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.SELECT_DIFFERENT_FILE,
                    ErrorAction.VALIDATE_FILE_FORMAT
                ),
                context = context
            )
        }
        
        recordError(errorState)
        _currentError.value = errorState
        return errorState
    }
    
    /**
     * Handle ARCore related errors with appropriate recovery strategies
     */
    fun handleARError(error: ARError, context: ErrorContext): ErrorState {
        Log.e(TAG, "AR Error: ${error.message}", error)
        
        val errorState = when (error) {
            is ARError.ARCoreNotSupported -> ErrorState(
                type = ErrorType.AR_NOT_SUPPORTED,
                message = "ARCore not supported",
                userMessage = "AR features are not supported on this device. You can still use the app in 2D mode.",
                severity = ErrorSeverity.LOW,
                isRecoverable = false,
                suggestedActions = listOf(
                    ErrorAction.USE_2D_MODE,
                    ErrorAction.CHECK_DEVICE_COMPATIBILITY
                ),
                context = context
            )
            
            is ARError.ARCoreNotInstalled -> ErrorState(
                type = ErrorType.AR_NOT_INSTALLED,
                message = "ARCore not installed",
                userMessage = "ARCore is required for AR features. Please install ARCore from Google Play Store.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.INSTALL_ARCORE,
                    ErrorAction.USE_2D_MODE
                ),
                context = context
            )
            
            is ARError.ARCoreOutdated -> ErrorState(
                type = ErrorType.AR_OUTDATED,
                message = "ARCore is outdated",
                userMessage = "ARCore needs to be updated for optimal performance. Please update from Google Play Store.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.UPDATE_ARCORE,
                    ErrorAction.USE_2D_MODE
                ),
                context = context
            )
            
            is ARError.CameraPermissionDenied -> ErrorState(
                type = ErrorType.AR_CAMERA_PERMISSION,
                message = "Camera permission denied",
                userMessage = "Camera permission is required for AR features. Please grant camera permission.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.REQUEST_CAMERA_PERMISSION,
                    ErrorAction.OPEN_SETTINGS
                ),
                context = context
            )
            
            is ARError.SessionInitializationFailed -> ErrorState(
                type = ErrorType.AR_SESSION_FAILED,
                message = "AR session initialization failed",
                userMessage = "Failed to start AR session. Please try again or restart the app.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.RESTART_APP,
                    ErrorAction.USE_2D_MODE
                ),
                context = context
            )
            
            is ARError.TrackingLost -> ErrorState(
                type = ErrorType.AR_TRACKING_LOST,
                message = "AR tracking lost",
                userMessage = "AR tracking lost. Move the device slowly and ensure good lighting.",
                severity = ErrorSeverity.LOW,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.IMPROVE_LIGHTING,
                    ErrorAction.MOVE_DEVICE_SLOWLY,
                    ErrorAction.RETRY_OPERATION
                ),
                context = context
            )
            
            is ARError.InsufficientResources -> ErrorState(
                type = ErrorType.AR_INSUFFICIENT_RESOURCES,
                message = "Insufficient resources for AR",
                userMessage = "Device doesn't have enough resources for AR. Close other apps or use 2D mode.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.FREE_MEMORY,
                    ErrorAction.CLOSE_OTHER_APPS,
                    ErrorAction.USE_2D_MODE
                ),
                context = context
            )
            
            is ARError.CameraInUse -> ErrorState(
                type = ErrorType.AR_CAMERA_IN_USE,
                message = "Camera is in use by another app",
                userMessage = "Camera is being used by another app. Please close other camera apps.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.CLOSE_OTHER_APPS,
                    ErrorAction.RETRY_OPERATION
                ),
                context = context
            )
            
            is ARError.SessionInterrupted -> ErrorState(
                type = ErrorType.AR_SESSION_INTERRUPTED,
                message = "AR session interrupted",
                userMessage = "AR session was interrupted. Please try again.",
                severity = ErrorSeverity.LOW,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.RESTART_AR_SESSION
                ),
                context = context
            )
            
            is ARError.RenderingError -> ErrorState(
                type = ErrorType.AR_RENDERING_ERROR,
                message = "AR rendering error: ${error.details}",
                userMessage = "Error occurred during AR rendering. Please try again.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.REDUCE_QUALITY,
                    ErrorAction.RESTART_APP
                ),
                context = context
            )
            
            is ARError.ConfigurationError -> ErrorState(
                type = ErrorType.AR_CONFIGURATION_ERROR,
                message = "AR configuration error: ${error.details}",
                userMessage = "AR configuration error occurred. Please try again.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.RESET_AR_SETTINGS
                ),
                context = context
            )
            
            is ARError.UnsupportedDevice -> ErrorState(
                type = ErrorType.AR_UNSUPPORTED_DEVICE,
                message = "Unsupported device: ${error.details}",
                userMessage = "This device doesn't support all AR features. Some functionality may be limited.",
                severity = ErrorSeverity.LOW,
                isRecoverable = false,
                suggestedActions = listOf(
                    ErrorAction.USE_2D_MODE,
                    ErrorAction.CHECK_DEVICE_COMPATIBILITY
                ),
                context = context
            )
            
            is ARError.SessionError -> ErrorState(
                type = ErrorType.AR_SESSION_ERROR,
                message = "AR session error: ${error.details}",
                userMessage = "AR session error occurred. Please try again.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.RESTART_AR_SESSION
                ),
                context = context
            )
            
            is ARError.TrackingError -> ErrorState(
                type = ErrorType.AR_TRACKING_ERROR,
                message = "AR tracking error: ${error.details}",
                userMessage = "AR tracking error occurred. Ensure good lighting and move slowly.",
                severity = ErrorSeverity.LOW,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.IMPROVE_LIGHTING,
                    ErrorAction.MOVE_DEVICE_SLOWLY,
                    ErrorAction.RETRY_OPERATION
                ),
                context = context
            )
            
            is ARError.AvatarError -> ErrorState(
                type = ErrorType.AR_AVATAR_ERROR,
                message = "Avatar error: ${error.details}",
                userMessage = "Error occurred with the avatar. Please try a different avatar.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.SELECT_DIFFERENT_AVATAR,
                    ErrorAction.RETRY_OPERATION
                ),
                context = context
            )
        }
        
        recordError(errorState)
        _currentError.value = errorState
        return errorState
    }
    
    /**
     * Handle network errors with appropriate recovery strategies
     */
    fun handleNetworkError(error: Throwable, context: ErrorContext): ErrorState {
        Log.e(TAG, "Network Error: ${error.message}", error)
        
        val errorState = when (error) {
            is UnknownHostException -> ErrorState(
                type = ErrorType.NETWORK_NO_CONNECTION,
                message = "No internet connection",
                userMessage = "No internet connection available. Please check your network settings.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.CHECK_NETWORK,
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.USE_OFFLINE_MODE
                ),
                context = context
            )
            
            is SocketTimeoutException -> ErrorState(
                type = ErrorType.NETWORK_TIMEOUT,
                message = "Network timeout",
                userMessage = "Network request timed out. Please check your connection and try again.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.CHECK_NETWORK,
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.USE_OFFLINE_MODE
                ),
                context = context
            )
            
            is ConnectException -> ErrorState(
                type = ErrorType.NETWORK_CONNECTION_FAILED,
                message = "Connection failed",
                userMessage = "Failed to connect to server. Please try again later.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.CHECK_NETWORK,
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.TRY_LATER
                ),
                context = context
            )
            
            else -> ErrorState(
                type = ErrorType.NETWORK_UNKNOWN,
                message = "Network error: ${error.message}",
                userMessage = "Network error occurred. Please check your connection and try again.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.CHECK_NETWORK,
                    ErrorAction.RETRY_OPERATION
                ),
                context = context
            )
        }
        
        recordError(errorState)
        _currentError.value = errorState
        return errorState
    }
    
    /**
     * Handle file access errors with appropriate recovery strategies
     */
    fun handleFileAccessError(error: Throwable, context: ErrorContext): ErrorState {
        Log.e(TAG, "File Access Error: ${error.message}", error)
        
        val errorState = when (error) {
            is SecurityException -> ErrorState(
                type = ErrorType.FILE_PERMISSION_DENIED,
                message = "File permission denied",
                userMessage = "Permission denied to access the file. Please grant storage permission.",
                severity = ErrorSeverity.HIGH,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.REQUEST_PERMISSIONS,
                    ErrorAction.OPEN_SETTINGS,
                    ErrorAction.SELECT_DIFFERENT_FILE
                ),
                context = context
            )
            
            is IOException -> ErrorState(
                type = ErrorType.FILE_IO_ERROR,
                message = "File I/O error: ${error.message}",
                userMessage = "File access error occurred. Please try again or select a different file.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.CHECK_FILE_PERMISSIONS,
                    ErrorAction.SELECT_DIFFERENT_FILE
                ),
                context = context
            )
            
            else -> ErrorState(
                type = ErrorType.FILE_UNKNOWN_ERROR,
                message = "File error: ${error.message}",
                userMessage = "File error occurred. Please try again.",
                severity = ErrorSeverity.MEDIUM,
                isRecoverable = true,
                suggestedActions = listOf(
                    ErrorAction.RETRY_OPERATION,
                    ErrorAction.SELECT_DIFFERENT_FILE
                ),
                context = context
            )
        }
        
        recordError(errorState)
        _currentError.value = errorState
        return errorState
    }
    
    /**
     * Clear the current error state
     */
    fun clearError() {
        _currentError.value = null
    }
    
    /**
     * Get error recovery suggestions based on error type
     */
    fun getRecoveryStrategies(errorType: ErrorType): List<RecoveryStrategy> {
        return when (errorType) {
            ErrorType.VRM_FILE_NOT_FOUND -> listOf(
                RecoveryStrategy.SELECT_DIFFERENT_FILE,
                RecoveryStrategy.CHECK_FILE_LOCATION
            )
            ErrorType.VRM_INVALID_FORMAT -> listOf(
                RecoveryStrategy.VALIDATE_FILE_FORMAT,
                RecoveryStrategy.SELECT_DIFFERENT_FILE
            )
            ErrorType.VRM_FILE_TOO_LARGE -> listOf(
                RecoveryStrategy.COMPRESS_FILE,
                RecoveryStrategy.SELECT_SMALLER_FILE
            )
            ErrorType.VRM_INSUFFICIENT_MEMORY -> listOf(
                RecoveryStrategy.FREE_MEMORY,
                RecoveryStrategy.RESTART_APP,
                RecoveryStrategy.REDUCE_QUALITY
            )
            ErrorType.AR_NOT_SUPPORTED -> listOf(
                RecoveryStrategy.USE_2D_MODE,
                RecoveryStrategy.CHECK_DEVICE_COMPATIBILITY
            )
            ErrorType.AR_NOT_INSTALLED -> listOf(
                RecoveryStrategy.INSTALL_ARCORE,
                RecoveryStrategy.USE_2D_MODE
            )
            ErrorType.NETWORK_NO_CONNECTION -> listOf(
                RecoveryStrategy.CHECK_NETWORK,
                RecoveryStrategy.USE_OFFLINE_MODE
            )
            ErrorType.FILE_PERMISSION_DENIED -> listOf(
                RecoveryStrategy.REQUEST_PERMISSIONS,
                RecoveryStrategy.OPEN_SETTINGS
            )
            else -> listOf(
                RecoveryStrategy.RETRY_OPERATION,
                RecoveryStrategy.RESTART_APP
            )
        }
    }
    
    /**
     * Check if an error is recoverable through automatic retry
     */
    fun isAutoRetryable(errorType: ErrorType): Boolean {
        return when (errorType) {
            ErrorType.NETWORK_TIMEOUT,
            ErrorType.NETWORK_CONNECTION_FAILED,
            ErrorType.FILE_IO_ERROR,
            ErrorType.AR_TRACKING_LOST,
            ErrorType.AR_SESSION_INTERRUPTED -> true
            else -> false
        }
    }
    
    /**
     * Record error for analytics and debugging
     */
    private fun recordError(errorState: ErrorState) {
        val errorRecord = ErrorRecord(
            errorState = errorState,
            timestamp = System.currentTimeMillis(),
            deviceInfo = getDeviceInfo(),
            appVersion = getAppVersion()
        )
        
        val currentHistory = _errorHistory.value.toMutableList()
        currentHistory.add(0, errorRecord) // Add to beginning
        
        // Keep only last 50 errors
        if (currentHistory.size > 50) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        _errorHistory.value = currentHistory
    }
    
    private fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                "${packageInfo.versionName} (${packageInfo.longVersionCode})"
            } else {
                "(${packageInfo.versionName})"
            }
        } catch (_: Exception) {
            "Unknown"
        }
    }
}