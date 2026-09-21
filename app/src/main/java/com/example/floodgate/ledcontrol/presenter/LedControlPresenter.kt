package com.example.floodgate.ledcontrol.presenter

import com.example.floodgate.ledcontrol.model.*

class LedControlPresenter(private val repository: LedControlRepository,
                          private val view: LedControlView, private val scheduler: LedScheduler) {
    private var state = LedUiState()
    private var active = false
    private var generation = 0
    private var startedAt = 0L
    private var commandStarted = 0L

    fun start() {
        if (active) return
        active = true
        val session = ++generation
        startedAt = repository.serverTime()
        state = state.copy(loading = true, sending = false, sendingCommand = null, error = null)
        view.render(state)
        repository.observe({ data ->
            if (active && generation == session) {
                state = state.copy(data = data, loading = false, error = null)
                tick()
            }
        }, { error ->
            if (active && generation == session) {
                state = state.copy(loading = false, error = error)
                tick()
            }
        }, { if (active && generation == session) { stop(); view.returnToSignIn() } })
        if (active) scheduler.repeat(::tick)
    }
    fun stop() { active = false; generation++; scheduler.stop(); repository.stop() }
    fun onTurnOnClicked() = send("ON")
    fun onTurnOffClicked() = send("OFF")
    private fun send(command: String) {
        if (!active || state.sending) return
        val session = generation
        commandStarted = repository.serverTime()
        state = state.copy(sending = true, sendingCommand = command, error = null)
        view.render(state)
        repository.send(command) { success ->
            if (active && session == generation) {
                state = state.copy(sending = false, sendingCommand = null, error = if (success) null else "Unable to send command. Please try again.")
                view.render(state)
                if (success) view.showSuccess("$command command sent to ESP32")
            }
        }
    }
    private fun tick() {
        if (!active) return
        val now = repository.serverTime()
        state = state.copy(device = availability(state.data.lastUpdate, now))
        if (state.loading && now - startedAt > 15000) state = state.copy(loading = false,
            error = "Still waiting for Firebase. Check your connection.")
        // Firebase can queue writes offline. Keep duplicate submissions disabled until acknowledgment.
        if (state.sending && now - commandStarted > 15000) state = state.copy(
            error = "Command is pending. It may be delivered when your connection returns.")
        view.render(state)
    }
    companion object {
        fun availability(timestamp: Long?, now: Long): DeviceStatus = when {
            timestamp == null || timestamp <= 0 || timestamp > now + 2000 -> DeviceStatus.UNAVAILABLE
            now - timestamp <= 10000 -> DeviceStatus.ONLINE
            else -> DeviceStatus.OFFLINE
        }
    }
}
