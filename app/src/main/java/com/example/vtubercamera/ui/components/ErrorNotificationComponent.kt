package com.example.vtubercamera.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vtubercamera.R
import com.example.vtubercamera.data.vrm.*

/**
 * Component for displaying error notifications to users
 */
@Composable
fun ErrorNotificationComponent(
    notifications: List<ErrorNotification>,
    onActionClick: (ErrorAction) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(
            items = notifications,
            key = { it.id }
        ) { notification ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                ErrorNotificationCard(
                    notification = notification,
                    onActionClick = onActionClick,
                    onDismiss = { onDismiss(notification.id) }
                )
            }
        }
    }
}

/**
 * Individual error notification card
 */
@Composable
private fun ErrorNotificationCard(
    notification: ErrorNotification,
    onActionClick: (ErrorAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (notification.severity) {
        ErrorSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
        ErrorSeverity.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        ErrorSeverity.LOW -> MaterialTheme.colorScheme.tertiaryContainer
    }
    
    val contentColor = when (notification.severity) {
        ErrorSeverity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
        ErrorSeverity.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
        ErrorSeverity.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    
    val borderColor = when (notification.severity) {
        ErrorSeverity.HIGH -> MaterialTheme.colorScheme.error
        ErrorSeverity.MEDIUM -> MaterialTheme.colorScheme.secondary
        ErrorSeverity.LOW -> MaterialTheme.colorScheme.tertiary
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with icon, title, and dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getIconForNotification(notification.icon),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = notification.getFormattedTime(),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = contentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Error message
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                lineHeight = 20.sp
            )
            
            // Actions
            if (notification.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    notification.actions.forEach { action ->
                        if (!action.isDismissAction) {
                            ErrorActionButton(
                                action = action,
                                onClick = { onActionClick(action.action!!) },
                                severity = notification.severity
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Action button for error notifications
 */
@Composable
private fun ErrorActionButton(
    action: NotificationAction,
    onClick: () -> Unit,
    severity: ErrorSeverity,
    modifier: Modifier = Modifier
) {
    val buttonColors = when (severity) {
        ErrorSeverity.HIGH -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        )
        ErrorSeverity.MEDIUM -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
        ErrorSeverity.LOW -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )
    }
    
    FilledTonalButton(
        onClick = onClick,
        colors = buttonColors,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Compact error notification for status bars or minimal displays
 */
@Composable
fun CompactErrorNotification(
    notification: ErrorNotification,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (notification.severity) {
        ErrorSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
        ErrorSeverity.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        ErrorSeverity.LOW -> MaterialTheme.colorScheme.tertiaryContainer
    }
    
    val contentColor = when (notification.severity) {
        ErrorSeverity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
        ErrorSeverity.MEDIUM -> MaterialTheme.colorScheme.onSecondaryContainer
        ErrorSeverity.LOW -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconForNotification(notification.icon),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Error notification badge for showing error count
 */
@Composable
fun ErrorNotificationBadge(
    errorCount: Int,
    hasCriticalErrors: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (errorCount > 0) {
        val backgroundColor = if (hasCriticalErrors) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.secondary
        }
        
        val contentColor = if (hasCriticalErrors) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onSecondary
        }
        
        Surface(
            modifier = modifier
                .clickable { onClick() }
                .clip(RoundedCornerShape(16.dp)),
            color = backgroundColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasCriticalErrors) Icons.Default.Error else Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = errorCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Get appropriate icon for notification type
 */
private fun getIconForNotification(icon: NotificationIcon): ImageVector {
    return when (icon) {
        NotificationIcon.GENERIC_ERROR -> Icons.Default.Error
        NotificationIcon.FILE_ERROR -> Icons.Default.InsertDriveFile
        NotificationIcon.FORMAT_ERROR -> Icons.Default.BrokenImage
        NotificationIcon.SIZE_ERROR -> Icons.Default.Storage
        NotificationIcon.AR_ERROR -> Icons.Default.ViewInAr
        NotificationIcon.TRACKING_ERROR -> Icons.Default.TrackChanges
        NotificationIcon.NETWORK_ERROR -> Icons.Default.NetworkCheck
        NotificationIcon.PERMISSION_ERROR -> Icons.Default.Security
        NotificationIcon.MEMORY_ERROR -> Icons.Default.Memory
    }
}