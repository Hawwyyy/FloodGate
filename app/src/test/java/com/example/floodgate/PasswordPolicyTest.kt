package com.example.floodgate

import com.example.floodgate.ui.auth.AuthValidationError
import com.example.floodgate.ui.auth.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {
    @Test
    fun acceptsPasswordAtEightCharacterBoundary() {
        assertTrue(AuthValidator.validateNewPassword("Abcdef1!").isEmpty())
    }

    @Test
    fun identifiesEachMissingRequirementIndividually() {
        val cases = mapOf(
            "Abcde1!" to AuthValidationError.PASSWORD_TOO_SHORT,
            "abcdef1!" to AuthValidationError.PASSWORD_MISSING_UPPERCASE,
            "ABCDEF1!" to AuthValidationError.PASSWORD_MISSING_LOWERCASE,
            "Abcdefg!" to AuthValidationError.PASSWORD_MISSING_NUMBER,
            "Abcdef12" to AuthValidationError.PASSWORD_MISSING_SPECIAL_CHARACTER
        )
        cases.forEach { (password, expected) ->
            assertEquals(listOf(expected), AuthValidator.validateNewPassword(password))
        }
    }

    @Test
    fun reportsAllMissingRequirementsAndClearsThemAsPasswordImproves() {
        assertEquals(
            listOf(
                AuthValidationError.PASSWORD_TOO_SHORT,
                AuthValidationError.PASSWORD_MISSING_UPPERCASE,
                AuthValidationError.PASSWORD_MISSING_NUMBER,
                AuthValidationError.PASSWORD_MISSING_SPECIAL_CHARACTER
            ),
            AuthValidator.validateNewPassword("abc")
        )
        assertEquals(
            listOf(AuthValidationError.PASSWORD_MISSING_SPECIAL_CHARACTER),
            AuthValidator.validateNewPassword("Abcdef12")
        )
        assertTrue(AuthValidator.validateNewPassword("Abcdef12!").isEmpty())
    }

    @Test
    fun whitespaceAndControlCharactersDoNotSatisfySpecialCharacterRule() {
        listOf("Abcdef1 ", "Abcdef1\t", "Abcdef1\n", "Abcdef1\u0000").forEach {
            assertEquals(
                listOf(AuthValidationError.PASSWORD_MISSING_SPECIAL_CHARACTER),
                AuthValidator.validateNewPassword(it)
            )
        }
    }

    @Test
    fun emptyPasswordReportsRequiredAndAllFiveRules() {
        val errors = AuthValidator.validateNewPassword("")
        assertEquals(6, errors.size)
        assertTrue(errors.contains(AuthValidationError.PASSWORD_REQUIRED))
    }
}
