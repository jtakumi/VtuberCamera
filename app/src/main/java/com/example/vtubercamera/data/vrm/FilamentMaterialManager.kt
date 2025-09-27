package com.example.vtubercamera.data.vrm

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Filament materials for VRM avatars
 * Handles material creation, shader setup, and texture binding
 */
@Singleton
class FilamentMaterialManager @Inject constructor() {
    
    companion object {
        private const val TAG = "FilamentMaterialManager"
        
        // Standard VRM material properties
        private const val DEFAULT_METALLIC = 0f
        private const val DEFAULT_ROUGHNESS = 1f
        private const val DEFAULT_ALPHA_CUTOFF = 0.5f
        
        // Shader definitions for VRM materials
        private const val VRM_VERTEX_SHADER = """
            attribute vec4 position;
            attribute vec3 normal;
            attribute vec2 uv0;
            attribute vec4 color;
            attribute vec4 boneWeights;
            attribute ivec4 boneIndices;
            
            uniform mat4 modelMatrix;
            uniform mat4 viewMatrix;
            uniform mat4 projectionMatrix;
            uniform mat4 normalMatrix;
            uniform mat4 boneMatrices[64];
            
            varying vec3 worldPosition;
            varying vec3 worldNormal;
            varying vec2 texCoord;
            varying vec4 vertexColor;
            
            void main() {
                // Bone transformation
                mat4 boneTransform = mat4(0.0);
                boneTransform += boneMatrices[boneIndices.x] * boneWeights.x;
                boneTransform += boneMatrices[boneIndices.y] * boneWeights.y;
                boneTransform += boneMatrices[boneIndices.z] * boneWeights.z;
                boneTransform += boneMatrices[boneIndices.w] * boneWeights.w;
                
                vec4 skinnedPosition = boneTransform * position;
                vec4 skinnedNormal = boneTransform * vec4(normal, 0.0);
                
                // Transform to world space
                vec4 worldPos = modelMatrix * skinnedPosition;
                worldPosition = worldPos.xyz;
                worldNormal = normalize((normalMatrix * skinnedNormal).xyz);
                
                // Pass through texture coordinates and vertex color
                texCoord = uv0;
                vertexColor = color;
                
                // Final position
                gl_Position = projectionMatrix * viewMatrix * worldPos;
            }
        """
        
        private const val VRM_FRAGMENT_SHADER = """
            precision mediump float;
            
            uniform vec4 baseColorFactor;
            uniform float metallicFactor;
            uniform float roughnessFactor;
            uniform vec3 emissiveFactor;
            uniform float alphaCutoff;
            uniform int alphaMode;
            
            uniform sampler2D baseColorTexture;
            uniform sampler2D normalTexture;
            uniform sampler2D metallicRoughnessTexture;
            uniform sampler2D emissiveTexture;
            uniform sampler2D occlusionTexture;
            
            uniform bool hasBaseColorTexture;
            uniform bool hasNormalTexture;
            uniform bool hasMetallicRoughnessTexture;
            uniform bool hasEmissiveTexture;
            uniform bool hasOcclusionTexture;
            
            // Lighting uniforms
            uniform vec3 lightDirection;
            uniform vec3 lightColor;
            uniform float lightIntensity;
            uniform vec3 ambientColor;
            uniform vec3 cameraPosition;
            
            varying vec3 worldPosition;
            varying vec3 worldNormal;
            varying vec2 texCoord;
            varying vec4 vertexColor;
            
            // PBR lighting calculation
            vec3 calculatePBR(vec3 albedo, float metallic, float roughness, vec3 normal, vec3 viewDir, vec3 lightDir) {
                vec3 halfVector = normalize(lightDir + viewDir);
                float NdotL = max(dot(normal, lightDir), 0.0);
                float NdotV = max(dot(normal, viewDir), 0.0);
                float NdotH = max(dot(normal, halfVector), 0.0);
                float VdotH = max(dot(viewDir, halfVector), 0.0);
                
                // Fresnel
                vec3 F0 = mix(vec3(0.04), albedo, metallic);
                vec3 F = F0 + (1.0 - F0) * pow(1.0 - VdotH, 5.0);
                
                // Distribution
                float alpha = roughness * roughness;
                float alpha2 = alpha * alpha;
                float denom = NdotH * NdotH * (alpha2 - 1.0) + 1.0;
                float D = alpha2 / (3.14159265 * denom * denom);
                
                // Geometry
                float k = (roughness + 1.0) * (roughness + 1.0) / 8.0;
                float G1L = NdotL / (NdotL * (1.0 - k) + k);
                float G1V = NdotV / (NdotV * (1.0 - k) + k);
                float G = G1L * G1V;
                
                // BRDF
                vec3 numerator = D * G * F;
                float denominator = 4.0 * NdotV * NdotL + 0.001;
                vec3 specular = numerator / denominator;
                
                vec3 kS = F;
                vec3 kD = vec3(1.0) - kS;
                kD *= 1.0 - metallic;
                
                return (kD * albedo / 3.14159265 + specular) * lightColor * lightIntensity * NdotL;
            }
            
            void main() {
                // Sample base color
                vec4 baseColor = baseColorFactor * vertexColor;
                if (hasBaseColorTexture) {
                    baseColor *= texture2D(baseColorTexture, texCoord);
                }
                
                // Alpha testing
                if (alphaMode == 1 && baseColor.a < alphaCutoff) { // MASK mode
                    discard;
                }
                
                // Sample material properties
                float metallic = metallicFactor;
                float roughness = roughnessFactor;
                if (hasMetallicRoughnessTexture) {
                    vec3 metallicRoughness = texture2D(metallicRoughnessTexture, texCoord).rgb;
                    metallic *= metallicRoughness.b;
                    roughness *= metallicRoughness.g;
                }
                
                // Sample normal map
                vec3 normal = normalize(worldNormal);
                if (hasNormalTexture) {
                    vec3 normalMap = texture2D(normalTexture, texCoord).rgb * 2.0 - 1.0;
                    // Simple normal mapping (should use proper tangent space)
                    normal = normalize(normal + normalMap * 0.1);
                }
                
                // Calculate lighting
                vec3 viewDir = normalize(cameraPosition - worldPosition);
                vec3 lightDir = normalize(-lightDirection);
                
                vec3 color = calculatePBR(baseColor.rgb, metallic, roughness, normal, viewDir, lightDir);
                
                // Add ambient lighting
                color += baseColor.rgb * ambientColor;
                
                // Sample emissive
                vec3 emissive = emissiveFactor;
                if (hasEmissiveTexture) {
                    emissive *= texture2D(emissiveTexture, texCoord).rgb;
                }
                color += emissive;
                
                // Apply occlusion
                if (hasOcclusionTexture) {
                    float occlusion = texture2D(occlusionTexture, texCoord).r;
                    color *= occlusion;
                }
                
                gl_FragColor = vec4(color, baseColor.a);
            }
        """
    }
    
