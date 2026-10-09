package dev.kinetik.ui

import dev.kinetik.ui.components.cssGradientEndpoints
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
