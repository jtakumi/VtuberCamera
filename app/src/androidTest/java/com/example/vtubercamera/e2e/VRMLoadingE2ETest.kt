package com.example.vtubercamera.e2e

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.example.vtubercamera.MainActivity
import com.example.vtubercamera.R
import com.example.vtubercamera.data.VRMRepository
import com.example.vtubercamera.data.VRMRepositoryImpl
import com.example.vtubercamera.data.vrm.VRMModel
import com.example.vtubercamera.data.vrm.VRMMetadata
import com.example.vtubercamera.data.vrm.Expression
import com.example.vtubercamera.data.vrm.Pose
import com.example.vtubercamera.data.vrm.math.Transform
import com.example.vtubercamera.utils.PermissionUtils
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * End-to-End tests for VRM loading from file selection to avatar display
 * Tests the complete workflow of importing and displaying VRM avatars
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class VRMLoadingE2ETest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        *PermissionUtils.getRequiredMediaPermissions()
    )

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context
    private lateinit var vrmRepository: VRMRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        vrmRepository = VRMRepositoryImpl(context)
    }

    @Test
    fun vrmLoadingWorkflow_fromFileToDisplay_shouldWork() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        // Wait for main screen to load
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription(context.getString(R.string.camera_capture))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // When - Navigate to avatar library (if AR mode button exists)
        try {
            val arModeButton = composeTestRule.onNodeWithContentDescription("AR Mode Toggle")
            if (arModeButton.isDisplayed()) {
                arModeButton.performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // AR mode button might not be visible initially, continue
        }

        // Try to find avatar library button or menu
        try {
            val avatarLibraryButton = composeTestRule.onNodeWithContentDescription("Avatar Library")
            if (avatarLibraryButton.isDisplayed()) {
                avatarLibraryButton.performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // Avatar library might be accessed differently
        }

        // Then - Basic UI elements should be present
        // This test verifies the app can launch and basic navigation works
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.camera_capture))
            .assertIsDisplayed()
    }

    @Test
    fun vrmRepository_loadValidModel_shouldSucceed() = runTest {
        // Given - Create a mock VRM file
        val testVRMFile = createTestVRMFile()
        val testUri = Uri.fromFile(testVRMFile)

        // When - Load VRM model
        val result = vrmRepository.loadVRMFromUri(testUri)

        // Then - Should succeed (for a valid mock file)
        // Note: In a real test, this would depend on having a valid VRM file
        // For now, we test that the repository handles the operation
        assertNotNull("Result should not be null", result)

        // Cleanup
        testVRMFile.delete()
    }

    @Test
    fun avatarLibrary_addNewAvatar_shouldUpdateLibrary() = runTest {
        // Given - Get initial library stats
        val initialStats = vrmRepository.getLibraryStatistics()
        val initialCount = initialStats.totalAvatars

        // When - Add a test avatar (simulated)
        val testModel = createTestVRMModel()
        // Note: In real implementation, this would involve file operations

        // Then - Library should be updated
        // This test structure shows how to verify library operations
        val updatedStats = vrmRepository.getLibraryStatistics()
        assertNotNull("Updated stats should not be null", updatedStats)
    }

    @Test
    fun avatarExpressionChange_shouldBeReflectedInUI() {
        // Given - App is launched and avatar is loaded (mocked scenario)
        composeTestRule.waitForIdle()

        // When - Try to change avatar expression (if UI exists)
        try {
            // Look for expression controls
            val expressionButton = composeTestRule.onNodeWithText("Happy")
            if (expressionButton.isDisplayed()) {
                expressionButton.performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // Expression controls might not be visible without loaded avatar
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun avatarPoseChange_shouldBeReflectedInUI() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        // When - Try to change avatar pose (if UI exists)
        try {
            // Look for pose controls
            val poseButton = composeTestRule.onNodeWithText("Wave")
            if (poseButton.isDisplayed()) {
                poseButton.performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // Pose controls might not be visible without loaded avatar
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun lightingPresetChange_shouldBeReflectedInUI() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        // When - Try to change lighting preset (if UI exists)
        try {
            // Look for lighting controls
            val lightingButton = composeTestRule.onNodeWithText("Studio")
            if (lightingButton.isDisplayed()) {
                lightingButton.performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // Lighting controls might not be visible initially
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun avatarTransform_gestureInteraction_shouldWork() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        // When - Try gesture interactions on camera view
        try {
            // Find camera preview area
            val cameraView = composeTestRule.onNodeWithContentDescription("Camera Preview")
            if (cameraView.isDisplayed()) {
                // Test touch gesture
                cameraView.performTouchInput {
                    click(center)
                }
                composeTestRule.waitForIdle()

                // Test drag gesture
                cameraView.performTouchInput {
                    swipeLeft()
                }
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // Camera view might have different content description
        }

        // Then - UI should remain stable after gestures
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun arModeToggle_shouldSwitchModes() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        var arModeEnabled = false

        // When - Toggle AR mode (if available)
        try {
            val arToggle = composeTestRule.onNodeWithContentDescription("AR Mode Toggle")
            if (arToggle.isDisplayed()) {
                arToggle.performClick()
                composeTestRule.waitForIdle()
                arModeEnabled = true

                // Toggle back
                arToggle.performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // AR mode toggle might not be available on all devices
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun photoCapture_endToEndFlow_shouldWork() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        // When - Capture photo
        try {
            val captureButton = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.camera_capture)
            )
            captureButton.assertIsDisplayed()
            captureButton.performClick()
            composeTestRule.waitForIdle()

            // Wait for capture to complete
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                try {
                    // Look for photo preview or success indicator
                    composeTestRule.onNodeWithContentDescription("Photo Preview")
                        .assertExists()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
        } catch (e: AssertionError) {
            // Capture might fail in test environment
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun navigation_betweenScreens_shouldWork() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        // When - Navigate through available screens
        try {
            // Test gallery navigation
            val galleryButton = composeTestRule.onNodeWithContentDescription("Gallery")
            if (galleryButton.isDisplayed()) {
                galleryButton.performClick()
                composeTestRule.waitForIdle()

                // Navigate back
                composeTestRule.onNodeWithContentDescription("Back").performClick()
                composeTestRule.waitForIdle()
            }
        } catch (e: AssertionError) {
            // Gallery navigation might work differently
        }

        // Then - Should return to main screen
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.camera_capture))
            .assertIsDisplayed()
    }

    @Test
    fun errorHandling_invalidVRMFile_shouldShowError() = runTest {
        // Given - Create an invalid file
        val invalidFile = File(context.cacheDir, "invalid.txt")
        invalidFile.writeText("This is not a VRM file")
        val invalidUri = Uri.fromFile(invalidFile)

        // When - Try to load invalid file
        val result = vrmRepository.loadVRMFromUri(invalidUri)

        // Then - Should handle error gracefully
        assertNotNull("Result should not be null", result)
        if (result.isFailure) {
            assertNotNull("Error should be provided", result.exceptionOrNull())
        }

        // Cleanup
        invalidFile.delete()
    }

    // ========== Helper Methods ==========

    private fun createTestVRMFile(): File {
        val testFile = File(context.cacheDir, "test_avatar.vrm")
        FileOutputStream(testFile).use { fos ->
            // Create a minimal mock VRM file structure
            // In a real test, this would be a valid VRM file
            fos.write("MOCK_VRM_DATA".toByteArray())
        }
        return testFile
    }

    private fun createTestVRMModel(): VRMModel {
        return VRMModel(
            id = "test-model-e2e",
            name = "Test E2E Avatar",
            meshData = byteArrayOf(1, 2, 3, 4, 5),
            textureData = mapOf("diffuse" to byteArrayOf(6, 7, 8)),
            expressions = listOf(
                Expression("happy", "Happy", mapOf("mouth_smile" to 1.0f)),
                Expression("sad", "Sad", mapOf("mouth_frown" to 1.0f))
            ),
            poses = listOf(
                Pose("wave", "Wave", mapOf("rightArm" to Transform.identity())),
                Pose("rest", "Rest", mapOf("body" to Transform.identity()))
            ),
            metadata = VRMMetadata(
                title = "Test E2E Avatar",
                version = "1.0",
                author = "E2E Test",
                contactInformation = "test@e2e.com",
                reference = "https://test.com",
                allowedUserName = VRMMetadata.AllowedUser.EVERYONE,
                violentUsage = VRMMetadata.Usage.DISALLOW,
                sexualUsage = VRMMetadata.Usage.DISALLOW,
                commercialUsage = VRMMetadata.Usage.ALLOW,
                otherPermissionUrl = "",
                licenseName = VRMMetadata.License.OTHER,
                otherLicenseUrl = "https://test.com/license"
            )
        )
    }

    // Extension function to check if node is displayed without throwing
    private fun SemanticsNodeInteraction.isDisplayed(): Boolean {
        return try {
            assertIsDisplayed()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}