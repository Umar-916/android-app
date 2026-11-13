package com.funsol.orbit.feature.search

import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.repository.SearchResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchLogicTest {
    @Test
    fun blankQueriesAreNotSearchable() {
        assertFalse(SearchLogic.isSearchable(""))
        assertFalse(SearchLogic.isSearchable("   "))
        assertTrue(SearchLogic.isSearchable(" a "))
    }

    @Test
    fun emptyResultsProduceNoSections() {
        assertTrue(SearchLogic.sectionsOf(SearchResults()).isEmpty())
    }

    @Test
    fun sectionsFollowFixedOrderAndSkipEmptyGroups() {
        val results = SearchResults(
            tasks = listOf(TaskEntity("t1", "Task", createdAt = 1, updatedAt = 1), TaskEntity("t2", "Task 2", createdAt = 1, updatedAt = 1)),
            projects = emptyList(),
            notes = listOf(NoteEntity("n1", createdAt = 1, updatedAt = 1)),
            habits = listOf(HabitEntity("h1", "Habit", startDate = 1, createdAt = 1)),
            goals = listOf(GoalEntity("g1", "Goal", createdAt = 1, updatedAt = 1)),
        )
        val sections = SearchLogic.sectionsOf(results)
        assertEquals(
            listOf(SearchSectionType.TASKS, SearchSectionType.NOTES, SearchSectionType.HABITS, SearchSectionType.GOALS),
            sections.map { it.type },
        )
        assertEquals(listOf(2, 1, 1, 1), sections.map { it.count })
        assertEquals(5, SearchUiState(SearchPhase.RESULTS, "x", sections).totalCount)
    }

    @Test
    fun debounceIsThreeHundredMilliseconds() {
        assertEquals(300L, SearchLogic.DEBOUNCE_MILLIS)
    }

    @Test
    fun projectsSectionCountsProjects() {
        val results = SearchResults(projects = listOf(ProjectEntity("p", "P", createdAt = 1, updatedAt = 1)))
        assertEquals(1, SearchLogic.sectionsOf(results).single().count)
    }
}
