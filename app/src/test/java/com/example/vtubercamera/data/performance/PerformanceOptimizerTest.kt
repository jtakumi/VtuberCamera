package com.example.vtubercamera.data.performance

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class PerformanceOptimizerTest {
    
    private lateinit var performanceOptimizer: PerformanceOptimizer
    private var textureQualityChanges = mutableListOf<QualityLevel>()
    private var renderQualityChanges = mutableListOf<QualityLevel>()
    private var frameRateChanges = mutableListOf<Float>()
    private var cacheClears = 0
    private var effectsToggles = mutableListOf<Boolean>()
    private var thermalThrottlings = mutableListOf<Boolean>()
    
    @Before
    fun setup() {
        performanceOptimizer = PerformanceOptimizer()
        
        // Reset tracking variables
        textureQualityChanges.clear()
        renderQualityChanges.clear()
        frameRateChanges.clear()
        cacheClears = 0
        effectsToggles.clear()
        thermalThrottlings.clear()
    }
    
    @Test
    fun `applyOptimizations should not run when auto optimization is disabled`() = runTest {
        // Given
        val settings = PerformanceSettings(autoOptimization = false)
        performanceOptimizer.updateSettings(settings)
        
        val performanceState = createPoorPerformanceState()
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        assertTrue(textureQualityChanges.isEmpty())
        assertTrue(renderQualityChanges.isEmpty())
        assertTrue(frameRateChanges.isEmpty())
        assertEquals(0, cacheClears)
        assertTrue(effectsToggles.isEmpty())
        assertTrue(thermalThrottlings.isEmpty())
    }
    
    @Test
    fun `applyOptimizations should reduce texture quality when recommended`() = runTest {
        // Given
        val performanceState = PerformanceState(
            memoryState = MemoryState(),
            frameRateState = FrameRateState(),
            batteryState = BatteryState(),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(OptimizationRecommendation.REDUCE_TEXTURE_QUALITY)
        )
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        assertEquals(1, textureQualityChanges.size)
        assertEquals(QualityLevel.MEDIUM, textureQualityChanges[0]) // Reduced from HIGH to MEDIUM
    }
    
    @Test
    fun `applyOptimizations should reduce render quality when recommended`() = runTest {
        // Given
        val performanceState = PerformanceState(
            memoryState = MemoryState(),
            frameRateState = FrameRateState(),
            batteryState = BatteryState(),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(OptimizationRecommendation.REDUCE_RENDER_QUALITY)
        )
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        assertEquals(1, renderQualityChanges.size)
        assertEquals(QualityLevel.MEDIUM, renderQualityChanges[0])
    }
    
    @Test
    fun `applyOptimizations should clear cache when recommended`() = runTest {
        // Given
        val performanceState = PerformanceState(
            memoryState = MemoryState(),
            frameRateState = FrameRateState(),
            batteryState = BatteryState(),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(OptimizationRecommendation.CLEAR_CACHE)
        )
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        assertEquals(1, cacheClears)
    }
    
    @Test
    fun `applyOptimizations should lower frame rate when recommended`() = runTest {
        // Given
        val settings = PerformanceSettings(targetFrameRate = 30f)
        performanceOptimizer.updateSettings(settings)
        
        val performanceState = PerformanceState(
            memoryState = MemoryState(),
            frameRateState = FrameRateState(),
            batteryState = BatteryState(),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(OptimizationRecommendation.LOWER_FRAME_RATE)
        )
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        assertEquals(1, frameRateChanges.size)
        assertEquals(25f, frameRateChanges[0], 0.1f) // Reduced from 30f to 25f
        assertEquals(25f, performanceOptimizer.settings.value.targetFrameRate, 0.1f)
    }
    
    @Test
    fun `applyOptimizations should disable effects when recommended`() = runTest {
        // Given
        val performanceState = PerformanceState(
            memoryState = MemoryState(),
            frameRateState = FrameRateState(),
            batteryState = BatteryState(),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(OptimizationRecommendation.DISABLE_EFFECTS)
        )
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        assertEquals(1, effectsToggles.size)
        assertFalse(effectsToggles[0])
    }
    
    @Test
    fun `applyOptimizations should enable thermal throttling when recommended`() = runTest {
        // Given
        val settings = PerformanceSettings(thermalThrottlingEnabled = true)
        performanceOptimizer.updateSettings(settings)
        
        val performanceState = PerformanceState(
            memoryState = MemoryState(),
            frameRateState = FrameRateState(),
            batteryState = BatteryState(),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(OptimizationRecommendation.THERMAL_THROTTLING)
        )
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        assertEquals(1, thermalThrottlings.size)
        assertTrue(thermalThrottlings[0])
    }
    
    @Test
    fun `restoreQualityIfPossible should improve quality when performance is excellent`() = runTest {
        // Given - Start with reduced quality
        val settings = PerformanceSettings(
            qualityLevel = QualityLevel.MEDIUM,
            targetFrameRate = 25f
        )
        performanceOptimizer.updateSettings(settings)
        
        val excellentPerformanceState = PerformanceState(
            memoryState = MemoryState(usagePercent = 40),
            frameRateState = FrameRateState(averageFps = 30f, targetFps = 25f),
            batteryState = BatteryState(batteryLevel = 80),
            overallHealth = PerformanceHealth.EXCELLENT,
            shouldOptimize = false,
            recommendedOptimizations = emptyList()
        )
        
        // When
        performanceOptimizer.restoreQualityIfPossible(
            excellentPerformanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) }
        )
        
        // Then
        assertEquals(1, textureQualityChanges.size)
        assertEquals(QualityLevel.HIGH, textureQualityChanges[0]) // Improved from MEDIUM to HIGH
        assertEquals(1, renderQualityChanges.size)
        assertEquals(QualityLevel.HIGH, renderQualityChanges[0])
        assertEquals(1, frameRateChanges.size)
        assertEquals(30f, frameRateChanges[0], 0.1f) // Improved from 25f to 30f
    }
    
    @Test
    fun `restoreQualityIfPossible should not improve quality when performance is poor`() = runTest {
        // Given
        val settings = PerformanceSettings(qualityLevel = QualityLevel.MEDIUM)
        performanceOptimizer.updateSettings(settings)
        
        val poorPerformanceState = createPoorPerformanceState()
        
        // When
        performanceOptimizer.restoreQualityIfPossible(
            poorPerformanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) }
        )
        
        // Then
        assertTrue(textureQualityChanges.isEmpty())
        assertTrue(renderQualityChanges.isEmpty())
        assertTrue(frameRateChanges.isEmpty())
    }
    
    @Test
    fun `optimization history should be tracked correctly`() = runTest {
        // Given
        val performanceState = PerformanceState(
            memoryState = MemoryState(),
            frameRateState = FrameRateState(),
            batteryState = BatteryState(),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(
                OptimizationRecommendation.REDUCE_TEXTURE_QUALITY,
                OptimizationRecommendation.CLEAR_CACHE
            )
        )
        
        // When
        performanceOptimizer.applyOptimizations(
            performanceState,
            onTextureQualityChange = { textureQualityChanges.add(it) },
            onRenderQualityChange = { renderQualityChanges.add(it) },
            onFrameRateChange = { frameRateChanges.add(it) },
            onCacheClear = { cacheClears++ },
            onEffectsToggle = { effectsToggles.add(it) },
            onThermalThrottling = { thermalThrottlings.add(it) }
        )
        
        // Then
        val history = performanceOptimizer.optimizationHistory.value
        assertEquals(2, history.size)
        assertTrue(history.any { it.action == OptimizationAction.TEXTURE_QUALITY_REDUCED })
        assertTrue(history.any { it.action == OptimizationAction.CACHE_CLEARED })
    }
    
    private fun createPoorPerformanceState(): PerformanceState {
        return PerformanceState(
            memoryState = MemoryState(usagePercent = 90, shouldOptimize = true),
            frameRateState = FrameRateState(averageFps = 15f, shouldReduceQuality = true),
            batteryState = BatteryState(batteryLevel = 10, isOverheating = true, shouldOptimize = true),
            overallHealth = PerformanceHealth.POOR,
            shouldOptimize = true,
            recommendedOptimizations = listOf(
                OptimizationRecommendation.REDUCE_TEXTURE_QUALITY,
                OptimizationRecommendation.REDUCE_RENDER_QUALITY,
                OptimizationRecommendation.THERMAL_THROTTLING
            )
        )
    }
}