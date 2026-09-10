// Archived OTP state management; not included in the app.
package com.example.floodgate.ui.auth

import android.os.SystemClock
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.floodgate.R
import com.example.floodgate.data.auth.EmailOtpChallenge
import com.example.floodgate.data.auth.EmailOtpRepository
import com.example.floodgate.data.auth.FirebaseEmailOtpRepository
import com.example.floodgate.data.auth.recoveryErrorResource

enum class RecoveryStep { EMAIL, CODE, VERIFIED }

class RecoveryViewModel(
    private val repository: EmailOtpRepository = FirebaseEmailOtpRepository(),
    private val clock: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {
    var step by mutableStateOf(RecoveryStep.EMAIL)
        private set
    var email by mutableStateOf("")
        private set
    var code by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var errorRes by mutableStateOf<Int?>(null)
        private set
    var emailError by mutableStateOf(false)
        private set
    var resendAt by mutableStateOf(0L)
        private set
    private var expiresAt = 0L
    private var challenge: EmailOtpChallenge? = null
    private var generation = 0

    fun editEmail(value: String) {
        if (busy) return
        email = value
        errorRes = null
        emailError = false
    }

    fun editCode(value: String) {
        if (busy) return
        code = value.filter { it in '0'..'9' }.take(6)
        errorRes = null
    }

    fun sendCode() {
        if (busy || (step == RecoveryStep.CODE && clock() < resendAt)) return
        val cleanEmail = email.trim()
        if (cleanEmail.length > 254 || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            emailError = true
            errorRes = R.string.error_email_invalid
            return
        }
        email = cleanEmail
        busy = true
        errorRes = null
        val requestGeneration = generation
        repository.send(email) { result ->
            if (generation == requestGeneration) {
                busy = false
                result.fold(onSuccess = {
                    challenge = it
                    expiresAt = clock() + it.expiresInSeconds * 1000L
                    resendAt = clock() + it.resendAfterSeconds * 1000L
                    code = ""
                    step = RecoveryStep.CODE
                }, onFailure = { errorRes = recoveryErrorResource(it) })
            }
        }
    }

    fun verifyCode() {
        if (busy) return
        val currentChallenge = challenge ?: return
        if (code.length != 6) {
            errorRes = R.string.recovery_code_required
            return
        }
        if (clock() >= expiresAt) {
            errorRes = R.string.recovery_expired_code
            return
        }
        busy = true
        errorRes = null
        val requestGeneration = generation
        repository.verify(email, currentChallenge.id, code) { result ->
            if (generation == requestGeneration) {
                busy = false
                result.fold(onSuccess = {
                    code = ""
                    challenge = null
                    step = RecoveryStep.VERIFIED
                }, onFailure = { errorRes = recoveryErrorResource(it) })
            }
        }
    }

    fun backToEmail() {
        generation++
        busy = false
        step = RecoveryStep.EMAIL
        code = ""
        challenge = null
        errorRes = null
        emailError = false
    }

    fun reset() {
        backToEmail()
        email = ""
    }

    override fun onCleared() { generation++ }
}
