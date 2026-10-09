package dev.kinetik.widget

import dev.kinetik.model.Library
import dev.kinetik.model.Seed
import dev.kinetik.session.Control
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.SessionState
import dev.kinetik.session.reduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetModelTest {
    private val lib = Seed.library()
    private val pull = SessionState.start(lib.groups[0].workouts[0])

    @Test fun idleShowsUpNextWithStart() {
        val m = widgetModel(null, lib)
        assertEquals("Up next", m.headline)
        assertEquals("Pull", m.title)
        assertEquals(lib.groups[0].workouts[0].id, m.startWorkoutId)
        assertTrue(m.controls.isEmpty())
    }

    @Test fun noWorkoutsAtAll() {
        val m = widgetModel(null, Library(groups = emptyList()))
        assertNull(m.startWorkoutId)
        assertEquals("No workouts", m.title)
    }

    @Test fun duringASet() {
        val m = widgetModel(pull, lib)
        assertEquals("Pull · Circuit 1/5", m.headline)
        assertEquals("Pull ups", m.title)
        assertEquals("8", m.big)
        assertFalse(m.isRest)
        assertEquals(listOf(Control.DONE), m.controls)
        assertNull(m.startWorkoutId)
    }

    @Test fun duringARest() {
        val m = widgetModel(reduce(pull, SessionEvent.Done), lib)
        assertEquals("Rest", m.title)
        assertEquals("3:00", m.big)
        assertTrue(m.isRest)
        assertEquals("Next: Chin ups · 8", m.sub)
        assertEquals(listOf(Control.PAUSE, Control.SKIP), m.controls)
    }

    @Test fun maxTimeDoesNotTickEverySecond() {
        val neg = pull.copy(index = 24, stepElapsedMs = 7_000)
        assertEquals("MAX", widgetModel(neg, lib).big)
    }

    @Test fun finished() {
        val done = pull.copy(phase = dev.kinetik.session.Phase.FINISHED)
        val m = widgetModel(done, lib)
        assertEquals("Workout complete", m.title)
        assertTrue(m.controls.isEmpty())
    }
}
