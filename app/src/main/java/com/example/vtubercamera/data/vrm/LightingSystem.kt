package com.example.vtubercamera.data.vrm

import android.util.Log
import com.google.ar.core.LightEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Lighting system for VRM avatars in AR
 * Handles environment light detection, manual lighting controls, and automatic adjustment
 */
@Singleton
class LightingSystem @Inject constructor() {
    
    companion object {
        private const val TAG = "LightingSystem"
        
        // Default lighting values
        private const val DEFAULT_BRIGHTNESS = 1.0f
        private const val DEFAULT_COLOR_TEMPERATURE = 6500f // Daylight
        private const val DEFAULT_SHADOW_STRENGTH = 0.5f
        private const val DEFAULT_AMBIENT_INTENSITY = 0.2f
        
        // Color temperature ranges
        private const val MIN_COLOR_TEMPERATURE = 2000f // Warm candlelight
        private const val MAX_COLOR_TEMPERATURE = 10000f // Cool blue sky
        
        // Brightness ranges
        private const val MIN_BRIGHTNESS = 0.1f
        private const val MAX_BRIGHTNESS = 3.0f
        
        // Shadow strength ranges
        private const val MIN_SHADOW_STRENGTH = 0.0f
        private const val MAX_SHADOW_STRENGTH = 1.0f
        
        // Auto adjustment parameters
        private const val AUTO_ADJUSTMENT_SMOOTHING = 0.1f
    }
    
    // Current lighting state
    private val _lightingSettings = MutableStateFlow(
        LightingSettings(
            brightness = DEFAULT_BRIGHTNESS,
            colorTemperature = DEFAULT_COLOR_TEMPERATURE,
            shadowStrength = DEFAULT_SHADOW_STRENGTH,
            ambientIntensity = DEFAULT_AMBIENT_INTENSITY,
            autoAdjustment = true
        )
    )
    val lightingSettings: StateFlow<LightingSettings> = _lightingSettings.asStateFlow()
    
    private val _environmentLighting = MutableStateFlow<EnvironmentLighting?>(null)
    val environmentLighting: StateFlow<EnvironmentLighting?> = _environmentLighting.asStateFlow()
    
    private val _finalLightingParameters = MutableStateFlow(
        createDefaultLightingParameters()
    )
    val finalLightingParameters: StateFlow<LightingParameters> = _finalLightingParameters.asStateFlow()
    
    // Auto adjustment state
    private var lastEnvironmentIntensity = 0f
    private var lastColorTemperature = DEFAULT_COLOR_TEMPERATURE
    
    /**
     * Update environment lighting from ARCore light estimate
     */
    fun updateEnvironmentLighting(lightEstimate: LightEstimate) {
        try {
            val environmentLighting = analyzeEnvironmentLighting(lightEstimate)
            _environmentLighting.value = environmentLighting
            
            Log.d(TAG, "Environment lighting updated - Intensity: ${environmentLighting.intensity}, " +
                    "Temperature: ${environmentLighting.colorTemperature}K")
            
            // Apply automatic adjustment if enabled
            if (_lightingSettings.value.autoAdjustment) {
                applyAutomaticAdjustment(environmentLighting)
            }
            
            // Update final lighting parameters
            updateFinalLightingParameters()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating environment lighting", e)
        }
    }
    
    /**
     * Update manual lighting settings
     */
    fun updateLightingSettings(settings: LightingSettings) {
        val clampedSettings = settings.copy(
            brightness = settings.brightness.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS),
            colorTemperature = settings.colorTemperature.coerceIn(MIN_COLOR_TEMPERATURE, MAX_COLOR_TEMPERATURE),
            shadowStrength = settings.shadowStrength.coerceIn(MIN_SHADOW_STRENGTH, MAX_SHADOW_STRENGTH),
            ambientIntensity = settings.ambientIntensity.coerceIn(0f, 1f)
        )
        
        _lightingSettings.value = clampedSettings
        updateFinalLightingParameters()
        
