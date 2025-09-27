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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.BlendShapeCategory
import com.example.vtubercamera.data.vrm.ExpressionData

/**
 * Expression control panel UI component for VRM avatar expression management
 * Provides expression selection, blending, and real-time preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressionControlPanel(
    expressionData: ExpressionData,
    currentExpression: Expression?,
    activeBlendShapes: Map<String, Float>,
    isTransitioning: Boolean,
    transitionProgress: Float,
    onExpressionSelected: (Expression?) -> Unit,
    onBlendShapeChanged: (String, Float) -> Unit,
    onTransitionDurationChanged: (Float) -> Unit,
    onClearExpression: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(BlendShapeCategory.EMOTION) }
    var showAdvancedControls by remember { mutableStateOf(false) }
    var transitionDuration by remember { mutableStateOf(0.3f) }
    
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
                    text = "Expression Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(
                        onClick = { showAdvancedControls = !showAdvancedControls }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Advanced Controls"
                        )
                    }
                    
                    IconButton(
                        onClick = onClearExpression
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Expression"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current Expression Info
            if (currentExpression != null || isTransitioning) {
                ExpressionStatusCard(
                    currentExpression = currentExpression,
                    isTransitioning = isTransitioning,
                    transitionProgress = transitionProgress
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Category Selection
            CategorySelector(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Expression List
            val expressions = expressionData.getExpressionsByCategory(selectedCategory)
            ExpressionList(
                expressions = expressions,
                currentExpression = currentExpression,
                onExpressionSelected = onExpressionSelected
            )
            
            // Advanced Controls
            if (showAdvancedControls) {
                Spacer(modifier = Modifier.height(16.dp))
                AdvancedExpressionControls(
                    expressionData = expressionData,
                    activeBlendShapes = activeBlendShapes,
                    transitionDuration = transitionDuration,
                    onBlendShapeChanged = onBlendShapeChanged,
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
private fun ExpressionStatusCard(
    currentExpression: Expression?,
    isTransitioning: Boolean,
    transitionProgress: Float
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
                    text = "Current Expression",
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
                text = currentExpression?.displayName ?: "None",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            if (isTransitioning && transitionProgress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = transitionProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: BlendShapeCategory,
    onCategorySelected: (BlendShapeCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(BlendShapeCategory.values()) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: BlendShapeCategory,
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
private fun ExpressionList(
    expressions: List<Expression>,
    currentExpression: Expression?,
    onExpressionSelected: (Expression?) -> Unit
) {
    if (expressions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expressions available in this category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(expressions) { expression ->
                ExpressionItem(
                    expression = expression,
                    isSelected = expression == currentExpression,
                    onSelected = { onExpressionSelected(expression) }
                )
            }
        }
    }
}

@Composable
private fun ExpressionItem(
    expression: Expression,
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
                    text = expression.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                // Expression complexity indicator
                ExpressionComplexityIndicator(expression = expression)
            }
            
            if (expression.blendShapeKeys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${expression.blendShapeKeys.size} blend shapes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Override indicators
            if (expression.hasOverrides()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (expression.overrideBlink != Expression.OverrideType.NONE) {
                        OverrideChip("Blink")
                    }
                    if (expression.overrideLookAt != Expression.OverrideType.NONE) {
                        OverrideChip("LookAt")
                    }
                    if (expression.overrideMouth != Expression.OverrideType.NONE) {
                        OverrideChip("Mouth")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressionComplexityIndicator(expression: Expression) {
    val complexity = expression.blendShapeKeys.size + 
                    expression.materialColorBindings.size + 
                    expression.textureTransformBindings.size
    
    val color = when {
        complexity == 0 -> Color.Gray
        complexity <= 5 -> Color.Green
        complexity <= 15 -> Color.Orange
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
private fun OverrideChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun AdvancedExpressionControls(
    expressionData: ExpressionData,
    activeBlendShapes: Map<String, Float>,
    transitionDuration: Float,
    onBlendShapeChanged: (String, Float) -> Unit,
    onTransitionDurationChanged: (Float) -> Unit
) {
    Column {
        Text(
            text = "Advanced Controls",
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
                text = "${String.format("%.1f", transitionDuration)}s",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = transitionDuration,
            onValueChange = onTransitionDurationChanged,
            valueRange = 0.1f..2.0f,
            steps = 19
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Manual Blend Shape Controls
        if (activeBlendShapes.isNotEmpty()) {
            Text(
                text = "Active Blend Shapes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.height(150.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeBlendShapes.keys.toList()) { shapeName ->
                    BlendShapeControl(
                        shapeName = shapeName,
                        value = activeBlendShapes[shapeName] ?: 0f,
                        onValueChanged = { value ->
                            onBlendShapeChanged(shapeName, value)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlendShapeControl(
    shapeName: String,
    value: Float,
    onValueChanged: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shapeName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChanged,
            valueRange = 0f..1f
        )
    }
}