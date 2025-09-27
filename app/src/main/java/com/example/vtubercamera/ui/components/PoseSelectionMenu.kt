package com.example.vtubercamera.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.PoseData

/**
 * Compact dropdown menu for selecting a pose.
 */
@Composable
fun PoseSelectionMenu(
    poseData: PoseData,
    currentPose: Pose?,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onPoseSelected: (Pose?) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        // Clear item
        DropdownMenuItem(
            text = { Text("Clear Pose") },
            onClick = { onPoseSelected(null) }
        )

        // Group by category
        Pose.PoseCategory.values().forEach { category ->
            val items = poseData.poses.filter { it.category == category }
            if (items.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {},
                    enabled = false
                )
                items.forEach { pose ->
                    DropdownMenuItem(
                        text = { Text(if (pose == currentPose) "✔ ${pose.displayName}" else pose.displayName) },
                        onClick = { onPoseSelected(pose) }
                    )
                }
            }
        }
    }
}

