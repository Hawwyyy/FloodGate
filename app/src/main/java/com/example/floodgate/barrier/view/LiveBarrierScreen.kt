package com.example.floodgate.barrier.view

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.floodgate.barrier.model.*
import com.example.floodgate.barrier.presenter.*
import com.example.floodgate.R
import com.example.floodgate.ui.theme.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val Brand = FloodGateAction
private val BrandSurface = FloodGateAction.copy(alpha = 0.08f)
private val Amber = Color(0xFF805000)
private val AmberSurface = Color(0xFFFFF1D6)
private val Red = Color(0xFFAC2525)
private val RedSurface = Color(0xFFFFE8E5)

private class DashboardTimer : BarrierTimer {
    private val handler = Handler(Looper.getMainLooper())
    private var job: Runnable? = null
    override fun start(tick: () -> Unit) {
        stop()
        job = object : Runnable {
            override fun run() {
                tick()
                if (job === this) handler.postDelayed(this, 1000)
            }
        }.also { handler.postDelayed(it, 1000) }
    }
    override fun stop() { job?.let(handler::removeCallbacks); job = null }
}

@Composable
fun LiveBarrierScreen(onSignOut: () -> Unit, modifier: Modifier = Modifier,
                      isVisible: Boolean = true,
                      showDeviceDetails: Boolean = false,
                      onOpenDevices: () -> Unit = {},
                      onNotifications: () -> Unit = {},
                      onProfile: () -> Unit = {},
                      repository: BarrierRepository = remember { FirebaseBarrierRepository() }) {
    var state by remember { mutableStateOf(BarrierUiState()) }
    val signOut by rememberUpdatedState(onSignOut)
    val presenter = remember(repository) {
        BarrierPresenter(repository, object : BarrierView {
            override fun render(stateValue: BarrierUiState) { state = stateValue }
            override fun signedOut() { signOut() }
        }, DashboardTimer(), SystemClock::elapsedRealtime)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(presenter) { onDispose { presenter.clear() } }
    DisposableEffect(presenter, lifecycle, isVisible) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (isVisible) presenter.start()
                Lifecycle.Event.ON_STOP -> presenter.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (isVisible && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) presenter.start()
        onDispose { lifecycle.removeObserver(observer); presenter.stop() }
    }
    if (isVisible) {
        val logout = { presenter.clear(); signOut() }
        if (showDeviceDetails) {
            com.example.floodgate.ledcontrol.view.GateDevicesScreen(logout, modifier) {
                BarrierDashboardContent(state, presenter::send, presenter::retry, logout,
                    details = true)
            }
        } else BarrierDashboardContent(state, presenter::send, presenter::retry,
            logout, modifier, onOpenDevices = onOpenDevices,
            onNotifications = onNotifications, onProfile = onProfile)
    }
}

