package com.example.vtubercamera.data.vrm

import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.data.vrm.math.Vector3
import com.example.vtubercamera.data.vrm.math.Quaternion
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for AvatarController
 */
class AvatarControllerTest {

    private lateinit var avatarController: AvatarController
    private lateinit var mockVRMModel: VRMModel

    @Before
    fun setUp() {
        avatarController = AvatarController()
        mockVRMModel = createTestVRMModel()
    }

    @Test
    fun `avatarController should initialize with default state`() = runTest {
        // When
        val state = avatarController.avatarState.first()

        // Then
        assertNull("Initial model should be null", state.model)
        assertEquals("Initial transform should be identity", Transform.identity(), state.transform)
        assertFalse("Initial visibility should be false", state.isVisible)
        assertFalse("Should not be loading initially", state.isLoading)
        assertEquals("Loading progress should be 0", 0.0f, state.loadingProgress, 0.001f)
    }

    @Test
    fun `loadModel should update avatar state with new model`() = runTest {
        // When
        avatarController.loadModel(mockVRMModel)

        // Then
        val state = avatarController.avatarState.first()
        assertEquals("Model should be set", mockVRMModel, state.model)
        assertFalse("Should not be loading after load", state.isLoading)
        assertEquals("Loading progress should be 1.0", 1.0f, state.loadingProgress, 0.001f)
    }

