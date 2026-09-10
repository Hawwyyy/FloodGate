package com.example.floodgate

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import com.example.floodgate.ui.theme.FloodGateTheme
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RecoveryFlowInstrumentedTest {
    @get:Rule val ui = createComposeRule()
    private val backend = FakeResetRepository()
    private var time = 1000L
    private val model = RecoveryViewModel(backend) { time }

    private fun launch() {
        ui.setContent {
            var recoveryOpen by remember { mutableStateOf(true) }
            FloodGateTheme {
                if (recoveryOpen) RecoveryFlow(model) { recoveryOpen = false }
                else Text("Sign In destination")
            }
        }
    }
    private fun submitEmail() {
        ui.onNode(hasSetTextAction()).performTextInput("  harrison@gmail.com  ")
        ui.onNodeWithText("Send Reset Link").performClick()
    }
    private fun accepted() {
        ui.runOnIdle { backend.completion!!.invoke(Result.success(Unit)) }
    }

    @Test fun emptyAndInvalidEmailsNeverCallFirebase() {
        launch()
        ui.onNodeWithText("Send Reset Link").performClick()
        ui.onNodeWithText("Please enter your email address.").assertIsDisplayed()
        ui.onNode(hasSetTextAction()).performTextInput("not-an-email")
        ui.onNodeWithText("Send Reset Link").performClick()
        ui.onNodeWithText("Please enter a valid email address.").assertIsDisplayed()
        assertEquals(0, backend.sends)
    }

    @Test fun screenshotAddressShowsFieldErrorAndNeverCallsFirebase() {
        launch()
        ui.onNode(hasSetTextAction()).performTextInput("dhuaiduad@sjadad.sdaju")
        ui.onNodeWithText("Send Reset Link").performClick()
        ui.onNodeWithText("Please enter a valid email address.").assertIsDisplayed()
        ui.onNodeWithText("Check Your Email").assertDoesNotExist()
        assertEquals(0, backend.sends)
        assertEquals(RecoveryStep.EMAIL, model.step)
        assertFalse(model.busy)
    }

    @Test fun trimsEmailAndBlocksDuplicateTapsUntilFirebaseCompletes() {
        launch(); submitEmail()
        ui.onNodeWithTag("reset_send").assertIsNotEnabled()
        ui.runOnIdle { model.sendResetLink(); model.sendResetLink() }
        assertEquals(1, backend.sends)
        assertEquals("harrison@gmail.com", backend.lastEmail)
        ui.onNodeWithText("Check Your Email").assertDoesNotExist()
        accepted()
        ui.onNodeWithText("Check Your Email").assertIsDisplayed()
        ui.onNodeWithText("h******@gmail.com", substring = true).assertIsDisplayed()
        ui.onNodeWithText("harrison@gmail.com", substring = true).assertDoesNotExist()
        ui.onNodeWithTag("recovery_code").assertDoesNotExist()
        ui.onNodeWithText("Verify Code").assertDoesNotExist()
    }

    @Test fun failureRestoresSendButtonAndShowsFriendlyMessage() {
        launch(); submitEmail()
        ui.runOnIdle {
            backend.completion!!(Result.failure(PasswordResetException(PasswordResetError.NETWORK)))
        }
        ui.onNodeWithText("Unable to send reset email. Please check your internet connection and try again.").assertIsDisplayed()
        ui.onNodeWithText("Send Reset Link").assertIsEnabled()
        ui.onNodeWithText("Check Your Email").assertDoesNotExist()
        ui.onNodeWithText("Send Reset Link").performClick()
        assertEquals(2, backend.sends)
    }

    @Test fun resendIsRateLimitedAndSuccessShowsNotice() {
        launch(); submitEmail(); accepted()
        ui.onNodeWithText("Resend available in 00:45").assertIsDisplayed()
        ui.onNodeWithTag("reset_resend").assertIsNotEnabled()
        ui.runOnIdle { model.sendResetLink() }
        assertEquals(1, backend.sends)
        ui.runOnIdle { time += 45_000; model.sendResetLink(); model.sendResetLink() }
        assertEquals(2, backend.sends)
        ui.onNodeWithTag("reset_resend").assertIsNotEnabled()
        accepted()
        ui.onNodeWithText("Password reset email sent again.").assertIsDisplayed()
        ui.onNodeWithText("Check Your Email").assertIsDisplayed()
        assertEquals(45, model.remainingResendSeconds())
    }

    @Test fun resendFailureStaysOnConfirmationAndBecomesRetryableAfterCooldown() {
        launch(); submitEmail(); accepted()
        ui.runOnIdle { time += 45_000; model.sendResetLink() }
        ui.runOnIdle { backend.completion!!(Result.failure(PasswordResetException(PasswordResetError.TOO_MANY_REQUESTS))) }
        ui.onNodeWithText("Too many requests. Please try again later.").assertIsDisplayed()
        ui.onNodeWithText("Check Your Email").assertIsDisplayed()
        ui.runOnIdle { time += 45_000; model.sendResetLink() }
        assertEquals(3, backend.sends)
    }

    @Test fun backToSignInClearsRecoveryAndStaleCallbacksCannotReopenIt() {
        launch(); submitEmail(); accepted()
        ui.runOnIdle { time += 45_000; model.sendResetLink() }
        ui.onNodeWithText("Back to Sign In").performClick()
        accepted() // This in-flight resend must not restore the dismissed screen.
        ui.onNodeWithText("Sign In destination").assertIsDisplayed()
        ui.onNodeWithText("Check Your Email").assertDoesNotExist()
        assertEquals(RecoveryStep.EMAIL, model.step)
        assertEquals("", model.email)
    }

    @Test fun backArrowReturnsToSignInDuringInitialSend() {
        launch(); submitEmail()
        ui.onNodeWithContentDescription("Navigate back").performClick()
        accepted()
        ui.onNodeWithText("Sign In destination").assertIsDisplayed()
        assertFalse(model.busy)
    }

    @Test fun emptyStoredEmailCannotBeResent() {
        model.reset(); model.sendResetLink()
        assertEquals(0, backend.sends)
        assertEquals(R.string.error_email_required, model.errorRes)
    }

    @Test fun existingSignInNavigationOpensResetAndReturnsWithoutOtp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val isolatedApp = FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, "reset-navigation-test")
        try {
            // Separate local Firebase instance: never alter the user's existing sign-in session.
            val authRepository = FirebaseAuthRepository(FirebaseAuth.getInstance(isolatedApp))
            ui.setContent { FloodGateTheme { FloodGateAuthApp({}, authRepository, model) } }
            ui.onNodeWithText("Forgot Password?").performClick()
            ui.onNodeWithText("Send Reset Link").assertIsDisplayed()
            submitEmail(); accepted()
            ui.onNodeWithText("Back to Sign In").performClick()
            ui.onNodeWithText("Great to have you back!").assertIsDisplayed()
            ui.onNodeWithText("Check Your Email").assertDoesNotExist()
            ui.onNodeWithText("Forgot Password?").performClick()
            ui.onNodeWithText("Send Reset Link").assertIsDisplayed()
            ui.onNodeWithTag("recovery_code").assertDoesNotExist()
        } finally { isolatedApp.delete() }
    }

    @Test fun recreationRetainsConfirmationAndDoesNotSendAgain() {
        val restoration = StateRestorationTester(ui)
        restoration.setContent { FloodGateTheme { RecoveryFlow(model) {} } }
        submitEmail(); accepted()
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Check Your Email").assertIsDisplayed()
        ui.onNodeWithTag("reset_resend").assertIsNotEnabled()
        assertEquals(1, backend.sends)
    }

    @Test fun smallPhoneWithLargeTextKeepsConfirmationControlsReachable() {
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                FloodGateTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp)) {
                        RecoveryScreen(RecoveryStep.CHECK_EMAIL, "harrison@gmail.com")
                    }
                }
            }
        }
        ui.onNodeWithText("Back to Sign In").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Resend Email").performScrollTo().assertIsDisplayed()
        capture("check-email-small")
    }

    @Test fun captureForgotAndConfirmationDesigns() {
        launch(); capture("forgot-password")
        submitEmail(); accepted(); capture("check-email")
    }

    @Test fun firebaseAdapterUsesVoidTaskStatusAndMapsErrorsWithoutRawMessages() {
        val request = TaskCompletionSource<Void>()
        var sentEmail = ""
        var result: Result<Unit>? = null
        val repository = FirebasePasswordResetRepository { email -> sentEmail = email; request.task }
        repository.sendResetEmail("user@example.com") { result = it }
        assertEquals("user@example.com", sentEmail)
        assertNull(result)
        request.setResult(null)
        ui.waitUntil { result != null }
        assertTrue(result!!.isSuccess)

        assertEquals(PasswordResetError.NETWORK, mapPasswordResetError(FirebaseNetworkException("private detail")))
        assertEquals(PasswordResetError.TOO_MANY_REQUESTS, mapPasswordResetError(FirebaseTooManyRequestsException("private detail")))
        assertEquals(PasswordResetError.INVALID_EMAIL, mapPasswordResetError(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "private detail")))
        assertEquals(PasswordResetError.INVALID_EMAIL, mapPasswordResetError(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "private detail")))
        assertEquals(PasswordResetError.UNKNOWN, mapPasswordResetError(IllegalStateException("private detail")))

        val failed = TaskCompletionSource<Void>()
        var error: Throwable? = null
        FirebasePasswordResetRepository { failed.task }.sendResetEmail("user@example.com") { error = it.exceptionOrNull() }
        failed.setException(FirebaseNetworkException("private detail"))
        ui.waitUntil { error != null }
        assertEquals(PasswordResetError.NETWORK, (error as PasswordResetException).reason)
        assertFalse(error!!.toString().contains("private detail"))
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "reset-link-test-screenshots").apply { mkdirs() }
        val bitmap = ui.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private class FakeResetRepository : PasswordResetRepository {
        var sends = 0
        var lastEmail = ""
        var completion: ((Result<Unit>) -> Unit)? = null
        override fun sendResetEmail(email: String, callback: (Result<Unit>) -> Unit) {
            sends++; lastEmail = email; completion = callback
        }
    }
}
