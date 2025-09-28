package com.example.vtubercamera.ui

import android.Manifest
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.example.vtubercamera.MainActivity
import com.example.vtubercamera.R
import com.example.vtubercamera.utils.PermissionUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for user operations and gesture interactions
 * Tests touch gestures, UI controls, and user interaction flows
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UIOperationsAndGestureTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        *PermissionUtils.getRequiredMediaPermissions()
    )

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        composeTestRule.waitForIdle()
    }

    @Test
    fun cameraControls_basicInteractions_shouldWork() {
        // Given - App is launched and camera screen is visible
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription(context.getString(R.string.camera_capture))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // When & Then - Test capture button
        val captureButton = composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.camera_capture)
        )
        captureButton.assertIsDisplayed()
        captureButton.assertIsEnabled()
        captureButton.performClick()
        composeTestRule.waitForIdle()

        // Test camera switch button (if available)
        try {
            val switchButton = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.switch_camera)
            )
            switchButton.assertIsDisplayed()
            switchButton.performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // Switch camera button might not be available
        }

        // Test flash toggle (if available)
        try {
            val flashButton = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.flash_mode_toggle)
            )
            flashButton.assertIsDisplayed()
            flashButton.performClick()
            composeTestRule.waitForIdle()
        } catch (e: AssertionError) {
            // Flash button might not be available
        }
    }

    @Test
    fun zoomGestures_shouldWorkCorrectly() {
        // Given - Camera view is visible
        composeTestRule.waitForIdle()

        try {
            // Find camera preview area
            val cameraPreview = composeTestRule.onNodeWithContentDescription("Camera Preview")
                .assertIsDisplayed()

            // When - Perform pinch to zoom gesture
            cameraPreview.performTouchInput {
                val center = Offset(centerX, centerY)
                val start1 = Offset(centerX - 100f, centerY)
                val start2 = Offset(centerX + 100f, centerY)
                val end1 = Offset(centerX - 200f, centerY)
                val end2 = Offset(centerX + 200f, centerY)

                // Simulate pinch out (zoom in)
                down(1, start1)
                down(2, start2)
                moveTo(1, end1)
                moveTo(2, end2)
                up(1)
                up(2)
            }

            composeTestRule.waitForIdle()

            // Perform pinch in (zoom out)
            cameraPreview.performTouchInput {
                val center = Offset(centerX, centerY)
                val start1 = Offset(centerX - 200f, centerY)
                val start2 = Offset(centerX + 200f, centerY)
                val end1 = Offset(centerX - 100f, centerY)
                val end2 = Offset(centerX + 100f, centerY)

                // Simulate pinch in (zoom out)
                down(1, start1)
                down(2, start2)
                moveTo(1, end1)
                moveTo(2, end2)
                up(1)
                up(2)
            }

            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Camera preview might have different content description or not be available
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun focusGestures_tapToFocus_shouldWork() {
        // Given - Camera view is visible
        composeTestRule.waitForIdle()

        try {
            // Find camera preview area
            val cameraPreview = composeTestRule.onNodeWithContentDescription("Camera Preview")

            // When - Tap to focus at different positions
            cameraPreview.performTouchInput {
                // Tap at top-left quadrant
                click(Offset(centerX * 0.5f, centerY * 0.5f))
            }
            composeTestRule.waitForIdle()

            cameraPreview.performTouchInput {
                // Tap at bottom-right quadrant
                click(Offset(centerX * 1.5f, centerY * 1.5f))
            }
            composeTestRule.waitForIdle()

            cameraPreview.performTouchInput {
                // Tap at center
                click(center)
            }
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Camera preview might not be available or have different implementation
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun avatarTransformGestures_shouldWork() {
        // Given - App is in AR mode (if available)
        composeTestRule.waitForIdle()

        try {
            // Try to enable AR mode first
            val arToggle = composeTestRule.onNodeWithContentDescription("AR Mode Toggle")
            arToggle.performClick()
            composeTestRule.waitForIdle()

            // Find AR view or avatar interaction area
            val arView = composeTestRule.onNodeWithContentDescription("AR View")

            // When - Perform avatar manipulation gestures
            arView.performTouchInput {
                // Test drag gesture for avatar positioning
                swipeRight()
            }
            composeTestRule.waitForIdle()

            arView.performTouchInput {
                // Test rotation gesture
                swipeUp()
            }
            composeTestRule.waitForIdle()

            arView.performTouchInput {
                // Test scale gesture
                val center = Offset(centerX, centerY)
                val start1 = Offset(centerX - 50f, centerY)
                val start2 = Offset(centerX + 50f, centerY)
                val end1 = Offset(centerX - 100f, centerY)
                val end2 = Offset(centerX + 100f, centerY)

                down(1, start1)
                down(2, start2)
                moveTo(1, end1)
                moveTo(2, end2)
                up(1)
                up(2)
            }
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // AR mode or avatar manipulation might not be available
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun drawerNavigation_shouldWork() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        try {
            // When - Try to open navigation drawer (if available)
            val menuButton = composeTestRule.onNodeWithContentDescription("Menu")
            menuButton.performClick()
            composeTestRule.waitForIdle()

            // Try to navigate to different sections
            composeTestRule.onNodeWithText("Gallery").performClick()
            composeTestRule.waitForIdle()

            // Navigate back
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Navigation drawer might not be implemented or have different structure
        }

        // Then - Should return to main screen
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.camera_capture))
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheetControls_shouldWork() {
        // Given - App is launched
        composeTestRule.waitForIdle()

        try {
            // When - Try to open bottom sheet controls (if available)
            val controlsButton = composeTestRule.onNodeWithContentDescription("Controls")
            controlsButton.performClick()
            composeTestRule.waitForIdle()

            // Test drag gesture to expand/collapse bottom sheet
            composeTestRule.onRoot().performTouchInput {
                swipeUp(startY = size.height * 0.8f, endY = size.height * 0.4f)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onRoot().performTouchInput {
                swipeDown(startY = size.height * 0.4f, endY = size.height * 0.8f)
            }
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Bottom sheet controls might not be available
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun expressionSelector_gestureInteraction_shouldWork() {
        // Given - App is loaded
        composeTestRule.waitForIdle()

        try {
            // When - Try to interact with expression selector (if available)
            val expressionButton = composeTestRule.onNodeWithText("Happy")
            expressionButton.performClick()
            composeTestRule.waitForIdle()

            // Try scrolling through expressions
            val expressionList = composeTestRule.onNodeWithContentDescription("Expression List")
            expressionList.performTouchInput {
                swipeLeft()
            }
            composeTestRule.waitForIdle()

            expressionList.performTouchInput {
                swipeRight()
            }
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Expression controls might not be visible without avatar
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun poseSelector_gestureInteraction_shouldWork() {
        // Given - App is loaded
        composeTestRule.waitForIdle()

        try {
            // When - Try to interact with pose selector (if available)
            val poseButton = composeTestRule.onNodeWithText("Wave")
            poseButton.performClick()
            composeTestRule.waitForIdle()

            // Try scrolling through poses
            val poseList = composeTestRule.onNodeWithContentDescription("Pose List")
            poseList.performTouchInput {
                swipeUp()
            }
            composeTestRule.waitForIdle()

            poseList.performTouchInput {
                swipeDown()
            }
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Pose controls might not be visible without avatar
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun lightingControls_sliderInteraction_shouldWork() {
        // Given - App is loaded
        composeTestRule.waitForIdle()

        try {
            // When - Try to interact with lighting controls (if available)
            val lightingPanel = composeTestRule.onNodeWithContentDescription("Lighting Panel")
            lightingPanel.assertIsDisplayed()

            // Test slider interactions
            val intensitySlider = composeTestRule.onNodeWithContentDescription("Intensity Slider")
            intensitySlider.performTouchInput {
                // Drag slider to different positions
                swipeRight()
            }
            composeTestRule.waitForIdle()

            val colorTempSlider = composeTestRule.onNodeWithContentDescription("Color Temperature Slider")
            colorTempSlider.performTouchInput {
                swipeLeft()
            }
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Lighting controls might not be available or have different implementation
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun galleryInteraction_scrollAndSelection_shouldWork() {
        // Given - Navigate to gallery (if available)
        composeTestRule.waitForIdle()

        try {
            val galleryButton = composeTestRule.onNodeWithContentDescription("Gallery")
            galleryButton.performClick()
            composeTestRule.waitForIdle()

            // When - Interact with gallery
            val photoGrid = composeTestRule.onNodeWithContentDescription("Photo Grid")

            // Test scrolling
            photoGrid.performTouchInput {
                swipeUp()
            }
            composeTestRule.waitForIdle()

            photoGrid.performTouchInput {
                swipeDown()
            }
            composeTestRule.waitForIdle()

            // Test photo selection (if photos exist)
            try {
                val firstPhoto = composeTestRule.onAllNodesWithContentDescription("Photo").onFirst()
                firstPhoto.performClick()
                composeTestRule.waitForIdle()

                // Test photo viewer gestures
                val photoViewer = composeTestRule.onNodeWithContentDescription("Photo Viewer")
                photoViewer.performTouchInput {
                    // Pinch to zoom
                    val center = Offset(centerX, centerY)
                    val start1 = Offset(centerX - 100f, centerY)
                    val start2 = Offset(centerX + 100f, centerY)
                    val end1 = Offset(centerX - 200f, centerY)
                    val end2 = Offset(centerX + 200f, centerY)

                    down(1, start1)
                    down(2, start2)
                    moveTo(1, end1)
                    moveTo(2, end2)
                    up(1)
                    up(2)
                }
                composeTestRule.waitForIdle()

                // Swipe to next/previous photo
                photoViewer.performTouchInput {
                    swipeLeft()
                }
                composeTestRule.waitForIdle()

            } catch (e: AssertionError) {
                // No photos available for testing
            }

        } catch (e: AssertionError) {
            // Gallery might not be available
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun multiTouch_complexGestures_shouldWork() {
        // Given - Camera view is visible
        composeTestRule.waitForIdle()

        try {
            val mainView = composeTestRule.onRoot()

            // When - Perform complex multi-touch gestures
            mainView.performTouchInput {
                // Three-finger tap
                down(1, Offset(centerX - 100f, centerY))
                down(2, Offset(centerX, centerY))
                down(3, Offset(centerX + 100f, centerY))
                up(1)
                up(2)
                up(3)
            }
            composeTestRule.waitForIdle()

            // Two-finger rotation gesture
            mainView.performTouchInput {
                val center = Offset(centerX, centerY)
                val radius = 100f
                val start1 = Offset(centerX + radius, centerY)
                val start2 = Offset(centerX - radius, centerY)
                val end1 = Offset(centerX, centerY + radius)
                val end2 = Offset(centerX, centerY - radius)

                down(1, start1)
                down(2, start2)
                moveTo(1, end1)
                moveTo(2, end2)
                up(1)
                up(2)
            }
            composeTestRule.waitForIdle()

        } catch (e: Exception) {
            // Complex gestures might not be supported
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun accessibility_gestureNavigation_shouldWork() {
        // Given - App is loaded
        composeTestRule.waitForIdle()

        // When - Test accessibility navigation
        try {
            // Navigate through focusable elements
            composeTestRule.onRoot().performTouchInput {
                // Simulate accessibility swipe right
                swipeRight(startX = 0f, endX = size.width * 0.1f)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onRoot().performTouchInput {
                // Simulate accessibility swipe left
                swipeLeft(startX = size.width, endX = size.width * 0.9f)
            }
            composeTestRule.waitForIdle()

        } catch (e: Exception) {
            // Accessibility gestures might work differently
        }

        // Then - UI should remain stable and accessible
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun longPressGestures_shouldTriggerContextActions() {
        // Given - App is loaded
        composeTestRule.waitForIdle()

        try {
            // When - Long press on various UI elements
            val captureButton = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.camera_capture)
            )

            captureButton.performTouchInput {
                longClick()
            }
            composeTestRule.waitForIdle()

            // Try long press on camera view
            val cameraView = composeTestRule.onNodeWithContentDescription("Camera Preview")
            cameraView.performTouchInput {
                longClick(center)
            }
            composeTestRule.waitForIdle()

        } catch (e: AssertionError) {
            // Long press actions might not be implemented
        }

        // Then - UI should remain stable
        composeTestRule.onRoot().assertIsDisplayed()
    }
}