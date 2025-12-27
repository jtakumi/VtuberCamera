package com.example.vtubercamera.data.vrm

/**
 * Represents the current state of an AR session
 * 
 * @param isInitialized Whether the AR session has been successfully initialized
 * @param trackingState Current tracking state of the AR session
 * @param lightEstimate Environmental light estimation data
 * @param planeDetection Whether plane detection is enabled
 * @param environmentalHDR Whether environmental HDR is enabled
 */
data class ARSessionState(
    val isInitialized: Boolean = false,
    val trackingState: TrackingState = TrackingState.STOPPED,
    val lightEstimate: LightEstimate? = null,
    val planeDetection: Boolean = true,
    val environmentalHDR: Boolean = false
)

/**
 * Represents the tracking state of an AR session
 */
enum class TrackingState {
    /** AR session is not tracking */
    STOPPED,
    
    /** AR session is paused */
    PAUSED,
    
    /** AR session is tracking normally */
    TRACKING,
    
    /** AR session tracking is temporarily lost */
    TRACKING_LOST
}

/**
 * Represents environmental light estimation data
 * 
 * @param pixelIntensity The average pixel intensity of the camera image
 * @param colorCorrection Color correction values for red, green, blue, and alpha channels
 * @param timestamp Timestamp when the light estimate was captured
 */
data class LightEstimate(
    val pixelIntensity: Float,
    val colorCorrection: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LightEstimate

        if (pixelIntensity != other.pixelIntensity) return false
        if (!colorCorrection.contentEquals(other.colorCorrection)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pixelIntensity.hashCode()
        result = 31 * result + colorCorrection.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}