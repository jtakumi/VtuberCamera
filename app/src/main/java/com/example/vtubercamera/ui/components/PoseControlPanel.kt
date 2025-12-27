package com.example.vtubercamera.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.PoseData
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.math.Vector3
import java.util.Locale

/**
 * Pose control panel UI component for VRM avatar pose management
 * Provides pose selection, bone manipulation, and real-time preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseControlPanel(
    poseData: PoseData,
    currentPose: Pose?,
    activeBoneTransforms: Map<String, Transform>,
    boneLocks: Set<String>,
    isTransitioning: Boolean,
    transitionProgress: Float,
    onPoseSelected: (Pose?) -> Unit,
    onBoneTransformChanged: (String, Transform) -> Unit,
    onBoneLockToggled: (String) -> Unit,
    onTransitionDurationChanged: (Float) -> Unit,
    onResetToDefaultPose: () -> Unit,
    onClearPose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(Pose.PoseCategory.GENERAL) }
    var showBoneEditor by remember { mutableStateOf(false) }
    var transitionDuration by remember { mutableFloatStateOf(0.5f) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pose Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(
                        onClick = { showBoneEditor = !showBoneEditor }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Bone Editor"
                        )
                    }
                    
                    IconButton(
                        onClick = onResetToDefaultPose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to Default"
                        )
                    }
                    
                    IconButton(
                        onClick = onClearPose
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Pose"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current Pose Info
            if (currentPose != null || isTransitioning) {
                PoseStatusCard(
                    currentPose = currentPose,
                    isTransitioning = isTransitioning,
                    transitionProgress = transitionProgress,
                    activeBoneCount = activeBoneTransforms.size
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Category Selection
            PoseCategorySelector(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Pose List
            val poses = poseData.poses.filter { it.category == selectedCategory }
            PoseList(
                poses = poses,
                currentPose = currentPose,
                onPoseSelected = onPoseSelected
            )
            
            // Bone Editor
            if (showBoneEditor) {
                Spacer(modifier = Modifier.height(16.dp))
                BoneEditor(
                    activeBoneTransforms = activeBoneTransforms,
                    boneLocks = boneLocks,
                    transitionDuration = transitionDuration,
                    onBoneTransformChanged = onBoneTransformChanged,
                    onBoneLockToggled = onBoneLockToggled,
                    onTransitionDurationChanged = { duration ->
                        transitionDuration = duration
                        onTransitionDurationChanged(duration)
                    }
                )
            }
        }
    }
}

@Composable
private fun PoseStatusCard(
    currentPose: Pose?,
    isTransitioning: Boolean,
    transitionProgress: Float,
    activeBoneCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTransitioning) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Pose",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (isTransitioning) {
                    Text(
                        text = "Transitioning... ${(transitionProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = currentPose?.displayName ?: "Custom Pose",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            if (activeBoneCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$activeBoneCount bones affected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isTransitioning && transitionProgress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                progress = { transitionProgress },
                modifier = Modifier.fillMaxWidth(),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
        }
    }
}

@Composable
private fun PoseCategorySelector(
    selectedCategory: Pose.PoseCategory,
    onCategorySelected: (Pose.PoseCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(Pose.PoseCategory.entries.toTypedArray()) { category ->
            PoseCategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun PoseCategoryChip(
    category: Pose.PoseCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun PoseList(
    poses: List<Pose>,
    currentPose: Pose?,
    onPoseSelected: (Pose?) -> Unit
) {
    if (poses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No poses available in this category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(poses) { pose ->
                PoseItem(
                    pose = pose,
                    isSelected = pose == currentPose,
                    onSelected = { onPoseSelected(pose) }
                )
            }
        }
    }
}

@Composable
private fun PoseItem(
    pose: Pose,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pose.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                // Pose complexity indicator
                PoseComplexityIndicator(pose = pose)
            }
            
            if (pose.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pose.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (pose.boneTransforms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pose.boneTransforms.size} bones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (pose.isLooping) {
                        LoopingIndicator()
                    }
                    
                    if (pose.duration > 0f) {
                        Text(
                            text = "${String.format(Locale.getDefault(),"%.1f", pose.duration)}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PoseComplexityIndicator(pose: Pose) {
    val statistics = pose.getStatistics()
    val complexity = statistics.getComplexityRating()
    
    val color = when (complexity) {
        0 -> Color.Gray
        1 -> Color.Green
        2, 3 -> Color(0xFFFFA500)
        else -> Color.Red
    }
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

@Composable
private fun LoopingIndicator() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Loop",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun BoneEditor(
    activeBoneTransforms: Map<String, Transform>,
    boneLocks: Set<String>,
    transitionDuration: Float,
    onBoneTransformChanged: (String, Transform) -> Unit,
    onBoneLockToggled: (String) -> Unit,
    onTransitionDurationChanged: (Float) -> Unit
) {
    Column {
        Text(
            text = "Bone Editor",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Transition Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transition Duration",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${String.format(Locale.getDefault(),"%.1f", transitionDuration)}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = transitionDuration,
            onValueChange = onTransitionDurationChanged,
            valueRange = 0.1f..3.0f,
            steps = 29
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bone Controls
        if (activeBoneTransforms.isNotEmpty()) {
            Text(
                text = "Active Bones",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeBoneTransforms.keys.toList().sorted()) { boneName ->
                    BoneControl(
                        boneName = boneName,
                        transform = activeBoneTransforms[boneName] ?: Transform.identity(),
                        isLocked = boneLocks.contains(boneName),
                        onTransformChanged = { transform ->
                            onBoneTransformChanged(boneName, transform)
                        },
                        onLockToggled = { onBoneLockToggled(boneName) }
                    )
                }
            }
        } else {
            Text(
                text = "No active bone transforms",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoneControl(
    boneName: String,
    transform: Transform,
    isLocked: Boolean,
    onTransformChanged: (Transform) -> Unit,
    onLockToggled: () -> Unit
) {
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = boneName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onLockToggled,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isLocked) "Unlock Bone" else "Lock Bone",
                        tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            if (!isLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simple position controls (X, Y, Z)
                Text(
                    text = "Position",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AxisControl(
                        label = "X",
                        value = transform.position.x,
                        range = -2f..2f,
                        onValueChanged = { newX ->
                            val newPosition = Vector3(newX, transform.position.y, transform.position.z)
                            onTransformChanged(transform.withPosition(newPosition))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    AxisControl(
                        label = "Y",
                        value = transform.position.y,
                        range = -2f..2f,
                        onValueChanged = { newY ->
                            val newPosition = Vector3(transform.position.x, newY, transform.position.z)
                            onTransformChanged(transform.withPosition(newPosition))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    AxisControl(
                        label = "Z",
                        value = transform.position.z,
                        range = -2f..2f,
                        onValueChanged = { newZ ->
                            val newPosition = Vector3(transform.position.x, transform.position.y, newZ)
                            onTransformChanged(transform.withPosition(newPosition))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AxisControl(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "$label: ${String.format(Locale.getDefault(),"%.2f", value)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChanged,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}