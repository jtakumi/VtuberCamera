package com.example.vtubercamera.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vtubercamera.data.performance.*

/**
 * パフォーマンス監視パネルUI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceMonitorPanel(
    performanceStatus: OverallPerformanceStatus,
    memoryState: MemoryState,
    frameRateData: FrameRateData,
    batteryInfo: BatteryInfo,
    powerConsumption: PowerConsumption,
    isMonitoring: Boolean,
    onToggleMonitoring: () -> Unit,
    onManualOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ヘッダー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "パフォーマンス監視",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 監視切り替えボタン
                    IconButton(
                        onClick = onToggleMonitoring
                    ) {
                        Icon(
                            imageVector = if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isMonitoring) "監視停止" else "監視開始",
                            tint = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // 手動最適化ボタン
                    IconButton(
                        onClick = onManualOptimization,
                        enabled = isMonitoring
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "手動最適化",
                            tint = if (isMonitoring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            // 総合スコア
            PerformanceScoreIndicator(
                score = performanceStatus.performanceScore,
                modifier = Modifier.fillMaxWidth()
            )
            
            // アラート表示
            if (performanceStatus.alerts.isNotEmpty()) {
                AlertSection(alerts = performanceStatus.alerts)
            }
            
            // メトリクス表示
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MemoryMetricsCard(memoryState = memoryState)
                }
                
                item {
                    FrameRateMetricsCard(frameRateData = frameRateData)
                }
                
                item {
                    BatteryMetricsCard(
                        batteryInfo = batteryInfo,
                        powerConsumption = powerConsumption
                    )
                }
            }
            
            // 推奨事項
            if (performanceStatus.recommendations.isNotEmpty()) {
                RecommendationsSection(recommendations = performanceStatus.recommendations)
            }
        }
    }
}

@Composable
private fun PerformanceScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        label = "performance_score"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPerformanceCircle(animatedScore)
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${animatedScore.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(animatedScore.toInt())
                )
                Text(
                    text = "スコア",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        Text(
            text = getScoreDescription(score),
            style = MaterialTheme.typography.bodyMedium,
            color = getScoreColor(score)
        )
    }
}

private fun DrawScope.drawPerformanceCircle(score: Float) {
    val strokeWidth = 12.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)
    
    // 背景円
    drawCircle(
        color = Color.Gray.copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
    
    // スコア円
    val sweepAngle = (score / 100f) * 360f
    drawArc(
        color = getScoreColorValue(score.toInt()),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

@Composable
private fun getScoreColor(score: Int): Color {
    return when {
        score >= 85 -> Color(0xFF4CAF50) // Green
        score >= 70 -> Color(0xFF8BC34A) // Light Green
        score >= 50 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun getScoreColorValue(score: Int): Color {
    return when {
        score >= 85 -> Color(0xFF4CAF50) // Green
        score >= 70 -> Color(0xFF8BC34A) // Light Green
        score >= 50 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun getScoreDescription(score: Int): String {
    return when {
        score >= 85 -> "優秀"
        score >= 70 -> "良好"
        score >= 50 -> "普通"
        else -> "要改善"
    }
}

@Composable
private fun AlertSection(
    alerts: List<PerformanceAlert>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "アラート",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            alerts.forEach { alert ->
                Text(
                    text = "• ${getAlertMessage(alert)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun MemoryMetricsCard(
    memoryState: MemoryState
) {
    MetricsCard(
        title = "メモリ使用量",
        icon = Icons.Default.Memory,
        value = "${memoryState.usagePercent}%",
        subtitle = "${memoryState.usedMemoryMB}MB / ${memoryState.maxMemoryMB}MB",
        progress = memoryState.usagePercent / 100f,
        isWarning = memoryState.shouldOptimize
    )
}

@Composable
private fun FrameRateMetricsCard(
    frameRateData: FrameRateData
) {
    MetricsCard(
        title = "フレームレート",
        icon = Icons.Default.Speed,
        value = "${frameRateData.currentFps.toInt()} FPS",
        subtitle = "平均: ${frameRateData.averageFps.toInt()} FPS",
        progress = frameRateData.currentFps / 60f,
        isWarning = !frameRateData.isStable
    )
}

@Composable
private fun BatteryMetricsCard(
    batteryInfo: BatteryInfo,
    powerConsumption: PowerConsumption
) {
    MetricsCard(
        title = "バッテリー",
        icon = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Battery6Bar,
        value = "${batteryInfo.level}%",
        subtitle = "${batteryInfo.temperature.toInt()}°C • ${powerConsumption.consumptionRate.toInt()}%/h",
        progress = batteryInfo.level / 100f,
        isWarning = batteryInfo.isLowBattery || batteryInfo.isOverheating
    )
}

@Composable
private fun MetricsCard(
    title: String,
    icon: ImageVector,
    value: String,
    subtitle: String,
    progress: Float,
    isWarning: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RecommendationsSection(
    recommendations: List<PerformanceRecommendation>
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "推奨事項",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            recommendations.forEach { recommendation ->
                Text(
                    text = "• ${getRecommendationMessage(recommendation)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun getAlertMessage(alert: PerformanceAlert): String {
    return when (alert) {
        PerformanceAlert.LOW_MEMORY -> "メモリ不足です"
        PerformanceAlert.HIGH_FRAME_DROPS -> "フレームドロップが多発しています"
        PerformanceAlert.OVERHEATING -> "デバイスが過熱しています"
        PerformanceAlert.CRITICAL_BATTERY -> "バッテリー残量が危険レベルです"
    }
}

private fun getRecommendationMessage(recommendation: PerformanceRecommendation): String {
    return when (recommendation) {
        PerformanceRecommendation.REDUCE_MEMORY_USAGE -> "メモリ使用量を削減してください"
        PerformanceRecommendation.IMPROVE_FRAME_RATE -> "フレームレートを改善してください"
        PerformanceRecommendation.COOL_DOWN_DEVICE -> "デバイスを冷却してください"
        PerformanceRecommendation.SAVE_BATTERY -> "バッテリーを節約してください"
    }
}