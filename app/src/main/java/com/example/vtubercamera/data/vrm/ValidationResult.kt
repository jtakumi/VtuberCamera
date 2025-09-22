package com.example.vtubercamera.data.vrm

/**
 * Result of VRM file validation
 */
sealed class ValidationResult {
    /**
     * Validation passed successfully
     */
    object Valid : ValidationResult()

    /**
     * Validation failed with specific errors
     */
    data class Invalid(
        val errors: List<ValidationError>
    ) : ValidationResult() {
        
        /**
         * Check if the validation has any errors
         */
        fun hasErrors(): Boolean = errors.isNotEmpty()

        /**
         * Get the first error message
         */
        fun getFirstErrorMessage(): String? = errors.firstOrNull()?.message

        /**
         * Get all error messages
         */
        fun getAllErrorMessages(): List<String> = errors.map { it.message }

        /**
         * Check if validation has critical errors that prevent loading
         */
        fun hasCriticalErrors(): Boolean = errors.any { it.severity == ValidationError.Severity.CRITICAL }

        /**
         * Get only critical errors
         */
        fun getCriticalErrors(): List<ValidationError> = errors.filter { it.severity == ValidationError.Severity.CRITICAL }

        /**
         * Get only warning errors
         */
        fun getWarnings(): List<ValidationError> = errors.filter { it.severity == ValidationError.Severity.WARNING }
    }

    /**
     * Check if validation is successful
     */
    fun isValid(): Boolean = this is Valid

    /**
     * Check if validation failed
     */
    fun isInvalid(): Boolean = this is Invalid
}

/**
 * Individual validation error
 */
data class ValidationError(
    val type: ErrorType,
    val message: String,
    val severity: Severity = Severity.CRITICAL,
    val details: String? = null
) {
    /**
     * Types of validation errors
     */
    enum class ErrorType {
        FILE_NOT_FOUND,
        INVALID_FORMAT,
        FILE_SIZE_EXCEEDED,
        CORRUPTED_DATA,
        UNSUPPORTED_VERSION,
        MISSING_REQUIRED_DATA,
        INVALID_MESH_DATA,
        INVALID_TEXTURE_DATA,
        INVALID_METADATA,
        PERMISSION_DENIED,
        MEMORY_INSUFFICIENT,
        UNKNOWN_ERROR
    }

    /**
     * Severity levels for validation errors
     */
    enum class Severity {
        WARNING,    // Non-critical issues that don't prevent loading
        CRITICAL    // Critical issues that prevent loading
    }

    /**
     * Check if this is a critical error
     */
    fun isCritical(): Boolean = severity == Severity.CRITICAL

    /**
     * Check if this is a warning
     */
    fun isWarning(): Boolean = severity == Severity.WARNING

    companion object {
        /**
         * Create a critical error
         */
        fun critical(type: ErrorType, message: String, details: String? = null): ValidationError =
            ValidationError(type, message, Severity.CRITICAL, details)

        /**
         * Create a warning
         */
        fun warning(type: ErrorType, message: String, details: String? = null): ValidationError =
            ValidationError(type, message, Severity.WARNING, details)

        /**
         * Common validation errors
         */
        fun fileNotFound(path: String): ValidationError = critical(
            ErrorType.FILE_NOT_FOUND,
            "VRM file not found",
            "File path: $path"
        )

        fun invalidFormat(details: String? = null): ValidationError = critical(
            ErrorType.INVALID_FORMAT,
            "Invalid VRM file format",
            details
        )

        fun fileSizeExceeded(actualSize: Long, maxSize: Long): ValidationError = critical(
            ErrorType.FILE_SIZE_EXCEEDED,
            "File size exceeds maximum allowed size",
            "Actual: ${actualSize / 1024 / 1024}MB, Max: ${maxSize / 1024 / 1024}MB"
        )

        fun corruptedData(details: String? = null): ValidationError = critical(
            ErrorType.CORRUPTED_DATA,
            "VRM file data is corrupted",
            details
        )

        fun unsupportedVersion(version: String): ValidationError = critical(
            ErrorType.UNSUPPORTED_VERSION,
            "Unsupported VRM version",
            "Version: $version"
        )

        fun missingRequiredData(dataType: String): ValidationError = critical(
            ErrorType.MISSING_REQUIRED_DATA,
            "Missing required VRM data",
            "Missing: $dataType"
        )

        fun invalidMeshData(details: String? = null): ValidationError = critical(
            ErrorType.INVALID_MESH_DATA,
            "Invalid mesh data in VRM file",
            details
        )

        fun invalidTextureData(details: String? = null): ValidationError = warning(
            ErrorType.INVALID_TEXTURE_DATA,
            "Invalid texture data in VRM file",
            details
        )

        fun invalidMetadata(details: String? = null): ValidationError = warning(
            ErrorType.INVALID_METADATA,
            "Invalid metadata in VRM file",
            details
        )

        fun permissionDenied(details: String? = null): ValidationError = critical(
            ErrorType.PERMISSION_DENIED,
            "Permission denied to access VRM file",
            details
        )

        fun memoryInsufficient(requiredMemory: Long): ValidationError = critical(
            ErrorType.MEMORY_INSUFFICIENT,
            "Insufficient memory to load VRM file",
            "Required: ${requiredMemory / 1024 / 1024}MB"
        )

        fun unknownError(details: String? = null): ValidationError = critical(
            ErrorType.UNKNOWN_ERROR,
            "Unknown error occurred during VRM validation",
            details
        )
    }
}