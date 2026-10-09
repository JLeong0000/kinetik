package dev.kinetik.plan

import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.model.OverrideScope
import dev.kinetik.model.Workout

data class PlannedSet(
    val circuit: Int,
    val slot: Int,
    val name: String,
    val kind: ExerciseKind,
    val reps: Int?,
    val weightKg: Double?,
    val eachSide: Boolean,
    val maxBreakSec: Int?,
    val repeat: Int,
    val repeatCount: Int,
    val overridden: Boolean,
)

enum class RestKind { EXERCISE, CIRCUIT }

sealed interface Step {
    data class Work(val set: PlannedSet) : Step
    data class Rest(val seconds: Int, val kind: RestKind, val circuit: Int) : Step
}

fun repsFor(start: Int, circuit: Int, drop: Int): Int = maxOf(1, start - circuit * drop)

/** The exercises that run in [circuit], each paired with whether it came from an override. */
internal fun circuitExercises(w: Workout, circuit: Int): List<Pair<Exercise, Boolean>> {
    w.overrides.firstOrNull { it.circuit == circuit && it.scope == OverrideScope.WHOLE }
        ?.let { whole -> return whole.exercises.map { it to true } }
    return w.exercises.mapIndexed { slot, e ->
        val swap = w.overrides.firstOrNull { it.circuit == circuit && it.scope == OverrideScope.SLOT && it.slot == slot }
        if (swap != null) swap.exercises.first() to true else e to false
    }
}

fun buildPlan(w: Workout): List<Step> {
    val steps = mutableListOf<Step>()
    for (c in 0 until w.circuits) {
        val list = circuitExercises(w, c)
        list.forEachIndexed { slot, (e, overridden) ->
            val reps = when {
                e.kind != ExerciseKind.REPS -> null
                overridden -> e.startReps
                else -> repsFor(e.startReps, c, w.repDrop)
            }
            val count = e.sets.coerceAtLeast(1)
            repeat(count) { r ->
                steps += Step.Work(
                    PlannedSet(c, slot, e.name, e.kind, reps, e.weightKg, e.eachSide, e.maxBreakSec, r, count, overridden),
                )
            }
            val lastInCircuit = slot == list.lastIndex
            val seconds = if (lastInCircuit) w.restCircuitSec else w.restExerciseSec
            if (seconds > 0) {
                steps += Step.Rest(seconds, if (lastInCircuit) RestKind.CIRCUIT else RestKind.EXERCISE, c)
            }
        }
    }
    if (steps.lastOrNull() is Step.Rest) steps.removeAt(steps.lastIndex)
    return steps
}

const val ESTIMATED_SET_SEC = 45

fun estimateSeconds(steps: List<Step>): Int = steps.sumOf {
    when (it) {
        is Step.Work -> ESTIMATED_SET_SEC
        is Step.Rest -> it.seconds
    }
}
