package com.funsol.orbit.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitMathTest {
    private val everyDay = WeekdayMask.ALL
    private val monWedFri = WeekdayMask.of(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))

    private val mon = LocalDate.of(2025, 3, 3)

    private fun days(vararg offsets: Int, base: LocalDate = mon) = offsets.map { base.plusDays(it.toLong()) }.toSet()

    @Test fun `daily habit current and longest streak`() {
        val logs = days(0, 1, 2, 4, 5)
        val s = HabitMath.stats(everyDay, mon, logs, today = mon.plusDays(5))
        assertEquals(2, s.currentStreak)
        assertEquals(3, s.longestStreak)
    }

    @Test fun `open today does not break the streak`() {
        val logs = days(0, 1, 2)
        val s = HabitMath.stats(everyDay, mon, logs, today = mon.plusDays(3))
        assertEquals(3, s.currentStreak)
    }

    @Test fun `missing yesterday breaks the streak`() {
        val logs = days(0, 1, 3)
        val s = HabitMath.stats(everyDay, mon, logs, today = mon.plusDays(3))
        assertEquals(1, s.currentStreak)
        assertEquals(2, s.longestStreak)
    }

    @Test fun `weekday schedule is not broken by unscheduled days`() {
        val logs = days(0, 2, 4)
        val s = HabitMath.stats(monWedFri, mon, logs, today = mon.plusDays(4))
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.longestStreak)
        assertEquals(100, s.completionPercent)
    }

    @Test fun `weekday schedule streak survives the weekend`() {
        val logs = days(0, 2, 4, 7)
        val s = HabitMath.stats(monWedFri, mon, logs, today = mon.plusDays(8))
        assertEquals(4, s.currentStreak)
    }

    @Test fun `missing a scheduled day resets only after that day`() {
        val logs = days(0, 4)
        val s = HabitMath.stats(monWedFri, mon, logs, today = mon.plusDays(4))
        assertEquals(1, s.currentStreak)
        assertEquals(1, s.longestStreak)
        assertEquals(67, s.completionPercent)
    }

    @Test fun `habit that starts in the future has no stats`() {
        val s = HabitMath.stats(everyDay, mon.plusDays(5), emptySet(), today = mon)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.scheduledDays)
        assertEquals(0, s.completionPercent)
    }

    @Test fun `today still open is not counted against completion percent`() {
        val s = HabitMath.stats(everyDay, mon, days(0), today = mon.plusDays(1))
        assertEquals(1, s.scheduledDays)
        assertEquals(100, s.completionPercent)
    }

    @Test fun `streak works across a month boundary and leap day`() {
        val start = LocalDate.of(2024, 2, 27)
        val logs = (0L..3L).map { start.plusDays(it) }.toSet()
        val s = HabitMath.stats(everyDay, start, logs, today = LocalDate.of(2024, 3, 1))
        assertEquals(4, s.currentStreak)
    }

    @Test fun `zero mask is treated as every day`() {
        assertTrue(HabitMath.isScheduled(0, mon))
        assertFalse(HabitMath.isScheduled(WeekdayMask.bit(DayOfWeek.TUESDAY), mon))
    }
}

class GoalMathTest {
    @Test fun `fraction clamps and never divides by zero`() {
        assertEquals(0f, GoalMath.fraction(5.0, 0.0), 0f)
        assertEquals(0f, GoalMath.fraction(5.0, -3.0), 0f)
        assertEquals(0f, GoalMath.fraction(Double.NaN, 10.0), 0f)
        assertEquals(0f, GoalMath.fraction(1.0, Double.POSITIVE_INFINITY), 0f)
        assertEquals(1f, GoalMath.fraction(20.0, 10.0), 0f)
        assertEquals(0f, GoalMath.fraction(-4.0, 10.0), 0f)
        assertEquals(0.5f, GoalMath.fraction(5.0, 10.0), 0f)
    }

    @Test fun `percent and reached`() {
        assertEquals(25, GoalMath.percent(1.0, 4.0))
        assertTrue(GoalMath.isReached(10.0, 10.0))
        assertFalse(GoalMath.isReached(9.99, 10.0))
        assertFalse(GoalMath.isReached(5.0, 0.0))
    }
}
