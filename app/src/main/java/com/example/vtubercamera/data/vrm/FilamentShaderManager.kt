package com.example.vtubercamera.data.vrm

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Filament shaders for VRM avatar rendering
 * Handles shader compilation, caching, and parameter binding
 */
@Singleton
class FilamentShaderManager @Inject constructor() {
    
    companion object {
        private const val TAG = "FilamentShaderManager"
        
        // VRM-specific shader variants
        private const val VRM_STANDARD_SHADER = "vrm_standard"
        private const val VRM_TRANSPARENT_SHADER = "vrm_transparent"
        private const val VRM_CUTOUT_SHADER = "vrm_cutout"
        private const val VRM_UNLIT_SHADER = "vrm_unlit"
        
        // Shader feature flags
        private const val FEATURE_BASE_COLOR_TEXTURE = "BASE_COLOR_TEXTURE"
        private const val FEATURE_NORMAL_TEXTURE = "NORMAL_TEXTURE"
        private const val FEATURE_METALLIC_ROUGHNESS_TEXTURE = "METALLIC_ROUGHNESS_TEXTURE"
        private const val FEATURE_EMISSIVE_TEXTURE = "EMISSIVE_TEXTURE"
        private const val FEATURE_OCCLUSION_TEXTURE = "OCCLUSION_TEXTURE"
        private const val FEATURE_VERTEX_COLORS = "VERTEX_COLORS"
        private const val FEATURE_SKINNING = "SKINNING"
        private const val FEATURE_MORPH_TARGETS = "MORPH_TARGETS"
    }
    
    // Shader cache
    private val shaderCache = mutableMapOf<String, CompiledShader>()
    private val shaderVariants = mutableMapOf<String, ShaderVariant>()
    
    init {
        initializeShaderVariants()
    }
    
    /**
     * Initialize standard VRM shader variants
     */
    private fun initializeShaderVariants() {
        Log.d(TAG, "Initializing VRM shader variants")
        
        // Standard PBR shader
        shaderVariants[VRM_STANDARD_SHADER] = ShaderVariant(
            name = VRM_STANDARD_SHADER,
            vertexShader = getStandardVertexShader(),
            fragmentShader = getStandardFragmentShader(),
            features = setOf(
                FEATURE_BASE_COLOR_TEXTURE,
                FEATURE_NORMAL_TEXTURE,
                FEATURE_METALLIC_ROUGHNESS_TEXTURE,
                FEATURE_EMISSIVE_TEXTURE,
                FEATURE_OCCLUSION_TEXTURE,
                FEATURE_VERTEX_COLORS,
                FEATURE_SKINNING
            )
        )
        
        // Transparent shader
        shaderVariants[VRM_TRANSPARENT_SHADER] = ShaderVariant(
            name = VRM_TRANSPARENT_SHADER,
            vertexShader = getStandardVertexShader(),
            fragmentShader = getTransparentFragmentShader(),
            features = setOf(
                FEATURE_BASE_COLOR_TEXTURE,
                FEATURE_NORMAL_TEXTURE,
                FEATURE_VERTEX_COLORS,
                FEATURE_SKINNING
            )
        )
        
        // Alpha cutout shader
        shaderVariants[VRM_CUTOUT_SHADER] = ShaderVariant(
            name = VRM_CUTOUT_SHADER,
            vertexShader = getStandardVertexShader(),
            fragmentShader = getCutoutFragmentShader(),
            features = setOf(
                FEATURE_BASE_COLOR_TEXTURE,
                FEATURE_NORMAL_TEXTURE,
                FEATURE_VERTEX_COLORS,
                FEATURE_SKINNING
            )
        )
        
        // Unlit shader for special effects
        shaderVariants[VRM_UNLIT_SHADER] = ShaderVariant(
            name = VRM_UNLIT_SHADER,
            vertexShader = getUnlitVertexShader(),
            fragmentShader = getUnlitFragmentShader(),
            features = setOf(
                FEATURE_BASE_COLOR_TEXTURE,
                FEATURE_VERTEX_COLORS,
                FEATURE_SKINNING
            )
        )
        
        Log.d(TAG, "Initialized ${shaderVariants.size} shader variants")
    }
    
