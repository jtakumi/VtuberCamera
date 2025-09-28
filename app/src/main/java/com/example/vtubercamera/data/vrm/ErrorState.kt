package com.example.vtubercamera.data.vrm

/**
 * Represents the current error state in the application
 */
data class ErrorState(
    val type: ErrorType,
    val message: String,
    val userMessage: String,
    val severity: ErrorSeverity,
    val isRecoverable: Boolean,
    val suggestedActions: List<ErrorAction>,
    val context: ErrorContext,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
) {
    /**
     * Check if this error can be automatically retried
     */
    fun canAutoRetry(): Boolean = isRecoverable && retryCount < 3
    
    /**
     * Create a new error state with incremented retry count
     */
    fun withRetry(): ErrorState = copy(retryCount = retryCount + 1)
    
    /**
     * Check if this is a critical error that should stop the operation
     */
    fun isCritical(): Boolean = severity == ErrorSeverity.HIGH
    
    /**
     * Check if this is a warning that can be ignored
     */
    fun isWarning(): Boolean = severity == ErrorSeverity.LOW
}

/**
 * Types of errors that can occur in the application
 */
enum class ErrorType {
    // VRM Loading Errors
    VRM_FILE_NOT_FOUND,
    VRM_INVALID_FORMAT,
    VRM_FILE_TOO_LARGE,
    VRM_CORRUPTED,
    VRM_UNSUPPORTED_VERSION,
    VRM_INSUFFICIENT_MEMORY,
    VRM_PERMISSION_DENIED,
    VRM_PARSE_ERROR,
    
    // AR Errors
    AR_NOT_SUPPORTED,
    AR_NOT_INSTALLED,
    AR_OUTDATED,
    AR_CAMERA_PERMISSION,
    AR_SESSION_FAILED,
    AR_TRACKING_LOST,
    AR_INSUFFICIENT_RESOURCES,
    AR_CAMERA_IN_USE,
    AR_SESSION_INTERRUPTED,
    AR_RENDERING_ERROR,
    AR_CONFIGURATION_ERROR,
    AR_UNSUPPORTED_DEVICE,
    AR_SESSION_ERROR,
    AR_TRACKING_ERROR,
    AR_AVATAR_ERROR,
    
    // Network Errors
    NETWORK_NO_CONNECTION,
    NETWORK_TIMEOUT,
    NETWORK_CONNECTION_FAILED,
    NETWORK_UNKNOWN,
    
    // File Access Errors
    FILE_PERMISSION_DENIED,
    FILE_IO_ERROR,
    FILE_UNKNOWN_ERROR,
    
    // General Errors
    UNKNOWN_ERROR
}

/**
 * Severity levels for errors
 */
enum class ErrorSeverity {
    LOW,     // Warning, doesn't prevent operation
    MEDIUM,  // Error that can be recovered from
    HIGH     // Critical error that stops operation
}

/**
 * Suggested actions that users can take to resolve errors
 */
enum class ErrorAction {
    // File Actions
    SELECT_DIFFERENT_FILE,
    SELECT_SMALLER_FILE,
    COMPRESS_FILE,
    VALIDATE_FILE_FORMAT,
    CHECK_FILE_PERMISSIONS,
    REDOWNLOAD_FILE,
    CONVERT_FILE_VERSION,
    
    // Permission Actions
    REQUEST_PERMISSIONS,
    REQUEST_CAMERA_PERMISSION,
    OPEN_SETTINGS,
    
    // AR Actions
    INSTALL_ARCORE,
    UPDATE_ARCORE,
    USE_2D_MODE,
    CHECK_DEVICE_COMPATIBILITY,
    IMPROVE_LIGHTING,
    MOVE_DEVICE_SLOWLY,
    RESTART_AR_SESSION,
    RESET_AR_SETTINGS,
    REDUCE_QUALITY,
    
    // Network Actions
    CHECK_NETWORK,
    USE_OFFLINE_MODE,
    TRY_LATER,
    
    // System Actions
    FREE_MEMORY,
    CLOSE_OTHER_APPS,
    RESTART_APP,
    RETRY_OPERATION,
    
    // Avatar Actions
    SELECT_DIFFERENT_AVATAR
}

/**
 * Context information about where the error occurred
 */
data class ErrorContext(
    val operation: String,
    val component: String,
    val additionalInfo: Map<String, String> = emptyMap()
) {
    companion object {
        fun vrmLoading(fileName: String) = ErrorContext(
            operation = "VRM Loading",
            component = "VRMRepository",
            additionalInfo = mapOf("fileName" to fileName)
        )
        
        fun arSession(sessionType: String) = ErrorContext(
            operation = "AR Session",
            component = "ARRepository",
            additionalInfo = mapOf("sessionType" to sessionType)
        )
        
        fun fileAccess(filePath: String) = ErrorContext(
            operation = "File Access",
            component = "FileSystem",
            additionalInfo = mapOf("filePath" to filePath)
        )
        
        fun network(url: String) = ErrorContext(
            operation = "Network Request",
            component = "NetworkClient",
            additionalInfo = mapOf("url" to url)
        )
        
        fun rendering(rendererType: String) = ErrorContext(
            operation = "Rendering",
            component = "ARRenderer",
            additionalInfo = mapOf("rendererType" to rendererType)
        )
    }
}

/**
 * Recovery strategies for different types of errors
 */
enum class RecoveryStrategy {
    // Automatic Recovery
    RETRY_OPERATION,
    RESTART_APP,
    FREE_MEMORY,
    REDUCE_QUALITY,
    
    // User Action Required
    SELECT_DIFFERENT_FILE,
    SELECT_SMALLER_FILE,
    COMPRESS_FILE,
    VALIDATE_FILE_FORMAT,
    CHECK_FILE_LOCATION,
    REQUEST_PERMISSIONS,
    OPEN_SETTINGS,
    
    // AR Specific
    INSTALL_ARCORE,
    UPDATE_ARCORE,
    USE_2D_MODE,
    CHECK_DEVICE_COMPATIBILITY,
    IMPROVE_LIGHTING,
    MOVE_DEVICE_SLOWLY,
    
    // Network Specific
    CHECK_NETWORK,
    USE_OFFLINE_MODE,
    TRY_LATER
}

/**
 * Record of an error that occurred
 */
data class ErrorRecord(
    val errorState: ErrorState,
    val timestamp: Long,
    val deviceInfo: String,
    val appVersion: String
) {
    /**
     * Check if this error occurred recently (within last 5 minutes)
     */
    fun isRecent(): Boolean = System.currentTimeMillis() - timestamp < 5 * 60 * 1000
    
    /**
     * Get a human-readable timestamp
     */
    fun getFormattedTimestamp(): String {
        val date = java.util.Date(timestamp)
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(date)
    }
}