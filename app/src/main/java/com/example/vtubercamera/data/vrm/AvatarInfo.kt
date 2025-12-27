package com.example.vtubercamera.data.vrm

import java.util.Date

/**
 * Information about a VRM avatar in the library
 */
data class AvatarInfo(
    val id: String,
    val name: String,
    val originalFileName: String = "",
    val thumbnailPath: String = "",
    val filePath: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val dateLastUsed: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val usageCount: Int = 0,
    val isFavorite: Boolean = false,
    val tags: Set<String> = emptySet(),
    val metadata: VRMMetadata? = null,
    val availableExpressions: List<String> = emptyList(),
    val availablePoses: List<String> = emptyList(),
    val version: String = "",
    val isValid: Boolean = true,
    val lastValidationDate: Long = System.currentTimeMillis()
) {
    /**
     * Get file size in a human-readable format
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)}MB"
            else -> "${fileSize / (1024 * 1024 * 1024)}GB"
        }
    }

    /**
     * Get formatted date added
     */
    fun getFormattedDateAdded(): String {
        return java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            .format(Date(dateAdded))
    }

    /**
     * Get formatted date last used
     */
    fun getFormattedDateLastUsed(): String {
        return java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
            .format(Date(dateLastUsed))
    }

    /**
     * Check if the avatar has been used recently (within last 7 days)
     */
    fun isRecentlyUsed(): Boolean {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return dateLastUsed > sevenDaysAgo
    }

    /**
     * Check if the avatar is newly added (within last 24 hours)
     */
    fun isNewlyAdded(): Boolean {
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return dateAdded > oneDayAgo
    }

    /**
     * Check if the avatar has expressions available
     */
    fun hasExpressions(): Boolean = availableExpressions.isNotEmpty()

    /**
     * Check if the avatar has poses available
     */
    fun hasPoses(): Boolean = availablePoses.isNotEmpty()

    /**
     * Check if the avatar has a specific expression
     */
    fun hasExpression(expressionName: String): Boolean = 
        availableExpressions.contains(expressionName)

    /**
     * Check if the avatar has a specific pose
     */
    fun hasPose(poseName: String): Boolean = 
        availablePoses.contains(poseName)

    /**
     * Check if the avatar has a specific tag
     */
    fun hasTag(tag: String): Boolean = tags.contains(tag)

    /**
     * Get usage frequency category
     */
    fun getUsageFrequency(): UsageFrequency = when {
        usageCount == 0 -> UsageFrequency.NEVER_USED
        usageCount <= 5 -> UsageFrequency.RARELY_USED
        usageCount <= 20 -> UsageFrequency.OCCASIONALLY_USED
        usageCount <= 50 -> UsageFrequency.FREQUENTLY_USED
        else -> UsageFrequency.HEAVILY_USED
    }

    /**
     * Create a copy with updated usage information
     */
    fun withUsage(): AvatarInfo = copy(
        usageCount = usageCount + 1,
        dateLastUsed = System.currentTimeMillis()
    )

    /**
     * Create a copy with updated name
     */
    fun withName(newName: String): AvatarInfo = copy(name = newName)

    /**
     * Create a copy with updated favorite status
     */
    fun withFavorite(favorite: Boolean): AvatarInfo = copy(isFavorite = favorite)

    /**
     * Create a copy with added tag
     */
    fun withTag(tag: String): AvatarInfo = copy(tags = tags + tag)

    /**
     * Create a copy with removed tag
     */
    fun withoutTag(tag: String): AvatarInfo = copy(tags = tags - tag)

    /**
     * Create a copy with updated tags
     */
    fun withTags(newTags: Set<String>): AvatarInfo = copy(tags = newTags)

    /**
     * Create a copy with updated validation status
     */
    fun withValidation(valid: Boolean): AvatarInfo = copy(
        isValid = valid,
        lastValidationDate = System.currentTimeMillis()
    )

    /**
     * Check if validation is outdated (older than 30 days)
     */
    fun isValidationOutdated(): Boolean {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
        return lastValidationDate < thirtyDaysAgo
    }

    /**
     * Get a summary of avatar capabilities
     */
    fun getCapabilitySummary(): String {
        val capabilities = mutableListOf<String>()
        
        if (hasExpressions()) {
            capabilities.add("${availableExpressions.size} expressions")
        }
        
        if (hasPoses()) {
            capabilities.add("${availablePoses.size} poses")
        }
        
        if (metadata?.isCommercialUsageAllowed() == true) {
            capabilities.add("Commercial use OK")
        }
        
        return if (capabilities.isNotEmpty()) {
            capabilities.joinToString(", ")
        } else {
            "Basic avatar"
        }
    }

    companion object {
        /**
         * Create a new AvatarInfo with generated ID
         */
        fun create(
            name: String,
            filePath: String,
            fileSize: Long,
            metadata: VRMMetadata? = null
        ): AvatarInfo = AvatarInfo(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            filePath = filePath,
            fileSize = fileSize,
            metadata = metadata,
            dateAdded = System.currentTimeMillis(),
            dateLastUsed = System.currentTimeMillis()
        )

        /**
         * Create from VRM file analysis
         */
        fun fromVRMFile(
            name: String,
            filePath: String,
            fileSize: Long,
            metadata: VRMMetadata,
            expressions: List<String>,
            poses: List<String>
        ): AvatarInfo = AvatarInfo(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            filePath = filePath,
            fileSize = fileSize,
            metadata = metadata,
            availableExpressions = expressions,
            availablePoses = poses,
            version = metadata.version,
            dateAdded = System.currentTimeMillis(),
            dateLastUsed = System.currentTimeMillis()
        )
    }

    /**
     * Usage frequency categories
     */
    enum class UsageFrequency {
        NEVER_USED,
        RARELY_USED,
        OCCASIONALLY_USED,
        FREQUENTLY_USED,
        HEAVILY_USED
    }
}