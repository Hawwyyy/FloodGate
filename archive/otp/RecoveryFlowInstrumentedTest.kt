// Archived OTP tests; not part of the active Android test suite.
package com.example.floodgate

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.floodgate.data.auth.*
import com.example.floodgate.ui.auth.*
import com.example.floodgate.ui.theme.FloodGateTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RecoveryFlowInstrumentedTest {
    @get:Rule val ui = createComposeRule()
    private val backend = FakeOtpRepository()
    private val model = RecoveryViewModel(backend)

    private fun launch() {
        ui.setContent { FloodGateTheme { RecoveryFlow(model) {} } }
    }
    private fun enterEmail() {
        ui.onNode(hasSetTextAction()).performTextInput("user@example.com")
        ui.onNodeWithText("Send Verification Code").performClick()
    }
    private fun delivered() {
        ui.runOnIdle { backend.sent!!.invoke(Result.success(EmailOtpChallenge("test-challenge", 600, 60))) }
    }

    @Test fun invalidEmailNeverCallsServer() {
        launch()
        ui.onNode(hasSetTextAction()).performTextInput("not-an-email")
        ui.onNodeWithText("Send Verification Code").performClick()
        ui.onNodeWithText("Please enter a valid email address.").assertIsDisplayed()
        assertEquals(0, backend.sends)
    }

    @Test fun failedDeliveryStaysOnEmailScreenAndAllowsRetry() {
        launch(); enterEmail()
        ui.runOnIdle { backend.sent!!.invoke(Result.failure(RecoveryException(RecoveryFailure.UNAVAILABLE))) }
        ui.onNodeWithText("Enter your email").assertIsDisplayed()
        ui.onNodeWithText("Send Verification Code").assertIsEnabled()
        ui.onNodeWithText("Verify your Email").assertDoesNotExist()
    }

    @Test fun duplicateSubmissionsBlockedAndResendWaitsForCooldown() {
        launch(); enterEmail()
        ui.runOnIdle { model.sendCode(); model.sendCode() }
        assertEquals(1, backend.sends)
        delivered()
        ui.onNodeWithText("Verify your Email").assertIsDisplayed()
        ui.runOnIdle { model.sendCode() }
        assertEquals(1, backend.sends)
        ui.onNodeWithTag("recovery_code").performTextInput("123456")
        ui.onNodeWithText("Verify Code").performClick()
        ui.runOnIdle { model.verifyCode() }
        assertEquals(1, backend.verifications)
    }

    @Test fun incompleteAndWrongCodeNeverShowSuccess() {
        launch(); enterEmail(); delivered()
        ui.onNodeWithTag("recovery_code").performTextInput("12")
        ui.onNodeWithText("Verify Code").performClick()
        ui.onNodeWithText("Please enter the 6-digit verification code.").assertIsDisplayed()
        assertEquals(0, backend.verifications)
        ui.onNodeWithTag("recovery_code").performTextReplacement("123456")
        ui.onNodeWithText("Verify Code").performClick()
        ui.runOnIdle { backend.verified!!.invoke(Result.failure(RecoveryException(RecoveryFailure.INVALID_CODE))) }
        ui.onNodeWithText("Incorrect verification code. Please try again.").assertIsDisplayed()
        ui.onNodeWithText("Email Verified!").assertDoesNotExist()
        ui.onNodeWithText("Verify Code").assertIsEnabled()
    }

    @Test fun pasteFiltersInputAndOnlyServerSuccessOpensVerifiedScreen() {
        launch(); capture("recovery-email")
        enterEmail(); delivered(); capture("recovery-code")
        ui.onNodeWithTag("recovery_code").performTextInput("1a2 34567")
        ui.runOnIdle { assertEquals("123456", model.code) }
        ui.onNodeWithTag("recovery_code").performTextReplacement("12345")
        ui.runOnIdle { assertEquals("12345", model.code) }
        ui.onNodeWithTag("recovery_code").performTextReplacement("123456")
        ui.onNodeWithText("Verify Code").performClick()
        ui.onNodeWithText("Email Verified!").assertDoesNotExist()
        ui.runOnIdle { backend.verified!!.invoke(Result.success(Unit)) }
        ui.onNodeWithText("Email Verified!").assertIsDisplayed()
        ui.onNodeWithText("Successfully logged in!").assertDoesNotExist()
        ui.runOnIdle { assertEquals("", model.code) }
        capture("recovery-verified")
    }

    @Test fun abandonedRequestCannotNavigateAndExpiryDoesNotCallServer() {
        var time = 1000L
        val local = RecoveryViewModel(backend) { time }
        local.editEmail("user@example.com"); local.sendCode()
        val staleCallback = backend.sent!!
        local.reset()
        staleCallback(Result.success(EmailOtpChallenge("stale", 1, 1)))
        assertEquals(RecoveryStep.EMAIL, local.step)
        local.editEmail("user@example.com"); local.sendCode()
        backend.sent!!(Result.success(EmailOtpChallenge("new", 1, 1)))
        local.editCode("123456"); time += 1001; local.verifyCode()
        assertEquals(R.string.recovery_expired_code, local.errorRes)
        assertEquals(0, backend.verifications)
    }

    @Test fun smallPhoneWithLargeTextKeepsControlsReachable() {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                FloodGateTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        RecoveryScreen(RecoveryStep.CODE, "user@example.com")
                    }
                }
            }
        }
        ui.onNodeWithText("Verify Code").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Resend Code").performScrollTo().assertIsDisplayed()
        capture("recovery-small-large-text")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "recovery-test-screenshots").apply { mkdirs() }
        val bitmap = ui.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private class FakeOtpRepository : EmailOtpRepository {
        var sends = 0
        var verifications = 0
        var sent: ((Result<EmailOtpChallenge>) -> Unit)? = null
        var verified: ((Result<Unit>) -> Unit)? = null
        override fun send(email: String, callback: (Result<EmailOtpChallenge>) -> Unit) { sends++; sent = callback }
        override fun verify(email: String, challengeId: String, code: String, callback: (Result<Unit>) -> Unit) {
            verifications++; verified = callback
        }
    }
}
