package dev.kinetik.ui.home

import dev.kinetik.model.Library
import dev.kinetik.model.Seed
import dev.kinetik.model.addGroup
import dev.kinetik.model.allWorkouts
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupsTest {
    // On app start every group is collapsed except the one holding Up next.
    @Test fun onStartOnlyTheUpNextGroupIsOpen() {
        val lib = Seed.library().addGroup("Mobility")
        assertEquals(setOf(lib.groups[0].id), expandedOnStart(lib))
    }

    @Test fun upNextInAnotherGroup() {
        val base = Seed.library().addGroup("Mobility")
        val moved = base.copy(groups = listOf(base.groups[1], base.groups[0]))
        val lastId = moved.allWorkouts().last().id // Abs completed → Pull is up next, in "Calisthenics split"
        assertEquals(setOf(moved.groups[1].id), expandedOnStart(moved.copy(lastCompletedWorkoutId = lastId)))
    }

    @Test fun noWorkoutsMeansNothingOpen() = assertEquals(emptySet<String>(), expandedOnStart(Library(groups = emptyList())))
}
