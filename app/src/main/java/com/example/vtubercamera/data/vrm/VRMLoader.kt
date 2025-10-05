package com.example.vtubercamera.data.vrm

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * VRM loader that manages the loading process and provides progress updates
 */
class VRMLoader(private val context: Context) {

    private val parser = VRMParser(context)

    /**
     * Load VRM model from URI with progress updates
     */
    fun loadVRM(uri: Uri): Flow<VRMLoadingState> = flow {
        emit(VRMLoadingState.Loading(0f, "Starting VRM loading..."))

        try {
            emit(VRMLoadingState.Loading(0.1f, "Opening file..."))

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                emit(VRMLoadingState.Error(VRMLoadingError.FileNotFound))
                return@flow
            }

            emit(VRMLoadingState.Loading(0.2f, "Reading file data..."))

            val result = parser.parseVRMFromStream(inputStream)

            result.fold(
                onSuccess = { vrmModel ->
                    emit(VRMLoadingState.Loading(0.9f, "Finalizing model..."))
                    emit(VRMLoadingState.Success(vrmModel))
                },
                onFailure = { error ->
                    emit(
                        VRMLoadingState.Error(
                            error as? VRMLoadingError ?: VRMLoadingError.ParseError(
                                error.message ?: "Unknown error"
                            )
                        )
                    )
                }
            )
        } catch (_: SecurityException) {
            emit(VRMLoadingState.Error(VRMLoadingError.PermissionDenied))
        } catch (e: IOException) {
            emit(VRMLoadingState.Error(VRMLoadingError.IOError(e.message ?: "IO error")))
        } catch (e: Exception) {
            emit(VRMLoadingState.Error(VRMLoadingError.ParseError(e.message ?: "Unknown error")))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Load VRM model from file path
     */
    fun loadVRMFromFile(filePath: String): Flow<VRMLoadingState> = flow {
        emit(VRMLoadingState.Loading(0f, "Starting file loading..."))

        try {
            val file = File(filePath)
            if (!file.exists()) {
                emit(VRMLoadingState.Error(VRMLoadingError.FileNotFound))
                return@flow
            }

            emit(VRMLoadingState.Loading(0.1f, "Opening file..."))

            val inputStream = FileInputStream(file)
            val result = parser.parseVRMFromStream(inputStream)

            result.fold(
                onSuccess = { vrmModel ->
                    emit(VRMLoadingState.Loading(0.9f, "Finalizing model..."))
                    emit(VRMLoadingState.Success(vrmModel))
                },
                onFailure = { error ->
                    emit(
                        VRMLoadingState.Error(
                            error as? VRMLoadingError ?: VRMLoadingError.ParseError(
                                error.message ?: "Unknown error"
                            )
                        )
                    )
                }
            )
        } catch (e: Exception) {
            emit(VRMLoadingState.Error(VRMLoadingError.IOError(e.message ?: "File loading error")))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Load VRM model from URL
     */
    fun loadVRMFromUrl(url: String): Flow<VRMLoadingState> = flow {
        emit(VRMLoadingState.Loading(0f, "Starting download..."))

        try {
            val urlObj = URL(url)
            val connection = urlObj.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            connection.contentLengthLong
            val inputStream = connection.inputStream

            emit(VRMLoadingState.Loading(0.1f, "Downloading VRM file..."))

            // If we need to track download progress, we would wrap the input stream
            // For now, we'll parse directly
            val result = parser.parseVRMFromStream(inputStream)

            result.fold(
                onSuccess = { vrmModel ->
                    emit(VRMLoadingState.Loading(0.9f, "Finalizing model..."))
                    emit(VRMLoadingState.Success(vrmModel))
                },
                onFailure = { error ->
                    emit(
                        VRMLoadingState.Error(
                            error as? VRMLoadingError ?: VRMLoadingError.ParseError(
                                error.message ?: "Unknown error"
                            )
                        )
                    )
                }
            )
        } catch (_: IOException) {
            emit(VRMLoadingState.Error(VRMLoadingError.NetworkError))
        } catch (e: Exception) {
            emit(VRMLoadingState.Error(VRMLoadingError.ParseError(e.message ?: "Download error")))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Load VRM model from byte array
     */
    suspend fun loadVRMFromBytes(data: ByteArray): VRMLoadingState = withContext(Dispatchers.IO) {
        try {
            val inputStream = data.inputStream()
            val result = parser.parseVRMFromStream(inputStream)

            result.fold(
                onSuccess = { vrmModel -> VRMLoadingState.Success(vrmModel) },
                onFailure = { error ->
                    VRMLoadingState.Error(
                        error as? VRMLoadingError ?: VRMLoadingError.ParseError(
                            error.message ?: "Unknown error"
                        )
                    )
                }
            )
        } catch (e: Exception) {
            VRMLoadingState.Error(
                VRMLoadingError.ParseError(
                    e.message ?: "Byte array parsing error"
                )
            )
        }
    }

    /**
     * Validate VRM file without full parsing
     */
    suspend fun validateVRM(uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ValidationResult.Invalid(
                    listOf(
                        ValidationError.fileNotFound(
                            uri.toString()
                        )
                    )
                )

            val headerBytes = ByteArray(12)
            val bytesRead = inputStream.read(headerBytes)
            inputStream.close()

            if (bytesRead < 12) {
                return@withContext ValidationResult.Invalid(
                    listOf(
                        ValidationError.critical(
                            ValidationError.ErrorType.CORRUPTED_DATA,
                            "File too small",
                            "File has less than 12 bytes"
                        )
                    )
                )
            }

            // Check glTF magic number
            val magic = String(headerBytes.sliceArray(0..3), Charsets.UTF_8)
            if (magic != "glTF") {
                return@withContext ValidationResult.Invalid(
                    listOf(
                        ValidationError.invalidFormat("Not a valid glTF/VRM file. Magic: $magic")
                    )
                )
            }

            ValidationResult.Valid
        } catch (e: Exception) {
            ValidationResult.Invalid(
                listOf(
                    ValidationError.unknownError("Validation error: ${e.message}")
                )
            )
        }
    }

    /**
     * Get VRM file information without full parsing
     */
    suspend fun getVRMInfo(uri: Uri): VRMFileInfo? = withContext(Dispatchers.IO) {
        try {
            val inputStream =
                context.contentResolver.openInputStream(uri) ?: return@withContext null
            val fileSize = inputStream.available().toLong()
            inputStream.close()

            // Get basic file info
            VRMFileInfo(
                fileName = getFileName(uri),
                fileSize = fileSize,
                isValid = validateVRM(uri) is ValidationResult.Valid,
                lastModified = System.currentTimeMillis() // Placeholder
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Get file name from URI
     */
    private fun getFileName(uri: Uri): String {
        return uri.lastPathSegment ?: "unknown.vrm"
    }

    /**
     * Check if device has enough memory to load VRM
     */
    fun canLoadVRM(estimatedSize: Long): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = maxMemory - usedMemory

        // Require at least 3x the file size in available memory
        return availableMemory > estimatedSize * 3
    }

    /**
     * Get memory usage statistics
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val availableMemory = maxMemory - usedMemory

        return MemoryStats(
            maxMemory = maxMemory,
            usedMemory = usedMemory,
            availableMemory = availableMemory,
            usagePercentage = (usedMemory.toFloat() / maxMemory * 100).toInt()
        )
    }
}

/**
 * Represents the loading state of a VRM model
 */
sealed class VRMLoadingState {
    object Idle : VRMLoadingState()
    data class Loading(val progress: Float, val message: String) : VRMLoadingState()
    data class Success(val vrmModel: VRMModel) : VRMLoadingState()
    data class Error(val error: VRMLoadingError) : VRMLoadingState()
}

/**
 * VRM file information
 */
data class VRMFileInfo(
    val fileName: String,
    val fileSize: Long,
    val isValid: Boolean,
    val lastModified: Long
) {
    /**
     * Get formatted file size
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)}MB"
            else -> "${fileSize / (1024 * 1024 * 1024)}GB"
        }
    }
}

/**
 * Memory usage statistics
 */
data class MemoryStats(
    val maxMemory: Long,
    val usedMemory: Long,
    val availableMemory: Long,
    val usagePercentage: Int
) {
    /**
     * Check if memory usage is critical
     */
    fun isCritical(): Boolean = usagePercentage > 85

    /**
     * Check if memory usage is high
     */
    fun isHigh(): Boolean = usagePercentage > 70

    /**
     * Get formatted memory usage
     */
    fun getFormattedUsage(): String {
        return "${formatBytes(usedMemory)} / ${formatBytes(maxMemory)} (${usagePercentage}%)"
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
}