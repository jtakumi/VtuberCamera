package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.data.vrm.math.Quaternion

/**
 * Represents the current state of the AR camera
 * 
 * @param position Current 3D position of the camera in world space
 * @param rotation Current rotation of the camera as a quaternion
 * @param fieldOfView Camera field of view in degrees
 * @param nearClippingPlane Near clipping plane distance
 * @param farClippingPlane Far clipping plane distance
 * @param viewMatrix Camera view matrix (4x4 matrix represented as FloatArray)
 * @param projectionMatrix Camera projection matrix (4x4 matrix represented as FloatArray)
 * @param isTracking Whether the camera is currently tracking in AR space
 * @param trackingQuality Quality of the current tracking
 * @param lastUpdateTimestamp Timestamp of the last camera state update
 */
data class ARCameraState(
    val position: Vector3 = Vector3.ZERO,
    val rotation: Quaternion = Quaternion.IDENTITY,
    val fieldOfView: Float = 60.0f,
    val nearClippingPlane: Float = 0.1f,
    val farClippingPlane: Float = 100.0f,
    val viewMatrix: FloatArray = IDENTITY_MATRIX_4X4,
    val projectionMatrix: FloatArray = IDENTITY_MATRIX_4X4,
    val isTracking: Boolean = false,
    val trackingQuality: TrackingQuality = TrackingQuality.UNKNOWN,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        val IDENTITY_MATRIX_4X4 = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
        
        /**
         * Creates a default AR camera state
         */
        fun default(): ARCameraState = ARCameraState()
        
        /**
         * Creates an AR camera state with specified position and rotation
         */
        fun create(position: Vector3, rotation: Quaternion): ARCameraState = ARCameraState(
            position = position,
            rotation = rotation,
            isTracking = true,
            trackingQuality = TrackingQuality.GOOD
        )
    }
    
    /**
     * Returns the forward direction vector of the camera
     */
    val forward: Vector3
        get() = rotation.rotateVector(Vector3.FORWARD)
    
    /**
     * Returns the right direction vector of the camera
     */
    val right: Vector3
        get() = rotation.rotateVector(Vector3.RIGHT)
    
    /**
     * Returns the up direction vector of the camera
     */
    val up: Vector3
        get() = rotation.rotateVector(Vector3.UP)
    
    /**
     * Returns true if the camera tracking is reliable
     */
    val isTrackingReliable: Boolean
        get() = isTracking && (trackingQuality == TrackingQuality.GOOD || trackingQuality == TrackingQuality.EXCELLENT)
    
    /**
     * Returns the camera's transform matrix combining position and rotation
     */
    fun getTransformMatrix(): FloatArray {
        val matrix = FloatArray(16)
        
        // Convert quaternion to rotation matrix
        val xx = rotation.x * rotation.x
        val yy = rotation.y * rotation.y
        val zz = rotation.z * rotation.z
        val xy = rotation.x * rotation.y
        val xz = rotation.x * rotation.z
        val yz = rotation.y * rotation.z
        val wx = rotation.w * rotation.x
        val wy = rotation.w * rotation.y
        val wz = rotation.w * rotation.z
        
        // Build transform matrix
        matrix[0] = 1 - 2 * (yy + zz)
        matrix[1] = 2 * (xy + wz)
        matrix[2] = 2 * (xz - wy)
        matrix[3] = 0f
        
        matrix[4] = 2 * (xy - wz)
        matrix[5] = 1 - 2 * (xx + zz)
        matrix[6] = 2 * (yz + wx)
        matrix[7] = 0f
        
        matrix[8] = 2 * (xz + wy)
        matrix[9] = 2 * (yz - wx)
        matrix[10] = 1 - 2 * (xx + yy)
        matrix[11] = 0f
        
        matrix[12] = position.x
        matrix[13] = position.y
        matrix[14] = position.z
        matrix[15] = 1f
        
        return matrix
    }
    
    /**
     * Creates a copy of this camera state with updated position
     */
    fun withPosition(newPosition: Vector3): ARCameraState = copy(
        position = newPosition,
        lastUpdateTimestamp = System.currentTimeMillis()
    )
    
    /**
     * Creates a copy of this camera state with updated rotation
     */
    fun withRotation(newRotation: Quaternion): ARCameraState = copy(
        rotation = newRotation,
        lastUpdateTimestamp = System.currentTimeMillis()
    )
    
    /**
     * Creates a copy of this camera state with updated tracking status
     */
    fun withTracking(tracking: Boolean, quality: TrackingQuality = trackingQuality): ARCameraState = copy(
        isTracking = tracking,
        trackingQuality = quality,
        lastUpdateTimestamp = System.currentTimeMillis()
    )
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ARCameraState

        if (position != other.position) return false
        if (rotation != other.rotation) return false
        if (fieldOfView != other.fieldOfView) return false
        if (nearClippingPlane != other.nearClippingPlane) return false
        if (farClippingPlane != other.farClippingPlane) return false
        if (!viewMatrix.contentEquals(other.viewMatrix)) return false
        if (!projectionMatrix.contentEquals(other.projectionMatrix)) return false
        if (isTracking != other.isTracking) return false
        if (trackingQuality != other.trackingQuality) return false
        if (lastUpdateTimestamp != other.lastUpdateTimestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + rotation.hashCode()
        result = 31 * result + fieldOfView.hashCode()
        result = 31 * result + nearClippingPlane.hashCode()
        result = 31 * result + farClippingPlane.hashCode()
        result = 31 * result + viewMatrix.contentHashCode()
        result = 31 * result + projectionMatrix.contentHashCode()
        result = 31 * result + isTracking.hashCode()
        result = 31 * result + trackingQuality.hashCode()
        result = 31 * result + lastUpdateTimestamp.hashCode()
        return result
    }
}

