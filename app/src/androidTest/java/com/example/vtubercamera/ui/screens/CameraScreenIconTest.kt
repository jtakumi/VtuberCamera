package com.example.vtubercamera.ui.screens

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.rule.GrantPermissionRule
import com.example.vtubercamera.R
import com.example.vtubercamera.utils.PermissionUtils
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class CameraScreenIconTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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

        val description = composeTestRule.activity.getString(R.string.switch_camera)
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun toggleFlashIcon_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        val description = composeTestRule.activity.getString(R.string.flash_mode_toggle)
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }
}
