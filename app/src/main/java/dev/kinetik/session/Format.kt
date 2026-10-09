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
    val title = "${s.workoutName} · ${s.section} ${s.circuit + 1}/${s.circuits}"
    val body = when (val step = s.step) {
        is Step.Work -> setLabel(step.set)
        // No countdown here: changing text re-posts the notification, which made the status-bar chip jump every second.
        is Step.Rest -> "Rest" + (s.nextWork()?.let { " · next ${it.name}" } ?: "")
        null -> ""
    }
    return title to if (s.phase == Phase.PAUSED) "Paused · $body" else body
}

/** (elapsed, total) ms of the current rest, for the lock-screen card's progress bar; null during a set. */
fun restProgress(s: SessionState): Pair<Long, Long>? {
    val total = restMs(s.step).takeIf { it > 0 } ?: return null
    return total - s.restRemainingMs to total
}

/** Buttons on the lock-screen card. Earbud play/pause is left to the music app. */
enum class Control(val label: String, val event: SessionEvent) {
    DONE("Done", SessionEvent.Done),
    PAUSE("Pause", SessionEvent.TogglePause),
    RESUME("Resume", SessionEvent.TogglePause),
    SKIP("Skip", SessionEvent.Skip),
}

fun controlsFor(s: SessionState): List<Control> = when {
    s.phase == Phase.FINISHED -> emptyList()
    s.step is Step.Work -> listOf(Control.DONE)
    s.phase == Phase.PAUSED -> listOf(Control.RESUME, Control.SKIP)
    else -> listOf(Control.PAUSE, Control.SKIP)
}

/** The foreground service (notification, wake lock, media button) runs only while a workout is in progress. */
fun serviceShouldRun(s: SessionState?): Boolean = s != null && s.phase != Phase.FINISHED

/** A workout starts when none is running (so a double-tapped START starts once) or the last one has finished. */
fun canStart(current: SessionState?): Boolean = current == null || current.phase == Phase.FINISHED
