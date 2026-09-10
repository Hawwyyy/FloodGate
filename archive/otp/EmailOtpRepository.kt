// Archived implementation; the active app uses Firebase password-reset email links.
package com.example.floodgate.data.auth

import com.example.floodgate.R
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

data class EmailOtpChallenge(val id: String, val expiresInSeconds: Int, val resendAfterSeconds: Int)

/** Codes are generated and checked only on the server, never in the Android client. */
interface EmailOtpRepository {
    fun send(email: String, callback: (Result<EmailOtpChallenge>) -> Unit)
    fun verify(email: String, challengeId: String, code: String, callback: (Result<Unit>) -> Unit)
}

enum class RecoveryFailure(val messageRes: Int) {
    INVALID_EMAIL(R.string.error_email_invalid),
    INVALID_CODE(R.string.recovery_invalid_code),
    EXPIRED_CODE(R.string.recovery_expired_code),
    TOO_MANY_ATTEMPTS(R.string.recovery_too_many_attempts),
    NETWORK(R.string.error_network),
    UNAVAILABLE(R.string.recovery_unavailable)
}

class RecoveryException(val failure: RecoveryFailure) : Exception()

class FirebaseEmailOtpRepository : EmailOtpRepository {
    private val functions by lazy { FirebaseFunctions.getInstance("us-central1") }

    override fun send(email: String, callback: (Result<EmailOtpChallenge>) -> Unit) {
        functions.getHttpsCallable("sendRecoveryCode").call(mapOf("email" to email))
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    callback(Result.failure(mapFailure(task.exception, sending = true)))
                } else {
                    callback(runCatching {
                        val data = task.result.data as Map<*, *>
                        EmailOtpChallenge(
                            data["challengeId"] as String,
                            (data["expiresInSeconds"] as Number).toInt(),
                            (data["resendAfterSeconds"] as Number).toInt()
                        ).also {
                            require(it.id.isNotBlank() && it.expiresInSeconds > 0 && it.resendAfterSeconds > 0)
                        }
                    })
                }
            }
    }

    override fun verify(email: String, challengeId: String, code: String, callback: (Result<Unit>) -> Unit) {
        functions.getHttpsCallable("verifyRecoveryCode")
            .call(mapOf("email" to email, "challengeId" to challengeId, "code" to code))
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    callback(Result.failure(mapFailure(task.exception)))
                } else {
                    callback(runCatching {
                        check((task.result.data as? Map<*, *>)?.get("verified") == true)
                    })
                }
            }
    }

    private fun mapFailure(error: Exception?, sending: Boolean = false): RecoveryException = RecoveryException(
        when ((error as? FirebaseFunctionsException)?.code) {
            FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                if (sending) RecoveryFailure.INVALID_EMAIL else RecoveryFailure.INVALID_CODE
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> RecoveryFailure.NETWORK
            FirebaseFunctionsException.Code.FAILED_PRECONDITION -> RecoveryFailure.EXPIRED_CODE
            FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> RecoveryFailure.TOO_MANY_ATTEMPTS
            else -> RecoveryFailure.UNAVAILABLE
        }
    )
}

fun recoveryErrorResource(error: Throwable): Int =
    (error as? RecoveryException)?.failure?.messageRes ?: R.string.recovery_request_failed
