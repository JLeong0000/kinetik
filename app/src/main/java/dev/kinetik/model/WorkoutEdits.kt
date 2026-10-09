package dev.kinetik.model

fun Workout.upsertExercise(e: Exercise): Workout {
    val i = exercises.indexOfFirst { it.id == e.id }
    return copy(exercises = if (i < 0) exercises + e else exercises.toMutableList().also { it[i] = e })
}

fun Workout.moveExercise(from: Int, to: Int): Workout {
    val order = exercises.indices.toList().moved(from, to) // order[newIndex] = oldIndex
    return copy(
        exercises = order.map { exercises[it] },
        overrides = overrides.map { o -> if (o.slot == null) o else o.copy(slot = order.indexOf(o.slot)) },
    )
}

fun Workout.deleteExercise(index: Int): Workout = copy(
    exercises = exercises.filterIndexed { i, _ -> i != index },
    overrides = overrides
        .filterNot { it.scope == OverrideScope.SLOT && it.slot == index }
        .map { o -> if (o.slot != null && o.slot > index) o.copy(slot = o.slot - 1) else o },
)

fun Workout.setCircuits(n: Int): Workout =
    copy(circuits = n, overrides = overrides.filter { it.circuit < n })

fun Workout.upsertOverride(o: CircuitOverride): Workout {
    val i = overrides.indexOfFirst { it.id == o.id }
    val list = if (i < 0) overrides + o else overrides.toMutableList().also { it[i] = o }
    return copy(overrides = list.sortedWith(compareBy({ it.circuit }, { it.slot ?: -1 })))
}

fun Workout.deleteOverride(id: String): Workout = copy(overrides = overrides.filterNot { it.id == id })

fun Workout.validate(): List<String> = buildList {
    if (name.isBlank()) add("Name can't be empty")
    if (circuits < 1) add("Needs at least 1 circuit")
    if (exercises.isEmpty()) add("Add at least one exercise")
    val all = exercises + overrides.flatMap { it.exercises }
    if (all.any { it.name.isBlank() }) add("Every exercise needs a name")
    if (all.any { it.kind == ExerciseKind.REPS && it.startReps < 1 }) add("Reps must be at least 1")
    if (all.any { it.sets < 1 }) add("Sets must be at least 1")
    for (o in overrides) {
        val c = o.circuit + 1
        if (o.circuit !in 0 until circuits) add("Override for circuit $c is past the last circuit")
        if (o.exercises.isEmpty()) add("Circuit $c override has no exercises")
        if (o.scope == OverrideScope.SLOT && (o.slot == null || o.slot !in exercises.indices)) {
            add("Override for circuit $c points at a missing exercise")
        }
    }
    overrides.groupBy { it.circuit }
        .filter { (_, os) ->
            val wholes = os.count { it.scope == OverrideScope.WHOLE }
            wholes > 1 || (wholes == 1 && os.size > 1) ||
                os.filter { it.scope == OverrideScope.SLOT }.groupBy { it.slot }.any { it.value.size > 1 }
        }
        .keys.sorted()
        .forEach { add("Circuit ${it + 1} has conflicting overrides") }
}
