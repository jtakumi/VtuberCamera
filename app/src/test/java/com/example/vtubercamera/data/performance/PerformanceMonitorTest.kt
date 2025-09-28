package com.example.vtubercamera.data.performance

import android.app.ActivityManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class PerformanceMonitorTest {
    
    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun setup() {
        context = mockk()
        activityManager = mockk()
        
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        
        performanceMonitor = PerformanceMonitor(context)
    }
    
    @Test
    fun `updateMemoryState should calculate memory usage correctly`() = runTest {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            availMem = 2L * 1024 * 1024 * 1024 // 2GB
            lowMemory = false
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = memoryInfo.availMem
            info.lowMemory = memoryInfo.lowMemory
        }
        
        // When
        performanceMonitor.updateMemoryState()
        
        // Then
        val memoryState = performanceMonitor.memoryState.value
        assertFalse(memoryState.isLowMemory)
        assertTrue(memoryState.usedMemoryMB > 0)
        assertTrue(memoryState.maxMemoryMB > 0)
        assertTrue(memoryState.usagePercent >= 0)
        assertTrue(memoryState.usagePercent <= 100)
    }
    
    @Test
    fun `updateMemoryState should detect low memory condition`() = runTest {
        // Given
        val memoryInfo = ActivityManager.MemoryInfo().apply {
            availMem = 100L * 1024 * 1024 // 100MB
            lowMemory = true
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = memoryInfo.availMem
            info.lowMemory = memoryInfo.lowMemory
        }
        
        // When
        performanceMonitor.updateMemoryState()
        
        // Then
        val memoryState = performanceMonitor.memoryState.value
        assertTrue(memoryState.isLowMemory)
        assertTrue(memoryState.shouldOptimize)
    }
    
    @Test
    fun `updateFrameRate should calculate frame rate metrics correctly`() = runTest {
        // Given
        val targetFps = 30f
        val currentFps = 25f
        
        // When
        performanceMonitor.updateFrameRate(currentFps, targetFps)
        
        // Then
        val frameRateState = performanceMonitor.frameRateState.value
        assertEquals(currentFps, frameRateState.currentFps, 0.1f)
        assertEquals(targetFps, frameRateState.targetFps, 0.1f)
        assertFalse(frameRateState.isStable) // 25fps < 30fps * 0.9
        assertTrue(frameRateState.shouldReduceQuality) // 25fps < 30fps * 0.8
    }
    
    @Test
    fun `updateFrameRate should detect stable frame rate`() = runTest {
        // Given
        val targetFps = 30f
        val currentFps = 29f
        
        // When
        performanceMonitor.updateFrameRate(currentFps, targetFps)
        
        // Then
        val frameRateState = performanceMonitor.frameRateState.value
        assertTrue(frameRateState.isStable) // 29fps >= 30fps * 0.9
        assertFalse(frameRateState.shouldReduceQuality) // 29fps >= 30fps * 0.8
    }
    
    @Test
    fun `updateBatteryState should detect overheating condition`() = runTest {
        // Given
        val batteryLevel = 80
        val isCharging = false
        val temperature = 45f // Over 40°C
        
        // When
        performanceMonitor.updateBatteryState(batteryLevel, isCharging, temperature)
        
        // Then
        val batteryState = performanceMonitor.batteryState.value
        assertEquals(batteryLevel, batteryState.batteryLevel)
        assertEquals(isCharging, batteryState.isCharging)
        assertEquals(temperature, batteryState.temperature, 0.1f)
        assertTrue(batteryState.isOverheating)
        assertTrue(batteryState.shouldOptimize)
    }
    
    @Test
    fun `updateBatteryState should detect low battery condition`() = runTest {
        // Given
        val batteryLevel = 15 // Below 20%
        val isCharging = false
        val temperature = 30f
        
        // When
        performanceMonitor.updateBatteryState(batteryLevel, isCharging, temperature)
        
        // Then
        val batteryState = performanceMonitor.batteryState.value
        assertTrue(batteryState.isLowBattery)
        assertTrue(batteryState.shouldOptimize)
        assertFalse(batteryState.isOverheating)
    }
    
    @Test
    fun `getOverallPerformanceState should calculate correct health status`() = runTest {
        // Given - Set up good performance conditions
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = 4L * 1024 * 1024 * 1024 // 4GB
            info.lowMemory = false
        }
        
        performanceMonitor.updateMemoryState()
        performanceMonitor.updateFrameRate(30f, 30f) // Perfect frame rate
        performanceMonitor.updateBatteryState(80, false, 30f) // Good battery
        
        // When
        val performanceState = performanceMonitor.getOverallPerformanceState()
        
        // Then
        assertEquals(PerformanceHealth.EXCELLENT, performanceState.overallHealth)
        assertFalse(performanceState.shouldOptimize)
        assertTrue(performanceState.recommendedOptimizations.isEmpty())
    }
    
    @Test
    fun `getOverallPerformanceState should recommend optimizations for poor performance`() = runTest {
        // Given - Set up poor performance conditions
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = 100L * 1024 * 1024 // 100MB
            info.lowMemory = true
        }
        
        performanceMonitor.updateMemoryState()
        performanceMonitor.updateFrameRate(15f, 30f) // Poor frame rate
        performanceMonitor.updateBatteryState(10, false, 45f) // Low battery, overheating
        
        // When
        val performanceState = performanceMonitor.getOverallPerformanceState()
        
        // Then
        assertEquals(PerformanceHealth.POOR, performanceState.overallHealth)
        assertTrue(performanceState.shouldOptimize)
        assertTrue(performanceState.recommendedOptimizations.isNotEmpty())
        assertTrue(performanceState.recommendedOptimizations.contains(OptimizationRecommendation.REDUCE_TEXTURE_QUALITY))
        assertTrue(performanceState.recommendedOptimizations.contains(OptimizationRecommendation.REDUCE_RENDER_QUALITY))
        assertTrue(performanceState.recommendedOptimizations.contains(OptimizationRecommendation.THERMAL_THROTTLING))
    }
    
    @Test
    fun `calculateOverallHealth should return correct health levels`() = runTest {
        // Test EXCELLENT health
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = 4L * 1024 * 1024 * 1024
            info.lowMemory = false
        }
        performanceMonitor.updateMemoryState()
        performanceMonitor.updateFrameRate(30f, 30f)
        performanceMonitor.updateBatteryState(80, false, 30f)
        
        var state = performanceMonitor.getOverallPerformanceState()
        assertEquals(PerformanceHealth.EXCELLENT, state.overallHealth)
        
        // Test GOOD health
        performanceMonitor.updateFrameRate(27f, 30f) // Slightly lower FPS
        state = performanceMonitor.getOverallPerformanceState()
        assertEquals(PerformanceHealth.GOOD, state.overallHealth)
        
        // Test FAIR health
        performanceMonitor.updateFrameRate(22f, 30f) // Lower FPS
        state = performanceMonitor.getOverallPerformanceState()
        assertEquals(PerformanceHealth.FAIR, state.overallHealth)
        
        // Test POOR health
        performanceMonitor.updateFrameRate(15f, 30f) // Very low FPS
        state = performanceMonitor.getOverallPerformanceState()
        assertEquals(PerformanceHealth.POOR, state.overallHealth)
    }
}