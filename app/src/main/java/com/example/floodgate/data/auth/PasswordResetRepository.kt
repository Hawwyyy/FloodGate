package com.example.floodgate.data.auth

import androidx.annotation.StringRes
import com.example.floodgate.R
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

fun interface PasswordResetRepository {
    fun sendResetEmail(email: String, callback: (Result<Unit>) -> Unit)
}

/** Firebase's hosted reset-link flow: no OTPs, database writes or custom mail server. */
class FirebasePasswordResetRepository(
    private val sendRequest: (String) -> Task<Void> = { email ->
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
    }
) : PasswordResetRepository {
    override fun sendResetEmail(email: String, callback: (Result<Unit>) -> Unit) {
        try {
            sendRequest(email).addOnCompleteListener { task ->
                // Task<Void> has a null result on success. Inspect status, not the result value.
                if (task.isSuccessful) callback(Result.success(Unit))
                else callback(Result.failure(PasswordResetException(mapPasswordResetError(task.exception))))
            }
        } catch (error: Exception) {
            callback(Result.failure(PasswordResetException(mapPasswordResetError(error))))
        }
    }
}

enum class PasswordResetError(@StringRes val messageRes: Int) {
    NETWORK(R.string.reset_error_network),
    INVALID_EMAIL(R.string.reset_error_email),
    TOO_MANY_REQUESTS(R.string.reset_error_rate_limit),
    UNKNOWN(R.string.reset_error_generic)
}

class PasswordResetException(val reason: PasswordResetError) : Exception()

internal fun mapPasswordResetError(error: Exception?): PasswordResetError = when (error) {
    is FirebaseNetworkException -> PasswordResetError.NETWORK
    is FirebaseTooManyRequestsException -> PasswordResetError.TOO_MANY_REQUESTS
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException -> PasswordResetError.INVALID_EMAIL
    else -> PasswordResetError.UNKNOWN
}
