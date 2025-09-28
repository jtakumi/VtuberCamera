package com.example.vtubercamera.data.vrm

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class ErrorHandlerTest {
    
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var errorHandler: ErrorHandler
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.example.vtubercamera"
        
        val packageInfo = mockk<PackageInfo>()
        every { packageInfo.versionName } returns "1.0.0"
        every { packageInfo.longVersionCode } returns 1L
        every { packageManager.getPackageInfo("com.example.vtubercamera", 0) } returns packageInfo
        
        errorHandler = ErrorHandler(context)
    }
    
    @Test
    fun `handleVRMError should create appropriate error state for FileNotFound`() = runTest {
        // Given
        val error = VRMLoadingError.FileNotFound
        val context = ErrorContext.vrmLoading("test.vrm")
        
        // When
        val errorState = errorHandler.handleVRMError(error, context)
        
        // Then
        assertEquals(ErrorType.VRM_FILE_NOT_FOUND, errorState.type)
        assertEquals(ErrorSeverity.HIGH, errorState.severity)
        assertFalse(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.SELECT_DIFFERENT_FILE))
        
        // Verify error is recorded
        val currentError = errorHandler.currentError.first()
        assertNotNull(currentError)
        assertEquals(errorState, currentError)
    }
    
    @Test
    fun `handleVRMError should create appropriate error state for InvalidFormat`() = runTest {
        // Given
        val error = VRMLoadingError.InvalidFormat
        val context = ErrorContext.vrmLoading("test.vrm")
        
        // When
        val errorState = errorHandler.handleVRMError(error, context)
        
        // Then
        assertEquals(ErrorType.VRM_INVALID_FORMAT, errorState.type)
        assertEquals(ErrorSeverity.HIGH, errorState.severity)
        assertFalse(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.VALIDATE_FILE_FORMAT))
    }
    
    @Test
    fun `handleVRMError should create appropriate error state for InsufficientMemory`() = runTest {
        // Given
        val error = VRMLoadingError.InsufficientMemory
        val context = ErrorContext.vrmLoading("test.vrm")
        
        // When
        val errorState = errorHandler.handleVRMError(error, context)
        
        // Then
        assertEquals(ErrorType.VRM_INSUFFICIENT_MEMORY, errorState.type)
        assertEquals(ErrorSeverity.HIGH, errorState.severity)
        assertTrue(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.FREE_MEMORY))
    }
    
    @Test
    fun `handleARError should create appropriate error state for ARCoreNotSupported`() = runTest {
        // Given
        val error = ARError.ARCoreNotSupported
        val context = ErrorContext.arSession("initialization")
        
        // When
        val errorState = errorHandler.handleARError(error, context)
        
        // Then
        assertEquals(ErrorType.AR_NOT_SUPPORTED, errorState.type)
        assertEquals(ErrorSeverity.LOW, errorState.severity)
        assertFalse(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.USE_2D_MODE))
    }
    
    @Test
    fun `handleARError should create appropriate error state for TrackingLost`() = runTest {
        // Given
        val error = ARError.TrackingLost
        val context = ErrorContext.arSession("tracking")
        
        // When
        val errorState = errorHandler.handleARError(error, context)
        
        // Then
        assertEquals(ErrorType.AR_TRACKING_LOST, errorState.type)
        assertEquals(ErrorSeverity.LOW, errorState.severity)
        assertTrue(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.IMPROVE_LIGHTING))
        assertTrue(errorState.suggestedActions.contains(ErrorAction.MOVE_DEVICE_SLOWLY))
    }
    
    @Test
    fun `handleNetworkError should create appropriate error state for UnknownHostException`() = runTest {
        // Given
        val error = UnknownHostException("No internet connection")
        val context = ErrorContext.network("https://example.com")
        
        // When
        val errorState = errorHandler.handleNetworkError(error, context)
        
        // Then
        assertEquals(ErrorType.NETWORK_NO_CONNECTION, errorState.type)
        assertEquals(ErrorSeverity.MEDIUM, errorState.severity)
        assertTrue(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.CHECK_NETWORK))
        assertTrue(errorState.suggestedActions.contains(ErrorAction.USE_OFFLINE_MODE))
    }
    
    @Test
    fun `handleFileAccessError should create appropriate error state for SecurityException`() = runTest {
        // Given
        val error = SecurityException("Permission denied")
        val context = ErrorContext.fileAccess("/path/to/file")
        
        // When
        val errorState = errorHandler.handleFileAccessError(error, context)
        
        // Then
        assertEquals(ErrorType.FILE_PERMISSION_DENIED, errorState.type)
        assertEquals(ErrorSeverity.HIGH, errorState.severity)
        assertTrue(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.REQUEST_PERMISSIONS))
    }
    
    @Test
    fun `handleFileAccessError should create appropriate error state for IOException`() = runTest {
        // Given
        val error = IOException("File read error")
        val context = ErrorContext.fileAccess("/path/to/file")
        
        // When
        val errorState = errorHandler.handleFileAccessError(error, context)
        
        // Then
        assertEquals(ErrorType.FILE_IO_ERROR, errorState.type)
        assertEquals(ErrorSeverity.MEDIUM, errorState.severity)
        assertTrue(errorState.isRecoverable)
        assertTrue(errorState.suggestedActions.contains(ErrorAction.RETRY_OPERATION))
    }
    
    @Test
    fun `clearError should clear current error state`() = runTest {
        // Given
        val error = VRMLoadingError.FileNotFound
        val context = ErrorContext.vrmLoading("test.vrm")
        errorHandler.handleVRMError(error, context)
        
        // When
        errorHandler.clearError()
        
        // Then
        val currentError = errorHandler.currentError.first()
        assertNull(currentError)
    }
    
    @Test
    fun `getRecoveryStrategies should return appropriate strategies for different error types`() {
        // Test VRM file not found
        val vrmStrategies = errorHandler.getRecoveryStrategies(ErrorType.VRM_FILE_NOT_FOUND)
        assertTrue(vrmStrategies.contains(RecoveryStrategy.SELECT_DIFFERENT_FILE))
        
        // Test AR not supported
        val arStrategies = errorHandler.getRecoveryStrategies(ErrorType.AR_NOT_SUPPORTED)
        assertTrue(arStrategies.contains(RecoveryStrategy.USE_2D_MODE))
        
        // Test network error
        val networkStrategies = errorHandler.getRecoveryStrategies(ErrorType.NETWORK_NO_CONNECTION)
        assertTrue(networkStrategies.contains(RecoveryStrategy.CHECK_NETWORK))
        
        // Test memory error
        val memoryStrategies = errorHandler.getRecoveryStrategies(ErrorType.VRM_INSUFFICIENT_MEMORY)
        assertTrue(memoryStrategies.contains(RecoveryStrategy.FREE_MEMORY))
    }
    
    @Test
    fun `isAutoRetryable should return true for retryable errors`() {
        assertTrue(errorHandler.isAutoRetryable(ErrorType.NETWORK_TIMEOUT))
        assertTrue(errorHandler.isAutoRetryable(ErrorType.FILE_IO_ERROR))
        assertTrue(errorHandler.isAutoRetryable(ErrorType.AR_TRACKING_LOST))
        
        assertFalse(errorHandler.isAutoRetryable(ErrorType.VRM_FILE_NOT_FOUND))
        assertFalse(errorHandler.isAutoRetryable(ErrorType.AR_NOT_SUPPORTED))
    }
    
    @Test
    fun `error history should be maintained correctly`() = runTest {
        // Given
        val error1 = VRMLoadingError.FileNotFound
        val error2 = ARError.TrackingLost
        val context1 = ErrorContext.vrmLoading("test1.vrm")
        val context2 = ErrorContext.arSession("tracking")
        
        // When
        errorHandler.handleVRMError(error1, context1)
        errorHandler.handleARError(error2, context2)
        
        // Then
        val errorHistory = errorHandler.errorHistory.first()
        assertEquals(2, errorHistory.size)
        
        // Most recent error should be first
        assertEquals(ErrorType.AR_TRACKING_LOST, errorHistory[0].errorState.type)
        assertEquals(ErrorType.VRM_FILE_NOT_FOUND, errorHistory[1].errorState.type)
    }
    
    @Test
    fun `error context should contain correct information`() {
        // Test VRM loading context
        val vrmContext = ErrorContext.vrmLoading("test.vrm")
        assertEquals("VRM Loading", vrmContext.operation)
        assertEquals("VRMRepository", vrmContext.component)
        assertEquals("test.vrm", vrmContext.additionalInfo["fileName"])
        
        // Test AR session context
        val arContext = ErrorContext.arSession("initialization")
        assertEquals("AR Session", arContext.operation)
        assertEquals("ARRepository", arContext.component)
        assertEquals("initialization", arContext.additionalInfo["sessionType"])
        
        // Test network context
        val networkContext = ErrorContext.network("https://example.com")
        assertEquals("Network Request", networkContext.operation)
        assertEquals("NetworkClient", networkContext.component)
        assertEquals("https://example.com", networkContext.additionalInfo["url"])
    }
}