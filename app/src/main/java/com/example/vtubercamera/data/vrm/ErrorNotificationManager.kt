package com.example.vtubercamera.data.vrm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages error notifications and user feedback for error states
 */
@Singleton
class ErrorNotificationManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "ErrorNotificationManager"
        private const val MAX_NOTIFICATIONS = 5
    }
    
    private val _activeNotifications = MutableStateFlow<List<ErrorNotification>>(emptyList())
    val activeNotifications: StateFlow<List<ErrorNotification>> = _activeNotifications.asStateFlow()
    
    private val _dismissedNotifications = MutableStateFlow<List<ErrorNotification>>(emptyList())
    val dismissedNotifications: StateFlow<List<ErrorNotification>> = _dismissedNotifications.asStateFlow()
    
    /**
     * Show an error notification to the user
     */
    fun showErrorNotification(errorState: ErrorState): ErrorNotification {
        val notification = createNotification(errorState)
        
        val currentNotifications = _activeNotifications.value.toMutableList()
        
        // Remove any existing notification for the same error type
        currentNotifications.removeAll { it.errorType == errorState.type }
        
        // Add new notification at the beginning
        currentNotifications.add(0, notification)
        
        // Keep only the most recent notifications
        if (currentNotifications.size > MAX_NOTIFICATIONS) {
            currentNotifications.removeAt(currentNotifications.size - 1)
        }
        
        _activeNotifications.value = currentNotifications
        
        Log.d(TAG, "Showing error notification: ${notification.title}")
        return notification
    }
    
    /**
     * Dismiss a specific notification
     */
    fun dismissNotification(notificationId: String) {
        val currentNotifications = _activeNotifications.value.toMutableList()
        val notification = currentNotifications.find { it.id == notificationId }
        
        if (notification != null) {
            currentNotifications.remove(notification)
            _activeNotifications.value = currentNotifications
            
            // Add to dismissed notifications
            val dismissedNotifications = _dismissedNotifications.value.toMutableList()
            dismissedNotifications.add(0, notification.copy(isDismissed = true, dismissedAt = System.currentTimeMillis()))
            
            // Keep only last 20 dismissed notifications
            if (dismissedNotifications.size > 20) {
                dismissedNotifications.removeAt(dismissedNotifications.size - 1)
            }
            
            _dismissedNotifications.value = dismissedNotifications
            
            Log.d(TAG, "Dismissed notification: ${notification.title}")
        }
    }
    
    /**
     * Dismiss all notifications
     */
    fun dismissAllNotifications() {
        val currentNotifications = _activeNotifications.value
        
        currentNotifications.forEach { notification ->
            val dismissedNotifications = _dismissedNotifications.value.toMutableList()
            dismissedNotifications.add(0, notification.copy(isDismissed = true, dismissedAt = System.currentTimeMillis()))
            _dismissedNotifications.value = dismissedNotifications
        }
        
        _activeNotifications.value = emptyList()
        Log.d(TAG, "Dismissed all notifications")
    }
    
    /**
     * Clear all notifications (both active and dismissed)
     */
    fun clearAllNotifications() {
        _activeNotifications.value = emptyList()
        _dismissedNotifications.value = emptyList()
        Log.d(TAG, "Cleared all notifications")
    }
    
    /**
     * Get notification for a specific error type
     */
    fun getNotificationForErrorType(errorType: ErrorType): ErrorNotification? {
        return _activeNotifications.value.find { it.errorType == errorType }
    }
    
    /**
     * Check if there are any critical error notifications
     */
    fun hasCriticalNotifications(): Boolean {
        return _activeNotifications.value.any { it.severity == ErrorSeverity.HIGH }
    }
    
    /**
     * Get count of active notifications by severity
     */
    fun getNotificationCountBySeverity(): Map<ErrorSeverity, Int> {
        val notifications = _activeNotifications.value
        return mapOf(
            ErrorSeverity.HIGH to notifications.count { it.severity == ErrorSeverity.HIGH },
            ErrorSeverity.MEDIUM to notifications.count { it.severity == ErrorSeverity.MEDIUM },
            ErrorSeverity.LOW to notifications.count { it.severity == ErrorSeverity.LOW }
        )
    }
    
    private fun createNotification(errorState: ErrorState): ErrorNotification {
        val notificationId = "${errorState.type.name}_${System.currentTimeMillis()}"
        
        return when (errorState.type) {
            ErrorType.VRM_FILE_NOT_FOUND -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "VRM File Not Found",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.FILE_ERROR,
                actions = listOf(
                    NotificationAction("Select File", ErrorAction.SELECT_DIFFERENT_FILE),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null // Don't auto-hide critical errors
            )
            
            ErrorType.VRM_INVALID_FORMAT -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "Invalid VRM Format",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.FORMAT_ERROR,
                actions = listOf(
                    NotificationAction("Select File", ErrorAction.SELECT_DIFFERENT_FILE),
                    NotificationAction("Help", ErrorAction.VALIDATE_FILE_FORMAT),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )
            
            ErrorType.VRM_FILE_TOO_LARGE -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "File Too Large",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.SIZE_ERROR,
                actions = listOf(
                    NotificationAction("Select Smaller", ErrorAction.SELECT_SMALLER_FILE),
                    NotificationAction("Compress", ErrorAction.COMPRESS_FILE),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )
            
            ErrorType.AR_NOT_SUPPORTED -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "AR Not Supported",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.AR_ERROR,
                actions = listOf(
                    NotificationAction("Use 2D Mode", ErrorAction.USE_2D_MODE),
                    NotificationAction("Check Device", ErrorAction.CHECK_DEVICE_COMPATIBILITY),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = 10000 // Auto-hide after 10 seconds
            )
            
            ErrorType.AR_NOT_INSTALLED -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "ARCore Required",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.AR_ERROR,
                actions = listOf(
                    NotificationAction("Install ARCore", ErrorAction.INSTALL_ARCORE),
                    NotificationAction("Use 2D Mode", ErrorAction.USE_2D_MODE),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )
            
            ErrorType.AR_TRACKING_LOST -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "AR Tracking Lost",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.TRACKING_ERROR,
                actions = listOf(
                    NotificationAction("Improve Lighting", ErrorAction.IMPROVE_LIGHTING),
                    NotificationAction("Move Slowly", ErrorAction.MOVE_DEVICE_SLOWLY),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = 5000 // Auto-hide after 5 seconds
            )
            
            ErrorType.NETWORK_NO_CONNECTION -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "No Internet Connection",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.NETWORK_ERROR,
                actions = listOf(
                    NotificationAction("Check Network", ErrorAction.CHECK_NETWORK),
                    NotificationAction("Offline Mode", ErrorAction.USE_OFFLINE_MODE),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = 8000
            )
            
            ErrorType.FILE_PERMISSION_DENIED -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "Permission Required",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.PERMISSION_ERROR,
                actions = listOf(
                    NotificationAction("Re-select File", ErrorAction.REQUEST_PERMISSIONS),
                    NotificationAction("Settings", ErrorAction.OPEN_SETTINGS),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )
            
            ErrorType.VRM_INSUFFICIENT_MEMORY -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "Low Memory",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.MEMORY_ERROR,
                actions = listOf(
                    NotificationAction("Free Memory", ErrorAction.FREE_MEMORY),
                    NotificationAction("Restart App", ErrorAction.RESTART_APP),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )

            ErrorType.VRM_PERMISSION_DENIED -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "Permission Required",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.PERMISSION_ERROR,
                actions = listOf(
                    NotificationAction("Re-select File", ErrorAction.REQUEST_PERMISSIONS),
                    NotificationAction("Settings", ErrorAction.OPEN_SETTINGS),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )

            ErrorType.VRM_PARSE_ERROR -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "VRM Parse Error",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.FORMAT_ERROR,
                actions = listOf(
                    NotificationAction("Select File", ErrorAction.SELECT_DIFFERENT_FILE),
                    NotificationAction("Help", ErrorAction.VALIDATE_FILE_FORMAT),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )

            ErrorType.VRM_CORRUPTED -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "Corrupted VRM File",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.FILE_ERROR,
                actions = listOf(
                    NotificationAction("Select File", ErrorAction.SELECT_DIFFERENT_FILE),
                    NotificationAction("Re-download", ErrorAction.REDOWNLOAD_FILE),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )

            ErrorType.VRM_UNSUPPORTED_VERSION -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "Unsupported VRM Version",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.FORMAT_ERROR,
                actions = listOf(
                    NotificationAction("Convert", ErrorAction.CONVERT_FILE_VERSION),
                    NotificationAction("Select File", ErrorAction.SELECT_DIFFERENT_FILE),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = null
            )
            
            else -> ErrorNotification(
                id = notificationId,
                errorType = errorState.type,
                title = "Error Occurred",
                message = errorState.userMessage,
                severity = errorState.severity,
                icon = NotificationIcon.GENERIC_ERROR,
                actions = listOf(
                    NotificationAction("Retry", ErrorAction.RETRY_OPERATION),
                    NotificationAction("Dismiss", null)
                ),
                autoHideAfterMs = 5000
            )
        }
    }
}

