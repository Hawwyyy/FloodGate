package com.example.floodgate

import android.graphics.Bitmap
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.floodgate.data.auth.*
import com.example.floodgate.ui.auth.*
import com.example.floodgate.ui.dashboard.DashboardScreen
import com.example.floodgate.ui.theme.FloodGateTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DashboardInstrumentedTest {
    @get:Rule val ui = createComposeRule()
    private val repository = FakeAuthRepository()
    private val recovery = RecoveryViewModel(object : PasswordResetRepository {
        override fun sendResetEmail(email: String, callback: (Result<Unit>) -> Unit) = Unit
    })
    private var exits = 0
    private var backDispatcher: OnBackPressedDispatcher? = null

    private fun launch() {
        ui.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            FloodGateTheme { FloodGateAuthApp({ exits++ }, repository, recovery) }
        }
    }

    @Test fun successfulSignInOpensDashboardAndSignOutReturnsToSignIn() {
        launch()
        ui.onAllNodes(hasSetTextAction())[0].performTextInput("dashboard@example.com")
        ui.onAllNodes(hasSetTextAction())[1].performTextInput("Password1!")
        ui.onNodeWithText("Sign In", ignoreCase = false).performScrollTo().performClick()
        assertEquals(1, repository.signIns)
        ui.onNodeWithTag("dashboard").assertDoesNotExist()
        ui.runOnIdle { repository.completeSignIn() }
        ui.onNodeWithTag("dashboard").assertIsDisplayed()
        ui.onNodeWithText("Demo data · No device connected").assertIsDisplayed()
        ui.onNodeWithText("Profile").performClick()
        ui.onNodeWithText("Signed in as dashboard@example.com").assertIsDisplayed()
        ui.onNodeWithText("Sign Out").performClick()
        ui.onNodeWithTag("dashboard").assertDoesNotExist()
        ui.onNodeWithText("Sign In", ignoreCase = false).assertExists()
        assertNull(repository.currentUser)
        assertEquals(1, repository.signOuts)
    }

    @Test fun failedSignInDoesNotOpenDashboard() {
        launch()
        ui.onAllNodes(hasSetTextAction())[0].performTextInput("dashboard@example.com")
        ui.onAllNodes(hasSetTextAction())[1].performTextInput("Password1!")
        ui.onNodeWithText("Sign In", ignoreCase = false).performScrollTo().performClick()
        ui.runOnIdle { repository.completion!!(SignInResult.Failure(AuthServiceError.INCORRECT_CREDENTIALS)) }
        ui.onNodeWithTag("dashboard").assertDoesNotExist()
        ui.onNodeWithText("Incorrect email or password.").assertExists()
    }

    @Test fun savedSessionOpensDashboardAndBackDoesNotReturnToSignIn() {
        repository.currentUser = AuthenticatedUser("test-user", "dashboard@example.com")
        launch()
        ui.onNodeWithTag("dashboard").assertIsDisplayed()
        ui.runOnIdle { backDispatcher!!.onBackPressed() }
        assertEquals(1, exits)
        ui.onNodeWithTag("dashboard").assertIsDisplayed()
        ui.onNodeWithText("Sign In", ignoreCase = false).assertDoesNotExist()
    }

    @Test fun dashboardSurvivesStateRestoration() {
        repository.currentUser = AuthenticatedUser("test-user", "dashboard@example.com")
        val restoration = StateRestorationTester(ui)
        restoration.setContent {
            FloodGateTheme { FloodGateAuthApp({}, repository, recovery) }
        }
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithTag("dashboard").assertIsDisplayed()
        ui.onNodeWithText("Demo data · No device connected").assertIsDisplayed()
    }

    @Test fun demoControlsDoNotPretendToOperateARealBarrier() {
        ui.setContent { FloodGateTheme { DashboardScreen("dashboard@example.com", {}) } }
        ui.onNodeWithText("Deploy").performScrollTo().performClick()
        ui.onNodeWithText("No command was sent.", substring = true).assertIsDisplayed()
        ui.onNodeWithText("Close").performClick()
        ui.onNodeWithText("Retract").performScrollTo().performClick()
        ui.onNodeWithText("No command was sent.", substring = true).assertIsDisplayed()
        ui.onNodeWithText("Close").performClick()
        ui.onNodeWithText("Deployed").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Devices").performClick()
        ui.onNodeWithText("No device integration", substring = true).assertIsDisplayed()
        ui.onNodeWithText("Close").performClick()
        ui.onNodeWithText("Notification").performClick()
        ui.onNodeWithText("Live notifications are not connected", substring = true).assertIsDisplayed()
    }

    @Test fun captureRegularDashboard() {
        ui.setContent { FloodGateTheme { DashboardScreen("dashboard@example.com", {}) } }
        ui.onNodeWithText("Home").assertIsDisplayed()
        ui.onNodeWithContentDescription("Sample flood barrier outside a home").assertIsDisplayed()
        capture("dashboard-regular")
        ui.onNodeWithText("Retract").performScrollTo().assertIsDisplayed()
        capture("dashboard-regular-bottom")
    }

    @Test fun smallPhoneAndLargeFontsKeepControlsReachable() {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                FloodGateTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        DashboardScreen("dashboard@example.com", {})
                    }
                }
            }
        }
        ui.onNodeWithText("Demo data · No device connected").assertIsDisplayed()
        capture("dashboard-small-top")
        ui.onNodeWithText("System Status").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Deploy").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Retract").assertIsDisplayed()
        ui.onNodeWithText("Profile").performScrollTo().assertIsDisplayed()
        capture("dashboard-small-bottom")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "dashboard-test-screenshots").apply { mkdirs() }
        val bitmap = ui.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private class FakeAuthRepository : AuthRepository {
        override var currentUser: AuthenticatedUser? = null
        var signIns = 0
        var signOuts = 0
        var completion: ((SignInResult) -> Unit)? = null
        override fun register(credentials: SignUpCredentials, onResult: (RegistrationResult) -> Unit) = Unit
        override fun signIn(credentials: SignInCredentials, onResult: (SignInResult) -> Unit) {
            signIns++
            completion = onResult
        }
        fun completeSignIn() {
            currentUser = AuthenticatedUser("test-user", "dashboard@example.com")
            completion!!(SignInResult.Success(currentUser!!))
        }
        override fun signOut() { signOuts++; currentUser = null }
    }
}