    /**
     * Get or compile shader for material
     */
    fun getShader(material: FilamentMaterial, requiredFeatures: Set<String>): CompiledShader {
        val shaderKey = generateShaderKey(material, requiredFeatures)
        
        return shaderCache.getOrPut(shaderKey) {
            Log.d(TAG, "Compiling shader for material: ${material.name}")
            
            val baseVariant = selectShaderVariant(material)
            val customizedShader = customizeShader(baseVariant, requiredFeatures)
            
            compileShader(customizedShader)
        }
    }
    
    /**
     * Select appropriate shader variant based on material
     */
    private fun selectShaderVariant(material: FilamentMaterial): ShaderVariant {
        return when (material.alphaMode) {
            AlphaMode.OPAQUE -> shaderVariants[VRM_STANDARD_SHADER]!!
            AlphaMode.MASK -> shaderVariants[VRM_CUTOUT_SHADER]!!
            AlphaMode.BLEND -> shaderVariants[VRM_TRANSPARENT_SHADER]!!
        }
    }
    
    /**
     * Customize shader based on required features
     */
    private fun customizeShader(baseVariant: ShaderVariant, requiredFeatures: Set<String>): ShaderVariant {
        val activeFeatures = baseVariant.features.intersect(requiredFeatures)
        
        val defines = activeFeatures.associateWith { "1" }
        
        return baseVariant.copy(
            vertexShader = preprocessShader(baseVariant.vertexShader, defines),
            fragmentShader = preprocessShader(baseVariant.fragmentShader, defines),
            features = activeFeatures
        )
    }
    
    /**
     * Preprocess shader with defines
     */
    private fun preprocessShader(shaderSource: String, defines: Map<String, String>): String {
        var processedShader = shaderSource
        
        // Add defines at the beginning
        val defineLines = defines.map { (key, value) -> "#define $key $value" }
        val defineBlock = defineLines.joinToString("\n") + "\n"
        
        // Insert defines after version directive
        val versionRegex = Regex("#version\\s+\\d+.*\n")
        processedShader = if (versionRegex.containsMatchIn(processedShader)) {
            versionRegex.replace(processedShader) { match ->
                match.value + defineBlock
            }
        } else {
            defineBlock + processedShader
        }
        
        return processedShader
    }
    
    /**
     * Compile shader (placeholder implementation)
     */
    private fun compileShader(variant: ShaderVariant): CompiledShader {
        // TODO: Actual Filament shader compilation when dependencies are available
        Log.d(TAG, "Compiling shader variant: ${variant.name}")
        
        return CompiledShader(
            name = variant.name,
            vertexShader = variant.vertexShader,
            fragmentShader = variant.fragmentShader,
            features = variant.features,
            compiled = false // Will be true when actually compiled
        )
    }
    
    /**
     * Generate unique shader key
     */
    private fun generateShaderKey(material: FilamentMaterial, features: Set<String>): String {
        val materialKey = "${material.alphaMode.name}_${material.doubleSided}"
        val featureKey = features.sorted().joinToString("_")
        return "${materialKey}_$featureKey"
    }
    
