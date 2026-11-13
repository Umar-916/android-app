package com.funsol.orbit.feature.tasks

import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.WeekdayMask
import java.time.DayOfWeek

data class RecurrenceDescriptor(
    val type: RecurrenceType,
    val interval: Int,
    val days: List<DayOfWeek>,
    val endEpochDay: Int?,
)

object RecurrenceSummary {
    const val MAX_INTERVAL = 999

    fun describe(type: RecurrenceType, interval: Int, weekdayMask: Int, endEpochDay: Int?): RecurrenceDescriptor? {
        if (type == RecurrenceType.NONE) return null
        return RecurrenceDescriptor(
            type = type,
            interval = interval.coerceIn(1, MAX_INTERVAL),
            days = if (type == RecurrenceType.WEEKLY) WeekdayMask.toDays(weekdayMask and WeekdayMask.ALL) else emptyList(),
            endEpochDay = endEpochDay,
        )
    }
}
