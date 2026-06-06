package com.skeshmiri.aphotoaday.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.skeshmiri.aphotoaday.ui.theme.EverydayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PermissionIntroScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun explainsStartupPermissionsBeforeContinuing() {
        composeRule.setContent {
            EverydayTheme {
                PermissionIntroScreen(onContinue = {})
            }
        }

        composeRule.onNodeWithText("A Photo a Day stays private").assertIsDisplayed()
        composeRule.onNodeWithText("Gallery access").assertIsDisplayed()
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Camera access").assertIsDisplayed()
        composeRule.onNodeWithText("Used only to take today's photo when you are ready.").assertIsDisplayed()
        composeRule.onNodeWithText("No internet access").assertIsDisplayed()
        composeRule.onNodeWithText("The app is shut off from the internet. Photos stay local on this device.")
            .assertIsDisplayed()
    }

    @Test
    fun continueButtonCallsHandler() {
        var continueCount = 0
        composeRule.setContent {
            EverydayTheme {
                PermissionIntroScreen(onContinue = { continueCount += 1 })
            }
        }

        composeRule.onNodeWithText("Continue").performClick()

        composeRule.runOnIdle {
            assertEquals(1, continueCount)
        }
    }
}
