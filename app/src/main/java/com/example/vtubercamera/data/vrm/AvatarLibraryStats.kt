package com.example.vtubercamera.data.vrm

/**
 * Statistics about the avatar library
 */
data class AvatarLibraryStats(
    val totalAvatars: Int,
    val totalFileSize: Long,
    val totalThumbnailSize: Long,
    val favoriteCount: Int,
    val recentlyUsedCount: Int,
    val newlyAddedCount: Int,
    val withExpressionsCount: Int,
    val withPosesCount: Int,
    val availableTags: List<String>,
    val usageFrequencies: Map<AvatarInfo.UsageFrequency, Int>
) {
    /**
     * Get formatted total file size
     */
    fun getFormattedTotalSize(): String {
        return formatBytes(totalFileSize)
    }
    
    /**
     * Get formatted thumbnail size
     */
    fun getFormattedThumbnailSize(): String {
        return formatBytes(totalThumbnailSize)
    }
    
    /**
     * Get formatted combined size
     */
    fun getFormattedCombinedSize(): String {
        return formatBytes(totalFileSize + totalThumbnailSize)
    }
    
    /**
     * Get average file size
     */
    fun getAverageFileSize(): Long {
        return if (totalAvatars > 0) totalFileSize / totalAvatars else 0L
    }
    
    /**
     * Get formatted average file size
     */
    fun getFormattedAverageFileSize(): String {
        return formatBytes(getAverageFileSize())
    }
    
    /**
     * Get percentage of avatars with expressions
     */
    fun getExpressionsPercentage(): Float {
        return if (totalAvatars > 0) (withExpressionsCount.toFloat() / totalAvatars) * 100f else 0f
    }
    
    /**
     * Get percentage of avatars with poses
     */
    fun getPosesPercentage(): Float {
        return if (totalAvatars > 0) (withPosesCount.toFloat() / totalAvatars) * 100f else 0f
    }
    
    /**
     * Get percentage of favorite avatars
     */
    fun getFavoritesPercentage(): Float {
        return if (totalAvatars > 0) (favoriteCount.toFloat() / totalAvatars) * 100f else 0f
    }
    
    /**
     * Get percentage of recently used avatars
     */
    fun getRecentlyUsedPercentage(): Float {
        return if (totalAvatars > 0) (recentlyUsedCount.toFloat() / totalAvatars) * 100f else 0f
    }
    
    /**
     * Get most common tags (top 10)
     */
    fun getTopTags(limit: Int = 10): List<String> {
        return availableTags.take(limit)
    }
    
    /**
     * Check if library is empty
     */
    fun isEmpty(): Boolean = totalAvatars == 0
    
    /**
     * Check if library is large (more than 50 avatars)
     */
    fun isLarge(): Boolean = totalAvatars > 50
    
    /**
     * Check if library uses significant storage (more than 500MB)
     */
    fun usesSignificantStorage(): Boolean = (totalFileSize + totalThumbnailSize) > 500 * 1024 * 1024
    
    /**
     * Get usage distribution summary
     */
    fun getUsageDistributionSummary(): String {
        val neverUsed = usageFrequencies[AvatarInfo.UsageFrequency.NEVER_USED] ?: 0
        val rarelyUsed = usageFrequencies[AvatarInfo.UsageFrequency.RARELY_USED] ?: 0
        val frequentlyUsed = usageFrequencies[AvatarInfo.UsageFrequency.FREQUENTLY_USED] ?: 0
        val heavilyUsed = usageFrequencies[AvatarInfo.UsageFrequency.HEAVILY_USED] ?: 0
        
        return when {
            heavilyUsed > 0 -> "Active library with heavily used avatars"
            frequentlyUsed > totalAvatars / 2 -> "Well-utilized library"
            neverUsed > totalAvatars / 2 -> "Many unused avatars"
            else -> "Mixed usage patterns"
        }
    }
    
    /**
     * Get library health score (0-100)
     */
    fun getHealthScore(): Int {
        var score = 100
        
        // Deduct points for unused avatars
        val neverUsed = usageFrequencies[AvatarInfo.UsageFrequency.NEVER_USED] ?: 0
        if (totalAvatars > 0) {
            val unusedPercentage = (neverUsed.toFloat() / totalAvatars) * 100
            score -= (unusedPercentage * 0.3).toInt()
        }
        
        // Deduct points for missing thumbnails
        val withThumbnails = totalAvatars // Assuming all have thumbnails if we got here
        if (totalAvatars > 0 && withThumbnails < totalAvatars) {
            val missingThumbnailsPercentage = ((totalAvatars - withThumbnails).toFloat() / totalAvatars) * 100
            score -= (missingThumbnailsPercentage * 0.2).toInt()
        }
        
        // Bonus points for good organization (tags, favorites)
        if (availableTags.isNotEmpty()) score += 5
        if (favoriteCount > 0) score += 5
        
        return maxOf(0, minOf(100, score))
    }
    
    /**
     * Get recommendations for library improvement
     */
    fun getRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        val neverUsed = usageFrequencies[AvatarInfo.UsageFrequency.NEVER_USED] ?: 0
        if (neverUsed > totalAvatars / 3) {
            recommendations.add("Consider removing unused avatars to free up space")
        }
        
        if (favoriteCount == 0 && totalAvatars > 5) {
            recommendations.add("Mark frequently used avatars as favorites for quick access")
        }
        
        if (availableTags.isEmpty() && totalAvatars > 10) {
            recommendations.add("Add tags to organize your avatars better")
        }
        
        if (usesSignificantStorage()) {
            recommendations.add("Library uses significant storage - consider cleanup")
        }
        
        if (withExpressionsCount < totalAvatars / 2) {
            recommendations.add("Look for avatars with more expressions for better variety")
        }
        
        return recommendations
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
 * Result of a library cleanup operation
 */
data class CleanupResult(
    val removedAvatars: Int,
    val fixedThumbnails: Int,
    val orphanedThumbnails: Int,
    val success: Boolean,
    val error: String? = null
) {
    /**
     * Check if any cleanup was performed
     */
    fun hasChanges(): Boolean = removedAvatars > 0 || fixedThumbnails > 0 || orphanedThumbnails > 0
    
    /**
     * Get summary of cleanup operation
     */
    fun getSummary(): String {
        return if (success) {
            if (hasChanges()) {
                val changes = mutableListOf<String>()
                if (removedAvatars > 0) changes.add("$removedAvatars avatars removed")
                if (fixedThumbnails > 0) changes.add("$fixedThumbnails thumbnails fixed")
                if (orphanedThumbnails > 0) changes.add("$orphanedThumbnails orphaned thumbnails cleaned")
                "Cleanup completed: ${changes.joinToString(", ")}"
            } else {
                "Library is already clean - no changes needed"
            }
        } else {
            "Cleanup failed: ${error ?: "Unknown error"}"
        }
    }
}