    // Material cache
    private val materialCache = mutableMapOf<String, FilamentMaterialInstance>()
    private val shaderCache = mutableMapOf<String, FilamentShader>()
    
    /**
     * Create or get cached material instance
     */
    fun createMaterial(material: FilamentMaterial, textures: Map<String, FilamentTexture>): FilamentMaterialInstance {
        val cacheKey = "${material.name}_${material.index}"
        
        return materialCache.getOrPut(cacheKey) {
            Log.d(TAG, "Creating material: ${material.name}")
            
            val shader = getOrCreateShader(material)
            val instance = FilamentMaterialInstance(
                name = material.name,
                shader = shader,
                parameters = createMaterialParameters(material, textures)
            )
            
            Log.d(TAG, "Created material instance: ${material.name}")
            instance
        }
    }
    
    /**
     * Get or create shader for material
     */
    private fun getOrCreateShader(material: FilamentMaterial): FilamentShader {
        val shaderKey = generateShaderKey(material)
        
        return shaderCache.getOrPut(shaderKey) {
            Log.d(TAG, "Creating shader for material: ${material.name}")
            
            FilamentShader(
                name = "vrm_material_${material.name}",
                vertexShader = VRM_VERTEX_SHADER,
                fragmentShader = VRM_FRAGMENT_SHADER,
                defines = generateShaderDefines(material)
            )
        }
    }
    
