package com.example.vtubercamera.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.vtubercamera.data.vrm.BlendShapeGroup
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.ExpressionData

/**
 * Compact dropdown menu for selecting an expression.
 */
@Composable
fun ExpressionSelectionMenu(
    expressionData: ExpressionData,
    currentExpression: Expression?,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onExpressionSelected: (Expression?) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        // Clear item
        DropdownMenuItem(
            text = { Text("Clear Expression") },
            onClick = { onExpressionSelected(null) }
        )

        // Groups
        expressionData.blendShapeGroups.forEach { group: BlendShapeGroup ->
            // Group header (disabled item styled as header)
            DropdownMenuItem(
                text = { Text(group.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {},
                enabled = false
            )
            group.expressions.forEach { expr ->
                DropdownMenuItem(
                    text = { Text(if (expr == currentExpression) "✔ ${expr.displayName}" else expr.displayName) },
                    onClick = { onExpressionSelected(expr) }
                )
            }
        }
    }
}

