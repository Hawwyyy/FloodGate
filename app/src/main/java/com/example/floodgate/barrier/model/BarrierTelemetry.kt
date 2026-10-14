package com.example.floodgate.barrier.model

data class BarrierTelemetry(
    val raw: Int? = null,
    val percent: Double? = null,
    val waterStatus: String? = null,
    val highWater: Boolean? = null,
    val led: Boolean? = null,
    val buzzer: Boolean? = null,
    val suppressed: Boolean? = null,
    val barrier: String? = null,
    val mode: String? = null,
    val source: String? = null,
    val angle: Int? = null,
    val updatedAt: Long? = null
) {
    val hasTelemetry get() = updatedAt != null &&
        (raw != null || percent != null || barrier != null || waterStatus != null)

    companion object {
        fun parse(value: Any?): BarrierTelemetry {
            val root = value as? Map<*, *> ?: return BarrierTelemetry()
            fun field(group: String, key: String) = (root[group] as? Map<*, *>)?.get(key)
            fun number(value: Any?, min: Double, max: Double): Double? =
                (value as? Number)?.toDouble()?.takeIf { it.isFinite() && it in min..max }
            fun integer(value: Any?, min: Int, max: Int): Int? =
                number(value, min.toDouble(), max.toDouble())?.takeIf { it % 1.0 == 0.0 }?.toInt()
            fun choice(value: Any?, vararg allowed: String) = (value as? String)?.takeIf { it in allowed }
            return BarrierTelemetry(
                raw = integer(field("sensor", "waterLevel"), 0, 4095),
                // No ADC-derived substitute: calibration belongs to the firmware.
                percent = number(field("sensor", "waterLevelPercent"), 0.0, 100.0),
                waterStatus = choice(field("sensor", "status"), "DRY", "LOW", "MODERATE", "HIGH", "FLOOD CONFIRMED"),
                highWater = field("sensor", "highWater") as? Boolean,
                led = field("alerts", "ledActive") as? Boolean,
                buzzer = field("alerts", "buzzerActive") as? Boolean,
                suppressed = field("alerts", "autoSuppressed") as? Boolean,
                barrier = choice(field("barrier", "state"), "DEPLOYED", "UNDEPLOYED"),
                mode = choice(field("barrier", "mode"), "AUTO", "MANUAL"),
                source = choice(field("barrier", "source"), "AUTO", "SWITCH", "WEBSITE"),
                angle = integer(field("barrier", "servoAngle"), 0, 90)?.takeIf { it == 0 || it == 90 },
                updatedAt = (root["updatedAt"] as? Long)?.takeIf { it > 0 }
            )
        }
    }
}

enum class BarrierAction(val wire: String, val reported: String) {
    DEPLOY("DEPLOY", "DEPLOYED"), RETRACT("UNDEPLOY", "UNDEPLOYED");
    fun children(id: String): Map<String, Any> = mapOf("barrier" to wire, "id" to id)
}

enum class BarrierError { PERMISSION, NETWORK, OTHER }

interface BarrierRepository {
    fun observe(onTelemetry: (BarrierTelemetry) -> Unit, onConnection: (Boolean) -> Unit,
                onError: (BarrierError) -> Unit, onSignedOut: () -> Unit)
    fun send(action: BarrierAction, id: String, complete: (BarrierError?) -> Unit)
    fun serverTime(): Long
    fun stop()
}
