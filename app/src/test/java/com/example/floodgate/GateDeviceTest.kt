package com.example.floodgate

import com.example.floodgate.ledcontrol.model.GateDevice
import org.junit.Assert.*
import org.junit.Test

class GateDeviceTest {
    @Test fun existingGateKeepsLegacyConnection() {
        assertEquals("Gate 1", GateDevice.Existing.name)
        assertEquals("eldroid/ledControl", GateDevice.Existing.controlPath)
    }
    @Test fun gatesHaveIsolatedPathsRegardlessOfDisplayName() {
        assertEquals("eldroid/gates/gate2/ledControl", GateDevice("gate2", "Back gate").controlPath)
        assertNotEquals(GateDevice.Existing.controlPath, GateDevice("gate2", "Gate 1").controlPath)
        assertFalse(GateDevice.validId("gate2/command"))
        assertFalse(GateDevice.validId(""))
        assertTrue(GateDevice.validId("back-gate"))
    }
}
