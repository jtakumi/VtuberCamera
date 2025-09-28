# AR Avatar Performance Optimization Guide

## Overview

This guide provides comprehensive performance optimization strategies for the AR Avatar feature in VTuber Camera. It covers memory management, rendering optimization, battery efficiency, and device-specific considerations.

## Performance Targets

### Frame Rate Targets
- **Minimum**: 24 FPS (acceptable for basic usage)
- **Target**: 30 FPS (smooth user experience)
- **Optimal**: 60 FPS (premium experience on high-end devices)

### Memory Targets
- **Base Memory**: <150MB (without avatar loaded)
- **With Avatar**: <250MB (single avatar loaded)
- **Peak Memory**: <400MB (during intensive operations)

### Battery Targets
- **Efficiency**: 90%+ compared to standard camera usage
- **Thermal**: No thermal throttling under normal usage
- **Background**: Minimal impact when not actively used

## Memory Optimization

### VRM Model Management

#### Efficient Loading Strategy
```kotlin
class VRMRepositoryImpl {
    private val modelCache = LruCache<String, VRMModel>(maxSize = 3)
    private val texturePool = TexturePool(maxTextures = 20)
    
    suspend fun loadVRMFromUri(uri: Uri): Result<VRMModel> {
        // Check cache first
        val cached = modelCache.get(uri.toString())
        if (cached != null) return Result.success(cached)
        
        // Load with memory monitoring
        return withContext(Dispatchers.IO) {
            val memoryBefore = getAvailableMemory()
            try {
                val model = parseVRMFile(uri)
                val optimizedModel = optimizeForDevice(model)
                modelCache.put(uri.toString(), optimizedModel)
                Result.success(optimizedModel)
            } catch (e: OutOfMemoryError) {
                // Clear cache and retry with lower quality
                clearCache()
                val lowQualityModel = parseVRMFileWithLowerQuality(uri)
                Result.success(lowQualityModel)
            }
        }
    }
}
```

#### Texture Optimization
```kotlin
class TextureOptimizer {
    fun optimizeTexture(texture: ByteArray, deviceTier: DeviceTier): ByteArray {
        return when (deviceTier) {
            DeviceTier.HIGH_END -> {
                // Keep original quality
                texture
            }
            DeviceTier.MID_RANGE -> {
                // Compress to 75% quality
                compressTexture(texture, quality = 0.75f)
            }
            DeviceTier.LOW_END -> {
                // Compress to 50% quality and reduce resolution
                val compressed = compressTexture(texture, quality = 0.5f)
                resizeTexture(compressed, scaleFactor = 0.5f)
            }
        }
    }
}
```

### Memory Monitoring
```kotlin
class MemoryMonitor {
    private val memoryThreshold = 0.8f // 80% of available memory
    
    fun checkMemoryPressure(): MemoryPressure {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryRatio = usedMemory.toFloat() / maxMemory.toFloat()
        
        return when {
            memoryRatio > 0.9f -> MemoryPressure.CRITICAL
            memoryRatio > 0.8f -> MemoryPressure.HIGH
            memoryRatio > 0.6f -> MemoryPressure.MODERATE
            else -> MemoryPressure.LOW
        }
    }
    
    fun handleMemoryPressure(pressure: MemoryPressure) {
        when (pressure) {
            MemoryPressure.CRITICAL -> {
                clearAllCaches()
                reduceRenderQuality()
                System.gc()
            }
            MemoryPressure.HIGH -> {
                clearOldCaches()
                reduceTextureQuality()
            }
            MemoryPressure.MODERATE -> {
                clearUnusedTextures()
            }
            MemoryPressure.LOW -> {
                // No action needed
            }
        }
    }
}
```

## Rendering Optimization

### Level of Detail (LOD) System

#### Distance-Based LOD
```kotlin
class LODManager {
    private val lodLevels = listOf(
        LODLevel(distance = 0f..2f, quality = 1.0f),      // High detail
        LODLevel(distance = 2f..5f, quality = 0.7f),      // Medium detail
        LODLevel(distance = 5f..10f, quality = 0.4f),     // Low detail
        LODLevel(distance = 10f..Float.MAX_VALUE, quality = 0.2f) // Minimal detail
    )
    
    fun calculateLOD(avatarDistance: Float): Float {
        return lodLevels.find { avatarDistance in it.distance }?.quality ?: 0.2f
    }
    
    fun applyLOD(vrmModel: VRMModel, lodLevel: Float): VRMModel {
        return vrmModel.copy(
            meshData = reduceMeshComplexity(vrmModel.meshData, lodLevel),
            textureData = reduceTextureResolution(vrmModel.textureData, lodLevel)
        )
    }
}
```

