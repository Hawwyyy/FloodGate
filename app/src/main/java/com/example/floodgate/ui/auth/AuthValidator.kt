package com.example.floodgate.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import com.example.floodgate.R
import java.util.Locale

data class SignUpCredentials(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)

data class SignInCredentials(
    val email: String,
    val password: String
)

enum class AuthValidationError(@StringRes val messageRes: Int) {
    FIRST_NAME_REQUIRED(R.string.error_first_name_required),
    FIRST_NAME_TOO_SHORT(R.string.error_first_name_too_short),
    FIRST_NAME_TOO_LONG(R.string.error_first_name_too_long),
    LAST_NAME_REQUIRED(R.string.error_last_name_required),
    LAST_NAME_TOO_SHORT(R.string.error_last_name_too_short),
    LAST_NAME_TOO_LONG(R.string.error_last_name_too_long),
    EMAIL_REQUIRED(R.string.error_email_required),
    EMAIL_INVALID(R.string.error_email_invalid),
    PASSWORD_REQUIRED(R.string.error_password_required),
    PASSWORD_TOO_SHORT(R.string.error_password_too_short),
    PASSWORD_MISSING_NUMBER(R.string.error_password_missing_number),
    PASSWORD_MISSING_UPPERCASE(R.string.error_password_missing_uppercase),
    PASSWORD_MISSING_LOWERCASE(R.string.error_password_missing_lowercase),
    PASSWORD_MISSING_SPECIAL_CHARACTER(R.string.error_password_missing_special_character),
    CONFIRM_PASSWORD_REQUIRED(R.string.error_confirm_password_required),
    PASSWORDS_DO_NOT_MATCH(R.string.error_passwords_do_not_match)
}

data class SignUpFieldErrors(
    val firstName: AuthValidationError? = null,
    val lastName: AuthValidationError? = null,
    val email: AuthValidationError? = null,
    val password: List<AuthValidationError> = emptyList(),
    val confirmPassword: AuthValidationError? = null
) {
    val hasErrors: Boolean
        get() = firstName != null || lastName != null || email != null ||
            password.isNotEmpty() || confirmPassword != null
}

data class SignInFieldErrors(
    val email: AuthValidationError? = null,
    val password: AuthValidationError? = null
) {
    val hasErrors: Boolean
        get() = email != null || password != null
}

data class SignUpValidationResult(
    val credentials: SignUpCredentials?,
    val errors: SignUpFieldErrors
)

data class SignInValidationResult(
    val credentials: SignInCredentials?,
    val errors: SignInFieldErrors
)

object AuthValidator {
    private const val MinimumNameLength = 2
    private const val MaximumNameLength = 100
    private const val MinimumPasswordLength = 8

    fun validateSignUp(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): SignUpValidationResult {
        val cleanFirstName = firstName.trim()
        val cleanLastName = lastName.trim()
        val cleanEmail = email.trim()

        val errors = SignUpFieldErrors(
            firstName = validateName(
                value = cleanFirstName,
                requiredError = AuthValidationError.FIRST_NAME_REQUIRED,
                shortError = AuthValidationError.FIRST_NAME_TOO_SHORT,
                longError = AuthValidationError.FIRST_NAME_TOO_LONG
            ),
            lastName = validateName(
                value = cleanLastName,
                requiredError = AuthValidationError.LAST_NAME_REQUIRED,
                shortError = AuthValidationError.LAST_NAME_TOO_SHORT,
                longError = AuthValidationError.LAST_NAME_TOO_LONG
            ),
            email = validateEmail(cleanEmail),
            password = validateNewPassword(password),
            confirmPassword = when {
                confirmPassword.isEmpty() -> AuthValidationError.CONFIRM_PASSWORD_REQUIRED
                confirmPassword != password -> AuthValidationError.PASSWORDS_DO_NOT_MATCH
                else -> null
            }
        )

        val credentials = if (errors.hasErrors) {
            null
        } else {
            SignUpCredentials(
                firstName = cleanFirstName,
                lastName = cleanLastName,
                email = cleanEmail,
                password = password,
                confirmPassword = confirmPassword
            )
        }

        return SignUpValidationResult(credentials = credentials, errors = errors)
    }

    fun validateSignIn(email: String, password: String): SignInValidationResult {
        val cleanEmail = email.trim()
        val errors = SignInFieldErrors(
            email = validateEmail(cleanEmail),
            password = if (password.isEmpty()) {
                AuthValidationError.PASSWORD_REQUIRED
            } else {
                null
            }
        )

        val credentials = if (errors.hasErrors) {
            null
        } else {
            SignInCredentials(email = cleanEmail, password = password)
        }

        return SignInValidationResult(credentials = credentials, errors = errors)
    }

    private fun validateName(
        value: String,
        requiredError: AuthValidationError,
        shortError: AuthValidationError,
        longError: AuthValidationError
    ): AuthValidationError? = when {
        value.isEmpty() -> requiredError
        value.length < MinimumNameLength -> shortError
        value.length > MaximumNameLength -> longError
        else -> null
    }

    // Shared by Sign In, Sign Up and Forgot Password. Syntax alone accepts made-up TLDs.
    fun validateEmail(email: String): AuthValidationError? {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) return AuthValidationError.EMAIL_REQUIRED
        if (cleanEmail.length > 254 || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return AuthValidationError.EMAIL_INVALID
        }
        val localPart = cleanEmail.substringBefore('@')
        val domain = cleanEmail.substringAfter('@')
        if (localPart.length > 64 || localPart.startsWith('.') || localPart.endsWith('.') ||
            ".." in localPart || domain.split('.').any {
                it.isEmpty() || it.length > 63 || it.startsWith('-') || it.endsWith('-')
            }
        ) return AuthValidationError.EMAIL_INVALID

        val topLevelDomain = domain.substringAfterLast('.').lowercase(Locale.ROOT)
        return if (topLevelDomain in EmailTopLevelDomains.recognized) null else AuthValidationError.EMAIL_INVALID
    }

    // Return every missing requirement so the form can explain exactly what to fix.
    fun validateNewPassword(password: String): List<AuthValidationError> = buildList {
        if (password.isEmpty()) add(AuthValidationError.PASSWORD_REQUIRED)
        if (password.length < MinimumPasswordLength) add(AuthValidationError.PASSWORD_TOO_SHORT)
        if (password.none(Char::isUpperCase)) add(AuthValidationError.PASSWORD_MISSING_UPPERCASE)
        if (password.none(Char::isLowerCase)) add(AuthValidationError.PASSWORD_MISSING_LOWERCASE)
        if (password.none(Char::isDigit)) add(AuthValidationError.PASSWORD_MISSING_NUMBER)
        // Whitespace and control characters do not count as special characters.
        if (password.none { !it.isLetterOrDigit() && !it.isWhitespace() && !it.isISOControl() }) {
            add(AuthValidationError.PASSWORD_MISSING_SPECIAL_CHARACTER)
        }
    }
}
