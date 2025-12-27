package com.example.vtubercamera.data.vrm

import android.util.Log
import com.example.vtubercamera.data.vrm.math.Quaternion
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.math.Vector3
import com.google.ar.core.Frame
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles AR camera configuration and pose tracking
 * Manages the integration between ARCore camera and 3D rendering camera
 */
@Singleton
class ARCameraConfig @Inject constructor() {

    companion object {
        private const val TAG = "ARCameraConfig"

        // Camera configuration constants
        private const val DEFAULT_NEAR_PLANE = 0.1f
        private const val DEFAULT_FAR_PLANE = 100.0f
        private const val DEFAULT_FOV = 45.0f
    }

    // Camera state
    private var isConfigured = false
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var nearPlane = DEFAULT_NEAR_PLANE
    private var farPlane = DEFAULT_FAR_PLANE

    // Current camera pose and projection
    private var currentCameraPose: Transform = Transform.identity()
    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)

    /**
     * Configure the AR camera with viewport dimensions
     */
    fun configure(
        width: Int,
        height: Int,
        near: Float = DEFAULT_NEAR_PLANE,
        far: Float = DEFAULT_FAR_PLANE
    ) {
        viewportWidth = width
        viewportHeight = height
        nearPlane = near
        farPlane = far

        Log.d(TAG, "Configuring AR camera: ${width}x${height}, near: $near, far: $far")

        // TODO: Configure Filament camera when available
        /*
        val aspectRatio = width.toFloat() / height.toFloat()
        camera.setProjection(
            Camera.Projection.PERSPECTIVE,
            DEFAULT_FOV.toDouble(),
            aspectRatio.toDouble(),
            near.toDouble(),
            far.toDouble()
        )
        */

        isConfigured = true
        Log.d(TAG, "AR camera configured successfully")
    }

    /**
     * Update camera with current AR frame data
     */
    fun updateWithFrame(frame: Frame) {
        if (!isConfigured) {
            Log.w(TAG, "Camera not configured, skipping frame update")
            return
        }

        try {
            val camera = frame.camera

            // Get camera pose from ARCore
            val pose = camera.pose
            updateCameraPose(pose)

            // Get projection matrix from ARCore
            camera.getProjectionMatrix(projectionMatrix, 0, nearPlane, farPlane)

            // Get view matrix from ARCore
            camera.getViewMatrix(viewMatrix, 0)

            // TODO: Apply matrices to Filament camera
            // applyCameraMatrices()

        } catch (e: Exception) {
            Log.e(TAG, "Error updating camera with frame", e)
        }
    }

    /**
     * Update camera pose from ARCore pose
     */
    private fun updateCameraPose(pose: com.google.ar.core.Pose) {
        try {
            // Extract position
            val translation = pose.translation
            val position = Vector3(translation[0], translation[1], translation[2])

            // Extract rotation
            val rotation = pose.rotationQuaternion
            val quaternion = Quaternion(rotation[0], rotation[1], rotation[2], rotation[3])

            // Update current camera pose
            currentCameraPose = Transform(
                position = position,
                rotation = quaternion,
                scale = Vector3.ONE
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error updating camera pose", e)
        }
    }

    /**
     * Apply camera matrices to the 3D renderer
     */
    private fun applyCameraMatrices() {
        // TODO: Apply projection and view matrices to Filament camera
        /*
        camera.setCustomProjection(
            projectionMatrix,
            nearPlane.toDouble(),
            farPlane.toDouble()
        )
        
        // Apply view matrix (inverse of camera transform)
        val inverseViewMatrix = FloatArray(16)
        Matrix.invertM(inverseViewMatrix, 0, viewMatrix, 0)
        camera.setModelMatrix(inverseViewMatrix)
        */
    }

    /**
     * Get current camera pose in world space
     */
    fun getCurrentCameraPose(): Transform = currentCameraPose

    /**
     * Get current projection matrix
     */
    fun getProjectionMatrix(): FloatArray = projectionMatrix.clone()

    /**
     * Get current view matrix
     */
    fun getViewMatrix(): FloatArray = viewMatrix.clone()

    /**
     * Convert screen coordinates to world ray
     * Useful for touch interaction with 3D objects
     */
    fun screenToWorldRay(screenX: Float, screenY: Float): Ray? {
        if (!isConfigured) return null

        return try {
            // Normalize screen coordinates to [-1, 1]
            val normalizedX = (2.0f * screenX / viewportWidth) - 1.0f
            val normalizedY = 1.0f - (2.0f * screenY / viewportHeight)

            // TODO: Implement proper ray casting when Filament is available
            // For now, return a placeholder ray from camera position
            Ray(
                origin = currentCameraPose.position,
                direction = Vector3(normalizedX, normalizedY, -1.0f).normalized()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error converting screen to world ray", e)
            null
        }
    }

    /**
     * Project world position to screen coordinates
     */
    fun worldToScreen(worldPosition: Vector3): Vector3? {
        if (!isConfigured) return null

        return try {
            // TODO: Implement proper world to screen projection
            // This would use the view and projection matrices

            // Placeholder implementation
            Vector3(
                worldPosition.x * viewportWidth * 0.5f + viewportWidth * 0.5f,
                worldPosition.y * viewportHeight * 0.5f + viewportHeight * 0.5f,
                worldPosition.z
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error converting world to screen", e)
            null
        }
    }

    /**
     * Get camera field of view
     */
    fun getFieldOfView(): Float = DEFAULT_FOV

    /**
     * Get camera aspect ratio
     */
    fun getAspectRatio(): Float {
        return if (viewportHeight > 0) {
            viewportWidth.toFloat() / viewportHeight.toFloat()
        } else {
            1.0f
        }
    }

    /**
     * Check if camera is configured
     */
    fun isConfigured(): Boolean = isConfigured

    /**
     * Reset camera configuration
     */
    fun reset() {
        isConfigured = false
        currentCameraPose = Transform.identity()
        projectionMatrix.fill(0.0f)
        viewMatrix.fill(0.0f)

        Log.d(TAG, "AR camera configuration reset")
    }

    /**
     * Get viewport dimensions
     */
    fun getViewportSize(): Pair<Int, Int> = Pair(viewportWidth, viewportHeight)

    /**
     * Get near and far plane distances
     */
    fun getClippingPlanes(): Pair<Float, Float> = Pair(nearPlane, farPlane)
}

/**
 * Represents a ray in 3D space for raycasting operations
 */
data class Ray(
    val origin: Vector3,
    val direction: Vector3
) {
    /**
     * Get a point along the ray at the given distance
     */
    fun getPoint(distance: Float): Vector3 {
        return origin + (direction * distance)
    }
}