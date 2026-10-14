package com.example.floodgate.ledcontrol.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.floodgate.ledcontrol.model.*

@Composable
fun GateDevicesScreen(onAuthLost: () -> Unit, modifier: Modifier = Modifier,
                      deviceDetails: (@Composable () -> Unit)? = null) {
    val repository = remember { GateDirectoryRepository() }
    var gates by remember { mutableStateOf(listOf(GateDevice.Existing)) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(repository, lifecycle, retry) {
        fun start() {
            loading = true; error = false
            repository.observe({ gates = it; loading = false }, { error = true; loading = false })
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) start()
            if (event == Lifecycle.Event.ON_STOP) repository.stop()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) start()
        onDispose { lifecycle.removeObserver(observer); repository.stop() }
    }
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Devices", style = MaterialTheme.typography.headlineLarge)
            Text("Your gates and their live connection status", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { adding = true }) { Text("Add gate") }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error) {
                Text("Unable to load additional gates. Check your connection and database permissions.",
                    color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { retry++ }) { Text("Retry") }
            }
        }
        if (deviceDetails != null) item(key = "floodgate_details") {
            var expanded by rememberSaveable { mutableStateOf(false) }
            Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("FloodGate 01", style = MaterialTheme.typography.titleLarge)
                    Text("Flood barrier · Main Entrance", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide details" else "View FloodGate 01 details")
                    }
                    if (expanded) deviceDetails()
                }
            }
        }
        items(gates, key = { it.id }) { gate -> ExternalLedCard(onAuthLost, gate, collapsible = true) }
    }
    if (adding) {
        var id by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { if (!busy) adding = false },
            title = { Text("Add gate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Give each ESP32 a unique ID and configure its firmware to use the matching path.")
                    OutlinedTextField(id, { id = it; message = null }, label = { Text("Device ID (e.g. gate2)") }, singleLine = true, enabled = !busy)
                    OutlinedTextField(name, { name = it; message = null }, label = { Text("Name (e.g. Back gate)") }, singleLine = true, enabled = !busy)
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                val deviceId = id.trim()
                if (!GateDevice.validId(deviceId) || deviceId == "gate1" || name.trim().isEmpty() || name.trim().length > 60) {
                    message = "Use a unique lowercase ID (letters, digits, - or _) and a name of 1–60 characters. gate1 is reserved."
                } else {
                    busy = true
                    repository.add(deviceId, name.trim()) { success ->
                        busy = false
                        if (success) adding = false else message = "Could not add gate. The ID may already exist, or access was denied."
                    }
                }
            }) { Text("Add") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { adding = false }) { Text("Cancel") } })
    }
}