    @Test
    fun `setTransform should update avatar transform`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)
        val newTransform = Transform(
            position = Vector3(1.0f, 2.0f, 3.0f),
            rotation = Quaternion.identity(),
            scale = Vector3(1.5f, 1.5f, 1.5f)
        )

        // When
        avatarController.setTransform(newTransform)

        // Then
        val state = avatarController.avatarState.first()
        assertEquals("Transform should be updated", newTransform, state.transform)
    }

    @Test
    fun `setVisible should update avatar visibility`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)

        // When
        avatarController.setVisible(true)

        // Then
        val state = avatarController.avatarState.first()
        assertTrue("Avatar should be visible", state.isVisible)

        // When
        avatarController.setVisible(false)

        // Then
        val updatedState = avatarController.avatarState.first()
        assertFalse("Avatar should not be visible", updatedState.isVisible)
    }

    @Test
    fun `clearModel should reset avatar state`() = runTest {
        // Given - Load a model first
        avatarController.loadModel(mockVRMModel)
        avatarController.setVisible(true)

        // When
        avatarController.clearModel()

        // Then
        val state = avatarController.avatarState.first()
        assertNull("Model should be null after clear", state.model)
        assertFalse("Visibility should be false after clear", state.isVisible)
        assertEquals("Transform should be identity after clear", Transform.identity(), state.transform)
    }

    @Test
    fun `shouldRender should return true when model is loaded and visible`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)
        avatarController.setVisible(true)

        // When
        val state = avatarController.avatarState.first()

        // Then
        assertTrue("Should render when model loaded and visible", state.shouldRender)
    }

    @Test
    fun `shouldRender should return false when model is null`() = runTest {
        // Given
        avatarController.setVisible(true)

        // When
        val state = avatarController.avatarState.first()

        // Then
        assertFalse("Should not render when model is null", state.shouldRender)
    }

    @Test
    fun `shouldRender should return false when not visible`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)
        avatarController.setVisible(false)

        // When
        val state = avatarController.avatarState.first()

        // Then
        assertFalse("Should not render when not visible", state.shouldRender)
    }

    @Test
    fun `setLoading should update loading state`() = runTest {
        // Given
        val progress = 0.5f

        // When
        avatarController.setLoading(true, progress)

        // Then
        val state = avatarController.avatarState.first()
        assertTrue("Should be loading", state.isLoading)
        assertEquals("Loading progress should match", progress, state.loadingProgress, 0.001f)

        // When
        avatarController.setLoading(false, 1.0f)

        // Then
        val updatedState = avatarController.avatarState.first()
        assertFalse("Should not be loading", updatedState.isLoading)
        assertEquals("Loading progress should be 1.0", 1.0f, updatedState.loadingProgress, 0.001f)
    }

    @Test
    fun `resetTransform should reset to identity transform`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)
        val customTransform = Transform(
            position = Vector3(5.0f, 5.0f, 5.0f),
            rotation = Quaternion.identity(),
            scale = Vector3(2.0f, 2.0f, 2.0f)
        )
        avatarController.setTransform(customTransform)

        // When
        avatarController.resetTransform()

        // Then
        val state = avatarController.avatarState.first()
        assertEquals("Transform should be identity", Transform.identity(), state.transform)
    }

    @Test
    fun `updateExpression should update current expression`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)
        val expression = Expression("happy", "Happy", mapOf("mouth_smile" to 1.0f))

        // When
        avatarController.updateExpression(expression)

        // Then
        val state = avatarController.avatarState.first()
        assertEquals("Expression should be updated", expression, state.currentExpression)
    }

    @Test
    fun `updatePose should update current pose`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)
        val pose = Pose("wave", "Wave Pose", mapOf("rightArm" to Transform.identity()))

        // When
        avatarController.updatePose(pose)

        // Then
        val state = avatarController.avatarState.first()
        assertEquals("Pose should be updated", pose, state.currentPose)
    }

    @Test
    fun `getTransform should return current transform`() = runTest {
        // Given
        val customTransform = Transform(
            position = Vector3(1.0f, 2.0f, 3.0f),
            rotation = Quaternion.identity(),
            scale = Vector3(1.0f, 1.0f, 1.0f)
        )
        avatarController.loadModel(mockVRMModel)
        avatarController.setTransform(customTransform)

        // When
        val transform = avatarController.getTransform()

        // Then
        assertEquals("Transform should match", customTransform, transform)
    }

    @Test
    fun `isVisible should return current visibility state`() = runTest {
        // Given
        avatarController.loadModel(mockVRMModel)

        // Initially false
        assertFalse("Should initially be invisible", avatarController.isVisible())

        // When
        avatarController.setVisible(true)

        // Then
        assertTrue("Should be visible after setting", avatarController.isVisible())
    }

    @Test
    fun `isLoaded should return true when model is loaded`() = runTest {
        // Initially false
        assertFalse("Should initially not be loaded", avatarController.isLoaded())

        // When
        avatarController.loadModel(mockVRMModel)

        // Then
        assertTrue("Should be loaded after loading model", avatarController.isLoaded())
    }

    @Test
    fun `getCurrentModel should return current model`() = runTest {
        // Initially null
        assertNull("Should initially have no model", avatarController.getCurrentModel())

        // When
        avatarController.loadModel(mockVRMModel)

        // Then
        assertEquals("Should return loaded model", mockVRMModel, avatarController.getCurrentModel())
    }

    // ========== Helper Methods ==========

    private fun createTestVRMModel(): VRMModel {
        return VRMModel(
            id = "test-avatar",
            name = "Test Avatar",
            meshData = byteArrayOf(1, 2, 3, 4, 5),
            textureData = mapOf("diffuse" to byteArrayOf(6, 7, 8, 9, 10)),
            expressions = listOf(
                Expression("happy", "Happy", mapOf("mouth_smile" to 1.0f)),
                Expression("sad", "Sad", mapOf("mouth_frown" to 1.0f))
            ),
            poses = listOf(
                Pose("wave", "Wave", mapOf("rightArm" to Transform.identity())),
                Pose("peace", "Peace Sign", mapOf("rightHand" to Transform.identity()))
            ),
            metadata = VRMMetadata(
                title = "Test Avatar",
                version = "1.0",
                author = "Test Author",
                contactInformation = "",
                reference = "",
                allowedUserName = VRMMetadata.AllowedUser.EVERYONE,
                violentUsage = VRMMetadata.Usage.DISALLOW,
                sexualUsage = VRMMetadata.Usage.DISALLOW,
                commercialUsage = VRMMetadata.Usage.ALLOW,
                otherPermissionUrl = "",
                licenseName = VRMMetadata.License.OTHER,
                otherLicenseUrl = ""
            )
        )
    }
}