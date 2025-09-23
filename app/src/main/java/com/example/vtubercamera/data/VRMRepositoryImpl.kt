package com.example.vtubercamera.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.data.vrm.VRMLoadingError
import com.example.vtubercamera.data.vrm.VRMModel
import com.example.vtubercamera.data.vrm.ValidationError
import com.example.vtubercamera.data.vrm.ValidationResult
import com.example.vtubercamera.data.vrm.VRMMetadata
import com.example.vtubercamera.data.vrm.VRMValidator
import com.example.vtubercamera.data.vrm.AvatarThumbnailGenerator
import com.example.vtubercamera.data.vrm.AvatarLibraryStats
import com.example.vtubercamera.data.vrm.CleanupResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of VRMRepository for managing VRM files and avatar library
 */
@Singleton
class VRMRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val thumbnailGenerator: AvatarThumbnailGenerator
) : VRMRepository {
    
    companion object {
        private const val TAG = "VRMRepositoryImpl"
        private const val AVATAR_LIBRARY_DIR = "avatar_library"
        private const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100MB
        private val SUPPORTED_VRM_VERSIONS = listOf("1.0", "0.0")
    }
    
    private val avatarLibraryDir: File by lazy {
        File(context.filesDir, AVATAR_LIBRARY_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val _avatarLibrary = MutableStateFlow<List<AvatarInfo>>(emptyList())
    
    init {
        // Load existing avatar library on initialization
        loadAvatarLibraryFromDisk()
    }
    
    override suspend fun loadVRMFromUri(uri: Uri): Result<VRMModel> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading VRM from URI: $uri")
            
            // First validate the file
            val validationResult = validateVRMFile(uri)
            if (validationResult.isInvalid()) {
                val errors = (validationResult as ValidationResult.Invalid).errors
                val criticalErrors = errors.filter { it.isCritical() }
                if (criticalErrors.isNotEmpty()) {
                    return@withContext Result.failure(
                        VRMLoadingError.ParseError(criticalErrors.first().message)
                    )
                }
            }
            
            // Read file content
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            val fileBytes = inputStream.use { it.readBytes() }
            
            // Parse VRM file
            val vrmModel = parseVRMFile(fileBytes, uri.toString())
            
            Log.d(TAG, "Successfully loaded VRM: ${vrmModel.name}")
            Result.success(vrmModel)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied accessing VRM file", e)
            Result.failure(VRMLoadingError.PermissionDenied)
        } catch (e: IOException) {
            Log.e(TAG, "IO error loading VRM file", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Unknown IO error"))
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory loading VRM file", e)
            Result.failure(VRMLoadingError.InsufficientMemory)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading VRM file", e)
            Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown error"))
        }
    }
    
    override suspend fun saveVRMToLibrary(vrmModel: VRMModel, name: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val avatarId = UUID.randomUUID().toString()
            val avatarName = name ?: vrmModel.name
            val fileName = "${avatarId}.vrm"
            val avatarFile = File(avatarLibraryDir, fileName)
            
            // Save VRM file to library directory
            FileOutputStream(avatarFile).use { output ->
                output.write(vrmModel.meshData)
            }
            
            // Generate thumbnail
            val thumbnailPath = thumbnailGenerator.generateThumbnail(vrmModel, avatarId)
            
            // Create avatar info
            val avatarInfo = AvatarInfo(
                id = avatarId,
                name = avatarName,
                originalFileName = vrmModel.name,
                thumbnailPath = thumbnailPath ?: "",
                filePath = avatarFile.absolutePath,
                fileSize = vrmModel.meshData.size.toLong(),
                metadata = vrmModel.metadata,
                availableExpressions = vrmModel.getExpressionNames(),
                availablePoses = vrmModel.getPoseNames(),
                version = vrmModel.version,
                dateAdded = System.currentTimeMillis(),
                dateLastUsed = System.currentTimeMillis()
            )
            
            // Add to library
            val currentLibrary = _avatarLibrary.value.toMutableList()
            currentLibrary.add(avatarInfo)
            _avatarLibrary.value = currentLibrary
            
            // Save library metadata
            saveAvatarLibraryToDisk()
            
            Log.d(TAG, "Successfully saved avatar to library: $avatarName")
            Result.success(avatarId)
            
        } catch (e: IOException) {
            Log.e(TAG, "IO error saving VRM to library", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to save VRM"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving VRM to library", e)
            Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown error"))
        }
    }
    
    override fun getAvatarLibrary(): Flow<List<AvatarInfo>> = _avatarLibrary.asStateFlow()
    
    override suspend fun getAvatarById(avatarId: String): AvatarInfo? {
        return _avatarLibrary.value.find { it.id == avatarId }
    }
    
    override suspend fun loadAvatarFromLibrary(avatarId: String): Result<VRMModel> = withContext(Dispatchers.IO) {
        try {
            val avatarInfo = getAvatarById(avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            val avatarFile = File(avatarInfo.filePath)
            if (!avatarFile.exists()) {
                return@withContext Result.failure(VRMLoadingError.FileNotFound)
            }
            
            val fileBytes = FileInputStream(avatarFile).use { it.readBytes() }
            val vrmModel = parseVRMFile(fileBytes, avatarInfo.name)
            
            // Record usage
            recordAvatarUsage(avatarId)
            
            Result.success(vrmModel)
            
        } catch (e: IOException) {
            Log.e(TAG, "IO error loading avatar from library", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to load avatar"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading avatar from library", e)
            Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown error"))
        }
    }
    
    override suspend fun deleteAvatar(avatarId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val avatarInfo = getAvatarById(avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            // Delete file
            val avatarFile = File(avatarInfo.filePath)
            if (avatarFile.exists()) {
                avatarFile.delete()
            }
            
            // Delete thumbnail
            thumbnailGenerator.deleteThumbnail(avatarId)
            
            // Remove from library
            val currentLibrary = _avatarLibrary.value.toMutableList()
            currentLibrary.removeAll { it.id == avatarId }
            _avatarLibrary.value = currentLibrary
            
            // Save updated library
            saveAvatarLibraryToDisk()
            
            Log.d(TAG, "Successfully deleted avatar: ${avatarInfo.name}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting avatar", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to delete avatar"))
        }
    }
    
    override suspend fun updateAvatarInfo(avatarInfo: AvatarInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentLibrary = _avatarLibrary.value.toMutableList()
            val index = currentLibrary.indexOfFirst { it.id == avatarInfo.id }
            
            if (index == -1) {
                return@withContext Result.failure(VRMLoadingError.FileNotFound)
            }
            
            currentLibrary[index] = avatarInfo
            _avatarLibrary.value = currentLibrary
            
            saveAvatarLibraryToDisk()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating avatar info", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to update avatar"))
        }
    }
    
    override suspend fun validateVRMFile(uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        VRMValidator.validateVRMFile(context, uri)
    }    

    override suspend fun getLibrarySize(): Long = withContext(Dispatchers.IO) {
        _avatarLibrary.value.sumOf { it.fileSize }
    }
    
    override suspend fun clearCache(): Unit = withContext(Dispatchers.IO) {
        // Clear any cached VRM data
        // For now, this is a placeholder as we don't have explicit caching
        Log.d(TAG, "Cache cleared")
    }
    
    override suspend fun searchAvatars(query: String): List<AvatarInfo> {
        val lowercaseQuery = query.lowercase()
        return _avatarLibrary.value.filter { avatar ->
            avatar.name.lowercase().contains(lowercaseQuery) ||
            avatar.tags.any { it.lowercase().contains(lowercaseQuery) } ||
            avatar.originalFileName.lowercase().contains(lowercaseQuery)
        }
    }
    
    override suspend fun getRecentlyUsedAvatars(limit: Int): List<AvatarInfo> {
        return _avatarLibrary.value
            .sortedByDescending { it.dateLastUsed }
            .take(limit)
    }
    
    override suspend fun getFavoriteAvatars(): List<AvatarInfo> {
        return _avatarLibrary.value.filter { it.isFavorite }
    }
    
    override suspend fun recordAvatarUsage(avatarId: String) {
        val avatarInfo = getAvatarById(avatarId) ?: return
        val updatedInfo = avatarInfo.withUsage()
        updateAvatarInfo(updatedInfo)
    }
    
    /**
     * Rename an avatar
     * 
     * @param avatarId The ID of the avatar to rename
     * @param newName The new name for the avatar
     * @return Result indicating success or error
     */
    override suspend fun renameAvatar(avatarId: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val avatarInfo = getAvatarById(avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            val updatedInfo = avatarInfo.withName(newName.trim())
            updateAvatarInfo(updatedInfo)
            
            Log.d(TAG, "Successfully renamed avatar $avatarId to: $newName")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming avatar", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to rename avatar"))
        }
    }
    
    /**
     * Toggle favorite status of an avatar
     * 
     * @param avatarId The ID of the avatar
     * @param isFavorite The new favorite status
     * @return Result indicating success or error
     */
    override suspend fun setAvatarFavorite(avatarId: String, isFavorite: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val avatarInfo = getAvatarById(avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            val updatedInfo = avatarInfo.withFavorite(isFavorite)
            updateAvatarInfo(updatedInfo)
            
            Log.d(TAG, "Successfully updated favorite status for avatar $avatarId: $isFavorite")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating avatar favorite status", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to update favorite status"))
        }
    }
    
    /**
     * Add tags to an avatar
     * 
     * @param avatarId The ID of the avatar
     * @param tags The tags to add
     * @return Result indicating success or error
     */
    override suspend fun addAvatarTags(avatarId: String, tags: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val avatarInfo = getAvatarById(avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            val cleanTags = tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val updatedInfo = avatarInfo.withTags(avatarInfo.tags + cleanTags)
            updateAvatarInfo(updatedInfo)
            
            Log.d(TAG, "Successfully added tags to avatar $avatarId: $cleanTags")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding avatar tags", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to add tags"))
        }
    }
    
    /**
     * Remove tags from an avatar
     * 
     * @param avatarId The ID of the avatar
     * @param tags The tags to remove
     * @return Result indicating success or error
     */
    override suspend fun removeAvatarTags(avatarId: String, tags: Set<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val avatarInfo = getAvatarById(avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            val tagsToRemove = tags.map { it.trim().lowercase() }.toSet()
            val updatedInfo = avatarInfo.withTags(avatarInfo.tags - tagsToRemove)
            updateAvatarInfo(updatedInfo)
            
            Log.d(TAG, "Successfully removed tags from avatar $avatarId: $tagsToRemove")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error removing avatar tags", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to remove tags"))
        }
    }
    
    /**
     * Regenerate thumbnail for an avatar
     * 
     * @param avatarId The ID of the avatar
     * @return Result containing the new thumbnail path or error
     */
    override suspend fun regenerateThumbnail(avatarId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val avatarInfo = getAvatarById(avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.FileNotFound)
            
            // Load the VRM model
            val vrmResult = loadAvatarFromLibrary(avatarId)
            if (vrmResult.isFailure) {
                return@withContext Result.failure(vrmResult.exceptionOrNull() ?: VRMLoadingError.ParseError("Failed to load VRM"))
            }
            
            val vrmModel = vrmResult.getOrThrow()
            
            // Delete old thumbnail
            thumbnailGenerator.deleteThumbnail(avatarId)
            
            // Generate new thumbnail
            val thumbnailPath = thumbnailGenerator.generateThumbnail(vrmModel, avatarId)
                ?: return@withContext Result.failure(VRMLoadingError.IOError("Failed to generate thumbnail"))
            
            // Update avatar info with new thumbnail path
            val updatedInfo = avatarInfo.copy(thumbnailPath = thumbnailPath)
            updateAvatarInfo(updatedInfo)
            
            Log.d(TAG, "Successfully regenerated thumbnail for avatar $avatarId")
            Result.success(thumbnailPath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error regenerating thumbnail", e)
            Result.failure(VRMLoadingError.IOError(e.message ?: "Failed to regenerate thumbnail"))
        }
    }
    
    /**
     * Get avatar library statistics
     * 
     * @return AvatarLibraryStats containing library information
     */
    override suspend fun getLibraryStatistics(): AvatarLibraryStats = withContext(Dispatchers.IO) {
        val avatars = _avatarLibrary.value
        val totalSize = avatars.sumOf { it.fileSize }
        val thumbnailSize = thumbnailGenerator.getTotalThumbnailSize()
        val favoriteCount = avatars.count { it.isFavorite }
        val recentlyUsedCount = avatars.count { it.isRecentlyUsed() }
        val newlyAddedCount = avatars.count { it.isNewlyAdded() }
        val withExpressionsCount = avatars.count { it.hasExpressions() }
        val withPosesCount = avatars.count { it.hasPoses() }
        
        val allTags = avatars.flatMap { it.tags }.distinct()
        val usageFrequencies = avatars.groupBy { it.getUsageFrequency() }
            .mapValues { it.value.size }
        
        AvatarLibraryStats(
            totalAvatars = avatars.size,
            totalFileSize = totalSize,
            totalThumbnailSize = thumbnailSize,
            favoriteCount = favoriteCount,
            recentlyUsedCount = recentlyUsedCount,
            newlyAddedCount = newlyAddedCount,
            withExpressionsCount = withExpressionsCount,
            withPosesCount = withPosesCount,
            availableTags = allTags,
            usageFrequencies = usageFrequencies
        )
    }
    
    /**
     * Cleanup library (remove orphaned files, validate avatars, etc.)
     * 
     * @return CleanupResult containing information about the cleanup operation
     */
    override suspend fun cleanupLibrary(): CleanupResult = withContext(Dispatchers.IO) {
        try {
            val avatars = _avatarLibrary.value.toMutableList()
            var removedAvatars = 0
            var fixedAvatars = 0
            
            // Check for missing files and remove invalid entries
            val validAvatars = avatars.filter { avatar ->
                val file = File(avatar.filePath)
                if (!file.exists()) {
                    Log.w(TAG, "Removing avatar with missing file: ${avatar.name}")
                    thumbnailGenerator.deleteThumbnail(avatar.id)
                    removedAvatars++
                    false
                } else {
                    true
                }
            }
            
            // Update library if any avatars were removed
            if (removedAvatars > 0) {
                _avatarLibrary.value = validAvatars
                saveAvatarLibraryToDisk()
            }
            
            // Clean up orphaned thumbnails
            val existingAvatarIds = validAvatars.map { it.id }.toSet()
            val orphanedThumbnails = thumbnailGenerator.cleanupOrphanedThumbnails(existingAvatarIds)
            
            // Fix missing thumbnails
            validAvatars.forEach { avatar ->
                if (avatar.thumbnailPath.isEmpty() || !thumbnailGenerator.hasThumbnail(avatar.id)) {
                    try {
                        val vrmResult = loadAvatarFromLibrary(avatar.id)
                        if (vrmResult.isSuccess) {
                            val thumbnailPath = thumbnailGenerator.generateThumbnail(vrmResult.getOrThrow(), avatar.id)
                            if (thumbnailPath != null) {
                                val updatedAvatar = avatar.copy(thumbnailPath = thumbnailPath)
                                updateAvatarInfo(updatedAvatar)
                                fixedAvatars++
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fix thumbnail for avatar: ${avatar.name}", e)
                    }
                }
            }
            
            CleanupResult(
                removedAvatars = removedAvatars,
                fixedThumbnails = fixedAvatars,
                orphanedThumbnails = orphanedThumbnails,
                success = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during library cleanup", e)
            CleanupResult(
                removedAvatars = 0,
                fixedThumbnails = 0,
                orphanedThumbnails = 0,
                success = false,
                error = e.message
            )
        }
    }
    
    /**
     * Parse VRM file bytes into VRMModel
     * This is a simplified implementation - in a real app, you'd use a proper VRM parser
     */
    private fun parseVRMFile(fileBytes: ByteArray, fileName: String): VRMModel {
        // This is a placeholder implementation
        // In a real implementation, you would:
        // 1. Parse the glTF/VRM structure
        // 2. Extract mesh data, textures, expressions, poses
        // 3. Validate VRM-specific extensions
        // 4. Create proper VRMModel with all data
        
        val id = UUID.randomUUID().toString()
        val name = fileName.substringBeforeLast(".")
        
        // Create minimal metadata
        val metadata = VRMMetadata(
            version = "1.0",
            author = "Unknown",
            contactInformation = "",
            reference = "",
            title = name,
            allowedUserName = VRMMetadata.AllowedUser.EVERYONE,
            violentUsage = VRMMetadata.Usage.DISALLOW,
            sexualUsage = VRMMetadata.Usage.DISALLOW,
            commercialUsage = VRMMetadata.Usage.DISALLOW,
            otherPermissionUrl = "",
            licenseName = VRMMetadata.LicenseType.OTHER,
            otherLicenseUrl = ""
        )
        
        return VRMModel(
            id = id,
            name = name,
            meshData = fileBytes,
            metadata = metadata,
            version = "1.0"
        )
    }
    
    /**
     * Load avatar library from disk storage
     */
    private fun loadAvatarLibraryFromDisk() {
        try {
            // In a real implementation, you would load avatar metadata from a database or JSON file
            // For now, we'll scan the avatar library directory
            val avatars = mutableListOf<AvatarInfo>()
            
            if (avatarLibraryDir.exists()) {
                avatarLibraryDir.listFiles { file -> file.extension == "vrm" }?.forEach { file ->
                    try {
                        val avatarInfo = AvatarInfo(
                            id = file.nameWithoutExtension,
                            name = file.nameWithoutExtension,
                            filePath = file.absolutePath,
                            fileSize = file.length(),
                            dateAdded = file.lastModified(),
                            dateLastUsed = file.lastModified()
                        )
                        avatars.add(avatarInfo)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to load avatar info for file: ${file.name}", e)
                    }
                }
            }
            
            _avatarLibrary.value = avatars
            Log.d(TAG, "Loaded ${avatars.size} avatars from library")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading avatar library from disk", e)
            _avatarLibrary.value = emptyList()
        }
    }
    
    /**
     * Save avatar library metadata to disk
     */
    private fun saveAvatarLibraryToDisk() {
        try {
            // In a real implementation, you would save avatar metadata to a database or JSON file
            // For now, this is a placeholder
            Log.d(TAG, "Avatar library metadata saved")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving avatar library to disk", e)
        }
    }
}