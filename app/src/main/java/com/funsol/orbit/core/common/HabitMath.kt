package com.funsol.orbit.core.common

import java.time.LocalDate

data class HabitStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val completionPercent: Int,
    val scheduledDays: Int,
    val completedDays: Int,
)

object HabitMath {
    fun isScheduled(mask: Int, day: LocalDate): Boolean {
        val effective = if (mask and WeekdayMask.ALL == 0) WeekdayMask.ALL else mask
        return WeekdayMask.contains(effective, day.dayOfWeek)
    }

    fun stats(scheduleMask: Int, start: LocalDate, logs: Set<LocalDate>, today: LocalDate): HabitStats {
        if (start.isAfter(today)) return HabitStats(0, 0, 0, 0, 0)

        var longest = 0
        var run = 0
        var scheduled = 0
        var completed = 0
        var day = start
        while (!day.isAfter(today)) {
            if (isScheduled(scheduleMask, day)) {
                val done = day in logs
                val decided = done || day.isBefore(today)
                if (decided) scheduled++
                if (done) {
                    completed++
                    run++
                    if (run > longest) longest = run
                } else if (day.isBefore(today)) {
                    run = 0
                }
            }
            day = day.plusDays(1)
        }

        var current = 0
        var cursor = today
        var first = true
        while (!cursor.isBefore(start)) {
            if (isScheduled(scheduleMask, cursor)) {
                if (cursor in logs) {
                    current++
                } else if (!(first && cursor == today)) {
                    break
                }
                first = false
            }
            cursor = cursor.minusDays(1)
        }

        val percent = if (scheduled == 0) 0 else kotlin.math.round(completed * 100f / scheduled).toInt().coerceIn(0, 100)
        return HabitStats(current, longest, percent, scheduled, completed)
    }
}

object GoalMath {
    fun fraction(current: Double, target: Double): Float {
        if (!target.isFinite() || !current.isFinite() || target <= 0.0) return 0f
        return (current / target).coerceIn(0.0, 1.0).toFloat()
    }

    fun percent(current: Double, target: Double): Int = (fraction(current, target) * 100).toInt()

    fun isReached(current: Double, target: Double): Boolean = target > 0.0 && current >= target
}
