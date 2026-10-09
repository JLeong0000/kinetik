package dev.kinetik.model

import kotlinx.serialization.json.Json

/** Workout <-> JSON string, used to keep the editor's unsaved draft across activity recreation. */
object WorkoutCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    fun encode(w: Workout): String = json.encodeToString(Workout.serializer(), w)
    fun decode(s: String): Workout = json.decodeFromString(Workout.serializer(), s)
}
