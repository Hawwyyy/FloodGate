package com.example.floodgate

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.example.floodgate.data.onboarding.OnboardingPreferences
import com.example.floodgate.ui.startup.StartupFlow
import com.example.floodgate.ui.theme.FloodGateTheme
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class StartupFlowInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferencesName = "startup_flow_test"
    private lateinit var preferences: OnboardingPreferences

    @Before
    fun setUp() {
        clearTestPreferences()
        preferences = OnboardingPreferences(context, preferencesName)
        composeRule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        clearTestPreferences()
    }

    @Test
    fun firstLaunch_showsSplashThenOnboarding_andRemembersCompletionOnNextLaunch() {
        val launch = mutableStateOf(0)
        composeRule.setContent {
            key(launch.value) {
                FloodGateTheme {
                    StartupFlow(isSignedIn = false, onExit = {}, preferences = preferences) {
                        Text("Authentication")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Smart Protection").assertExists()
        composeRule.onNodeWithText("Next").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(3_000)
        composeRule.onNodeWithText("Smart Protection").assertExists()
        composeRule.onNodeWithText("Next").assertDoesNotExist()
        finishSplash()
        composeRule.onNodeWithText("Smart Flood\nProtection").assertExists()
        assertFalse(preferences.isCompleted)
        composeRule.onNodeWithText("Next").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Real-Time\nMonitoring").assertExists()
        composeRule.onNodeWithText("Barrier Status").assertExists()
        composeRule.onNodeWithText("Water Level").assertExists()
        composeRule.onNodeWithText("System Status").assertExists()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Instant Alerts,\nPeace of Mind").assertExists()
        composeRule.onNodeWithText("Get Started").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Authentication").assertExists()
        assertTrue(OnboardingPreferences(context, preferencesName).isCompleted)

        composeRule.runOnIdle { launch.value++ }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Smart Protection").assertExists()
        finishSplash()
        composeRule.onNodeWithText("Authentication").assertExists()
        composeRule.onNodeWithText("Next").assertDoesNotExist()
    }

    @Test
    fun signedInUser_skipsOnboardingAfterSplash() {
        composeRule.setContent {
            FloodGateTheme {
                StartupFlow(isSignedIn = true, onExit = {}, preferences = preferences) {
                    Text("Authenticated screen")
                }
            }
        }

        composeRule.onNodeWithText("Smart Protection").assertExists()
        finishSplash()
        composeRule.onNodeWithText("Authenticated screen").assertExists()
        composeRule.onNodeWithText("Next").assertDoesNotExist()
    }

    @Test
    fun noOnboardingPages_goesDirectlyToAuthenticationAfterSplash() {
        composeRule.setContent {
            FloodGateTheme {
                StartupFlow(
                    isSignedIn = false,
                    onExit = {},
                    onboardingPages = emptyList(),
                    preferences = preferences
                ) {
                    Text("Authentication")
                }
            }
        }

        finishSplash()
        composeRule.onNodeWithText("Authentication").assertExists()
        assertFalse(preferences.isCompleted)
    }

    @Test
    fun availablePages_advanceInOrder_andOnlyFinalPageCompletesOnboarding() {
        val pages: List<@Composable (() -> Unit, (Int) -> Unit) -> Unit> = (1..3).map { number ->
            { onNext, _ -> Button(onClick = onNext) { Text("Page $number") } }
        }
        composeRule.setContent {
            FloodGateTheme {
                StartupFlow(
                    isSignedIn = false,
                    onExit = {},
                    onboardingPages = pages,
                    preferences = preferences
                ) {
                    Text("Authentication")
                }
            }
        }

        finishSplash()
        for (number in 1..3) {
            assertFalse(preferences.isCompleted)
            composeRule.onNodeWithText("Page $number").performClick()
            composeRule.mainClock.advanceTimeByFrame()
        }
        composeRule.onNodeWithText("Authentication").assertExists()
        assertTrue(preferences.isCompleted)
    }

    @Test
    fun activityRecreation_keepsCurrentOnboardingPage_withoutShowingSplashAgain() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            FloodGateTheme {
                StartupFlow(isSignedIn = false, onExit = {}, preferences = preferences) {
                    Text("Authentication")
                }
            }
        }

        finishSplash()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Smart Flood\nProtection").assertExists()
        composeRule.onNodeWithText("Smart Protection").assertDoesNotExist()
        assertFalse(preferences.isCompleted)
    }

    @Test
    fun progressDotsNavigateDirectlyBetweenAllThreePages() {
        composeRule.setContent {
            FloodGateTheme {
                StartupFlow(isSignedIn = false, onExit = {}, preferences = preferences) {
                    Text("Authentication")
                }
            }
        }

        finishSplash()
        composeRule.onNodeWithContentDescription("Go to onboarding page 3").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Instant Alerts,\nPeace of Mind").assertExists()
        assertFalse(preferences.isCompleted)
        composeRule.onNodeWithContentDescription("Go to onboarding page 2").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Real-Time\nMonitoring").assertExists()
        composeRule.onNodeWithContentDescription("Go to onboarding page 1").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Smart Flood\nProtection").assertExists()
    }

    @Test
    fun captureSplashAndAllOnboardingScreens() {
        composeRule.setContent {
            FloodGateTheme {
                StartupFlow(isSignedIn = false, onExit = {}, preferences = preferences) {
                    Text("Authentication")
                }
            }
        }

        capture("splash-tap-to-continue")
        finishSplash()
        capture("onboarding-1")
        composeRule.onNodeWithText("Next").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        capture("onboarding-2")
        composeRule.onNodeWithText("Next").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        capture("onboarding-3")
    }

    private fun finishSplash() {
        composeRule.onNodeWithTag("splash_screen").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun clearTestPreferences() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    private fun capture(name: String) {
        val directory = File(context.getExternalFilesDir(null), "onboarding-test-screenshots")
            .apply { mkdirs() }
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
