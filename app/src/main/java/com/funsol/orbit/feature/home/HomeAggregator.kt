package com.funsol.orbit.feature.home

import com.funsol.orbit.core.common.HabitMath
import com.funsol.orbit.core.common.Progress
import com.funsol.orbit.core.common.ProgressMath
import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import java.time.LocalDate
import java.time.ZoneId

data class HomeProjectItem(val project: ProjectEntity, val progress: Progress)

data class HomeHabitItem(val habit: HabitEntity, val checkedToday: Boolean, val currentStreak: Int)

data class UpcomingDay(val epochDay: Int, val tasks: List<TaskEntity>)

data class HomeOverview(
    val completedLast7Days: Int = 0,
    val openTasks: Int = 0,
    val activeProjects: Int = 0,
    val bestStreak: Int = 0,
)

data class HomeData(
    val progress: Progress = Progress(0, 0),
    val todayTasks: List<TaskEntity> = emptyList(),
    val todayCount: Int = 0,
    val upcomingGroups: List<UpcomingDay> = emptyList(),
    val upcomingCount: Int = 0,
    val overdueTasks: List<TaskEntity> = emptyList(),
    val overdueCount: Int = 0,
    val projects: List<HomeProjectItem> = emptyList(),
    val activeProjectCount: Int = 0,
    val habits: List<HomeHabitItem> = emptyList(),
    val habitsDone: Int = 0,
    val overview: HomeOverview = HomeOverview(),
    val isBlank: Boolean = true,
)

object HomeAggregator {
    const val MAX_TODAY = 5
    const val MAX_OVERDUE = 5
    const val MAX_UPCOMING = 8
    const val MAX_PROJECTS = 4
    const val UPCOMING_DAYS = 7
    const val OVERVIEW_DAYS = 7

    fun aggregate(
        tasks: List<TaskEntity>,
        projects: List<ProjectEntity>,
        habits: List<HabitEntity>,
        logs: List<HabitLogEntity>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
    ): HomeData {
        val todayDay = today.toEpochDayInt()
        val open = tasks.filter(TaskRules::isOpen)

        val completedToday = tasks.count { TaskRules.isCompleted(it) && completedOnOrDueToday(it, today, zone) }
        val openToday = open.filter { it.dueDate == todayDay }
        val progress = Progress(completedToday, openToday.size + completedToday)

        val byUrgency = compareByDescending<TaskEntity> { it.priority.ordinal }.thenBy { it.createdAt }
        val todayAll = openToday.sortedWith(compareBy<TaskEntity> { it.dueTime ?: Int.MAX_VALUE }.then(byUrgency))
        val overdueAll = open
            .filter { it.dueDate != null && it.dueDate < todayDay }
            .sortedWith(compareBy<TaskEntity> { it.dueDate }.then(byUrgency))
        val upcomingAll = open
            .filter { it.dueDate != null && it.dueDate > todayDay && it.dueDate <= todayDay + UPCOMING_DAYS }
            .sortedWith(compareBy<TaskEntity> { it.dueDate }.thenBy { it.dueTime ?: Int.MAX_VALUE }.then(byUrgency))
        val upcomingGroups = upcomingAll.take(MAX_UPCOMING)
            .groupBy { it.dueDate!! }
            .map { (day, list) -> UpcomingDay(day, list) }

        val activeProjects = projects.filter { it.status == ProjectStatus.ACTIVE }
        val tasksByProject = tasks.filter { it.projectId != null }.groupBy { it.projectId }
        val projectItems = activeProjects
            .sortedBy { it.name.lowercase() }
            .take(MAX_PROJECTS)
            .map { HomeProjectItem(it, ProgressMath.ofTasks(tasksByProject[it.id].orEmpty())) }

        val liveHabits = habits.filter { !it.archived }
        val logDays = logs.groupBy({ it.habitId }, { it.date })
        val habitStats = liveHabits.associate { h ->
            val days = logDays[h.id].orEmpty()
            h.id to (days.toSet() to HabitMath.stats(h.scheduleMask, h.startDate.toLocalDate(), days.map { it.toLocalDate() }.toSet(), today))
        }
        val habitItems = liveHabits
            .filter { HabitMath.isScheduled(it.scheduleMask, today) && it.startDate <= todayDay }
            .sortedBy { it.name.lowercase() }
            .map { h ->
                val (days, stats) = habitStats.getValue(h.id)
                HomeHabitItem(h, todayDay in days, stats.currentStreak)
            }

        val weekStart = today.minusDays((OVERVIEW_DAYS - 1).toLong())
        val completedWeek = tasks.count {
            TaskRules.isCompleted(it) && it.completedAt?.toLocalDate(zone)?.let { d -> !d.isBefore(weekStart) && !d.isAfter(today) } == true
        }
        val overview = HomeOverview(
            completedLast7Days = completedWeek,
            openTasks = open.size,
            activeProjects = activeProjects.size,
            bestStreak = habitStats.values.maxOfOrNull { it.second.currentStreak } ?: 0,
        )

        return HomeData(
            progress = progress,
            todayTasks = todayAll.take(MAX_TODAY),
            todayCount = todayAll.size,
            upcomingGroups = upcomingGroups,
            upcomingCount = upcomingAll.size,
            overdueTasks = overdueAll.take(MAX_OVERDUE),
            overdueCount = overdueAll.size,
            projects = projectItems,
            activeProjectCount = activeProjects.size,
            habits = habitItems,
            habitsDone = habitItems.count { it.checkedToday },
            overview = overview,
            isBlank = tasks.none { it.status != TaskStatus.ARCHIVED } &&
                activeProjects.isEmpty() && liveHabits.isEmpty(),
        )
    }

    private fun completedOnOrDueToday(t: TaskEntity, today: LocalDate, zone: ZoneId): Boolean =
        t.completedAt?.toLocalDate(zone) == today || t.dueDate == today.toEpochDayInt()
}
