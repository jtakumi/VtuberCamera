package com.example.vtubercamera.data.vrm

/**
 * VRM expression (blend shape) data
 */
data class Expression(
    val name: String,
    val displayName: String = name,
    val blendShapeKeys: Map<String, Float> = emptyMap(),
    val materialColorBindings: Map<String, MaterialColorBinding> = emptyMap(),
    val textureTransformBindings: Map<String, TextureTransformBinding> = emptyMap(),
    val isBinary: Boolean = false,
    val overrideBlink: OverrideType = OverrideType.NONE,
    val overrideLookAt: OverrideType = OverrideType.NONE,
    val overrideMouth: OverrideType = OverrideType.NONE
) {
    /**
     * Override types for expressions
     */
    enum class OverrideType {
        NONE,
        BLOCK,
        BLEND
    }

    /**
     * Material color binding for expressions
     */
    data class MaterialColorBinding(
        val materialName: String,
        val propertyName: String,
        val targetValue: FloatArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MaterialColorBinding

            if (materialName != other.materialName) return false
            if (propertyName != other.propertyName) return false
            if (!targetValue.contentEquals(other.targetValue)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = materialName.hashCode()
            result = 31 * result + propertyName.hashCode()
            result = 31 * result + targetValue.contentHashCode()
            return result
        }
    }

    /**
     * Texture transform binding for expressions
     */
    data class TextureTransformBinding(
        val materialName: String,
        val propertyName: String,
        val scale: Pair<Float, Float> = Pair(1f, 1f),
        val offset: Pair<Float, Float> = Pair(0f, 0f)
    )

    /**
     * Check if this expression has blend shape data
     */
    fun hasBlendShapes(): Boolean = blendShapeKeys.isNotEmpty()

    /**
     * Check if this expression has material bindings
     */
    fun hasMaterialBindings(): Boolean = materialColorBindings.isNotEmpty()

    /**
     * Check if this expression has texture transform bindings
     */
    fun hasTextureTransformBindings(): Boolean = textureTransformBindings.isNotEmpty()

    /**
     * Get the weight for a specific blend shape
     */
    fun getBlendShapeWeight(shapeName: String): Float = blendShapeKeys[shapeName] ?: 0f

    /**
     * Check if this expression overrides any default behaviors
     */
    fun hasOverrides(): Boolean = 
        overrideBlink != OverrideType.NONE || 
        overrideLookAt != OverrideType.NONE || 
        overrideMouth != OverrideType.NONE

    /**
     * Create a copy with modified blend shape weights
     */
    fun withBlendShapeWeight(shapeName: String, weight: Float): Expression {
        val newBlendShapes = blendShapeKeys.toMutableMap()
        newBlendShapes[shapeName] = weight.coerceIn(0f, 1f)
        return copy(blendShapeKeys = newBlendShapes)
    }

    /**
     * Create a copy with all blend shape weights scaled by a factor
     */
    fun withScaledWeights(scaleFactor: Float): Expression {
        val scaledWeights = blendShapeKeys.mapValues { (_, weight) ->
            (weight * scaleFactor).coerceIn(0f, 1f)
        }
        return copy(blendShapeKeys = scaledWeights)
    }

    /**
     * Blend this expression with another expression
     */
    fun blendWith(other: Expression, weight: Float): Expression {
        val clampedWeight = weight.coerceIn(0f, 1f)
        val inverseWeight = 1f - clampedWeight
        
        val blendedShapes = mutableMapOf<String, Float>()
        
        // Blend blend shapes
        val allShapeNames = (blendShapeKeys.keys + other.blendShapeKeys.keys).distinct()
        for (shapeName in allShapeNames) {
            val thisWeight = blendShapeKeys[shapeName] ?: 0f
            val otherWeight = other.blendShapeKeys[shapeName] ?: 0f
            blendedShapes[shapeName] = (thisWeight * inverseWeight + otherWeight * clampedWeight).coerceIn(0f, 1f)
        }
        
        return copy(
            name = "${name}_blend_${other.name}",
            displayName = "$displayName + ${other.displayName}",
            blendShapeKeys = blendedShapes
        )
    }

    companion object {
        /**
         * Create a neutral expression (all weights at 0)
         */
        fun neutral(): Expression = Expression(
            name = "neutral",
            displayName = "Neutral"
        )

        /**
         * Common VRM expression presets
         */
        fun happy(): Expression = Expression(
            name = "happy",
            displayName = "Happy",
            blendShapeKeys = mapOf(
                "mouth_smile" to 1f,
                "eye_smile_left" to 0.6f,
                "eye_smile_right" to 0.6f
            )
        )

        fun angry(): Expression = Expression(
            name = "angry",
            displayName = "Angry",
            blendShapeKeys = mapOf(
                "brow_angry_left" to 1f,
                "brow_angry_right" to 1f,
                "mouth_frown" to 0.8f
            )
        )

        fun sad(): Expression = Expression(
            name = "sad",
            displayName = "Sad",
            blendShapeKeys = mapOf(
                "brow_sad_left" to 1f,
                "brow_sad_right" to 1f,
                "mouth_sad" to 1f
            )
        )

        fun surprised(): Expression = Expression(
            name = "surprised",
            displayName = "Surprised",
            blendShapeKeys = mapOf(
                "eye_wide_left" to 1f,
                "eye_wide_right" to 1f,
                "mouth_o" to 1f,
                "brow_up_left" to 0.8f,
                "brow_up_right" to 0.8f
            )
        )
    }
}