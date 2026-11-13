package com.funsol.orbit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ReminderEntity
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate, dueTime, createdAt")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observe(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY status = 'COMPLETED', dueDate IS NULL, dueDate, createdAt")
    fun observeByProject(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :fromDay AND :toDay AND status != 'ARCHIVED' ORDER BY dueDate, dueTime")
    fun observeDueBetween(fromDay: Int, toDay: Int): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE (title LIKE :pattern ESCAPE '\\' OR description LIKE :pattern ESCAPE '\\') " +
            "ORDER BY updatedAt DESC LIMIT :limit",
    )
    suspend fun search(pattern: String, limit: Int = 50): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE seriesId = :seriesId AND dueDate = :dueDate")
    suspend fun countOccurrence(seriesId: String, dueDate: Int): Int

    @Query("DELETE FROM tasks WHERE seriesId = :seriesId AND dueDate = :dueDate AND status = 'TODO' AND updatedAt = createdAt")
    suspend fun deleteUntouchedOpenOccurrence(seriesId: String, dueDate: Int): Int

    @Query(
        "SELECT * FROM tasks WHERE reminderOffsetMinutes IS NOT NULL AND dueDate IS NOT NULL " +
            "AND status IN ('TODO','IN_PROGRESS')",
    )
    suspend fun getReminderCandidates(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: String)

    @Query("SELECT id FROM tasks WHERE projectId = :projectId")
    suspend fun idsByProject(projectId: String): List<String>

    @Query("DELETE FROM tasks")
    suspend fun clear()
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position, rowid")
    fun observeFor(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks")
    fun observeAll(): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position, rowid")
    suspend fun getFor(taskId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks")
    suspend fun getAll(): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(subtasks: List<SubtaskEntity>)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteFor(taskId: String)

    @Query("DELETE FROM subtasks")
    suspend fun clear()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY status = 'ARCHIVED', updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observe(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun get(id: String): ProjectEntity?

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE (name LIKE :pattern ESCAPE '\\' OR description LIKE :pattern ESCAPE '\\') ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun search(pattern: String, limit: Int = 50): List<ProjectEntity>

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM projects")
    suspend fun clear()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteEntity?

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE (title LIKE :pattern ESCAPE '\\' OR body LIKE :pattern ESCAPE '\\') ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun search(pattern: String, limit: Int = 50): List<NoteEntity>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM notes")
    suspend fun clear()

    @Query("SELECT * FROM note_folders ORDER BY name COLLATE NOCASE")
    fun observeFolders(): Flow<List<NoteFolderEntity>>

    @Query("SELECT * FROM note_folders")
    suspend fun getFolders(): List<NoteFolderEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFolder(folder: NoteFolderEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFolders(folders: List<NoteFolderEntity>)

    @Update
    suspend fun updateFolder(folder: NoteFolderEntity)

    @Query("DELETE FROM note_folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("DELETE FROM note_folders")
    suspend fun clearFolders()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY archived, createdAt")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observe(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun get(id: String): HabitEntity?

    @Query("SELECT * FROM habits")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE (name LIKE :pattern ESCAPE '\\' OR description LIKE :pattern ESCAPE '\\') ORDER BY name LIMIT :limit")
    suspend fun search(pattern: String, limit: Int = 50): List<HabitEntity>

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM habits")
    suspend fun clear()

    @Query("SELECT * FROM habit_logs")
    fun observeAllLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date")
    fun observeLogs(habitId: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllLogs(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLogs(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: String, date: Int)

    @Query("DELETE FROM habit_logs")
    suspend fun clearLogs()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY status = 'ARCHIVED', status = 'COMPLETED', updatedAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: String): GoalEntity?

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE (title LIKE :pattern ESCAPE '\\' OR description LIKE :pattern ESCAPE '\\') ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun search(pattern: String, limit: Int = 50): List<GoalEntity>

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM goals")
    suspend fun clear()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun get(taskId: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("UPDATE reminders SET fired = 1 WHERE taskId = :taskId")
    suspend fun markFired(taskId: String)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun delete(taskId: String)

    @Query("SELECT taskId FROM reminders")
    suspend fun allTaskIds(): List<String>

    @Query("DELETE FROM reminders")
    suspend fun clear()
}
