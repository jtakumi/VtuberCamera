package com.example.vtubercamera.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.vtubercamera.data.performance.QualityLevel
import com.example.vtubercamera.ui.components.PerformanceMonitorPanel
import com.example.vtubercamera.ui.viewmodels.PerformanceMonitorDemoViewModel

/**
 * パフォーマンス監視機能のデモ画面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceMonitorDemoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerformanceMonitorDemoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("パフォーマンス監視デモ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // パフォーマンス監視パネル
            PerformanceMonitorPanel(
                performanceStatus = uiState.performanceStatus,
                memoryState = uiState.memoryState,
                frameRateData = uiState.frameRateData,
                batteryInfo = uiState.batteryInfo,
                powerConsumption = uiState.powerConsumption,
                isMonitoring = uiState.isMonitoring,
                onToggleMonitoring = viewModel::toggleMonitoring,
                onManualOptimization = viewModel::performManualOptimization
            )

            // 設定パネル
            PerformanceSettingsPanel(
                settings = uiState.settings,
                onSettingsChange = viewModel::updateSettings
            )

            // シミュレーション制御
            SimulationControlPanel(
                onSimulateHighMemoryUsage = viewModel::simulateHighMemoryUsage,
                onSimulateLowFrameRate = viewModel::simulateLowFrameRate,
                onSimulateOverheating = viewModel::simulateOverheating,
                onResetSimulation = viewModel::resetSimulation
            )
        }
    }
}

@Composable
private fun PerformanceSettingsPanel(
    settings: com.example.vtubercamera.data.performance.PerformanceSettings,
    onSettingsChange: (com.example.vtubercamera.data.performance.PerformanceSettings) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "パフォーマンス設定",
                style = MaterialTheme.typography.titleMedium
            )

            // 自動最適化の切り替え
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自動最適化")
                Switch(
                    checked = settings.autoOptimization,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(autoOptimization = it))
                    }
                )
            }

            // ターゲットフレームレート
            Text("ターゲットフレームレート: ${settings.targetFrameRate.toInt()} FPS")
            Slider(
                value = settings.targetFrameRate,
                onValueChange = {
                    onSettingsChange(settings.copy(targetFrameRate = it))
                },
                valueRange = 15f..60f,
                steps = 8
            )

            // 品質レベル
            Text("品質レベル: ${settings.qualityLevel.name}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityLevel.entries.forEach { level ->
                    FilterChip(
                        selected = settings.qualityLevel == level,
                        onClick = {
                            onSettingsChange(settings.copy(qualityLevel = level))
                        },
                        label = { Text(level.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulationControlPanel(
    onSimulateHighMemoryUsage: () -> Unit,
    onSimulateLowFrameRate: () -> Unit,
    onSimulateOverheating: () -> Unit,
    onResetSimulation: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "シミュレーション制御",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "パフォーマンス問題をシミュレートして最適化機能をテストできます",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSimulateHighMemoryUsage,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("メモリ不足")
                }

                Button(
                    onClick = onSimulateLowFrameRate,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("低FPS")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSimulateOverheating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("過熱")
                }

                OutlinedButton(
                    onClick = onResetSimulation,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("リセット")
                }
            }
        }
    }
}