    /**
     * Get standard vertex shader source
     */
    private fun getStandardVertexShader(): String = """
        #version 300 es
        
        layout(location = 0) in vec3 position;
        layout(location = 1) in vec3 normal;
        layout(location = 2) in vec2 uv0;
        layout(location = 3) in vec4 color;
        
        #ifdef SKINNING
        layout(location = 4) in vec4 boneWeights;
        layout(location = 5) in ivec4 boneIndices;
        uniform mat4 boneMatrices[64];
        #endif
        
        #ifdef MORPH_TARGETS
        layout(location = 6) in vec3 morphTarget0;
        layout(location = 7) in vec3 morphTarget1;
        uniform float morphWeights[8];
        #endif
        
        uniform mat4 modelMatrix;
        uniform mat4 viewMatrix;
        uniform mat4 projectionMatrix;
        uniform mat3 normalMatrix;
        
        out vec3 worldPosition;
        out vec3 worldNormal;
        out vec2 texCoord;
        out vec4 vertexColor;
        
        void main() {
            vec3 pos = position;
            vec3 norm = normal;
            
            #ifdef MORPH_TARGETS
            pos += morphTarget0 * morphWeights[0];
            pos += morphTarget1 * morphWeights[1];
            #endif
            
            #ifdef SKINNING
            mat4 boneTransform = mat4(0.0);
            boneTransform += boneMatrices[boneIndices.x] * boneWeights.x;
            boneTransform += boneMatrices[boneIndices.y] * boneWeights.y;
            boneTransform += boneMatrices[boneIndices.z] * boneWeights.z;
            boneTransform += boneMatrices[boneIndices.w] * boneWeights.w;
            
            vec4 skinnedPosition = boneTransform * vec4(pos, 1.0);
            vec4 skinnedNormal = boneTransform * vec4(norm, 0.0);
            
            pos = skinnedPosition.xyz;
            norm = skinnedNormal.xyz;
            #endif
            
            vec4 worldPos = modelMatrix * vec4(pos, 1.0);
            worldPosition = worldPos.xyz;
            worldNormal = normalize(normalMatrix * norm);
            
            texCoord = uv0;
            
            #ifdef VERTEX_COLORS
            vertexColor = color;
            #else
            vertexColor = vec4(1.0);
            #endif
            
            gl_Position = projectionMatrix * viewMatrix * worldPos;
        }
    """
    
