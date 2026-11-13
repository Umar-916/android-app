package com.funsol.orbit.feature.calendar

import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarLogicTest {
    private val today = LocalDate.of(2026, 11, 12)
    private val todayDay = today.toEpochDay().toInt()

    private fun task(
        id: String,
        due: Int? = todayDay,
        time: Int? = null,
        priority: Priority = Priority.MEDIUM,
        status: TaskStatus = TaskStatus.TODO,
    ) = TaskEntity(id = id, title = id, priority = priority, status = status, createdAt = 0, updatedAt = 0, dueDate = due, dueTime = time)

    @Test
    fun dayInfoCapsDotsAtThreeAndOrdersOpenUrgentFirst() {
        val tasks = listOf(
            task("done", status = TaskStatus.COMPLETED, priority = Priority.URGENT),
            task("low", priority = Priority.LOW),
            task("urgent", priority = Priority.URGENT),
            task("high", priority = Priority.HIGH),
        )
        val info = CalendarLogic.dayInfo(tasks, today)
        assertEquals(3, info.dots.size)
        assertEquals(listOf(Priority.URGENT, Priority.HIGH, Priority.LOW), info.dots.map { it.priority })
        assertTrue(info.dots.all { it.kind == DotKind.OPEN })
        assertEquals(4, info.total)
        assertEquals(3, info.open)
    }

    @Test
    fun dotKindsDistinguishOverdueOpenAndCompleted() {
        val past = todayDay - 2
        val info = CalendarLogic.dayInfo(
            listOf(
                task("od", due = past),
                task("done", due = past, status = TaskStatus.COMPLETED),
            ),
            today,
        )
        assertEquals(listOf(DotKind.OVERDUE, DotKind.COMPLETED), info.dots.map { it.kind })
        assertEquals(1, info.overdue)
    }

    @Test
    fun archivedAndUndatedTasksAreNotOnTheCalendar() {
        val all = listOf(
            task("a"),
            task("b", status = TaskStatus.ARCHIVED),
            task("c", due = null),
        )
        val byDay = CalendarLogic.groupByDay(all)
        assertEquals(setOf(todayDay), byDay.keys)
        assertEquals(listOf("a"), byDay.getValue(todayDay).map { it.id })
    }

    @Test
    fun dayScheduleSplitsAllDayFromTimedAndOrdersByTime() {
        val schedule = CalendarLogic.daySchedule(
            listOf(
                task("pm", time = 15 * 60),
                task("allday"),
                task("am", time = 8 * 60 + 30),
            ),
        )
        assertEquals(listOf("allday"), schedule.allDay.map { it.id })
        assertEquals(listOf("am", "pm"), schedule.timed.map { it.id })
        assertEquals(3, schedule.count)
    }

    @Test
    fun agendaStartsWithOverdueThenOpenTasksFromTodayGroupedByDay() {
        val all = listOf(
            task("future", due = todayDay + 2),
            task("overdue", due = todayDay - 3),
            task("doneToday", status = TaskStatus.COMPLETED),
            task("today2", time = 600),
            task("today1", time = 480),
            task("doneOverdue", due = todayDay - 1, status = TaskStatus.COMPLETED),
            task("nodate", due = null),
        )
        val agenda = CalendarLogic.buildAgenda(all, today)
        assertEquals(3, agenda.size)
        assertEquals(AgendaKey.Overdue, agenda[0].key)
        assertEquals(listOf("overdue"), agenda[0].tasks.map { it.id })
        assertEquals(AgendaKey.Day(todayDay), agenda[1].key)
        assertEquals(listOf("today1", "today2"), agenda[1].tasks.map { it.id })
        assertEquals(AgendaKey.Day(todayDay + 2), agenda[2].key)
    }

    @Test
    fun emptyAgendaHasNoGroups() {
        assertTrue(CalendarLogic.buildAgenda(emptyList(), today).isEmpty())
    }
}
