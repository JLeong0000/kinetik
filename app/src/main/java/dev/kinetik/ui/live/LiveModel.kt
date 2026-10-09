package dev.kinetik.ui.live

import dev.kinetik.model.ExerciseKind
import dev.kinetik.plan.PlannedSet
import dev.kinetik.plan.Step
import dev.kinetik.session.Phase
import dev.kinetik.session.SessionState
import dev.kinetik.session.countdown
import dev.kinetik.session.stopwatch

enum class RowStatus { DONE, NOW, NEXT }

data class QueueRow(val reps: String, val name: String, val status: RowStatus, val restAfter: String?)

data class LiveModel(
    val workoutName: String,
    val circuitNumber: Int,
    val circuits: Int,
    val timeLeft: String,
    val isRest: Boolean,
    val paused: Boolean,
    val name: String,
    val big: String,
    val bigIsClock: Boolean,
    /** "MAX" rather than a rep count; drawn smaller so it fits the ring on one line. */
    val bigIsWord: Boolean,
    val unit: String?,
    val sub: String,
    val tags: List<String>,
    val beepZone: Boolean,
    val restFraction: Float,
    val segments: Int,
    val activeSegment: Int,
    val nextName: String?,
    val nextReps: String?,
    val canBreak: Boolean,
    val breakLeft: String?,
    val queue: List<QueueRow>,
)

private fun repsLabel(p: PlannedSet): String = when (p.kind) {
    ExerciseKind.REPS -> "${p.reps}"
    ExerciseKind.MAX_REPS -> "MAX"
    ExerciseKind.MAX_TIME -> "MAX·T"
}

private fun kg(w: Double) = if (w % 1.0 == 0.0) "${w.toInt()} kg" else "$w kg"

fun liveModel(s: SessionState): LiveModel {
    val circuit = s.circuit
    val inCircuit = s.steps.withIndex().filter { (_, st) -> st is Step.Work && st.set.circuit == circuit }
    val slots = inCircuit.map { (it.value as Step.Work).set.slot }.distinct()

    val queue = slots.map { slot ->
        val idx = inCircuit.filter { (it.value as Step.Work).set.slot == slot }.map { it.index }
        val first = (s.steps[idx.first()] as Step.Work).set
        val status = when {
            s.work != null && s.index in idx -> RowStatus.NOW
            idx.last() < s.index -> RowStatus.DONE
            else -> RowStatus.NEXT
        }
        val rest = (s.steps.getOrNull(idx.last() + 1) as? Step.Rest)?.let { countdown(it.seconds * 1000L) }
        QueueRow(repsLabel(first), first.name, status, rest)
    }

    val work = s.work
    val next = s.nextWork()
    val restStep = s.step as? Step.Rest
    return LiveModel(
        workoutName = s.workoutName,
        circuitNumber = circuit + 1,
        circuits = s.circuits,
        timeLeft = countdown(s.timeLeftMs()),
        isRest = restStep != null,
        paused = s.phase == Phase.PAUSED,
        name = work?.name ?: "Rest",
        big = when {
            restStep != null -> countdown(s.restRemainingMs)
            work?.kind == ExerciseKind.MAX_TIME -> stopwatch(s.stepElapsedMs)
            work?.kind == ExerciseKind.MAX_REPS -> "MAX"
            else -> "${work?.reps ?: ""}"
        },
        bigIsClock = restStep != null || work?.kind == ExerciseKind.MAX_TIME,
        bigIsWord = work?.kind == ExerciseKind.MAX_REPS,
        unit = if (work?.kind == ExerciseKind.REPS) "reps" else null,
        sub = work?.let { "Ex ${slots.indexOf(it.slot) + 1} of ${slots.size}" } ?: "Rest",
        tags = buildList {
            work?.weightKg?.let { add(kg(it)) }
            if (work != null && work.repeatCount > 1) add("${work.repeat + 1} of ${work.repeatCount}")
        },
        beepZone = restStep != null && s.restRemainingMs <= 5_000,
        restFraction = restStep?.let { s.restRemainingMs / (it.seconds * 1000f) } ?: 0f,
        segments = slots.size,
        activeSegment = work?.let { slots.indexOf(it.slot) } ?: -1,
        nextName = next?.name,
        nextReps = next?.let(::repsLabel),
        canBreak = work?.maxBreakSec != null,
        breakLeft = s.breakRemainingMs?.let(::countdown),
        queue = queue,
    )
}
