package dev.kinetik.model

fun newCircuitWorkout() = Workout(
    name = "New workout", circuits = 4, repDrop = 1, restExerciseSec = 180, restCircuitSec = 240,
    exercises = listOf(Exercise(name = "Exercise 1")),
)

fun newRegularWorkout() = Workout(
    name = "New workout", type = WorkoutType.REGULAR, circuits = 1, repDrop = 0, restExerciseSec = 180, restCircuitSec = 0,
    exercises = emptyList(),
    blocks = listOf(Block(name = "Main", exercises = listOf(Exercise(name = "Exercise 1", startReps = 10, sets = 3)))),
)

private fun Workout.mapBlock(id: String, f: (Block) -> Block): Workout =
    copy(blocks = blocks.map { if (it.id == id) f(it) else it })

fun Workout.addBlock(name: String = "Block ${blocks.size + 1}"): Workout =
    copy(blocks = blocks + Block(name = name, exercises = emptyList()))

fun Workout.renameBlock(id: String, name: String): Workout = mapBlock(id) { it.copy(name = name) }

fun Workout.moveBlock(id: String, delta: Int): Workout {
    val i = blocks.indexOfFirst { it.id == id }
    return if (i < 0) this else copy(blocks = blocks.moved(i, i + delta))
}

fun Workout.deleteBlock(id: String): Workout = copy(blocks = blocks.filterNot { it.id == id })

fun Workout.upsertBlockExercise(blockId: String, e: Exercise): Workout = mapBlock(blockId) { b ->
    val i = b.exercises.indexOfFirst { it.id == e.id }
    b.copy(exercises = if (i < 0) b.exercises + e else b.exercises.toMutableList().also { it[i] = e })
}

fun Workout.deleteBlockExercise(blockId: String, exerciseId: String): Workout =
    mapBlock(blockId) { b -> b.copy(exercises = b.exercises.filterNot { it.id == exerciseId }) }

fun Workout.moveBlockExercise(blockId: String, from: Int, to: Int): Workout =
    mapBlock(blockId) { b -> b.copy(exercises = b.exercises.moved(from, to)) }

/** Numbered sections of the workout: circuits, or the non-empty blocks of a regular workout. */
val Workout.sections: Int
    get() = if (type == WorkoutType.REGULAR) blocks.count { it.exercises.isNotEmpty() } else circuits

private fun count(n: Int, word: String) = if (n == 1) "1 $word" else "$n ${word}s"

/** e.g. "5 circuits · 4 exercises" or "1 block · 1 exercise". */
fun Workout.summary(): String = if (type == WorkoutType.REGULAR) {
    "${count(sections, "block")} · ${count(blocks.sumOf { it.exercises.size }, "exercise")}"
} else {
    "${count(circuits, "circuit")} · ${count(exercises.size, "exercise")}"
}
