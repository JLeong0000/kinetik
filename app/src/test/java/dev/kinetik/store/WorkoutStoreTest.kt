package dev.kinetik.store

import dev.kinetik.model.Seed
import dev.kinetik.model.Settings
import dev.kinetik.model.allWorkouts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WorkoutStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun firstLaunchSeedsAndWritesFile() {
        val dir = tmp.newFolder()
        val store = WorkoutStore(dir)
        assertEquals(listOf("Pull", "Push", "Legs", "Abs"), store.library.value.allWorkouts().map { it.name })
        assertTrue(File(dir, "workouts.json").exists())
        assertFalse(store.recoveredFromCorruption)
    }

    @Test fun updatesPersistAcrossInstances() {
        val dir = tmp.newFolder()
        WorkoutStore(dir).update { it.copy(settings = Settings(voiceOn = false)) }
        assertFalse(WorkoutStore(dir).library.value.settings.voiceOn)
    }

    @Test fun corruptFileIsKeptAndSeedLoaded() {
        val dir = tmp.newFolder()
        File(dir, "workouts.json").writeText("{ not json")
        val store = WorkoutStore(dir)
        assertTrue(store.recoveredFromCorruption)
        assertEquals("{ not json", File(dir, "workouts.bad.json").readText())
        assertEquals(4, store.library.value.allWorkouts().size)
        store.clearRecoveredFlag()
        assertFalse(store.recoveredFromCorruption)
    }

    @Test fun unknownFieldsAreIgnored() {
        val dir = tmp.newFolder()
        File(dir, "workouts.json").writeText("""{"groups":[],"futureField":1}""")
        val store = WorkoutStore(dir)
        assertFalse(store.recoveredFromCorruption)
        assertEquals(0, store.library.value.groups.size)
    }

    @Test fun seedHelperIsUsed() {
        val store = WorkoutStore(tmp.newFolder()) { Seed.library().copy(lastCompletedWorkoutId = "x") }
        assertEquals("x", store.library.value.lastCompletedWorkoutId)
    }
}
