package com.example.vtubercamera.data

import com.example.vtubercamera.data.vrm.ARCameraState
import com.example.vtubercamera.data.vrm.ARSessionState
import com.example.vtubercamera.data.vrm.AvatarState
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.LightingPreset
import com.example.vtubercamera.data.vrm.LightingSettings
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.VRMMetadata
import com.example.vtubercamera.data.vrm.math.Quaternion
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.ui.viewmodels.ARPhotoStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for data models and utility classes
 */
class DataModelsTest {

    // ========== Math Library Tests ==========

    @Test
    fun `Vector3 should be created with correct values`() {
        // Given
        val vector = Vector3(1.0f, 2.0f, 3.0f)

        // Then
        assertEquals("X should match", 1.0f, vector.x, 0.001f)
        assertEquals("Y should match", 2.0f, vector.y, 0.001f)
        assertEquals("Z should match", 3.0f, vector.z, 0.001f)
    }

    @Test
    fun `Vector3 zero should return zero vector`() {
        // When
        val vector = Vector3.ZERO

        // Then
        assertEquals("X should be zero", 0.0f, vector.x, 0.001f)
        assertEquals("Y should be zero", 0.0f, vector.y, 0.001f)
        assertEquals("Z should be zero", 0.0f, vector.z, 0.001f)
    }

    @Test
    fun `Vector3 one should return unit vector`() {
        // When
        val vector = Vector3.ONE

        // Then
        assertEquals("X should be one", 1.0f, vector.x, 0.001f)
        assertEquals("Y should be one", 1.0f, vector.y, 0.001f)
        assertEquals("Z should be one", 1.0f, vector.z, 0.001f)
    }

    @Test
    fun `Vector3 add should return correct sum`() {
        // Given
        val vector1 = Vector3(1.0f, 2.0f, 3.0f)
        val vector2 = Vector3(4.0f, 5.0f, 6.0f)

        // When
        val result = vector1 + vector2

        // Then
        assertEquals("X should be sum", 5.0f, result.x, 0.001f)
        assertEquals("Y should be sum", 7.0f, result.y, 0.001f)
        assertEquals("Z should be sum", 9.0f, result.z, 0.001f)
    }

    @Test
    fun `Vector3 subtract should return correct difference`() {
        // Given
        val vector1 = Vector3(5.0f, 7.0f, 9.0f)
        val vector2 = Vector3(1.0f, 2.0f, 3.0f)

        // When
        val result = vector1 - vector2

        // Then
        assertEquals("X should be difference", 4.0f, result.x, 0.001f)
        assertEquals("Y should be difference", 5.0f, result.y, 0.001f)
        assertEquals("Z should be difference", 6.0f, result.z, 0.001f)
    }

    @Test
    fun `Vector3 multiply should return correct product`() {
        // Given
        val vector = Vector3(2.0f, 3.0f, 4.0f)
        val scalar = 2.0f

        // When
        val result = vector * scalar

        // Then
        assertEquals("X should be multiplied", 4.0f, result.x, 0.001f)
        assertEquals("Y should be multiplied", 6.0f, result.y, 0.001f)
        assertEquals("Z should be multiplied", 8.0f, result.z, 0.001f)
    }

    @Test
    fun `Vector3 magnitude should return correct length`() {
        // Given
        val vector = Vector3(3.0f, 4.0f, 0.0f)

        // When
        val magnitude = vector.magnitude()

        // Then
        assertEquals("Magnitude should be 5", 5.0f, magnitude, 0.001f)
    }

    @Test
    fun `Vector3 normalized should return unit vector`() {
        // Given
        val vector = Vector3(3.0f, 4.0f, 0.0f)

        // When
        val normalized = vector.normalized()

        // Then
        assertEquals("Normalized X should be correct", 0.6f, normalized.x, 0.001f)
        assertEquals("Normalized Y should be correct", 0.8f, normalized.y, 0.001f)
        assertEquals("Normalized Z should be correct", 0.0f, normalized.z, 0.001f)
        assertEquals("Normalized magnitude should be 1", 1.0f, normalized.magnitude(), 0.001f)
    }

    @Test
    fun `Quaternion identity should return identity quaternion`() {
        // When
        val identity = Quaternion.IDENTITY

        // Then
        assertEquals("X should be zero", 0.0f, identity.x, 0.001f)
        assertEquals("Y should be zero", 0.0f, identity.y, 0.001f)
        assertEquals("Z should be zero", 0.0f, identity.z, 0.001f)
        assertEquals("W should be one", 1.0f, identity.w, 0.001f)
    }

    @Test
    fun `Transform identity should return identity transform`() {
        // When
        val identity = Transform.identity()

        // Then
        assertEquals("Position should be zero", Vector3.ZERO, identity.position)
        assertEquals("Rotation should be identity", Quaternion.IDENTITY, identity.rotation)
        assertEquals("Scale should be one", Vector3.ONE, identity.scale)
    }

    // ========== VRM Metadata Tests ==========

