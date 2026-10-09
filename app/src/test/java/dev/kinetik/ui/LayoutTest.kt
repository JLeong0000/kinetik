package dev.kinetik.ui

import dev.kinetik.ui.components.cssGradientEndpoints
import dev.kinetik.ui.components.ringSegmentAt
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutTest {
    @Test fun coverMainAndTabletop() {
        assertEquals(LayoutMode.COVER, layoutModeFor(412, null))
        assertEquals(LayoutMode.MAIN, layoutModeFor(954, Fold(halfOpened = false, horizontal = false)))
        assertEquals(LayoutMode.MAIN, layoutModeFor(954, Fold(halfOpened = true, horizontal = false)))
        assertEquals(LayoutMode.TABLETOP, layoutModeFor(954, Fold(halfOpened = true, horizontal = true)))
    }

    @Test fun ringTapsMapToSegmentsClockwiseFromTheTop() {
        // 400 px ring, 20 px stroke: the arc's centre line is 180 px from the middle.
        assertEquals(0, ringSegmentAt(220f, 20f, 400f, 20f, 4))   // just right of 12 o'clock
        assertEquals(1, ringSegmentAt(380f, 220f, 400f, 20f, 4))  // just below 3 o'clock
        assertEquals(2, ringSegmentAt(180f, 380f, 400f, 20f, 4))  // just left of 6 o'clock
        assertEquals(3, ringSegmentAt(20f, 180f, 400f, 20f, 4))   // just above 9 o'clock
        assertNull(ringSegmentAt(200f, 200f, 400f, 20f, 4))       // the middle, where the text is
    }

    @Test fun cssAngles() {
        val (s180, e180) = cssGradientEndpoints(180f, 100f, 200f)
        assertEquals(50f, s180.x, 0.01f); assertEquals(0f, s180.y, 0.01f)
        assertEquals(50f, e180.x, 0.01f); assertEquals(200f, e180.y, 0.01f)
        val (s90, e90) = cssGradientEndpoints(90f, 100f, 200f)
        assertEquals(0f, s90.x, 0.01f); assertEquals(100f, e90.x, 0.01f)
        val (s190, e190) = cssGradientEndpoints(190f, 100f, 200f)
        assertTrue(s190.x > e190.x) // 190° runs down and slightly to the left
    }
}
