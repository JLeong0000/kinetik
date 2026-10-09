package dev.kinetik.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PressGateTest {
    @Test fun dropsPressesInsideTheWindow() {
        val gate = PressGate(300)
        assertTrue(gate.allow(10_000))
        assertFalse(gate.allow(10_150))
        assertFalse(gate.allow(10_299))
        assertTrue(gate.allow(10_300))
    }

    @Test fun droppedPressesDoNotExtendTheWindow() {
        val gate = PressGate(300)
        assertTrue(gate.allow(0))
        assertFalse(gate.allow(200))
        assertTrue(gate.allow(320))
    }

    @Test fun firstPressIsAlwaysAllowed() = assertTrue(PressGate().allow(0))
}
