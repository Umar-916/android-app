package com.funsol.orbit.core.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funsol.orbit.FakeReminderScheduler
import com.funsol.orbit.core.backup.BackupCodec
import com.funsol.orbit.core.backup.BackupFile
import com.funsol.orbit.core.backup.BackupManager
import com.funsol.orbit.core.backup.BackupParseResult
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.inMemoryDb
import com.funsol.orbit.project
import com.funsol.orbit.task
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private lateinit var db: OrbitDatabase
    private lateinit var reminders: FakeReminderScheduler
    private lateinit var repo: TaskRepository
    private val today = LocalDate.of(2025, 3, 10)

    @Before fun setUp() {
        db = inMemoryDb()
        reminders = FakeReminderScheduler()
        repo = TaskRepositoryImpl(db, reminders, now = { 1_000L }, today = { today })
    }

    @After fun tearDown() { db.close() }

    private fun recurring(id: String, due: LocalDate, type: RecurrenceType = RecurrenceType.DAILY) =
        task(id, dueDate = due.toEpochDayInt()).copy(recurrenceType = type)

    @Test fun completingARecurringTaskCreatesExactlyOneSuccessor() = runBlocking {
        val saved = repo.save(recurring("a", today))
        repo.setCompleted(saved.id, true)
        val all = db.taskDao().getAll().sortedBy { it.dueDate }
        assertEquals(2, all.size)
        assertEquals(TaskStatus.COMPLETED, all[0].status)
        assertEquals(TaskStatus.TODO, all[1].status)
        assertEquals(today.plusDays(1).toEpochDayInt(), all[1].dueDate)
        assertEquals(all[0].seriesId, all[1].seriesId)
    }

    @Test fun completingTwiceDoesNotDuplicateTheSuccessor() = runBlocking {
        val saved = repo.save(recurring("a", today))
        repo.setCompleted(saved.id, true)
        repo.setCompleted(saved.id, true)
        assertEquals(2, db.taskDao().getAll().size)
    }

    @Test fun reopeningAndCompletingAgainStillDoesNotDuplicate() = runBlocking {
        val saved = repo.save(recurring("a", today))
        repo.setCompleted(saved.id, true)
        repo.setCompleted(saved.id, false)
        repo.setCompleted(saved.id, true)
        assertEquals(2, db.taskDao().getAll().size)
    }

    @Test fun completingOneOccurrenceLeavesTheOthersAlone() = runBlocking {
        val first = repo.save(recurring("a", today))
        repo.setCompleted(first.id, true)
        val successor = db.taskDao().getAll().first { it.id != first.id }
        assertEquals(TaskStatus.TODO, successor.status)
        repo.setCompleted(successor.id, true)
        val statuses = db.taskDao().getAll().groupBy { it.status }.mapValues { it.value.size }
        assertEquals(2, statuses[TaskStatus.COMPLETED])
        assertEquals(1, statuses[TaskStatus.TODO])
    }

    @Test fun nonRecurringTaskHasNoSuccessor() = runBlocking {
        val saved = repo.save(task("a", dueDate = today.toEpochDayInt()))
        repo.setCompleted(saved.id, true)
        assertEquals(1, db.taskDao().getAll().size)
    }

    @Test fun staleOccurrenceSuccessorLandsOnToday() = runBlocking {
        val saved = repo.save(recurring("a", today.minusDays(10)))
        repo.setCompleted(saved.id, true)
        val next = db.taskDao().getAll().first { it.status == TaskStatus.TODO }
        assertEquals(today.toEpochDayInt(), next.dueDate)
    }

    @Test fun subtasksAreCopiedUncheckedToTheSuccessor() = runBlocking {
        val saved = repo.save(
            recurring("a", today),
            listOf(com.funsol.orbit.core.database.entity.SubtaskEntity("s1", "a", "step", done = true)),
        )
        repo.setCompleted(saved.id, true)
        val successor = db.taskDao().getAll().first { it.id != saved.id }
        val copied = db.subtaskDao().getFor(successor.id)
        assertEquals(1, copied.size)
        assertEquals(false, copied[0].done)
    }

    @Test fun deletingATaskCancelsItsReminder() = runBlocking {
        repo.save(task("a", dueDate = today.toEpochDayInt()))
        repo.delete("a")
        assertTrue("a" in reminders.cancelled)
    }

    @Test fun savingAnArchivedOrCompletedTaskStillGoesThroughTheSchedulerSoItCanCancel() = runBlocking {
        repo.save(task("a", dueDate = today.toEpochDayInt()).copy(status = TaskStatus.ARCHIVED))
        assertTrue("a" in reminders.synced)
    }

    @Test fun deletingAProjectWithoutDeletingTasksKeepsThem() = runBlocking {
        val projects = ProjectRepositoryImpl(db, reminders, now = { 1L })
        projects.save(project("p"))
        repo.save(task("a", projectId = "p"))
        projects.delete("p", deleteTasks = false)
        assertEquals(1, db.taskDao().getAll().size)
        assertEquals(null, db.taskDao().get("a")!!.projectId)
    }

    @Test fun deletingAProjectWithTasksDeletesThemAndCancelsReminders() = runBlocking {
        val projects = ProjectRepositoryImpl(db, reminders, now = { 1L })
        projects.save(project("p"))
        repo.save(task("a", projectId = "p"))
        repo.save(task("b", projectId = "p"))
        repo.save(task("keep"))
        projects.delete("p", deleteTasks = true)
        assertEquals(listOf("keep"), db.taskDao().getAll().map { it.id })
        assertTrue(reminders.cancelled.containsAll(listOf("a", "b")))
    }

    @Test fun goalProgressNeverDividesByZeroAndCompletesAtTarget() = runBlocking {
        val goals = GoalRepositoryImpl(db, now = { 5L })
        val saved = goals.save(GoalEntity("g", "Read", targetValue = 0.0, currentValue = -3.0, createdAt = 1, updatedAt = 1))
        assertTrue(saved.targetValue > 0)
        assertEquals(0.0, saved.currentValue, 0.0)
        goals.updateProgress("g", saved.targetValue)
        assertEquals(GoalStatus.COMPLETED, goals.get("g")!!.status)
        goals.updateProgress("g", 0.0)
        assertEquals(GoalStatus.ACTIVE, goals.get("g")!!.status)
        goals.updateProgress("g", Double.NaN)
        assertEquals(0.0, goals.get("g")!!.currentValue, 0.0)
    }

    @Test fun backupRestoreRoundTripsAndFailedRestoreKeepsExistingData() = runBlocking {
        repo.save(task("a", title = "Original"))
        val manager = BackupManager(db, reminders)
        val json = manager.export()
        val parsed = manager.parse(json) as BackupParseResult.Valid

        repo.save(task("b", title = "Added later"))
        manager.restore(parsed.file)
        assertEquals(listOf("a"), db.taskDao().getAll().map { it.id })

        val broken = BackupFile(tasks = listOf(task("x"), task("x")))
        try {
            manager.restore(broken)
            fail("duplicate primary keys must fail")
        } catch (_: Exception) {
        }
        assertEquals(listOf("a"), db.taskDao().getAll().map { it.id })
        assertTrue(BackupCodec.parse(BackupCodec.encode(broken)) is BackupParseResult.Invalid)
    }
}
