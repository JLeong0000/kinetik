package dev.kinetik.session

import dev.kinetik.model.Workout
import dev.kinetik.plan.ESTIMATED_SET_SEC
import dev.kinetik.plan.PlannedSet
import dev.kinetik.plan.Step
import dev.kinetik.plan.buildPlan
import dev.kinetik.plan.estimateSeconds

enum class Phase { RUNNING, PAUSED, FINISHED }

data class SessionState(
    val workoutId: String,
    val workoutName: String,
    val circuits: Int,
    val steps: List<Step>,
    val index: Int = 0,
    val phase: Phase = if (steps.isEmpty()) Phase.FINISHED else Phase.RUNNING,
    /** Time in the current step: the stopwatch for a set, elapsed time for a rest. */
    val stepElapsedMs: Long = 0,
    val restRemainingMs: Long = restMs(steps.getOrNull(index)),
    val breakRemainingMs: Long? = null,
    val totalElapsedMs: Long = 0,
) {
    val step: Step? get() = steps.getOrNull(index)
    val work: PlannedSet? get() = (step as? Step.Work)?.set
    val isRest: Boolean get() = step is Step.Rest

    val circuit: Int
        get() = when (val s = step) {
            is Step.Work -> s.set.circuit
            is Step.Rest -> s.circuit
            null -> circuits - 1
        }

    fun nextWork(): PlannedSet? = steps.drop(index + 1).firstNotNullOfOrNull { (it as? Step.Work)?.set }

    fun timeLeftMs(): Long {
        val current = when (step) {
            is Step.Rest -> restRemainingMs
            is Step.Work -> maxOf(0, ESTIMATED_SET_SEC * 1000L - stepElapsedMs)
            null -> 0
        }
        return current + estimateSeconds(steps.drop(index + 1)) * 1000L
    }

    companion object {
        fun start(w: Workout) = SessionState(w.id, w.name, w.circuits, buildPlan(w))
    }
}

internal fun restMs(step: Step?): Long = (step as? Step.Rest)?.seconds?.times(1000L) ?: 0L

sealed interface SessionEvent {
    data class Tick(val deltaMs: Long) : SessionEvent
    data object Done : SessionEvent
    data object TogglePause : SessionEvent
    data object MinusThirty : SessionEvent
    data object Skip : SessionEvent
    data object ToggleBreak : SessionEvent
}

fun reduce(s: SessionState, e: SessionEvent): SessionState {
    if (s.phase == Phase.FINISHED) return s
    return when (e) {
        is SessionEvent.Tick -> tick(s, e.deltaMs)
        SessionEvent.Done -> if (s.step is Step.Work) advance(s) else s
        SessionEvent.TogglePause ->
            if (!s.isRest) s else s.copy(phase = if (s.phase == Phase.PAUSED) Phase.RUNNING else Phase.PAUSED)
        SessionEvent.MinusThirty -> {
            if (!s.isRest) return s
            val left = maxOf(0, s.restRemainingMs - 30_000)
            if (left == 0L) advance(s) else s.copy(restRemainingMs = left)
        }
        SessionEvent.Skip -> if (s.isRest) advance(s) else s
        SessionEvent.ToggleBreak -> {
            val seconds = s.work?.maxBreakSec ?: return s
            s.copy(breakRemainingMs = if (s.breakRemainingMs == null) seconds * 1000L else null)
        }
    }
}

private fun tick(s: SessionState, d: Long): SessionState {
    if (s.phase == Phase.PAUSED || d <= 0) return s
    val t = s.copy(stepElapsedMs = s.stepElapsedMs + d, totalElapsedMs = s.totalElapsedMs + d)
    return when {
        t.isRest -> {
            val left = t.restRemainingMs - d
            if (left <= 0) advance(t) else t.copy(restRemainingMs = left)
        }
        t.breakRemainingMs != null -> {
            val left = t.breakRemainingMs - d
            t.copy(breakRemainingMs = if (left <= 0) null else left)
        }
        else -> t
    }
}

/** Moves to the next step (never more than one), always running. Overshoot from a long tick is dropped. */
private fun advance(s: SessionState): SessionState {
    val next = s.index + 1
    if (next >= s.steps.size) return s.copy(phase = Phase.FINISHED, breakRemainingMs = null, restRemainingMs = 0)
    return s.copy(
        index = next,
        phase = Phase.RUNNING,
        stepElapsedMs = 0,
        breakRemainingMs = null,
        restRemainingMs = restMs(s.steps[next]),
    )
}
