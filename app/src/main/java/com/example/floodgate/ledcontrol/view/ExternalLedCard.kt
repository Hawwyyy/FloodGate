package com.example.floodgate.ledcontrol.view

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.floodgate.ledcontrol.model.*
import com.example.floodgate.ledcontrol.presenter.*
import com.example.floodgate.ui.theme.FloodGateAction
import com.example.floodgate.ui.theme.FloodGateSurfacePrimary
import com.example.floodgate.ui.theme.FloodGateNeutral100
import com.example.floodgate.ui.theme.FloodGateNeutralDefault
import com.example.floodgate.ui.dashboard.DashboardTokens
import java.text.DateFormat
import java.util.Date

private class MainThreadLedScheduler : LedScheduler {
    private val handler = Handler(Looper.getMainLooper())
    private var job: Runnable? = null
    override fun repeat(action: () -> Unit) {
        stop()
        job = object : Runnable {
            override fun run() { action(); handler.postDelayed(this, 1000) }
        }.also { handler.postDelayed(it, 1000) }
    }
    override fun stop() { job?.let(handler::removeCallbacks); job = null }
}

@Composable
fun ExternalLedCard(onAuthLost: () -> Unit) {
    var state by remember { mutableStateOf(LedUiState()) }
    val context = LocalContext.current
    val currentAuthLost by rememberUpdatedState(onAuthLost)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val presenter = remember {
        LedControlPresenter(FirebaseLedControlRepository(), object : LedControlView {
            override fun render(stateValue: LedUiState) { state = stateValue }
            override fun showSuccess(message: String) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            override fun returnToSignIn() { currentAuthLost() }
        }, MainThreadLedScheduler())
    }
    DisposableEffect(lifecycle, presenter) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> presenter.start()
                Lifecycle.Event.ON_STOP -> presenter.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) presenter.start()
        onDispose { lifecycle.removeObserver(observer); presenter.stop() }
    }
    Column(Modifier.fillMaxWidth().background(DashboardTokens.Card,
        RoundedCornerShape(DashboardTokens.CardRadius)).padding(DashboardTokens.PagePadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("External LED", style = MaterialTheme.typography.headlineSmall)
        Text("ESP32 · ${state.device.name}", style = MaterialTheme.typography.bodySmall,
            color = com.example.floodgate.ui.theme.FloodGateNeutralDefault,
            modifier = Modifier.background(com.example.floodgate.ui.theme.FloodGateNeutral100,
                RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
        LedStatusRow("Requested Command", state.data.command ?: "UNKNOWN")
        val actual = when (state.data.actualState) { true -> "ON"; false -> "OFF"; null -> "UNKNOWN" }
        LedStatusRow(if (state.device == DeviceStatus.ONLINE) "Actual LED Status" else "Last Known LED", actual)
        Text("Last update: " + (state.data.lastUpdate?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it))
        } ?: "Unavailable"), style = MaterialTheme.typography.bodySmall)
        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Loading LED status…")
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            if (!state.sending) TextButton(onClick = { presenter.stop(); presenter.start() }) { Text("Retry") }
        }
        // Full-width buttons remain readable on small phones and with larger text.
        // Only fresh telemetry confirms the state. Allow reversing an unconfirmed request.
        val confirmedOn = state.device == DeviceStatus.ONLINE && state.data.actualState == true && state.data.command != "OFF"
        val confirmedOff = state.device == DeviceStatus.ONLINE && state.data.actualState == false && state.data.command != "ON"
        LedCommandButton(if (confirmedOn) "LED is on" else "Turn on", true,
            !state.loading && !state.sending && !confirmedOn,
            state.sending && state.sendingCommand == "ON", presenter::onTurnOnClicked)
        LedCommandButton(if (confirmedOff) "LED is off" else "Turn off", true,
            !state.loading && !state.sending && !confirmedOff,
            state.sending && state.sendingCommand == "OFF", presenter::onTurnOffClicked)
        if (state.sending) {
            Text("Sending ${state.sendingCommand.orEmpty()} command…", style = MaterialTheme.typography.bodySmall)
        } else if ((state.data.command == "ON" && state.data.actualState != true) ||
            (state.data.command == "OFF" && state.data.actualState != false)) {
            Text("Waiting for ESP32 confirmation", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LedCommandButton(label: String, filled: Boolean, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    val active = enabled || loading
    val container by animateColorAsState(
        if (!active) FloodGateNeutral100 else if (filled) FloodGateAction else FloodGateAction.copy(alpha = 0.08f),
        animationSpec = tween(180), label = "LED button background")
    val foreground = if (!active) FloodGateNeutralDefault else if (filled) FloodGateSurfacePrimary else FloodGateAction
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = foreground,
            disabledContainerColor = container, disabledContentColor = foreground
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = foreground, strokeWidth = 2.dp)
        else Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LedStatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = com.example.floodgate.ui.theme.FloodGateNeutralDefault)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
    }
}
