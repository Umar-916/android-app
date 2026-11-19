package com.funsol.orbit.feature.analytics

import com.funsol.orbit.core.common.HabitMath
import com.funsol.orbit.core.common.HabitStats
import com.funsol.orbit.core.common.GoalMath
import com.funsol.orbit.core.common.Progress
import com.funsol.orbit.core.common.ProgressMath
import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import java.time.LocalDate
import java.time.ZoneId

enum class AnalyticsRange(val days: Int) { WEEK(7), MONTH(30) }

data class ChartBar(val start: LocalDate, val end: LocalDate, val value: Int)

data class ProjectStat(val project: ProjectEntity, val progress: Progress)

data class HabitStat(val habit: HabitEntity, val stats: HabitStats)

data class GoalStat(val goal: GoalEntity, val percent: Int)

data class AnalyticsData(
    val completedAllTime: Int = 0,
    val completedInRange: Int = 0,
    val completedLast7Days: Int = 0,
    val overdueNow: Int = 0,
    val openTasks: Int = 0,
    val completionRatePercent: Int? = null,
    val daily: List<ChartBar> = emptyList(),
    val weekly: List<ChartBar> = emptyList(),
    val projects: List<ProjectStat> = emptyList(),
    val habits: List<HabitStat> = emptyList(),
    val goals: List<GoalStat> = emptyList(),
    val isEmpty: Boolean = true,
)

object AnalyticsCalculator {
    const val WEEKLY_BUCKETS = 5

    fun calculate(
        tasks: List<TaskEntity>,
        projects: List<ProjectEntity>,
        habits: List<HabitEntity>,
        logs: List<HabitLogEntity>,
        goals: List<GoalEntity>,
        today: LocalDate,
        range: AnalyticsRange = AnalyticsRange.WEEK,
        zone: ZoneId = ZoneId.systemDefault(),
    ): AnalyticsData {
        val completedDates: List<LocalDate> = tasks
            .filter { TaskRules.isCompleted(it) }
            .mapNotNull { it.completedAt?.toLocalDate(zone) }
        val completedAll = tasks.count { TaskRules.isCompleted(it) }

        fun countBetween(from: LocalDate, to: LocalDate) = completedDates.count { !it.isBefore(from) && !it.isAfter(to) }

        val rangeStart = today.minusDays((range.days - 1).toLong())

        val startDay = rangeStart.toEpochDayInt()
        val endDay = today.toEpochDayInt()
        val dueInRange = tasks.filter { it.dueDate != null && it.dueDate in startDay..endDay }
        val doneDue = dueInRange.count { TaskRules.isCompleted(it) }
        val openDue = dueInRange.count { TaskRules.isOpen(it) }
        val rate = if (doneDue + openDue == 0) null else doneDue * 100 / (doneDue + openDue)

        val daysCount = if (range == AnalyticsRange.WEEK) 7 else 30
        val daily = (daysCount - 1 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            ChartBar(d, d, countBetween(d, d))
        }

        val weekly = (WEEKLY_BUCKETS - 1 downTo 0).map { w ->
            val end = today.minusDays(7L * w)
            val start = end.minusDays(6)
            ChartBar(start, end, countBetween(start, end))
        }

        val tasksByProject = tasks.filter { it.projectId != null }.groupBy { it.projectId }
        val projectStats = projects
            .filter { it.status != ProjectStatus.ARCHIVED }
            .map { ProjectStat(it, ProgressMath.ofTasks(tasksByProject[it.id].orEmpty())) }
            .sortedWith(compareByDescending<ProjectStat> { it.progress.fraction }.thenBy { it.project.name.lowercase() })

        val logsByHabit = logs.groupBy({ it.habitId }, { it.date.toLocalDate() })
        val habitStats = habits
            .filter { !it.archived }
            .map { h ->
                HabitStat(h, HabitMath.stats(h.scheduleMask, h.startDate.toLocalDate(), logsByHabit[h.id].orEmpty().toSet(), today))
            }
            .sortedWith(compareByDescending<HabitStat> { it.stats.completionPercent }.thenBy { it.habit.name.lowercase() })

        val goalStats = goals
            .filter { it.status != GoalStatus.ARCHIVED }
            .map { GoalStat(it, GoalMath.percent(it.currentValue, it.targetValue)) }
            .sortedByDescending { it.percent }

        return AnalyticsData(
            completedAllTime = completedAll,
            completedInRange = countBetween(rangeStart, today),
            completedLast7Days = countBetween(today.minusDays(6), today),
            overdueNow = tasks.count { TaskRules.isOverdue(it, today) },
            openTasks = tasks.count { TaskRules.isOpen(it) },
            completionRatePercent = rate,
            daily = daily,
            weekly = weekly,
            projects = projectStats,
            habits = habitStats,
            goals = goalStats,
            isEmpty = tasks.isEmpty() && projects.isEmpty() && habits.isEmpty() && goals.isEmpty(),
        )
    }
}
