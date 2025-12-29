package com.example.vtubercamera.domain.lighting

import com.example.vtubercamera.data.vrm.EnvironmentLighting
import com.example.vtubercamera.data.vrm.LightingPreset
import com.example.vtubercamera.data.vrm.LightingSettings
import com.example.vtubercamera.data.vrm.LightingSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class LightingFeature @Inject constructor(
    private val lightingSystem: LightingSystem,
) {

    val lightingSettings: StateFlow<LightingSettings> = lightingSystem.lightingSettings

    val environmentLighting: StateFlow<EnvironmentLighting?> = lightingSystem.environmentLighting

    fun updateLightingSettings(scope: CoroutineScope, settings: LightingSettings) {
        scope.launch {
            lightingSystem.updateLightingSettings(settings)
        }
    }

    fun selectLightingPreset(scope: CoroutineScope, preset: LightingPreset) {
        scope.launch {
            lightingSystem.updateLightingSettings(preset.settings)
        }
    }

    fun resetLightingToDefaults(scope: CoroutineScope) {
        scope.launch {
            lightingSystem.resetToDefaults()
        }
    }

    fun getLightingPresets(): List<LightingPreset> = lightingSystem.getLightingPresets()

    fun getCurrentLightingPresetName(currentSettings: LightingSettings): String? {
        return getLightingPresets().find { preset ->
            preset.settings == currentSettings
        }?.name
    }
}