    /**
     * Generate shader key based on material features
     */
    private fun generateShaderKey(material: FilamentMaterial): String {
        val features = mutableListOf<String>()
        
        if (material.baseColorTexture != null) features.add("BASE_COLOR_TEX")
        if (material.normalTexture != null) features.add("NORMAL_TEX")
        if (material.metallicRoughnessTexture != null) features.add("METALLIC_ROUGHNESS_TEX")
        if (material.emissiveTexture != null) features.add("EMISSIVE_TEX")
        if (material.occlusionTexture != null) features.add("OCCLUSION_TEX")
        if (material.doubleSided) features.add("DOUBLE_SIDED")
        
        features.add("ALPHA_${material.alphaMode.name}")
        
        return features.joinToString("_")
    }
    
    /**
     * Generate shader defines based on material
     */
    private fun generateShaderDefines(material: FilamentMaterial): Map<String, String> {
        val defines = mutableMapOf<String, String>()
        
        if (material.baseColorTexture != null) defines["HAS_BASE_COLOR_TEXTURE"] = "1"
        if (material.normalTexture != null) defines["HAS_NORMAL_TEXTURE"] = "1"
        if (material.metallicRoughnessTexture != null) defines["HAS_METALLIC_ROUGHNESS_TEXTURE"] = "1"
        if (material.emissiveTexture != null) defines["HAS_EMISSIVE_TEXTURE"] = "1"
        if (material.occlusionTexture != null) defines["HAS_OCCLUSION_TEXTURE"] = "1"
        
        defines["ALPHA_MODE"] = when (material.alphaMode) {
            AlphaMode.OPAQUE -> "0"
            AlphaMode.MASK -> "1"
            AlphaMode.BLEND -> "2"
        }
        
        return defines
    }
    
    /**
     * Create material parameters
     */
    private fun createMaterialParameters(material: FilamentMaterial, textures: Map<String, FilamentTexture>): MaterialParameters {
        val parameters = MaterialParameters()
        
        // Set basic material properties
        parameters.setFloat4("baseColorFactor", material.baseColorFactor)
        parameters.setFloat("metallicFactor", material.metallicFactor)
        parameters.setFloat("roughnessFactor", material.roughnessFactor)
        parameters.setFloat3("emissiveFactor", material.emissiveFactor)
        parameters.setFloat("alphaCutoff", material.alphaCutoff)
        parameters.setInt("alphaMode", material.alphaMode.ordinal)
        
        // Set texture parameters
        material.baseColorTexture?.let { textureName ->
            textures[textureName]?.let { texture ->
                parameters.setTexture("baseColorTexture", texture)
                parameters.setBool("hasBaseColorTexture", true)
            }
        }
        
        material.normalTexture?.let { textureName ->
            textures[textureName]?.let { texture ->
                parameters.setTexture("normalTexture", texture)
                parameters.setBool("hasNormalTexture", true)
            }
        }
        
        material.metallicRoughnessTexture?.let { textureName ->
            textures[textureName]?.let { texture ->
                parameters.setTexture("metallicRoughnessTexture", texture)
                parameters.setBool("hasMetallicRoughnessTexture", true)
            }
        }
        
        material.emissiveTexture?.let { textureName ->
            textures[textureName]?.let { texture ->
                parameters.setTexture("emissiveTexture", texture)
                parameters.setBool("hasEmissiveTexture", true)
            }
        }
        
        material.occlusionTexture?.let { textureName ->
            textures[textureName]?.let { texture ->
                parameters.setTexture("occlusionTexture", texture)
                parameters.setBool("hasOcclusionTexture", true)
            }
        }
        
        return parameters
    }
    
    /**
     * Update material lighting parameters
     */
    fun updateLighting(materialInstance: FilamentMaterialInstance, lightingParams: LightingParameters) {
        materialInstance.parameters.apply {
            setFloat3("lightDirection", lightingParams.lightDirection)
            setFloat3("lightColor", lightingParams.lightColor)
            setFloat("lightIntensity", lightingParams.lightIntensity)
            setFloat3("ambientColor", lightingParams.ambientColor)
            setFloat3("cameraPosition", lightingParams.cameraPosition)
        }
    }
    
