package com.funsol.orbit.feature.calendar

import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarMode { DAY, AGENDA }

object CalendarGrid {
    const val WEEKS = 6
    const val DAYS_PER_WEEK = 7

    fun weekdayOrder(first: DayOfWeek): List<DayOfWeek> = List(DAYS_PER_WEEK) { first.plus(it.toLong()) }

    fun build(month: YearMonth, first: DayOfWeek): List<List<LocalDate>> {
        val firstOfMonth = month.atDay(1)
        val leading = Math.floorMod(firstOfMonth.dayOfWeek.value - first.value, DAYS_PER_WEEK)
        val start = firstOfMonth.minusDays(leading.toLong())
        return List(WEEKS) { week -> List(DAYS_PER_WEEK) { day -> start.plusDays((week * DAYS_PER_WEEK + day).toLong()) } }
    }

    fun isInMonth(date: LocalDate, month: YearMonth): Boolean = YearMonth.from(date) == month
}

enum class DotKind { OPEN, OVERDUE, COMPLETED }

data class DayDot(val priority: Priority, val kind: DotKind)

data class DayInfo(val dots: List<DayDot>, val total: Int, val open: Int, val overdue: Int)

data class DaySchedule(val allDay: List<TaskEntity>, val timed: List<TaskEntity>) {
    val isEmpty: Boolean get() = allDay.isEmpty() && timed.isEmpty()
    val count: Int get() = allDay.size + timed.size
}

sealed interface AgendaKey {
    data object Overdue : AgendaKey
    data class Day(val epochDay: Int) : AgendaKey
}

data class AgendaGroup(val key: AgendaKey, val tasks: List<TaskEntity>)

object CalendarLogic {
    const val MAX_DOTS = 3

    private val dayOrder: Comparator<TaskEntity> = compareBy<TaskEntity> { if (TaskRules.isOpen(it)) 0 else 1 }
        .thenByDescending { it.priority.ordinal }

    fun dayInfo(tasks: List<TaskEntity>, today: LocalDate): DayInfo {
        val sorted = tasks.sortedWith(dayOrder)
        val dots = sorted.take(MAX_DOTS).map { t ->
            DayDot(
                priority = t.priority,
                kind = when {
                    TaskRules.isCompleted(t) -> DotKind.COMPLETED
                    TaskRules.isOverdue(t, today) -> DotKind.OVERDUE
                    else -> DotKind.OPEN
                },
            )
        }
        return DayInfo(
            dots = dots,
            total = tasks.size,
            open = tasks.count { TaskRules.isOpen(it) },
            overdue = tasks.count { TaskRules.isOverdue(it, today) },
        )
    }

    fun calendarTasks(all: List<TaskEntity>): List<TaskEntity> =
        all.filter { it.dueDate != null && !TaskRules.isArchived(it) }

    fun groupByDay(all: List<TaskEntity>): Map<Int, List<TaskEntity>> =
        calendarTasks(all).groupBy { it.dueDate!! }

    fun daySchedule(tasksOfDay: List<TaskEntity>): DaySchedule {
        val allDay = tasksOfDay.filter { it.dueTime == null }.sortedWith(dayOrder.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        val timed = tasksOfDay.filter { it.dueTime != null }
            .sortedWith(compareBy<TaskEntity> { it.dueTime }.then(dayOrder))
        return DaySchedule(allDay, timed)
    }

    fun buildAgenda(all: List<TaskEntity>, today: LocalDate): List<AgendaGroup> {
        val todayDay = today.toEpochDayInt()
        val byTime = compareBy<TaskEntity> { it.dueDate }
            .thenBy { it.dueTime ?: -1 }
            .thenByDescending { it.priority.ordinal }
        val overdue = all.filter { TaskRules.isOverdue(it, today) }.sortedWith(byTime)
        val upcoming = all.filter { TaskRules.isOpen(it) && it.dueDate != null && it.dueDate >= todayDay }
            .sortedWith(byTime)
            .groupBy { it.dueDate!! }
        val result = ArrayList<AgendaGroup>(upcoming.size + 1)
        if (overdue.isNotEmpty()) result += AgendaGroup(AgendaKey.Overdue, overdue)
        for ((day, list) in upcoming) result += AgendaGroup(AgendaKey.Day(day), list)
        return result
    }

    fun shiftSelection(selected: LocalDate, today: LocalDate, delta: Long): LocalDate {
        val target = YearMonth.from(selected).plusMonths(delta)
        return if (target == YearMonth.from(today)) today
        else target.atDay(minOf(selected.dayOfMonth, target.lengthOfMonth()))
    }
}
