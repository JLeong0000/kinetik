package dev.kinetik.plan

import dev.kinetik.cues.announce
import dev.kinetik.model.Block
import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.model.Workout
import dev.kinetik.model.WorkoutType
import dev.kinetik.model.addBlock
import dev.kinetik.model.deleteBlock
import dev.kinetik.model.deleteBlockExercise
import dev.kinetik.model.moveBlock
import dev.kinetik.model.moveBlockExercise
import dev.kinetik.model.newRegularWorkout
import dev.kinetik.model.renameBlock
import dev.kinetik.model.upsertBlockExercise
import dev.kinetik.model.summary
import dev.kinetik.model.validate
import dev.kinetik.session.SessionState
import dev.kinetik.session.notificationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularTest {
    private val pushDay = Workout(
        name = "Push day", type = WorkoutType.REGULAR, circuits = 1, repDrop = 0, restExerciseSec = 180, restCircuitSec = 0,
        exercises = emptyList(),
        blocks = listOf(
            Block(name = "Warm-up", exercises = listOf(Exercise(name = "Band pull aparts", startReps = 15, sets = 2, restSec = 30))),
            Block(name = "Empty", exercises = emptyList()),
            Block(name = "Main", exercises = listOf(
                Exercise(name = "Bench press", startReps = 8, sets = 3, restSec = 120),
                Exercise(name = "Row", startReps = 10, sets = 3, restSec = 90),
            )),
        ),
    )

    private fun describe(step: Step): String = when (step) {
        is Step.Rest -> "rest ${step.seconds}"
        is Step.Work -> with(step.set) {
            val what = when (kind) {
                ExerciseKind.REPS -> "$reps"
                ExerciseKind.MAX_REPS -> "MAX"
                ExerciseKind.MAX_TIME -> "MAX-TIME"
            }
            "${circuit + 1}.${slot + 1} $name $what (${repeat + 1}/$repeatCount)"
        }
    }

    @Test fun straightSetsWithSetAndExerciseRests() {
        val expected = listOf(
            "1.1 Band pull aparts 15 (1/2)", "rest 30", "1.1 Band pull aparts 15 (2/2)", "rest 180",
            "2.1 Bench press 8 (1/3)", "rest 120", "2.1 Bench press 8 (2/3)", "rest 120", "2.1 Bench press 8 (3/3)", "rest 180",
            "2.2 Row 10 (1/3)", "rest 90", "2.2 Row 10 (2/3)", "rest 90", "2.2 Row 10 (3/3)",
        )
        assertEquals(expected, buildPlan(pushDay).map(::describe))
    }

    @Test fun globalSetRestOverridesEachExercise() {
        val rests = buildPlan(pushDay.copy(setRestSec = 60)).filterIsInstance<Step.Rest>().map { it.seconds }
        assertEquals(listOf(60, 180, 60, 60, 180, 60, 60), rests)
    }

    @Test fun sessionSaysBlockNotCircuit() {
        val s = SessionState.start(pushDay)
        assertEquals("Block", s.section)
        assertEquals(2, s.circuits) // the empty block doesn't count
        assertEquals("Push day · Block 1/2" to "Band pull aparts · 15 (1/2)", notificationText(s))
    }

    @Test fun announcesTheSet() {
        val bench = (buildPlan(pushDay)[4] as Step.Work).set
        assertEquals("Bench press, 8, set 1 of 3", announce(bench))
    }

    @Test fun validation() {
        assertEquals(emptyList<String>(), pushDay.validate())
        val noExercises = pushDay.copy(blocks = listOf(Block(name = "Main", exercises = emptyList())))
        assertTrue("Add at least one exercise" in noExercises.validate())
        assertTrue("Every block needs a name" in pushDay.renameBlock(pushDay.blocks[0].id, " ").validate())
    }

    @Test fun newRegularWorkoutIsValidAndHasOneBlock() {
        val w = newRegularWorkout()
        assertEquals(WorkoutType.REGULAR, w.type)
        assertEquals(1, w.blocks.size)
        assertEquals(emptyList<String>(), w.validate())
    }

    @Test fun blockEdits() {
        var w = pushDay.addBlock()
        assertEquals("Block 4", w.blocks.last().name)
        val main = w.blocks[2]
        w = w.moveBlock(main.id, -2)
        assertEquals(listOf("Main", "Warm-up", "Empty", "Block 4"), w.blocks.map { it.name })
        w = w.moveBlockExercise(main.id, 0, 1)
        assertEquals(listOf("Row", "Bench press"), w.blocks[0].exercises.map { it.name })
        val row = w.blocks[0].exercises[0]
        w = w.upsertBlockExercise(main.id, row.copy(sets = 5))
        assertEquals(5, w.blocks[0].exercises[0].sets)
        w = w.upsertBlockExercise(main.id, Exercise(name = "Dips"))
        assertEquals(3, w.blocks[0].exercises.size)
        w = w.deleteBlockExercise(main.id, row.id)
        assertEquals(listOf("Bench press", "Dips"), w.blocks[0].exercises.map { it.name })
        w = w.deleteBlock(w.blocks.last().id)
        assertEquals(3, w.blocks.size)
    }

    @Test fun summaryUsesSingularForOne() {
        assertEquals("1 block · 1 exercise", newRegularWorkout().summary())
        assertEquals("2 blocks · 3 exercises", pushDay.summary())
        assertEquals("5 circuits · 4 exercises", dev.kinetik.model.Seed.pull().summary())
    }

    @Test fun oneSetShowsNoSetRest() {
        assertEquals("1 × 10", dev.kinetik.ui.components.setsLabel(Exercise(name = "X", startReps = 10, sets = 1), null))
        assertEquals("3 × 10 · 1:30", dev.kinetik.ui.components.setsLabel(Exercise(name = "X", startReps = 10, sets = 3), null))
    }
}
