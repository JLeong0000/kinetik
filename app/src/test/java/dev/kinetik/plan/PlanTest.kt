package dev.kinetik.plan

import dev.kinetik.model.Exercise
import dev.kinetik.model.ExerciseKind
import dev.kinetik.model.Seed
import dev.kinetik.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTest {
    /** Compact, readable form of a step for golden comparisons. */
    private fun describe(step: Step): String = when (step) {
        is Step.Rest -> "rest ${step.seconds}"
        is Step.Work -> with(step.set) {
            val what = when (kind) {
                ExerciseKind.REPS -> "$reps"
                ExerciseKind.MAX_REPS -> "MAX"
                ExerciseKind.MAX_TIME -> "MAX-TIME"
            }
            val rep = if (repeatCount > 1) " (${repeat + 1}/$repeatCount)" else ""
            "${circuit + 1}.${slot + 1} $name $what$rep"
        }
    }

    private fun plan(w: Workout) = buildPlan(w).map(::describe)

    @Test fun pullGolden() {
        val expected = listOf(
            "1.1 Pull ups 8", "rest 180", "1.2 Chin ups 8", "rest 180",
            "1.3 Inverted pull up rows 9", "rest 180", "1.4 Inverted chin up rows 9", "rest 240",
            "2.1 Pull ups 6", "rest 180", "2.2 Chin ups 6", "rest 180",
            "2.3 Inverted pull up rows 7", "rest 180", "2.4 Inverted chin up rows 7", "rest 240",
            "3.1 Pull ups 4", "rest 180", "3.2 Chin ups 4", "rest 180",
            "3.3 Inverted pull up rows 5", "rest 180", "3.4 Inverted chin up rows 5", "rest 240",
            "4.1 Pull up negatives MAX-TIME (1/4)", "4.1 Pull up negatives MAX-TIME (2/4)",
            "4.1 Pull up negatives MAX-TIME (3/4)", "4.1 Pull up negatives MAX-TIME (4/4)", "rest 180",
            "4.2 Chin up negatives MAX-TIME (1/4)", "4.2 Chin up negatives MAX-TIME (2/4)",
            "4.2 Chin up negatives MAX-TIME (3/4)", "4.2 Chin up negatives MAX-TIME (4/4)", "rest 180",
            "4.3 Inverted pull up rows 3", "rest 180", "4.4 Inverted chin up rows 3", "rest 240",
            "5.1 Alternating chin up / pull up negatives MAX",
        )
        assertEquals(expected, plan(Seed.pull()))
    }

    @Test fun pushGolden() {
        val p = plan(Seed.push())
        assertEquals(35, p.size)
        assertEquals(listOf("1.1 Dips 13", "rest 180", "1.2 Pike push ups 11", "rest 180",
            "1.3 Archer push ups 12", "rest 180", "1.4 Pseudo push ups 11", "rest 240"), p.take(8))
        assertEquals(listOf("4.1 Dips 10", "rest 180", "4.2 Pike push ups 8", "rest 180",
            "4.3 Archer push ups 9", "rest 180", "4.4 Pseudo push ups 8", "rest 240",
            "5.1 Dips MAX", "rest 240", "6.1 Push ups MAX"), p.takeLast(11))
        val maxDips = (buildPlan(Seed.push())[32] as Step.Work).set
        assertEquals(10, maxDips.maxBreakSec)
        assertTrue(maxDips.overridden)
    }

    @Test fun legsGolden() {
        val steps = buildPlan(Seed.legs())
        assertEquals(35, steps.size)
        assertEquals("1.1 Shrimp squats 11", describe(steps[0]))
        assertEquals("6.1 Hand assisted pistol squats MAX", describe(steps.last()))
    }

    @Test fun absGolden() {
        val expected = listOf(
            "1.1 Hanging knee twists 8", "rest 180", "1.2 Hanging leg raises 9", "rest 180",
            "2.1 Hanging knee twists 7", "rest 180", "2.2 Hanging leg raises 8", "rest 180",
            "3.1 Hanging knee twists 6", "rest 180", "3.2 Hanging leg raises 7", "rest 180",
            "4.1 Hanging knee twists 5", "rest 180", "4.2 Hanging leg raises 6", "rest 180",
            "5.1 Hanging knee twists MAX", "rest 180", "5.2 Hanging leg raises MAX",
        )
        assertEquals(expected, plan(Seed.abs()))
    }

    @Test fun repsNeverDropBelowOne() {
        val w = Workout(name = "W", circuits = 3, repDrop = 2, restExerciseSec = 0, restCircuitSec = 30,
            exercises = listOf(Exercise(name = "X", startReps = 3)))
        assertEquals(listOf("1.1 X 3", "rest 30", "2.1 X 1", "rest 30", "3.1 X 1"), plan(w))
    }

    @Test fun zeroSecondRestsAreLeftOut() {
        val w = Workout(name = "W", circuits = 1, repDrop = 0, restExerciseSec = 0, restCircuitSec = 0,
            exercises = listOf(Exercise(name = "A", startReps = 5), Exercise(name = "B", startReps = 5)))
        assertEquals(listOf("1.1 A 5", "1.2 B 5"), plan(w))
    }

    @Test fun estimateAddsRestsAndSets() {
        val w = Workout(name = "W", circuits = 2, repDrop = 0, restExerciseSec = 60, restCircuitSec = 120,
            exercises = listOf(Exercise(name = "A"), Exercise(name = "B")))
        // 4 sets × 45 + rests 60 + 120 + 60 = 420
        assertEquals(420, estimateSeconds(buildPlan(w)))
    }

    @Test fun pullGridShowsDropsSwapsAndWholeCircuit() {
        val g = planGrid(Seed.pull())
        assertEquals(5, g.size)
        assertEquals(listOf(GridCell.Reps(8), GridCell.Reps(8), GridCell.Reps(9), GridCell.Reps(9)), g[0].cells)
        assertEquals(GridCell.Swapped("MAX·T×4", "Pull up negatives"), g[3].cells[0])
        assertEquals(GridCell.Reps(3), g[3].cells[2])
        assertEquals("Max Alternating chin up / pull up negatives", g[4].whole)
    }

    @Test fun pushGridLabelsMaxBreaks() {
        val g = planGrid(Seed.push())
        assertEquals("Max Dips · 10s break", g[4].whole)
        assertEquals("Max Push ups · 10s break", g[5].whole)
        assertEquals(GridCell.Reps(10), g[3].cells[0])
    }
}
