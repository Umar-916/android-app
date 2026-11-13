package com.funsol.orbit.core.repository

import androidx.room.withTransaction
import com.funsol.orbit.core.common.GoalMath
import com.funsol.orbit.core.common.newId
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeAll(): Flow<List<ProjectEntity>>
    fun observe(id: String): Flow<ProjectEntity?>
    suspend fun get(id: String): ProjectEntity?
    suspend fun save(project: ProjectEntity): ProjectEntity
    suspend fun setStatus(id: String, status: ProjectStatus)

    suspend fun delete(id: String, deleteTasks: Boolean)
}

class ProjectRepositoryImpl(
    private val db: OrbitDatabase,
    private val reminders: ReminderScheduler,
    private val now: () -> Long = System::currentTimeMillis,
) : ProjectRepository {
    override fun observeAll() = db.projectDao().observeAll()
    override fun observe(id: String) = db.projectDao().observe(id)
    override suspend fun get(id: String) = db.projectDao().get(id)

    override suspend fun save(project: ProjectEntity): ProjectEntity {
        val existing = db.projectDao().get(project.id)
        val p = project.copy(
            name = project.name.trim(),
            createdAt = existing?.createdAt ?: project.createdAt,
            updatedAt = now(),
        )
        db.projectDao().upsert(p)
        return p
    }

    override suspend fun setStatus(id: String, status: ProjectStatus) {
        val p = db.projectDao().get(id) ?: return
        db.projectDao().upsert(p.copy(status = status, updatedAt = now()))
    }

    override suspend fun delete(id: String, deleteTasks: Boolean) {
        val taskIds = if (deleteTasks) db.taskDao().idsByProject(id) else emptyList()
        db.withTransaction {
            if (deleteTasks) db.taskDao().deleteByProject(id)
            db.projectDao().delete(id)
        }
        taskIds.forEach { reminders.cancel(it) }
    }
}

interface NoteRepository {
    fun observeAll(): Flow<List<NoteEntity>>
    fun observeFolders(): Flow<List<NoteFolderEntity>>
    suspend fun get(id: String): NoteEntity?
    suspend fun save(note: NoteEntity): NoteEntity
    suspend fun delete(id: String)
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun createFolder(name: String): NoteFolderEntity?
    suspend fun renameFolder(id: String, name: String)

    suspend fun deleteFolder(id: String)
}

class NoteRepositoryImpl(
    private val db: OrbitDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : NoteRepository {
    override fun observeAll() = db.noteDao().observeAll()
    override fun observeFolders() = db.noteDao().observeFolders()
    override suspend fun get(id: String) = db.noteDao().get(id)

    override suspend fun save(note: NoteEntity): NoteEntity {
        val existing = db.noteDao().get(note.id)
        val n = note.copy(createdAt = existing?.createdAt ?: note.createdAt, updatedAt = now())
        db.noteDao().upsert(n)
        return n
    }

    override suspend fun delete(id: String) = db.noteDao().delete(id)

    override suspend fun setPinned(id: String, pinned: Boolean) {
        db.noteDao().get(id)?.let { db.noteDao().upsert(it.copy(pinned = pinned)) }
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        db.noteDao().get(id)?.let { db.noteDao().upsert(it.copy(archived = archived, pinned = if (archived) false else it.pinned)) }
    }

    override suspend fun createFolder(name: String): NoteFolderEntity? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val existing = db.noteDao().getFolders().firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing
        val folder = NoteFolderEntity(newId(), trimmed, now())
        db.noteDao().insertFolder(folder)
        return folder
    }

    override suspend fun renameFolder(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val folder = db.noteDao().getFolders().firstOrNull { it.id == id } ?: return
        if (db.noteDao().getFolders().any { it.id != id && it.name.equals(trimmed, true) }) return
        db.noteDao().updateFolder(folder.copy(name = trimmed))
    }

    override suspend fun deleteFolder(id: String) = db.noteDao().deleteFolder(id)
}

interface HabitRepository {
    fun observeAll(): Flow<List<HabitEntity>>
    fun observe(id: String): Flow<HabitEntity?>
    fun observeAllLogs(): Flow<List<HabitLogEntity>>
    fun observeLogs(habitId: String): Flow<List<HabitLogEntity>>
    suspend fun get(id: String): HabitEntity?
    suspend fun save(habit: HabitEntity): HabitEntity
    suspend fun delete(id: String)

    suspend fun setChecked(habitId: String, epochDay: Int, checked: Boolean)
}

class HabitRepositoryImpl(private val db: OrbitDatabase) : HabitRepository {
    override fun observeAll() = db.habitDao().observeAll()
    override fun observe(id: String) = db.habitDao().observe(id)
    override fun observeAllLogs() = db.habitDao().observeAllLogs()
    override fun observeLogs(habitId: String) = db.habitDao().observeLogs(habitId)
    override suspend fun get(id: String) = db.habitDao().get(id)

    override suspend fun save(habit: HabitEntity): HabitEntity {
        val h = habit.copy(name = habit.name.trim(), scheduleMask = habit.scheduleMask.takeIf { it and 127 != 0 } ?: 127)
        db.habitDao().upsert(h)
        return h
    }

    override suspend fun delete(id: String) = db.habitDao().delete(id)

    override suspend fun setChecked(habitId: String, epochDay: Int, checked: Boolean) {
        if (checked) db.habitDao().insertLog(HabitLogEntity(newId(), habitId, epochDay))
        else db.habitDao().deleteLog(habitId, epochDay)
    }
}

interface GoalRepository {
    fun observeAll(): Flow<List<GoalEntity>>
    suspend fun get(id: String): GoalEntity?
    suspend fun save(goal: GoalEntity): GoalEntity
    suspend fun delete(id: String)

    suspend fun updateProgress(id: String, newValue: Double)
}

class GoalRepositoryImpl(
    private val db: OrbitDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : GoalRepository {
    override fun observeAll() = db.goalDao().observeAll()
    override suspend fun get(id: String) = db.goalDao().get(id)

    override suspend fun save(goal: GoalEntity): GoalEntity {
        val existing = db.goalDao().get(goal.id)
        val target = goal.targetValue.takeIf { it.isFinite() && it > 0 } ?: 1.0
        val current = goal.currentValue.takeIf { it.isFinite() && it >= 0 } ?: 0.0
        val g = goal.copy(
            title = goal.title.trim(),
            targetValue = target,
            currentValue = current,
            createdAt = existing?.createdAt ?: goal.createdAt,
            updatedAt = now(),
            status = if (existing?.status == GoalStatus.COMPLETED && goal.status == GoalStatus.ACTIVE) GoalStatus.ACTIVE
            else resolveStatus(goal.status, current, target),
        )
        db.goalDao().upsert(g)
        return g
    }

    override suspend fun delete(id: String) = db.goalDao().delete(id)

    override suspend fun updateProgress(id: String, newValue: Double) {
        val goal = db.goalDao().get(id) ?: return
        val value = newValue.takeIf { it.isFinite() && it >= 0 } ?: return
        db.goalDao().upsert(goal.copy(currentValue = value, updatedAt = now(), status = resolveStatus(goal.status, value, goal.targetValue)))
    }

    private fun resolveStatus(status: GoalStatus, current: Double, target: Double): GoalStatus = when {
        status == GoalStatus.ARCHIVED -> status
        GoalMath.isReached(current, target) -> GoalStatus.COMPLETED
        else -> GoalStatus.ACTIVE
    }
}
