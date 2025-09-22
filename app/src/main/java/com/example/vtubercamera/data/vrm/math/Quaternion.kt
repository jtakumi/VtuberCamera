package com.example.vtubercamera.data.vrm.math

import kotlin.math.*

/**
 * Quaternion class for 3D rotations in VRM models
 */
data class Quaternion(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 1f
) {
    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        /**
         * Create a quaternion from Euler angles (in radians)
         */
        fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quaternion {
            val cy = cos(yaw * 0.5f)
            val sy = sin(yaw * 0.5f)
            val cp = cos(pitch * 0.5f)
            val sp = sin(pitch * 0.5f)
            val cr = cos(roll * 0.5f)
            val sr = sin(roll * 0.5f)

            return Quaternion(
                x = sr * cp * cy - cr * sp * sy,
                y = cr * sp * cy + sr * cp * sy,
                z = cr * cp * sy - sr * sp * cy,
                w = cr * cp * cy + sr * sp * sy
            )
        }

        /**
         * Create a quaternion from axis-angle representation
         */
        fun fromAxisAngle(axis: Vector3, angle: Float): Quaternion {
            val normalizedAxis = axis.normalized()
            val halfAngle = angle * 0.5f
            val sinHalfAngle = sin(halfAngle)
            
            return Quaternion(
                x = normalizedAxis.x * sinHalfAngle,
                y = normalizedAxis.y * sinHalfAngle,
                z = normalizedAxis.z * sinHalfAngle,
                w = cos(halfAngle)
            )
        }

        /**
         * Spherical linear interpolation between two quaternions
         */
        fun slerp(a: Quaternion, b: Quaternion, t: Float): Quaternion {
            val clampedT = t.coerceIn(0f, 1f)
            
            var dot = a.dot(b)
            var q2 = b
            
            // If dot product is negative, slerp won't take the shorter path
            if (dot < 0f) {
                q2 = Quaternion(-b.x, -b.y, -b.z, -b.w)
                dot = -dot
            }
            
            // If the quaternions are very close, use linear interpolation
            if (dot > 0.9995f) {
                return Quaternion(
                    x = a.x + (q2.x - a.x) * clampedT,
                    y = a.y + (q2.y - a.y) * clampedT,
                    z = a.z + (q2.z - a.z) * clampedT,
                    w = a.w + (q2.w - a.w) * clampedT
                ).normalized()
            }
            
            val theta0 = acos(dot.coerceIn(-1f, 1f))
            val theta = theta0 * clampedT
            val sinTheta = sin(theta)
            val sinTheta0 = sin(theta0)
            
            val s0 = cos(theta) - dot * sinTheta / sinTheta0
            val s1 = sinTheta / sinTheta0
            
            return Quaternion(
                x = s0 * a.x + s1 * q2.x,
                y = s0 * a.y + s1 * q2.y,
                z = s0 * a.z + s1 * q2.z,
                w = s0 * a.w + s1 * q2.w
            )
        }
    }

    /**
     * Calculate the magnitude of the quaternion
     */
    fun magnitude(): Float = sqrt(x * x + y * y + z * z + w * w)

    /**
     * Return a normalized version of this quaternion
     */
    fun normalized(): Quaternion {
        val mag = magnitude()
        return if (mag > 0f) Quaternion(x / mag, y / mag, z / mag, w / mag) else IDENTITY
    }

    /**
     * Return the conjugate of this quaternion
     */
    fun conjugate(): Quaternion = Quaternion(-x, -y, -z, w)

    /**
     * Return the inverse of this quaternion
     */
    fun inverse(): Quaternion {
        val mag = magnitude()
        val conjugated = conjugate()
        val magSquared = mag * mag
        return if (magSquared > 0f) {
            Quaternion(
                conjugated.x / magSquared,
                conjugated.y / magSquared,
                conjugated.z / magSquared,
                conjugated.w / magSquared
            )
        } else IDENTITY
    }

    /**
     * Quaternion multiplication
     */
    operator fun times(other: Quaternion): Quaternion = Quaternion(
        x = w * other.x + x * other.w + y * other.z - z * other.y,
        y = w * other.y - x * other.z + y * other.w + z * other.x,
        z = w * other.z + x * other.y - y * other.x + z * other.w,
        w = w * other.w - x * other.x - y * other.y - z * other.z
    )

    /**
     * Dot product
     */
    fun dot(other: Quaternion): Float = x * other.x + y * other.y + z * other.z + w * other.w

    /**
     * Rotate a vector by this quaternion
     */
    fun rotateVector(vector: Vector3): Vector3 {
        val qVector = Vector3(x, y, z)
        val uv = qVector.cross(vector)
        val uuv = qVector.cross(uv)
        
        return vector + (uv * (2f * w)) + (uuv * 2f)
    }

    /**
     * Convert to Euler angles (in radians)
     */
    fun toEuler(): Vector3 {
        // Roll (x-axis rotation)
        val sinRCosP = 2 * (w * x + y * z)
        val cosRCosP = 1 - 2 * (x * x + y * y)
        val roll = atan2(sinRCosP, cosRCosP)

        // Pitch (y-axis rotation)
        val sinP = 2 * (w * y - z * x)
        val pitch = if (abs(sinP) >= 1) {
            if (sinP >= 0) PI.toFloat() / 2 else -PI.toFloat() / 2
        } else {
            asin(sinP)
        }

        // Yaw (z-axis rotation)
        val sinYCosP = 2 * (w * z + x * y)
        val cosYCosP = 1 - 2 * (y * y + z * z)
        val yaw = atan2(sinYCosP, cosYCosP)

        return Vector3(pitch, yaw, roll)
    }
}