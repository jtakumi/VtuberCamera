package com.example.vtubercamera.data.vrm

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.example.vtubercamera.data.vrm.math.Vector3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads expression and blend shape data from VRM files
 */
class VRMExpressionLoader {
    
    companion object {
        // Standard VRM expression presets
        private val VRM_PRESET_EXPRESSIONS = setOf(
            "happy", "angry", "sad", "relaxed", "surprised",
            "aa", "ih", "ou", "ee", "oh",
            "blink", "blinkLeft", "blinkRight",
            "lookUp", "lookDown", "lookLeft", "lookRight",
            "neutral"
        )
        
        // Common blend shape naming conventions
        private val COMMON_BLEND_SHAPE_NAMES = mapOf(
            "eye_blink_left" to "blinkLeft",
            "eye_blink_right" to "blinkRight",
            "eye_look_up_left" to "lookUp",
            "eye_look_down_left" to "lookDown",
            "eye_look_in_left" to "lookLeft",
            "eye_look_out_left" to "lookRight",
            "mouth_smile_left" to "happy",
            "mouth_frown_left" to "sad",
            "mouth_funnel" to "oh",
            "mouth_pucker" to "ou",
            "jaw_open" to "aa"
        )
    }
    
    /**
     * Load expressions from VRM extension data
     */
    suspend fun loadExpressions(
        vrmExtension: JsonObject,
        meshData: MeshData
    ): ExpressionData = withContext(Dispatchers.IO) {
        val expressions = mutableListOf<Expression>()
        val blendShapeGroups = mutableListOf<BlendShapeGroup>()
        
        try {
            // Handle VRM 1.0 expressions
            val expressionsObj = vrmExtension.getAsJsonObject("expressions")
            if (expressionsObj != null) {
                val presets = expressionsObj.getAsJsonObject("preset")
                val customs = expressionsObj.getAsJsonArray("custom")
                
                // Load preset expressions
                if (presets != null) {
                    expressions.addAll(loadPresetExpressions(presets, meshData))
                }
                
                // Load custom expressions
                if (customs != null) {
                    expressions.addAll(loadCustomExpressions(customs, meshData))
                }
            }
            
            // Handle VRM 0.x blend shapes (for backward compatibility)
            val blendShapeMaster = vrmExtension.getAsJsonObject("blendShapeMaster")
            if (blendShapeMaster != null) {
                val blendShapeGroupsArray = blendShapeMaster.getAsJsonArray("blendShapeGroups")
                if (blendShapeGroupsArray != null) {
                    expressions.addAll(loadLegacyBlendShapes(blendShapeGroupsArray, meshData))
                }
            }
            
            // Create blend shape groups for organization
            blendShapeGroups.addAll(createBlendShapeGroups(expressions))
            
        } catch (e: Exception) {
            // Log error but continue with what we have
        }
        
        ExpressionData(
            expressions = expressions,
            blendShapeGroups = blendShapeGroups,
            presetExpressions = expressions.filter { isPresetExpression(it.name) },
            customExpressions = expressions.filter { !isPresetExpression(it.name) }
        )
    }
    
    /**
     * Load preset expressions from VRM 1.0 format
     */
    private suspend fun loadPresetExpressions(
        presets: JsonObject,
        meshData: MeshData
    ): List<Expression> = withContext(Dispatchers.IO) {
        val expressions = mutableListOf<Expression>()
        
        VRM_PRESET_EXPRESSIONS.forEach { presetName ->
            val presetData = presets.getAsJsonObject(presetName)
            if (presetData != null) {
                val expression = parseExpression(presetName, presetData, meshData, true)
                if (expression != null) {
                    expressions.add(expression)
                }
            }
        }
        
        expressions
    }
    
    /**
     * Load custom expressions from VRM 1.0 format
     */
    private suspend fun loadCustomExpressions(
        customs: JsonArray,
        meshData: MeshData
    ): List<Expression> = withContext(Dispatchers.IO) {
        val expressions = mutableListOf<Expression>()
        
        for (i in 0 until customs.size()) {
            val customData = customs[i].asJsonObject
            val name = customData.get("name")?.asString ?: "custom_$i"
            val expression = parseExpression(name, customData, meshData, false)
            if (expression != null) {
                expressions.add(expression)
            }
        }
        
        expressions
    }
    
