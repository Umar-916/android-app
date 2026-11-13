package com.funsol.orbit.feature.habits

import com.funsol.orbit.core.common.HabitMath
import com.funsol.orbit.core.common.WeekdayMask
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class HabitsLogicTest {
    private val monWedFri = WeekdayMask.of(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

    @Test
    fun presetsRoundTrip() {
        assertEquals(SchedulePreset.DAILY, HabitSchedule.presetOf(127))
        assertEquals(SchedulePreset.WEEKDAYS, HabitSchedule.presetOf(0b0011111))
        assertEquals(SchedulePreset.WEEKENDS, HabitSchedule.presetOf(0b1100000))
        assertEquals(SchedulePreset.CUSTOM, HabitSchedule.presetOf(monWedFri))
        assertEquals(127, HabitSchedule.maskOf(SchedulePreset.DAILY, 5))
        assertEquals(0b0011111, HabitSchedule.maskOf(SchedulePreset.WEEKDAYS, 5))
        assertEquals(0b1100000, HabitSchedule.maskOf(SchedulePreset.WEEKENDS, 5))
        assertEquals(5, HabitSchedule.maskOf(SchedulePreset.CUSTOM, 5))
    }

    @Test
    fun atLeastOneDayRequired() {
        assertFalse(HabitSchedule.isValid(0))
        assertTrue(HabitSchedule.isValid(1))
        assertTrue(HabitSchedule.isValid(127))
    }

    @Test
    fun monWedFriHabitIsNotBrokenByTuesdayAndThursday() {
        val logs = setOf(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 5),
            LocalDate.of(2024, 1, 8),
        )
        val stats = HabitMath.stats(monWedFri, LocalDate.of(2024, 1, 1), logs, today = LocalDate.of(2024, 1, 9))
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
        assertEquals(100, stats.completionPercent)
    }

    @Test
    fun openTodayDoesNotBreakStreak() {
        val start = LocalDate.of(2024, 1, 1)
        val logs = (1..4).map { LocalDate.of(2024, 1, it) }.toSet()
        val stats = HabitMath.stats(WeekdayMask.ALL, start, logs, today = LocalDate.of(2024, 1, 5))
        assertEquals(4, stats.currentStreak)
    }

    @Test
    fun missedPastScheduledDayBreaksStreak() {
        val start = LocalDate.of(2024, 1, 1)
        val logs = setOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 4))
        val stats = HabitMath.stats(WeekdayMask.ALL, start, logs, today = LocalDate.of(2024, 1, 5))
        assertEquals(1, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun streakCrossesLeapDay() {
        val start = LocalDate.of(2024, 2, 27)
        val logs = setOf(LocalDate.of(2024, 2, 27), LocalDate.of(2024, 2, 28), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1))
        val stats = HabitMath.stats(WeekdayMask.ALL, start, logs, today = LocalDate.of(2024, 3, 1))
        assertEquals(4, stats.currentStreak)
        assertEquals(100, stats.completionPercent)
    }

    @Test
    fun streakCrossesMonthBoundary() {
        val start = LocalDate.of(2024, 1, 30)
        val logs = setOf(LocalDate.of(2024, 1, 30), LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 1))
        val stats = HabitMath.stats(WeekdayMask.ALL, start, logs, today = LocalDate.of(2024, 2, 1))
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun habitStartingInTheFutureHasNoStats() {
        val stats = HabitMath.stats(WeekdayMask.ALL, LocalDate.of(2024, 5, 1), emptySet(), today = LocalDate.of(2024, 4, 1))
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.scheduledDays)
    }

    @Test
    fun buildItemsFlagsTodayCorrectly() {
        val today = LocalDate.of(2024, 1, 8)
        val daily = HabitEntity(id = "d", name = "Daily", startDate = today.minusDays(3).toEpochDayInt(), createdAt = 1)
        val saturdays = HabitEntity(
            id = "s", name = "Sat", scheduleMask = WeekdayMask.bit(DayOfWeek.SATURDAY),
            startDate = today.minusDays(30).toEpochDayInt(), createdAt = 2,
        )
        val logs = listOf(HabitLogEntity("l1", "d", today.toEpochDayInt()), HabitLogEntity("l2", "d", today.minusDays(1).toEpochDayInt()))
        val items = HabitsLogic.buildItems(listOf(saturdays, daily), logs, today)
        assertEquals(listOf("d", "s"), items.map { it.habit.id })
        assertTrue(items[0].scheduledToday)
        assertTrue(items[0].checkedToday)
        assertEquals(2, items[0].stats.currentStreak)
        assertFalse(items[1].scheduledToday)
        assertFalse(items[1].checkedToday)
    }

    @Test
    fun habitNotStartedYetIsNotScheduledToday() {
        val today = LocalDate.of(2024, 1, 8)
        val habit = HabitEntity(id = "f", name = "Future", startDate = today.plusDays(2).toEpochDayInt(), createdAt = 1)
        assertFalse(HabitsLogic.buildItems(listOf(habit), emptyList(), today).single().scheduledToday)
    }

    @Test
    fun leadingBlanksDependOnFirstDayOfWeek() {
        val jan2024 = YearMonth.of(2024, 1)
        assertEquals(0, HabitCalendarMath.leadingBlanks(jan2024, DayOfWeek.MONDAY))
        assertEquals(1, HabitCalendarMath.leadingBlanks(jan2024, DayOfWeek.SUNDAY))
        assertEquals(2, HabitCalendarMath.leadingBlanks(jan2024, DayOfWeek.SATURDAY))
        val feb2024 = YearMonth.of(2024, 2)
        assertEquals(3, HabitCalendarMath.leadingBlanks(feb2024, DayOfWeek.MONDAY))
    }

    @Test
    fun weekdayOrderStartsAtFirstDay() {
        val order = HabitCalendarMath.weekdayOrder(DayOfWeek.SUNDAY)
        assertEquals(DayOfWeek.SUNDAY, order.first())
        assertEquals(DayOfWeek.SATURDAY, order.last())
        assertEquals(7, order.distinct().size)
    }

    @Test
    fun weeksFormFullRowsAndContainEveryDay() {
        val feb = YearMonth.of(2024, 2)
        val weeks = HabitCalendarMath.weeks(
            feb, DayOfWeek.MONDAY, monWedFri, LocalDate.of(2024, 2, 1), emptySet(), LocalDate.of(2024, 2, 15),
        )
        assertEquals(5, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertNull(weeks[0][0])
        assertNull(weeks[0][2])
        assertEquals(1, weeks[0][3]!!.date.dayOfMonth)
        assertEquals(29, weeks.flatten().count { it != null })
        assertTrue(weeks.flatten().filterNotNull().any { it.date.dayOfMonth == 29 })
    }

    @Test
    fun cellStatesFollowTheSchedule() {
        val start = LocalDate.of(2024, 2, 5)
        val today = LocalDate.of(2024, 2, 15)
        val logs = setOf(LocalDate.of(2024, 2, 12))
        fun cell(day: Int) = HabitCalendarMath.cellFor(LocalDate.of(2024, 2, day), monWedFri, start, logs, today)

        assertEquals(DayStatus.DONE, cell(12).status)
        assertTrue(cell(12).toggleable)
        assertEquals(DayStatus.MISSED, cell(14).status)
        assertTrue(cell(14).toggleable)
        assertEquals(DayStatus.NOT_SCHEDULED, cell(13).status)
        assertFalse(cell(13).toggleable)
        assertEquals(DayStatus.NOT_SCHEDULED, cell(2).status)
        assertEquals(DayStatus.NOT_SCHEDULED, cell(15).status)
        assertTrue(cell(15).isToday)
        assertEquals(DayStatus.SCHEDULED, cell(16).status)
        assertFalse(cell(16).toggleable)
    }

    @Test
    fun todayScheduledAndOpenIsScheduledAndTogglable() {
        val today = LocalDate.of(2024, 2, 14)
        val c = HabitCalendarMath.cellFor(today, monWedFri, LocalDate.of(2024, 1, 1), emptySet(), today)
        assertEquals(DayStatus.SCHEDULED, c.status)
        assertTrue(c.toggleable)
        assertTrue(c.isToday)
    }
}
