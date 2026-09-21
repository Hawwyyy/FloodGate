package com.example.floodgate.ledcontrol.presenter

import com.example.floodgate.ledcontrol.model.*

data class LedUiState(
    val data: LedControlState = LedControlState(),
    val device: DeviceStatus = DeviceStatus.UNAVAILABLE,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val sendingCommand: String? = null,
    val error: String? = null
)

interface LedControlView {
    fun render(state: LedUiState)
    fun showSuccess(message: String)
    fun returnToSignIn()
}

interface LedScheduler {
    fun repeat(action: () -> Unit)
    fun stop()
}
