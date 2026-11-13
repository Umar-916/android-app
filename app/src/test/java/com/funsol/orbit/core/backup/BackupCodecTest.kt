package com.funsol.orbit.core.backup

import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.repository.SearchRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private fun sample() = BackupFile(
        exportedAt = 1_700_000_000_000,
        projects = listOf(ProjectEntity("p1", "Launch", createdAt = 1, updatedAt = 1)),
        tasks = listOf(
            TaskEntity("t1", "Write, \"spec\"", projectId = "p1", dueDate = 20000, createdAt = 1, updatedAt = 1),
            TaskEntity("t2", "Repeat", dueDate = 20001, recurrenceType = RecurrenceType.DAILY, seriesId = "t2", createdAt = 1, updatedAt = 1),
        ),
        subtasks = listOf(SubtaskEntity("s1", "t1", "outline")),
        noteFolders = listOf(NoteFolderEntity("f1", "Work", 1)),
        notes = listOf(NoteEntity("n1", "Title", "Body ✓ اردو", folderId = "f1", createdAt = 1, updatedAt = 2)),
        habits = listOf(HabitEntity("h1", "Read", startDate = 20000, createdAt = 1)),
        habitLogs = listOf(HabitLogEntity("l1", "h1", 20001)),
        goals = listOf(GoalEntity("g1", "Run", targetValue = 100.0, currentValue = 10.0, createdAt = 1, updatedAt = 1)),
    )

    @Test fun `export then parse round trips every table`() {
        val original = sample()
        val result = BackupCodec.parse(BackupCodec.encode(original))
        assertTrue(result is BackupParseResult.Valid)
        val valid = result as BackupParseResult.Valid
        assertEquals(original, valid.file)
        assertEquals(2, valid.summary.tasks)
        assertEquals(1, valid.summary.notes)
    }

    @Test fun `garbage is rejected`() {
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.NOT_A_BACKUP), BackupCodec.parse("hello world"))
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.NOT_A_BACKUP), BackupCodec.parse(""))
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.NOT_A_BACKUP), BackupCodec.parse("[1,2,3]"))
    }

    @Test fun `truncated json is rejected`() {
        val text = BackupCodec.encode(sample())
        val result = BackupCodec.parse(text.substring(0, text.length / 2))
        assertTrue(result is BackupParseResult.Invalid)
    }

    @Test fun `a json file from another app is rejected`() {
        val result = BackupCodec.parse("""{"app":"other","formatVersion":1}""")
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.NOT_A_BACKUP), result)
    }

    @Test fun `newer format versions are refused instead of half imported`() {
        val result = BackupCodec.parse("""{"app":"orbit-planner","formatVersion":99}""")
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.UNSUPPORTED_VERSION), result)
    }

    @Test fun `unknown fields and unknown enum values are tolerated`() {
        val text = BackupCodec.encode(sample())
            .replace("\"status\": \"TODO\"", "\"status\": \"SOMETHING_NEW\"")
            .replaceFirst("{", "{\"futureField\": 1,")
        val result = BackupCodec.parse(text)
        assertTrue(result is BackupParseResult.Valid)
    }

    @Test fun `dangling references are inconsistent`() {
        val bad = sample().copy(tasks = sample().tasks + TaskEntity("t3", "Orphan", projectId = "missing", createdAt = 1, updatedAt = 1))
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.INCONSISTENT), BackupCodec.parse(BackupCodec.encode(bad)))

        val badLog = sample().copy(habitLogs = sample().habitLogs + HabitLogEntity("l2", "nope", 1))
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.INCONSISTENT), BackupCodec.parse(BackupCodec.encode(badLog)))

        val badSub = sample().copy(subtasks = listOf(SubtaskEntity("s9", "ghost", "x")))
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.INCONSISTENT), BackupCodec.parse(BackupCodec.encode(badSub)))
    }

    @Test fun `duplicate ids and duplicate check ins are inconsistent`() {
        val dupTask = sample().copy(tasks = sample().tasks + sample().tasks.first())
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.INCONSISTENT), BackupCodec.parse(BackupCodec.encode(dupTask)))

        val dupLog = sample().copy(habitLogs = sample().habitLogs + HabitLogEntity("l-other-id", "h1", 20001))
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.INCONSISTENT), BackupCodec.parse(BackupCodec.encode(dupLog)))
    }

    @Test fun `duplicate recurring occurrences are inconsistent`() {
        val dup = sample().copy(
            tasks = sample().tasks + TaskEntity("t9", "Repeat", dueDate = 20001, seriesId = "t2", createdAt = 1, updatedAt = 1),
        )
        assertEquals(BackupParseResult.Invalid(BackupParseResult.Reason.INCONSISTENT), BackupCodec.parse(BackupCodec.encode(dup)))
    }

    @Test fun `an empty backup is valid`() {
        assertTrue(BackupCodec.parse(BackupCodec.encode(BackupFile())) is BackupParseResult.Valid)
    }

    @Test fun `csv cells are quoted and neutralise formula injection`() {
        assertEquals("plain", BackupCodec.csvCell("plain"))
        assertEquals("\"a,b\"", BackupCodec.csvCell("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", BackupCodec.csvCell("say \"hi\""))
        assertEquals("\"line1\nline2\"", BackupCodec.csvCell("line1\nline2"))
        assertEquals("'=SUM(A1)", BackupCodec.csvCell("=SUM(A1)"))
        assertEquals("'+1", BackupCodec.csvCell("+1"))
        assertEquals("a,b", BackupCodec.csvRow("a", "b"))
    }

    @Test fun `like patterns escape wildcards`() {
        assertEquals("%50\\%%", SearchRepositoryImpl.likePattern("50%"))
        assertEquals("%a\\_b%", SearchRepositoryImpl.likePattern("a_b"))
        assertEquals("%c:\\\\x%", SearchRepositoryImpl.likePattern("c:\\x"))
    }
}
