package com.example.vtubercamera.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vtubercamera.data.vrm.EnvironmentLighting
import com.example.vtubercamera.data.vrm.LightingPreset
import com.example.vtubercamera.data.vrm.LightingSettings
import com.example.vtubercamera.data.vrm.LightingSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AR Lighting Demo Screen
 * Demonstrates the lighting and shadow system functionality
 */
@HiltViewModel
class ARLightingDemoViewModel @Inject constructor(
    private val lightingSystem: LightingSystem
) : ViewModel() {
    
    // Expose lighting system state
    val lightingSettings: StateFlow<LightingSettings> = lightingSystem.lightingSettings
    val environmentLighting: StateFlow<EnvironmentLighting?> = lightingSystem.environmentLighting
    
    // UI state
    private val _isLightingPanelExpanded = MutableStateFlow(true)
    val isLightingPanelExpanded: StateFlow<Boolean> = _isLightingPanelExpanded.asStateFlow()
    
    private val _lightingPresets = MutableStateFlow(lightingSystem.getLightingPresets())
    val lightingPresets: StateFlow<List<LightingPreset>> = _lightingPresets.asStateFlow()
    
    /**
     * Update lighting settings
     */
    fun updateLightingSettings(settings: LightingSettings) {
        viewModelScope.launch {
            lightingSystem.updateLightingSettings(settings)
        }
    }
    
    /**
     * Select a lighting preset
     */
    fun selectLightingPreset(preset: LightingPreset) {
        viewModelScope.launch {
            lightingSystem.updateLightingSettings(preset.settings)
        }
    }
    
    /**
     * Set lighting panel expanded state
     */
    fun setLightingPanelExpanded(expanded: Boolean) {
        _isLightingPanelExpanded.value = expanded
    }
    
    /**
     * Reset lighting settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            lightingSystem.resetToDefaults()
        }
    }
    
    /**
     * Simulate bright environment for demo
     */
    fun simulateBrightEnvironment() {
        viewModelScope.launch {
            val mockEnvironmentLighting = EnvironmentLighting(
                intensity = 0.9f,
                colorTemperature = 6500f,
                lightDirection = floatArrayOf(0.3f, -0.8f, 0.2f),
                ambientIntensity = 0.4f,
                timestamp = System.currentTimeMillis()
            )
            
            // Simulate environment lighting update
            simulateEnvironmentLighting(mockEnvironmentLighting)
        }
    }
    
    /**
     * Simulate dark environment for demo
     */
    fun simulateDarkEnvironment() {
        viewModelScope.launch {
            val mockEnvironmentLighting = EnvironmentLighting(
                intensity = 0.2f,
                colorTemperature = 3000f,
                lightDirection = floatArrayOf(0f, -1f, 0f),
                ambientIntensity = 0.1f,
                timestamp = System.currentTimeMillis()
            )
            
            // Simulate environment lighting update
            simulateEnvironmentLighting(mockEnvironmentLighting)
        }
    }
    
    /**
     * Simulate environment lighting update
     * In real implementation, this would come from ARCore LightEstimate
     */
    private fun simulateEnvironmentLighting(environmentLighting: EnvironmentLighting) {
        // Create a mock LightEstimate-like object
        // In real implementation, this would be an actual ARCore LightEstimate
        val mockLightEstimate = MockLightEstimate(environmentLighting.intensity)
        
        // Update lighting system (this would normally be called from ARRenderer)
        // For demo purposes, we'll directly update the environment lighting
        // lightingSystem.updateEnvironmentLighting(mockLightEstimate)
        
        // For demo, we'll manually trigger the lighting update logic
        if (lightingSettings.value.autoAdjustment) {
            val adjustedSettings = calculateAutoAdjustedSettings(environmentLighting)
            lightingSystem.updateLightingSettings(adjustedSettings)
        }
    }
    
    /**
     * Calculate auto-adjusted settings based on environment
     * This simulates the auto-adjustment logic from LightingSystem
     */
    private fun calculateAutoAdjustedSettings(environmentLighting: EnvironmentLighting): LightingSettings {
        val currentSettings = lightingSettings.value
        
        // Inverse relationship: darker environment needs brighter avatar lighting
        val adjustedBrightness = (1.0f + (1f - environmentLighting.intensity) * 0.5f).coerceIn(0.1f, 3.0f)
        
        // Stronger shadows in brighter environments
        val adjustedShadowStrength = (environmentLighting.intensity * 0.8f).coerceIn(0f, 1f)
        
        return currentSettings.copy(
            brightness = adjustedBrightness,
            colorTemperature = environmentLighting.colorTemperature,
            shadowStrength = adjustedShadowStrength,
            ambientIntensity = environmentLighting.ambientIntensity
        )
    }
}

/**
 * Mock LightEstimate for demo purposes
 * In real implementation, this would be the actual ARCore LightEstimate
 */
private class MockLightEstimate(private val intensity: Float) {
    val pixelIntensity: Float get() = intensity
    val environmentalHdrMainLightDirection: FloatArray? get() = null
}