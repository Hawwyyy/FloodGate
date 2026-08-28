package com.example.floodgate.data.auth

import androidx.annotation.StringRes
import com.example.floodgate.R
import com.example.floodgate.ui.auth.SignInCredentials
import com.example.floodgate.ui.auth.SignUpCredentials
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

data class AuthenticatedUser(
    val uid: String,
    val email: String
)

enum class AuthServiceError(@StringRes val messageRes: Int) {
    ACCOUNT_EXISTS(R.string.error_account_exists),
    INCORRECT_CREDENTIALS(R.string.error_incorrect_credentials),
    NETWORK(R.string.error_network),
    TOO_MANY_REQUESTS(R.string.error_too_many_requests),
    DATABASE_NOT_CONFIGURED(R.string.error_database_not_configured),
    PROFILE_SAVE_FAILED(R.string.error_profile_save_failed),
    REGISTRATION_FAILED(R.string.error_registration_failed),
    SIGN_IN_FAILED(R.string.error_sign_in_failed)
}

sealed interface RegistrationResult {
    data class Success(val uid: String) : RegistrationResult
    data class Failure(val error: AuthServiceError) : RegistrationResult
}

sealed interface SignInResult {
    data class Success(val user: AuthenticatedUser) : SignInResult
    data class Failure(val error: AuthServiceError) : SignInResult
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private var operationInProgress = false

    val currentUser: AuthenticatedUser?
        get() = auth.currentUser?.let(::toAuthenticatedUser)

    fun register(
        credentials: SignUpCredentials,
        onResult: (RegistrationResult) -> Unit
    ) {
        if (operationInProgress) return
        operationInProgress = true

        auth.createUserWithEmailAndPassword(credentials.email, credentials.password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    finishRegistration(
                        RegistrationResult.Failure(mapRegistrationError(task.exception)),
                        onResult
                    )
                    return@addOnCompleteListener
                }

                val user = task.result?.user
                if (user == null) {
                    finishRegistration(
                        RegistrationResult.Failure(AuthServiceError.REGISTRATION_FAILED),
                        onResult
                    )
                    return@addOnCompleteListener
                }

                saveProfile(user, credentials, onResult)
            }
    }

    fun signIn(
        credentials: SignInCredentials,
        onResult: (SignInResult) -> Unit
    ) {
        if (operationInProgress) return
        operationInProgress = true

        auth.signInWithEmailAndPassword(credentials.email, credentials.password)
            .addOnCompleteListener { task ->
                operationInProgress = false

                if (!task.isSuccessful) {
                    onResult(SignInResult.Failure(mapSignInError(task.exception)))
                    return@addOnCompleteListener
                }

                val user = task.result?.user
                if (user != null) {
                    onResult(SignInResult.Success(toAuthenticatedUser(user)))
                } else {
                    onResult(SignInResult.Failure(AuthServiceError.SIGN_IN_FAILED))
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun saveProfile(
        user: FirebaseUser,
        credentials: SignUpCredentials,
        onResult: (RegistrationResult) -> Unit
    ) {
        val database = try {
            FirebaseDatabase.getInstance()
        } catch (_: RuntimeException) {
            rollbackCreatedUser(
                user = user,
                error = AuthServiceError.DATABASE_NOT_CONFIGURED,
                onResult = onResult
            )
            return
        }

        // Passwords and confirmation values are deliberately excluded.
        val profile = mapOf(
            "firstName" to credentials.firstName,
            "lastName" to credentials.lastName,
            "email" to (user.email ?: credentials.email),
            "createdAt" to ServerValue.TIMESTAMP
        )

        database.reference
            .child("users")
            .child(user.uid)
            .setValue(profile)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = user.uid
                    // Account creation signs the user in automatically. The product flow
                    // requires an explicit sign-in after registration.
                    auth.signOut()
                    finishRegistration(RegistrationResult.Success(uid), onResult)
                } else {
                    rollbackCreatedUser(
                        user = user,
                        error = AuthServiceError.PROFILE_SAVE_FAILED,
                        onResult = onResult
                    )
                }
            }
    }

    private fun rollbackCreatedUser(
        user: FirebaseUser,
        error: AuthServiceError,
        onResult: (RegistrationResult) -> Unit
    ) {
        user.delete().addOnCompleteListener {
            auth.signOut()
            finishRegistration(RegistrationResult.Failure(error), onResult)
        }
    }

    private fun finishRegistration(
        result: RegistrationResult,
        onResult: (RegistrationResult) -> Unit
    ) {
        operationInProgress = false
        onResult(result)
    }

    private fun mapRegistrationError(exception: Exception?): AuthServiceError = when (exception) {
        is FirebaseAuthUserCollisionException -> AuthServiceError.ACCOUNT_EXISTS
        is FirebaseAuthWeakPasswordException -> AuthServiceError.REGISTRATION_FAILED
        is FirebaseNetworkException -> AuthServiceError.NETWORK
        is FirebaseTooManyRequestsException -> AuthServiceError.TOO_MANY_REQUESTS
        else -> AuthServiceError.REGISTRATION_FAILED
    }

    private fun mapSignInError(exception: Exception?): AuthServiceError = when (exception) {
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> AuthServiceError.INCORRECT_CREDENTIALS
        is FirebaseNetworkException -> AuthServiceError.NETWORK
        is FirebaseTooManyRequestsException -> AuthServiceError.TOO_MANY_REQUESTS
        is FirebaseAuthException -> AuthServiceError.INCORRECT_CREDENTIALS
        else -> AuthServiceError.SIGN_IN_FAILED
    }

    private fun toAuthenticatedUser(user: FirebaseUser): AuthenticatedUser = AuthenticatedUser(
        uid = user.uid,
        email = user.email.orEmpty()
    )
}
