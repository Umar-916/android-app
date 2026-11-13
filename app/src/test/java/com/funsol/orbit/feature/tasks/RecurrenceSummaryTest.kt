package com.funsol.orbit.feature.tasks

import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.WeekdayMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class RecurrenceSummaryTest {
    @Test
    fun noneHasNoDescriptor() {
        assertNull(RecurrenceSummary.describe(RecurrenceType.NONE, 3, 0, null))
    }

    @Test
    fun weeklyListsDaysMondayFirst() {
        val mask = WeekdayMask.of(listOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY))
        val d = RecurrenceSummary.describe(RecurrenceType.WEEKLY, 2, mask, 100)!!
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), d.days)
        assertEquals(2, d.interval)
        assertEquals(100, d.endEpochDay)
    }

    @Test
    fun nonWeeklyIgnoresDaysAndIntervalIsClamped() {
        val d = RecurrenceSummary.describe(RecurrenceType.MONTHLY, 0, WeekdayMask.ALL, null)!!
        assertEquals(emptyList<DayOfWeek>(), d.days)
        assertEquals(1, d.interval)
        assertEquals(RecurrenceSummary.MAX_INTERVAL, RecurrenceSummary.describe(RecurrenceType.DAILY, 100000, 0, null)!!.interval)
    }
}
