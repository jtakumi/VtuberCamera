package com.example.vtubercamera.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vtubercamera.data.vrm.math.Quaternion
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.math.Vector3
import kotlin.math.PI

/**
 * Panel to adjust avatar transform: position, rotation, and scale
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarTransformPanel(
    transform: Transform,
    onTransformChange: (Transform) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local UI state mirrors provided transform; updates propagate via callback
    // Position (meters)
    var posX by remember(transform) { mutableStateOf(transform.position.x) }
    var posY by remember(transform) { mutableStateOf(transform.position.y) }
    var posZ by remember(transform) { mutableStateOf(transform.position.z) }

    // Rotation in degrees (convert from quaternion -> euler radians -> degrees)
    val eulerRad = remember(transform) { transform.rotation.toEuler() }
    var pitchDeg by remember(transform) { mutableStateOf(eulerRad.x * 180f / PI.toFloat()) }
    var yawDeg by remember(transform) { mutableStateOf(eulerRad.y * 180f / PI.toFloat()) }
    var rollDeg by remember(transform) { mutableStateOf(eulerRad.z * 180f / PI.toFloat()) }

    // Uniform scale factor
    var uniformScale by remember(transform) { mutableStateOf(transform.scale.x.coerceIn(0.05f, 5f)) }

    fun emitChange() {
        val newPos = Vector3(posX, posY, posZ)
        val newRot = Quaternion.fromEuler(
            pitch = pitchDeg * PI.toFloat() / 180f,
            yaw = yawDeg * PI.toFloat() / 180f,
            roll = rollDeg * PI.toFloat() / 180f
        )
        val newScale = Vector3(uniformScale, uniformScale, uniformScale)
        onTransformChange(Transform(position = newPos, rotation = newRot, scale = newScale))
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avatar Transform",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onReset) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset Transform")
                }
            }

            // Position controls
            Text(text = "Position (m)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            AxisSlider(
                label = "X",
                value = posX,
                range = -2f..2f,
                onChange = { posX = it; emitChange() }
            )
            AxisSlider(
                label = "Y",
                value = posY,
                range = -2f..2f,
                onChange = { posY = it; emitChange() }
            )
            AxisSlider(
                label = "Z",
                value = posZ,
                range = -2f..2f,
                onChange = { posZ = it; emitChange() }
            )

            // Rotation controls
            Text(text = "Rotation (deg)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            AxisSlider(
                label = "Pitch",
                value = pitchDeg,
                range = -180f..180f,
                onChange = { pitchDeg = it; emitChange() }
            )
            AxisSlider(
                label = "Yaw",
                value = yawDeg,
                range = -180f..180f,
                onChange = { yawDeg = it; emitChange() }
            )
            AxisSlider(
                label = "Roll",
                value = rollDeg,
                range = -180f..180f,
                onChange = { rollDeg = it; emitChange() }
            )

            // Scale control (uniform)
            Text(text = "Scale", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = String.format("%.2f×", uniformScale), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Slider(
                value = uniformScale,
                onValueChange = { uniformScale = it; emitChange() },
                valueRange = 0.1f..3.0f,
                steps = 28,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AxisSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label: ${String.format("%.2f", value)}", style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

