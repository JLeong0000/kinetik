package dev.kinetik.session

import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.model.Seed
import dev.kinetik.model.Workout
import dev.kinetik.plan.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    private val two = Workout(
        name = "Two", circuits = 1, repDrop = 0, restExerciseSec = 180, restCircuitSec = 240,
        exercises = listOf(
            Exercise(name = "A", startReps = 5),
            Exercise(name = "B", kind = ExerciseKind.MAX_REPS, maxBreakSec = 10),
        ),
    )

    private fun SessionState.after(vararg events: SessionEvent) = events.fold(this) { s, e -> reduce(s, e) }
    private val tick1s = SessionEvent.Tick(1_000)

    @Test fun startsOnFirstSet() {
        val s = SessionState.start(two)
        assertEquals("A", s.work?.name)
        assertEquals(Phase.RUNNING, s.phase)
    }

    @Test fun doneStartsRestThatAutoAdvances() {
        var s = SessionState.start(two).after(SessionEvent.Done)
        assertTrue(s.isRest)
        assertEquals(180_000, s.restRemainingMs)
        s = s.after(SessionEvent.Tick(179_000))
        assertTrue(s.isRest)
        assertEquals(1_000, s.restRemainingMs)
        s = s.after(tick1s)
        assertEquals("B", s.work?.name)
        assertEquals(0, s.stepElapsedMs)
        assertEquals(180_000, s.totalElapsedMs)
    }

    @Test fun doneOnLastSetFinishesWithNoTrailingRest() {
        val s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Skip, SessionEvent.Done)
        assertEquals(Phase.FINISHED, s.phase)
    }

    @Test fun pauseFreezesTime() {
        val s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.TogglePause, SessionEvent.Tick(60_000))
        assertEquals(180_000, s.restRemainingMs)
        assertEquals(Phase.PAUSED, s.phase)
        assertEquals(Phase.RUNNING, s.after(SessionEvent.TogglePause).phase)
    }

    @Test fun minusThirtyShortensRest() {
        val s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.MinusThirty)
        assertEquals(150_000, s.restRemainingMs)
    }

    @Test fun minusThirtyWithLittleLeftEndsRest() {
        val s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Tick(160_000), SessionEvent.MinusThirty)
        assertEquals("B", s.work?.name)
    }

    @Test fun skipAndMinusThirtyAreIgnoredDuringASet() {
        val s = SessionState.start(two)
        assertEquals(s, s.after(SessionEvent.Skip, SessionEvent.MinusThirty))
    }

    @Test fun maxTimeSetCountsUp() {
        val s = SessionState.start(Seed.pull()).copy(index = 24).after(SessionEvent.Tick(4_200))
        assertEquals(ExerciseKind.MAX_TIME, s.work?.kind)
        assertEquals(4_200, s.stepElapsedMs)
        assertEquals("0:04", stopwatch(s.stepElapsedMs))
    }

    @Test fun breakCountsDownAndClears() {
        var s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Skip, SessionEvent.ToggleBreak)
        assertEquals(10_000L, s.breakRemainingMs)
        s = s.after(SessionEvent.Tick(9_000))
        assertEquals(1_000L, s.breakRemainingMs)
        s = s.after(tick1s)
        assertNull(s.breakRemainingMs)
        assertEquals("B", s.work?.name)
    }

    @Test fun breakIsOnlyForSetsWithAMaxBreak() {
        val s = SessionState.start(two)
        assertEquals(s, s.after(SessionEvent.ToggleBreak))
    }

    // Review focus 1: a double-tapped DONE must not skip the rest.
    @Test fun secondDoneDuringRestIsIgnored() {
        val s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Done)
        assertTrue(s.isRest)
        assertEquals(1, s.index)
    }

    // Review focus 2: one huge tick (phone slept) ends the rest but never skips a set.
    @Test fun hugeTickNeverSkipsASet() {
        val s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Tick(3_600_000))
        assertEquals("B", s.work?.name)
        assertEquals(Phase.RUNNING, s.phase)
    }

    // Review focus 3: skip or −30 s while paused resumes on the next step.
    @Test fun skipWhilePausedResumes() {
        val skipped = SessionState.start(two).after(SessionEvent.Done, SessionEvent.TogglePause, SessionEvent.Skip)
        assertEquals("B", skipped.work?.name)
        assertEquals(Phase.RUNNING, skipped.phase)
        val trimmed = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Tick(170_000), SessionEvent.TogglePause, SessionEvent.MinusThirty)
        assertEquals(Phase.RUNNING, trimmed.phase)
    }

    @Test fun finishedIgnoresEverything() {
        val done = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Skip, SessionEvent.Done)
        assertEquals(done, done.after(tick1s, SessionEvent.TogglePause, SessionEvent.Done))
    }

    @Test fun timeLeftCountsRestsAndEstimatedSets() {
        val s = SessionState.start(two)
        assertEquals(45_000L + 180_000 + 45_000, s.timeLeftMs())
        assertEquals(180_000L + 45_000, s.after(SessionEvent.Done).timeLeftMs())
    }

    // Lock-screen card: the buttons shown for each state (earbuds are left to the music app).
    @Test fun controlsForEachState() {
        val s = SessionState.start(two)
        assertEquals(listOf(Control.DONE), controlsFor(s))
        val rest = s.after(SessionEvent.Done)
        assertEquals(listOf(Control.PAUSE, Control.SKIP), controlsFor(rest))
        assertEquals(listOf(Control.RESUME, Control.SKIP), controlsFor(rest.after(SessionEvent.Tick(800), SessionEvent.TogglePause)))
        assertEquals(emptyList<Control>(), controlsFor(rest.after(SessionEvent.Skip, SessionEvent.Done)))
        assertEquals(SessionEvent.TogglePause, Control.RESUME.event)
    }

    @Test fun formats() {
        assertEquals("3:00", countdown(180_000))
        assertEquals("0:05", countdown(4_001))
        assertEquals("0:04", stopwatch(4_999))
        assertEquals("68:00", countdown(4_080_000))
        val pull = SessionState.start(Seed.pull())
        assertEquals("Pull · Circuit 1/5" to "Pull ups · 8", notificationText(pull))
        val rest = reduce(pull, SessionEvent.Done)
        assertEquals("Pull · Circuit 1/5" to "Rest · next Chin ups", notificationText(rest))
        assertEquals("Paused · Rest · next Chin ups", notificationText(reduce(rest, SessionEvent.TogglePause)).second)
        val neg = (pull.steps[24] as Step.Work).set
        assertEquals("Pull up negatives · max time (1/4)", setLabel(neg))
    }

    @Test fun jumpToCircuitStartsItsFirstSet() {
        val pull = SessionState.start(Seed.pull())
        val s = pull.after(SessionEvent.Done, tick1s, SessionEvent.TogglePause, SessionEvent.JumpTo(3))
        assertEquals(3, s.circuit)
        assertEquals("Pull up negatives", s.work?.name)
        assertEquals(Phase.RUNNING, s.phase)
        assertEquals(0L, s.stepElapsedMs)
        assertEquals(1_000L, s.totalElapsedMs)
        // Back to an earlier circuit works too.
        assertEquals(0, s.after(SessionEvent.JumpTo(0)).circuit)
    }

    @Test fun jumpToAnExerciseInACircuit() {
        val pull = SessionState.start(Seed.pull())
        assertEquals("Inverted pull up rows", reduce(pull, SessionEvent.JumpTo(0, exercise = 2)).work?.name)
        // Repeated sets start at the first one.
        val neg = reduce(pull, SessionEvent.JumpTo(3, exercise = 1)).work!!
        assertEquals("Chin up negatives" to 0, neg.name to neg.repeat)
        assertEquals(pull, reduce(pull, SessionEvent.JumpTo(0, exercise = 9)))
    }

    @Test fun jumpClearsABreakAndIgnoresBadOrSameTargets() {
        val s = SessionState.start(two).after(SessionEvent.Done, SessionEvent.Skip, SessionEvent.ToggleBreak)
        assertTrue(s.breakRemainingMs != null)
        assertNull(s.after(SessionEvent.JumpTo(0)).breakRemainingMs)
        val first = SessionState.start(Seed.pull())
        assertEquals(first, reduce(first, SessionEvent.JumpTo(0)))
        assertEquals(first, reduce(first, SessionEvent.JumpTo(9)))
    }

    // On device: a countdown in the notification text re-posted it every second, and the status-bar chip jumped each time.
    @Test fun notificationTextIsStillDuringARest() {
        val rest = reduce(SessionState.start(Seed.pull()), SessionEvent.Done)
        assertEquals(notificationText(rest), notificationText(rest.after(tick1s, tick1s, tick1s)))
    }

    @Test fun restProgressDrivesTheMediaCardBar() {
        val pull = SessionState.start(Seed.pull())
        assertNull(restProgress(pull))
        val rest = reduce(pull, SessionEvent.Done).after(SessionEvent.Tick(800))
        assertEquals(800L to 180_000L, restProgress(rest))
        assertEquals(30_800L to 180_000L, restProgress(rest.after(SessionEvent.MinusThirty)))
    }

    // Final review: a stale notification Pause tapped as a set starts must not freeze the set.
    @Test fun pauseIsIgnoredDuringASet() {
        val s = SessionState.start(two)
        assertEquals(s, s.after(SessionEvent.TogglePause))
    }

    // Final review: the service, wake lock and media session stop when the workout completes.
    @Test fun serviceRunsOnlyWhileAWorkoutIsInProgress() {
        val s = SessionState.start(two)
        assertTrue(serviceShouldRun(s))
        assertTrue(serviceShouldRun(s.after(SessionEvent.Done)))
        assertEquals(false, serviceShouldRun(s.after(SessionEvent.Done, SessionEvent.Skip, SessionEvent.Done)))
        assertEquals(false, serviceShouldRun(null))
    }

    // Final review: a double-tapped START must not start the workout twice.
    @Test fun aSecondStartIsIgnoredWhileASessionExists() {
        assertTrue(canStart(null))
        assertEquals(false, canStart(SessionState.start(two)))
        assertTrue(canStart(SessionState.start(two).after(SessionEvent.Done, SessionEvent.Skip, SessionEvent.Done)))
    }
}
