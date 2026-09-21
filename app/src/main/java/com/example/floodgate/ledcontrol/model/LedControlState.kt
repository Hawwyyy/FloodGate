package com.example.floodgate.ledcontrol.model

data class LedControlState(
    val command: String? = null,
    val actualState: Boolean? = null,
    val lastUpdate: Long? = null,
    val heartbeat: Long? = null
)

enum class DeviceStatus { ONLINE, OFFLINE, UNAVAILABLE }

interface LedControlRepository {
    fun observe(onState: (LedControlState) -> Unit, onError: (String) -> Unit, onAuthLost: () -> Unit)
    fun send(command: String, onComplete: (Boolean) -> Unit)
    fun serverTime(): Long
    fun stop()
}
