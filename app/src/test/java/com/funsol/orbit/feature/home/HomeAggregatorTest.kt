package com.funsol.orbit.feature.home

import com.funsol.orbit.core.common.WeekdayMask
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class HomeAggregatorTest {
    private val zone = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 3, 10)

    private fun millis(date: LocalDate, h: Int = 0, m: Int = 0, s: Int = 0): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(h, m, s)).toInstant(zone).toEpochMilli()

    private fun task(
        id: String,
        status: TaskStatus = TaskStatus.TODO,
        due: LocalDate? = null,
        completedAt: Long? = null,
        priority: Priority = Priority.MEDIUM,
        projectId: String? = null,
        dueTime: Int? = null,
    ) = TaskEntity(
        id = id, title = id, status = status, dueDate = due?.toEpochDayInt(), completedAt = completedAt,
        priority = priority, projectId = projectId, dueTime = dueTime, createdAt = 1, updatedAt = 1,
    )

    private fun aggregate(
        tasks: List<TaskEntity> = emptyList(),
        projects: List<ProjectEntity> = emptyList(),
        habits: List<HabitEntity> = emptyList(),
        logs: List<HabitLogEntity> = emptyList(),
        day: LocalDate = today,
    ) = HomeAggregator.aggregate(tasks, projects, habits, logs, day, zone)

    @Test
    fun emptyDatabaseGivesBlankStateAndNoProgress() {
        val data = aggregate()
        assertTrue(data.isBlank)
        assertEquals(0, data.progress.total)
        assertEquals(0f, data.progress.fraction, 0f)
        assertEquals(0, data.overview.bestStreak)
    }

    @Test
    fun progressCountsCompletedTodayAndOpenDueToday() {
        val tasks = listOf(
            task("done-today", TaskStatus.COMPLETED, completedAt = millis(today, 9)),
            task("open-today", due = today),
            task("open-today-2", due = today),
            task("done-yesterday", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(1), 20)),
            task("open-tomorrow", due = today.plusDays(1)),
        )
        val p = aggregate(tasks).progress
        assertEquals(1, p.done)
        assertEquals(3, p.total)
    }

    @Test
    fun completedTaskDueTodayCountsEvenIfCompletedEarlier() {
        val tasks = listOf(task("t", TaskStatus.COMPLETED, due = today, completedAt = millis(today.minusDays(1), 22)))
        assertEquals(1, aggregate(tasks).progress.done)
    }

    @Test
    fun midnightBoundaryDecidesWhichDayACompletionBelongsTo() {
        val lastSecond = task("a", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(1), 23, 59, 59))
        val firstSecond = task("b", TaskStatus.COMPLETED, completedAt = millis(today, 0, 0, 0))
        assertEquals(1, aggregate(listOf(lastSecond, firstSecond)).progress.done)
        assertEquals(1, aggregate(listOf(lastSecond, firstSecond), day = today.minusDays(1)).progress.done)
        assertEquals(0, aggregate(listOf(firstSecond), day = today.minusDays(1)).progress.done)
    }

    @Test
    fun taskDueTodayBecomesOverdueAfterMidnight() {
        val t = task("t", due = today)
        assertEquals(1, aggregate(listOf(t)).todayCount)
        assertEquals(0, aggregate(listOf(t)).overdueCount)
        val next = aggregate(listOf(t), day = today.plusDays(1))
        assertEquals(0, next.todayCount)
        assertEquals(1, next.overdueCount)
    }

    @Test
    fun upcomingCoversNextSevenDaysOnlyAndGroupsByDay() {
        val tasks = listOf(
            task("today", due = today),
            task("d1a", due = today.plusDays(1)),
            task("d1b", due = today.plusDays(1)),
            task("d7", due = today.plusDays(7)),
            task("d8", due = today.plusDays(8)),
            task("completed", TaskStatus.COMPLETED, due = today.plusDays(2), completedAt = 5),
        )
        val data = aggregate(tasks)
        assertEquals(3, data.upcomingCount)
        assertEquals(listOf(today.plusDays(1).toEpochDayInt(), today.plusDays(7).toEpochDayInt()), data.upcomingGroups.map { it.epochDay })
        assertEquals(2, data.upcomingGroups.first().tasks.size)
    }

    @Test
    fun overdueIsSortedOldestFirstAndCapped() {
        val tasks = (1..8).map { task("o$it", due = today.minusDays(it.toLong())) }
        val data = aggregate(tasks)
        assertEquals(8, data.overdueCount)
        assertEquals(HomeAggregator.MAX_OVERDUE, data.overdueTasks.size)
        assertEquals("o8", data.overdueTasks.first().id)
    }

    @Test
    fun todayListSortsByTimeThenPriority() {
        val tasks = listOf(
            task("no-time-low", due = today, priority = Priority.LOW),
            task("late", due = today, dueTime = 18 * 60),
            task("early", due = today, dueTime = 8 * 60),
            task("no-time-urgent", due = today, priority = Priority.URGENT),
        )
        assertEquals(listOf("early", "late", "no-time-urgent", "no-time-low"), aggregate(tasks).todayTasks.map { it.id })
    }

    @Test
    fun onlyActiveProjectsAreListedWithRealProgress() {
        val active = ProjectEntity("p1", "Alpha", createdAt = 1, updatedAt = 1)
        val hold = ProjectEntity("p2", "Beta", status = ProjectStatus.ON_HOLD, createdAt = 1, updatedAt = 1)
        val tasks = listOf(
            task("a", TaskStatus.COMPLETED, projectId = "p1", completedAt = 1),
            task("b", projectId = "p1"),
            task("c", TaskStatus.ARCHIVED, projectId = "p1"),
        )
        val data = aggregate(tasks, listOf(active, hold))
        assertEquals(1, data.projects.size)
        assertEquals(1, data.activeProjectCount)
        assertEquals(1, data.projects.first().progress.done)
        assertEquals(2, data.projects.first().progress.total)
    }

    @Test
    fun habitsListOnlyThoseScheduledTodayAndReflectCheckIns() {
        val everyDay = HabitEntity("h1", "Read", startDate = today.minusDays(10).toEpochDayInt(), createdAt = 1)
        val mondaysOnly = HabitEntity(
            "h2", "Gym", scheduleMask = WeekdayMask.bit(DayOfWeek.MONDAY),
            startDate = today.minusDays(10).toEpochDayInt(), createdAt = 1,
        )
        val archived = HabitEntity("h3", "Old", archived = true, startDate = today.minusDays(10).toEpochDayInt(), createdAt = 1)
        val logs = (0..2).map { HabitLogEntity("l$it", "h1", today.minusDays(it.toLong()).toEpochDayInt()) }
        val data = aggregate(habits = listOf(everyDay, mondaysOnly, archived), logs = logs)
        assertEquals(listOf("h1"), data.habits.map { it.habit.id })
        assertTrue(data.habits.single().checkedToday)
        assertEquals(1, data.habitsDone)
        assertEquals(3, data.habits.single().currentStreak)
        assertEquals(3, data.overview.bestStreak)
    }

    @Test
    fun overviewCountsCompletionsOfTheLastSevenDays() {
        val tasks = listOf(
            task("in-1", TaskStatus.COMPLETED, completedAt = millis(today, 10)),
            task("in-2", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(6), 0, 0, 0)),
            task("out", TaskStatus.COMPLETED, completedAt = millis(today.minusDays(7), 23, 59, 59)),
            task("open"),
        )
        val o = aggregate(tasks).overview
        assertEquals(2, o.completedLast7Days)
        assertEquals(1, o.openTasks)
        assertFalse(aggregate(tasks).isBlank)
    }
}
