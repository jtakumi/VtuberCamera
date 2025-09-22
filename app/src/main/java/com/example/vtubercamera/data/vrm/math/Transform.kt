package com.example.vtubercamera.data.vrm.math

/**
 * Transform class representing position, rotation, and scale for VRM models
 */
data class Transform(
    val position: Vector3 = Vector3.ZERO,
    val rotation: Quaternion = Quaternion.IDENTITY,
    val scale: Vector3 = Vector3.ONE
) {
    companion object {
        /**
         * Identity transform (no translation, rotation, or scaling)
         */
        fun identity() = Transform(
            position = Vector3.ZERO,
            rotation = Quaternion.IDENTITY,
            scale = Vector3.ONE
        )

        /**
         * Create a transform with only position
         */
        fun translate(position: Vector3) = Transform(position = position)

        /**
         * Create a transform with only rotation
         */
        fun rotate(rotation: Quaternion) = Transform(rotation = rotation)

        /**
         * Create a transform with only scale
         */
        fun scale(scale: Vector3) = Transform(scale = scale)

        /**
         * Create a transform with uniform scale
         */
        fun scale(uniformScale: Float) = Transform(scale = Vector3(uniformScale, uniformScale, uniformScale))
    }

    /**
     * Apply this transform to a point
     */
    fun transformPoint(point: Vector3): Vector3 {
        // Scale first, then rotate, then translate
        val scaled = Vector3(point.x * scale.x, point.y * scale.y, point.z * scale.z)
        val rotated = rotation.rotateVector(scaled)
        return rotated + position
    }

    /**
     * Apply this transform to a direction (ignores translation and scale)
     */
    fun transformDirection(direction: Vector3): Vector3 {
        return rotation.rotateVector(direction)
    }

    /**
     * Combine this transform with another transform
     */
    fun combine(other: Transform): Transform {
        return Transform(
            position = transformPoint(other.position),
            rotation = rotation * other.rotation,
            scale = Vector3(scale.x * other.scale.x, scale.y * other.scale.y, scale.z * other.scale.z)
        )
    }

    /**
     * Get the inverse of this transform
     */
    fun inverse(): Transform {
        val invRotation = rotation.inverse()
        val invScale = Vector3(
            if (scale.x != 0f) 1f / scale.x else 0f,
            if (scale.y != 0f) 1f / scale.y else 0f,
            if (scale.z != 0f) 1f / scale.z else 0f
        )
        val invPosition = invRotation.rotateVector(position * -1f)
        val scaledInvPosition = Vector3(
            invPosition.x * invScale.x,
            invPosition.y * invScale.y,
            invPosition.z * invScale.z
        )

        return Transform(
            position = scaledInvPosition,
            rotation = invRotation,
            scale = invScale
        )
    }

    /**
     * Linear interpolation between two transforms
     */
    fun lerp(other: Transform, t: Float): Transform {
        return Transform(
            position = position.lerp(other.position, t),
            rotation = Quaternion.slerp(rotation, other.rotation, t),
            scale = scale.lerp(other.scale, t)
        )
    }

    /**
     * Create a copy with modified position
     */
    fun withPosition(newPosition: Vector3): Transform = copy(position = newPosition)

    /**
     * Create a copy with modified rotation
     */
    fun withRotation(newRotation: Quaternion): Transform = copy(rotation = newRotation)

    /**
     * Create a copy with modified scale
     */
    fun withScale(newScale: Vector3): Transform = copy(scale = newScale)

    /**
     * Create a copy with modified uniform scale
     */
    fun withScale(uniformScale: Float): Transform = copy(scale = Vector3(uniformScale, uniformScale, uniformScale))

    /**
     * Translate by a delta
     */
    fun translate(delta: Vector3): Transform = copy(position = position + delta)

    /**
     * Rotate by a delta rotation
     */
    fun rotate(deltaRotation: Quaternion): Transform = copy(rotation = rotation * deltaRotation)

    /**
     * Scale by a factor
     */
    fun scaleBy(factor: Vector3): Transform = copy(scale = Vector3(scale.x * factor.x, scale.y * factor.y, scale.z * factor.z))

    /**
     * Scale by a uniform factor
     */
    fun scaleBy(uniformFactor: Float): Transform = copy(scale = scale * uniformFactor)
}