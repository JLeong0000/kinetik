package dev.kinetik.cues

import dev.kinetik.model.Exercise
import dev.kinetik.model.Seed
import dev.kinetik.model.Workout
import dev.kinetik.plan.Step
import dev.kinetik.plan.buildPlan
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.SessionState
import dev.kinetik.session.reduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CuesTest {
    private val two = Workout(
        name = "Two", circuits = 1, repDrop = 0, restExerciseSec = 10, restCircuitSec = 10,
        exercises = listOf(Exercise(name = "A", startReps = 5), Exercise(name = "B", startReps = 6)),
    )

    /** Applies [e] and returns the resulting state and the cues it produced. */
    private fun step(s: SessionState, e: SessionEvent): Pair<SessionState, List<Cue>> {
        val n = reduce(s, e)
        return n to cuesFor(s, e, n)
    }

    @Test fun announcements() {
        val push = buildPlan(Seed.push())
        val legs = buildPlan(Seed.legs())
        val pull = buildPlan(Seed.pull())
        assertEquals("Dips, 13", announce((push[0] as Step.Work).set))
        assertEquals("Shrimp squats, 11", announce((legs[0] as Step.Work).set))
        assertEquals("Pull up negatives, max time, 1 of 4", announce((pull[24] as Step.Work).set))
        assertEquals("Dips, max", announce((push[32] as Step.Work).set))
    }

    @Test fun startAnnouncesFirstSet() {
        val s = SessionState.start(two)
        assertEquals(listOf(Cue.Speak("A, 5")), cuesFor(null, null, s))
    }

    @Test fun doneSaysRest() {
        val (_, cues) = step(SessionState.start(two), SessionEvent.Done)
        assertEquals(listOf(Cue.Speak("Rest")), cues)
    }

    @Test fun beepsAtFiveToOneThenLongBeepAndAnnounce() {
        var s = reduce(SessionState.start(two), SessionEvent.Done) // 10 s rest
        val heard = mutableListOf<Cue>()
        repeat(100) {
            val (n, cues) = step(s, SessionEvent.Tick(100))
            heard += cues
            s = n
        }
        assertEquals(List(5) { Cue.ShortBeep } + listOf(Cue.LongBeep, Cue.Speak("B, 6")), heard)
    }

    @Test fun skipAnnouncesWithoutLongBeep() {
        val rest = reduce(SessionState.start(two), SessionEvent.Done)
        assertEquals(listOf(Cue.Speak("B, 6")), step(rest, SessionEvent.Skip).second)
    }

    @Test fun finishing() {
        val last = reduce(reduce(SessionState.start(two), SessionEvent.Done), SessionEvent.Skip)
        val (done, cues) = step(last, SessionEvent.Done)
        assertEquals(listOf(Cue.LongBeep, Cue.Speak("Workout complete")), cues)
        assertEquals(emptyList<Cue>(), step(done, SessionEvent.Tick(100)).second)
    }

    @Test fun maxBreakBeepsAndEndsWithLongBeep() {
        var s = SessionState.start(Seed.push()).copy(index = 32) // Max dips, 10 s break
        s = reduce(s, SessionEvent.ToggleBreak)
        val heard = mutableListOf<Cue>()
        repeat(10) {
            val (n, cues) = step(s, SessionEvent.Tick(1_000))
            heard += cues
            s = n
        }
        assertEquals(List(5) { Cue.ShortBeep } + Cue.LongBeep, heard)
    }

    // Review focus 4: a 0:00 rest gives back-to-back announcements, no "Rest", no beeps.
    @Test fun zeroRestAnnouncesBackToBack() {
        val noRest = two.copy(restExerciseSec = 0)
        val (_, cues) = step(SessionState.start(noRest), SessionEvent.Done)
        assertEquals(listOf(Cue.Speak("B, 6")), cues)
    }

    // Review focus 5: −30 s with under 30 s left jumps straight to the next set.
    @Test fun minusThirtyNearTheEndJustAnnounces() {
        val longRest = two.copy(restExerciseSec = 40)
        val rest = reduce(reduce(SessionState.start(longRest), SessionEvent.Done), SessionEvent.Tick(15_000))
        val (next, cues) = step(rest, SessionEvent.MinusThirty)
        assertEquals("B", next.work?.name)
        assertEquals(listOf(Cue.Speak("B, 6")), cues)
    }

    @Test fun minusThirtyIntoTheLastFiveSecondsBeepsOnce() {
        val longRest = two.copy(restExerciseSec = 40)
        val rest = reduce(reduce(SessionState.start(longRest), SessionEvent.Done), SessionEvent.Tick(7_000)) // 33 s left
        assertEquals(listOf(Cue.ShortBeep), step(rest, SessionEvent.MinusThirty).second) // 3 s left
    }

    @Test fun beepPcmFadesInAndOut() {
        val pcm = beepPcm(880.0, 0.15)
        assertEquals(6_615, pcm.size)
        assertTrue(abs(pcm.first().toInt()) < 200)
        assertTrue(abs(pcm.last().toInt()) < 200)
        assertTrue(pcm.maxOf { it.toInt() } > 20_000)
    }
}
