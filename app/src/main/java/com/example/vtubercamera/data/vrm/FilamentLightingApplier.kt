package com.example.vtubercamera.data.vrm

import android.util.Log
import com.google.ar.core.LightEstimate
import javax.inject.Inject

class FilamentLightingApplier @Inject constructor() {
    fun applyLighting(
        tag: String,
        lightingSystem: LightingSystem,
        shadowSystem: ShadowSystem,
        lightEstimate: LightEstimate,
    ) {
        try {
            lightingSystem.updateEnvironmentLighting(lightEstimate)

            val cameraPosition = floatArrayOf(0f, 0f, 5f)
            val cameraTarget = floatArrayOf(0f, 0f, 0f)
            shadowSystem.updateShadows(
                lightingSystem.finalLightingParameters.value,
                cameraPosition,
                cameraTarget,
            )

            Log.d(tag, "Updated lighting - Intensity: ${lightEstimate.pixelIntensity}")
        } catch (e: Exception) {
            Log.e(tag, "Error setting lighting", e)
        }
    }
}