    /**
     * Load legacy blend shapes from VRM 0.x format
     */
    private suspend fun loadLegacyBlendShapes(
        blendShapeGroups: JsonArray,
        meshData: MeshData
    ): List<Expression> = withContext(Dispatchers.IO) {
        val expressions = mutableListOf<Expression>()
        
        for (i in 0 until blendShapeGroups.size()) {
            val groupData = blendShapeGroups[i].asJsonObject
            val expression = parseLegacyBlendShapeGroup(groupData, meshData)
            if (expression != null) {
                expressions.add(expression)
            }
        }
        
        expressions
    }
    
    /**
     * Parse a single expression from JSON data
     */
    private fun parseExpression(
        name: String,
        expressionData: JsonObject,
        meshData: MeshData,
        isPreset: Boolean
    ): Expression? {
        try {
            val displayName = expressionData.get("name")?.asString ?: name.replaceFirstChar { it.uppercase() }
            val isBinary = expressionData.get("isBinary")?.asBoolean ?: false
            
            // Parse morph target bindings (blend shapes)
            val morphTargetBinds = expressionData.getAsJsonArray("morphTargetBinds")
            val blendShapeKeys = mutableMapOf<String, Float>()
            
            morphTargetBinds?.forEach { bindElement ->
                val bind = bindElement.asJsonObject
                val node = bind.get("node")?.asInt
                val index = bind.get("index")?.asInt
                val weight = bind.get("weight")?.asFloat ?: 0f
                
                if (node != null && index != null) {
                    val shapeName = generateBlendShapeName(node, index, meshData)
                    blendShapeKeys[shapeName] = weight
                }
            }
            
            // Parse material color bindings
            val materialColorBinds = expressionData.getAsJsonArray("materialColorBinds")
            val materialColorBindings = mutableMapOf<String, Expression.MaterialColorBinding>()
            
            materialColorBinds?.forEach { bindElement ->
                val bind = bindElement.asJsonObject
                val material = bind.get("material")?.asInt
                val type = bind.get("type")?.asString ?: "color"
                val targetValue = bind.getAsJsonArray("targetValue")
                
                if (material != null && targetValue != null) {
                    val materialName = "material_$material"
                    val colorArray = FloatArray(targetValue.size()) { i ->
                        targetValue[i].asFloat
                    }
                    
                    materialColorBindings[materialName] = Expression.MaterialColorBinding(
                        materialName = materialName,
                        propertyName = type,
                        targetValue = colorArray
                    )
                }
            }
            
            // Parse texture transform bindings
            val textureTransformBinds = expressionData.getAsJsonArray("textureTransformBinds")
            val textureTransformBindings = mutableMapOf<String, Expression.TextureTransformBinding>()
            
            textureTransformBinds?.forEach { bindElement ->
                val bind = bindElement.asJsonObject
                val material = bind.get("material")?.asInt
                val scaling = bind.getAsJsonArray("scaling")
                val offset = bind.getAsJsonArray("offset")
                
                if (material != null) {
                    val materialName = "material_$material"
                    val scale = if (scaling != null && scaling.size() >= 2) {
                        Pair(scaling[0].asFloat, scaling[1].asFloat)
                    } else {
                        Pair(1f, 1f)
                    }
                    val offsetPair = if (offset != null && offset.size() >= 2) {
                        Pair(offset[0].asFloat, offset[1].asFloat)
                    } else {
                        Pair(0f, 0f)
                    }
                    
                    textureTransformBindings[materialName] = Expression.TextureTransformBinding(
                        materialName = materialName,
                        propertyName = "mainTexture",
                        scale = scale,
                        offset = offsetPair
                    )
                }
            }
            
            // Parse override settings
            val overrideBlink = parseOverrideType(expressionData.get("overrideBlink")?.asString)
            val overrideLookAt = parseOverrideType(expressionData.get("overrideLookAt")?.asString)
            val overrideMouth = parseOverrideType(expressionData.get("overrideMouth")?.asString)
            
            return Expression(
                name = name,
                displayName = displayName,
                blendShapeKeys = blendShapeKeys,
                materialColorBindings = materialColorBindings,
                textureTransformBindings = textureTransformBindings,
                isBinary = isBinary,
                overrideBlink = overrideBlink,
                overrideLookAt = overrideLookAt,
                overrideMouth = overrideMouth
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Parse legacy blend shape group from VRM 0.x
     */
    private fun parseLegacyBlendShapeGroup(
        groupData: JsonObject,
        meshData: MeshData
    ): Expression? {
        try {
            val name = groupData.get("name")?.asString ?: return null
            val presetName = groupData.get("presetName")?.asString
            val isBinary = groupData.get("isBinary")?.asBoolean ?: false
            
            val binds = groupData.getAsJsonArray("binds")
            val blendShapeKeys = mutableMapOf<String, Float>()
            
            binds?.forEach { bindElement ->
                val bind = bindElement.asJsonObject
                val mesh = bind.get("mesh")?.asInt
                val index = bind.get("index")?.asInt
                val weight = bind.get("weight")?.asFloat ?: 0f
                
                if (mesh != null && index != null) {
                    val shapeName = generateBlendShapeName(mesh, index, meshData)
                    blendShapeKeys[shapeName] = weight
                }
            }
            
            val materialValues = groupData.getAsJsonArray("materialValues")
            val materialColorBindings = mutableMapOf<String, Expression.MaterialColorBinding>()
            
            materialValues?.forEach { valueElement ->
                val valueData = valueElement.asJsonObject
                val materialName = valueData.get("materialName")?.asString ?: return@forEach
                val propertyName = valueData.get("propertyName")?.asString ?: return@forEach
                val targetValue = valueData.getAsJsonArray("targetValue")
                
                if (targetValue != null) {
                    val colorArray = FloatArray(targetValue.size()) { i ->
                        targetValue[i].asFloat
                    }
                    
                    materialColorBindings[materialName] = Expression.MaterialColorBinding(
                        materialName = materialName,
                        propertyName = propertyName,
                        targetValue = colorArray
                    )
                }
            }
            
            return Expression(
                name = presetName ?: name,
                displayName = name.replaceFirstChar { it.uppercase() },
                blendShapeKeys = blendShapeKeys,
                materialColorBindings = materialColorBindings,
                isBinary = isBinary
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Generate blend shape name from mesh and index
     */
    private fun generateBlendShapeName(meshIndex: Int, shapeIndex: Int, meshData: MeshData): String {
        val meshName = if (meshIndex < meshData.meshes.size) {
            meshData.meshes[meshIndex].name
        } else {
            "mesh_$meshIndex"
        }
        return "${meshName}_shape_$shapeIndex"
    }
    
    /**
     * Parse override type from string
     */
    private fun parseOverrideType(value: String?): Expression.OverrideType {
        return when (value?.lowercase()) {
            "block" -> Expression.OverrideType.BLOCK
            "blend" -> Expression.OverrideType.BLEND
            else -> Expression.OverrideType.NONE
        }
    }
    
    /**
     * Check if expression name is a preset
     */
    private fun isPresetExpression(name: String): Boolean {
        return VRM_PRESET_EXPRESSIONS.contains(name.lowercase())
    }
    
    /**
     * Create blend shape groups for organization
     */
    private fun createBlendShapeGroups(expressions: List<Expression>): List<BlendShapeGroup> {
        val groups = mutableListOf<BlendShapeGroup>()
        
        // Group by category
        val faceExpressions = expressions.filter { expr ->
            listOf("happy", "sad", "angry", "surprised", "relaxed").any { 
                expr.name.contains(it, ignoreCase = true) 
            }
        }
        
        val eyeExpressions = expressions.filter { expr ->
            listOf("blink", "look", "eye").any { 
                expr.name.contains(it, ignoreCase = true) 
            }
        }
        
        val mouthExpressions = expressions.filter { expr ->
            listOf("aa", "ih", "ou", "ee", "oh", "mouth").any { 
                expr.name.contains(it, ignoreCase = true) 
            }
        }
        
        val customExpressions = expressions.filter { expr ->
            !faceExpressions.contains(expr) && 
            !eyeExpressions.contains(expr) && 
            !mouthExpressions.contains(expr)
        }
        
        if (faceExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Face Expressions",
                expressions = faceExpressions,
                category = BlendShapeCategory.EMOTION
            ))
        }
        
        if (eyeExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Eye Expressions",
                expressions = eyeExpressions,
                category = BlendShapeCategory.EYE
            ))
        }
        
        if (mouthExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Mouth Expressions",
                expressions = mouthExpressions,
                category = BlendShapeCategory.MOUTH
            ))
        }
        
        if (customExpressions.isNotEmpty()) {
            groups.add(BlendShapeGroup(
                name = "Custom Expressions",
                expressions = customExpressions,
                category = BlendShapeCategory.CUSTOM
            ))
        }
        
        return groups
    }
    
