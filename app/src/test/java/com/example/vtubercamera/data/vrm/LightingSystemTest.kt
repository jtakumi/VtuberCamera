package com.example.vtubercamera.data.vrm

import com.google.ar.core.LightEstimate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for LightingSystem
 */
class LightingSystemTest {
    
    @Mock
    private lateinit var mockLightEstimate: LightEstimate
    
    private lateinit var lightingSystem: LightingSystem
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        lightingSystem = LightingSystem()
    }
    
    @Test
    fun `initial lighting settings should have default values`() = runTest {
        val settings = lightingSystem.lightingSettings.first()
        
        assertEquals(1.0f, settings.brightness)
        assertEquals(6500f, settings.colorTemperature)
        assertEquals(0.5f, settings.shadowStrength)
        assertEquals(0.2f, settings.ambientIntensity)
        assertTrue(settings.autoAdjustment)
    }
    
    @Test
    fun `updateLightingSettings should clamp values to valid ranges`() = runTest {
        val invalidSettings = LightingSettings(
            brightness = 10.0f, // Above max
            colorTemperature = 1000f, // Below min
            shadowStrength = 2.0f, // Above max
            ambientIntensity = -0.5f, // Below min
            autoAdjustment = false
        )
        
        lightingSystem.updateLightingSettings(invalidSettings)
        val settings = lightingSystem.lightingSettings.first()
        
        assertTrue(settings.brightness <= 3.0f)
        assertTrue(settings.colorTemperature >= 2000f)
        assertTrue(settings.shadowStrength <= 1.0f)
        assertTrue(settings.ambientIntensity >= 0f)
    }
    
    @Test
    fun `updateEnvironmentLighting should create environment lighting data`() = runTest {
        whenever(mockLightEstimate.pixelIntensity).thenReturn(0.8f)
        whenever(mockLightEstimate.environmentalHdrMainLightDirection).thenReturn(null)
        
        lightingSystem.updateEnvironmentLighting(mockLightEstimate)
        
        val environmentLighting = lightingSystem.environmentLighting.first()
        assertNotNull(environmentLighting)
        assertEquals(0.8f, environmentLighting.intensity)
        assertTrue(environmentLighting.colorTemperature > 0f)
    }
    
    @Test
    fun `auto adjustment should modify settings based on environment`() = runTest {
        // Enable auto adjustment
        lightingSystem.setAutoAdjustment(true)
        
        // Simulate dark environment
        whenever(mockLightEstimate.pixelIntensity).thenReturn(0.2f)
        whenever(mockLightEstimate.environmentalHdrMainLightDirection).thenReturn(null)
        
        val initialSettings = lightingSystem.lightingSettings.first()
        lightingSystem.updateEnvironmentLighting(mockLightEstimate)
        val adjustedSettings = lightingSystem.lightingSettings.first()
        
        // In dark environment, brightness should increase
        assertTrue(adjustedSettings.brightness >= initialSettings.brightness)
    }
    
    @Test
    fun `lighting presets should contain expected presets`() {
        val presets = lightingSystem.getLightingPresets()
        
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.any { it.name == "Daylight" })
        assertTrue(presets.any { it.name == "Indoor" })
        assertTrue(presets.any { it.name == "Golden Hour" })
        assertTrue(presets.any { it.name == "Studio" })
        assertTrue(presets.any { it.name == "Auto" })
    }
    
    @Test
    fun `final lighting parameters should be updated when settings change`() = runTest {
        val newSettings = LightingSettings(
            brightness = 2.0f,
            colorTemperature = 3000f,
            shadowStrength = 0.8f,
            ambientIntensity = 0.4f,
            autoAdjustment = false
        )
        
        lightingSystem.updateLightingSettings(newSettings)
        val finalParams = lightingSystem.finalLightingParameters.first()
        
        assertEquals(2.0f, finalParams.lightIntensity)
        assertEquals(0.8f, finalParams.shadowStrength)
        assertEquals(3000f, finalParams.colorTemperature)
    }
    
    @Test
    fun `resetToDefaults should restore default settings`() = runTest {
        // Change settings
        val customSettings = LightingSettings(
            brightness = 2.5f,
            colorTemperature = 3000f,
            shadowStrength = 0.8f,
            ambientIntensity = 0.6f,
            autoAdjustment = false
        )
        lightingSystem.updateLightingSettings(customSettings)
        
        // Reset to defaults
        lightingSystem.resetToDefaults()
        val settings = lightingSystem.lightingSettings.first()
        
        assertEquals(1.0f, settings.brightness)
        assertEquals(6500f, settings.colorTemperature)
        assertEquals(0.5f, settings.shadowStrength)
        assertEquals(0.2f, settings.ambientIntensity)
        assertTrue(settings.autoAdjustment)
    }
}