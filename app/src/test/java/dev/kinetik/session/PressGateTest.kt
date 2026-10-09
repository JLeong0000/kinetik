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

    // Final review: after DONE the rest controls appear under the thumb, so a slow second tap must not hit Skip.
    @Test fun aLongerHoldCanFollowAPress() {
        val gate = PressGate(300)
        assertTrue(gate.allow(0, holdMs = 700))
        assertFalse(gate.allow(500))
        assertTrue(gate.allow(700))
    }
}