/**
 * Represents the quality of AR camera tracking
 */
enum class TrackingQuality {
    /**
     * Tracking quality is unknown or not initialized
     */
    UNKNOWN,
    
    /**
     * Poor tracking quality - may cause jittery movement
     */
    POOR,
    
    /**
     * Adequate tracking quality - functional but not optimal
     */
    ADEQUATE,
    
    /**
     * Good tracking quality - stable and reliable
     */
    GOOD,
    
    /**
     * Excellent tracking quality - very stable and precise
     */
    EXCELLENT
}

/**
 * Represents camera movement constraints in AR space
 * 
 * @param canMove Whether the camera can move in 3D space
 * @param canRotate Whether the camera can rotate
 * @param movementBounds Optional bounding box for camera movement
 * @param rotationLimits Optional rotation limits in degrees
 */
data class ARCameraConstraints(
    val canMove: Boolean = true,
    val canRotate: Boolean = true,
    val movementBounds: ARBoundingBox? = null,
    val rotationLimits: ARRotationLimits? = null
) {
    companion object {
        /**
         * No constraints - camera can move and rotate freely
         */
        val FREE = ARCameraConstraints()
        
        /**
         * Camera is locked in position but can rotate
         */
        val ROTATION_ONLY = ARCameraConstraints(canMove = false)
        
        /**
         * Camera is completely locked
         */
        val LOCKED = ARCameraConstraints(canMove = false, canRotate = false)
    }
}

/**
 * Represents a 3D bounding box for camera movement constraints
 * 
 * @param min Minimum corner of the bounding box
 * @param max Maximum corner of the bounding box
 */
data class ARBoundingBox(
    val min: Vector3,
    val max: Vector3
) {
    /**
     * Returns the center point of the bounding box
     */
    val center: Vector3
        get() = Vector3(
            (min.x + max.x) * 0.5f,
            (min.y + max.y) * 0.5f,
            (min.z + max.z) * 0.5f
        )
    
    /**
     * Returns the size of the bounding box
     */
    val size: Vector3
        get() = max - min
    
    /**
     * Checks if a point is within this bounding box
     */
    fun contains(point: Vector3): Boolean {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z
    }
    
    /**
     * Clamps a point to be within this bounding box
     */
    fun clamp(point: Vector3): Vector3 = Vector3(
        point.x.coerceIn(min.x, max.x),
        point.y.coerceIn(min.y, max.y),
        point.z.coerceIn(min.z, max.z)
    )
}

/**
 * Represents rotation limits for the AR camera
 * 
 * @param minPitch Minimum pitch angle in degrees
 * @param maxPitch Maximum pitch angle in degrees
 * @param minYaw Minimum yaw angle in degrees
 * @param maxYaw Maximum yaw angle in degrees
 * @param minRoll Minimum roll angle in degrees
 * @param maxRoll Maximum roll angle in degrees
 */
data class ARRotationLimits(
    val minPitch: Float = -90f,
    val maxPitch: Float = 90f,
    val minYaw: Float = -180f,
    val maxYaw: Float = 180f,
    val minRoll: Float = -180f,
    val maxRoll: Float = 180f
) {
    companion object {
        /**
         * No rotation limits
         */
        val NONE = ARRotationLimits()
        
        /**
         * Typical FPS-style camera limits (no roll, limited pitch)
         */
        val FPS_STYLE = ARRotationLimits(
            minPitch = -89f,
            maxPitch = 89f,
            minRoll = 0f,
            maxRoll = 0f
        )
    }
    
    /**
     * Clamps Euler angles to be within these limits
     */
    fun clampEuler(euler: Vector3): Vector3 = Vector3(
        euler.x.coerceIn(minPitch, maxPitch),
        euler.y.coerceIn(minYaw, maxYaw),
        euler.z.coerceIn(minRoll, maxRoll)
    )
}