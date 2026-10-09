package dev.kinetik.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    private fun workout(vararg names: String) = Workout(
        name = "W", circuits = 3, repDrop = 1, restExerciseSec = 60, restCircuitSec = 120,
        exercises = names.map { Exercise(name = it) },
    )

    private fun slotOverride(circuit: Int, slot: Int) = CircuitOverride(
        circuit = circuit, scope = OverrideScope.SLOT, slot = slot,
        exercises = listOf(Exercise(name = "Neg", kind = ExerciseKind.MAX_TIME)),
    )

    @Test fun movedClampsTarget() {
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").moved(0, 9))
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(2, -4))
        assertEquals(listOf("a", "b"), listOf("a", "b").moved(1, 1))
    }

    @Test fun moveExerciseRemapsSlotOverrides() {
        val w = workout("A", "B", "C").copy(overrides = listOf(slotOverride(0, 0), slotOverride(1, 2)))
        val moved = w.moveExercise(0, 2) // B, C, A
        assertEquals(listOf("B", "C", "A"), moved.exercises.map { it.name })
        assertEquals(listOf(2, 1), moved.overrides.map { it.slot })
    }

    @Test fun deleteExerciseDropsItsOverridesAndShiftsLaterSlots() {
        val w = workout("A", "B", "C").copy(overrides = listOf(slotOverride(0, 0), slotOverride(1, 2)))
        val d = w.deleteExercise(0)
        assertEquals(listOf("B", "C"), d.exercises.map { it.name })
        assertEquals(1, d.overrides.size)
        assertEquals(1, d.overrides.single().slot)
    }

    @Test fun setCircuitsDropsOverridesPastTheEnd() {
        val w = workout("A").copy(overrides = listOf(slotOverride(0, 0), slotOverride(2, 0)))
        assertEquals(listOf(0), w.setCircuits(2).overrides.map { it.circuit })
    }

    @Test fun upsertOverrideReplacesByIdAndSorts() {
        val a = slotOverride(2, 0)
        val b = slotOverride(0, 0)
        val w = workout("A").upsertOverride(a).upsertOverride(b).upsertOverride(a.copy(circuit = 1))
        assertEquals(listOf(0, 1), w.overrides.map { it.circuit })
    }

    @Test fun validWorkoutHasNoErrors() = assertEquals(emptyList<String>(), workout("A").validate())

    @Test fun validateCatchesEachProblem() {
        assertTrue("Name can't be empty" in workout("A").copy(name = " ").validate())
        assertTrue("Needs at least 1 circuit" in workout("A").copy(circuits = 0).validate())
        assertTrue("Add at least one exercise" in workout().validate())
        assertTrue("Every exercise needs a name" in workout("").validate())
        assertTrue("Reps must be at least 1" in workout("A").upsertExercise(Exercise(name = "B", startReps = 0)).validate())
        assertTrue("Sets must be at least 1" in workout("A").upsertExercise(Exercise(name = "B", sets = 0)).validate())
        assertTrue("Override for circuit 5 is past the last circuit" in workout("A").copy(overrides = listOf(slotOverride(4, 0))).validate())
        assertTrue("Override for circuit 1 points at a missing exercise" in workout("A").copy(overrides = listOf(slotOverride(0, 3))).validate())
        val empty = CircuitOverride(circuit = 0, scope = OverrideScope.WHOLE, exercises = emptyList())
        assertTrue("Circuit 1 override has no exercises" in workout("A").copy(overrides = listOf(empty)).validate())
        val whole = CircuitOverride(circuit = 1, scope = OverrideScope.WHOLE, exercises = listOf(Exercise(name = "Max")))
        assertTrue("Circuit 2 has conflicting overrides" in workout("A").copy(overrides = listOf(whole, slotOverride(1, 0))).validate())
        assertTrue("Circuit 2 has conflicting overrides" in workout("A").copy(overrides = listOf(slotOverride(1, 0), slotOverride(1, 0))).validate())
    }

    private fun library(): Library {
        val g1 = Group(name = "G1", workouts = listOf(workout("A").copy(name = "One"), workout("A").copy(name = "Two")))
        val g2 = Group(name = "G2", workouts = listOf(workout("A").copy(name = "Three")))
        return Library(groups = listOf(g1, g2))
    }

    @Test fun upNextRotatesThroughAllWorkouts() {
        val lib = library()
        val ids = lib.allWorkouts().map { it.id }
        assertEquals("One", lib.upNext()?.name)
        assertEquals("Two", lib.copy(lastCompletedWorkoutId = ids[0]).upNext()?.name)
        assertEquals("One", lib.copy(lastCompletedWorkoutId = ids[2]).upNext()?.name)
        assertEquals("One", lib.copy(lastCompletedWorkoutId = "deleted").upNext()?.name)
        assertNull(Library(groups = emptyList()).upNext())
    }

    @Test fun moveWorkoutWithinAndBetweenGroups() {
        val lib = library()
        val two = lib.allWorkouts()[1]
        assertEquals(listOf("Two", "One"), lib.moveWorkout(two.id, -1).groups[0].workouts.map { it.name })
        val moved = lib.moveWorkoutToGroup(two.id, lib.groups[1].id)
        assertEquals(listOf("One"), moved.groups[0].workouts.map { it.name })
        assertEquals(listOf("Three", "Two"), moved.groups[1].workouts.map { it.name })
    }

    @Test fun deleteGroupOnlyWhenEmpty() {
        val lib = library().addGroup("Empty")
        assertEquals(3, lib.deleteGroup(lib.groups[0].id).groups.size)
        assertEquals(2, lib.deleteGroup(lib.groups[2].id).groups.size)
    }

    @Test fun libraryRoundTripsThroughJson() {
        val json = Json { encodeDefaults = true }
        val lib = library().copy(lastCompletedWorkoutId = "x", settings = Settings(voiceOn = false))
        val text = json.encodeToString(Library.serializer(), lib)
        assertEquals(lib, json.decodeFromString(Library.serializer(), text))
    }
}
