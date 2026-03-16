package com.example.vtubercamera.data.vrm

class VRMAssetCache {
    private val modelCache = mutableMapOf<String, VRMModel>()
    private val meshCache = mutableMapOf<String, MeshData>()
    private val textureCache = mutableMapOf<String, TextureData>()
    private val expressionCache = mutableMapOf<String, ExpressionData>()
    private val poseCache = mutableMapOf<String, PoseData>()

    fun putModel(modelId: String, model: VRMModel) {
        modelCache[modelId] = model
    }

    fun getModel(modelId: String): VRMModel? = modelCache[modelId]

    fun putMesh(modelId: String, meshData: MeshData) {
        meshCache[modelId] = meshData
    }

    fun getMesh(modelId: String): MeshData? = meshCache[modelId]

    fun putTexture(modelId: String, textureData: TextureData) {
        textureCache[modelId] = textureData
    }

    fun getTexture(modelId: String): TextureData? = textureCache[modelId]

    fun putExpression(modelId: String, expressionData: ExpressionData) {
        expressionCache[modelId] = expressionData
    }

    fun getExpression(modelId: String): ExpressionData? = expressionCache[modelId]

    fun putPose(modelId: String, poseData: PoseData) {
        poseCache[modelId] = poseData
    }

    fun getPose(modelId: String): PoseData? = poseCache[modelId]

    fun clearModel(modelId: String) {
        modelCache.remove(modelId)
        meshCache.remove(modelId)
        textureCache.remove(modelId)
        expressionCache.remove(modelId)
        poseCache.remove(modelId)
    }

    fun clearAll() {
        modelCache.clear()
        meshCache.clear()
        textureCache.clear()
        expressionCache.clear()
        poseCache.clear()
    }

    fun memoryStats(): VRMMemoryStats {
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
            cachedTextures = textureCache.size,
        )
    }
}
