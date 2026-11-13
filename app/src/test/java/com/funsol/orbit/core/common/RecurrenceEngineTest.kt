package com.funsol.orbit.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceEngineTest {
    private fun rule(
        type: RecurrenceType,
        interval: Int = 1,
        mask: Int = 0,
        end: LocalDate? = null,
        anchor: Int = 1,
    ) = Recurrence(type, interval, mask, end?.toEpochDayInt(), anchor)

    @Test fun `none never repeats`() {
        assertNull(RecurrenceEngine.nextAfter(LocalDate.of(2025, 1, 1), rule(RecurrenceType.NONE)))
    }

    @Test fun `daily and every three days`() {
        assertEquals(LocalDate.of(2025, 3, 2), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 1), rule(RecurrenceType.DAILY)))
        assertEquals(LocalDate.of(2025, 3, 4), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 1), rule(RecurrenceType.DAILY, 3)))
    }

    @Test fun `daily crosses month and leap day`() {
        assertEquals(LocalDate.of(2024, 2, 29), RecurrenceEngine.nextAfter(LocalDate.of(2024, 2, 28), rule(RecurrenceType.DAILY)))
        assertEquals(LocalDate.of(2025, 3, 1), RecurrenceEngine.nextAfter(LocalDate.of(2025, 2, 28), rule(RecurrenceType.DAILY)))
        assertEquals(LocalDate.of(2026, 1, 1), RecurrenceEngine.nextAfter(LocalDate.of(2025, 12, 31), rule(RecurrenceType.DAILY)))
    }

    @Test fun `weekly without mask repeats on same weekday`() {
        assertEquals(LocalDate.of(2025, 3, 10), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 3), rule(RecurrenceType.WEEKLY)))
        assertEquals(LocalDate.of(2025, 3, 17), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 3), rule(RecurrenceType.WEEKLY, 2)))
    }

    @Test fun `weekly with mask picks the next selected weekday`() {
        val monWedFri = WeekdayMask.of(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        val r = rule(RecurrenceType.WEEKLY, mask = monWedFri)
        assertEquals(LocalDate.of(2025, 3, 5), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 3), r))
        assertEquals(LocalDate.of(2025, 3, 7), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 5), r))
        assertEquals(LocalDate.of(2025, 3, 10), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 7), r))
    }

    @Test fun `biweekly with mask skips a week`() {
        val monFri = WeekdayMask.of(listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val r = rule(RecurrenceType.WEEKLY, interval = 2, mask = monFri)
        assertEquals(LocalDate.of(2025, 3, 7), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 3), r))
        assertEquals(LocalDate.of(2025, 3, 17), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 7), r))
    }

    @Test fun `monthly clamps to month length and does not drift`() {
        val r = rule(RecurrenceType.MONTHLY, anchor = 31)
        val feb = RecurrenceEngine.nextAfter(LocalDate.of(2025, 1, 31), r)!!
        assertEquals(LocalDate.of(2025, 2, 28), feb)
        assertEquals(LocalDate.of(2025, 3, 31), RecurrenceEngine.nextAfter(feb, r))
        assertEquals(LocalDate.of(2024, 2, 29), RecurrenceEngine.nextAfter(LocalDate.of(2024, 1, 31), r))
    }

    @Test fun `monthly rolls over the year`() {
        assertEquals(LocalDate.of(2026, 1, 15), RecurrenceEngine.nextAfter(LocalDate.of(2025, 12, 15), rule(RecurrenceType.MONTHLY, anchor = 15)))
    }

    @Test fun `yearly on leap day returns to the 29th in leap years`() {
        val r = rule(RecurrenceType.YEARLY, anchor = 29)
        val y1 = RecurrenceEngine.nextAfter(LocalDate.of(2024, 2, 29), r)!!
        assertEquals(LocalDate.of(2025, 2, 28), y1)
        assertEquals(LocalDate.of(2026, 2, 28), RecurrenceEngine.nextAfter(y1, r))
        assertEquals(LocalDate.of(2028, 2, 29), RecurrenceEngine.nextAfter(LocalDate.of(2027, 2, 28), r))
    }

    @Test fun `series ends after end date`() {
        val r = rule(RecurrenceType.DAILY, end = LocalDate.of(2025, 3, 3))
        assertEquals(LocalDate.of(2025, 3, 3), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 2), r))
        assertNull(RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 3), r))
    }

    @Test fun `completing a stale occurrence lands on today or later, never on a pile of overdue copies`() {
        val today = LocalDate.of(2025, 3, 20)
        val next = RecurrenceEngine.nextForCompletion(LocalDate.of(2025, 3, 1), rule(RecurrenceType.DAILY), today)
        assertEquals(today, next)
    }

    @Test fun `completing an on-time occurrence gives exactly the next one`() {
        val today = LocalDate.of(2025, 3, 20)
        assertEquals(LocalDate.of(2025, 3, 21), RecurrenceEngine.nextForCompletion(today, rule(RecurrenceType.DAILY), today))
        assertEquals(LocalDate.of(2025, 3, 27), RecurrenceEngine.nextForCompletion(today, rule(RecurrenceType.WEEKLY), today))
    }

    @Test fun `skipping stays inside the end date`() {
        val r = rule(RecurrenceType.DAILY, end = LocalDate.of(2025, 3, 10))
        assertNull(RecurrenceEngine.nextForCompletion(LocalDate.of(2025, 3, 1), r, LocalDate.of(2025, 3, 20)))
    }

    @Test fun `interval below one is treated as one`() {
        assertEquals(LocalDate.of(2025, 3, 2), RecurrenceEngine.nextAfter(LocalDate.of(2025, 3, 1), rule(RecurrenceType.DAILY, interval = 0)))
    }
}
