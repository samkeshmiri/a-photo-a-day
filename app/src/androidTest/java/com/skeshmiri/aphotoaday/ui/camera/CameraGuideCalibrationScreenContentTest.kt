package com.skeshmiri.aphotoaday.ui.camera

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Assert.assertEquals
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
                    hasUnsavedChanges = false,
                    onSaveGuideSettings = {},
                    onResetGuideSettings = {},
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFramingOverlayTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideVerticalSliderTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideHorizontalSliderTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideSaveButtonTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideResetButtonTag).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CameraCaptureButtonTag).assertCountEquals(0)
    }

    @Test
    fun disablesSaveAndResetWhenGuidePositionIsUnchanged() {
        composeRule.setContent {
            EverydayTheme {
                CameraGuideCalibrationScreenContent(
                    hasCameraPermission = true,
                    guideSettings = CameraGuideSettings(),
                    onRequestPermission = {},
                    onVerticalGuideProgressChange = {},
                    onHorizontalGuideProgressChange = {},
                    hasUnsavedChanges = false,
                    onSaveGuideSettings = {},
                    onResetGuideSettings = {},
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraGuideSaveButtonTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(CameraGuideResetButtonTag).assertIsNotEnabled()
    }

    @Test
    fun enablesSaveAndResetWhenGuidePositionChanges() {
        composeRule.setContent {
            EverydayTheme {
                CameraGuideCalibrationScreenContent(
                    hasCameraPermission = true,
                    guideSettings = CameraGuideSettings(verticalGuideProgress = 0.7f),
                    onRequestPermission = {},
                    onVerticalGuideProgressChange = {},
                    onHorizontalGuideProgressChange = {},
                    hasUnsavedChanges = true,
                    onSaveGuideSettings = {},
                    onResetGuideSettings = {},
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraGuideSaveButtonTag).assertIsEnabled()
        composeRule.onNodeWithTag(CameraGuideResetButtonTag).assertIsEnabled()
    }

    @Test
    fun saveAndResetButtonsInvokeCallbacks() {
        var saveCount = 0
        var resetCount = 0
        composeRule.setContent {
            EverydayTheme {
                CameraGuideCalibrationScreenContent(
                    hasCameraPermission = true,
                    guideSettings = CameraGuideSettings(),
                    onRequestPermission = {},
                    onVerticalGuideProgressChange = {},
                    onHorizontalGuideProgressChange = {},
                    hasUnsavedChanges = true,
                    onSaveGuideSettings = { saveCount += 1 },
                    onResetGuideSettings = { resetCount += 1 },
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraGuideSaveButtonTag).performClick()
        composeRule.onNodeWithTag(CameraGuideResetButtonTag).performClick()

        composeRule.runOnIdle {
            assertEquals(1, saveCount)
            assertEquals(1, resetCount)
        }
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
                    hasUnsavedChanges = false,
                    onSaveGuideSettings = {},
                    onResetGuideSettings = {},
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
    }
}
