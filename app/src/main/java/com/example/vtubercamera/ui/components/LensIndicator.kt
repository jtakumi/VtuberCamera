package com.example.vtubercamera.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vtubercamera.utils.CameraCapabilityManager
import kotlinx.coroutines.delay

/**
 * Component to show current active lens and switching feedback
 */
@Composable
fun LensIndicator(
    lensType: CameraCapabilityManager.LensType,
    lensDisplayName: String,
    canSwitchLens: Boolean,
    modifier: Modifier = Modifier,
    showTemporary: Boolean = false,
    onTemporaryDismiss: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(showTemporary) {
        if (showTemporary) {
            isVisible = true
            delay(2000) // Show for 2 seconds
            isVisible = false
            onTemporaryDismiss()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    AnimatedVisibility(
        visible = canSwitchLens,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .scale(scale)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = getLensIcon(lensType),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = lensDisplayName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Component showing lens switching gesture hint
 */
@Composable
fun LensSwitchHint(
    canSwitchLens: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = canSwitchLens,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lens,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = "Pinch to switch lens",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * Get appropriate icon for lens type
 */
private fun getLensIcon(lensType: CameraCapabilityManager.LensType): ImageVector {
    return when (lensType) {
        CameraCapabilityManager.LensType.WIDE_ANGLE -> Icons.Default.CameraAlt // Wide angle representation
        CameraCapabilityManager.LensType.NORMAL -> Icons.Default.Lens // Normal lens representation
        CameraCapabilityManager.LensType.TELEPHOTO -> Icons.Default.CameraAlt // Telephoto representation
        CameraCapabilityManager.LensType.FRONT -> Icons.Default.CameraAlt // Front camera representation
    }
}

/**
 * Animated lens switching feedback overlay
 */
@Composable
fun LensSwitchFeedback(
    isVisible: Boolean,
    newLensName: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lens,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Text(
                    text = "Switched to",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                
                Text(
                    text = newLensName,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}