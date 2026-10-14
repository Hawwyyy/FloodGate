package com.example.floodgate

import com.example.floodgate.barrier.model.*
import com.example.floodgate.barrier.presenter.*
import org.junit.Assert.*
import org.junit.Test

class BarrierPresenterTest {
    private class Repo : BarrierRepository {
        var now = 100000L
        lateinit var telemetry: (BarrierTelemetry) -> Unit
        lateinit var connection: (Boolean) -> Unit
        lateinit var error: (BarrierError) -> Unit
        lateinit var signedOut: () -> Unit
        lateinit var result: (BarrierError?) -> Unit
        val writes = mutableListOf<Map<String, Any>>()
        var stops = 0
        override fun observe(onTelemetry: (BarrierTelemetry) -> Unit, onConnection: (Boolean) -> Unit,
                             onError: (BarrierError) -> Unit, onSignedOut: () -> Unit) {
            telemetry = onTelemetry; connection = onConnection; error = onError; signedOut = onSignedOut
        }
        override fun send(action: BarrierAction, id: String, complete: (BarrierError?) -> Unit) {
            writes.add(action.children(id)); result = complete
        }
        override fun serverTime() = now
        override fun stop() { stops++ }
    }
    private class Timer : BarrierTimer {
        lateinit var tick: () -> Unit
        override fun start(tick: () -> Unit) { this.tick = tick }
        override fun stop() {}
    }
    private class View : BarrierView {
        var state = BarrierUiState()
        var loggedOut = false
        override fun render(state: BarrierUiState) { this.state = state }
        override fun signedOut() { loggedOut = true }
    }
    private class Fixture {
        val repo = Repo(); val timer = Timer(); val view = View()
        var time = 0L
        val presenter = BarrierPresenter(repo, view, timer, { time })
        init {
            presenter.start()
            repo.connection(true)
            repo.telemetry(BarrierTelemetry(percent = 31.84, raw = 3200, barrier = "UNDEPLOYED", updatedAt = repo.now))
        }
    }
    @Test fun uniqueAtomicCommandsNeverChangeReportedState() {
        val f = Fixture()
        f.presenter.send(BarrierAction.DEPLOY)
        f.presenter.send(BarrierAction.RETRACT)
        assertEquals(1, f.repo.writes.size)
        assertEquals(setOf("barrier", "id"), f.repo.writes[0].keys)
        assertEquals("DEPLOY", f.repo.writes[0]["barrier"])
        f.repo.result(null)
        assertEquals("UNDEPLOYED", f.view.state.telemetry.barrier)
        assertEquals("Command sent. Waiting for device state.", f.view.state.feedback)
        f.presenter.send(BarrierAction.DEPLOY)
        assertNotEquals(f.repo.writes[0]["id"], f.repo.writes[1]["id"])
        f.repo.result(null)
        f.repo.now++
        f.repo.telemetry(f.view.state.telemetry.copy(barrier = "DEPLOYED", updatedAt = f.repo.now))
        assertEquals("Device reports deployed.", f.view.state.feedback)
    }
    @Test fun commandOnlyEventsDoNotRefreshChartOrFreshness() {
        val f = Fixture()
        val t = f.view.state.telemetry
        repeat(30) { f.repo.telemetry(t) }
        assertEquals(1, f.view.state.readings.size)
        f.repo.now += 15001
        f.timer.tick()
        assertEquals(Freshness.STALE, f.view.state.freshness)
        assertFalse(f.view.state.controlsEnabled)
        f.presenter.send(BarrierAction.DEPLOY)
        assertTrue(f.repo.writes.isEmpty())
        f.repo.telemetry(t.copy(updatedAt = f.repo.now))
        assertTrue(f.view.state.controlsEnabled)
        repeat(25) { f.repo.now++; f.repo.telemetry(t.copy(updatedAt = f.repo.now)) }
        assertEquals(20, f.view.state.readings.size)
    }
    @Test fun disconnectPendingTimeoutAndLateCompletionAreHonest() {
        val f = Fixture()
        f.presenter.send(BarrierAction.RETRACT)
        f.repo.connection(false)
        f.time += 16000
        f.timer.tick()
        assertTrue(f.view.state.sending)
        assertTrue(f.view.state.feedback!!.contains("does not cancel"))
        assertFalse(f.view.state.controlsEnabled)
        f.repo.result(null)
        assertFalse(f.view.state.sending)
        assertEquals("Command sent. Waiting for device state.", f.view.state.feedback)
        f.repo.connection(true)
        assertTrue(f.view.state.controlsEnabled)
    }
    @Test fun permissionErrorAndSignOutClearStateAndIgnoreLateWrites() {
        val f = Fixture()
        f.repo.error(BarrierError.PERMISSION)
        assertFalse(f.view.state.controlsEnabled)
        assertTrue(f.view.state.error!!.contains("Access denied"))
        f.presenter.retry()
        f.repo.connection(true)
        f.repo.telemetry(BarrierTelemetry(percent = 20.0, updatedAt = f.repo.now))
        f.presenter.send(BarrierAction.DEPLOY)
        f.repo.signedOut()
        assertTrue(f.view.loggedOut)
        assertTrue(f.view.state.readings.isEmpty())
        assertEquals(BarrierTelemetry(), f.view.state.telemetry)
        f.repo.result(null)
        assertNull(f.view.state.feedback)
        assertTrue(f.repo.stops > 0)
    }
    @Test fun stopResumeRetainsMemoryButRejectsOldObserver() {
        val f = Fixture()
        val old = f.repo.telemetry
        f.presenter.stop()
        old(BarrierTelemetry(percent = 99.0, updatedAt = 100001))
        f.presenter.start()
        assertEquals(1, f.view.state.readings.size)
        assertFalse(f.view.state.controlsEnabled)
    }
    @Test fun missingAndFutureTelemetryCannotEnableCommands() {
        val f = Fixture()
        f.repo.telemetry(BarrierTelemetry())
        assertEquals(Freshness.UNAVAILABLE, f.view.state.freshness)
        assertFalse(f.view.state.controlsEnabled)
        f.repo.telemetry(BarrierTelemetry(percent = 1.0, updatedAt = f.repo.now + 90000))
        assertEquals(Freshness.UNAVAILABLE, f.view.state.freshness)
    }
    @Test fun parserUsesCalibrationAndKeepsFloodSeparateFromOutputs() {
        val parsed = BarrierTelemetry.parse(mapOf(
            "sensor" to mapOf("waterLevel" to 3200L, "waterLevelPercent" to 31.84,
                "status" to "FLOOD CONFIRMED", "highWater" to true),
            "alerts" to mapOf("ledActive" to false, "buzzerActive" to false, "autoSuppressed" to true),
            "barrier" to mapOf("state" to "UNDEPLOYED", "source" to "SWITCH"), "updatedAt" to 100000L))
        assertEquals(31.84, parsed.percent!!, 0.001)
        assertEquals("FLOOD CONFIRMED", parsed.waterStatus)
        assertEquals(false, parsed.led)
        assertEquals("SWITCH", parsed.source)
        assertNull(BarrierTelemetry.parse(mapOf("sensor" to mapOf("waterLevel" to 3200))).percent)
        val invalid = BarrierTelemetry.parse(mapOf("sensor" to mapOf("waterLevelPercent" to Double.NaN,
            "waterLevel" to 5000, "status" to "garbage", "highWater" to "true"), "updatedAt" to "wrong"))
        assertEquals(BarrierTelemetry(), invalid)
    }
}