    /**
     * Analyze expression complexity
     */
    fun analyzeExpressionComplexity(expression: Expression): ExpressionComplexity {
        val blendShapeCount = expression.blendShapeKeys.size
        val materialBindingCount = expression.materialColorBindings.size
        val textureBindingCount = expression.textureTransformBindings.size
        
        val totalBindings = blendShapeCount + materialBindingCount + textureBindingCount
        
        val complexity = when {
            totalBindings == 0 -> ExpressionComplexityLevel.EMPTY
            totalBindings <= 5 -> ExpressionComplexityLevel.SIMPLE
            totalBindings <= 15 -> ExpressionComplexityLevel.MODERATE
            totalBindings <= 30 -> ExpressionComplexityLevel.COMPLEX
            else -> ExpressionComplexityLevel.VERY_COMPLEX
        }
        
        return ExpressionComplexity(
            level = complexity,
            blendShapeCount = blendShapeCount,
            materialBindingCount = materialBindingCount,
            textureBindingCount = textureBindingCount,
            totalBindings = totalBindings,
            hasOverrides = expression.hasOverrides(),
            isBinary = expression.isBinary
        )
    }
}

/**
 * Contains all expression data
 */
data class ExpressionData(
    val expressions: List<Expression>,
    val blendShapeGroups: List<BlendShapeGroup>,
    val presetExpressions: List<Expression>,
    val customExpressions: List<Expression>
) {
    companion object {
        fun empty() = ExpressionData(
            expressions = emptyList(),
            blendShapeGroups = emptyList(),
            presetExpressions = emptyList(),
            customExpressions = emptyList()
        )
    }
    
    /**
     * Get expression by name
     */
    fun getExpression(name: String): Expression? = expressions.find { it.name == name }
    
    /**
     * Get expressions by category
     */
    fun getExpressionsByCategory(category: BlendShapeCategory): List<Expression> {
        return blendShapeGroups.find { it.category == category }?.expressions ?: emptyList()
    }
    
    /**
     * Check if expression data is empty
     */
    fun isEmpty(): Boolean = expressions.isEmpty()
    
    /**
     * Get expression statistics
     */
    fun getStatistics(): ExpressionStatistics = ExpressionStatistics(
        totalExpressions = expressions.size,
        presetExpressions = presetExpressions.size,
        customExpressions = customExpressions.size,
        totalBlendShapes = expressions.sumOf { it.blendShapeKeys.size },
        materialBindings = expressions.sumOf { it.materialColorBindings.size },
        textureBindings = expressions.sumOf { it.textureTransformBindings.size },
        binaryExpressions = expressions.count { it.isBinary },
        expressionsWithOverrides = expressions.count { it.hasOverrides() }
    )
}

