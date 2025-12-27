package com.example.vtubercamera.data.vrm

import android.net.Uri
import android.util.Log
import com.example.vtubercamera.data.VRMRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level manager for avatar library operations
 * Provides convenient methods for managing avatars with additional business logic
 */
@Singleton
class AvatarLibraryManager @Inject constructor(
    private val vrmRepository: VRMRepository
) {
    
    companion object {
        private const val TAG = "AvatarLibraryManager"
        private const val MAX_LIBRARY_SIZE = 1024 * 1024 * 1024L // 1GB
        private const val MAX_AVATAR_COUNT = 100
    }
    
    /**
     * Get all avatars in the library
     */
    fun getAvatarLibrary(): Flow<List<AvatarInfo>> = vrmRepository.getAvatarLibrary()
    
    /**
     * Get avatars sorted by different criteria
     */
    fun getAvatarsSortedBy(sortBy: AvatarSortBy): Flow<List<AvatarInfo>> {
        return vrmRepository.getAvatarLibrary().map { avatars ->
            when (sortBy) {
                AvatarSortBy.NAME_ASC -> avatars.sortedBy { it.name.lowercase() }
                AvatarSortBy.NAME_DESC -> avatars.sortedByDescending { it.name.lowercase() }
                AvatarSortBy.DATE_ADDED_ASC -> avatars.sortedBy { it.dateAdded }
                AvatarSortBy.DATE_ADDED_DESC -> avatars.sortedByDescending { it.dateAdded }
                AvatarSortBy.DATE_USED_ASC -> avatars.sortedBy { it.dateLastUsed }
                AvatarSortBy.DATE_USED_DESC -> avatars.sortedByDescending { it.dateLastUsed }
                AvatarSortBy.SIZE_ASC -> avatars.sortedBy { it.fileSize }
                AvatarSortBy.SIZE_DESC -> avatars.sortedByDescending { it.fileSize }
                AvatarSortBy.USAGE_COUNT_ASC -> avatars.sortedBy { it.usageCount }
                AvatarSortBy.USAGE_COUNT_DESC -> avatars.sortedByDescending { it.usageCount }
                AvatarSortBy.FAVORITES_FIRST -> avatars.sortedWith(
                    compareByDescending<AvatarInfo> { it.isFavorite }
                        .thenBy { it.name.lowercase() }
                )
            }
        }
    }
    
    /**
     * Get filtered avatars
     */
    fun getFilteredAvatars(filter: AvatarFilter): Flow<List<AvatarInfo>> {
        return vrmRepository.getAvatarLibrary().map { avatars ->
            avatars.filter { avatar ->
                when (filter) {
                    AvatarFilter.ALL -> true
                    AvatarFilter.FAVORITES -> avatar.isFavorite
                    AvatarFilter.RECENTLY_USED -> avatar.isRecentlyUsed()
                    AvatarFilter.NEWLY_ADDED -> avatar.isNewlyAdded()
                    AvatarFilter.WITH_EXPRESSIONS -> avatar.hasExpressions()
                    AvatarFilter.WITH_POSES -> avatar.hasPoses()
                    AvatarFilter.NEVER_USED -> avatar.usageCount == 0
                    is AvatarFilter.BY_TAG -> avatar.hasTag(filter.tag)
                    is AvatarFilter.BY_SEARCH -> {
                        val query = filter.query.lowercase()
                        avatar.name.lowercase().contains(query) ||
                        avatar.tags.any { it.lowercase().contains(query) } ||
                        avatar.originalFileName.lowercase().contains(query)
                    }
                }
            }
        }
    }
    
    /**
     * Import a new avatar from URI
     */
    suspend fun importAvatar(uri: Uri, customName: String? = null): Result<String> {
        try {
            Log.d(TAG, "Importing avatar from URI: $uri")
            
            // Check library limits
            val stats = vrmRepository.getLibraryStatistics()
            if (stats.totalAvatars >= MAX_AVATAR_COUNT) {
                return Result.failure(VRMLoadingError.ParseError("Avatar library is full (max $MAX_AVATAR_COUNT avatars)"))
            }
            
            if (stats.totalFileSize >= MAX_LIBRARY_SIZE) {
                return Result.failure(VRMLoadingError.ParseError("Avatar library storage is full (max ${formatBytes(MAX_LIBRARY_SIZE)})"))
            }
            
            // Load VRM from URI
            val vrmResult = vrmRepository.loadVRMFromUri(uri)
            if (vrmResult.isFailure) {
                return Result.failure(vrmResult.exceptionOrNull() ?: VRMLoadingError.ParseError("Failed to load VRM"))
            }
            
            val vrmModel = vrmResult.getOrThrow()
            
            // Check for duplicate names
            val finalName = if (customName != null) {
                generateUniqueName(customName)
            } else {
                generateUniqueName(vrmModel.name)
            }
            
            // Save to library
            val saveResult = vrmRepository.saveVRMToLibrary(vrmModel, finalName)
            if (saveResult.isFailure) {
                return Result.failure(saveResult.exceptionOrNull() ?: VRMLoadingError.IOError("Failed to save avatar"))
            }
            
            val avatarId = saveResult.getOrThrow()
            Log.d(TAG, "Successfully imported avatar: $finalName (ID: $avatarId)")
            
            return Result.success(avatarId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing avatar", e)
            return Result.failure(VRMLoadingError.ParseError(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Bulk import multiple avatars
     */
    suspend fun importAvatars(uris: List<Uri>): BulkImportResult {
        val results = mutableListOf<ImportResult>()
        var successCount = 0
        var failureCount = 0
        
        for (uri in uris) {
            try {
                val result = importAvatar(uri)
                if (result.isSuccess) {
                    results.add(ImportResult.Success(uri, result.getOrThrow()))
                    successCount++
                } else {
                    results.add(ImportResult.Failure(uri, result.exceptionOrNull()?.message ?: "Unknown error"))
                    failureCount++
                }
            } catch (e: Exception) {
                results.add(ImportResult.Failure(uri, e.message ?: "Unknown error"))
                failureCount++
            }
        }
        
        return BulkImportResult(
            results = results,
            successCount = successCount,
            failureCount = failureCount,
            totalCount = uris.size
        )
    }
    
    /**
     * Delete multiple avatars
     */
    suspend fun deleteAvatars(avatarIds: List<String>): BulkDeleteResult {
        val results = mutableListOf<DeleteResult>()
        var successCount = 0
        var failureCount = 0
        
        for (avatarId in avatarIds) {
            try {
                val result = vrmRepository.deleteAvatar(avatarId)
                if (result.isSuccess) {
                    results.add(DeleteResult.Success(avatarId))
                    successCount++
                } else {
                    results.add(DeleteResult.Failure(avatarId, result.exceptionOrNull()?.message ?: "Unknown error"))
                    failureCount++
                }
            } catch (e: Exception) {
                results.add(DeleteResult.Failure(avatarId, e.message ?: "Unknown error"))
                failureCount++
            }
        }
        
        return BulkDeleteResult(
            results = results,
            successCount = successCount,
            failureCount = failureCount,
            totalCount = avatarIds.size
        )
    }
    
    /**
     * Get avatar recommendations based on usage patterns
     */
    suspend fun getAvatarRecommendations(): AvatarRecommendations {
        val avatars = vrmRepository.getAvatarLibrary().map { it }.toString() // Get current value
        val stats = vrmRepository.getLibraryStatistics()
        
        val mostUsed = vrmRepository.getRecentlyUsedAvatars(5)
        val favorites = vrmRepository.getFavoriteAvatars()
        val withExpressions = avatars // Placeholder - would filter avatars with expressions
        val newAvatars = avatars // Placeholder - would filter newly added avatars
        
        return AvatarRecommendations(
            mostUsed = mostUsed,
            favorites = favorites,
            withExpressions = emptyList(), // Placeholder
            newAvatars = emptyList(), // Placeholder
            suggestions = stats.getRecommendations()
        )
    }
    
    /**
     * Validate library health and get suggestions
     */
    suspend fun validateLibraryHealth(): LibraryHealthReport {
        val stats = vrmRepository.getLibraryStatistics()
        val cleanupResult = vrmRepository.cleanupLibrary()
        
        val issues = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        // Check for common issues
        if (stats.totalFileSize > MAX_LIBRARY_SIZE * 0.8) {
            issues.add("Library is approaching storage limit")
            suggestions.add("Consider removing unused avatars or cleaning up duplicates")
        }
        
        if (stats.totalAvatars > MAX_AVATAR_COUNT * 0.8) {
            issues.add("Library is approaching avatar count limit")
            suggestions.add("Consider organizing avatars with tags and favorites")
        }
        
        val neverUsed = stats.usageFrequencies[AvatarInfo.UsageFrequency.NEVER_USED] ?: 0
        if (neverUsed > stats.totalAvatars / 2) {
            issues.add("Many avatars are never used")
            suggestions.add("Review and remove avatars you don't need")
        }
        
        suggestions.addAll(stats.getRecommendations())
        
        return LibraryHealthReport(
            healthScore = stats.getHealthScore(),
            issues = issues,
            suggestions = suggestions.distinct(),
            cleanupResult = cleanupResult,
            stats = stats
        )
    }
    
    /**
     * Generate a unique name for an avatar
     */
    private suspend fun generateUniqueName(baseName: String): String {
        val existingNames = vrmRepository.getAvatarLibrary().map { avatars ->
            avatars.map { it.name.lowercase() }.toSet()
        }.toString() // Placeholder - would get actual names
        
        var uniqueName = baseName
        var counter = 1
        
        // This is a simplified implementation
        // In a real app, you'd check against existing names properly
        while (existingNames.contains(uniqueName.lowercase())) {
            uniqueName = "$baseName ($counter)"
            counter++
        }
        
        return uniqueName
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
}

/**
 * Sorting options for avatars
 */
enum class AvatarSortBy {
    NAME_ASC,
    NAME_DESC,
    DATE_ADDED_ASC,
    DATE_ADDED_DESC,
    DATE_USED_ASC,
    DATE_USED_DESC,
    SIZE_ASC,
    SIZE_DESC,
    USAGE_COUNT_ASC,
    USAGE_COUNT_DESC,
    FAVORITES_FIRST
}

/**
 * Filtering options for avatars
 */
sealed class AvatarFilter {
    object ALL : AvatarFilter()
    object FAVORITES : AvatarFilter()
    object RECENTLY_USED : AvatarFilter()
    object NEWLY_ADDED : AvatarFilter()
    object WITH_EXPRESSIONS : AvatarFilter()
    object WITH_POSES : AvatarFilter()
    object NEVER_USED : AvatarFilter()
    data class BY_TAG(val tag: String) : AvatarFilter()
    data class BY_SEARCH(val query: String) : AvatarFilter()
}

/**
 * Result of importing a single avatar
 */
sealed class ImportResult {
    data class Success(val uri: Uri, val avatarId: String) : ImportResult()
    data class Failure(val uri: Uri, val error: String) : ImportResult()
}

/**
 * Result of bulk import operation
 */
data class BulkImportResult(
    val results: List<ImportResult>,
    val successCount: Int,
    val failureCount: Int,
    val totalCount: Int
) {
    fun isAllSuccessful(): Boolean = failureCount == 0
    fun hasFailures(): Boolean = failureCount > 0
    
    fun getSummary(): String {
        return "Imported $successCount/$totalCount avatars successfully"
    }
}

/**
 * Result of deleting a single avatar
 */
sealed class DeleteResult {
    data class Success(val avatarId: String) : DeleteResult()
    data class Failure(val avatarId: String, val error: String) : DeleteResult()
}

/**
 * Result of bulk delete operation
 */
data class BulkDeleteResult(
    val results: List<DeleteResult>,
    val successCount: Int,
    val failureCount: Int,
    val totalCount: Int
) {
    fun isAllSuccessful(): Boolean = failureCount == 0
    fun hasFailures(): Boolean = failureCount > 0
    
    fun getSummary(): String {
        return "Deleted $successCount/$totalCount avatars successfully"
    }
}

/**
 * Avatar recommendations based on usage patterns
 */
data class AvatarRecommendations(
    val mostUsed: List<AvatarInfo>,
    val favorites: List<AvatarInfo>,
    val withExpressions: List<AvatarInfo>,
    val newAvatars: List<AvatarInfo>,
    val suggestions: List<String>
)

/**
 * Library health report
 */
data class LibraryHealthReport(
    val healthScore: Int,
    val issues: List<String>,
    val suggestions: List<String>,
    val cleanupResult: CleanupResult,
    val stats: AvatarLibraryStats
) {
    fun isHealthy(): Boolean = healthScore >= 80 && issues.isEmpty()
    fun needsAttention(): Boolean = healthScore < 60 || issues.isNotEmpty()
    
    fun getSummary(): String {
        return when {
            isHealthy() -> "Library is in good health (Score: $healthScore/100)"
            needsAttention() -> "Library needs attention (Score: $healthScore/100)"
            else -> "Library health is fair (Score: $healthScore/100)"
        }
    }
}