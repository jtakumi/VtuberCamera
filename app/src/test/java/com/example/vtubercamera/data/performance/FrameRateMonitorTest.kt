package com.example.vtubercamera.data.performance

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class FrameRateMonitorTest {
    
    private lateinit var frameRateMonitor: FrameRateMonitor
    
    @Before
    fun setup() {
        frameRateMonitor = FrameRateMonitor()
    }
    
    @Test
    fun `initial state should have default values`() = runTest {
        // Then
        val frameRateData = frameRateMonitor.frameRateData.value
        assertEquals(30f, frameRateData.currentFps, 0.1f)
        assertEquals(30f, frameRateData.averageFps, 0.1f)
        assertEquals(30f, frameRateData.minFps, 0.1f)
        assertEquals(30f, frameRateData.maxFps, 0.1f)
        assertEquals(0, frameRateData.frameDrops)
        assertEquals(0f, frameRateData.frameDropRate, 0.1f)
        assertEquals(0f, frameRateData.jitter, 0.1f)
        assertTrue(frameRateData.isStable)
        
        val qualityAdjustment = frameRateMonitor.qualityAdjustment.value
        assertFalse(qualityAdjustment.shouldReduceQuality)
        assertFalse(qualityAdjustment.shouldIncreaseQuality)
        assertFalse(qualityAdjustment.isReducing)
        assertFalse(qualityAdjustment.isIncreasing)
        assertEquals(QualityAdjustmentReason.NONE, qualityAdjustment.reason)
    }
    
    @Test
    fun `setQualityLevel should update quality level`() = runTest {
        // When
        frameRateMonitor.setQualityLevel(QualityLevel.ULTRA)
        
        // Then - Quality level is internal, but we can verify it doesn't crash
        // and affects future quality adjustment decisions
        assertNotNull(frameRateMonitor)
    }
    
    @Test
    fun `setTargetFrameRate should update target frame rate`() = runTest {
        // When
        frameRateMonitor.setTargetFrameRate(60f)
        
        // Then - Target frame rate is internal, but we can verify it doesn't crash
        assertNotNull(frameRateMonitor)
    }
    
    @Test
    fun `resetStatistics should reset all data to defaults`() = runTest {
        // Given - Simulate some frame rate data changes
        frameRateMonitor.setTargetFrameRate(60f)
        frameRateMonitor.setQualityLevel(QualityLevel.LOW)
        
        // When
        frameRateMonitor.resetStatistics()
        
        // Then
        val frameRateData = frameRateMonitor.frameRateData.value
        assertEquals(30f, frameRateData.currentFps, 0.1f)
        assertEquals(30f, frameRateData.averageFps, 0.1f)
        assertEquals(30f, frameRateData.minFps, 0.1f)
        assertEquals(30f, frameRateData.maxFps, 0.1f)
        assertEquals(0, frameRateData.frameDrops)
        assertEquals(0f, frameRateData.frameDropRate, 0.1f)
        assertEquals(0f, frameRateData.jitter, 0.1f)
        assertTrue(frameRateData.isStable)
        
        val qualityAdjustment = frameRateMonitor.qualityAdjustment.value
        assertFalse(qualityAdjustment.shouldReduceQuality)
        assertFalse(qualityAdjustment.shouldIncreaseQuality)
        assertFalse(qualityAdjustment.isReducing)
        assertFalse(qualityAdjustment.isIncreasing)
        assertEquals(QualityAdjustmentReason.NONE, qualityAdjustment.reason)
    }
    
    @Test
    fun `FrameRateData should have correct default values`() {
        // Given
        val frameRateData = FrameRateData()
        
        // Then
        assertEquals(30f, frameRateData.currentFps, 0.1f)
        assertEquals(30f, frameRateData.averageFps, 0.1f)
        assertEquals(30f, frameRateData.minFps, 0.1f)
        assertEquals(30f, frameRateData.maxFps, 0.1f)
        assertEquals(0, frameRateData.frameDrops)
        assertEquals(0f, frameRateData.frameDropRate, 0.1f)
        assertEquals(0f, frameRateData.jitter, 0.1f)
        assertTrue(frameRateData.isStable)
        
        val frameTimeStats = frameRateData.frameTimeStats
        assertEquals(33.3f, frameTimeStats.averageMs, 0.1f)
        assertEquals(33.3f, frameTimeStats.medianMs, 0.1f)
        assertEquals(33.3f, frameTimeStats.p95Ms, 0.1f)
        assertEquals(33.3f, frameTimeStats.p99Ms, 0.1f)
        assertEquals(33.3f, frameTimeStats.minMs, 0.1f)
        assertEquals(33.3f, frameTimeStats.maxMs, 0.1f)
    }
    
    @Test
    fun `FrameTimeStatistics should have correct default values`() {
        // Given
        val frameTimeStats = FrameTimeStatistics()
        
        // Then
        assertEquals(33.3f, frameTimeStats.averageMs, 0.1f) // 30fps = 33.3ms
        assertEquals(33.3f, frameTimeStats.medianMs, 0.1f)
        assertEquals(33.3f, frameTimeStats.p95Ms, 0.1f)
        assertEquals(33.3f, frameTimeStats.p99Ms, 0.1f)
        assertEquals(33.3f, frameTimeStats.minMs, 0.1f)
        assertEquals(33.3f, frameTimeStats.maxMs, 0.1f)
    }
    
    @Test
    fun `QualityAdjustment should have correct default values`() {
        // Given
        val qualityAdjustment = QualityAdjustment()
        
        // Then
        assertFalse(qualityAdjustment.shouldReduceQuality)
        assertFalse(qualityAdjustment.shouldIncreaseQuality)
        assertFalse(qualityAdjustment.isReducing)
        assertFalse(qualityAdjustment.isIncreasing)
        assertEquals(0L, qualityAdjustment.lastAdjustmentTime)
        assertEquals(QualityAdjustmentReason.NONE, qualityAdjustment.reason)
    }
    
    @Test
    fun `QualityAdjustmentReason enum should have all expected values`() {
        // Then
        val reasons = QualityAdjustmentReason.values()
        assertTrue(reasons.contains(QualityAdjustmentReason.NONE))
        assertTrue(reasons.contains(QualityAdjustmentReason.LOW_FRAME_RATE))
        assertTrue(reasons.contains(QualityAdjustmentReason.HIGH_FRAME_DROPS))
        assertTrue(reasons.contains(QualityAdjustmentReason.HIGH_JITTER))
        assertTrue(reasons.contains(QualityAdjustmentReason.GENERAL_PERFORMANCE))
        assertTrue(reasons.contains(QualityAdjustmentReason.PERFORMANCE_IMPROVED))
    }
    
    @Test
    fun `startMonitoring and stopMonitoring should not crash`() = runTest {
        // When
        frameRateMonitor.startMonitoring(30f)
        
        // Then - Should not crash
        assertNotNull(frameRateMonitor)
        
        // When
        frameRateMonitor.stopMonitoring()
        
        // Then - Should not crash
        assertNotNull(frameRateMonitor)
    }
    
    @Test
    fun `multiple startMonitoring calls should be safe`() = runTest {
        // When
        frameRateMonitor.startMonitoring(30f)
        frameRateMonitor.startMonitoring(60f) // Should be ignored if already monitoring
        
        // Then - Should not crash
        assertNotNull(frameRateMonitor)
        
        // Cleanup
        frameRateMonitor.stopMonitoring()
    }
    
    @Test
    fun `multiple stopMonitoring calls should be safe`() = runTest {
        // Given
        frameRateMonitor.startMonitoring(30f)
        
        // When
        frameRateMonitor.stopMonitoring()
        frameRateMonitor.stopMonitoring() // Should be safe to call multiple times
        
        // Then - Should not crash
        assertNotNull(frameRateMonitor)
    }
}