        Log.d(TAG, "Lighting settings updated: $clampedSettings")
    }
    
    /**
     * Reset lighting settings to defaults
     */
    fun resetToDefaults() {
        updateLightingSettings(
            LightingSettings(
                brightness = DEFAULT_BRIGHTNESS,
                colorTemperature = DEFAULT_COLOR_TEMPERATURE,
                shadowStrength = DEFAULT_SHADOW_STRENGTH,
                ambientIntensity = DEFAULT_AMBIENT_INTENSITY,
                autoAdjustment = true
            )
        )
    }
    
    /**
     * Enable or disable automatic lighting adjustment
     */
    fun setAutoAdjustment(enabled: Boolean) {
        val currentSettings = _lightingSettings.value
        updateLightingSettings(currentSettings.copy(autoAdjustment = enabled))
    }
    
    /**
     * Get lighting presets for common scenarios
     */
    fun getLightingPresets(): List<LightingPreset> {
        return listOf(
            LightingPreset(
                name = "Daylight",
                settings = LightingSettings(
                    brightness = 1.2f,
                    colorTemperature = 6500f,
                    shadowStrength = 0.6f,
                    ambientIntensity = 0.3f,
                    autoAdjustment = false
                )
            ),
            LightingPreset(
                name = "Indoor",
                settings = LightingSettings(
                    brightness = 0.8f,
                    colorTemperature = 3000f,
                    shadowStrength = 0.4f,
                    ambientIntensity = 0.4f,
                    autoAdjustment = false
                )
            ),
            LightingPreset(
                name = "Golden Hour",
                settings = LightingSettings(
                    brightness = 1.0f,
                    colorTemperature = 2500f,
                    shadowStrength = 0.8f,
                    ambientIntensity = 0.2f,
                    autoAdjustment = false
                )
            ),
            LightingPreset(
                name = "Studio",
                settings = LightingSettings(
                    brightness = 1.5f,
                    colorTemperature = 5500f,
                    shadowStrength = 0.3f,
                    ambientIntensity = 0.5f,
                    autoAdjustment = false
                )
            ),
            LightingPreset(
                name = "Auto",
                settings = LightingSettings(
                    brightness = DEFAULT_BRIGHTNESS,
                    colorTemperature = DEFAULT_COLOR_TEMPERATURE,
                    shadowStrength = DEFAULT_SHADOW_STRENGTH,
                    ambientIntensity = DEFAULT_AMBIENT_INTENSITY,
                    autoAdjustment = true
                )
            )
        )
    }
    
    // Private helper methods
    
    /**
     * Analyze environment lighting from ARCore light estimate
     */
    private fun analyzeEnvironmentLighting(lightEstimate: LightEstimate): EnvironmentLighting {
        val intensity = lightEstimate.pixelIntensity
        
        // Estimate color temperature based on intensity and time of day
        // This is a simplified estimation - in a real app you might use more sophisticated methods
        val estimatedColorTemperature = estimateColorTemperature(intensity)
        
        // Calculate directional light from spherical harmonics if available
        val lightDirection = if (lightEstimate.environmentalHdrMainLightDirection != null) {
            val direction = lightEstimate.environmentalHdrMainLightDirection
            floatArrayOf(direction[0], direction[1], direction[2])
        } else {
            // Default overhead lighting
            floatArrayOf(0f, -1f, 0f)
        }
        
        // Calculate ambient lighting
        val ambientIntensity = (intensity * 0.2f).coerceIn(0f, 1f)
        
        return EnvironmentLighting(
            intensity = intensity,
            colorTemperature = estimatedColorTemperature,
            lightDirection = lightDirection,
            ambientIntensity = ambientIntensity,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Estimate color temperature from light intensity
     */
    private fun estimateColorTemperature(intensity: Float): Float {
        // Simple heuristic: higher intensity often correlates with cooler light (daylight)
        // Lower intensity often correlates with warmer light (indoor/artificial)
        return when {
            intensity > 0.8f -> 6500f // Bright daylight
            intensity > 0.5f -> 5500f // Overcast daylight
            intensity > 0.3f -> 4000f // Indoor fluorescent
            else -> 3000f // Warm indoor lighting
        }.coerceIn(MIN_COLOR_TEMPERATURE, MAX_COLOR_TEMPERATURE)
    }
    
    /**
     * Apply automatic lighting adjustment based on environment
     */
    private fun applyAutomaticAdjustment(environmentLighting: EnvironmentLighting) {
        val currentSettings = _lightingSettings.value
        
        // Smooth adjustment to prevent flickering
        val targetIntensity = lerp(lastEnvironmentIntensity, environmentLighting.intensity, AUTO_ADJUSTMENT_SMOOTHING)
        val targetColorTemperature = lerp(lastColorTemperature, environmentLighting.colorTemperature, AUTO_ADJUSTMENT_SMOOTHING)
        
        // Calculate adjusted brightness based on environment
        val adjustedBrightness = calculateAutoBrightness(targetIntensity)
        
        // Calculate adjusted shadow strength based on light intensity
        val adjustedShadowStrength = calculateAutoShadowStrength(targetIntensity)
        
        // Update settings with automatic adjustments
        val autoAdjustedSettings = currentSettings.copy(
            brightness = adjustedBrightness,
            colorTemperature = targetColorTemperature,
            shadowStrength = adjustedShadowStrength,
            ambientIntensity = environmentLighting.ambientIntensity
        )
        
        _lightingSettings.value = autoAdjustedSettings
        
        // Update tracking variables
        lastEnvironmentIntensity = targetIntensity
        lastColorTemperature = targetColorTemperature
        
        Log.d(TAG, "Auto adjustment applied - Brightness: $adjustedBrightness, " +
                "Temperature: ${targetColorTemperature}K, Shadow: $adjustedShadowStrength")
    }
    
    /**
     * Calculate automatic brightness adjustment
     */
    private fun calculateAutoBrightness(environmentIntensity: Float): Float {
        // Inverse relationship: darker environment needs brighter avatar lighting
        val baseBrightness = DEFAULT_BRIGHTNESS
        val adjustment = (1f - environmentIntensity) * 0.5f
        return (baseBrightness + adjustment).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }
    
    /**
     * Calculate automatic shadow strength
     */
    private fun calculateAutoShadowStrength(environmentIntensity: Float): Float {
        // Stronger shadows in brighter environments
        return (environmentIntensity * 0.8f).coerceIn(MIN_SHADOW_STRENGTH, MAX_SHADOW_STRENGTH)
    }
    
    /**
     * Update final lighting parameters for rendering
     */
    private fun updateFinalLightingParameters() {
        val settings = _lightingSettings.value
        val environment = _environmentLighting.value
        
        // Convert color temperature to RGB
        val lightColor = colorTemperatureToRGB(settings.colorTemperature)
        
        // Apply brightness
        val finalLightColor = floatArrayOf(
            lightColor[0] * settings.brightness,
            lightColor[1] * settings.brightness,
            lightColor[2] * settings.brightness
        )
        
        // Calculate ambient color
        val ambientColor = floatArrayOf(
            finalLightColor[0] * settings.ambientIntensity,
            finalLightColor[1] * settings.ambientIntensity,
            finalLightColor[2] * settings.ambientIntensity
        )
        
        // Use environment light direction if available, otherwise default
        val lightDirection = environment?.lightDirection ?: floatArrayOf(0f, -1f, 0f)
        
        val finalParameters = LightingParameters(
            lightDirection = lightDirection,
            lightColor = finalLightColor,
            lightIntensity = settings.brightness,
            ambientColor = ambientColor,
            cameraPosition = floatArrayOf(0f, 0f, 5f),
            shadowStrength = settings.shadowStrength,
            colorTemperature = settings.colorTemperature
        )
        
        _finalLightingParameters.value = finalParameters
    }
    
    /**
     * Convert color temperature to RGB values
     */
    private fun colorTemperatureToRGB(temperature: Float): FloatArray {
        val temp = temperature / 100f
        
        val red = when {
            temp <= 66f -> 1f
            else -> {
                val r = temp - 60f
                val red = 329.69873f * r.pow(-0.13320476f)
                (red / 255f).coerceIn(0f, 1f)
            }
        }
        
        val green = when {
            temp <= 66f -> {
                val g = 99.4708f * ln(temp) - 161.11957f
                (g / 255f).coerceIn(0f, 1f)
            }
            else -> {
                val g = temp - 60f
                val green = 288.12216f * g.pow(-0.075514846f)
                (green / 255f).coerceIn(0f, 1f)
            }
        }
        
        val blue = when {
            temp >= 66f -> 1f
            temp <= 19f -> 0f
            else -> {
                val b = temp - 10f
                val blue = 138.51773f * ln(b) - 305.0448f
                (blue / 255f).coerceIn(0f, 1f)
            }
        }
        
        return floatArrayOf(red, green, blue)
    }
    
    /**
     * Linear interpolation helper
     */
    private fun lerp(start: Float, end: Float, factor: Float): Float {
        return start + factor * (end - start)
    }
    
    /**
     * Create default lighting parameters
     */
    private fun createDefaultLightingParameters(): LightingParameters {
        return LightingParameters(
            lightDirection = floatArrayOf(0f, -1f, 0f),
            lightColor = floatArrayOf(DEFAULT_BRIGHTNESS, DEFAULT_BRIGHTNESS, DEFAULT_BRIGHTNESS),
            lightIntensity = DEFAULT_BRIGHTNESS,
            ambientColor = floatArrayOf(DEFAULT_AMBIENT_INTENSITY, DEFAULT_AMBIENT_INTENSITY, DEFAULT_AMBIENT_INTENSITY),
            cameraPosition = floatArrayOf(0f, 0f, 5f)
        )
    }
}

/**
 * Lighting settings for manual control
 */
data class LightingSettings(
    val brightness: Float = 1.0f,
    val colorTemperature: Float = 6500f,
    val shadowStrength: Float = 0.5f,
    val ambientIntensity: Float = 0.2f,
    val autoAdjustment: Boolean = true
)

/**
 * Environment lighting analysis result
 */
data class EnvironmentLighting(
    val intensity: Float,
    val colorTemperature: Float,
    val lightDirection: FloatArray,
    val ambientIntensity: Float,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EnvironmentLighting

        if (intensity != other.intensity) return false
        if (colorTemperature != other.colorTemperature) return false
        if (!lightDirection.contentEquals(other.lightDirection)) return false
        if (ambientIntensity != other.ambientIntensity) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intensity.hashCode()
        result = 31 * result + colorTemperature.hashCode()
        result = 31 * result + lightDirection.contentHashCode()
        result = 31 * result + ambientIntensity.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Lighting preset for quick settings
 */
data class LightingPreset(
    val name: String,
    val settings: LightingSettings
)

/**
 * Extended lighting parameters with shadow support
 */
data class LightingParameters(
    val lightDirection: FloatArray = floatArrayOf(0f, -1f, 0f),
    val lightColor: FloatArray = floatArrayOf(1f, 1f, 1f),
    val lightIntensity: Float = 1f,
    val ambientColor: FloatArray = floatArrayOf(0.2f, 0.2f, 0.2f),
    val cameraPosition: FloatArray = floatArrayOf(0f, 0f, 5f),
    val shadowStrength: Float = 0.5f,
    val colorTemperature: Float = 6500f
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
        if (shadowStrength != other.shadowStrength) return false
        if (colorTemperature != other.colorTemperature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lightDirection.contentHashCode()
        result = 31 * result + lightColor.contentHashCode()
        result = 31 * result + lightIntensity.hashCode()
        result = 31 * result + ambientColor.contentHashCode()
        result = 31 * result + cameraPosition.contentHashCode()
        result = 31 * result + shadowStrength.hashCode()
        result = 31 * result + colorTemperature.hashCode()
        return result
    }
}