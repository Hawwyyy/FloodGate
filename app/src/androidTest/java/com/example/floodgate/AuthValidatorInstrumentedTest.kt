package com.example.floodgate

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.floodgate.ui.auth.AuthValidationError
import com.example.floodgate.ui.auth.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthValidatorInstrumentedTest {

    @Test
    fun signUp_trimsNamesAndEmail_beforeReturningCredentials() {
        val result = AuthValidator.validateSignUp(
            firstName = "  Ada  ",
            lastName = "  Lovelace ",
            email = "  user@gmail.com ",
            password = "password1",
            confirmPassword = "password1"
        )

        assertFalse(result.errors.hasErrors)
        assertNotNull(result.credentials)
        assertEquals("Ada", result.credentials?.firstName)
        assertEquals("Lovelace", result.credentials?.lastName)
        assertEquals("user@gmail.com", result.credentials?.email)
    }

    @Test
    fun signUp_rejectsEmptyAndShortNames() {
        val result = AuthValidator.validateSignUp(
            firstName = " ",
            lastName = "A",
            email = "user@gmail.com",
            password = "password1",
            confirmPassword = "password1"
        )

        assertEquals(AuthValidationError.FIRST_NAME_REQUIRED, result.errors.firstName)
        assertEquals(AuthValidationError.LAST_NAME_TOO_SHORT, result.errors.lastName)
        assertNull(result.credentials)
    }

    @Test
    fun signUp_rejectsInvalidEmail() {
        val result = AuthValidator.validateSignUp(
            firstName = "Ada",
            lastName = "Lovelace",
            email = "not-an-email",
            password = "password1",
            confirmPassword = "password1"
        )

        assertEquals(AuthValidationError.EMAIL_INVALID, result.errors.email)
        assertNull(result.credentials)
    }

    @Test
    fun signUp_rejectsShortPassword() {
        val result = AuthValidator.validateSignUp(
            firstName = "Ada",
            lastName = "Lovelace",
            email = "user@gmail.com",
            password = "pass1",
            confirmPassword = "pass1"
        )

        assertEquals(AuthValidationError.PASSWORD_TOO_SHORT, result.errors.password)
        assertNull(result.credentials)
    }

    @Test
    fun signUp_rejectsPasswordWithoutNumber() {
        val result = AuthValidator.validateSignUp(
            firstName = "Ada",
            lastName = "Lovelace",
            email = "user@gmail.com",
            password = "password",
            confirmPassword = "password"
        )

        assertEquals(AuthValidationError.PASSWORD_MISSING_NUMBER, result.errors.password)
        assertNull(result.credentials)
    }

    @Test
    fun signUp_rejectsMismatchedConfirmation() {
        val result = AuthValidator.validateSignUp(
            firstName = "Ada",
            lastName = "Lovelace",
            email = "user@gmail.com",
            password = "password1",
            confirmPassword = "password2"
        )

        assertEquals(
            AuthValidationError.PASSWORDS_DO_NOT_MATCH,
            result.errors.confirmPassword
        )
        assertNull(result.credentials)
    }

    @Test
    fun signIn_requiresValidEmailAndNonEmptyPassword() {
        val result = AuthValidator.validateSignIn(
            email = "invalid",
            password = ""
        )

        assertEquals(AuthValidationError.EMAIL_INVALID, result.errors.email)
        assertEquals(AuthValidationError.PASSWORD_REQUIRED, result.errors.password)
        assertNull(result.credentials)
    }
}
