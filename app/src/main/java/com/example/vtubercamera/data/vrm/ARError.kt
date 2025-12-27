package com.example.vtubercamera.data.vrm

/**
 * Base class for AR-related errors
 */
sealed class ARError : Exception() {
    
    /**
     * The device does not support ARCore
     */
    object ARCoreNotSupported : ARError() {
        private fun readResolve(): Any = ARCoreNotSupported
        override val message: String = "ARCore is not supported on this device"
    }
    
    /**
     * ARCore is not installed on the device
     */
    object ARCoreNotInstalled : ARError() {
        private fun readResolve(): Any = ARCoreNotInstalled
        override val message: String = "ARCore is not installed. Please install ARCore from Google Play Store"
    }
    
    /**
     * Camera permission was denied by the user
     */
    object CameraPermissionDenied : ARError() {
        private fun readResolve(): Any = CameraPermissionDenied
        override val message: String = "Camera permission is required for AR functionality"
    }
    
    /**
     * Failed to initialize AR session
     */
    object SessionInitializationFailed : ARError() {
        private fun readResolve(): Any = SessionInitializationFailed
        override val message: String = "Failed to initialize AR session"
    }
    
    /**
     * AR tracking was lost and cannot be recovered
     */
    object TrackingLost : ARError() {
        private fun readResolve(): Any = TrackingLost
        override val message: String = "AR tracking lost. Please move the device slowly"
    }
    
    /**
     * Error occurred during AR rendering
     * 
     * @param details Detailed error information
     */
    data class RenderingError(val details: String) : ARError() {
        override val message: String = "AR rendering error: $details"
    }
    
    /**
     * ARCore service is outdated and needs to be updated
     */
    object ARCoreOutdated : ARError() {
        private fun readResolve(): Any = ARCoreOutdated
        override val message: String = "ARCore service is outdated. Please update ARCore"
    }
    
    /**
     * Device does not have sufficient resources for AR
     */
    object InsufficientResources : ARError() {
        private fun readResolve(): Any = InsufficientResources
        override val message: String = "Device does not have sufficient resources for AR"
    }
    
    /**
     * AR session was interrupted by another app
     */
    object SessionInterrupted : ARError() {
        private fun readResolve(): Any = SessionInterrupted
        override val message: String = "AR session was interrupted"
    }
    
    /**
     * Camera is being used by another application
     */
    object CameraInUse : ARError() {
        private fun readResolve(): Any = CameraInUse
        override val message: String = "Camera is being used by another application"
    }
    
    /**
     * Generic AR configuration error
     * 
     * @param details Detailed error information
     */
    data class ConfigurationError(val details: String) : ARError() {
        override val message: String = "AR configuration error: $details"
    }
    
    /**
     * Device is not compatible with AR features
     * 
     * @param details Detailed error information
     */
    data class UnsupportedDevice(val details: String) : ARError() {
        override val message: String = "Unsupported device: $details"
    }
    
    /**
     * AR session error
     * 
     * @param details Detailed error information
     */
    data class SessionError(val details: String) : ARError() {
        override val message: String = "Session error: $details"
    }
    
    /**
     * AR tracking error
     * 
     * @param details Detailed error information
     */
    data class TrackingError(val details: String) : ARError() {
        override val message: String = "Tracking error: $details"
    }
    
    /**
     * Avatar loading or processing error
     * 
     * @param details Detailed error information
     */
    data class AvatarError(val details: String) : ARError() {
        override val message: String = "Avatar error: $details"
    }
}