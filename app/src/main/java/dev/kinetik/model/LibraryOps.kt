package dev.kinetik.model

fun Library.allWorkouts(): List<Workout> = groups.flatMap { it.workouts }

fun Library.workout(id: String): Workout? = allWorkouts().firstOrNull { it.id == id }

fun Library.groupOf(workoutId: String): Group? =
    groups.firstOrNull { g -> g.workouts.any { it.id == workoutId } }

/** The workout after the last completed one, wrapping around; the first workout if none was completed. */
fun Library.upNext(): Workout? {
    val all = allWorkouts()
    if (all.isEmpty()) return null
    val last = all.indexOfFirst { it.id == lastCompletedWorkoutId }
    return if (last < 0) all.first() else all[(last + 1) % all.size]
}

fun Library.updateWorkout(w: Workout): Library =
    copy(groups = groups.map { g -> g.copy(workouts = g.workouts.map { if (it.id == w.id) w else it }) })

fun Library.addWorkout(groupId: String, w: Workout): Library =
    copy(groups = groups.map { if (it.id == groupId) it.copy(workouts = it.workouts + w) else it })

fun Library.deleteWorkout(id: String): Library =
    copy(groups = groups.map { g -> g.copy(workouts = g.workouts.filterNot { it.id == id }) })

fun Library.moveWorkout(id: String, delta: Int): Library =
    copy(groups = groups.map { g ->
        val i = g.workouts.indexOfFirst { it.id == id }
        if (i < 0) g else g.copy(workouts = g.workouts.moved(i, i + delta))
    })

fun Library.moveWorkoutToGroup(id: String, groupId: String): Library {
    val w = workout(id) ?: return this
    if (groupOf(id)?.id == groupId || groups.none { it.id == groupId }) return this
    return deleteWorkout(id).addWorkout(groupId, w)
}

fun Library.addGroup(name: String): Library =
    copy(groups = groups + Group(name = name, workouts = emptyList()))

fun Library.renameGroup(id: String, name: String): Library =
    copy(groups = groups.map { if (it.id == id) it.copy(name = name) else it })

/** Only empty groups can be deleted, so workouts are never lost by accident. */
fun Library.deleteGroup(id: String): Library =
    if (groups.firstOrNull { it.id == id }?.workouts?.isEmpty() == true) copy(groups = groups.filterNot { it.id == id })
    else this

fun Library.moveGroup(id: String, delta: Int): Library {
    val i = groups.indexOfFirst { it.id == id }
    return if (i < 0) this else copy(groups = groups.moved(i, i + delta))
}

/** Moves the element at [from] to [to], clamping [to] into range. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    val target = to.coerceIn(0, lastIndex)
    if (from == target) return this
    return toMutableList().apply { add(target, removeAt(from)) }
}
