package com.example.vtubercamera.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.vtubercamera.R
import com.example.vtubercamera.utils.PermissionUtils
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenIconTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        *PermissionUtils.getRequiredMediaPermissions()
    )

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun TestCameraScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.switch_camera)
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.FlashOff,
                    contentDescription = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.flash_mode_toggle)
                )
            }
        }
    }

    @Test
    fun switchCameraIcon_isDisplayed() {
        composeTestRule.setContent {
            TestCameraScreen()
        }

        val description = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.switch_camera)
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    @Test
    fun toggleFlashIcon_isDisplayed() {
        composeTestRule.setContent {
            TestCameraScreen()
        }

        val description = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.flash_mode_toggle)
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }
}
