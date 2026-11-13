package com.funsol.orbit.feature.habits

import com.funsol.orbit.core.common.HabitMath
import com.funsol.orbit.core.common.HabitStats
import com.funsol.orbit.core.common.WeekdayMask
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class SchedulePreset { DAILY, WEEKDAYS, WEEKENDS, CUSTOM }

object HabitSchedule {
    const val WEEKENDS_MASK = 0b1100000

    fun maskOf(preset: SchedulePreset, current: Int): Int = when (preset) {
        SchedulePreset.DAILY -> WeekdayMask.ALL
        SchedulePreset.WEEKDAYS -> WeekdayMask.WEEKDAYS
        SchedulePreset.WEEKENDS -> WEEKENDS_MASK
        SchedulePreset.CUSTOM -> current
    }

    fun presetOf(mask: Int): SchedulePreset = when (mask and WeekdayMask.ALL) {
        WeekdayMask.ALL -> SchedulePreset.DAILY
        WeekdayMask.WEEKDAYS -> SchedulePreset.WEEKDAYS
        WEEKENDS_MASK -> SchedulePreset.WEEKENDS
        else -> SchedulePreset.CUSTOM
    }

    fun isValid(mask: Int): Boolean = mask and WeekdayMask.ALL != 0
}

data class HabitListItem(
    val habit: HabitEntity,
    val stats: HabitStats,
    val scheduledToday: Boolean,
    val checkedToday: Boolean,
)

object HabitsLogic {
    fun canWrite(habit: HabitEntity, epochDay: Int, todayEpochDay: Int, checked: Boolean): Boolean {
        if (!checked) return true
        if (habit.archived || todayEpochDay <= 0) return false
        if (epochDay > todayEpochDay || epochDay < habit.startDate) return false
        return HabitMath.isScheduled(habit.scheduleMask, epochDay.toLocalDate())
    }

    fun buildItems(habits: List<HabitEntity>, logs: List<HabitLogEntity>, today: LocalDate): List<HabitListItem> {
        val todayDay = today.toEpochDayInt()
        val byHabit = logs.groupBy { it.habitId }
        return habits.sortedWith(compareBy<HabitEntity> { it.createdAt }.thenBy { it.id }).map { habit ->
            val habitLogs = byHabit[habit.id].orEmpty()
            val dates = habitLogs.mapTo(HashSet()) { it.date.toLocalDate() }
            HabitListItem(
                habit = habit,
                stats = HabitMath.stats(habit.scheduleMask, habit.startDate.toLocalDate(), dates, today),
                scheduledToday = HabitMath.isScheduled(habit.scheduleMask, today) && habit.startDate <= todayDay,
                checkedToday = habitLogs.any { it.date == todayDay },
            )
        }
    }
}

enum class DayStatus { DONE, MISSED, SCHEDULED, NOT_SCHEDULED }

data class DayCell(
    val date: LocalDate,
    val status: DayStatus,
    val isToday: Boolean,
    val toggleable: Boolean,
)

object HabitCalendarMath {
    fun leadingBlanks(month: YearMonth, firstDay: DayOfWeek): Int =
        (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7

    fun cellFor(date: LocalDate, scheduleMask: Int, start: LocalDate, logs: Set<LocalDate>, today: LocalDate): DayCell {
        val scheduled = !date.isBefore(start) && HabitMath.isScheduled(scheduleMask, date)
        val done = date in logs
        val status = when {
            done -> DayStatus.DONE
            !scheduled -> DayStatus.NOT_SCHEDULED
            date.isBefore(today) -> DayStatus.MISSED
            else -> DayStatus.SCHEDULED
        }
        return DayCell(
            date = date,
            status = status,
            isToday = date == today,
            toggleable = !date.isAfter(today) && (scheduled || done),
        )
    }

    fun weeks(
        month: YearMonth,
        firstDay: DayOfWeek,
        scheduleMask: Int,
        start: LocalDate,
        logs: Set<LocalDate>,
        today: LocalDate,
    ): List<List<DayCell?>> {
        val cells = ArrayList<DayCell?>()
        repeat(leadingBlanks(month, firstDay)) { cells.add(null) }
        for (d in 1..month.lengthOfMonth()) {
            cells.add(cellFor(month.atDay(d), scheduleMask, start, logs, today))
        }
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }

    fun weekdayOrder(firstDay: DayOfWeek): List<DayOfWeek> = List(7) { firstDay.plus(it.toLong()) }
}
