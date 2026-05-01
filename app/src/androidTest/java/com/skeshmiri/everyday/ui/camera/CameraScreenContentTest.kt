package com.skeshmiri.everyday.ui.camera

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.skeshmiri.everyday.model.DailyPhoto
import com.skeshmiri.everyday.ui.theme.EverydayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CameraScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsPermissionRequestWhenCameraPermissionIsMissing() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = false,
                        isLoading = false,
                    ),
                    onRequestPermission = {},
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithText("Allow camera").assertIsDisplayed()
    }

    @Test
    fun showsLoadingStateWhenTodaysPhotoExists() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                        todayPhoto = DailyPhoto(
                            id = 1L,
                            uri = Uri.parse("content://everyday/photo/1"),
                            displayName = "2026-03-27_084500.jpg",
                            dateKey = "2026-03-27",
                            capturedAt = Instant.parse("2026-03-27T08:45:00Z"),
                            width = 1200,
                            height = 1600,
                        ),
                    ),
                    onRequestPermission = {},
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNode(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate),
        ).assertIsDisplayed()
    }

    @Test
    fun showsFramingOverlayWhileReadyToCapture() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                    ),
                    onRequestPermission = {},
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFramingOverlayTag).assertIsDisplayed()
    }

    @Test
    fun hidesFramingOverlayWhenDisabled() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                    ),
                    showFramingOverlay = false,
                    onRequestPermission = {},
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onAllNodesWithTag(CameraFramingOverlayTag).assertCountEquals(0)
    }

    @Test
    fun tappingCaptureButtonCallsOnCapture() {
        var captureCount = 0

        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                    ),
                    onRequestPermission = {},
                    onCapture = { captureCount += 1 },
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraCaptureButtonTag).performClick()
        composeRule.runOnIdle {
            assertEquals(1, captureCount)
        }
    }

    @Test
    fun longPressingCaptureButtonTogglesFramingOverlay() {
        composeRule.setContent {
            var showOverlay by remember { mutableStateOf(true) }

            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                    ),
                    showFramingOverlay = showOverlay,
                    onRequestPermission = {},
                    onCapture = {},
                    onToggleFramingOverlay = { showOverlay = !showOverlay },
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraCaptureButtonTag).performTouchInput {
            longClick()
        }

        composeRule.onAllNodesWithTag(CameraFramingOverlayTag).assertCountEquals(0)
    }
}
