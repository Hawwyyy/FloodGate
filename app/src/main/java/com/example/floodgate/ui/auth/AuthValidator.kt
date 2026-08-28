package com.example.floodgate.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import com.example.floodgate.R

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
    CONFIRM_PASSWORD_REQUIRED(R.string.error_confirm_password_required),
    PASSWORDS_DO_NOT_MATCH(R.string.error_passwords_do_not_match)
}

data class SignUpFieldErrors(
    val firstName: AuthValidationError? = null,
    val lastName: AuthValidationError? = null,
    val email: AuthValidationError? = null,
    val password: AuthValidationError? = null,
    val confirmPassword: AuthValidationError? = null
) {
    val hasErrors: Boolean
        get() = firstName != null || lastName != null || email != null ||
            password != null || confirmPassword != null
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

    private fun validateEmail(email: String): AuthValidationError? = when {
        email.isEmpty() -> AuthValidationError.EMAIL_REQUIRED
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> AuthValidationError.EMAIL_INVALID
        else -> null
    }

    private fun validateNewPassword(password: String): AuthValidationError? = when {
        password.isEmpty() -> AuthValidationError.PASSWORD_REQUIRED
        password.length < MinimumPasswordLength -> AuthValidationError.PASSWORD_TOO_SHORT
        password.none(Char::isDigit) -> AuthValidationError.PASSWORD_MISSING_NUMBER
        else -> null
    }
}
