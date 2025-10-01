package com.example.vtubercamera.data.vrm

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized error handler for file access operations
 */
@Singleton
class FileAccessErrorHandler @Inject constructor(
    private val context: Context,
    private val errorHandler: ErrorHandler,
    private val errorNotificationManager: ErrorNotificationManager
) {
    
    companion object {
        private const val TAG = "FileAccessErrorHandler"
        private const val MIN_FREE_SPACE_MB = 100L // Minimum 100MB free space required
    }
    
    /**
     * Execute a file operation with comprehensive error handling
     */
    suspend fun <T> executeFileOperation(
        operation: suspend () -> T,
        operationName: String,
        filePath: String
    ): Result<T> = withContext(Dispatchers.IO) {
        val context = ErrorContext.fileAccess(filePath)
        
        try {
            Log.d(TAG, "Executing file operation: $operationName for path: $filePath")
            
            // Pre-flight checks
            val preflightResult = performPreflightChecks(filePath, operationName)
            if (preflightResult.isFailure) {
                val error = preflightResult.exceptionOrNull()!!
                val errorState = errorHandler.handleFileAccessError(error, context)
                errorNotificationManager.showErrorNotification(errorState)
                return@withContext Result.failure(error)
            }
            
            val result = operation()
            Log.d(TAG, "File operation completed successfully: $operationName")
            return@withContext Result.success(result)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for file operation: $operationName", e)
            val errorState = errorHandler.handleFileAccessError(e, context)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(e)
            
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "File not found for operation: $operationName", e)
            val vrmError = VRMLoadingError.FileNotFound
            val errorState = errorHandler.handleVRMError(vrmError, context)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(vrmError)
            
        } catch (e: IOException) {
            Log.e(TAG, "IO error for file operation: $operationName", e)
            val errorState = handleIOError(e, context, filePath)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(e)
            
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory for file operation: $operationName", e)
            val vrmError = VRMLoadingError.InsufficientMemory
            val errorState = errorHandler.handleVRMError(vrmError, context)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(vrmError)
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error for file operation: $operationName", e)
            val errorState = errorHandler.handleFileAccessError(e, context)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(e)
        }
    }
    
    /**
     * Handle URI-based file operations
     */
    suspend fun <T> executeUriOperation(
        operation: suspend () -> T,
        operationName: String,
        uri: Uri
    ): Result<T> = withContext(Dispatchers.IO) {
        val context = ErrorContext.fileAccess(uri.toString())
        
        try {
            Log.d(TAG, "Executing URI operation: $operationName for URI: $uri")
            
            // Check if URI is accessible
            val uriCheckResult = checkUriAccessibility(uri)
            if (uriCheckResult.isFailure) {
                val error = uriCheckResult.exceptionOrNull()!!
                val errorState = errorHandler.handleFileAccessError(error, context)
                errorNotificationManager.showErrorNotification(errorState)
                return@withContext Result.failure(error)
            }
            
            val result = operation()
            Log.d(TAG, "URI operation completed successfully: $operationName")
            return@withContext Result.success(result)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for URI operation: $operationName", e)
            val errorState = errorHandler.handleFileAccessError(e, context)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(e)
            
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "URI not found for operation: $operationName", e)
            val vrmError = VRMLoadingError.FileNotFound
            val errorState = errorHandler.handleVRMError(vrmError, context)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(vrmError)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error for URI operation: $operationName", e)
            val errorState = errorHandler.handleFileAccessError(e, context)
            errorNotificationManager.showErrorNotification(errorState)
            Result.failure(e)
        }
    }
    
    /**
     * Perform pre-flight checks before file operations
     */
    private fun performPreflightChecks(filePath: String, operationName: String): Result<Unit> {
        try {
            val file = File(filePath)
            
            // Check if file exists for read operations
            if (operationName.contains("read", ignoreCase = true) || 
                operationName.contains("load", ignoreCase = true)) {
                if (!file.exists()) {
                    return Result.failure(FileNotFoundException("File does not exist: $filePath"))
                }
                
                if (!file.canRead()) {
                    return Result.failure(SecurityException("Cannot read file: $filePath"))
                }
            }
            
            // Check parent directory for write operations
            if (operationName.contains("write", ignoreCase = true) || 
                operationName.contains("save", ignoreCase = true)) {
                val parentDir = file.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        return Result.failure(IOException("Cannot create directory: ${parentDir.absolutePath}"))
                    }
                }
                
                if (parentDir != null && !parentDir.canWrite()) {
                    return Result.failure(SecurityException("Cannot write to directory: ${parentDir.absolutePath}"))
                }
                
                // Check available space
                val availableSpace = getAvailableSpaceInMB(parentDir ?: file)
                if (availableSpace < MIN_FREE_SPACE_MB) {
                    return Result.failure(IOException("Insufficient storage space. Available: ${availableSpace}MB, Required: ${MIN_FREE_SPACE_MB}MB"))
                }
            }
            
            return Result.success(Unit)
            
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Check if URI is accessible
     */
    private fun checkUriAccessibility(uri: Uri): Result<Unit> {
        try {
            // Try to get basic information about the URI
            val contentResolver = context.contentResolver
            
            // Check if we can open an input stream
            contentResolver.openInputStream(uri)?.use { 
                // Just check if we can open it, don't read anything
            } ?: return Result.failure(FileNotFoundException("Cannot access URI: $uri"))
            
            return Result.success(Unit)
            
        } catch (_: SecurityException) {
            return Result.failure(SecurityException("Permission denied for URI: $uri"))
        } catch (_: FileNotFoundException) {
            return Result.failure(FileNotFoundException("URI not found: $uri"))
        } catch (e: Exception) {
            return Result.failure(IOException("Cannot access URI: $uri - ${e.message}"))
        }
    }
    
    /**
     * Handle specific IO errors with detailed context
     */
    private fun handleIOError(error: IOException, context: ErrorContext, filePath: String): ErrorState {
        val errorMessage = error.message?.lowercase() ?: ""
        
        return when {
            errorMessage.contains("no space left") || errorMessage.contains("disk full") -> {
                ErrorState(
                    type = ErrorType.FILE_IO_ERROR,
                    message = "Insufficient storage space",
                    userMessage = "Not enough storage space available. Please free up some space and try again.",
                    severity = ErrorSeverity.HIGH,
                    isRecoverable = true,
                    suggestedActions = listOf(
                        ErrorAction.FREE_MEMORY,
                        ErrorAction.SELECT_DIFFERENT_FILE
                    ),
                    context = context
                )
            }
            
            errorMessage.contains("permission denied") || errorMessage.contains("access denied") -> {
                ErrorState(
                    type = ErrorType.FILE_PERMISSION_DENIED,
                    message = "File permission denied",
                    userMessage = "Permission denied to access the file. Please check file permissions.",
                    severity = ErrorSeverity.HIGH,
                    isRecoverable = true,
                    suggestedActions = listOf(
                        ErrorAction.REQUEST_PERMISSIONS,
                        ErrorAction.OPEN_SETTINGS,
                        ErrorAction.SELECT_DIFFERENT_FILE
                    ),
                    context = context
                )
            }
            
            errorMessage.contains("read-only") -> {
                ErrorState(
                    type = ErrorType.FILE_IO_ERROR,
                    message = "File is read-only",
                    userMessage = "The file or storage location is read-only. Please select a different location.",
                    severity = ErrorSeverity.MEDIUM,
                    isRecoverable = true,
                    suggestedActions = listOf(
                        ErrorAction.SELECT_DIFFERENT_FILE,
                        ErrorAction.CHECK_FILE_PERMISSIONS
                    ),
                    context = context
                )
            }
            
            errorMessage.contains("corrupted") || errorMessage.contains("invalid") -> {
                ErrorState(
                    type = ErrorType.VRM_CORRUPTED,
                    message = "File appears to be corrupted",
                    userMessage = "The file appears to be corrupted or damaged. Please try a different file.",
                    severity = ErrorSeverity.HIGH,
                    isRecoverable = true,
                    suggestedActions = listOf(
                        ErrorAction.SELECT_DIFFERENT_FILE,
                        ErrorAction.REDOWNLOAD_FILE,
                        ErrorAction.VALIDATE_FILE_FORMAT
                    ),
                    context = context
                )
            }
            
            else -> {
                errorHandler.handleFileAccessError(error, context)
            }
        }
    }
    
    /**
     * Get available storage space in MB
     */
    private fun getAvailableSpaceInMB(file: File): Long {
        return try {
            val stat = StatFs(file.absolutePath)
            val availableBytes = stat.availableBytes
            availableBytes / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            Log.w(TAG, "Could not get available space for: ${file.absolutePath}", e)
            0L
        }
    }
    
    /**
     * Check if external storage is available and writable
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * Check if external storage is available for reading
     */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }
    
    /**
     * Get file size from URI
     */
    fun getFileSizeFromUri(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else {
                    -1L
                }
            } ?: -1L
        } catch (e: Exception) {
            Log.w(TAG, "Could not get file size for URI: $uri", e)
            -1L
        }
    }
    
    /**
     * Get file name from URI
     */
    fun getFileNameFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get file name for URI: $uri", e)
            null
        }
    }
}