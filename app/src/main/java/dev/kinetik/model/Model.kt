package dev.kinetik.model

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
enum class ExerciseKind { REPS, MAX_REPS, MAX_TIME }

@Serializable
enum class OverrideScope { SLOT, WHOLE }

/** CIRCUIT: rounds of the same exercises with rep drops. REGULAR: blocks of straight sets. */
@Serializable
enum class WorkoutType { CIRCUIT, REGULAR }

@Serializable
data class Exercise(
    val id: String = newId(),
    val name: String,
    val kind: ExerciseKind = ExerciseKind.REPS,
    val startReps: Int = 10,
    val weightKg: Double? = null,
    val sets: Int = 1,
    val maxBreakSec: Int? = null,
    /** Regular workouts: rest between this exercise's sets. */
    val restSec: Int = 90,
)

/** A named section of a regular workout; its exercises run as straight sets, in order. */
@Serializable
data class Block(
    val id: String = newId(),
    val name: String,
    val exercises: List<Exercise>,
)

/** Changes one circuit. [circuit] and [slot] are 0-based. SLOT uses exercises[0]; WHOLE runs the whole list. */
@Serializable
data class CircuitOverride(
    val id: String = newId(),
    val circuit: Int,
    val scope: OverrideScope,
    val slot: Int? = null,
    val exercises: List<Exercise>,
)

@Serializable
data class Workout(
    val id: String = newId(),
    val name: String,
    val circuits: Int,
    val repDrop: Int,
    val restExerciseSec: Int,
    val restCircuitSec: Int,
    val exercises: List<Exercise>,
    val overrides: List<CircuitOverride> = emptyList(),
    val type: WorkoutType = WorkoutType.CIRCUIT,
    /** Regular workouts only. */
    val blocks: List<Block> = emptyList(),
    /** Regular workouts: when set, the rest between sets for every exercise (overrides each exercise's own). */
    val setRestSec: Int? = null,
)

@Serializable
data class Group(
    val id: String = newId(),
    val name: String,
    val workouts: List<Workout>,
)

@Serializable
data class Settings(
    val voiceOn: Boolean = true,
    val beepVolume: Int = 80,
    val keepScreenOn: Boolean = true,
    /** A TextToSpeech voice name from `VOICE_LABELS`; null means the first one installed. */
    val voiceName: String? = null,
)

@Serializable
data class Library(
    val groups: List<Group>,
    val lastCompletedWorkoutId: String? = null,
    val settings: Settings = Settings(),
)
