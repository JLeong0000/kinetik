package dev.kinetik.model

import dev.kinetik.model.ExerciseKind.MAX_REPS
import dev.kinetik.model.ExerciseKind.MAX_TIME
import dev.kinetik.model.OverrideScope.SLOT
import dev.kinetik.model.OverrideScope.WHOLE

/** The owner's training plan, loaded on first launch and by "Reset workouts". */
object Seed {
    fun library() = Library(
        groups = listOf(Group(name = "Calisthenics split", workouts = listOf(pull(), push(), legs(), abs()))),
    )

    fun pull() = Workout(
        name = "Pull", circuits = 5, repDrop = 2, restExerciseSec = 180, restCircuitSec = 240,
        exercises = listOf(
            Exercise(name = "Pull ups", startReps = 8),
            Exercise(name = "Chin ups", startReps = 8),
            Exercise(name = "Inverted pull up rows", startReps = 9),
            Exercise(name = "Inverted chin up rows", startReps = 9),
        ),
        overrides = listOf(
            CircuitOverride(circuit = 3, scope = SLOT, slot = 0,
                exercises = listOf(Exercise(name = "Pull up negatives", kind = MAX_TIME, sets = 4))),
            CircuitOverride(circuit = 3, scope = SLOT, slot = 1,
                exercises = listOf(Exercise(name = "Chin up negatives", kind = MAX_TIME, sets = 4))),
            CircuitOverride(circuit = 4, scope = WHOLE,
                exercises = listOf(Exercise(name = "Alternating chin up / pull up negatives", kind = MAX_REPS))),
        ),
    )

    fun push() = Workout(
        name = "Push", circuits = 6, repDrop = 1, restExerciseSec = 180, restCircuitSec = 240,
        exercises = listOf(
            Exercise(name = "Dips", startReps = 13, weightKg = 10.0),
            Exercise(name = "Pike push ups", startReps = 11),
            Exercise(name = "Archer push ups", startReps = 12),
            Exercise(name = "Pseudo push ups", startReps = 11),
        ),
        overrides = listOf(
            CircuitOverride(circuit = 4, scope = WHOLE,
                exercises = listOf(Exercise(name = "Dips", kind = MAX_REPS, maxBreakSec = 10))),
            CircuitOverride(circuit = 5, scope = WHOLE,
                exercises = listOf(Exercise(name = "Push ups", kind = MAX_REPS, maxBreakSec = 10))),
        ),
    )

    fun legs() = Workout(
        name = "Legs", circuits = 6, repDrop = 1, restExerciseSec = 180, restCircuitSec = 240,
        exercises = listOf(
            Exercise(name = "Shrimp squats", startReps = 11),
            Exercise(name = "Hand assisted Nordic curls", startReps = 10),
            Exercise(name = "Hand assisted pistol squats", startReps = 11),
            Exercise(name = "Single calf raises", startReps = 15),
        ),
        overrides = listOf(
            CircuitOverride(circuit = 4, scope = WHOLE,
                exercises = listOf(Exercise(name = "Hand assisted Nordic curls", kind = MAX_REPS, maxBreakSec = 10))),
            CircuitOverride(circuit = 5, scope = WHOLE,
                exercises = listOf(Exercise(name = "Hand assisted pistol squats", kind = MAX_REPS, maxBreakSec = 10))),
        ),
    )

    fun abs() = Workout(
        name = "Abs", circuits = 5, repDrop = 1, restExerciseSec = 180, restCircuitSec = 180,
        exercises = listOf(
            Exercise(name = "Hanging knee twists", startReps = 8),
            Exercise(name = "Hanging leg raises", startReps = 9),
        ),
        overrides = listOf(
            CircuitOverride(circuit = 4, scope = SLOT, slot = 0,
                exercises = listOf(Exercise(name = "Hanging knee twists", kind = MAX_REPS))),
            CircuitOverride(circuit = 4, scope = SLOT, slot = 1,
                exercises = listOf(Exercise(name = "Hanging leg raises", kind = MAX_REPS))),
        ),
    )
}
