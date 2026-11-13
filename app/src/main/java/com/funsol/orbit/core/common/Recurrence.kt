package com.funsol.orbit.core.common

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.min

enum class RecurrenceType { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

data class Recurrence(
    val type: RecurrenceType,
    val interval: Int = 1,
    val weekdayMask: Int = 0,
    val endEpochDay: Int? = null,
    val anchorDay: Int = 1,
) {
    val isRepeating: Boolean get() = type != RecurrenceType.NONE
}

object RecurrenceEngine {
    private const val MAX_SKIPS = 1500

    fun nextAfter(from: LocalDate, rule: Recurrence): LocalDate? {
        if (!rule.isRepeating) return null
        val interval = rule.interval.coerceAtLeast(1)
        val next = when (rule.type) {
            RecurrenceType.NONE -> return null
            RecurrenceType.DAILY -> from.plusDays(interval.toLong())
            RecurrenceType.WEEKLY -> nextWeekly(from, interval, rule.weekdayMask)
            RecurrenceType.MONTHLY -> {
                val first = from.withDayOfMonth(1).plusMonths(interval.toLong())
                first.withDayOfMonth(min(rule.anchorDay.coerceAtLeast(1), first.lengthOfMonth()))
            }
            RecurrenceType.YEARLY -> {
                val first = from.withDayOfMonth(1).plusYears(interval.toLong())
                first.withDayOfMonth(min(rule.anchorDay.coerceAtLeast(1), first.lengthOfMonth()))
            }
        }
        val end = rule.endEpochDay
        return if (end != null && next.toEpochDay() > end) null else next
    }

    fun nextForCompletion(completedDue: LocalDate, rule: Recurrence, today: LocalDate): LocalDate? {
        var candidate = nextAfter(completedDue, rule) ?: return null
        var guard = 0
        while (candidate.isBefore(today) && guard++ < MAX_SKIPS) {
            candidate = nextAfter(candidate, rule) ?: return null
        }
        return candidate
    }

    private fun nextWeekly(from: LocalDate, interval: Int, mask: Int): LocalDate {
        if (mask == 0) return from.plusWeeks(interval.toLong())
        for (offset in 1..(7 - from.dayOfWeek.value)) {
            val day = from.plusDays(offset.toLong())
            if (WeekdayMask.contains(mask, day.dayOfWeek)) return day
        }
        val monday = from.minusDays((from.dayOfWeek.value - 1).toLong()).plusWeeks(interval.toLong())
        for (i in 0..6) {
            val day = monday.plusDays(i.toLong())
            if (WeekdayMask.contains(mask, day.dayOfWeek)) return day
        }
        return from.plusWeeks(interval.toLong())
    }

    fun weekdaysOf(mask: Int): List<DayOfWeek> = WeekdayMask.toDays(mask)
}
