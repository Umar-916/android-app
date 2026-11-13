package com.funsol.orbit.feature.tasks

import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import java.time.LocalDate
import java.time.YearMonth

enum class TaskView { ALL, TODAY, UPCOMING, OVERDUE, COMPLETED, ARCHIVED }

fun TaskView.matches(task: TaskEntity, today: LocalDate): Boolean = when (this) {
    TaskView.ALL -> !TaskRules.isArchived(task)
    TaskView.TODAY -> TaskRules.isDueToday(task, today)
    TaskView.UPCOMING -> TaskRules.isUpcoming(task, today)
    TaskView.OVERDUE -> TaskRules.isOverdue(task, today)
    TaskView.COMPLETED -> TaskRules.isCompleted(task)
    TaskView.ARCHIVED -> TaskRules.isArchived(task)
}

enum class DateFilter { ANY, TODAY, NEXT_7_DAYS, THIS_MONTH, NO_DATE, CUSTOM }

sealed interface ProjectFilter {
    data object AnyProject : ProjectFilter
    data object NoProject : ProjectFilter
    data class OneProject(val id: String) : ProjectFilter
}

data class TaskFilters(
    val date: DateFilter = DateFilter.ANY,
    val customStart: Int? = null,
    val customEnd: Int? = null,
    val priorities: Set<Priority> = emptySet(),
    val statuses: Set<TaskStatus> = emptySet(),
    val project: ProjectFilter = ProjectFilter.AnyProject,
) {
    val activeCount: Int
        get() = (if (date != DateFilter.ANY) 1 else 0) +
            (if (priorities.isNotEmpty()) 1 else 0) +
            (if (statuses.isNotEmpty()) 1 else 0) +
            (if (project != ProjectFilter.AnyProject) 1 else 0)

    val isActive: Boolean get() = activeCount > 0

    fun matches(task: TaskEntity, today: LocalDate): Boolean {
        if (priorities.isNotEmpty() && task.priority !in priorities) return false
        if (statuses.isNotEmpty() && task.status !in statuses) return false
        when (project) {
            ProjectFilter.AnyProject -> Unit
            ProjectFilter.NoProject -> if (task.projectId != null) return false
            is ProjectFilter.OneProject -> if (task.projectId != project.id) return false
        }
        return dateMatches(task.dueDate, today)
    }

    private fun dateMatches(due: Int?, today: LocalDate): Boolean {
        val todayDay = today.toEpochDayInt()
        return when (date) {
            DateFilter.ANY -> true
            DateFilter.TODAY -> due == todayDay
            DateFilter.NEXT_7_DAYS -> due != null && due in todayDay..(todayDay + 6)
            DateFilter.THIS_MONTH -> due != null && YearMonth.from(due.toLocalDate()) == YearMonth.from(today)
            DateFilter.NO_DATE -> due == null
            DateFilter.CUSTOM -> due != null &&
                (customStart == null || due >= customStart) &&
                (customEnd == null || due <= customEnd)
        }
    }
}

enum class TaskSortField { DUE_DATE, PRIORITY, CREATED, TITLE }

data class TaskSort(val field: TaskSortField = TaskSortField.DUE_DATE, val reversed: Boolean = false)

data class ProjectTag(val name: String, val color: Int)

data class TaskItem(
    val task: TaskEntity,
    val project: ProjectTag?,
    val subtasksDone: Int,
    val subtasksTotal: Int,
)

object TaskListLogic {
    fun comparator(sort: TaskSort): Comparator<TaskEntity> = when (sort.field) {
        TaskSortField.DUE_DATE -> Comparator { a, b ->
            val c = compareNullsLast(a.dueDate, b.dueDate, sort.reversed)
            if (c != 0) c else compareNullsLast(a.dueTime, b.dueTime, sort.reversed)
        }
        TaskSortField.PRIORITY -> {
            val natural = Comparator<TaskEntity> { a, b -> b.priority.ordinal.compareTo(a.priority.ordinal) }
            if (sort.reversed) natural.reversed() else natural
        }
        TaskSortField.CREATED -> {
            val natural = Comparator<TaskEntity> { a, b -> b.createdAt.compareTo(a.createdAt) }
            if (sort.reversed) natural.reversed() else natural
        }
        TaskSortField.TITLE -> {
            val natural = Comparator<TaskEntity> { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.title, b.title) }
            if (sort.reversed) natural.reversed() else natural
        }
    }

    private fun compareNullsLast(a: Int?, b: Int?, reversed: Boolean): Int = when {
        a == null && b == null -> 0
        a == null -> 1
        b == null -> -1
        reversed -> b.compareTo(a)
        else -> a.compareTo(b)
    }

    fun matchesQuery(task: TaskEntity, query: String): Boolean =
        query.isEmpty() || task.title.contains(query, ignoreCase = true) || task.description.contains(query, ignoreCase = true)

    fun visibleTasks(
        tasks: List<TaskEntity>,
        view: TaskView,
        filters: TaskFilters,
        query: String,
        sort: TaskSort,
        today: LocalDate,
    ): List<TaskEntity> {
        val q = query.trim()
        return tasks.asSequence()
            .filter { view.matches(it, today) }
            .filter { filters.matches(it, today) }
            .filter { matchesQuery(it, q) }
            .toList()
            .sortedWith(comparator(sort))
    }

    fun counts(tasks: List<TaskEntity>, today: LocalDate): Map<TaskView, Int> =
        TaskView.entries.associateWith { view -> tasks.count { view.matches(it, today) } }
}
