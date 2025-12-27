package com.example.vtubercamera.data.vrm

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for VRM file operations including parsing, loading, and caching
 */
@Singleton
class VRMManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val loader = VRMLoader(context)
    private val parser = VRMParser(context)
    private val meshExtractor = VRMMeshExtractor()
    private val textureExtractor = VRMTextureExtractor()
    private val expressionLoader = VRMExpressionLoader()
    private val poseLoader = VRMPoseLoader()
    
    // Simple in-memory cache
    private val modelCache = mutableMapOf<String, VRMModel>()
    private val meshCache = mutableMapOf<String, MeshData>()
    private val textureCache = mutableMapOf<String, TextureData>()
    private val expressionCache = mutableMapOf<String, ExpressionData>()
    private val poseCache = mutableMapOf<String, PoseData>()
    
    /**
     * Load complete VRM model with all components
     */
    fun loadCompleteVRM(uri: Uri): Flow<VRMLoadingProgress> = flow {
        emit(VRMLoadingProgress.Starting)
        
        try {
            // Basic validation
            emit(VRMLoadingProgress.Validating)
            val validation = loader.validateVRM(uri)
            if (validation !is ValidationResult.Valid) {
                emit(VRMLoadingProgress.Error(VRMLoadingError.InvalidFormat))
                return@flow
            }
            
            // Load basic VRM model
            emit(VRMLoadingProgress.LoadingModel(0.1f))
            val modelResult = parser.parseVRM(uri)
            
            modelResult.fold(
                onSuccess = { vrmModel ->
                    val modelId = vrmModel.id
                    modelCache[modelId] = vrmModel
                    
                    // Load detailed components
                    emit(VRMLoadingProgress.LoadingMesh(0.3f))
                    loadMeshData(modelId, vrmModel)
                    
                    emit(VRMLoadingProgress.LoadingTextures(0.5f))
                    loadTextureData(modelId, vrmModel)
                    
                    emit(VRMLoadingProgress.LoadingExpressions(0.7f))
                    loadExpressionData(modelId, vrmModel)
                    
                    emit(VRMLoadingProgress.LoadingPoses(0.9f))
                    loadPoseData(modelId, vrmModel)
                    
                    emit(VRMLoadingProgress.Complete(createCompleteVRMData(modelId, vrmModel)))
                },
                onFailure = { error ->
                    emit(VRMLoadingProgress.Error(error as? VRMLoadingError ?: VRMLoadingError.ParseError(error.message ?: "Unknown error")))
                }
            )
        } catch (e: Exception) {
            emit(VRMLoadingProgress.Error(VRMLoadingError.ParseError(e.message ?: "Loading failed")))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Load VRM model only (without detailed components)
     */
    suspend fun loadVRMModel(uri: Uri): Result<VRMModel> = withContext(Dispatchers.IO) {
        return@withContext parser.parseVRM(uri)
    }
    
    /**
     * Load mesh data for VRM model
     */
    private suspend fun loadMeshData(modelId: String, vrmModel: VRMModel): MeshData = withContext(Dispatchers.IO) {
        meshCache[modelId]?.let { return@withContext it }
        
        try {
            // Parse mesh data from VRM binary data
            val meshData = meshExtractor.extractMeshData(
                json = parseJsonFromVRM(vrmModel.meshData),
                binaryData = vrmModel.meshData
            )
            
            meshCache[modelId] = meshData
            meshData
        } catch (_: Exception) {
            MeshData.empty()
        }
    }
    
    /**
     * Load texture data for VRM model
     */
    private suspend fun loadTextureData(modelId: String, vrmModel: VRMModel): TextureData = withContext(Dispatchers.IO) {
        textureCache[modelId]?.let { return@withContext it }
        
        try {
            val textureData = textureExtractor.extractTextures(
                json = parseJsonFromVRM(vrmModel.meshData),
                binaryData = vrmModel.meshData,
                fullFileData = vrmModel.meshData
            )
            
            textureCache[modelId] = textureData
            textureData
        } catch (_: Exception) {
            TextureData.empty()
        }
    }
    
    /**
     * Load expression data for VRM model
     */
    private suspend fun loadExpressionData(modelId: String, vrmModel: VRMModel): ExpressionData = withContext(Dispatchers.IO) {
        expressionCache[modelId]?.let { return@withContext it }
        
        try {
            val meshData = meshCache[modelId] ?: MeshData.empty()
            val vrmExtension = extractVRMExtension(vrmModel.meshData)
            
            val expressionData = if (vrmExtension != null) {
                expressionLoader.loadExpressions(vrmExtension, meshData)
            } else {
                // Use the expressions from the model
                ExpressionData(
                    expressions = vrmModel.expressions,
                    blendShapeGroups = emptyList(),
                    presetExpressions = vrmModel.expressions.filter { isPresetExpression(it.name) },
                    customExpressions = vrmModel.expressions.filter { !isPresetExpression(it.name) }
                )
            }
            
            expressionCache[modelId] = expressionData
            expressionData
        } catch (_: Exception) {
            ExpressionData.empty()
        }
    }
    
    /**
     * Load pose data for VRM model
     */
    private suspend fun loadPoseData(modelId: String, vrmModel: VRMModel): PoseData = withContext(Dispatchers.IO) {
        poseCache[modelId]?.let { return@withContext it }
        
        try {
            val gltfJson = parseJsonFromVRM(vrmModel.meshData)
            val vrmExtension = extractVRMExtension(vrmModel.meshData)
            
            val poseData = poseLoader.loadPosesAndAnimations(
                json = gltfJson,
                binaryData = vrmModel.meshData,
                vrmExtension = vrmExtension
            )
            
            poseCache[modelId] = poseData
            poseData
        } catch (_: Exception) {
            // Use the poses from the model
            PoseData(
                poses = vrmModel.poses,
                animations = emptyList(),
                boneMapping = BoneMapping.empty(),
                staticPoses = vrmModel.poses,
                loopingAnimations = emptyList()
            )
        }
    }
    
    /**
     * Get cached VRM model
     */
    fun getCachedModel(modelId: String): VRMModel? = modelCache[modelId]
    
    /**
     * Get cached mesh data
     */
    fun getCachedMeshData(modelId: String): MeshData? = meshCache[modelId]
    
    /**
     * Get cached texture data
     */
    fun getCachedTextureData(modelId: String): TextureData? = textureCache[modelId]
    
    /**
     * Get cached expression data
     */
    fun getCachedExpressionData(modelId: String): ExpressionData? = expressionCache[modelId]
    
    /**
     * Get cached pose data
     */
    fun getCachedPoseData(modelId: String): PoseData? = poseCache[modelId]
    
    /**
     * Clear cache for specific model
     */
    fun clearModelCache(modelId: String) {
        modelCache.remove(modelId)
        meshCache.remove(modelId)
        textureCache.remove(modelId)
        expressionCache.remove(modelId)
        poseCache.remove(modelId)
    }
    
    /**
     * Clear all caches
     */
    fun clearAllCaches() {
        modelCache.clear()
        meshCache.clear()
        textureCache.clear()
        expressionCache.clear()
        poseCache.clear()
    }
    
    /**
     * Get memory usage statistics
     */
    fun getMemoryStats(): VRMMemoryStats {
        val modelMemory = modelCache.values.sumOf { it.getEstimatedMemoryUsage() }
        val meshMemory = meshCache.values.sumOf { it.getMemoryUsage() }
        val textureMemory = textureCache.values.sumOf { it.totalMemoryUsage }
        
        return VRMMemoryStats(
            totalMemoryUsage = modelMemory + meshMemory + textureMemory,
            modelMemory = modelMemory,
            meshMemory = meshMemory,
            textureMemory = textureMemory,
            cachedModels = modelCache.size,
            cachedMeshes = meshCache.size,
            cachedTextures = textureCache.size
        )
    }
    
    /**
     * Validate VRM file
     */
    suspend fun validateVRMFile(uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        return@withContext loader.validateVRM(uri)
    }
    
    /**
     * Get VRM file information
     */
    suspend fun getVRMFileInfo(uri: Uri): VRMFileInfo? = withContext(Dispatchers.IO) {
        return@withContext loader.getVRMInfo(uri)
    }
    
    /**
     * Check if device can load VRM
     */
    fun canLoadVRM(estimatedSize: Long): Boolean {
        return loader.canLoadVRM(estimatedSize)
    }
    
    /**
     * Create complete VRM data combining all components
     */
    private fun createCompleteVRMData(modelId: String, vrmModel: VRMModel): CompleteVRMData {
        return CompleteVRMData(
            model = vrmModel,
            meshData = meshCache[modelId] ?: MeshData.empty(),
            textureData = textureCache[modelId] ?: TextureData.empty(),
            expressionData = expressionCache[modelId] ?: ExpressionData.empty(),
            poseData = poseCache[modelId] ?: PoseData.empty()
        )
    }
    
    /**
     * Helper method to parse JSON from VRM binary data
     */
    private fun parseJsonFromVRM(vrmData: ByteArray): com.google.gson.JsonObject {
        // This is a simplified implementation
        // In reality, you would parse the glTF binary format
        return com.google.gson.JsonObject()
    }
    
    /**
     * Helper method to extract VRM extension
     */
    private fun extractVRMExtension(vrmData: ByteArray): com.google.gson.JsonObject? {
        // This is a simplified implementation
        // In reality, you would parse the VRM extension from glTF
        return null
    }
    
    /**
     * Check if expression name is a preset
     */
    private fun isPresetExpression(name: String): Boolean {
        val presets = setOf("happy", "angry", "sad", "relaxed", "surprised", "neutral", "blink")
        return presets.contains(name.lowercase())
    }
}

/**
 * VRM loading progress states
 */
sealed class VRMLoadingProgress {
    object Starting : VRMLoadingProgress()
    object Validating : VRMLoadingProgress()
    data class LoadingModel(val progress: Float) : VRMLoadingProgress()
    data class LoadingMesh(val progress: Float) : VRMLoadingProgress()
    data class LoadingTextures(val progress: Float) : VRMLoadingProgress()
    data class LoadingExpressions(val progress: Float) : VRMLoadingProgress()
    data class LoadingPoses(val progress: Float) : VRMLoadingProgress()
    data class Complete(val vrmData: CompleteVRMData) : VRMLoadingProgress()
    data class Error(val error: VRMLoadingError) : VRMLoadingProgress()
}

/**
 * Complete VRM data containing all components
 */
data class CompleteVRMData(
    val model: VRMModel,
    val meshData: MeshData,
    val textureData: TextureData,
    val expressionData: ExpressionData,
    val poseData: PoseData
) {
    /**
     * Get comprehensive statistics
     */
    fun getStatistics(): CompleteVRMStatistics {
        return CompleteVRMStatistics(
            modelStats = model.getStatistics(),
            meshStats = meshData,
            textureStats = textureData,
            expressionStats = expressionData.getStatistics(),
            poseStats = poseData,
            totalMemoryUsage = model.getEstimatedMemoryUsage() + 
                             meshData.getMemoryUsage() + 
                             textureData.totalMemoryUsage
        )
    }
    
    /**
     * Check if VRM is ready for rendering
     */
    fun isReadyForRendering(): Boolean {
        return !meshData.isEmpty() && model.hasExpressions()
    }
    
    /**
     * Get complexity rating
     */
    fun getComplexityRating(): VRMComplexity {
        val meshComplexity = when {
            meshData.totalTriangles < 5000 -> 1
            meshData.totalTriangles < 15000 -> 2
            meshData.totalTriangles < 30000 -> 3
            meshData.totalTriangles < 50000 -> 4
            else -> 5
        }
        
        val textureComplexity = when {
            textureData.totalMemoryUsage < 10 * 1024 * 1024 -> 1
            textureData.totalMemoryUsage < 50 * 1024 * 1024 -> 2
            textureData.totalMemoryUsage < 100 * 1024 * 1024 -> 3
            textureData.totalMemoryUsage < 200 * 1024 * 1024 -> 4
            else -> 5
        }
        
        val expressionComplexity = when {
            expressionData.expressions.size < 10 -> 1
            expressionData.expressions.size < 25 -> 2
            expressionData.expressions.size < 50 -> 3
            expressionData.expressions.size < 100 -> 4
            else -> 5
        }
        
        val overallComplexity = (meshComplexity + textureComplexity + expressionComplexity) / 3
        
        return VRMComplexity(
            overall = overallComplexity,
            mesh = meshComplexity,
            texture = textureComplexity,
            expression = expressionComplexity,
            pose = if (poseData.poses.size > 10) 3 else 1
        )
    }
}

/**
 * VRM memory usage statistics
 */
data class VRMMemoryStats(
    val totalMemoryUsage: Long,
    val modelMemory: Long,
    val meshMemory: Long,
    val textureMemory: Long,
    val cachedModels: Int,
    val cachedMeshes: Int,
    val cachedTextures: Int
) {
    /**
     * Get formatted total memory usage
     */
    fun getFormattedTotalMemory(): String {
        return formatBytes(totalMemoryUsage)
    }
    
    /**
     * Get memory breakdown
     */
    fun getMemoryBreakdown(): Map<String, Long> {
        return mapOf(
            "Models" to modelMemory,
            "Meshes" to meshMemory,
            "Textures" to textureMemory
        )
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> "${bytes / (1024 * 1024 * 1024)}GB"
        }
    }
}

/**
 * Complete VRM statistics
 */
data class CompleteVRMStatistics(
    val modelStats: ModelStatistics,
    val meshStats: MeshData,
    val textureStats: TextureData,
    val expressionStats: ExpressionStatistics,
    val poseStats: PoseData,
    val totalMemoryUsage: Long
)

/**
 * VRM complexity rating
 */
data class VRMComplexity(
    val overall: Int,
    val mesh: Int,
    val texture: Int,
    val expression: Int,
    val pose: Int
) {
    /**
     * Check if VRM is suitable for mobile rendering
     */
    fun isMobileFriendly(): Boolean = overall <= 3
    
    /**
     * Check if VRM is high performance
     */
    fun isHighPerformance(): Boolean = overall >= 4
    
    /**
     * Get recommendation text
     */
    fun getRecommendation(): String {
        return when (overall) {
            1, 2 -> "Excellent for mobile devices"
            3 -> "Good for most devices"
            4 -> "May impact performance on older devices"
            5 -> "Recommended for high-end devices only"
            else -> "Unknown complexity"
        }
    }
}