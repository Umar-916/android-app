package com.funsol.orbit.feature.projects

import com.funsol.orbit.core.common.Progress
import com.funsol.orbit.core.common.ProgressMath
import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import java.time.LocalDate

enum class ProjectFilter { ACTIVE, ON_HOLD, COMPLETED, ARCHIVED, ALL }

data class ProjectListItem(val project: ProjectEntity, val progress: Progress, val overdue: Int = 0)

data class ProjectStats(
    val total: Int,
    val completed: Int,
    val inProgress: Int,
    val todo: Int,
    val overdue: Int,
    val percent: Int,
)

object ProjectsLogic {
    fun matches(filter: ProjectFilter, status: ProjectStatus): Boolean = when (filter) {
        ProjectFilter.ALL -> true
        ProjectFilter.ACTIVE -> status == ProjectStatus.ACTIVE
        ProjectFilter.ON_HOLD -> status == ProjectStatus.ON_HOLD
        ProjectFilter.COMPLETED -> status == ProjectStatus.COMPLETED
        ProjectFilter.ARCHIVED -> status == ProjectStatus.ARCHIVED
    }

    fun buildItems(
        projects: List<ProjectEntity>,
        tasks: List<TaskEntity>,
        filter: ProjectFilter,
        today: LocalDate? = null,
    ): List<ProjectListItem> {
        val byProject = tasks.filter { it.projectId != null }.groupBy { it.projectId }
        return projects
            .filter { matches(filter, it.status) }
            .sortedWith(compareBy<ProjectEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
            .map { project ->
                val own = byProject[project.id].orEmpty()
                ProjectListItem(project, ProgressMath.ofTasks(own), if (today == null) 0 else own.count { TaskRules.isOverdue(it, today) })
            }
    }

    fun stats(tasks: List<TaskEntity>, today: LocalDate): ProjectStats {
        val counted = tasks.filter { it.status != TaskStatus.ARCHIVED }
        val progress = ProgressMath.ofTasks(tasks)
        return ProjectStats(
            total = counted.size,
            completed = counted.count { it.status == TaskStatus.COMPLETED },
            inProgress = counted.count { it.status == TaskStatus.IN_PROGRESS },
            todo = counted.count { it.status == TaskStatus.TODO },
            overdue = counted.count { TaskRules.isOverdue(it, today) },
            percent = progress.percent,
        )
    }

    fun sortTasks(tasks: List<TaskEntity>): List<TaskEntity> = tasks.sortedWith(
        compareBy<TaskEntity> { if (TaskRules.isOpen(it)) 0 else 1 }
            .thenBy { it.dueDate ?: Int.MAX_VALUE }
            .thenBy { it.createdAt },
    )

    fun isDateRangeValid(start: Int?, end: Int?): Boolean = start == null || end == null || end >= start
}
