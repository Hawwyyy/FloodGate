package com.example.floodgate.barrier.presenter

import com.example.floodgate.barrier.model.*
import java.util.UUID

data class WaterReading(val timestamp: Long, val percent: Double)
enum class Freshness { UNAVAILABLE, ONLINE, STALE }
data class BarrierUiState(
    val telemetry: BarrierTelemetry = BarrierTelemetry(),
    val connected: Boolean = false,
    val freshness: Freshness = Freshness.UNAVAILABLE,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val error: String? = null,
    val feedback: String? = null,
    val readings: List<WaterReading> = emptyList()
) {
    val controlsEnabled get() = connected && freshness == Freshness.ONLINE && !loading && !sending && error == null
}

interface BarrierView {
    fun render(state: BarrierUiState)
    fun signedOut()
}
interface BarrierTimer {
    fun start(tick: () -> Unit)
    fun stop()
}

class BarrierPresenter(
    private val repository: BarrierRepository,
    private val view: BarrierView,
    private val timer: BarrierTimer,
    private val monotonicTime: () -> Long,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private var state = BarrierUiState()
    private var active = false
    private var observation = 0
    private var session = 0
    private var started = 0L
    private var sentAt = 0L
    private var waitingSince = 0L
    private var baseline = 0L
    private var target: BarrierAction? = null
    private var lastChartTime = 0L

    fun start() {
        if (active) return
        active = true
        val generation = ++observation
        started = monotonicTime()
        state = state.copy(connected = false, loading = true, error = null)
        render()
        repository.observe({ telemetry ->
            if (active && observation == generation) {
                state = state.copy(telemetry = telemetry, loading = false)
                val time = telemetry.updatedAt
                if (time != null && time > lastChartTime && time <= repository.serverTime() + 2000) {
                    lastChartTime = time
                    telemetry.percent?.let { percent ->
                        state = state.copy(readings = (state.readings + WaterReading(time, percent)).takeLast(20))
                    }
                }
                tick()
            }
        }, { connected ->
            if (active && observation == generation) { state = state.copy(connected = connected); tick() }
        }, { error ->
            if (active && observation == generation) {
                state = state.copy(loading = false, error = errorMessage(error)); render()
            }
        }, { if (active && observation == generation) { clear(); view.signedOut() } })
        if (active) timer.start(::tick)
    }

    fun stop() {
        active = false; observation++
        timer.stop(); repository.stop()
        state = state.copy(connected = false)
    }

    fun clear() {
        stop(); session++
        state = BarrierUiState(); target = null; lastChartTime = 0L
        view.render(state)
    }

    fun retry() { stop(); start() }

    fun send(action: BarrierAction) {
        tick() // Recheck freshness when confirming an already-open dialog.
        if (!active || !state.controlsEnabled) return
        val currentSession = session
        target = action
        baseline = state.telemetry.updatedAt ?: 0L
        sentAt = monotonicTime()
        state = state.copy(sending = true, feedback = "Sending command…", error = null)
        render()
        repository.send(action, newId()) { error ->
            if (session == currentSession) {
                waitingSince = monotonicTime()
                state = state.copy(sending = false,
                    error = error?.let(::errorMessage),
                    feedback = if (error == null) "Command sent. Waiting for device state." else null)
                if (error != null) target = null
                // The next telemetry callback may report a matching state. This is not an ID acknowledgment.
                render()
            }
        }
    }

    private fun tick() {
        if (!active) return
        val t = state.telemetry
        val age = t.updatedAt?.let { repository.serverTime() - it }
        val freshness = when {
            !t.hasTelemetry || age == null || age < -2000 -> Freshness.UNAVAILABLE
            age > 15000 -> Freshness.STALE
            else -> Freshness.ONLINE
        }
        state = state.copy(freshness = freshness)
        if (state.loading && monotonicTime() - started >= 15000) {
            state = state.copy(loading = false, error = "Telemetry has not loaded. Check your connection and retry.")
        }
        if (state.sending && monotonicTime() - sentAt >= 15000) {
            state = state.copy(feedback = "Write is still pending. It may complete after reconnection; this timeout does not cancel it.")
        }
        val requested = target
        if (!state.sending && requested != null) {
            if (freshness == Freshness.ONLINE && (t.updatedAt ?: 0L) > baseline && t.barrier == requested.reported) {
                state = state.copy(feedback = if (requested == BarrierAction.DEPLOY)
                    "Device reports deployed." else "Device reports retracted.")
                target = null
            } else if (monotonicTime() - waitingSince >= 15000) {
                state = state.copy(feedback = "No matching device state reported yet. The command was sent; physical movement is unverified.")
            }
        }
        render()
    }

    private fun render() { if (active) view.render(state) }
    private fun errorMessage(error: BarrierError) = when (error) {
        BarrierError.PERMISSION -> "Access denied. Check that your signed-in account has device and command permissions."
        BarrierError.NETWORK -> "Unable to reach Firebase. Check your internet connection and retry."
        BarrierError.OTHER -> "Firebase could not complete the operation. Please retry."
    }
}
