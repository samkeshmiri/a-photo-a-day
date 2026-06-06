package com.skeshmiri.aphotoaday.ui.camera

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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CameraScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsCameraPermissionMessageWhenCameraPermissionIsMissing() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = false,
                        isLoading = false,
                    ),
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithText("Camera access is needed to take your daily photo.").assertIsDisplayed()
        composeRule.onNodeWithText("Enable camera access in Android Settings, then return to the app.")
            .assertIsDisplayed()
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
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFramingOverlayTag).assertIsDisplayed()
    }

    @Test
    fun showsFirstPhotoInstructionStepWhenReadyForFirstCapture() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                        hasAnySavedPhotos = false,
                    ),
                    hasSeenFirstPhotoInstructions = false,
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFirstPhotoInstructionsTag).assertIsDisplayed()
        composeRule.onNodeWithText("Line your eyes up with the grid cross-sections.").assertIsDisplayed()
    }

    @Test
    fun continuingFirstPhotoInstructionsAddsGridToggleInstruction() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                        hasAnySavedPhotos = false,
                    ),
                    hasSeenFirstPhotoInstructions = false,
                    onCapture = {},
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFirstPhotoInstructionsContinueTag).performClick()

        composeRule.onNodeWithText("Line your eyes up with the grid cross-sections.").assertIsDisplayed()
        composeRule.onNodeWithText("Press and hold the camera button to turn the grid lines on or off.")
            .assertIsDisplayed()
    }

    @Test
    fun completingFirstPhotoInstructionsCallsHandlerAndRemovesOverlay() {
        var completionCount = 0

        composeRule.setContent {
            var hasSeenInstructions by remember { mutableStateOf(false) }

            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                        hasAnySavedPhotos = false,
                    ),
                    hasSeenFirstPhotoInstructions = hasSeenInstructions,
                    onCapture = {},
                    onFirstPhotoInstructionsCompleted = {
                        completionCount += 1
                        hasSeenInstructions = true
                    },
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraFirstPhotoInstructionsContinueTag).performClick()
        composeRule.onNodeWithTag(CameraFirstPhotoInstructionsContinueTag).performClick()

        composeRule.onAllNodesWithTag(CameraFirstPhotoInstructionsTag).assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(1, completionCount)
        }
    }

    @Test
    fun disablesCaptureWhileFirstPhotoInstructionsAreActive() {
        var captureCount = 0

        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                        hasAnySavedPhotos = false,
                    ),
                    hasSeenFirstPhotoInstructions = false,
                    onCapture = { captureCount += 1 },
                    preview = { Box {} },
                )
            }
        }

        composeRule.onNodeWithTag(CameraCaptureButtonTag).assertIsNotEnabled()
        composeRule.runOnIdle {
            assertEquals(0, captureCount)
        }
    }

    @Test
    fun firstPhotoInstructionsForceFramingOverlayVisibleWithoutSavedPreference() {
        composeRule.setContent {
            EverydayTheme {
                CameraScreenContent(
                    uiState = CameraUiState(
                        hasCameraPermission = true,
                        isLoading = false,
                        hasAnySavedPhotos = false,
                    ),
                    showFramingOverlay = false,
                    hasSeenFirstPhotoInstructions = false,
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
