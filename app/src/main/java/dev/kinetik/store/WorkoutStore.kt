package dev.kinetik.store

import dev.kinetik.model.Library
import dev.kinetik.model.Seed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Keeps the whole library in one JSON file. Writes are atomic (temp file + rename).
 * An unreadable file is kept as workouts.bad.json and replaced by the seed data.
 */
class WorkoutStore(private val dir: File, private val seed: () -> Library = Seed::library) {
    private val file = File(dir, "workouts.json")

    var recoveredFromCorruption = false
        private set

    private val _library = MutableStateFlow(load())
    val library: StateFlow<Library> = _library.asStateFlow()

    @Synchronized
    fun update(transform: (Library) -> Library) {
        val next = transform(_library.value)
        if (next == _library.value) return
        write(next)
        _library.value = next
    }

    fun clearRecoveredFlag() {
        recoveredFromCorruption = false
    }

    private fun load(): Library {
        if (!file.exists()) return seed().also(::write)
        return try {
            json.decodeFromString(Library.serializer(), file.readText())
        } catch (e: Exception) {
            val bad = File(dir, "workouts.bad.json")
            bad.delete()
            file.renameTo(bad)
            recoveredFromCorruption = true
            seed().also(::write)
        }
    }

    private fun write(lib: Library) {
        dir.mkdirs()
        val tmp = File(dir, "workouts.json.tmp")
        tmp.writeText(json.encodeToString(Library.serializer(), lib))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    companion object {
        val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }
}