/**
 * Represents an error notification shown to the user
 */
data class ErrorNotification(
    val id: String,
    val errorType: ErrorType,
    val title: String,
    val message: String,
    val severity: ErrorSeverity,
    val icon: NotificationIcon,
    val actions: List<NotificationAction>,
    val timestamp: Long = System.currentTimeMillis(),
    val autoHideAfterMs: Long? = null,
    val isDismissed: Boolean = false,
    val dismissedAt: Long? = null
) {
    /**
     * Check if this notification should auto-hide
     */
    fun shouldAutoHide(): Boolean {
        return autoHideAfterMs != null && 
               System.currentTimeMillis() - timestamp > autoHideAfterMs
    }
    
    /**
     * Check if this notification is recent (within last 30 seconds)
     */
    fun isRecent(): Boolean {
        return System.currentTimeMillis() - timestamp < 30000
    }
    
    /**
     * Get formatted timestamp
     */
    fun getFormattedTime(): String {
        val date = java.util.Date(timestamp)
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(date)
    }
}

/**
 * Action that can be taken from a notification
 */
data class NotificationAction(
    val label: String,
    val action: ErrorAction? // null for dismiss action
) {
    val isDismissAction: Boolean = action == null
}

/**
 * Icons for different types of error notifications
 */
enum class NotificationIcon {
    GENERIC_ERROR,
    FILE_ERROR,
    FORMAT_ERROR,
    SIZE_ERROR,
    AR_ERROR,
    TRACKING_ERROR,
    NETWORK_ERROR,
    PERMISSION_ERROR,
    MEMORY_ERROR
}
