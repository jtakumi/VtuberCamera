package com.example.vtubercamera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vtubercamera.R
import com.example.vtubercamera.data.vrm.LightingSettings
import com.example.vtubercamera.data.vrm.LightingPreset
import kotlin.math.roundToInt
import kotlin.math.pow
import kotlin.math.ln

/**
 * Lighting control panel for adjusting avatar lighting in AR
 * Provides controls for brightness, color temperature, shadow strength, and presets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightingControlPanel(
    lightingSettings: LightingSettings,
    lightingPresets: List<LightingPreset>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLightingSettingsChange: (LightingSettings) -> Unit,
    onPresetSelected: (LightingPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.lighting_controls),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                IconButton(
                    onClick = { onExpandedChange(!isExpanded) }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Auto adjustment toggle (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auto_lighting),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = lightingSettings.autoAdjustment,
                    onCheckedChange = { enabled ->
                        onLightingSettingsChange(lightingSettings.copy(autoAdjustment = enabled))
                    }
                )
            }
            
            // Expanded controls
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Lighting presets
                    LightingPresetSelector(
                        presets = lightingPresets,
                        currentSettings = lightingSettings,
                        onPresetSelected = onPresetSelected,
                        enabled = !lightingSettings.autoAdjustment
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Manual controls (disabled when auto adjustment is on)
                    LightingManualControls(
                        settings = lightingSettings,
                        onSettingsChange = onLightingSettingsChange,
                        enabled = !lightingSettings.autoAdjustment
                    )
                }
            }
        }
    }
}

/**
 * Lighting preset selector
 */
@Composable
private fun LightingPresetSelector(
    presets: List<LightingPreset>,
    currentSettings: LightingSettings,
    onPresetSelected: (LightingPreset) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.lighting_presets),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { preset ->
                LightingPresetChip(
                    preset = preset,
                    isSelected = isPresetSelected(preset, currentSettings),
                    onSelected = { onPresetSelected(preset) },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * Individual lighting preset chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LightingPresetChip(
    preset: LightingPreset,
    isSelected: Boolean,
    onSelected: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onSelected,
        label = {
            Text(
                text = preset.name,
                fontSize = 12.sp
            )
        },
        selected = isSelected,
        enabled = enabled,
        modifier = modifier,
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

/**
 * Manual lighting controls
 */
@Composable
private fun LightingManualControls(
    settings: LightingSettings,
    onSettingsChange: (LightingSettings) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.manual_controls),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Brightness control
        LightingSliderControl(
            label = stringResource(R.string.brightness),
            value = settings.brightness,
            valueRange = 0.1f..3.0f,
            steps = 29, // 0.1 to 3.0 in 0.1 increments
            onValueChange = { brightness ->
                onSettingsChange(settings.copy(brightness = brightness))
            },
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            icon = Icons.Default.Brightness6,
            enabled = enabled
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Color temperature control
        LightingSliderControl(
            label = stringResource(R.string.color_temperature),
            value = settings.colorTemperature,
            valueRange = 2000f..10000f,
            steps = 80, // 2000 to 10000 in 100K increments
            onValueChange = { temperature ->
                onSettingsChange(settings.copy(colorTemperature = temperature))
            },
            valueFormatter = { "${it.roundToInt()}K" },
            icon = Icons.Default.Thermostat,
            enabled = enabled
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Shadow strength control
        LightingSliderControl(
            label = stringResource(R.string.shadow_strength),
            value = settings.shadowStrength,
            valueRange = 0f..1f,
            steps = 10, // 0 to 1 in 0.1 increments
            onValueChange = { shadowStrength ->
                onSettingsChange(settings.copy(shadowStrength = shadowStrength))
            },
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            icon = Icons.Default.Opacity,
            enabled = enabled
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Ambient intensity control
        LightingSliderControl(
            label = stringResource(R.string.ambient_light),
            value = settings.ambientIntensity,
            valueRange = 0f..1f,
            steps = 10, // 0 to 1 in 0.1 increments
            onValueChange = { ambientIntensity ->
                onSettingsChange(settings.copy(ambientIntensity = ambientIntensity))
            },
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            icon = Icons.Default.WbTwilight,
            enabled = enabled
        )
    }
}

/**
 * Individual slider control for lighting parameters
 */
@Composable
private fun LightingSliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Color temperature indicator
 */
@Composable
private fun ColorTemperatureIndicator(
    temperature: Float,
    modifier: Modifier = Modifier
) {
    val color = colorTemperatureToColor(temperature)
    
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

/**
 * Check if a preset matches current settings
 */
private fun isPresetSelected(preset: LightingPreset, currentSettings: LightingSettings): Boolean {
    val settings = preset.settings
    return kotlin.math.abs(settings.brightness - currentSettings.brightness) < 0.1f &&
            kotlin.math.abs(settings.colorTemperature - currentSettings.colorTemperature) < 100f &&
            kotlin.math.abs(settings.shadowStrength - currentSettings.shadowStrength) < 0.1f &&
            kotlin.math.abs(settings.ambientIntensity - currentSettings.ambientIntensity) < 0.1f &&
            settings.autoAdjustment == currentSettings.autoAdjustment
}

/**
 * Convert color temperature to display color
 */
private fun colorTemperatureToColor(temperature: Float): Color {
    val temp = temperature / 100f
    
    val red = when {
        temp <= 66f -> 1f
        else -> {
            val r = temp - 60f
            val red = 329.698727446f * r.pow(-0.1332047592f)
            (red / 255f).coerceIn(0f, 1f)
        }
    }
    
    val green = when {
        temp <= 66f -> {
            val g = 99.4708025861f * ln(temp) - 161.1195681661f
            (g / 255f).coerceIn(0f, 1f)
        }
        else -> {
            val g = temp - 60f
            val green = 288.1221695283f * g.pow(-0.0755148492f)
            (green / 255f).coerceIn(0f, 1f)
        }
    }
    
    val blue = when {
        temp >= 66f -> 1f
        temp <= 19f -> 0f
        else -> {
            val b = temp - 10f
            val blue = 138.5177312231f * ln(b) - 305.0447927307f
            (blue / 255f).coerceIn(0f, 1f)
        }
    }
    
    return Color(red, green, blue)
}