    @Test
    fun `VRMMetadata should be created with all properties`() {
        // Given
        val metadata = VRMMetadata(
            title = "Test Avatar",
            version = "1.0",
            author = "Test Author",
            contactInformation = "test@example.com",
            reference = "https://example.com",
            allowedUserName = VRMMetadata.AllowedUser.EVERYONE,
            violentUsage = VRMMetadata.Usage.DISALLOW,
            sexualUsage = VRMMetadata.Usage.DISALLOW,
            commercialUsage = VRMMetadata.Usage.ALLOW,
            otherPermissionUrl = "",
            licenseName = VRMMetadata.LicenseType.REDISTRIBUTION_PROHIBITED,
            otherLicenseUrl = "https://example.com/license"
        )

        // Then
        assertEquals("Title should match", "Test Avatar", metadata.title)
        assertEquals("Version should match", "1.0", metadata.version)
        assertEquals("Author should match", "Test Author", metadata.author)
        assertEquals("Contact should match", "test@example.com", metadata.contactInformation)
        assertEquals("Reference should match", "https://example.com", metadata.reference)
        assertEquals(
            "Allowed user should match",
            VRMMetadata.AllowedUser.EVERYONE,
            metadata.allowedUserName
        )
        assertEquals(
            "Violent usage should match",
            VRMMetadata.Usage.DISALLOW,
            metadata.violentUsage
        )
        assertEquals("Sexual usage should match", VRMMetadata.Usage.DISALLOW, metadata.sexualUsage)
        assertEquals(
            "Commercial usage should match",
            VRMMetadata.Usage.ALLOW,
            metadata.commercialUsage
        )
        assertEquals(
            "License name should match",
            VRMMetadata.LicenseType.REDISTRIBUTION_PROHIBITED,
            metadata.licenseName
        )
        assertEquals(
            "License URL should match",
            "https://example.com/license",
            metadata.otherLicenseUrl
        )
    }

    // ========== Expression Tests ==========

    @Test
    fun `Expression should be created with blend shapes`() {
        // Given
        val blendShapes = mapOf(
            "mouth_smile" to 1.0f,
            "eye_blink_left" to 0.5f,
            "eye_blink_right" to 0.5f
        )
        val expression = Expression("happy", "Happy Expression", blendShapes)

        // Then
        assertEquals("ID should match", "happy", expression.name)
        assertEquals("Name should match", "Happy Expression", expression.name)
        assertEquals("Blend shapes should match", blendShapes, expression.blendShapeKeys)
        assertEquals("Should have 3 blend shapes", 3, expression.blendShapeKeys.size)
    }

    @Test
    fun `Expression getBlendShapeWeight should return correct weight`() {
        // Given
        val blendShapes = mapOf("mouth_smile" to 0.8f)
        val expression = Expression("happy", "Happy", blendShapes)

        // When
        val weight = expression.getBlendShapeWeight("mouth_smile")

        // Then
        assertEquals("Weight should match", 0.8f, weight, 0.001f)
    }

    @Test
    fun `Expression getBlendShapeWeight should return zero for missing shape`() {
        // Given
        val expression = Expression("happy", "Happy", emptyMap())

        // When
        val weight = expression.getBlendShapeWeight("nonexistent_shape")

        // Then
        assertEquals("Weight should be zero", 0.0f, weight, 0.001f)
    }

    // ========== Pose Tests ==========

    @Test
    fun `Pose should be created with bone transforms`() {
        // Given
        val boneTransforms = mapOf(
            "rightArm" to Transform.identity(),
            "leftArm" to Transform(
                position = Vector3(1.0f, 0.0f, 0.0f),
                rotation = Quaternion.IDENTITY,
                scale = Vector3.ONE
            )
        )
        val pose = Pose("wave", "Wave Pose", boneTransforms)

        // Then
        assertEquals("ID should match", "wave", pose.name)
        assertEquals("Name should match", "Wave Pose", pose.name)
        assertEquals("Bone transforms should match", boneTransforms, pose.boneTransforms)
        assertEquals("Should have 2 bone transforms", 2, pose.boneTransforms.size)
    }

    @Test
    fun `Pose getBoneTransform should return correct transform`() {
        // Given
        val transform = Transform(
            position = Vector3(1.0f, 2.0f, 3.0f),
            rotation = Quaternion.IDENTITY,
            scale = Vector3.ONE
        )
        val pose = Pose("test", "Test", mapOf("testBone" to transform))

        // When
        val result = pose.getBoneTransform("testBone")

        // Then
        assertEquals("Transform should match", transform, result)
    }

    @Test
    fun `Pose getBoneTransform should return identity for missing bone`() {
        // Given
        val pose = Pose("test", "Test", emptyMap())

        // When
        val result = pose.getBoneTransform("nonexistent_bone")

        // Then
        assertEquals("Transform should be identity", Transform.identity(), result)
    }

    // ========== AvatarState Tests ==========

