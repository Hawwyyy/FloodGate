package com.example.floodgate

import com.example.floodgate.ledcontrol.model.*
import com.example.floodgate.ledcontrol.presenter.*
import org.junit.Assert.*
import org.junit.Test

class LedControlPresenterTest {
    private class Repo : LedControlRepository {
        var now = 100000L
        lateinit var update: (LedControlState) -> Unit
        lateinit var result: (Boolean) -> Unit
        var sends = 0
        override fun observe(onState: (LedControlState) -> Unit, onError: (String) -> Unit, onAuthLost: () -> Unit) { update = onState }
        override fun send(command: String, onComplete: (Boolean) -> Unit) { sends++; result = onComplete }
        override fun serverTime() = now
        override fun stop() {}
    }
    private class View : LedControlView {
        var latest = LedUiState()
        var successes = 0
        override fun render(state: LedUiState) { latest = state }
        override fun showSuccess(message: String) { successes++ }
        override fun returnToSignIn() {}
    }
    private class Timer : LedScheduler {
        lateinit var tick: () -> Unit
        override fun repeat(action: () -> Unit) { tick = action }
        override fun stop() {}
    }
    @Test fun acknowledgmentDoesNotInventActualStateAndRepeatedClicksAreBlocked() {
        val repo = Repo(); val view = View(); val timer = Timer()
        val presenter = LedControlPresenter(repo, view, timer)
        presenter.start()
        repo.update(LedControlState("OFF", false, repo.now))
        presenter.onTurnOnClicked(); presenter.onTurnOnClicked()
        assertEquals(1, repo.sends)
        repo.result(true)
        assertEquals(false, view.latest.data.actualState)
        assertFalse(view.latest.sending)
        assertEquals(1, view.successes)
        repo.update(LedControlState("ON", true, repo.now))
        assertEquals(true, view.latest.data.actualState)
        repo.now += 10001
        timer.tick()
        assertEquals(DeviceStatus.OFFLINE, view.latest.device)
        repo.update(LedControlState("ON", true, repo.now))
        assertEquals(DeviceStatus.ONLINE, view.latest.device)
        presenter.stop()
        repo.result(false)
        assertNull(view.latest.error)
    }
    @Test fun missingAndFutureTimestampsAreUnavailable() {
        assertEquals(DeviceStatus.UNAVAILABLE, LedControlPresenter.availability(null, 100000))
        assertEquals(DeviceStatus.UNAVAILABLE, LedControlPresenter.availability(0, 100000))
        assertEquals(DeviceStatus.UNAVAILABLE, LedControlPresenter.availability(200000, 100000))
        assertEquals(DeviceStatus.ONLINE, LedControlPresenter.availability(90000, 100000))
        assertEquals(DeviceStatus.OFFLINE, LedControlPresenter.availability(89999, 100000))
    }
}
