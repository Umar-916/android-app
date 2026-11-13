package com.funsol.orbit.feature.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridTest {
    @Test
    fun alwaysSixWeeksOfSevenConsecutiveDays() {
        val grid = CalendarGrid.build(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertEquals(6, grid.size)
        grid.forEach { assertEquals(7, it.size) }
        val flat = grid.flatten()
        assertEquals(42, flat.size)
        flat.zipWithNext().forEach { (a, b) -> assertEquals(a.plusDays(1), b) }
    }

    @Test
    fun firstCellIsTheFirstDayOfWeekOnOrBeforeTheFirstOfTheMonth() {
        for (first in DayOfWeek.entries) {
            for (monthNo in 1..12) {
                val month = YearMonth.of(2026, monthNo)
                val grid = CalendarGrid.build(month, first)
                val firstCell = grid[0][0]
                assertEquals("$month/$first", first, firstCell.dayOfWeek)
                val firstOfMonth = month.atDay(1)
                assertFalse(firstCell.isAfter(firstOfMonth))
                assertTrue(firstOfMonth.toEpochDay() - firstCell.toEpochDay() in 0..6)
            }
        }
    }

    @Test
    fun monthStartingOnEachWeekdayMondayFirst() {
        val cases = mapOf(
            YearMonth.of(2025, 9) to 0,
            YearMonth.of(2025, 4) to 1,
            YearMonth.of(2025, 10) to 2,
            YearMonth.of(2025, 5) to 3,
            YearMonth.of(2025, 8) to 4,
            YearMonth.of(2025, 11) to 5,
            YearMonth.of(2025, 6) to 6,
        )
        for ((month, leading) in cases) {
            val grid = CalendarGrid.build(month, DayOfWeek.MONDAY)
            val index = grid.flatten().indexOf(month.atDay(1))
            assertEquals(month.toString(), leading, index)
        }
    }

    @Test
    fun firstDayOfWeekSundayAndSaturday() {
        val nov = YearMonth.of(2026, 11)
        assertEquals(0, CalendarGrid.build(nov, DayOfWeek.SUNDAY).flatten().indexOf(nov.atDay(1)))
        assertEquals(1, CalendarGrid.build(nov, DayOfWeek.SATURDAY).flatten().indexOf(nov.atDay(1)))
        assertEquals(6, CalendarGrid.build(nov, DayOfWeek.MONDAY).flatten().indexOf(nov.atDay(1)))
    }

    @Test
    fun leapYearFebruary() {
        val leap = CalendarGrid.build(YearMonth.of(2024, 2), DayOfWeek.MONDAY).flatten()
        assertTrue(leap.contains(LocalDate.of(2024, 2, 29)))
        assertEquals(29, leap.count { CalendarGrid.isInMonth(it, YearMonth.of(2024, 2)) })
        val common = CalendarGrid.build(YearMonth.of(2025, 2), DayOfWeek.MONDAY).flatten()
        assertEquals(28, common.count { CalendarGrid.isInMonth(it, YearMonth.of(2025, 2)) })
        assertTrue(common.contains(LocalDate.of(2025, 2, 28)))
        assertEquals(LocalDate.of(2025, 3, 1), common[common.indexOf(LocalDate.of(2025, 2, 28)) + 1])
    }

    @Test
    fun decemberRollsOverIntoJanuary() {
        val dec = CalendarGrid.build(YearMonth.of(2026, 12), DayOfWeek.MONDAY).flatten()
        assertTrue(dec.contains(LocalDate.of(2026, 12, 31)))
        assertTrue(dec.contains(LocalDate.of(2027, 1, 1)))
        assertEquals(31, dec.count { CalendarGrid.isInMonth(it, YearMonth.of(2026, 12)) })
        val jan = CalendarGrid.build(YearMonth.of(2027, 1), DayOfWeek.MONDAY).flatten()
        assertTrue(jan.contains(LocalDate.of(2026, 12, 28)))
    }

    @Test
    fun weekdayOrderRotates() {
        assertEquals(
            listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            CalendarGrid.weekdayOrder(DayOfWeek.SATURDAY),
        )
    }

    @Test
    fun shiftSelectionClampsDayAndSnapsToToday() {
        val today = LocalDate.of(2026, 11, 12)
        assertEquals(LocalDate.of(2025, 2, 28), CalendarLogic.shiftSelection(LocalDate.of(2025, 1, 31), today, 1))
        assertEquals(LocalDate.of(2024, 2, 29), CalendarLogic.shiftSelection(LocalDate.of(2024, 1, 29), today, 1))
        assertEquals(LocalDate.of(2027, 1, 15), CalendarLogic.shiftSelection(LocalDate.of(2026, 12, 15), today, 1))
        assertEquals(today, CalendarLogic.shiftSelection(LocalDate.of(2026, 10, 3), today, 1))
    }
}
