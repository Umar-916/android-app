package com.funsol.orbit.core.common

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

fun LocalDate.toEpochDayInt(): Int = toEpochDay().toInt()

fun Int.toLocalDate(): LocalDate = LocalDate.ofEpochDay(toLong())

fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute

fun Int.toLocalTime(): LocalTime = LocalTime.of((this / 60).coerceIn(0, 23), (this % 60).coerceIn(0, 59))

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun Long.toLocalDateTime(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

fun LocalDate.startOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

const val DEFAULT_REMINDER_MINUTE_OF_DAY = 9 * 60

fun reminderTriggerMillis(
    dueEpochDay: Int,
    dueMinuteOfDay: Int?,
    offsetMinutes: Int,
    zone: ZoneId = ZoneId.systemDefault(),
): Long {
    val due = ZonedDateTime.of(
        dueEpochDay.toLocalDate(),
        (dueMinuteOfDay ?: DEFAULT_REMINDER_MINUTE_OF_DAY).toLocalTime(),
        zone,
    )
    return due.minusMinutes(offsetMinutes.toLong()).toInstant().toEpochMilli()
}

object WeekdayMask {
    const val ALL = 0b1111111
    const val WEEKDAYS = 0b0011111

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)
    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0
    fun of(days: Collection<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or bit(d) }
    fun toDays(mask: Int): List<DayOfWeek> = DayOfWeek.entries.filter { contains(mask, it) }
    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)
}
