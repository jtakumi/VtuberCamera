package com.example.vtubercamera.data

import android.net.Uri
import com.example.vtubercamera.data.vrm.AvatarInfo
import com.example.vtubercamera.data.vrm.AvatarLibraryStats
import com.example.vtubercamera.data.vrm.CleanupResult
import com.example.vtubercamera.data.vrm.VRMModel
import com.example.vtubercamera.data.vrm.ValidationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for VRM file operations
 * Handles loading, validation, and management of VRM avatar files
 */
interface VRMRepository {
    
    /**
     * Load a VRM model from the given URI
     * 
     * @param uri The URI of the VRM file to load
     * @return Result containing the loaded VRMModel or error
     */
    suspend fun loadVRMFromUri(uri: Uri): Result<VRMModel>
    
    /**
     * Save a VRM model to the avatar library
     * 
     * @param vrmModel The VRM model to save
     * @param name Custom name for the avatar (optional)
     * @return Result containing the avatar ID or error
     */
    suspend fun saveVRMToLibrary(vrmModel: VRMModel, name: String? = null): Result<String>
    
    /**
     * Get all avatars in the library as a Flow
     * 
     * @return Flow of avatar list that updates when library changes
     */
    fun getAvatarLibrary(): Flow<List<AvatarInfo>>
    
    /**
     * Get a specific avatar by ID
     * 
     * @param avatarId The ID of the avatar to retrieve
     * @return The AvatarInfo if found, null otherwise
     */
    suspend fun getAvatarById(avatarId: String): AvatarInfo?
    
    /**
     * Load a VRM model from the library by avatar ID
     * 
     * @param avatarId The ID of the avatar to load
     * @return Result containing the loaded VRMModel or error
     */
    suspend fun loadAvatarFromLibrary(avatarId: String): Result<VRMModel>
    
    /**
     * Delete an avatar from the library
     * 
     * @param avatarId The ID of the avatar to delete
     * @return Result indicating success or error
     */
    suspend fun deleteAvatar(avatarId: String): Result<Unit>
    
    /**
     * Update avatar information (name, tags, etc.)
     * 
     * @param avatarInfo The updated avatar information
     * @return Result indicating success or error
     */
    suspend fun updateAvatarInfo(avatarInfo: AvatarInfo): Result<Unit>
    
    /**
     * Validate a VRM file without loading it completely
     * 
     * @param uri The URI of the VRM file to validate
     * @return ValidationResult indicating if the file is valid
     */
    suspend fun validateVRMFile(uri: Uri): ValidationResult
    
    /**
     * Get the total size of all avatars in the library
     * 
     * @return Total size in bytes
     */
    suspend fun getLibrarySize(): Long
    
    /**
     * Clear all cached VRM data
     */
    suspend fun clearCache()
    
    /**
     * Search avatars by name or tags
     * 
     * @param query Search query
     * @return List of matching avatars
     */
    suspend fun searchAvatars(query: String): List<AvatarInfo>
    
    /**
     * Get recently used avatars
     * 
     * @param limit Maximum number of avatars to return
     * @return List of recently used avatars
     */
    suspend fun getRecentlyUsedAvatars(limit: Int = 10): List<AvatarInfo>
    
    /**
     * Get favorite avatars
     * 
     * @return List of favorite avatars
     */
    suspend fun getFavoriteAvatars(): List<AvatarInfo>
    
    /**
     * Record avatar usage (updates usage count and last used date)
     * 
     * @param avatarId The ID of the avatar that was used
     */
    suspend fun recordAvatarUsage(avatarId: String)
    
    /**
     * Rename an avatar
     * 
     * @param avatarId The ID of the avatar to rename
     * @param newName The new name for the avatar
     * @return Result indicating success or error
     */
    suspend fun renameAvatar(avatarId: String, newName: String): Result<Unit>
    
    /**
     * Toggle favorite status of an avatar
     * 
     * @param avatarId The ID of the avatar
     * @param isFavorite The new favorite status
     * @return Result indicating success or error
     */
    suspend fun setAvatarFavorite(avatarId: String, isFavorite: Boolean): Result<Unit>
    
    /**
     * Add tags to an avatar
     * 
     * @param avatarId The ID of the avatar
     * @param tags The tags to add
     * @return Result indicating success or error
     */
    suspend fun addAvatarTags(avatarId: String, tags: Set<String>): Result<Unit>
    
    /**
     * Remove tags from an avatar
     * 
     * @param avatarId The ID of the avatar
     * @param tags The tags to remove
     * @return Result indicating success or error
     */
    suspend fun removeAvatarTags(avatarId: String, tags: Set<String>): Result<Unit>
    
    /**
     * Regenerate thumbnail for an avatar
     * 
     * @param avatarId The ID of the avatar
     * @return Result containing the new thumbnail path or error
     */
    suspend fun regenerateThumbnail(avatarId: String): Result<String>
    
    /**
     * Get avatar library statistics
     * 
     * @return AvatarLibraryStats containing library information
     */
    suspend fun getLibraryStatistics(): AvatarLibraryStats
    
    /**
     * Cleanup library (remove orphaned files, validate avatars, etc.)
     * 
     * @return CleanupResult containing information about the cleanup operation
     */
    suspend fun cleanupLibrary(): CleanupResult
}