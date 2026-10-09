package dev.kinetik.plan

import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.model.OverrideScope
import dev.kinetik.model.Workout

sealed interface GridCell {
    data class Reps(val reps: Int) : GridCell
    data class Max(val label: String) : GridCell
    data class Swapped(val label: String, val name: String) : GridCell
}

/** One circuit column. [whole] is set when the entire circuit is replaced; then [cells] is empty. */
data class GridColumn(val circuit: Int, val whole: String?, val cells: List<GridCell>)

fun shortLabel(e: Exercise, reps: Int): String {
    val base = when (e.kind) {
        ExerciseKind.REPS -> "$reps"
        ExerciseKind.MAX_REPS -> "MAX"
        ExerciseKind.MAX_TIME -> "MAX·T"
    }
    return if (e.sets > 1) "$base×${e.sets}" else base
}

fun wholeLabel(exercises: List<Exercise>): String = exercises.joinToString(" + ") { e ->
    val what = when (e.kind) {
        ExerciseKind.REPS -> "${e.startReps} ${e.name}"
        ExerciseKind.MAX_REPS -> "Max ${e.name}"
        ExerciseKind.MAX_TIME -> "Max-time ${e.name}"
    }
    what + (e.maxBreakSec?.let { " · ${it}s break" } ?: "")
}

fun planGrid(w: Workout): List<GridColumn> = (0 until w.circuits).map { c ->
    val whole = w.overrides.firstOrNull { it.circuit == c && it.scope == OverrideScope.WHOLE }
    if (whole != null) {
        GridColumn(c, wholeLabel(whole.exercises), emptyList())
    } else {
        GridColumn(c, null, w.exercises.mapIndexed { slot, e ->
            val swap = w.overrides.firstOrNull { it.circuit == c && it.scope == OverrideScope.SLOT && it.slot == slot }
            when {
                swap != null -> swap.exercises.first().let { GridCell.Swapped(shortLabel(it, it.startReps), it.name) }
                e.kind == ExerciseKind.REPS -> GridCell.Reps(repsFor(e.startReps, c, w.repDrop))
                else -> GridCell.Max(shortLabel(e, 0))
            }
        })
    }
}