#### Performance-Based LOD
```kotlin
class AdaptiveLODManager {
    private var currentFrameRate = 60f
    private val targetFrameRate = 30f
    
    fun updateLOD(currentFPS: Float): Float {
        currentFrameRate = currentFPS
        
        return when {
            currentFPS < targetFrameRate * 0.8f -> {
                // Reduce quality significantly
                0.5f
            }
            currentFPS < targetFrameRate -> {
                // Reduce quality moderately
                0.7f
            }
            currentFPS > targetFrameRate * 1.2f -> {
                // Can increase quality
                1.0f
            }
            else -> {
                // Maintain current quality
                getCurrentLODLevel()
            }
        }
    }
}
```

### Culling Optimizations

#### Frustum Culling
```kotlin
class FrustumCuller {
    fun isInFrustum(avatarBounds: BoundingBox, camera: Camera): Boolean {
        val frustum = camera.frustum
        
        // Check if bounding box intersects with camera frustum
        return frustum.intersects(avatarBounds)
    }
    
    fun cullAvatars(avatars: List<Avatar>, camera: Camera): List<Avatar> {
        return avatars.filter { avatar ->
            isInFrustum(avatar.boundingBox, camera)
        }
    }
}
```

#### Occlusion Culling
```kotlin
class OcclusionCuller {
    fun isOccluded(avatar: Avatar, camera: Camera, scene: Scene): Boolean {
        // Simplified occlusion test using ray casting
        val ray = Ray(camera.position, avatar.position - camera.position)
        val hits = scene.raycast(ray)
        
        return hits.any { hit ->
            hit.distance < avatar.distanceFromCamera && hit.isOpaque
        }
    }
}
```

### Batch Rendering
```kotlin
class BatchRenderer {
    private val renderBatches = mutableMapOf<Material, MutableList<RenderItem>>()
    
    fun addToBatch(item: RenderItem) {
        val batch = renderBatches.getOrPut(item.material) { mutableListOf() }
        batch.add(item)
    }
    
    fun renderBatches() {
        renderBatches.forEach { (material, items) ->
            // Set material once for all items
            setMaterial(material)
            
            // Render all items with the same material
            items.forEach { item ->
                renderItem(item)
            }
        }
        
        // Clear batches for next frame
        renderBatches.clear()
    }
}
```

## Frame Rate Optimization

### Adaptive Frame Rate Control
```kotlin
class FrameRateController {
    private var targetFrameRate = 30f
    private val frameTimeHistory = CircularBuffer<Float>(size = 60)
    
    fun updateFrameRate(deltaTime: Float) {
        frameTimeHistory.add(deltaTime)
        
        val averageFrameTime = frameTimeHistory.average()
        val currentFPS = 1f / averageFrameTime
        
        when {
            currentFPS < targetFrameRate * 0.8f -> {
                // Reduce rendering load
                reduceRenderingQuality()
                enableFrameSkipping()
            }
            currentFPS > targetFrameRate * 1.2f -> {
                // Can increase quality
                increaseRenderingQuality()
                disableFrameSkipping()
            }
        }
    }
    
    private fun reduceRenderingQuality() {
        // Reduce shadow quality
        setShadowQuality(ShadowQuality.LOW)
        
        // Reduce texture filtering
        setTextureFiltering(TextureFilter.BILINEAR)
        
        // Reduce post-processing effects
        disablePostProcessing()
    }
}
```

### Frame Skipping Strategy
```kotlin
class FrameSkipper {
    private var skipCounter = 0
    private var skipPattern = 0 // 0 = no skip, 1 = skip every other frame
    
    fun shouldSkipFrame(): Boolean {
        if (skipPattern == 0) return false
        
        skipCounter++
        val shouldSkip = skipCounter % (skipPattern + 1) != 0
        
        return shouldSkip
    }
    
    fun adjustSkipPattern(currentFPS: Float, targetFPS: Float) {
        skipPattern = when {
            currentFPS < targetFPS * 0.5f -> 2 // Skip 2 out of 3 frames
            currentFPS < targetFPS * 0.8f -> 1 // Skip every other frame
            else -> 0 // No skipping
        }
    }
}
```

