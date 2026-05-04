package com.skeshmiri.aphotoaday.ui.gallery

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.skeshmiri.aphotoaday.model.DailyPhoto
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class GalleryScreenContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsExportActionWhenPhotosExist() {
        composeRule.setContent {
            EverydayTheme {
                GalleryScreenContent(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = listOf(photo(id = 1L)),
                        estimatedDurationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(1, 5),
                    ),
                    onOpenPhoto = {},
                    onOpenExportDialog = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Export video").assertIsDisplayed()
    }
    @Test
    fun exportDialogUpdatesEstimatedLengthWhenPresetChanges() {
        composeRule.setContent {
            var selectedFps by mutableStateOf(5)
            val photos = List(36) { index -> photo(id = index.toLong()) }

            EverydayTheme {
                GalleryExportDialog(
                    uiState = GalleryUiState(
                        isLoading = false,
                        photos = photos,
                        selectedFps = selectedFps,
                        estimatedDurationSeconds = GalleryVideoExportDefaults.estimatedDurationSeconds(
                            photoCount = photos.size,
                            fps = selectedFps,
                        ),
                    ),
                    onDismiss = {},
                    onSelectFps = { selectedFps = it },
                    onExport = {},
                    onDone = {},
                )
            }
        }

        composeRule.onNodeWithText("Estimated length: 7.2 seconds").assertIsDisplayed()
        composeRule.onNodeWithText("3 fps").performClick()
        composeRule.onNodeWithText("Estimated length: 12.0 seconds").assertIsDisplayed()
    }

    private fun photo(id: Long) = DailyPhoto(
        id = id,
        uri = Uri.parse("content://everyday/photo/$id"),
        displayName = "2026-03-27_084500.jpg",
        dateKey = "2026-03-27",
        capturedAt = Instant.parse("2026-03-27T08:45:00Z"),
        width = 1200,
        height = 1600,
    )
}
