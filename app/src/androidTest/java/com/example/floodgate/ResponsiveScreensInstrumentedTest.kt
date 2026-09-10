package com.example.floodgate

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.floodgate.ui.auth.SignInScreen
import com.example.floodgate.ui.auth.SignUpScreen
import com.example.floodgate.ui.onboarding.OnboardingAlertsScreen
import com.example.floodgate.ui.onboarding.OnboardingMonitoringScreen
import com.example.floodgate.ui.onboarding.OnboardingScreen
import com.example.floodgate.ui.theme.FloodGateTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResponsiveScreensInstrumentedTest {
    @get:Rule
    val ui = createComposeRule()

    @Test
    fun allOnboardingPagesFitAndNavigateOnSmallPhone() {
        ui.setContent {
            var page by remember { mutableIntStateOf(0) }
            FloodGateTheme {
                Box(Modifier.requiredSize(320.dp, 640.dp).testTag(SMALL_SCREEN)) {
                    when (page) {
                        0 -> OnboardingScreen(onNextClick = { page = 1 }, onPageSelected = { page = it })
                        1 -> OnboardingMonitoringScreen(
                            onNextClick = { page = 2 },
                            onPageSelected = { page = it }
                        )
                        else -> OnboardingAlertsScreen(
                            onGetStartedClick = {},
                            onPageSelected = { page = it }
                        )
                    }
                }
            }
        }

        ui.onNodeWithText("Smart Flood\nProtection").assertFullyInsideSmallScreen()
        ui.onNodeWithText("Next").assertFullyInsideSmallScreen().performClick()

        ui.onNodeWithText("Real-Time\nMonitoring").assertFullyInsideSmallScreen()
        ui.onNodeWithContentDescription("Sample flood barrier outside a home")
            .assertFullyInsideSmallScreen()
        ui.onNodeWithText("Water Level").assertFullyInsideSmallScreen()
        ui.onNodeWithText("System Status").assertFullyInsideSmallScreen()
        ui.onNodeWithText("Next").assertFullyInsideSmallScreen()
        capture("onboarding-monitoring-small")
        ui.onNodeWithText("Next").performClick()

        ui.onNodeWithText("Instant Alerts,\nPeace of Mind").assertFullyInsideSmallScreen()
        ui.onNodeWithText("Get Started").assertFullyInsideSmallScreen()
        ui.onNodeWithContentDescription("Go to onboarding page 1").performClick()
        ui.onNodeWithText("Smart Flood\nProtection").assertIsDisplayed()
    }

    @Test
    fun authenticationScreensRemainReachableOnSmallPhoneWithLargeText() {
        ui.setContent {
            var signUp by remember { mutableStateOf(false) }
            val density = LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1.3f)
            ) {
                FloodGateTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp).testTag(SMALL_SCREEN)) {
                        if (signUp) {
                            SignUpScreen(onSignInClick = { signUp = false })
                        } else {
                            SignInScreen(onSignUpClick = { signUp = true })
                        }
                    }
                }
            }
        }

        ui.onNodeWithText("Great to have you back!").assertFullyInsideSmallScreen()
        ui.onNodeWithText("Continue with Google").performScrollTo().assertFullyInsideSmallScreen()
        ui.onNodeWithText("Sign up").assertFullyInsideSmallScreen().performClick()

        ui.onNodeWithText("Create your account").assertFullyInsideSmallScreen()
        ui.onNodeWithText("Continue with Google").performScrollTo().assertFullyInsideSmallScreen()
        ui.onNodeWithText("Already have an account?").performScrollTo().assertFullyInsideSmallScreen()
        ui.onNodeWithText("Sign in").performScrollTo().assertFullyInsideSmallScreen()
        capture("authentication-small-large-text")
    }

    private fun SemanticsNodeInteraction.assertFullyInsideSmallScreen(): SemanticsNodeInteraction {
        assertIsDisplayed()
        val screenBounds = ui.onNodeWithTag(SMALL_SCREEN).fetchSemanticsNode().boundsInRoot
        val contentBounds = fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected $contentBounds to fit inside small screen $screenBounds",
            contentBounds.fitsInside(screenBounds)
        )
        return this
    }

    private fun Rect.fitsInside(container: Rect): Boolean {
        val tolerance = 1f
        return left >= container.left - tolerance && top >= container.top - tolerance &&
            right <= container.right + tolerance && bottom <= container.bottom + tolerance
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "responsive-test-screenshots")
            .apply { mkdirs() }
        val bitmap = ui.onNodeWithTag(SMALL_SCREEN).captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private companion object {
        const val SMALL_SCREEN = "small_phone_surface"
    }
}
