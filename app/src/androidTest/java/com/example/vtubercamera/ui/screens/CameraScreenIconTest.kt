package com.example.vtubercamera.ui.screens

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.vtubercamera.R
import com.example.vtubercamera.utils.PermissionUtils
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CameraScreenIconTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        // Grant camera and gallery permissions once for all tests
        @JvmField
        @ClassRule
        val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.CAMERA,
            *PermissionUtils.getRequiredMediaPermissions()
        )
    }

    @Test
    fun switchCameraIcon_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        val description = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.switch_camera)
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun toggleFlashIcon_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        val description = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.flash_mode_toggle)
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }
}
