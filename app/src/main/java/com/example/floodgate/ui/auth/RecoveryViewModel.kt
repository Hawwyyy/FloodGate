package com.example.floodgate.ui.auth

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.floodgate.R
import com.example.floodgate.data.auth.FirebasePasswordResetRepository
import com.example.floodgate.data.auth.PasswordResetError
import com.example.floodgate.data.auth.PasswordResetException
import com.example.floodgate.data.auth.PasswordResetRepository

enum class RecoveryStep { EMAIL, CHECK_EMAIL }

class RecoveryViewModel(
    private val repository: PasswordResetRepository = FirebasePasswordResetRepository(),
    private val clock: () -> Long = { SystemClock.elapsedRealtime() }
) : ViewModel() {
    var step by mutableStateOf(RecoveryStep.EMAIL)
        private set
    var email by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var errorRes by mutableStateOf<Int?>(null)
        private set
    var emailError by mutableStateOf(false)
        private set
    var noticeRes by mutableStateOf<Int?>(null)
        private set
    var resendAt by mutableStateOf(0L)
        private set
    private var generation = 0

    fun remainingResendSeconds(): Int =
        ((resendAt - clock() + 999L) / 1000L).coerceAtLeast(0L).toInt()

    fun editEmail(value: String) {
        if (busy || step != RecoveryStep.EMAIL) return
        email = value
        errorRes = null
        emailError = false
    }

    fun sendResetLink() {
        val resending = step == RecoveryStep.CHECK_EMAIL
        if (busy || (resending && remainingResendSeconds() > 0)) return
        val cleanEmail = email.trim()
        val validationError = AuthValidator.validateEmail(cleanEmail)?.messageRes
        errorRes = validationError
        emailError = validationError != null
        noticeRes = null
        if (validationError != null) return

        email = cleanEmail
        busy = true
        if (resending) resendAt = clock() + RESEND_COOLDOWN_MS
        val requestGeneration = ++generation
        repository.sendResetEmail(cleanEmail) { result ->
            // A closed screen or duplicated callback cannot navigate or alter the next request.
            if (requestGeneration == generation && busy) {
                busy = false
                result.fold(onSuccess = {
                    resendAt = clock() + RESEND_COOLDOWN_MS
                    step = RecoveryStep.CHECK_EMAIL
                    if (resending) noticeRes = R.string.reset_resent
                }, onFailure = {
                    errorRes = ((it as? PasswordResetException)?.reason ?: PasswordResetError.UNKNOWN).messageRes
                })
            }
        }
    }

    fun reset() {
        generation++
        busy = false
        step = RecoveryStep.EMAIL
        email = ""
        errorRes = null
        emailError = false
        noticeRes = null
        resendAt = 0L
    }

    override fun onCleared() { generation++ }

    private companion object { const val RESEND_COOLDOWN_MS = 45_000L }
}