@Composable
internal fun BarrierDashboardContent(state: BarrierUiState, onCommand: (BarrierAction) -> Unit,
                                     onRetry: () -> Unit, onSignOut: () -> Unit,
                                     modifier: Modifier = Modifier, details: Boolean = false,
                                     onOpenDevices: () -> Unit = {},
                                     onNotifications: () -> Unit = {},
                                     onProfile: () -> Unit = {}) {
    var confirmation by remember { mutableStateOf<BarrierAction?>(null) }
    var technicalExpanded by remember { mutableStateOf(false) }
    val t = state.telemetry
    val contentModifier = if (details) modifier.fillMaxWidth()
        else modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    Column(contentModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!details) DashboardIdentity(onOpenDevices, onNotifications, onProfile)
        val deviceStatus = when (state.freshness) {
            Freshness.UNAVAILABLE -> "ESP32 · UNAVAILABLE"
            Freshness.STALE -> "ESP32 · OFFLINE / STALE"
            Freshness.ONLINE -> if (state.connected) "ESP32 · FRESH TELEMETRY" else "ESP32 · CONNECTION UNVERIFIED"
        }
        if (!state.connected || state.freshness != Freshness.ONLINE) {
            OfflineNotice(state, if (details) null else onOpenDevices)
        } else Notice(deviceStatus, BrandSurface)
        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Brand)
            Text("Loading telemetry…")
        }
        state.error?.let {
            Notice(it, RedSurface)
            TextButton(onClick = onRetry) { Text("Retry", color = Brand) }
        }
        if (t.waterStatus == "FLOOD CONFIRMED") {
            Section("Flood confirmed") {
                Text("High water confirmed · Barrier ${when (t.barrier) { "DEPLOYED" -> "deployed"; "UNDEPLOYED" -> "retracted"; else -> "unknown" }}", color = Red)
                if (t.suppressed == true) Text("Automatic retriggering suppressed until water falls to 30% or below.", color = Red)
            }
        }
        Section("Water level") {
            Text(t.percent?.let { String.format(Locale.getDefault(), "%.2f%%", it) } ?: "Unknown",
                style = MaterialTheme.typography.headlineLarge, color = Brand,
                modifier = Modifier.testTag("water_percent"))
            t.percent?.let {
                LinearProgressIndicator(progress = (it / 100).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Brand, trackColor = FloodGateProgressInactive)
            }
            StatusRow("Water status", t.waterStatus ?: "Unknown")
            if (details) {
                StatusRow("Above safe water level", yesNo(t.highWater))
            }
        }
        if (details) Section("Alerts & safety") {
            StatusRow("Warning light", onOff(t.led))
            StatusRow("Alarm buzzer", onOff(t.buzzer))
            StatusRow("Automatic reactivation", when (t.suppressed) {
                true -> "Paused"; false -> "Not paused"; null -> "Unknown"
            })
            if (t.suppressed == true) Text("Resumes when water falls to 30% or below.",
                style = MaterialTheme.typography.bodySmall, color = FloodGateNeutral400)
        }
        Section(if (details) "Barrier & controls" else "Quick controls") {
            StatusRow("Device-reported state", when (t.barrier) { "DEPLOYED" -> "Deployed"; "UNDEPLOYED" -> "Retracted"; else -> "Unknown" })
            if (details) {
            StatusRow("Operating mode", when (t.mode) { "AUTO" -> "Automatic"; "MANUAL" -> "Manual"; else -> "Unknown" })
            StatusRow("Last controlled by", when (t.source) {
                "WEBSITE" -> "Remote control"; "SWITCH" -> "Physical switch"; "AUTO" -> "Automatic"; else -> "Unknown"
            })
            }
            Button(onClick = { confirmation = BarrierAction.DEPLOY }, enabled = state.controlsEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("barrier_deploy"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Deploy") }
            OutlinedButton(onClick = { confirmation = BarrierAction.RETRACT }, enabled = state.controlsEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("barrier_retract"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand)) { Text("Retract") }
            if (state.sending) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Brand)
                Text("Sending… Controls are temporarily disabled.")
            }
            state.feedback?.let { Notice(it, BrandSurface) }
        }
        if (!details) TextButton(onClick = onOpenDevices,
            modifier = Modifier.fillMaxWidth().testTag("open_device_details")) {
            Text("View device details", color = Brand)
        }
        if (details) Section("Recent water readings") {
            Text("Latest ${state.readings.size} of 20 · this session", style = MaterialTheme.typography.bodySmall)
            WaterChart(state.readings)
        }
        if (details) Section("Connection & technical details") {
            StatusRow("App connection", if (state.connected) "Connected" else "Disconnected")
            StatusRow("Last device update", (t.updatedAt?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it))
            } ?: "Unknown"))
            TextButton(onClick = { technicalExpanded = !technicalExpanded }) {
                Text(if (technicalExpanded) "Hide technical details" else "Show technical details")
            }
            if (technicalExpanded) {
                StatusRow("Raw sensor reading (ADC)", t.raw?.let { "$it / 4095" } ?: "Unknown")
                StatusRow("Commanded servo angle", t.angle?.let { "$it°" } ?: "Unknown")
                Text("Water percentage is calibrated by the device. Automatic activation requires above 30% for five seconds.",
                    style = MaterialTheme.typography.bodySmall, color = FloodGateNeutral400)
                Text("The device reports its commanded state, not a measured barrier position. It has no position sensor or command acknowledgment.",
                    style = MaterialTheme.typography.bodySmall, color = FloodGateNeutral400)
            }
        }
    }
    confirmation?.let { action ->
        AlertDialog(onDismissRequest = { confirmation = null },
            title = { Text(if (action == BarrierAction.DEPLOY) "Deploy barrier?" else "Retract barrier?") },
            text = { Text(if (action == BarrierAction.DEPLOY)
                "Send a deploy command to FloodGate 01? The device turns on its LED and buzzer and commands the servo to 90°."
                else "Send a retract command to FloodGate 01? This turns outputs off. During high water, automatic retriggering stays suppressed until water drops to 30% or below.") },
            confirmButton = { TextButton(enabled = state.controlsEnabled, modifier = Modifier.testTag("barrier_confirm"),
                onClick = { confirmation = null; onCommand(action) }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } })
    }
}

