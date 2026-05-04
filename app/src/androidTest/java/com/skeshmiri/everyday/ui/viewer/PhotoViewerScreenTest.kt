package com.skeshmiri.everyday.ui.viewer

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.skeshmiri.everyday.ui.theme.EverydayTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PhotoViewerScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tappingImageCallsOnClose() {
        var closed by mutableStateOf(false)

        composeRule.setContent {
            EverydayTheme {
                PhotoViewerScreen(
                    uri = Uri.parse("content://everyday/photo/1"),
                    title = "2026-05-03",
                    contentDescription = "2026-05-03_084500.jpg",
                    capturedAt = null,
                    onClose = { closed = true },
                    onShare = {},
                )
            }
        }

        composeRule.onNodeWithTag(PhotoViewerImageTag).performClick()

        composeRule.runOnIdle {
            assertTrue(closed)
        }
    }

    @Test
    fun tappingShareButtonCallsOnShare() {
        var shared by mutableStateOf(false)

        composeRule.setContent {
            EverydayTheme {
                PhotoViewerScreen(
                    uri = Uri.parse("content://everyday/photo/1"),
                    title = "2026-05-03",
                    contentDescription = "2026-05-03_084500.jpg",
                    capturedAt = null,
                    onClose = {},
                    onShare = { shared = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Share photo").performClick()

        composeRule.runOnIdle {
            assertTrue(shared)
        }
    }
}
