package com.example.vtubercamera.data.vrm.math

import kotlin.math.sqrt

/**
 * 3D vector class for VRM model transformations
 */
data class Vector3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    companion object {
        val ZERO = Vector3(0f, 0f, 0f)
        val ONE = Vector3(1f, 1f, 1f)
        val UP = Vector3(0f, 1f, 0f)
        val RIGHT = Vector3(1f, 0f, 0f)
        val FORWARD = Vector3(0f, 0f, 1f)
    }

    /**
     * Calculate the magnitude (length) of the vector
     */
    fun magnitude(): Float = sqrt(x * x + y * y + z * z)

    /**
     * Calculate the squared magnitude (more efficient when comparing distances)
     */
    fun sqrMagnitude(): Float = x * x + y * y + z * z

    /**
     * Return a normalized version of this vector
     */
    fun normalized(): Vector3 {
        val mag = magnitude()
        return if (mag > 0f) Vector3(x / mag, y / mag, z / mag) else ZERO
    }

    /**
     * Vector addition
     */
    operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)

    /**
     * Vector subtraction
     */
    operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)

    /**
     * Scalar multiplication
     */
    operator fun times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)

    /**
     * Scalar division
     */
    operator fun div(scalar: Float): Vector3 = Vector3(x / scalar, y / scalar, z / scalar)

    /**
     * Dot product
     */
    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    /**
     * Cross product
     */
    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    /**
     * Linear interpolation between two vectors
     */
    fun lerp(other: Vector3, t: Float): Vector3 {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector3(
            x + (other.x - x) * clampedT,
            y + (other.y - y) * clampedT,
            z + (other.z - z) * clampedT
        )
    }
}