package com.example.floodgate.ledcontrol.model

data class GateDevice(val id: String, val name: String) {
    val controlPath: String
        get() = if (id == "gate1") "eldroid/ledControl" else "eldroid/gates/$id/ledControl"

    companion object {
        val Existing = GateDevice("gate1", "Gate 1")
        fun validId(id: String) = id.matches(Regex("[a-z][a-z0-9_-]{0,39}"))
    }
}