    /**
     * Get standard fragment shader source
     */
    private fun getStandardFragmentShader(): String = """
        #version 300 es
        precision mediump float;
        
        uniform vec4 baseColorFactor;
        uniform float metallicFactor;
        uniform float roughnessFactor;
        uniform vec3 emissiveFactor;
        
        #ifdef BASE_COLOR_TEXTURE
        uniform sampler2D baseColorTexture;
        #endif
        
        #ifdef NORMAL_TEXTURE
        uniform sampler2D normalTexture;
        #endif
        
        #ifdef METALLIC_ROUGHNESS_TEXTURE
        uniform sampler2D metallicRoughnessTexture;
        #endif
        
        #ifdef EMISSIVE_TEXTURE
        uniform sampler2D emissiveTexture;
        #endif
        
        #ifdef OCCLUSION_TEXTURE
        uniform sampler2D occlusionTexture;
        #endif
        
        uniform vec3 lightDirection;
        uniform vec3 lightColor;
        uniform float lightIntensity;
        uniform vec3 ambientColor;
        uniform vec3 cameraPosition;
        
        in vec3 worldPosition;
        in vec3 worldNormal;
        in vec2 texCoord;
        in vec4 vertexColor;
        
        out vec4 fragColor;
        
        vec3 calculatePBR(vec3 albedo, float metallic, float roughness, vec3 normal, vec3 viewDir, vec3 lightDir) {
            vec3 halfVector = normalize(lightDir + viewDir);
            float NdotL = max(dot(normal, lightDir), 0.0);
            float NdotV = max(dot(normal, viewDir), 0.0);
            float NdotH = max(dot(normal, halfVector), 0.0);
            float VdotH = max(dot(viewDir, halfVector), 0.0);
            
            vec3 F0 = mix(vec3(0.04), albedo, metallic);
            vec3 F = F0 + (1.0 - F0) * pow(1.0 - VdotH, 5.0);
            
            float alpha = roughness * roughness;
            float alpha2 = alpha * alpha;
            float denom = NdotH * NdotH * (alpha2 - 1.0) + 1.0;
            float D = alpha2 / (3.14159265 * denom * denom);
            
            float k = (roughness + 1.0) * (roughness + 1.0) / 8.0;
            float G1L = NdotL / (NdotL * (1.0 - k) + k);
            float G1V = NdotV / (NdotV * (1.0 - k) + k);
            float G = G1L * G1V;
            
            vec3 numerator = D * G * F;
            float denominator = 4.0 * NdotV * NdotL + 0.001;
            vec3 specular = numerator / denominator;
            
            vec3 kS = F;
            vec3 kD = vec3(1.0) - kS;
            kD *= 1.0 - metallic;
            
            return (kD * albedo / 3.14159265 + specular) * lightColor * lightIntensity * NdotL;
        }
        
        void main() {
            vec4 baseColor = baseColorFactor * vertexColor;
            
            #ifdef BASE_COLOR_TEXTURE
            baseColor *= texture(baseColorTexture, texCoord);
            #endif
            
            float metallic = metallicFactor;
            float roughness = roughnessFactor;
            
            #ifdef METALLIC_ROUGHNESS_TEXTURE
            vec3 metallicRoughness = texture(metallicRoughnessTexture, texCoord).rgb;
            metallic *= metallicRoughness.b;
            roughness *= metallicRoughness.g;
            #endif
            
            vec3 normal = normalize(worldNormal);
            
            #ifdef NORMAL_TEXTURE
            vec3 normalMap = texture(normalTexture, texCoord).rgb * 2.0 - 1.0;
            normal = normalize(normal + normalMap * 0.1);
            #endif
            
            vec3 viewDir = normalize(cameraPosition - worldPosition);
            vec3 lightDir = normalize(-lightDirection);
            
            vec3 color = calculatePBR(baseColor.rgb, metallic, roughness, normal, viewDir, lightDir);
            color += baseColor.rgb * ambientColor;
            
            #ifdef EMISSIVE_TEXTURE
            vec3 emissive = emissiveFactor * texture(emissiveTexture, texCoord).rgb;
            #else
            vec3 emissive = emissiveFactor;
            #endif
            color += emissive;
            
            #ifdef OCCLUSION_TEXTURE
            float occlusion = texture(occlusionTexture, texCoord).r;
            color *= occlusion;
            #endif
            
            fragColor = vec4(color, baseColor.a);
        }
    """
    
    /**
     * Get transparent fragment shader source
     */
    private fun getTransparentFragmentShader(): String = """
        #version 300 es
        precision mediump float;
        
        uniform vec4 baseColorFactor;
        
        #ifdef BASE_COLOR_TEXTURE
        uniform sampler2D baseColorTexture;
        #endif
        
        uniform vec3 lightDirection;
        uniform vec3 lightColor;
        uniform vec3 ambientColor;
        
        in vec3 worldPosition;
        in vec3 worldNormal;
        in vec2 texCoord;
        in vec4 vertexColor;
        
        out vec4 fragColor;
        
        void main() {
            vec4 baseColor = baseColorFactor * vertexColor;
            
            #ifdef BASE_COLOR_TEXTURE
            baseColor *= texture(baseColorTexture, texCoord);
            #endif
            
            vec3 normal = normalize(worldNormal);
            vec3 lightDir = normalize(-lightDirection);
            float NdotL = max(dot(normal, lightDir), 0.0);
            
            vec3 color = baseColor.rgb * (ambientColor + lightColor * NdotL);
            
            fragColor = vec4(color, baseColor.a);
        }
    """
    
