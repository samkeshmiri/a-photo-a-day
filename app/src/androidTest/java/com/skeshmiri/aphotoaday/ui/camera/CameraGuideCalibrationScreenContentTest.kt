package com.skeshmiri.aphotoaday.ui.camera

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

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
        composeRule.onNodeWithTag(CameraGuideLatestPhotoOverlayButtonTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideLinesButtonTag).assertIsDisplayed()
        composeRule.onAllNodesWithTag(CameraCaptureButtonTag).assertCountEquals(0)
    }

    @Test
    fun disablesLatestPhotoOverlayButtonWhenNoLatestPhotoExists() {
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

        composeRule.onNodeWithTag(CameraGuideLatestPhotoOverlayButtonTag).assertIsNotEnabled()
    }

    @Test
    fun togglesLatestPhotoOverlayWhenLatestPhotoExists() {
        var showLatestPhotoOverlay by mutableStateOf(false)
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
                    latestPhoto = latestPhoto(),
                    showLatestPhotoOverlay = showLatestPhotoOverlay,
                    onLatestPhotoOverlayCheckedChange = { showLatestPhotoOverlay = it },
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraGuideLatestPhotoOverlayButtonTag)
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(true, showLatestPhotoOverlay)
        }
        composeRule.onNodeWithTag(CameraGuideLatestPhotoOverlayTag).assertIsDisplayed()
    }

    @Test
    fun togglesGuideLines() {
        var showGuideLines by mutableStateOf(true)
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
                    showGuideLines = showGuideLines,
                    onGuideLinesCheckedChange = { showGuideLines = it },
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFramingOverlayTag).assertIsDisplayed()
        composeRule.onNodeWithTag(CameraGuideLinesButtonTag).performClick()

        composeRule.runOnIdle {
            assertEquals(false, showGuideLines)
        }
        composeRule.onAllNodesWithTag(CameraFramingOverlayTag).assertCountEquals(0)
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
    fun doubleTappingGuideSlidersSnapsThemToMiddle() {
        var widthProgress by mutableStateOf(0.2f)
        var heightProgress by mutableStateOf(0.8f)

        composeRule.setContent {
            EverydayTheme {
                CameraGuideCalibrationScreenContent(
                    hasCameraPermission = true,
                    guideSettings = CameraGuideSettings(
                        verticalGuideProgress = widthProgress,
                        horizontalGuideProgress = heightProgress,
                    ),
                    onRequestPermission = {},
                    onVerticalGuideProgressChange = { widthProgress = it },
                    onHorizontalGuideProgressChange = { heightProgress = it },
                    hasUnsavedChanges = true,
                    onSaveGuideSettings = {},
                    onResetGuideSettings = {},
                    onClose = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraGuideVerticalSliderTag).performTouchInput {
            doubleClick()
        }
        composeRule.onNodeWithTag(CameraGuideHorizontalSliderTag).performTouchInput {
            doubleClick()
        }

        composeRule.runOnIdle {
            assertEquals(0.5f, widthProgress, 0.001f)
            assertEquals(0.5f, heightProgress, 0.001f)
        }
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

    private fun latestPhoto(): DailyPhoto =
        DailyPhoto(
            id = 1L,
            uri = Uri.EMPTY,
            displayName = "latest.jpg",
            dateKey = "2026-06-05",
            capturedAt = Instant.parse("2026-06-05T08:00:00Z"),
            width = 1080,
            height = 1440,
        )
}
