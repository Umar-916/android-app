package com.funsol.orbit.core.backup

import androidx.room.withTransaction
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.notifications.ReminderScheduler
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val app: String = APP_ID,
    val formatVersion: Int = FORMAT_VERSION,
    val dbVersion: Int = OrbitDatabase.VERSION,
    val exportedAt: Long = 0,
    val projects: List<ProjectEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val subtasks: List<SubtaskEntity> = emptyList(),
    val noteFolders: List<NoteFolderEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
) {
    companion object {
        const val APP_ID = "orbit-planner"
        const val FORMAT_VERSION = 1
    }
}

data class BackupSummary(
    val tasks: Int,
    val projects: Int,
    val notes: Int,
    val habits: Int,
    val goals: Int,
    val exportedAt: Long,
)

sealed interface BackupParseResult {
    data class Valid(val file: BackupFile, val summary: BackupSummary) : BackupParseResult
    data class Invalid(val reason: Reason) : BackupParseResult
    enum class Reason { NOT_A_BACKUP, UNSUPPORTED_VERSION, CORRUPT, INCONSISTENT }
}

object BackupCodec {
    private const val MIN_EPOCH_DAY = 0
    private const val MAX_EPOCH_DAY = 100_000

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun parse(text: String): BackupParseResult {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            return BackupParseResult.Invalid(BackupParseResult.Reason.NOT_A_BACKUP)
        } catch (e: IllegalArgumentException) {
            return BackupParseResult.Invalid(BackupParseResult.Reason.CORRUPT)
        }
        if (file.app != BackupFile.APP_ID) return BackupParseResult.Invalid(BackupParseResult.Reason.NOT_A_BACKUP)
        if (file.formatVersion > BackupFile.FORMAT_VERSION || file.formatVersion < 1) {
            return BackupParseResult.Invalid(BackupParseResult.Reason.UNSUPPORTED_VERSION)
        }
        if (!isConsistent(file)) return BackupParseResult.Invalid(BackupParseResult.Reason.INCONSISTENT)
        return BackupParseResult.Valid(
            file,
            BackupSummary(file.tasks.size, file.projects.size, file.notes.size, file.habits.size, file.goals.size, file.exportedAt),
        )
    }

    fun isConsistent(f: BackupFile): Boolean {
        fun <T> unique(items: List<T>, key: (T) -> String) = items.map(key).toSet().size == items.size
        if (!unique(f.projects) { it.id } || !unique(f.tasks) { it.id } || !unique(f.subtasks) { it.id } ||
            !unique(f.noteFolders) { it.id } || !unique(f.notes) { it.id } || !unique(f.habits) { it.id } ||
            !unique(f.habitLogs) { it.id } || !unique(f.goals) { it.id }
        ) return false
        if (f.habitLogs.map { it.habitId to it.date }.toSet().size != f.habitLogs.size) return false
        val series = f.tasks.filter { it.seriesId != null && it.dueDate != null }.map { it.seriesId to it.dueDate }
        if (series.toSet().size != series.size) return false
        if (f.noteFolders.map { it.name.lowercase() }.toSet().size != f.noteFolders.size) return false

        val dayRange = MIN_EPOCH_DAY..MAX_EPOCH_DAY
        if (!f.tasks.all { t ->
                (t.dueDate == null || t.dueDate in dayRange) &&
                    (t.dueTime == null || t.dueTime in 0..1439) &&
                    t.recurrenceInterval in 1..1000 &&
                    t.recurrenceWeekdays in 0..127
            }
        ) return false
        if (!f.habits.all { it.startDate in dayRange }) return false

        val projectIds = f.projects.map { it.id }.toSet()
        val taskIds = f.tasks.map { it.id }.toSet()
        val habitIds = f.habits.map { it.id }.toSet()
        val folderIds = f.noteFolders.map { it.id }.toSet()
        return f.tasks.all { it.projectId == null || it.projectId in projectIds } &&
            f.subtasks.all { it.taskId in taskIds } &&
            f.habitLogs.all { it.habitId in habitIds } &&
            f.notes.all { it.folderId == null || it.folderId in folderIds }
    }

    fun csvRow(vararg cells: String): String = cells.joinToString(",") { csvCell(it) }

    fun csvCell(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else safe
    }
}

class BackupManager(
    private val db: OrbitDatabase,
    private val reminders: ReminderScheduler,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun export(): String {
        val file = db.withTransaction {
            BackupFile(
                exportedAt = now(),
                projects = db.projectDao().getAll(),
                tasks = db.taskDao().getAll(),
                subtasks = db.subtaskDao().getAll(),
                noteFolders = db.noteDao().getFolders(),
                notes = db.noteDao().getAll(),
                habits = db.habitDao().getAll(),
                habitLogs = db.habitDao().getAllLogs(),
                goals = db.goalDao().getAll(),
            )
        }
        return BackupCodec.encode(file)
    }

    fun parse(text: String): BackupParseResult = BackupCodec.parse(text)

    suspend fun restore(file: BackupFile) {
        db.withTransaction {
            db.habitDao().clearLogs()
            db.habitDao().clear()
            db.subtaskDao().clear()
            db.reminderDao().clear()
            db.taskDao().clear()
            db.projectDao().clear()
            db.noteDao().clear()
            db.noteDao().clearFolders()
            db.goalDao().clear()

            db.projectDao().insertAll(file.projects)
            db.taskDao().insertAll(file.tasks)
            db.subtaskDao().upsertAll(file.subtasks)
            db.noteDao().insertFolders(file.noteFolders)
            db.noteDao().insertAll(file.notes)
            db.habitDao().insertAll(file.habits)
            db.habitDao().insertLogs(file.habitLogs)
            db.goalDao().insertAll(file.goals)
        }
        reminders.reconcileAll()
    }

    suspend fun exportTasksCsv(): String {
        val projects = db.projectDao().getAll().associate { it.id to it.name }
        val rows = db.taskDao().getAll().sortedBy { it.dueDate ?: Int.MAX_VALUE }
        return buildString {
            appendLine("title,description,status,priority,due_date,due_time,project,recurrence,created_at,completed_at")
            rows.forEach { t ->
                appendLine(
                    BackupCodec.csvRow(
                        t.title, t.description, t.status.name, t.priority.name,
                        t.dueDate?.toLocalDate()?.toString().orEmpty(),
                        t.dueTime?.let { "%02d:%02d".format(it / 60, it % 60) }.orEmpty(),
                        t.projectId?.let { projects[it] }.orEmpty(),
                        t.recurrenceType.name,
                        java.time.Instant.ofEpochMilli(t.createdAt).toString(),
                        t.completedAt?.let { java.time.Instant.ofEpochMilli(it).toString() }.orEmpty(),
                    ),
                )
            }
        }
    }

    suspend fun exportHabitsCsv(): String {
        val habits = db.habitDao().getAll().associateBy { it.id }
        return buildString {
            appendLine("habit,date")
            db.habitDao().getAllLogs()
                .sortedWith(compareBy({ habits[it.habitId]?.name }, { it.date }))
                .forEach { l -> appendLine(BackupCodec.csvRow(habits[l.habitId]?.name.orEmpty(), l.date.toLocalDate().toString())) }
        }
    }
}
