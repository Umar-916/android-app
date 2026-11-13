package com.funsol.orbit.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.repository.SearchRepositoryImpl
import com.funsol.orbit.habit
import com.funsol.orbit.inMemoryDb
import com.funsol.orbit.project
import com.funsol.orbit.task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrityTest {
    private lateinit var db: OrbitDatabase

    @Before fun setUp() { db = inMemoryDb() }
    @After fun tearDown() { db.close() }

    @Test fun deletingAProjectUnlinksTasksInsteadOfDeletingThem() = runBlocking {
        db.projectDao().upsert(project("p"))
        db.taskDao().insert(task("t", projectId = "p"))
        db.projectDao().delete("p")
        val t = db.taskDao().get("t")
        assertNotNull(t)
        assertNull(t!!.projectId)
    }

    @Test fun deletingATaskCascadesToSubtasks() = runBlocking {
        db.taskDao().insert(task("t"))
        db.subtaskDao().upsert(SubtaskEntity("s", "t", "x"))
        db.taskDao().delete("t")
        assertTrue(db.subtaskDao().getAll().isEmpty())
    }

    @Test fun subtaskForMissingTaskIsRejected() = runBlocking {
        try {
            db.subtaskDao().upsert(SubtaskEntity("s", "missing", "x"))
            fail("foreign key must be enforced")
        } catch (_: SQLiteConstraintException) {
        }
    }

    @Test fun habitCheckInIsUniquePerDay() = runBlocking {
        db.habitDao().upsert(habit("h"))
        val first = db.habitDao().insertLog(HabitLogEntity("a", "h", 20001))
        val second = db.habitDao().insertLog(HabitLogEntity("b", "h", 20001))
        assertTrue(first != -1L)
        assertEquals(-1L, second)
        assertEquals(1, db.habitDao().getAllLogs().size)
    }

    @Test fun deletingAHabitDeletesItsHistory() = runBlocking {
        db.habitDao().upsert(habit("h"))
        db.habitDao().insertLog(HabitLogEntity("a", "h", 1))
        db.habitDao().delete("h")
        assertTrue(db.habitDao().getAllLogs().isEmpty())
    }

    @Test fun aRecurringSeriesCannotHaveTwoOccurrencesOnTheSameDay() = runBlocking {
        val base = task("a", dueDate = 100).copy(seriesId = "s", recurrenceType = RecurrenceType.DAILY)
        assertTrue(db.taskDao().insertIgnore(base) != -1L)
        assertEquals(-1L, db.taskDao().insertIgnore(base.copy(id = "b")))
        assertEquals(1, db.taskDao().getAll().size)
    }

    @Test fun deletingANoteFolderKeepsTheNotes() = runBlocking {
        db.noteDao().insertFolder(NoteFolderEntity("f", "Work", 1))
        db.noteDao().upsert(NoteEntity("n", "t", "b", folderId = "f", createdAt = 1, updatedAt = 1))
        db.noteDao().deleteFolder("f")
        assertNull(db.noteDao().get("n")!!.folderId)
    }

    @Test fun searchTreatsPercentAndUnderscoreLiterally() = runBlocking {
        db.taskDao().insert(task("1", title = "Reach 50% goal"))
        db.taskDao().insert(task("2", title = "Reach 500 goal"))
        db.taskDao().insert(task("3", title = "snake_case task"))
        db.taskDao().insert(task("4", title = "snakeXcase task"))
        assertEquals(listOf("1"), db.taskDao().search(SearchRepositoryImpl.likePattern("50%")).map { it.id })
        assertEquals(listOf("3"), db.taskDao().search(SearchRepositoryImpl.likePattern("snake_case")).map { it.id })
    }

    @Test fun flowsEmitUpdates() = runBlocking {
        assertTrue(db.taskDao().observeAll().first().isEmpty())
        db.taskDao().insert(task("t"))
        assertEquals(1, db.taskDao().observeAll().first().size)
    }

    @Test fun thousandsOfTasksQueryQuickly() = runBlocking {
        db.taskDao().insertAll((1..5000).map { task("t$it", title = "Task $it", dueDate = 20000 + it % 365) })
        val start = System.nanoTime()
        val found = db.taskDao().search(SearchRepositoryImpl.likePattern("Task 4999"))
        val ms = (System.nanoTime() - start) / 1_000_000
        assertEquals(1, found.size)
        assertTrue("search took ${ms}ms", ms < 1500)
    }
}
