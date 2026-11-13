package com.funsol.orbit.core.repository

import androidx.room.withTransaction
import com.funsol.orbit.core.common.Recurrence
import com.funsol.orbit.core.common.RecurrenceEngine
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.newId
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun observeAll(): Flow<List<TaskEntity>>
    fun observe(id: String): Flow<TaskEntity?>
    fun observeByProject(projectId: String): Flow<List<TaskEntity>>
    fun observeSubtasks(taskId: String): Flow<List<SubtaskEntity>>
    fun observeAllSubtasks(): Flow<List<SubtaskEntity>>
    suspend fun get(id: String): TaskEntity?
    suspend fun getSubtasks(taskId: String): List<SubtaskEntity>

    suspend fun save(task: TaskEntity, subtasks: List<SubtaskEntity>? = null): TaskEntity

    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun setStatus(id: String, status: TaskStatus)
    suspend fun delete(id: String)
    suspend fun setSubtaskDone(subtaskId: String, taskId: String, done: Boolean)
}

class TaskRepositoryImpl(
    private val db: OrbitDatabase,
    private val reminders: ReminderScheduler,
    private val now: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = { LocalDate.now() },
) : TaskRepository {
    private val tasks get() = db.taskDao()
    private val subtasks get() = db.subtaskDao()

    override fun observeAll() = tasks.observeAll()
    override fun observe(id: String) = tasks.observe(id)
    override fun observeByProject(projectId: String) = tasks.observeByProject(projectId)
    override fun observeSubtasks(taskId: String) = subtasks.observeFor(taskId)
    override fun observeAllSubtasks() = subtasks.observeAll()
    override suspend fun get(id: String) = tasks.get(id)
    override suspend fun getSubtasks(taskId: String) = subtasks.getFor(taskId)

    override suspend fun save(task: TaskEntity, subtasks: List<SubtaskEntity>?): TaskEntity {
        val saved = db.withTransaction {
            val existing = tasks.get(task.id)
            val t0 = task.copy(
                title = task.title.trim(),
                updatedAt = now(),
                createdAt = existing?.createdAt ?: task.createdAt,
            )
            val recurring = t0.recurrenceType != RecurrenceType.NONE && t0.dueDate != null
            val t1 = when {
                recurring -> t0.copy(
                    seriesId = t0.seriesId ?: t0.id,
                    recurrenceInterval = t0.recurrenceInterval.coerceAtLeast(1),
                    recurrenceAnchorDay = t0.dueDate.toLocalDate().dayOfMonth,
                )
                else -> t0.copy(
                    recurrenceType = RecurrenceType.NONE,
                    recurrenceWeekdays = 0,
                    recurrenceEndDate = null,
                    seriesId = if (t0.recurrenceType == RecurrenceType.NONE) t0.seriesId else null,
                )
            }
            val t2 = if (t1.status == TaskStatus.COMPLETED && t1.completedAt == null) t1.copy(completedAt = now()) else t1
            if (existing == null) tasks.insert(t2) else tasks.update(t2)
            if (subtasks != null) {
                this.subtasks.deleteFor(t2.id)
                this.subtasks.upsertAll(subtasks.mapIndexed { i, s -> s.copy(taskId = t2.id, position = i) })
            }
            t2
        }
        reminders.sync(saved)
        return saved
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        var updated: TaskEntity? = null
        db.withTransaction {
            val task = tasks.get(id) ?: return@withTransaction
            if (completed) {
                if (task.status == TaskStatus.COMPLETED) return@withTransaction
                val done = task.copy(status = TaskStatus.COMPLETED, completedAt = task.completedAt ?: now(), updatedAt = now())
                tasks.update(done)
                createSuccessor(task)
                updated = done
            } else {
                if (task.status == TaskStatus.COMPLETED) removeSuccessor(task)
                val reopened = task.copy(status = TaskStatus.TODO, completedAt = null, updatedAt = now())
                tasks.update(reopened)
                updated = reopened
            }
        }
        updated?.let { reminders.sync(it) }
        updated?.seriesId?.let { reminders.reconcileAll() }
    }

    private suspend fun removeSuccessor(completed: TaskEntity) {
        val due = completed.dueDate ?: return
        val series = completed.seriesId ?: return
        if (completed.recurrenceType == RecurrenceType.NONE) return
        val rule = Recurrence(
            type = completed.recurrenceType,
            interval = completed.recurrenceInterval,
            weekdayMask = completed.recurrenceWeekdays,
            endEpochDay = completed.recurrenceEndDate,
            anchorDay = completed.recurrenceAnchorDay,
        )
        val next = RecurrenceEngine.nextForCompletion(due.toLocalDate(), rule, today()) ?: return
        tasks.deleteUntouchedOpenOccurrence(series, next.toEpochDayInt())
    }

    private suspend fun createSuccessor(completed: TaskEntity) {
        val due = completed.dueDate ?: return
        val series = completed.seriesId ?: return
        if (completed.recurrenceType == RecurrenceType.NONE) return
        val rule = Recurrence(
            type = completed.recurrenceType,
            interval = completed.recurrenceInterval,
            weekdayMask = completed.recurrenceWeekdays,
            endEpochDay = completed.recurrenceEndDate,
            anchorDay = completed.recurrenceAnchorDay,
        )
        val next = RecurrenceEngine.nextForCompletion(due.toLocalDate(), rule, today()) ?: return
        val nextDay = next.toEpochDayInt()
        if (tasks.countOccurrence(series, nextDay) > 0) return
        val successor = completed.copy(
            id = newId(),
            status = TaskStatus.TODO,
            completedAt = null,
            dueDate = nextDay,
            createdAt = now(),
            updatedAt = now(),
        )
        val rowId = tasks.insertIgnore(successor)
        if (rowId != -1L) {
            val copies = subtasks.getFor(completed.id).map { it.copy(id = newId(), taskId = successor.id, done = false) }
            subtasks.upsertAll(copies)
        }
    }

    override suspend fun setStatus(id: String, status: TaskStatus) {
        if (status == TaskStatus.COMPLETED) return setCompleted(id, true)
        val updated = db.withTransaction {
            val task = tasks.get(id) ?: return@withTransaction null
            task.copy(status = status, completedAt = if (status == TaskStatus.ARCHIVED) task.completedAt else null, updatedAt = now()).also { tasks.update(it) }
        } ?: return
        reminders.sync(updated)
    }

    override suspend fun delete(id: String) {
        reminders.cancel(id)
        tasks.delete(id)
    }

    override suspend fun setSubtaskDone(subtaskId: String, taskId: String, done: Boolean) {
        subtasks.getFor(taskId).firstOrNull { it.id == subtaskId }?.let { subtasks.upsert(it.copy(done = done)) }
    }
}