## Battery Optimization

### Power-Aware Rendering
```kotlin
class PowerManager {
    fun getBatteryLevel(): Float {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) / 100f
    }
    
    fun getThermalState(): ThermalState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NORMAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT_THROTTLING
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE_THROTTLING
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE_THROTTLING
            else -> ThermalState.CRITICAL_THROTTLING
        }
    }
    
    fun adjustPerformanceForPower() {
        val batteryLevel = getBatteryLevel()
        val thermalState = getThermalState()
        
        when {
            batteryLevel < 0.2f || thermalState >= ThermalState.MODERATE_THROTTLING -> {
                // Aggressive power saving
                setFrameRate(15f)
                setRenderQuality(0.3f)
                disableNonEssentialFeatures()
            }
            batteryLevel < 0.5f || thermalState >= ThermalState.LIGHT_THROTTLING -> {
                // Moderate power saving
                setFrameRate(24f)
                setRenderQuality(0.6f)
            }
            else -> {
                // Normal operation
                setFrameRate(30f)
                setRenderQuality(1.0f)
            }
        }
    }
}
```

### Background Processing Optimization
```kotlin
class BackgroundOptimizer {
    fun onAppPaused() {
        // Pause non-essential background tasks
        pauseVRMPreloading()
        pauseTextureOptimization()
        pauseAnalytics()
        
        // Reduce update frequency
        setUpdateInterval(1000) // 1 second instead of 16ms
    }
    
    fun onAppResumed() {
        // Resume normal operation
        resumeVRMPreloading()
        resumeTextureOptimization()
        resumeAnalytics()
        
        // Restore normal update frequency
        setUpdateInterval(16) // 60 FPS
    }
}
```

## Device-Specific Optimizations

### Device Tier Detection
```kotlin
class DeviceTierDetector {
    fun detectDeviceTier(): DeviceTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRAM = memoryInfo.totalMem / (1024 * 1024) // MB
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val gpuRenderer = getGPURenderer()
        
        return when {
            totalRAM >= 8192 && cpuCores >= 8 && isHighEndGPU(gpuRenderer) -> {
                DeviceTier.HIGH_END
            }
            totalRAM >= 4096 && cpuCores >= 6 -> {
                DeviceTier.MID_RANGE
            }
            else -> {
                DeviceTier.LOW_END
            }
        }
    }
    
    private fun isHighEndGPU(renderer: String): Boolean {
        val highEndGPUs = listOf(
            "Adreno 730", "Adreno 740", "Adreno 750",
            "Mali-G78", "Mali-G710", "Mali-G715",
            "PowerVR", "Tegra"
        )
        
        return highEndGPUs.any { gpu ->
            renderer.contains(gpu, ignoreCase = true)
        }
    }
}
```

### Tier-Specific Settings
```kotlin
class TierOptimizer {
    fun applyTierOptimizations(tier: DeviceTier) {
        when (tier) {
            DeviceTier.HIGH_END -> {
                setMaxTextureSize(2048)
                setShadowMapSize(1024)
                setAntiAliasing(AntiAliasing.MSAA_4X)
                setPostProcessing(true)
                setMaxAvatars(3)
            }
            DeviceTier.MID_RANGE -> {
                setMaxTextureSize(1024)
                setShadowMapSize(512)
                setAntiAliasing(AntiAliasing.FXAA)
                setPostProcessing(false)
                setMaxAvatars(2)
            }
            DeviceTier.LOW_END -> {
                setMaxTextureSize(512)
                setShadowMapSize(256)
                setAntiAliasing(AntiAliasing.NONE)
                setPostProcessing(false)
                setMaxAvatars(1)
            }
        }
    }
}
```

## Profiling and Monitoring

