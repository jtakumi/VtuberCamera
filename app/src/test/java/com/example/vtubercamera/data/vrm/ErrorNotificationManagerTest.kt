package com.example.vtubercamera.data.vrm

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ErrorNotificationManagerTest {
    
    private lateinit var context: Context
    private lateinit var notificationManager: ErrorNotificationManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        notificationManager = ErrorNotificationManager(context)
    }
    
    @Test
    fun `showErrorNotification should create and display notification`() = runTest {
        // Given
        val errorState = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found",
            userMessage = "The VRM file could not be found",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test.vrm")
        )
        
        // When
        val notification = notificationManager.showErrorNotification(errorState)
        
        // Then
        assertEquals(ErrorType.VRM_FILE_NOT_FOUND, notification.errorType)
        assertEquals("VRM File Not Found", notification.title)
        assertEquals(errorState.userMessage, notification.message)
        assertEquals(ErrorSeverity.HIGH, notification.severity)
        assertEquals(NotificationIcon.FILE_ERROR, notification.icon)
        
        // Check that notification is in active list
        val activeNotifications = notificationManager.activeNotifications.first()
        assertEquals(1, activeNotifications.size)
        assertEquals(notification, activeNotifications[0])
    }
    
    @Test
    fun `showErrorNotification should replace existing notification of same type`() = runTest {
        // Given
        val errorState1 = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found 1",
            userMessage = "First error",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test1.vrm")
        )
        
        val errorState2 = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found 2",
            userMessage = "Second error",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test2.vrm")
        )
        
        // When
        notificationManager.showErrorNotification(errorState1)
        notificationManager.showErrorNotification(errorState2)
        
        // Then
        val activeNotifications = notificationManager.activeNotifications.first()
        assertEquals(1, activeNotifications.size)
        assertEquals("Second error", activeNotifications[0].message)
    }
    
    @Test
    fun `dismissNotification should move notification to dismissed list`() = runTest {
        // Given
        val errorState = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found",
            userMessage = "The VRM file could not be found",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test.vrm")
        )
        
        val notification = notificationManager.showErrorNotification(errorState)
        
        // When
        notificationManager.dismissNotification(notification.id)
        
        // Then
        val activeNotifications = notificationManager.activeNotifications.first()
        val dismissedNotifications = notificationManager.dismissedNotifications.first()
        
        assertEquals(0, activeNotifications.size)
        assertEquals(1, dismissedNotifications.size)
        assertTrue(dismissedNotifications[0].isDismissed)
        assertNotNull(dismissedNotifications[0].dismissedAt)
    }
    
    @Test
    fun `dismissAllNotifications should clear all active notifications`() = runTest {
        // Given
        val errorState1 = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found",
            userMessage = "First error",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test1.vrm")
        )
        
        val errorState2 = ErrorState(
            type = ErrorType.AR_TRACKING_LOST,
            message = "Tracking lost",
            userMessage = "Second error",
            severity = ErrorSeverity.LOW,
            isRecoverable = true,
            suggestedActions = listOf(ErrorAction.IMPROVE_LIGHTING),
            context = ErrorContext.arSession("tracking")
        )
        
        notificationManager.showErrorNotification(errorState1)
        notificationManager.showErrorNotification(errorState2)
        
        // When
        notificationManager.dismissAllNotifications()
        
        // Then
        val activeNotifications = notificationManager.activeNotifications.first()
        val dismissedNotifications = notificationManager.dismissedNotifications.first()
        
        assertEquals(0, activeNotifications.size)
        assertEquals(2, dismissedNotifications.size)
    }
    
    @Test
    fun `hasCriticalNotifications should return true when critical notifications exist`() = runTest {
        // Given
        val criticalErrorState = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found",
            userMessage = "Critical error",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test.vrm")
        )
        
        val warningErrorState = ErrorState(
            type = ErrorType.AR_TRACKING_LOST,
            message = "Tracking lost",
            userMessage = "Warning error",
            severity = ErrorSeverity.LOW,
            isRecoverable = true,
            suggestedActions = listOf(ErrorAction.IMPROVE_LIGHTING),
            context = ErrorContext.arSession("tracking")
        )
        
        // When
        notificationManager.showErrorNotification(warningErrorState)
        assertFalse(notificationManager.hasCriticalNotifications())
        
        notificationManager.showErrorNotification(criticalErrorState)
        assertTrue(notificationManager.hasCriticalNotifications())
    }
    
    @Test
    fun `getNotificationCountBySeverity should return correct counts`() = runTest {
        // Given
        val highSeverityError = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found",
            userMessage = "High severity error",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test.vrm")
        )
        
        val mediumSeverityError = ErrorState(
            type = ErrorType.NETWORK_TIMEOUT,
            message = "Network timeout",
            userMessage = "Medium severity error",
            severity = ErrorSeverity.MEDIUM,
            isRecoverable = true,
            suggestedActions = listOf(ErrorAction.CHECK_NETWORK),
            context = ErrorContext.network("https://example.com")
        )
        
        val lowSeverityError = ErrorState(
            type = ErrorType.AR_TRACKING_LOST,
            message = "Tracking lost",
            userMessage = "Low severity error",
            severity = ErrorSeverity.LOW,
            isRecoverable = true,
            suggestedActions = listOf(ErrorAction.IMPROVE_LIGHTING),
            context = ErrorContext.arSession("tracking")
        )
        
        // When
        notificationManager.showErrorNotification(highSeverityError)
        notificationManager.showErrorNotification(mediumSeverityError)
        notificationManager.showErrorNotification(lowSeverityError)
        
        // Then
        val counts = notificationManager.getNotificationCountBySeverity()
        assertEquals(1, counts[ErrorSeverity.HIGH])
        assertEquals(1, counts[ErrorSeverity.MEDIUM])
        assertEquals(1, counts[ErrorSeverity.LOW])
    }
    
    @Test
    fun `notification should have correct actions based on error type`() = runTest {
        // Test VRM file not found notification
        val vrmErrorState = ErrorState(
            type = ErrorType.VRM_FILE_NOT_FOUND,
            message = "File not found",
            userMessage = "VRM file not found",
            severity = ErrorSeverity.HIGH,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.SELECT_DIFFERENT_FILE),
            context = ErrorContext.vrmLoading("test.vrm")
        )
        
        val vrmNotification = notificationManager.showErrorNotification(vrmErrorState)
        
        assertTrue(vrmNotification.actions.any { it.action == ErrorAction.SELECT_DIFFERENT_FILE })
        assertTrue(vrmNotification.actions.any { it.isDismissAction })
        
        // Test AR not supported notification
        val arErrorState = ErrorState(
            type = ErrorType.AR_NOT_SUPPORTED,
            message = "AR not supported",
            userMessage = "AR not supported on this device",
            severity = ErrorSeverity.LOW,
            isRecoverable = false,
            suggestedActions = listOf(ErrorAction.USE_2D_MODE),
            context = ErrorContext.arSession("initialization")
        )
        
        val arNotification = notificationManager.showErrorNotification(arErrorState)
        
        assertTrue(arNotification.actions.any { it.action == ErrorAction.USE_2D_MODE })
        assertTrue(arNotification.actions.any { it.action == ErrorAction.CHECK_DEVICE_COMPATIBILITY })
        assertNotNull(arNotification.autoHideAfterMs) // Should auto-hide
    }
    
    @Test
    fun `notification should auto-hide based on configuration`() = runTest {
        // Given
        val errorState = ErrorState(
            type = ErrorType.AR_TRACKING_LOST,
            message = "Tracking lost",
            userMessage = "AR tracking lost",
            severity = ErrorSeverity.LOW,
            isRecoverable = true,
            suggestedActions = listOf(ErrorAction.IMPROVE_LIGHTING),
            context = ErrorContext.arSession("tracking")
        )
        
        // When
        val notification = notificationManager.showErrorNotification(errorState)
        
        // Then
        assertNotNull(notification.autoHideAfterMs)
        assertEquals(5000L, notification.autoHideAfterMs) // Should auto-hide after 5 seconds
        
        // Test that it should auto-hide after the specified time
        Thread.sleep(100) // Small delay to ensure timestamp difference
        assertFalse(notification.shouldAutoHide()) // Should not auto-hide yet
    }
}