    /**
     * Clear material cache
     */
    fun clearCache() {
        Log.d(TAG, "Clearing material cache")
        materialCache.clear()
        shaderCache.clear()
    }
    
    /**
     * Get material statistics
     */
    fun getStatistics(): MaterialManagerStatistics {
        return MaterialManagerStatistics(
            cachedMaterials = materialCache.size,
            cachedShaders = shaderCache.size
        )
    }
}

/**
 * Filament material instance
 */
data class FilamentMaterialInstance(
    val name: String,
    val shader: FilamentShader,
    val parameters: MaterialParameters
)

/**
 * Filament shader definition
 */
data class FilamentShader(
    val name: String,
    val vertexShader: String,
    val fragmentShader: String,
    val defines: Map<String, String>
)

/**
 * Material parameters container
 */
class MaterialParameters {
    private val floatParams = mutableMapOf<String, Float>()
    private val float3Params = mutableMapOf<String, FloatArray>()
    private val float4Params = mutableMapOf<String, FloatArray>()
    private val intParams = mutableMapOf<String, Int>()
    private val boolParams = mutableMapOf<String, Boolean>()
    private val textureParams = mutableMapOf<String, FilamentTexture>()
    
    fun setFloat(name: String, value: Float) {
        floatParams[name] = value
    }
    
    fun setFloat3(name: String, value: FloatArray) {
        require(value.size == 3) { "Float3 parameter must have 3 components" }
        float3Params[name] = value
    }
    
    fun setFloat4(name: String, value: FloatArray) {
        require(value.size == 4) { "Float4 parameter must have 4 components" }
        float4Params[name] = value
    }
    
    fun setInt(name: String, value: Int) {
        intParams[name] = value
    }
    
    fun setBool(name: String, value: Boolean) {
        boolParams[name] = value
    }
    
    fun setTexture(name: String, texture: FilamentTexture) {
        textureParams[name] = texture
    }
    
    // Getters
    fun getFloat(name: String): Float? = floatParams[name]
    fun getFloat3(name: String): FloatArray? = float3Params[name]
    fun getFloat4(name: String): FloatArray? = float4Params[name]
    fun getInt(name: String): Int? = intParams[name]
    fun getBool(name: String): Boolean? = boolParams[name]
    fun getTexture(name: String): FilamentTexture? = textureParams[name]
    
    fun getAllFloats(): Map<String, Float> = floatParams.toMap()
    fun getAllFloat3s(): Map<String, FloatArray> = float3Params.toMap()
    fun getAllFloat4s(): Map<String, FloatArray> = float4Params.toMap()
    fun getAllInts(): Map<String, Int> = intParams.toMap()
    fun getAllBools(): Map<String, Boolean> = boolParams.toMap()
    fun getAllTextures(): Map<String, FilamentTexture> = textureParams.toMap()
}

/**
 * Lighting parameters for materials
 */
data class LightingParameters(
    val lightDirection: FloatArray = floatArrayOf(0f, -1f, 0f),
    val lightColor: FloatArray = floatArrayOf(1f, 1f, 1f),
    val lightIntensity: Float = 1f,
    val ambientColor: FloatArray = floatArrayOf(0.2f, 0.2f, 0.2f),
    val cameraPosition: FloatArray = floatArrayOf(0f, 0f, 5f)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LightingParameters

        if (!lightDirection.contentEquals(other.lightDirection)) return false
        if (!lightColor.contentEquals(other.lightColor)) return false
        if (lightIntensity != other.lightIntensity) return false
        if (!ambientColor.contentEquals(other.ambientColor)) return false
        if (!cameraPosition.contentEquals(other.cameraPosition)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lightDirection.contentHashCode()
        result = 31 * result + lightColor.contentHashCode()
        result = 31 * result + lightIntensity.hashCode()
        result = 31 * result + ambientColor.contentHashCode()
        result = 31 * result + cameraPosition.contentHashCode()
        return result
    }
}

/**
 * Material manager statistics
 */
data class MaterialManagerStatistics(
    val cachedMaterials: Int,
    val cachedShaders: Int
)