    @Test
    fun `AvatarState should have default values`() {
        // When
        val state = AvatarState()

        // Then
        assertNull("Model should be null", state.model)
        assertEquals("Transform should be identity", Transform.identity(), state.transform)
        assertNull("Expression should be null", state.currentExpression)
        assertNull("Pose should be null", state.currentPose)
        assertFalse("Should not be visible", state.isVisible)
        assertFalse("Should not be loading", state.isLoading)
        assertEquals("Loading progress should be 0", 0.0f, state.loadingProgress, 0.001f)
    }

    @Test
    fun `AvatarState shouldRender should return true when visible and model exists`() {
        // Given
        val mockModel = createTestVRMModel()
        val state = AvatarState(
            model = mockModel,
            isVisible = true
        )

        // Then
        assertTrue("Should render when visible and model exists", state.shouldRender)
    }

    @Test
    fun `AvatarState shouldRender should return false when not visible`() {
        // Given
        val mockModel = createTestVRMModel()
        val state = AvatarState(
            model = mockModel,
            isVisible = false
        )

        // Then
        assertFalse("Should not render when not visible", state.shouldRender)
    }

    @Test
    fun `AvatarState shouldRender should return false when model is null`() {
        // Given
        val state = AvatarState(
            model = null,
            isVisible = true
        )

        // Then
        assertFalse("Should not render when model is null", state.shouldRender)
    }

    // ========== AR Session State Tests ==========

    @Test
    fun `ARSessionState should have default values`() {
        // When
        val state = ARSessionState()

        // Then
        assertFalse("Should not be initialized", state.isInitialized)
        assertFalse("Should not be tracking", state.isInitialized)
        assertNull("Error should be null", state.lightEstimate)
    }

    @Test
    fun `ARCameraState default should return default state`() {
        // When
        val state = ARCameraState.default()

        // Then
        assertNotNull("State should not be null", state)
        // Additional assertions would depend on ARCameraState implementation
    }

    // ========== Lighting Settings Tests ==========

    @Test
    fun `LightingSettings should be created with correct values`() {
        // Given
        val settings = LightingSettings(
            brightness = 0.8f,
            colorTemperature = 5500f,
            shadowStrength = 0.6f,
            ambientIntensity = 0.3f
        )

        // Then
        assertEquals("Intensity should match", 0.8f, settings.brightness, 0.001f)
        assertEquals("Color temperature should match", 5500f, settings.colorTemperature, 0.1f)
        assertEquals("Shadow strength should match", 0.6f, settings.shadowStrength, 0.001f)
        assertEquals("Ambient intensity should match", 0.3f, settings.ambientIntensity, 0.001f)
    }

    @Test
    fun `LightingPreset should be created with settings`() {
        // Given
        val settings = LightingSettings(
            brightness = 1.0f,
            colorTemperature = 6500f,
            shadowStrength = 0.8f,
            ambientIntensity = 0.4f
        )
        val preset = LightingPreset("Studio", settings)

        // Then
        assertEquals("Name should match", "Studio", preset.name)
        assertEquals("Settings should match", settings, preset.settings)
    }

    // ========== ARPhotoStats Tests ==========

    @Test
    fun `ARPhotoStats should be created with correct values`() {
        // Given
        val avatarNames = listOf("Avatar1", "Avatar2")
        val poseNames = listOf("Wave", "Peace")
        val expressionNames = listOf("Happy", "Sad")

        val stats = ARPhotoStats(
            totalARPhotos = 10,
            uniqueAvatars = 2,
            uniquePoses = 2,
            uniqueExpressions = 2,
            avatarNames = avatarNames,
            poseNames = poseNames,
            expressionNames = expressionNames
        )

        // Then
        assertEquals("Total AR photos should match", 10, stats.totalARPhotos)
        assertEquals("Unique avatars should match", 2, stats.uniqueAvatars)
        assertEquals("Unique poses should match", 2, stats.uniquePoses)
        assertEquals("Unique expressions should match", 2, stats.uniqueExpressions)
        assertEquals("Avatar names should match", avatarNames, stats.avatarNames)
        assertEquals("Pose names should match", poseNames, stats.poseNames)
        assertEquals("Expression names should match", expressionNames, stats.expressionNames)
    }

    // ========== Helper Methods ==========

    private fun createTestVRMModel(): com.example.vtubercamera.data.vrm.VRMModel {
        return com.example.vtubercamera.data.vrm.VRMModel(
            id = "test-model",
            name = "Test Model",
            meshData = byteArrayOf(),
            textureData = emptyMap(),
            expressions = emptyList(),
            poses = emptyList(),
            metadata = VRMMetadata(
                title = "Test",
                version = "1.0",
                author = "Test Author",
                contactInformation = "",
                reference = "",
                allowedUserName = VRMMetadata.AllowedUser.EVERYONE,
                violentUsage = VRMMetadata.Usage.DISALLOW,
                sexualUsage = VRMMetadata.Usage.DISALLOW,
                commercialUsage = VRMMetadata.Usage.ALLOW,
                otherPermissionUrl = "",
                licenseName = VRMMetadata.LicenseType.OTHER,
                otherLicenseUrl = ""
            )
        )
    }
}