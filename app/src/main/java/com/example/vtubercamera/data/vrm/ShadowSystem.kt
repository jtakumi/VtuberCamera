package com.example.vtubercamera.data.vrm

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shadow rendering system for VRM avatars
 * Handles shadow map generation, shadow casting, and shadow receiving
 */
@Singleton
class ShadowSystem @Inject constructor() {
    
    companion object {
        private const val TAG = "ShadowSystem"
        
        // Shadow map configuration
        private const val SHADOW_MAP_SIZE = 1024
        private const val SHADOW_CASCADE_COUNT = 3
        private const val SHADOW_NEAR_PLANE = 0.1f
        private const val SHADOW_FAR_PLANE = 50f
        
        // Shadow quality settings
        private const val SHADOW_BIAS = 0.005f
        private const val SHADOW_NORMAL_BIAS = 0.1f
        private const val PCF_KERNEL_SIZE = 3
        
        // Shadow shader definitions
        private const val SHADOW_VERTEX_SHADER = """
            attribute vec4 position;
            attribute vec4 boneWeights;
            attribute ivec4 boneIndices;
            
            uniform mat4 modelMatrix;
            uniform mat4 lightSpaceMatrix;
            uniform mat4 boneMatrices[64];
            
            void main() {
                // Bone transformation
                mat4 boneTransform = mat4(0.0);
                boneTransform += boneMatrices[boneIndices.x] * boneWeights.x;
                boneTransform += boneMatrices[boneIndices.y] * boneWeights.y;
                boneTransform += boneMatrices[boneIndices.z] * boneWeights.z;
                boneTransform += boneMatrices[boneIndices.w] * boneWeights.w;
                
                vec4 skinnedPosition = boneTransform * position;
                vec4 worldPosition = modelMatrix * skinnedPosition;
                
                gl_Position = lightSpaceMatrix * worldPosition;
            }
        """
        
        private const val SHADOW_FRAGMENT_SHADER = """
            precision highp float;
            
            void main() {
                // Depth is automatically written to gl_FragDepth
                // For better precision, we could pack depth into color channels
                gl_FragColor = vec4(gl_FragCoord.z, gl_FragCoord.z, gl_FragCoord.z, 1.0);
            }
        """
        
        private const val SHADOW_RECEIVE_FRAGMENT_ADDITION = """
            // Shadow receiving additions to main fragment shader
            uniform sampler2D shadowMap;
            uniform mat4 lightSpaceMatrix;
            uniform float shadowStrength;
            uniform float shadowBias;
            uniform vec2 shadowMapSize;
            
            varying vec4 fragPosLightSpace;
            
            float calculateShadow(vec4 fragPosLightSpace, vec3 normal, vec3 lightDir) {
                // Perspective divide
                vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
                
                // Transform to [0,1] range
                projCoords = projCoords * 0.5 + 0.5;
                
                // Check if fragment is outside light frustum
                if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || 
                    projCoords.y < 0.0 || projCoords.y > 1.0) {
                    return 0.0; // No shadow outside light frustum
                }
                
                // Get closest depth value from shadow map
                float closestDepth = texture2D(shadowMap, projCoords.xy).r;
                
                // Get depth of current fragment from light's perspective
                float currentDepth = projCoords.z;
                
                // Calculate bias to prevent shadow acne
                float bias = max(shadowBias * (1.0 - dot(normal, lightDir)), shadowBias * 0.1);
                
                // PCF (Percentage Closer Filtering) for soft shadows
                float shadow = 0.0;
                vec2 texelSize = 1.0 / shadowMapSize;
                int kernelSize = """ + PCF_KERNEL_SIZE + """;
                int halfKernel = kernelSize / 2;
                
                for (int x = -halfKernel; x <= halfKernel; ++x) {
                    for (int y = -halfKernel; y <= halfKernel; ++y) {
                        vec2 offset = vec2(float(x), float(y)) * texelSize;
                        float pcfDepth = texture2D(shadowMap, projCoords.xy + offset).r;
                        shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
                    }
                }
                
                shadow /= float(kernelSize * kernelSize);
                
                // Apply shadow strength
                shadow *= shadowStrength;
                
                return shadow;
            }
        """
    }
    
