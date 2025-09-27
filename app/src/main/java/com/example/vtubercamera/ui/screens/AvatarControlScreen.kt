package com.example.vtubercamera.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vtubercamera.ui.components.ExpressionControlPanel
import com.example.vtubercamera.ui.components.PoseControlPanel
import com.example.vtubercamera.ui.viewmodels.AvatarControlViewModel

/**
 * Main avatar control screen that combines expression and pose controls
 * Provides a tabbed interface for managing VRM avatar expressions and poses
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarControlScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvatarControlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(AvatarControlTab.EXPRESSIONS) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Avatar Controls",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(
                    onClick = { /* Open settings */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            AvatarControlTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.title) },
                    icon = { Icon(tab.icon, contentDescription = tab.title) }
                )
            }
        }
        
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                when (selectedTab) {
                    AvatarControlTab.EXPRESSIONS -> {
                        ExpressionControlContent(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                    AvatarControlTab.POSES -> {
                        PoseControlContent(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                    AvatarControlTab.SETTINGS -> {
                        AvatarSettingsContent(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressionControlContent(
    uiState: AvatarControlUiState,
    viewModel: AvatarControlViewModel
) {
    if (uiState.expressionData.isEmpty()) {
        EmptyStateCard(
            title = "No Expression Data",
            description = "Load a VRM avatar to access expression controls",
            icon = Icons.Default.EmojiEmotions
        )
    } else {
        ExpressionControlPanel(
            expressionData = uiState.expressionData,
            currentExpression = uiState.currentExpression,
            activeBlendShapes = uiState.activeBlendShapes,
            isTransitioning = uiState.isExpressionTransitioning,
            transitionProgress = uiState.expressionTransitionProgress,
            onExpressionSelected = { expression ->
                viewModel.selectExpression(expression)
            },
            onBlendShapeChanged = { shapeName, weight ->
                viewModel.setBlendShapeWeight(shapeName, weight)
            },
            onTransitionDurationChanged = { duration ->
                viewModel.setExpressionTransitionDuration(duration)
            },
            onClearExpression = {
                viewModel.clearExpression()
            }
        )
    }
}

@Composable
private fun PoseControlContent(
    uiState: AvatarControlUiState,
    viewModel: AvatarControlViewModel
) {
    if (uiState.poseData.isEmpty()) {
        EmptyStateCard(
            title = "No Pose Data",
            description = "Load a VRM avatar to access pose controls",
            icon = Icons.Default.Accessibility
        )
    } else {
        PoseControlPanel(
            poseData = uiState.poseData,
            currentPose = uiState.currentPose,
            activeBoneTransforms = uiState.activeBoneTransforms,
            boneLocks = uiState.boneLocks,
            isTransitioning = uiState.isPoseTransitioning,
            transitionProgress = uiState.poseTransitionProgress,
            onPoseSelected = { pose ->
                viewModel.selectPose(pose)
            },
            onBoneTransformChanged = { boneName, transform ->
                viewModel.setBoneTransform(boneName, transform)
            },
            onBoneLockToggled = { boneName ->
                viewModel.toggleBoneLock(boneName)
            },
            onTransitionDurationChanged = { duration ->
                viewModel.setPoseTransitionDuration(duration)
            },
            onResetToDefaultPose = {
                viewModel.resetToDefaultPose()
            },
            onClearPose = {
                viewModel.clearPose()
            }
        )
    }
}

@Composable
private fun AvatarSettingsContent(
    uiState: AvatarControlUiState,
    viewModel: AvatarControlViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar Information Card
        AvatarInfoCard(uiState = uiState)
        
        // Control Settings Card
        ControlSettingsCard(
            uiState = uiState,
            viewModel = viewModel
        )
        
        // Performance Settings Card
        PerformanceSettingsCard(
            uiState = uiState,
            viewModel = viewModel
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvatarInfoCard(uiState: AvatarControlUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Avatar Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (uiState.avatarModel != null) {
                InfoRow("Name", uiState.avatarModel.name)
                InfoRow("Expressions", "${uiState.expressionData.expressions.size}")
                InfoRow("Poses", "${uiState.poseData.poses.size}")
                InfoRow("Bones", "${uiState.poseData.boneMapping.getAllBoneNames().size}")
            } else {
                Text(
                    text = "No avatar loaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ControlSettingsCard(
    uiState: AvatarControlUiState,
    viewModel: AvatarControlViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Control Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Auto-reset option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-reset on avatar change",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = uiState.autoResetOnAvatarChange,
                    onCheckedChange = { viewModel.setAutoResetOnAvatarChange(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Smooth transitions option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Smooth transitions",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = uiState.smoothTransitions,
                    onCheckedChange = { viewModel.setSmoothTransitions(it) }
                )
            }
        }
    }
}

@Composable
private fun PerformanceSettingsCard(
    uiState: AvatarControlUiState,
    viewModel: AvatarControlViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Performance Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Update frequency
            Text(
                text = "Update Frequency: ${uiState.updateFrequency} FPS",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Slider(
                value = uiState.updateFrequency.toFloat(),
                onValueChange = { viewModel.setUpdateFrequency(it.toInt()) },
                valueRange = 15f..60f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quality setting
            Text(
                text = "Quality: ${uiState.qualitySetting}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Slider(
                value = when (uiState.qualitySetting) {
                    "Low" -> 0f
                    "Medium" -> 1f
                    "High" -> 2f
                    else -> 1f
                },
                onValueChange = { value ->
                    val quality = when (value.toInt()) {
                        0 -> "Low"
                        1 -> "Medium"
                        2 -> "High"
                        else -> "Medium"
                    }
                    viewModel.setQualitySetting(quality)
                },
                valueRange = 0f..2f,
                steps = 1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Enum for avatar control tabs
 */
enum class AvatarControlTab(
    val title: String,
    val icon: ImageVector
) {
    EXPRESSIONS("Expressions", Icons.Default.EmojiEmotions),
    POSES("Poses", Icons.Default.Accessibility),
    SETTINGS("Settings", Icons.Default.Settings)
}