@Composable
private fun OfflineNotice(state: BarrierUiState, onDetails: (() -> Unit)?) {
    val title = when {
        state.freshness == Freshness.STALE -> "Device Offline"
        state.freshness == Freshness.UNAVAILABLE -> "Device Unavailable"
        else -> "Connection Unavailable"
    }
    val lastSeen = state.telemetry.updatedAt?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
    } ?: "Not available"
    Surface(color = AmberSurface, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("device_offline_notice")) {
        BoxWithConstraints(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            val compact = maxWidth < 420.dp || LocalDensity.current.fontScale > 1.15f
            val message: @Composable (Modifier) -> Unit = { modifier ->
                Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(Color(0xFFF4AD42), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_wifi_off), null,
                            Modifier.size(24.dp), tint = Color.White)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = Amber)
                        Text("Live monitoring unavailable.",
                            style = MaterialTheme.typography.bodySmall, color = Amber)
                        Text("Last telemetry: $lastSeen", style = MaterialTheme.typography.bodySmall,
                            color = Amber.copy(alpha = 0.8f))
                    }
                }
            }
            val action: @Composable () -> Unit = {
                onDetails?.let { click ->
                    if (compact) IconButton(onClick = click, modifier = Modifier.size(48.dp)) {
                        Icon(painterResource(R.drawable.dashboard_arrow_right), "View details",
                            Modifier.size(20.dp), tint = Amber)
                    } else OutlinedButton(onClick = click, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF4AD42)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber),
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("View details")
                        Spacer(Modifier.width(4.dp))
                        Icon(painterResource(R.drawable.dashboard_arrow_right), null, Modifier.size(16.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                message(Modifier.weight(1f))
                action()
            }
        }
    }
}

@Composable
private fun DashboardIdentity(onOpenDevices: () -> Unit, onNotifications: () -> Unit,
                              onProfile: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale > 1.15f
            val brand: @Composable (Modifier) -> Unit = { modifier ->
                Row(modifier, verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(R.drawable.dashboard_logo), null, Modifier.size(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text("FloodGate", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = FloodGateHeading)
                        Text("Flood Protection System", style = MaterialTheme.typography.bodySmall,
                            color = FloodGateNeutral400)
                    }
                }
            }
            val actions: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderAction(R.drawable.dashboard_notification, "Notifications", onNotifications)
                    HeaderAction(R.drawable.dashboard_settings, "Profile settings", onProfile)
                }
            }
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                brand(Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
            } else Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                brand(Modifier.weight(1f))
                actions()
            }
        }
        Surface(onClick = onOpenDevices, color = Color.White, shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(painterResource(R.drawable.dashboard_device), null,
                    Modifier.size(28.dp), tint = FloodGateNeutralDefault)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("FloodGate 01", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = FloodGateHeading)
                    Text("Main Entrance", style = MaterialTheme.typography.bodyMedium,
                        color = FloodGateNeutral400)
                }
                Icon(painterResource(R.drawable.dashboard_arrow_down), "View devices",
                    Modifier.size(20.dp), tint = FloodGateHeading)
            }
        }
    }
}

@Composable
private fun HeaderAction(icon: Int, label: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.White) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(icon), label, Modifier.size(24.dp), tint = FloodGateHeading)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = FloodGateSectionHeading)
            Divider(color = FloodGateDashboardBackground, thickness = 1.dp)
            content()
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.15f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = FloodGateNeutral400)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = FloodGateNeutral400)
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}

@Composable
private fun BarrierOverview(telemetry: BarrierTelemetry) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)) {
        Image(painterResource(R.drawable.dashboard_barrier), "Flood barrier illustration, not a live camera",
            Modifier.fillMaxWidth().aspectRatio(380f / 206f), contentScale = ContentScale.Crop)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Barrier overview", style = FloodGateSectionHeading)
            StatusRow("Barrier status", telemetry.barrier ?: "Unknown")
            StatusRow("Flood status", telemetry.waterStatus ?: "Unknown")
        }
    }
}

@Composable
private fun Notice(text: String, background: Color) {
    Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium,
            color = if (background == RedSurface) Red else if (background == AmberSurface) Amber else Brand)
    }
}

@Composable
private fun WaterChart(readings: List<WaterReading>) {
    if (readings.isEmpty()) {
        Text("Waiting for calibrated readings. Command changes do not add readings.")
        return
    }
    val description = "Recent water percentages, oldest first: " + readings.joinToString { String.format(Locale.getDefault(), "%.2f", it.percent) }
    Text("100%", style = MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = description }) {
        val inset = 4.dp.toPx()
        val w = size.width - inset * 2
        val h = size.height - inset * 2
        for (fraction in listOf(0f, 0.5f, 1f)) {
            val y = inset + h * fraction
            drawLine(FloodGateDivider, Offset(inset, y), Offset(inset + w, y), 1.dp.toPx())
        }
        val path = Path()
        readings.forEachIndexed { index, reading ->
            val x = inset + if (readings.size == 1) w / 2 else w * index / (readings.size - 1)
            val y = inset + h * (1 - reading.percent.toFloat() / 100)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(Brand, 3.dp.toPx(), Offset(x, y))
        }
        drawPath(path, Brand, style = Stroke(2.dp.toPx()))
    }
    Text("0% · Oldest → newest", style = MaterialTheme.typography.bodySmall)
}

private fun onOff(value: Boolean?) = when (value) { true -> "ON"; false -> "OFF"; null -> "Unknown" }
private fun yesNo(value: Boolean?) = when (value) { true -> "Yes"; false -> "No"; null -> "Unknown" }