    // Shadow system state
    private var isInitialized = false
    private var shadowMapFramebuffer: ShadowMapFramebuffer? = null
    private var shadowShader: FilamentShader? = null
    private var lightSpaceMatrix = FloatArray(16)
    private var shadowCasters = mutableListOf<ShadowCaster>()
    private var shadowReceivers = mutableListOf<ShadowReceiver>()
    
    // Shadow quality settings
    private var shadowQuality = ShadowQuality.MEDIUM
    private var shadowMapSize = SHADOW_MAP_SIZE
    private var enableSoftShadows = true
    
    /**
     * Initialize shadow system
     */
    fun initialize() {
        if (isInitialized) return
        
        try {
            Log.d(TAG, "Initializing shadow system")
            
            // Create shadow map framebuffer
            shadowMapFramebuffer = createShadowMapFramebuffer()
            
            // Create shadow shader
            shadowShader = createShadowShader()
            
            isInitialized = true
            Log.d(TAG, "Shadow system initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize shadow system", e)
            throw ARError.RenderingError("Failed to initialize shadow system: ${e.message}")
        }
    }
    
    /**
     * Update shadow system with current lighting
     */
    fun updateShadows(lightingParameters: LightingParameters, cameraPosition: FloatArray, cameraTarget: FloatArray) {
        if (!isInitialized) return
        
        try {
            // Calculate light space matrix for shadow mapping
            calculateLightSpaceMatrix(lightingParameters.lightDirection, cameraPosition, cameraTarget)
            
            // Render shadow map
            renderShadowMap()
            
            Log.d(TAG, "Shadow system updated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shadows", e)
        }
    }
    
    /**
     * Add shadow caster (object that casts shadows)
     */
    fun addShadowCaster(renderable: FilamentRenderable) {
        val shadowCaster = ShadowCaster(
            renderable = renderable,
            castsShadows = true
        )
        shadowCasters.add(shadowCaster)
        
        Log.d(TAG, "Added shadow caster: ${renderable.name}")
    }
    
    /**
     * Add shadow receiver (object that receives shadows)
     */
    fun addShadowReceiver(renderable: FilamentRenderable) {
        val shadowReceiver = ShadowReceiver(
            renderable = renderable,
            receivesShadows = true
        )
        shadowReceivers.add(shadowReceiver)
        
        Log.d(TAG, "Added shadow receiver: ${renderable.name}")
    }
    
    /**
     * Remove shadow caster
     */
    fun removeShadowCaster(renderable: FilamentRenderable) {
        shadowCasters.removeAll { it.renderable.name == renderable.name }
        Log.d(TAG, "Removed shadow caster: ${renderable.name}")
    }
    
    /**
     * Remove shadow receiver
     */
    fun removeShadowReceiver(renderable: FilamentRenderable) {
        shadowReceivers.removeAll { it.renderable.name == renderable.name }
        Log.d(TAG, "Removed shadow receiver: ${renderable.name}")
    }
    
    /**
     * Clear all shadow casters and receivers
     */
    fun clearShadowObjects() {
        shadowCasters.clear()
        shadowReceivers.clear()
        Log.d(TAG, "Cleared all shadow objects")
    }
    
    /**
     * Set shadow quality
     */
    fun setShadowQuality(quality: ShadowQuality) {
        shadowQuality = quality
        shadowMapSize = when (quality) {
            ShadowQuality.LOW -> 512
            ShadowQuality.MEDIUM -> 1024
            ShadowQuality.HIGH -> 2048
            ShadowQuality.ULTRA -> 4096
        }
        
        // Recreate shadow map with new size
        if (isInitialized) {
            shadowMapFramebuffer?.cleanup()
            shadowMapFramebuffer = createShadowMapFramebuffer()
        }
        
        Log.d(TAG, "Shadow quality set to $quality (${shadowMapSize}x${shadowMapSize})")
    }
    
