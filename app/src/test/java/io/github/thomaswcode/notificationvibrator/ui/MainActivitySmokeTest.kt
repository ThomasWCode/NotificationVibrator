package io.github.thomaswcode.notificationvibrator.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.thomaswcode.notificationvibrator.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opens every screen to catch crashes in composition; it can't check how vibrations feel. */
@RunWith(RobolectricTestRunner::class)
// Roughly a Galaxy S24 screen
@Config(sdk = [34], qualifiers = "w384dp-h832dp-xxhdpi")
class MainActivitySmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensOnSetupUntilNotificationAccessIsGranted() {
        compose.onNodeWithText("1. Notification access").assertIsDisplayed()
        compose.onNodeWithText("Turn on access").assertIsDisplayed()
        compose.onNodeWithText("Global mute (recommended)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun perAppModeShowsItsInstructions() {
        compose.onNodeWithText("Per-app mute").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTextCount("Vibrate for Silent categories") > 0
        }
    }

    @Test
    fun defaultPatternCanBeChangedFromAppsTab() {
        compose.onNodeWithText("Apps").performClick()
        compose.onNodeWithText("Default pattern").assertIsDisplayed()
        compose.onNodeWithText("Default pattern").performClick()
        compose.onNodeWithText("Save").assertIsDisplayed()
        compose.onNodeWithText("Save").performClick()
    }

    @Test
    fun tapRecorderOpensAndReturns() {
        compose.onNodeWithText("Patterns").performClick()
        compose.onNodeWithText("Record rhythm").performClick()
        compose.onNodeWithText("Hold here").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Record rhythm").assertIsDisplayed()
    }

    @Test
    fun morseScreenEncodesText() {
        compose.onNodeWithText("Patterns").performClick()
        compose.onNodeWithText("Morse code").performClick()
        compose.onNodeWithText("Text").performTextInput("W")
        compose.onNodeWithText(".--").assertIsDisplayed()
    }

    @Test
    fun activityTabShowsEmptyState() {
        compose.onNodeWithText("Activity").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTextCount("Nothing yet", substring = true) > 0 }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTextCount(
        text: String,
        substring: Boolean = false,
    ): Int = onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).fetchSemanticsNodes().size
}
