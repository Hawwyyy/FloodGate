package com.example.floodgate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.floodgate.barrier.model.*
import com.example.floodgate.data.auth.*
import com.example.floodgate.ui.auth.*
import com.example.floodgate.ui.dashboard.DashboardScreen
import com.example.floodgate.ui.theme.FloodGateTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DashboardInstrumentedTest {
    @get:Rule val ui = createComposeRule()
    private val auth = FakeAuth()
    private val device = FakeBarrier()
    private val recovery = RecoveryViewModel(object : PasswordResetRepository {
        override fun sendResetEmail(email: String, callback: (Result<Unit>) -> Unit) = Unit
    })

    @Test fun loginOpensLiveDashboardAndSignOutCleansUp() {
        ui.setContent { FloodGateTheme { FloodGateAuthApp({}, auth, recovery, device) } }
        ui.onAllNodes(hasSetTextAction())[0].performTextInput("dashboard@example.com")
        ui.onAllNodes(hasSetTextAction())[1].performTextInput("Password1!")
        ui.onNodeWithText("Sign In", ignoreCase = false).performScrollTo().performClick()
        ui.onNodeWithText("FloodGate 01").assertIsDisplayed()
        ui.onNodeWithText("Sign out", ignoreCase = true).assertDoesNotExist()
        ui.onNodeWithText("Profile").performClick()
        ui.onNodeWithText("Sign out", ignoreCase = true).performClick()
        ui.onNodeWithText("Sign In", ignoreCase = false).assertExists()
        assertNull(auth.currentUser)
        assertTrue(device.stops > 0)
    }

    @Test fun savedLoginSurvivesRestoration() {
        auth.currentUser = AuthenticatedUser("test", "dashboard@example.com")
        val restoration = StateRestorationTester(ui)
        restoration.setContent { FloodGateTheme { FloodGateAuthApp({}, auth, recovery, device) } }
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("FloodGate 01").assertIsDisplayed()
    }

    @Test fun confirmationWritesFreshIdsAndDoesNotInventMovement() {
        ui.setContent { FloodGateTheme { DashboardScreen("demo@example.com", {}, device) } }
        repeat(2) {
            ui.onNodeWithTag("barrier_deploy").performScrollTo().performClick()
            ui.onNodeWithTag("barrier_confirm").performClick()
            ui.onNodeWithText("Command sent. Waiting for device state.").performScrollTo().assertIsDisplayed()
        }
        assertEquals(2, device.ids.size)
        assertNotEquals(device.ids[0], device.ids[1])
        ui.onNodeWithText("Retracted", ignoreCase = false).performScrollTo().assertIsDisplayed()
        ui.runOnIdle { device.emit(device.sample.copy(barrier = "DEPLOYED", source = "SWITCH", updatedAt = System.currentTimeMillis())) }
        ui.onNodeWithTag("open_device_details").performScrollTo().performClick()
        ui.onNodeWithText("View FloodGate 01 details").performScrollTo().performClick()
        ui.onNodeWithText("Physical switch").performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("barrier_retract").performScrollTo().performClick()
        ui.onNodeWithTag("barrier_confirm").performClick()
        assertEquals(BarrierAction.RETRACT, device.actions.last())
    }

    @Test fun smallScreenLargeTextAndFloodWithOutputsOff() {
        device.sample = device.sample.copy(waterStatus = "FLOOD CONFIRMED", highWater = true,
            suppressed = true, led = false, buzzer = false)
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                FloodGateTheme { Box(Modifier.requiredSize(320.dp, 640.dp)) { DashboardScreen("demo@example.com", {}, device) } }
            }
        }
        ui.onNodeWithText("Flood confirmed", ignoreCase = false).performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("water_percent").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Raw ADC").assertDoesNotExist()
        ui.onNodeWithText("Recent water readings").assertDoesNotExist()
        ui.onNodeWithTag("open_device_details").performScrollTo().performClick()
        ui.onNodeWithText("Remote control").assertDoesNotExist()
        ui.onNodeWithText("View FloodGate 01 details").performScrollTo().performClick()
        ui.onNodeWithText("Remote control").performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("barrier_deploy").performScrollTo().assertIsDisplayed().assertIsEnabled()
        ui.onNodeWithTag("barrier_retract").performScrollTo().assertIsDisplayed()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "barrier-small-screen.png").outputStream().use {
            ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        ui.runOnIdle { device.disconnect() }
        ui.onNodeWithTag("barrier_retract").assertIsNotEnabled()
    }

    private class FakeBarrier : BarrierRepository {
        var sample = BarrierTelemetry(raw = 3200, percent = 31.84, waterStatus = "HIGH",
            highWater = true, led = false, buzzer = false, barrier = "UNDEPLOYED",
            mode = "MANUAL", source = "WEBSITE", angle = 0, updatedAt = System.currentTimeMillis())
        lateinit var emit: (BarrierTelemetry) -> Unit
        lateinit var connection: (Boolean) -> Unit
        val ids = mutableListOf<String>()
        val actions = mutableListOf<BarrierAction>()
        var stops = 0
        override fun observe(onTelemetry: (BarrierTelemetry) -> Unit, onConnection: (Boolean) -> Unit,
                             onError: (BarrierError) -> Unit, onSignedOut: () -> Unit) {
            emit = onTelemetry; connection = onConnection
            onConnection(true); onTelemetry(sample.copy(updatedAt = System.currentTimeMillis()))
        }
        override fun send(action: BarrierAction, id: String, complete: (BarrierError?) -> Unit) {
            ids.add(id); actions.add(action); complete(null)
        }
        fun disconnect() = connection(false)
        override fun serverTime() = System.currentTimeMillis()
        override fun stop() { stops++ }
    }
    private class FakeAuth : AuthRepository {
        override var currentUser: AuthenticatedUser? = null
        override fun register(credentials: SignUpCredentials, onResult: (RegistrationResult) -> Unit) = Unit
        override fun signIn(credentials: SignInCredentials, onResult: (SignInResult) -> Unit) {
            currentUser = AuthenticatedUser("test", credentials.email)
            onResult(SignInResult.Success(currentUser!!))
        }
        override fun signOut() { currentUser = null }
    }
}