    /**
     * Get cutout fragment shader source
     */
    private fun getCutoutFragmentShader(): String = """
        #version 300 es
        precision mediump float;
        
        uniform vec4 baseColorFactor;
        uniform float alphaCutoff;
        
        #ifdef BASE_COLOR_TEXTURE
        uniform sampler2D baseColorTexture;
        #endif
        
        uniform vec3 lightDirection;
        uniform vec3 lightColor;
        uniform vec3 ambientColor;
        
        in vec3 worldPosition;
        in vec3 worldNormal;
        in vec2 texCoord;
        in vec4 vertexColor;
        
        out vec4 fragColor;
        
        void main() {
            vec4 baseColor = baseColorFactor * vertexColor;
            
            #ifdef BASE_COLOR_TEXTURE
            baseColor *= texture(baseColorTexture, texCoord);
            #endif
            
            if (baseColor.a < alphaCutoff) {
                discard;
            }
            
            vec3 normal = normalize(worldNormal);
            vec3 lightDir = normalize(-lightDirection);
            float NdotL = max(dot(normal, lightDir), 0.0);
            
            vec3 color = baseColor.rgb * (ambientColor + lightColor * NdotL);
            
            fragColor = vec4(color, 1.0);
        }
    """
    
    /**
     * Get unlit vertex shader source
     */
    private fun getUnlitVertexShader(): String = """
        #version 300 es
        
        layout(location = 0) in vec3 position;
        layout(location = 2) in vec2 uv0;
        layout(location = 3) in vec4 color;
        
        #ifdef SKINNING
        layout(location = 4) in vec4 boneWeights;
        layout(location = 5) in ivec4 boneIndices;
        uniform mat4 boneMatrices[64];
        #endif
        
        uniform mat4 modelMatrix;
        uniform mat4 viewMatrix;
        uniform mat4 projectionMatrix;
        
        out vec2 texCoord;
        out vec4 vertexColor;
        
        void main() {
            vec3 pos = position;
            
            #ifdef SKINNING
            mat4 boneTransform = mat4(0.0);
            boneTransform += boneMatrices[boneIndices.x] * boneWeights.x;
            boneTransform += boneMatrices[boneIndices.y] * boneWeights.y;
            boneTransform += boneMatrices[boneIndices.z] * boneWeights.z;
            boneTransform += boneMatrices[boneIndices.w] * boneWeights.w;
            
            pos = (boneTransform * vec4(pos, 1.0)).xyz;
            #endif
            
            texCoord = uv0;
            vertexColor = color;
            
            gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);
        }
    """
    
    /**
     * Get unlit fragment shader source
     */
    private fun getUnlitFragmentShader(): String = """
        #version 300 es
        precision mediump float;
        
        uniform vec4 baseColorFactor;
        
        #ifdef BASE_COLOR_TEXTURE
        uniform sampler2D baseColorTexture;
        #endif
        
        in vec2 texCoord;
        in vec4 vertexColor;
        
        out vec4 fragColor;
        
        void main() {
            vec4 baseColor = baseColorFactor * vertexColor;
            
            #ifdef BASE_COLOR_TEXTURE
            baseColor *= texture(baseColorTexture, texCoord);
            #endif
            
            fragColor = baseColor;
        }
    """
    
    /**
     * Clear shader cache
     */
    fun clearCache() {
        Log.d(TAG, "Clearing shader cache")
        shaderCache.clear()
    }
    
    /**
     * Get shader statistics
     */
    fun getStatistics(): ShaderManagerStatistics {
        return ShaderManagerStatistics(
            cachedShaders = shaderCache.size,
            availableVariants = shaderVariants.size
        )
    }
}

/**
 * Shader variant definition
 */
data class ShaderVariant(
    val name: String,
    val vertexShader: String,
    val fragmentShader: String,
    val features: Set<String>
)

/**
 * Compiled shader
 */
data class CompiledShader(
    val name: String,
    val vertexShader: String,
    val fragmentShader: String,
    val features: Set<String>,
    val compiled: Boolean
)

/**
 * Shader manager statistics
 */
data class ShaderManagerStatistics(
    val cachedShaders: Int,
    val availableVariants: Int
)