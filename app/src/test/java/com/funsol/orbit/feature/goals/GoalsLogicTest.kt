package com.funsol.orbit.feature.goals

import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsLogicTest {
    private fun goal(
        id: String,
        status: GoalStatus = GoalStatus.ACTIVE,
        targetDate: Int? = null,
        updatedAt: Long = 0,
        title: String = id,
    ) = GoalEntity(id = id, title = title, status = status, targetDate = targetDate, createdAt = 0, updatedAt = updatedAt)

    @Test
    fun deadlineKinds() {
        assertEquals(DeadlineKind.NONE, GoalsLogic.deadline(goal("a"), 100).kind)
        assertEquals(Deadline(DeadlineKind.DAYS_LEFT, 5), GoalsLogic.deadline(goal("a", targetDate = 105), 100))
        assertEquals(DeadlineKind.TODAY, GoalsLogic.deadline(goal("a", targetDate = 100), 100).kind)
        assertEquals(Deadline(DeadlineKind.OVERDUE, 3), GoalsLogic.deadline(goal("a", targetDate = 97), 100))
    }

    @Test
    fun finishedGoalsNeverShowADeadline() {
        assertEquals(DeadlineKind.NONE, GoalsLogic.deadline(goal("a", GoalStatus.COMPLETED, targetDate = 1), 100).kind)
        assertEquals(DeadlineKind.NONE, GoalsLogic.deadline(goal("a", GoalStatus.ARCHIVED, targetDate = 1), 100).kind)
    }

    @Test
    fun sectionsSplitByStatusAndSortByDeadline() {
        val sections = GoalsLogic.sections(
            listOf(
                goal("undated", title = "Undated"),
                goal("late", targetDate = 300),
                goal("soon", targetDate = 200),
                goal("old", GoalStatus.COMPLETED, updatedAt = 1),
                goal("new", GoalStatus.COMPLETED, updatedAt = 9),
                goal("arch", GoalStatus.ARCHIVED),
            ),
        )
        assertEquals(listOf("soon", "late", "undated"), sections.inProgress.map { it.id })
        assertEquals(listOf("new", "old"), sections.completed.map { it.id })
        assertEquals(listOf("arch"), sections.archived.map { it.id })
    }

    @Test
    fun dateRangeValidation() {
        assertTrue(GoalsLogic.isDateRangeValid(null, null))
        assertTrue(GoalsLogic.isDateRangeValid(5, 5))
        assertTrue(GoalsLogic.isDateRangeValid(5, null))
        assertFalse(GoalsLogic.isDateRangeValid(6, 5))
    }
}
