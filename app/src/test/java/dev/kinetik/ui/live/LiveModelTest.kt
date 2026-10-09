package dev.kinetik.ui.live

import dev.kinetik.model.Seed
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.SessionState
import dev.kinetik.session.reduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveModelTest {
    private val pull = SessionState.start(Seed.pull())

    @Test fun firstSet() {
        val m = liveModel(pull)
        assertFalse(m.isRest)
        assertEquals("Pull ups", m.name)
        assertEquals("8", m.big)
        assertEquals("Ex 1 of 4", m.sub)
        assertEquals(1, m.circuitNumber)
        assertEquals(5, m.circuits)
        assertEquals(4, m.segments)
        assertEquals(0, m.activeSegment)
        assertEquals("Chin ups", m.nextName)
        assertEquals("8", m.nextReps)
        assertEquals(listOf(RowStatus.NOW, RowStatus.NEXT, RowStatus.NEXT, RowStatus.NEXT), m.queue.map { it.status })
        assertEquals("3:00", m.queue[0].restAfter)
        assertEquals("4:00", m.queue[3].restAfter)
    }

    @Test fun rest() {
        val m = liveModel(reduce(pull, SessionEvent.Done))
        assertTrue(m.isRest)
        assertEquals("Rest", m.name)
        assertEquals("3:00", m.big)
        assertTrue(m.bigIsClock)
        assertEquals(1f, m.restFraction)
        assertFalse(m.beepZone)
        assertEquals(RowStatus.DONE, m.queue[0].status)
        assertEquals(RowStatus.NEXT, m.queue[1].status)
        val late = liveModel(reduce(reduce(pull, SessionEvent.Done), SessionEvent.Tick(176_000)))
        assertTrue(late.beepZone)
        assertEquals("0:04", late.big)
    }

    @Test fun maxTimeRepeatShowsStopwatchAndSetTag() {
        val neg = pull.copy(index = 25, stepElapsedMs = 7_500)
        val m = liveModel(neg)
        assertEquals("0:07", m.big)
        assertTrue(m.bigIsClock)
        assertTrue("2 of 4" in m.tags)
        assertEquals("MAX·T", m.queue[0].reps)
    }

    @Test fun maxBreak() {
        val push = SessionState.start(Seed.push()).copy(index = 32)
        val m = liveModel(push)
        assertEquals("MAX", m.big)
        assertTrue(m.canBreak)
        assertNull(m.breakLeft)
        assertEquals("0:10", liveModel(reduce(push, SessionEvent.ToggleBreak)).breakLeft)
        assertEquals(1, m.segments)
    }

    @Test fun weightTagOnly() {
        assertTrue("10 kg" in liveModel(SessionState.start(Seed.push())).tags)
        assertTrue(liveModel(SessionState.start(Seed.legs())).tags.isEmpty())
    }

    // Final check on device: "MAX" at rep-number size wrapped onto two lines and overflowed the ring.
    @Test fun maxIsMarkedAsAWordSoItIsDrawnSmaller() {
        assertTrue(liveModel(SessionState.start(Seed.push()).copy(index = 32)).bigIsWord)
        assertFalse(liveModel(pull).bigIsWord)
    }
}
