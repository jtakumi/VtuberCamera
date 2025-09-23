package com.example.vtubercamera.data.vrm

/**
 * Base class for VRM loading related errors
 */
sealed class VRMLoadingError : Exception() {
    
    /**
     * The specified VRM file was not found
     */
    object FileNotFound : VRMLoadingError() {
        override val message: String = "VRM file not found"
    }
    
    /**
     * The file is not a valid VRM format
     */
    object InvalidFormat : VRMLoadingError() {
        override val message: String = "Invalid VRM file format"
    }
    
    /**
     * The VRM file exceeds the maximum allowed size
     */
    object FileSizeExceeded : VRMLoadingError() {
        override val message: String = "VRM file size exceeds maximum limit"
    }
    
    /**
     * The VRM file data is corrupted or incomplete
     */
    object CorruptedData : VRMLoadingError() {
        override val message: String = "VRM file data is corrupted"
    }
    
    /**
     * The VRM file version is not supported
     */
    object UnsupportedVersion : VRMLoadingError() {
        override val message: String = "Unsupported VRM file version"
    }
    
    /**
     * Error occurred while parsing the VRM file
     * 
     * @param details Detailed error information
     */
    data class ParseError(val details: String) : VRMLoadingError() {
        override val message: String = "VRM parsing error: $details"
    }
    
    /**
     * Insufficient memory to load the VRM file
     */
    object InsufficientMemory : VRMLoadingError() {
        override val message: String = "Insufficient memory to load VRM file"
    }
    
    /**
     * Permission denied to access the VRM file
     */
    object PermissionDenied : VRMLoadingError() {
        override val message: String = "Permission denied to access VRM file"
    }
    
    /**
     * Network error while downloading VRM file
     */
    object NetworkError : VRMLoadingError() {
        override val message: String = "Network error while loading VRM file"
    }
    
    /**
     * Generic I/O error during VRM file operations
     * 
     * @param details Detailed error information
     */
    data class IOError(val details: String) : VRMLoadingError() {
        override val message: String = "I/O error: $details"
    }
}