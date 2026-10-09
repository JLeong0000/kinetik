package dev.kinetik.cues

import dev.kinetik.model.ExerciseKind
import dev.kinetik.plan.PlannedSet
import dev.kinetik.plan.Step
import dev.kinetik.session.Phase
import dev.kinetik.session.SessionEvent
import dev.kinetik.session.SessionState

sealed interface Cue {
    data class Speak(val text: String) : Cue
    data object ShortBeep : Cue
    data object LongBeep : Cue
}

fun announce(p: PlannedSet): String = buildString {
    when (p.kind) {
        ExerciseKind.REPS -> append("${p.name}, ${p.reps}")
        ExerciseKind.MAX_REPS -> append("Max ${p.name}")
        ExerciseKind.MAX_TIME -> append("Max time ${p.name}")
    }
    if (p.repeatCount > 1) append(", set ${p.repeat + 1} of ${p.repeatCount}")
}

private fun ceilSec(ms: Long) = (ms + 999) / 1000

/** True when the displayed whole second drops into 5..1. */
private fun countdownBeep(before: Long, after: Long): Boolean {
    val a = ceilSec(before)
    val b = ceilSec(after)
    return b < a && b in 1..5
}

/** The cues caused by moving from [prev] to [next] via [event]. [prev] is null when a session starts. */
fun cuesFor(prev: SessionState?, event: SessionEvent?, next: SessionState): List<Cue> {
    if (next.phase == Phase.FINISHED) {
        return if (prev?.phase == Phase.FINISHED) emptyList() else listOf(Cue.LongBeep, Cue.Speak("Workout complete"))
    }
    val out = mutableListOf<Cue>()
    if (prev == null || prev.index != next.index) {
        if (prev?.step is Step.Rest && event is SessionEvent.Tick) out += Cue.LongBeep
        when (val step = next.step) {
            is Step.Work -> out += Cue.Speak(announce(step.set))
            is Step.Rest -> out += Cue.Speak("Rest")
            null -> Unit
        }
        return out
    }
    if (next.isRest && countdownBeep(prev.restRemainingMs, next.restRemainingMs)) out += Cue.ShortBeep
    val pb = prev.breakRemainingMs
    val nb = next.breakRemainingMs
    if (pb != null && nb != null && countdownBeep(pb, nb)) out += Cue.ShortBeep
    if (pb != null && nb == null && event is SessionEvent.Tick) out += Cue.LongBeep
    return out
}
