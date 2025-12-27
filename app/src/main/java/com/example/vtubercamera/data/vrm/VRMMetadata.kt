package com.example.vtubercamera.data.vrm

/**
 * VRM metadata containing information about the VRM model
 */
data class VRMMetadata(
    val title: String = "",
    val version: String = "",
    val author: String = "",
    val contactInformation: String = "",
    val reference: String = "",
    val texture: String = "",
    val allowedUserName: AllowedUser = AllowedUser.ONLY_AUTHOR,
    val violentUsage: Usage = Usage.DISALLOW,
    val sexualUsage: Usage = Usage.DISALLOW,
    val commercialUsage: Usage = Usage.DISALLOW,
    val otherPermissionUrl: String = "",
    val licenseName: LicenseType = LicenseType.REDISTRIBUTION_PROHIBITED,
    val otherLicenseUrl: String = ""
) {
    /**
     * Allowed user types for the VRM model
     */
    enum class AllowedUser {
        ONLY_AUTHOR,
        EXPLICITLY_LICENSED_PERSON,
        EVERYONE
    }

    /**
     * Usage permission types
     */
    enum class Usage {
        DISALLOW,
        ALLOW
    }

    /**
     * License types for VRM models
     */
    enum class LicenseType {
        REDISTRIBUTION_PROHIBITED,
        CC0,
        CC_BY,
        CC_BY_NC,
        CC_BY_SA,
        CC_BY_NC_SA,
        CC_BY_ND,
        CC_BY_NC_ND,
        OTHER
    }

    /**
     * Check if the model allows commercial usage
     */
    fun isCommercialUsageAllowed(): Boolean = commercialUsage == Usage.ALLOW

    /**
     * Check if the model allows violent usage
     */
    fun isViolentUsageAllowed(): Boolean = violentUsage == Usage.ALLOW

    /**
     * Check if the model allows sexual usage
     */
    fun isSexualUsageAllowed(): Boolean = sexualUsage == Usage.ALLOW

    /**
     * Check if the model can be used by everyone
     */
    fun isPubliclyUsable(): Boolean = allowedUserName == AllowedUser.EVERYONE

    /**
     * Get a human-readable license description
     */
    fun getLicenseDescription(): String = when (licenseName) {
        LicenseType.REDISTRIBUTION_PROHIBITED -> "Redistribution Prohibited"
        LicenseType.CC0 -> "CC0 - Public Domain"
        LicenseType.CC_BY -> "CC BY - Attribution"
        LicenseType.CC_BY_NC -> "CC BY-NC - Attribution Non-Commercial"
        LicenseType.CC_BY_SA -> "CC BY-SA - Attribution Share-Alike"
        LicenseType.CC_BY_NC_SA -> "CC BY-NC-SA - Attribution Non-Commercial Share-Alike"
        LicenseType.CC_BY_ND -> "CC BY-ND - Attribution No-Derivatives"
        LicenseType.CC_BY_NC_ND -> "CC BY-NC-ND - Attribution Non-Commercial No-Derivatives"
        LicenseType.OTHER -> "Other License"
    }
}