### Performance Metrics Collection
```kotlin
class PerformanceProfiler {
    private val frameTimeHistory = CircularBuffer<Float>(size = 300) // 5 seconds at 60fps
    private val memoryHistory = CircularBuffer<Long>(size = 60) // 1 minute at 1 sample/second
    
    fun recordFrameTime(deltaTime: Float) {
        frameTimeHistory.add(deltaTime)
    }
    
    fun recordMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        memoryHistory.add(usedMemory)
    }
    
    fun getPerformanceReport(): PerformanceReport {
        val avgFrameTime = frameTimeHistory.average()
        val avgFPS = 1f / avgFrameTime
        val minFPS = 1f / frameTimeHistory.max()
        val maxFPS = 1f / frameTimeHistory.min()
        
        val avgMemory = memoryHistory.average()
        val maxMemory = memoryHistory.max()
        
        return PerformanceReport(
            averageFPS = avgFPS,
            minimumFPS = minFPS,
            maximumFPS = maxFPS,
            averageMemoryMB = avgMemory / (1024 * 1024),
            peakMemoryMB = maxMemory / (1024 * 1024),
            frameTimeVariance = frameTimeHistory.variance()
        )
    }
}
```

### Real-Time Performance Overlay
```kotlin
@Composable
fun PerformanceOverlay(
    performanceData: PerformanceData,
    modifier: Modifier = Modifier
) {
    if (BuildConfig.DEBUG) {
        Column(
            modifier = modifier
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp)
        ) {
            Text(
                text = "FPS: ${performanceData.currentFPS.toInt()}",
                color = when {
                    performanceData.currentFPS >= 30f -> Color.Green
                    performanceData.currentFPS >= 24f -> Color.Yellow
                    else -> Color.Red
                }
            )
            
            Text(
                text = "Memory: ${performanceData.memoryUsageMB.toInt()}MB",
                color = when {
                    performanceData.memoryUsageMB < 200f -> Color.Green
                    performanceData.memoryUsageMB < 300f -> Color.Yellow
                    else -> Color.Red
                }
            )
            
            Text(
                text = "Battery: ${(performanceData.batteryLevel * 100).toInt()}%",
                color = when {
                    performanceData.batteryLevel > 0.5f -> Color.Green
                    performanceData.batteryLevel > 0.2f -> Color.Yellow
                    else -> Color.Red
                }
            )
        }
    }
}
```

## Best Practices

### Memory Management
1. **Use Object Pools**: Reuse frequently created objects
2. **Lazy Loading**: Load resources only when needed
3. **Cache Wisely**: Balance memory usage with performance
4. **Monitor Pressure**: React to memory pressure events
5. **Clean Up**: Properly dispose of resources

### Rendering Performance
1. **Batch Operations**: Group similar rendering operations
2. **Cull Aggressively**: Skip unnecessary rendering
3. **Use LOD**: Reduce detail for distant objects
4. **Optimize Shaders**: Keep fragment shaders simple
5. **Minimize State Changes**: Group by material and state

### Battery Efficiency
1. **Adaptive Quality**: Adjust based on battery level
2. **Thermal Awareness**: Reduce load when device is hot
3. **Background Optimization**: Minimize background processing
4. **Frame Rate Control**: Don't render faster than needed
5. **Power-Aware Features**: Disable non-essential features

### Testing and Validation
1. **Profile Regularly**: Use Android Studio profiler
2. **Test on Real Devices**: Emulators don't show real performance
3. **Test Different Tiers**: Cover low, mid, and high-end devices
4. **Monitor Metrics**: Track performance over time
5. **User Feedback**: Listen to user reports about performance

## Troubleshooting Common Issues

### Low Frame Rate
- Check GPU utilization
- Reduce rendering complexity
- Enable frame skipping
- Lower texture quality
- Disable post-processing

### High Memory Usage
- Clear unused caches
- Reduce texture sizes
- Implement object pooling
- Check for memory leaks
- Use memory profiler

### Battery Drain
- Reduce frame rate
- Lower rendering quality
- Disable background processing
- Check for wake locks
- Monitor CPU usage

### Thermal Throttling
- Reduce GPU load
- Lower frame rate
- Disable intensive features
- Add thermal monitoring
- Implement cooling periods

## Conclusion

Performance optimization for AR Avatar features requires a holistic approach covering memory management, rendering efficiency, and power consumption. The key is to implement adaptive systems that can adjust quality and performance based on device capabilities and current conditions.

Regular profiling and testing on real devices across different performance tiers is essential for maintaining optimal user experience. The strategies outlined in this guide provide a foundation for achieving smooth, efficient AR avatar rendering while preserving battery life and preventing thermal issues.