package com.skeshmiri.aphotoaday.ui.camera

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Rule
import org.junit.Test

class CameraGuideCalibrationScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsCameraPreviewGuidesAndSliders() {
        composeRule.setContent {
            EverydayTheme {
                CameraGuideCalibrationScreenContent(
                    hasCameraPermission = true,
                    guideSettings = CameraGuideSettings(),
                    onRequestPermission = {},
                    onVerticalGuideProgressChange = {},
                    onHorizontalGuideProgressChange = {},
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFramingOverlayTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideVerticalSliderTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideHorizontalSliderTag).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CameraCaptureButtonTag).assertCountEquals(0)
    }

    @Test
    fun requestsCameraPermissionWhenMissing() {
        composeRule.setContent {
            EverydayTheme {
                CameraGuideCalibrationScreenContent(
                    hasCameraPermission = false,
                    guideSettings = CameraGuideSettings(),
                    onRequestPermission = {},
                    onVerticalGuideProgressChange = {},
                    onHorizontalGuideProgressChange = {},
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
    }
}
