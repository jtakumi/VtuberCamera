package com.example.vtubercamera.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import com.example.vtubercamera.R
import org.junit.Rule
import org.junit.Test

class CameraScreenIconTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun switchCameraIcon_isDisplayed() {
        composeTestRule.setContent {
            CameraScreen()
        }

        val description = composeTestRule.activity.getString(R.string.switch_camera)
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed()
    }
}
