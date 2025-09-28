package com.example.vtubercamera.data.vrm

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized error handler for network-related operations
 */
@Singleton
class NetworkErrorHandler @Inject constructor(
    private val context: Context,
    private val errorHandler: ErrorHandler,
    private val errorNotificationManager: ErrorNotificationManager
) {
    
    companion object {
        private const val TAG = "NetworkErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
    }
    
    /**
     * Execute a network operation with automatic error handling and retry
     */
    suspend fun <T> executeWithErrorHandling(
        operation: suspend () -> T,
        operationName: String,
        url: String = "",
        maxRetries: Int = MAX_RETRY_ATTEMPTS
    ): Result<T> {
        var lastException: Exception? = null
        val context = ErrorContext.network(url)
        
        repeat(maxRetries) { attempt ->
            try {
                // Check network connectivity before attempting operation
                if (!isNetworkAvailable()) {
                    val error = UnknownHostException("No network connection available")
                    val errorState = errorHandler.handleNetworkError(error, context)
                    if (attempt == 0) { // Only show notification on first attempt
                        errorNotificationManager.showErrorNotification(errorState)
                    }
                    throw error
                }
                
                Log.d(TAG, "Executing network operation: $operationName (attempt ${attempt + 1})")
                val result = operation()
                
                // Clear any previous error notifications for successful operation
                if (attempt > 0) {
                    Log.d(TAG, "Network operation succeeded after ${attempt + 1} attempts")
                }
                
                return Result.success(result)
                
            } catch (e: UnknownHostException) {
                Log.w(TAG, "Network unavailable for operation: $operationName", e)
                lastException = e
                val errorState = errorHandler.handleNetworkError(e, context)
                if (attempt == 0) {
                    errorNotificationManager.showErrorNotification(errorState)
                }
                
            } catch (e: SocketTimeoutException) {
                Log.w(TAG, "Network timeout for operation: $operationName", e)
                lastException = e
                val errorState = errorHandler.handleNetworkError(e, context)
                if (attempt == 0) {
                    errorNotificationManager.showErrorNotification(errorState)
                }
                
            } catch (e: ConnectException) {
                Log.w(TAG, "Connection failed for operation: $operationName", e)
                lastException = e
                val errorState = errorHandler.handleNetworkError(e, context)
                if (attempt == 0) {
                    errorNotificationManager.showErrorNotification(errorState)
                }
                
            } catch (e: IOException) {
                Log.w(TAG, "IO error for network operation: $operationName", e)
                lastException = e
                val errorState = errorHandler.handleNetworkError(e, context)
                if (attempt == 0) {
                    errorNotificationManager.showErrorNotification(errorState)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error for network operation: $operationName", e)
                // For non-network exceptions, don't retry
                return Result.failure(e)
            }
            
            // Wait before retrying (exponential backoff)
            if (attempt < maxRetries - 1) {
                val delayMs = RETRY_DELAY_MS * (attempt + 1)
                Log.d(TAG, "Retrying network operation in ${delayMs}ms...")
                delay(delayMs)
            }
        }
        
        Log.e(TAG, "Network operation failed after $maxRetries attempts: $operationName")
        return Result.failure(lastException ?: Exception("Network operation failed"))
    }
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network availability", e)
            false
        }
    }
    
    /**
     * Check if network is metered (mobile data)
     */
    fun isNetworkMetered(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if network is metered", e)
            true // Assume metered if we can't determine
        }
    }
    
    /**
     * Get network type description
     */
    fun getNetworkTypeDescription(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return try {
            val network = connectivityManager.activeNetwork ?: return "No connection"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error getting network type", e)
            "Unknown"
        }
    }
    
    /**
     * Handle file download errors specifically
     */
    suspend fun handleFileDownloadError(
        error: Exception,
        fileName: String,
        fileUrl: String
    ): ErrorState {
        val context = ErrorContext(
            operation = "File Download",
            component = "NetworkClient",
            additionalInfo = mapOf(
                "fileName" to fileName,
                "fileUrl" to fileUrl,
                "networkType" to getNetworkTypeDescription(),
                "isMetered" to isNetworkMetered().toString()
            )
        )
        
        val errorState = when (error) {
            is UnknownHostException -> errorHandler.handleNetworkError(error, context)
            is SocketTimeoutException -> errorHandler.handleNetworkError(error, context)
            is ConnectException -> errorHandler.handleNetworkError(error, context)
            is IOException -> errorHandler.handleFileAccessError(error, context)
            else -> errorHandler.handleNetworkError(error, context)
        }
        
        errorNotificationManager.showErrorNotification(errorState)
        return errorState
    }
    
    /**
     * Handle VRM file upload errors
     */
    suspend fun handleVRMUploadError(
        error: Exception,
        fileName: String,
        fileSize: Long
    ): ErrorState {
        val context = ErrorContext(
            operation = "VRM Upload",
            component = "NetworkClient",
            additionalInfo = mapOf(
                "fileName" to fileName,
                "fileSize" to fileSize.toString(),
                "networkType" to getNetworkTypeDescription(),
                "isMetered" to isNetworkMetered().toString()
            )
        )
        
        val errorState = when (error) {
            is UnknownHostException -> errorHandler.handleNetworkError(error, context)
            is SocketTimeoutException -> {
                // For uploads, timeout might be due to large file size
                if (fileSize > 50 * 1024 * 1024) { // 50MB
                    ErrorState(
                        type = ErrorType.VRM_FILE_TOO_LARGE,
                        message = "File too large for upload",
                        userMessage = "The VRM file is too large to upload over this connection. Try a smaller file or use WiFi.",
                        severity = ErrorSeverity.MEDIUM,
                        isRecoverable = true,
                        suggestedActions = listOf(
                            ErrorAction.SELECT_SMALLER_FILE,
                            ErrorAction.CHECK_NETWORK,
                            ErrorAction.COMPRESS_FILE
                        ),
                        context = context
                    )
                } else {
                    errorHandler.handleNetworkError(error, context)
                }
            }
            else -> errorHandler.handleNetworkError(error, context)
        }
        
        errorNotificationManager.showErrorNotification(errorState)
        return errorState
    }
}