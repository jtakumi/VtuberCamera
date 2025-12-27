package com.example.vtubercamera.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vtubercamera.R
import com.example.vtubercamera.ui.components.LightingControlPanel
import com.example.vtubercamera.ui.viewmodels.ARLightingDemoViewModel
import java.util.Locale

/**
 * Demo screen showing the lighting and shadow system integration
 * This demonstrates how the lighting controls would be used in the AR camera
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARLightingDemoScreen(
    modifier: Modifier = Modifier,
    viewModel: ARLightingDemoViewModel = hiltViewModel()
) {
    val lightingSettings by viewModel.lightingSettings.collectAsStateWithLifecycle()
    val lightingPresets by viewModel.lightingPresets.collectAsStateWithLifecycle()
    val environmentLighting by viewModel.environmentLighting.collectAsStateWithLifecycle()
    val isLightingPanelExpanded by viewModel.isLightingPanelExpanded.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.lighting_controls),
            style = MaterialTheme.typography.headlineMedium
        )

        // Environment lighting info
        environmentLighting?.let { env ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Environment Lighting",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Intensity: ${
                            String.format(
                                Locale.getDefault(),
                                "%.2f",
                                env.intensity
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Color Temperature: ${env.colorTemperature.toInt()}K",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ambient: ${
                            String.format(
                                Locale.getDefault(),
                                "%.2f",
                                env.ambientIntensity
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Lighting control panel
        LightingControlPanel(
            lightingSettings = lightingSettings,
            lightingPresets = lightingPresets,
            isExpanded = isLightingPanelExpanded,
            onExpandedChange = viewModel::setLightingPanelExpanded,
            onLightingSettingsChange = viewModel::updateLightingSettings,
            onPresetSelected = viewModel::selectLightingPreset
        )

        // Current settings display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Brightness: ${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            lightingSettings.brightness
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Color Temperature: ${lightingSettings.colorTemperature.toInt()}K",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Shadow Strength: ${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            lightingSettings.shadowStrength
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Ambient Intensity: ${
                        String.format(
                            Locale.getDefault(),
                            "%.1f",
                            lightingSettings.ambientIntensity
                        )
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Auto Adjustment: ${if (lightingSettings.autoAdjustment) "ON" else "OFF"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Demo buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = viewModel::simulateBrightEnvironment,
                modifier = Modifier.weight(1f)
            ) {
                Text("Bright Environment")
            }

            Button(
                onClick = viewModel::simulateDarkEnvironment,
                modifier = Modifier.weight(1f)
            ) {
                Text("Dark Environment")
            }
        }

        Button(
            onClick = viewModel::resetToDefaults,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset to Defaults")
        }
    }
}