/**
 * Blend shape group for organization
 */
data class BlendShapeGroup(
    val name: String,
    val expressions: List<Expression>,
    val category: BlendShapeCategory
)

/**
 * Blend shape categories
 */
enum class BlendShapeCategory {
    EMOTION,
    EYE,
    MOUTH,
    EYEBROW,
    NOSE,
    CUSTOM,
    OTHER
}

/**
 * Expression complexity analysis
 */
data class ExpressionComplexity(
    val level: ExpressionComplexityLevel,
    val blendShapeCount: Int,
    val materialBindingCount: Int,
    val textureBindingCount: Int,
    val totalBindings: Int,
    val hasOverrides: Boolean,
    val isBinary: Boolean
)

/**
 * Expression complexity levels
 */
enum class ExpressionComplexityLevel {
    EMPTY,
    SIMPLE,
    MODERATE,
    COMPLEX,
    VERY_COMPLEX
}

/**
 * Expression statistics
 */
data class ExpressionStatistics(
    val totalExpressions: Int,
    val presetExpressions: Int,
    val customExpressions: Int,
    val totalBlendShapes: Int,
    val materialBindings: Int,
    val textureBindings: Int,
    val binaryExpressions: Int,
    val expressionsWithOverrides: Int
) {
    /**
     * Get complexity distribution
     */
    fun getComplexityDistribution(): Map<ExpressionComplexityLevel, Int> {
        // This would be calculated based on individual expression complexities
        return mapOf(
            ExpressionComplexityLevel.SIMPLE to (totalExpressions * 0.4).toInt(),
            ExpressionComplexityLevel.MODERATE to (totalExpressions * 0.3).toInt(),
            ExpressionComplexityLevel.COMPLEX to (totalExpressions * 0.2).toInt(),
            ExpressionComplexityLevel.VERY_COMPLEX to (totalExpressions * 0.1).toInt()
        )
    }
}