package com.funsol.orbit.core.common

import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object TaskRules {
    fun isOpen(t: TaskEntity) = t.status == TaskStatus.TODO || t.status == TaskStatus.IN_PROGRESS
    fun isOverdue(t: TaskEntity, today: LocalDate) = isOpen(t) && t.dueDate != null && t.dueDate < today.toEpochDayInt()
    fun isDueToday(t: TaskEntity, today: LocalDate) = isOpen(t) && t.dueDate == today.toEpochDayInt()
    fun isUpcoming(t: TaskEntity, today: LocalDate) = isOpen(t) && t.dueDate != null && t.dueDate > today.toEpochDayInt()
    fun isCompleted(t: TaskEntity) = t.status == TaskStatus.COMPLETED
    fun isArchived(t: TaskEntity) = t.status == TaskStatus.ARCHIVED
}

data class Progress(val done: Int, val total: Int) {
    val fraction: Float get() = if (total <= 0) 0f else (done.toFloat() / total).coerceIn(0f, 1f)
    val percent: Int get() = (fraction * 100).toInt()
}

object ProgressMath {
    fun ofTasks(tasks: List<TaskEntity>): Progress {
        val counted = tasks.filter { it.status != TaskStatus.ARCHIVED }
        return Progress(counted.count { it.status == TaskStatus.COMPLETED }, counted.size)
    }
}

object DateFormats {
    fun date(epochDay: Int, locale: Locale = Locale.getDefault()): String =
        epochDay.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun dateShort(epochDay: Int, locale: Locale = Locale.getDefault()): String =
        epochDay.toLocalDate().format(DateTimeFormatter.ofPattern("d MMM", locale))

    fun weekdayLong(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("EEEE", locale))

    fun fullDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))

    fun monthYear(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale))

    fun time(minuteOfDay: Int, locale: Locale = Locale.getDefault()): String =
        minuteOfDay.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

    fun dateTime(millis: Long, locale: Locale = Locale.getDefault()): String =
        millis.toLocalDateTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
}
