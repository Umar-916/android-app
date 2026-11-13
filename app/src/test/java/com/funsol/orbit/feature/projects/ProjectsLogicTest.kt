package com.funsol.orbit.feature.projects

import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProjectsLogicTest {
    private val today = LocalDate.of(2024, 3, 10)

    private fun project(id: String, name: String = id, status: ProjectStatus = ProjectStatus.ACTIVE) =
        ProjectEntity(id = id, name = name, status = status, createdAt = 0, updatedAt = 0)

    private fun task(
        id: String,
        projectId: String?,
        status: TaskStatus = TaskStatus.TODO,
        due: LocalDate? = null,
        createdAt: Long = 0,
    ) = TaskEntity(
        id = id,
        title = id,
        status = status,
        projectId = projectId,
        dueDate = due?.toEpochDayInt(),
        createdAt = createdAt,
        updatedAt = 0,
    )

    @Test
    fun filterMatchesStatus() {
        assertTrue(ProjectsLogic.matches(ProjectFilter.ALL, ProjectStatus.ARCHIVED))
        assertTrue(ProjectsLogic.matches(ProjectFilter.ACTIVE, ProjectStatus.ACTIVE))
        assertFalse(ProjectsLogic.matches(ProjectFilter.ACTIVE, ProjectStatus.ON_HOLD))
        assertTrue(ProjectsLogic.matches(ProjectFilter.ON_HOLD, ProjectStatus.ON_HOLD))
        assertTrue(ProjectsLogic.matches(ProjectFilter.COMPLETED, ProjectStatus.COMPLETED))
        assertTrue(ProjectsLogic.matches(ProjectFilter.ARCHIVED, ProjectStatus.ARCHIVED))
    }

    @Test
    fun buildItemsGroupsTasksPerProjectAndSortsByName() {
        val projects = listOf(project("b", "beta"), project("a", "Alpha"), project("c", "gamma", ProjectStatus.ARCHIVED))
        val tasks = listOf(
            task("t1", "a", TaskStatus.COMPLETED),
            task("t2", "a", TaskStatus.TODO),
            task("t3", "b", TaskStatus.COMPLETED),
            task("t4", null),
            task("t5", "a", TaskStatus.ARCHIVED), // archived tasks are not counted
        )
        val items = ProjectsLogic.buildItems(projects, tasks, ProjectFilter.ACTIVE)
        assertEquals(listOf("a", "b"), items.map { it.project.id })
        assertEquals(1, items[0].progress.done)
        assertEquals(2, items[0].progress.total)
        assertEquals(1, items[1].progress.done)
        assertEquals(1, items[1].progress.total)
    }

    @Test
    fun projectWithoutTasksHasZeroProgress() {
        val items = ProjectsLogic.buildItems(listOf(project("a")), emptyList(), ProjectFilter.ALL)
        assertEquals(0, items.single().progress.total)
        assertEquals(0f, items.single().progress.fraction, 0f)
    }

    @Test
    fun statsCountEveryBucket() {
        val tasks = listOf(
            task("1", "p", TaskStatus.COMPLETED),
            task("2", "p", TaskStatus.IN_PROGRESS, due = today.minusDays(1)),
            task("3", "p", TaskStatus.TODO, due = today), // due today is not overdue
            task("4", "p", TaskStatus.TODO, due = today.minusDays(5)),
            task("5", "p", TaskStatus.COMPLETED, due = today.minusDays(5)), // completed is never overdue
            task("6", "p", TaskStatus.ARCHIVED),
        )
        val stats = ProjectsLogic.stats(tasks, today)
        assertEquals(5, stats.total)
        assertEquals(2, stats.completed)
        assertEquals(1, stats.inProgress)
        assertEquals(2, stats.todo)
        assertEquals(2, stats.overdue)
        assertEquals(40, stats.percent)
    }

    @Test
    fun statsOfEmptyProjectAreZero() {
        val stats = ProjectsLogic.stats(emptyList(), today)
        assertEquals(ProjectStats(0, 0, 0, 0, 0, 0), stats)
    }

    @Test
    fun sortTasksPutsOpenTasksFirstByDueDate() {
        val tasks = listOf(
            task("done", "p", TaskStatus.COMPLETED, due = today.minusDays(9)),
            task("undated", "p"),
            task("late", "p", due = today.plusDays(5)),
            task("soon", "p", due = today.plusDays(1)),
        )
        assertEquals(listOf("soon", "late", "undated", "done"), ProjectsLogic.sortTasks(tasks).map { it.id })
    }

    @Test
    fun dateRangeValidation() {
        assertTrue(ProjectsLogic.isDateRangeValid(null, null))
        assertTrue(ProjectsLogic.isDateRangeValid(10, null))
        assertTrue(ProjectsLogic.isDateRangeValid(null, 10))
        assertTrue(ProjectsLogic.isDateRangeValid(10, 10))
        assertTrue(ProjectsLogic.isDateRangeValid(10, 11))
        assertFalse(ProjectsLogic.isDateRangeValid(11, 10))
    }
}
