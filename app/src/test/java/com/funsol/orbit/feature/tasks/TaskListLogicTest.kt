package com.funsol.orbit.feature.tasks

import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskListLogicTest {
    private val today = LocalDate.of(2026, 11, 12)
    private val todayDay = today.toEpochDay().toInt()

    private fun task(
        id: String,
        title: String = id,
        due: Int? = null,
        priority: Priority = Priority.MEDIUM,
        status: TaskStatus = TaskStatus.TODO,
        created: Long = 0L,
        description: String = "",
        projectId: String? = null,
        dueTime: Int? = null,
    ) = TaskEntity(
        id = id, title = title, description = description, priority = priority, status = status,
        createdAt = created, updatedAt = created, dueDate = due, dueTime = dueTime, projectId = projectId,
    )

    private fun ids(list: List<TaskEntity>) = list.map { it.id }

    @Test
    fun viewsMatchTaskRules() {
        val all = listOf(
            task("overdue", due = todayDay - 1),
            task("today", due = todayDay),
            task("upcoming", due = todayDay + 3),
            task("nodate"),
            task("done", status = TaskStatus.COMPLETED, due = todayDay - 5),
            task("arch", status = TaskStatus.ARCHIVED, due = todayDay),
            task("wip", status = TaskStatus.IN_PROGRESS, due = todayDay),
        )
        for (view in TaskView.entries) {
            val expected = when (view) {
                TaskView.ALL -> all.filter { !TaskRules.isArchived(it) }
                TaskView.TODAY -> all.filter { TaskRules.isDueToday(it, today) }
                TaskView.UPCOMING -> all.filter { TaskRules.isUpcoming(it, today) }
                TaskView.OVERDUE -> all.filter { TaskRules.isOverdue(it, today) }
                TaskView.COMPLETED -> all.filter { TaskRules.isCompleted(it) }
                TaskView.ARCHIVED -> all.filter { TaskRules.isArchived(it) }
            }
            assertEquals(view.name, ids(expected).toSet(), ids(all.filter { view.matches(it, today) }).toSet())
        }
        assertEquals(setOf("today", "wip"), ids(all.filter { TaskView.TODAY.matches(it, today) }).toSet())
        assertEquals(setOf("overdue"), ids(all.filter { TaskView.OVERDUE.matches(it, today) }).toSet())
    }

    @Test
    fun countsAreRealPerView() {
        val all = listOf(task("a", due = todayDay), task("b", due = todayDay - 1), task("c", status = TaskStatus.ARCHIVED))
        val counts = TaskListLogic.counts(all, today)
        assertEquals(2, counts[TaskView.ALL])
        assertEquals(1, counts[TaskView.TODAY])
        assertEquals(1, counts[TaskView.OVERDUE])
        assertEquals(1, counts[TaskView.ARCHIVED])
        assertEquals(0, counts[TaskView.COMPLETED])
    }

    @Test
    fun dueDateSortPutsNullsLastInBothDirections() {
        val list = listOf(task("none"), task("late", due = 30), task("early", due = 10))
        val asc = list.sortedWith(TaskListLogic.comparator(TaskSort(TaskSortField.DUE_DATE, reversed = false)))
        val desc = list.sortedWith(TaskListLogic.comparator(TaskSort(TaskSortField.DUE_DATE, reversed = true)))
        assertEquals(listOf("early", "late", "none"), ids(asc))
        assertEquals(listOf("late", "early", "none"), ids(desc))
    }

    @Test
    fun dueDateSortUsesTimeAsTieBreaker() {
        val list = listOf(
            task("noTime", due = 5),
            task("pm", due = 5, dueTime = 900),
            task("am", due = 5, dueTime = 480),
        )
        val sorted = list.sortedWith(TaskListLogic.comparator(TaskSort()))
        assertEquals(listOf("am", "pm", "noTime"), ids(sorted))
    }

    @Test
    fun prioritySortIsUrgentFirstAndReversible() {
        val list = listOf(
            task("low", priority = Priority.LOW),
            task("urgent", priority = Priority.URGENT),
            task("high", priority = Priority.HIGH),
            task("med", priority = Priority.MEDIUM),
        )
        assertEquals(
            listOf("urgent", "high", "med", "low"),
            ids(list.sortedWith(TaskListLogic.comparator(TaskSort(TaskSortField.PRIORITY)))),
        )
        assertEquals(
            listOf("low", "med", "high", "urgent"),
            ids(list.sortedWith(TaskListLogic.comparator(TaskSort(TaskSortField.PRIORITY, reversed = true)))),
        )
    }

    @Test
    fun sortIsStableForEqualKeys() {
        val list = listOf(
            task("1", priority = Priority.HIGH, due = 7),
            task("2", priority = Priority.HIGH, due = 7),
            task("3", priority = Priority.HIGH, due = 7),
        )
        for (field in TaskSortField.entries) {
            if (field == TaskSortField.TITLE) continue
            assertEquals(field.name, listOf("1", "2", "3"), ids(list.sortedWith(TaskListLogic.comparator(TaskSort(field)))))
        }
    }

    @Test
    fun titleSortIsCaseInsensitiveAndCreatedIsNewestFirst() {
        val list = listOf(task("b", title = "banana", created = 1), task("a", title = "Apple", created = 3), task("c", title = "cherry", created = 2))
        assertEquals(listOf("a", "b", "c"), ids(list.sortedWith(TaskListLogic.comparator(TaskSort(TaskSortField.TITLE)))))
        assertEquals(listOf("a", "c", "b"), ids(list.sortedWith(TaskListLogic.comparator(TaskSort(TaskSortField.CREATED)))))
    }

    @Test
    fun searchMatchesTitleAndDescriptionCaseInsensitive() {
        val all = listOf(
            task("1", title = "Buy MILK"),
            task("2", title = "Call mum", description = "ask about milk recipe"),
            task("3", title = "Gym"),
        )
        val result = TaskListLogic.visibleTasks(all, TaskView.ALL, TaskFilters(), "  milk ", TaskSort(), today)
        assertEquals(setOf("1", "2"), ids(result).toSet())
        assertEquals(3, TaskListLogic.visibleTasks(all, TaskView.ALL, TaskFilters(), "", TaskSort(), today).size)
    }

    @Test
    fun dateFilters() {
        val all = listOf(
            task("today", due = todayDay),
            task("in6", due = todayDay + 6),
            task("in7", due = todayDay + 7),
            task("eom", due = LocalDate.of(2026, 11, 30).toEpochDay().toInt()),
            task("dec", due = LocalDate.of(2026, 12, 1).toEpochDay().toInt()),
            task("none"),
        )
        fun run(f: TaskFilters) = ids(TaskListLogic.visibleTasks(all, TaskView.ALL, f, "", TaskSort(), today)).toSet()
        assertEquals(setOf("today"), run(TaskFilters(date = DateFilter.TODAY)))
        assertEquals(setOf("today", "in6"), run(TaskFilters(date = DateFilter.NEXT_7_DAYS)))
        assertEquals(setOf("today", "in6", "in7", "eom"), run(TaskFilters(date = DateFilter.THIS_MONTH)))
        assertEquals(setOf("none"), run(TaskFilters(date = DateFilter.NO_DATE)))
        assertEquals(
            setOf("in6", "in7"),
            run(TaskFilters(date = DateFilter.CUSTOM, customStart = todayDay + 6, customEnd = todayDay + 7)),
        )
        assertEquals(6, run(TaskFilters()).size)
    }

    @Test
    fun priorityStatusAndProjectFilters() {
        val all = listOf(
            task("a", priority = Priority.HIGH, projectId = "p1"),
            task("b", priority = Priority.LOW, projectId = "p2", status = TaskStatus.IN_PROGRESS),
            task("c", priority = Priority.HIGH),
        )
        fun run(f: TaskFilters) = ids(TaskListLogic.visibleTasks(all, TaskView.ALL, f, "", TaskSort(), today)).toSet()
        assertEquals(setOf("a", "c"), run(TaskFilters(priorities = setOf(Priority.HIGH))))
        assertEquals(setOf("b"), run(TaskFilters(statuses = setOf(TaskStatus.IN_PROGRESS))))
        assertEquals(setOf("a"), run(TaskFilters(project = ProjectFilter.OneProject("p1"))))
        assertEquals(setOf("c"), run(TaskFilters(project = ProjectFilter.NoProject)))
        assertEquals(setOf("a"), run(TaskFilters(priorities = setOf(Priority.HIGH), project = ProjectFilter.OneProject("p1"))))
    }

    @Test
    fun activeFilterCount() {
        assertFalse(TaskFilters().isActive)
        val f = TaskFilters(date = DateFilter.TODAY, priorities = setOf(Priority.LOW), project = ProjectFilter.NoProject)
        assertEquals(3, f.activeCount)
        assertTrue(f.isActive)
    }
}
