package com.example.floodgate

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.example.floodgate.ui.auth.SignInScreen
import com.example.floodgate.ui.theme.FloodGateTheme
import org.junit.Rule
import org.junit.Test

class PasswordVisibilityInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun passwordIcon_togglesBetweenHiddenAndVisibleStates() {
        composeRule.setContent {
            FloodGateTheme {
                SignInScreen()
            }
        }

        composeRule
            .onNodeWithContentDescription("Show password")
            .assertExists()
            .performClick()

        composeRule
            .onNodeWithContentDescription("Hide password")
            .assertExists()
            .performClick()

        composeRule
            .onNodeWithContentDescription("Show password")
            .assertExists()
    }
}
