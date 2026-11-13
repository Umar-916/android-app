package com.funsol.orbit.core.common

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

class DateTimeUtilsTest {
    @Test fun `epoch day and minute of day round trip`() {
        val d = LocalDate.of(2024, 2, 29)
        assertEquals(d, d.toEpochDayInt().toLocalDate())
        assertEquals(LocalTime.of(23, 59), LocalTime.of(23, 59).toMinuteOfDay().toLocalTime())
        assertEquals(0, LocalTime.MIDNIGHT.toMinuteOfDay())
    }

    @Test fun `a stored due date does not move when the device time zone changes`() {
        val due = LocalDate.of(2025, 6, 15).toEpochDayInt()
        assertEquals(LocalDate.of(2025, 6, 15), due.toLocalDate())
    }

    @Test fun `reminder is computed in the local zone`() {
        val due = LocalDate.of(2025, 6, 15).toEpochDayInt()
        val karachi = ZoneId.of("Asia/Karachi") // UTC+5
        val utcMillis = reminderTriggerMillis(due, 9 * 60, 30, ZoneOffset.UTC)
        val pkMillis = reminderTriggerMillis(due, 9 * 60, 30, karachi)
        assertEquals(5 * 60 * 60 * 1000L, utcMillis - pkMillis)
        assertEquals(
            ZonedDateTime.of(2025, 6, 15, 8, 30, 0, 0, karachi).toInstant().toEpochMilli(),
            pkMillis,
        )
    }

    @Test fun `date only task reminds at nine`() {
        val due = LocalDate.of(2025, 6, 15).toEpochDayInt()
        val zone = ZoneId.of("Europe/Madrid")
        val m = reminderTriggerMillis(due, null, 0, zone)
        assertEquals(9, Instant(m).atZone(zone).hour)
    }

    @Test fun `reminder inside a spring forward gap still fires on that day`() {
        val zone = ZoneId.of("America/New_York")
        val due = LocalDate.of(2025, 3, 9).toEpochDayInt()
        val m = reminderTriggerMillis(due, 2 * 60 + 30, 0, zone)
        val at = Instant(m).atZone(zone)
        assertEquals(LocalDate.of(2025, 3, 9), at.toLocalDate())
        assertEquals(3, at.hour)
    }

    @Test fun `start of day is correct on a 23 hour and a 25 hour day`() {
        val zone = ZoneId.of("America/New_York")
        val spring = LocalDate.of(2025, 3, 9)
        val fall = LocalDate.of(2025, 11, 2)
        assertEquals(23L * 3600_000, spring.plusDays(1).startOfDayMillis(zone) - spring.startOfDayMillis(zone))
        assertEquals(25L * 3600_000, fall.plusDays(1).startOfDayMillis(zone) - fall.startOfDayMillis(zone))
    }

    @Test fun `millis until midnight just before and after midnight`() {
        val zone = ZoneId.of("UTC")
        assertEquals(1_000L, millisUntilMidnight(ZonedDateTime.of(2025, 12, 31, 23, 59, 59, 0, zone)))
        assertEquals(24 * 3600_000L, millisUntilMidnight(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, zone)))
    }

    @Test fun `millis until midnight on a dst day respects the shorter day`() {
        val zone = ZoneId.of("America/New_York")
        val start = ZonedDateTime.of(2025, 3, 9, 0, 0, 0, 0, zone)
        assertEquals(23L * 3600_000, millisUntilMidnight(start))
    }

    @Test fun `today flow emits again once the date changes`() = runTest {
        val zone = ZoneId.of("UTC")
        val calls = AtomicInteger(0)
        val clock: (ZoneId) -> ZonedDateTime = {
            if (calls.getAndIncrement() == 0) ZonedDateTime.of(2025, 12, 31, 23, 59, 59, 0, zone)
            else ZonedDateTime.of(2026, 1, 1, 0, 0, 1, 0, zone)
        }
        val emitted = todayFlow(zone = { zone }, nowProvider = clock).take(2).toList()
        assertEquals(listOf(LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 1)), emitted)
    }

    @Test fun `weekday mask helpers`() {
        val mask = WeekdayMask.of(listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.SUNDAY))
        assertEquals(0b1000001, mask)
        assertEquals(listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.SUNDAY), WeekdayMask.toDays(mask))
        assertEquals(WeekdayMask.bit(java.time.DayOfWeek.MONDAY), WeekdayMask.toggle(mask, java.time.DayOfWeek.SUNDAY))
    }

    private fun Instant(ms: Long) = java.time.Instant.ofEpochMilli(ms)
}
