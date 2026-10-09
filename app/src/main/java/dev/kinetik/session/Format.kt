package dev.kinetik.session

import dev.kinetik.model.ExerciseKind
import dev.kinetik.plan.PlannedSet
import dev.kinetik.plan.Step

/** Countdowns round up, so the display reads 0:01 until the very end. */
fun countdown(ms: Long): String {
    val sec = ((ms + 999) / 1000).toInt()
    return "%d:%02d".format(sec / 60, sec % 60)
}

fun stopwatch(ms: Long): String {
    val sec = (ms / 1000).toInt()
    return "%d:%02d".format(sec / 60, sec % 60)
}

fun setLabel(p: PlannedSet): String = buildString {
    append(p.name)
    append(" · ")
    append(
        when (p.kind) {
            ExerciseKind.REPS -> "${p.reps}"
            ExerciseKind.MAX_REPS -> "max"
            ExerciseKind.MAX_TIME -> "max time"
        },
    )
    if (p.repeatCount > 1) append(" (${p.repeat + 1}/${p.repeatCount})")
}

fun notificationText(s: SessionState): Pair<String, String> {
    if (s.phase == Phase.FINISHED) return s.workoutName to "Workout complete"
    val title = "${s.workoutName} · Circuit ${s.circuit + 1}/${s.circuits}"
    val body = when (val step = s.step) {
        is Step.Work -> setLabel(step.set)
        is Step.Rest -> "Rest ${countdown(s.restRemainingMs)}" + (s.nextWork()?.let { " · next ${it.name}" } ?: "")
        null -> ""
    }
    return title to if (s.phase == Phase.PAUSED) "Paused · $body" else body
}

/** What the headphone play/pause button does: finish the set, or pause/resume a rest. */
fun primaryEvent(s: SessionState): SessionEvent? = when {
    s.phase == Phase.FINISHED -> null
    s.step is Step.Work -> SessionEvent.Done
    else -> SessionEvent.TogglePause
}