    /**
     * Enable or disable soft shadows
     */
    fun setSoftShadowsEnabled(enabled: Boolean) {
        enableSoftShadows = enabled
        Log.d(TAG, "Soft shadows ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Get shadow map texture for use in materials
     */
    fun getShadowMapTexture(): FilamentTexture? {
        return shadowMapFramebuffer?.depthTexture
    }
    
    /**
     * Get light space matrix for shadow mapping
     */
    fun getLightSpaceMatrix(): FloatArray {
        return lightSpaceMatrix.copyOf()
    }
    
    /**
     * Get shadow shader additions for materials
     */
    fun getShadowShaderAdditions(): String {
        return SHADOW_RECEIVE_FRAGMENT_ADDITION
    }
    
    /**
     * Cleanup shadow system
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up shadow system")
        
        shadowMapFramebuffer?.cleanup()
        shadowMapFramebuffer = null
        shadowShader = null
        shadowCasters.clear()
        shadowReceivers.clear()
        
        isInitialized = false
    }
    
    // Private helper methods
    
    /**
     * Create shadow map framebuffer
     */
    private fun createShadowMapFramebuffer(): ShadowMapFramebuffer {
        // TODO: Create actual Filament framebuffer when dependencies are available
        // This would involve:
        // 1. Creating depth texture
        // 2. Creating framebuffer
        // 3. Attaching depth texture to framebuffer
        
        val depthTexture = FilamentTexture(
            name = "shadow_map_depth",
            data = ByteArray(shadowMapSize * shadowMapSize * 4), // Placeholder
            format = TextureFormat.UNKNOWN, // Use UNKNOWN as placeholder for depth texture
            width = shadowMapSize,
            height = shadowMapSize,
            mipLevels = 1,
            sRGB = false
        )
        
        return ShadowMapFramebuffer(
            framebufferId = 0, // Placeholder
            depthTexture = depthTexture,
            size = shadowMapSize
        )
    }
    
    /**
     * Create shadow shader
     */
    private fun createShadowShader(): FilamentShader {
        return FilamentShader(
            name = "shadow_map_shader",
            vertexShader = SHADOW_VERTEX_SHADER,
            fragmentShader = SHADOW_FRAGMENT_SHADER,
            defines = mapOf(
                "SHADOW_MAP_SIZE" to shadowMapSize.toString(),
                "ENABLE_SOFT_SHADOWS" to if (enableSoftShadows) "1" else "0"
            )
        )
    }
    
    /**
     * Calculate light space matrix for shadow mapping
     */
    private fun calculateLightSpaceMatrix(lightDirection: FloatArray, cameraPosition: FloatArray, cameraTarget: FloatArray) {
        // Calculate light position (opposite to light direction)
        val lightDistance = 20f
        val lightPosition = floatArrayOf(
            -lightDirection[0] * lightDistance,
            -lightDirection[1] * lightDistance,
            -lightDirection[2] * lightDistance
        )
        
        // Calculate scene bounds for optimal shadow map coverage
        val sceneBounds = calculateSceneBounds(cameraPosition, cameraTarget)
        
        // Create orthographic projection matrix for directional light
        val lightProjection = createOrthographicMatrix(
            -sceneBounds.width / 2f, sceneBounds.width / 2f,
            -sceneBounds.height / 2f, sceneBounds.height / 2f,
            SHADOW_NEAR_PLANE, SHADOW_FAR_PLANE
        )
        
        // Create light view matrix
        val lightView = createLookAtMatrix(
            lightPosition,
            floatArrayOf(0f, 0f, 0f), // Look at scene center
            floatArrayOf(0f, 1f, 0f)  // Up vector
        )
        
        // Combine projection and view matrices
        lightSpaceMatrix = multiplyMatrices(lightProjection, lightView)
    }
    
    /**
     * Calculate scene bounds for shadow map optimization
     */
    private fun calculateSceneBounds(cameraPosition: FloatArray, cameraTarget: FloatArray): SceneBounds {
        // Simple bounds calculation - in a real implementation this would be more sophisticated
        val distance = calculateDistance(cameraPosition, cameraTarget)
        val bounds = maxOf(distance * 0.5f, 5f)
        
        return SceneBounds(
            width = bounds * 2f,
            height = bounds * 2f,
            depth = bounds * 2f
        )
    }
    
    /**
     * Render shadow map
     */
    private fun renderShadowMap() {
        val framebuffer = shadowMapFramebuffer ?: return
        val shader = shadowShader ?: return
        
        // TODO: Actual shadow map rendering when Filament is available
        // This would involve:
        // 1. Binding shadow map framebuffer
        // 2. Setting viewport to shadow map size
        // 3. Clearing depth buffer
        // 4. Setting light space matrix uniform
        // 5. Rendering all shadow casters with shadow shader
        // 6. Unbinding framebuffer
        
        Log.d(TAG, "Rendering shadow map with ${shadowCasters.size} casters")
    }
    
    /**
     * Create orthographic projection matrix
     */
    private fun createOrthographicMatrix(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float): FloatArray {
        val matrix = FloatArray(16)
        
        matrix[0] = 2f / (right - left)
        matrix[1] = 0f
        matrix[2] = 0f
        matrix[3] = 0f
        
        matrix[4] = 0f
        matrix[5] = 2f / (top - bottom)
        matrix[6] = 0f
        matrix[7] = 0f
        
        matrix[8] = 0f
        matrix[9] = 0f
        matrix[10] = -2f / (far - near)
        matrix[11] = 0f
        
        matrix[12] = -(right + left) / (right - left)
        matrix[13] = -(top + bottom) / (top - bottom)
        matrix[14] = -(far + near) / (far - near)
        matrix[15] = 1f
        
        return matrix
    }
    
    /**
     * Create look-at view matrix
     */
    private fun createLookAtMatrix(eye: FloatArray, center: FloatArray, up: FloatArray): FloatArray {
        val f = normalize(subtract(center, eye))
        val s = normalize(cross(f, up))
        val u = cross(s, f)
        
        val matrix = FloatArray(16)
        
        matrix[0] = s[0]
        matrix[1] = u[0]
        matrix[2] = -f[0]
        matrix[3] = 0f
        
        matrix[4] = s[1]
        matrix[5] = u[1]
        matrix[6] = -f[1]
        matrix[7] = 0f
        
        matrix[8] = s[2]
        matrix[9] = u[2]
        matrix[10] = -f[2]
        matrix[11] = 0f
        
        matrix[12] = -dot(s, eye)
        matrix[13] = -dot(u, eye)
        matrix[14] = dot(f, eye)
        matrix[15] = 1f
        
        return matrix
    }
    
    /**
     * Multiply two 4x4 matrices
     */
    private fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(16)
        
        for (i in 0..3) {
            for (j in 0..3) {
                result[i * 4 + j] = 
                    a[i * 4 + 0] * b[0 * 4 + j] +
                    a[i * 4 + 1] * b[1 * 4 + j] +
                    a[i * 4 + 2] * b[2 * 4 + j] +
                    a[i * 4 + 3] * b[3 * 4 + j]
            }
        }
        
        return result
    }
    
    // Vector math helper functions
    private fun subtract(a: FloatArray, b: FloatArray): FloatArray {
        return floatArrayOf(a[0] - b[0], a[1] - b[1], a[2] - b[2])
    }
    
    private fun normalize(v: FloatArray): FloatArray {
        val length = kotlin.math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        return if (length > 0f) floatArrayOf(v[0] / length, v[1] / length, v[2] / length) else v
    }
    
    private fun cross(a: FloatArray, b: FloatArray): FloatArray {
        return floatArrayOf(
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        )
    }
    
    private fun dot(a: FloatArray, b: FloatArray): Float {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    }
    
    private fun calculateDistance(a: FloatArray, b: FloatArray): Float {
        val dx = a[0] - b[0]
        val dy = a[1] - b[1]
        val dz = a[2] - b[2]
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}

/**
 * Shadow quality levels
 */
enum class ShadowQuality {
    LOW,    // 512x512
    MEDIUM, // 1024x1024
    HIGH,   // 2048x2048
    ULTRA   // 4096x4096
}

/**
 * Shadow map framebuffer
 */
data class ShadowMapFramebuffer(
    val framebufferId: Int,
    val depthTexture: FilamentTexture,
    val size: Int
) {
    fun cleanup() {
        // TODO: Cleanup Filament framebuffer resources
    }
}

/**
 * Shadow caster object
 */
data class ShadowCaster(
    val renderable: FilamentRenderable,
    var castsShadows: Boolean = true
)

/**
 * Shadow receiver object
 */
data class ShadowReceiver(
    val renderable: FilamentRenderable,
    var receivesShadows: Boolean = true
)

/**
 * Scene bounds for shadow map optimization
 */
data class SceneBounds(
    val width: Float,
    val height: Float,
    val depth: Float
)