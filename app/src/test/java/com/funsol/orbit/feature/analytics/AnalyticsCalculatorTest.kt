package com.funsol.orbit.feature.analytics

import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class AnalyticsCalculatorTest {
    private val zone = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 3, 10)

    private fun millis(date: LocalDate, h: Int = 12, m: Int = 0, s: Int = 0, z: ZoneId = zone): Long =
        LocalDateTime.of(date, LocalTime.of(h, m, s)).atZone(z).toInstant().toEpochMilli()

    private fun task(
        id: String,
        status: TaskStatus = TaskStatus.TODO,
        due: LocalDate? = null,
        completedAt: Long? = null,
        projectId: String? = null,
    ) = TaskEntity(
        id = id, title = id, status = status, dueDate = due?.toEpochDayInt(), completedAt = completedAt,
        projectId = projectId, createdAt = 1, updatedAt = 1,
    )

    private fun calc(
        tasks: List<TaskEntity> = emptyList(),
        projects: List<ProjectEntity> = emptyList(),
        habits: List<HabitEntity> = emptyList(),
        logs: List<HabitLogEntity> = emptyList(),
        goals: List<GoalEntity> = emptyList(),
        range: AnalyticsRange = AnalyticsRange.WEEK,
        zoneId: ZoneId = zone,
        day: LocalDate = today,
    ) = AnalyticsCalculator.calculate(tasks, projects, habits, logs, goals, day, range, zoneId)

    @Test
    fun emptyDataIsEmptyWithoutNaN() {
        val d = calc()
        assertTrue(d.isEmpty)
        assertNull(d.completionRatePercent)
        assertEquals(7, d.daily.size)
        assertEquals(AnalyticsCalculator.WEEKLY_BUCKETS, d.weekly.size)
        assertTrue(d.daily.all { it.value == 0 })
        assertEquals(0, d.completedAllTime)
    }

    @Test
    fun summaryTilesCountRealTasks() {
        val tasks = listOf(
            task("c1", TaskStatus.COMPLETED, completedAt = millis(today)),
            task("c2", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(20))),
            task("o1", due = today.minusDays(1)),
            task("o2", due = today.plusDays(1)),
            task("o3"),
            task("a", TaskStatus.ARCHIVED),
        )
        val d = calc(tasks)
        assertEquals(2, d.completedAllTime)
        assertEquals(1, d.completedLast7Days)
        assertEquals(1, d.completedInRange)
        assertEquals(1, d.overdueNow)
        assertEquals(3, d.openTasks)
        assertFalse(d.isEmpty)
        assertEquals(2, calc(tasks, range = AnalyticsRange.MONTH).completedInRange)
    }

    @Test
    fun completionRateUsesTasksDueInRangeOnly() {
        val tasks = listOf(
            task("done", TaskStatus.COMPLETED, due = today.minusDays(2), completedAt = millis(today.minusDays(2))),
            task("open", due = today.minusDays(1)),
            task("future", due = today.plusDays(3)),
            task("old", TaskStatus.COMPLETED, due = today.minusDays(20), completedAt = millis(today.minusDays(20))),
            task("archived", TaskStatus.ARCHIVED, due = today),
        )
        assertEquals(50, calc(tasks).completionRatePercent)
        assertEquals(66, calc(tasks, range = AnalyticsRange.MONTH).completionRatePercent)
    }

    @Test
    fun dailyBarsRunOldestToNewestAndRespectDayBoundaries() {
        val tasks = listOf(
            task("a", TaskStatus.COMPLETED, completedAt = millis(today, 0, 0, 0)),
            task("b", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(1), 23, 59, 59)),
            task("c", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(6), 0, 0, 0)),
            task("d", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(7), 23, 59, 59)),
        )
        val bars = calc(tasks).daily
        assertEquals(today.minusDays(6), bars.first().start)
        assertEquals(today, bars.last().start)
        assertEquals(listOf(1, 0, 0, 0, 0, 1, 1), bars.map { it.value })
    }

    @Test
    fun weeklyBucketsAreContiguousSevenDayWindows() {
        val tasks = listOf(
            task("w5", TaskStatus.COMPLETED, completedAt = millis(today)),
            task("w4", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(7))),
            task("w4b", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(13))),
            task("w1", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(34))),
            task("beyond", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(35))),
        )
        val weekly = calc(tasks).weekly
        assertEquals(listOf(1, 0, 0, 2, 1), weekly.map { it.value })
        weekly.zipWithNext().forEach { (a, b) -> assertEquals(a.end.plusDays(1), b.start) }
        assertEquals(today, weekly.last().end)
    }

    @Test
    fun dstTransitionDoesNotShiftCompletionsToTheWrongDay() {
        val berlin = ZoneId.of("Europe/Berlin")
        val springForward = LocalDate.of(2026, 3, 29)
        val tasks = listOf(
            task("late", TaskStatus.COMPLETED, completedAt = millis(springForward, 23, 30, z = berlin)),
            task("early", TaskStatus.COMPLETED, completedAt = millis(springForward.plusDays(1), 0, 15, z = berlin)),
        )
        val bars = calc(tasks, zoneId = berlin, day = springForward.plusDays(1)).daily
        assertEquals(1, bars.single { it.start == springForward }.value)
        assertEquals(1, bars.single { it.start == springForward.plusDays(1) }.value)
    }

    @Test
    fun projectsExcludeArchivedAndUseProgressMath() {
        val p1 = ProjectEntity("p1", "Alpha", createdAt = 1, updatedAt = 1)
        val p2 = ProjectEntity("p2", "Old", status = ProjectStatus.ARCHIVED, createdAt = 1, updatedAt = 1)
        val tasks = listOf(
            task("a", TaskStatus.COMPLETED, projectId = "p1", completedAt = 1),
            task("b", projectId = "p1"),
        )
        val d = calc(tasks, projects = listOf(p1, p2))
        assertEquals(listOf("p1"), d.projects.map { it.project.id })
        assertEquals(50, d.projects.single().progress.percent)
    }

    @Test
    fun habitsAndGoalsUseTheSharedMathHelpers() {
        val habit = HabitEntity("h", "Read", startDate = today.minusDays(3).toEpochDayInt(), createdAt = 1)
        val archived = HabitEntity("x", "Gone", archived = true, startDate = today.toEpochDayInt(), createdAt = 1)
        val logs = listOf(
            HabitLogEntity("1", "h", today.minusDays(2).toEpochDayInt()),
            HabitLogEntity("2", "h", today.minusDays(1).toEpochDayInt()),
            HabitLogEntity("3", "h", today.toEpochDayInt()),
        )
        val g1 = GoalEntity("g1", "Run", targetValue = 200.0, currentValue = 50.0, createdAt = 1, updatedAt = 1)
        val g2 = GoalEntity("g2", "Archived", status = GoalStatus.ARCHIVED, createdAt = 1, updatedAt = 1)
        val d = calc(habits = listOf(habit, archived), logs = logs, goals = listOf(g1, g2))
        assertEquals(1, d.habits.size)
        assertEquals(3, d.habits.single().stats.currentStreak)
        assertEquals(75, d.habits.single().stats.completionPercent)
        assertEquals(listOf(25), d.goals.map { it.percent })
    }
}
