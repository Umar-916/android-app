package com.funsol.orbit.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.funsol.orbit.core.common.RecurrenceType
import kotlinx.serialization.Serializable

enum class Priority { LOW, MEDIUM, HIGH, URGENT }

enum class TaskStatus { TODO, IN_PROGRESS, COMPLETED, ARCHIVED }

enum class ProjectStatus { ACTIVE, ON_HOLD, COMPLETED, ARCHIVED }

enum class GoalStatus { ACTIVE, COMPLETED, ARCHIVED }

@Serializable
@Entity(
    tableName = "projects",
    indices = [Index("status"), Index("name")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val color: Int = 0xFF7167F5.toInt(),
    val startDate: Int? = null,
    val endDate: Int? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("projectId"),
        Index("status"),
        Index("dueDate"),
        Index("title"),
        Index(value = ["seriesId", "dueDate"], unique = true),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val dueDate: Int? = null,
    val dueTime: Int? = null,
    val reminderOffsetMinutes: Int? = null,
    val projectId: String? = null,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceWeekdays: Int = 0,
    val recurrenceEndDate: Int? = null,
    val recurrenceAnchorDay: Int = 1,
    val seriesId: String? = null,
)

@Serializable
@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0,
)

@Serializable
@Entity(tableName = "note_folders", indices = [Index("name", unique = true)])
data class NoteFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Serializable
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = NoteFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("folderId"), Index("pinned"), Index("archived"), Index("updatedAt"), Index("title")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val body: String = "",
    val folderId: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "habits", indices = [Index("archived"), Index("name")])
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val color: Int = 0xFF7167F5.toInt(),
    val scheduleMask: Int = 127,
    val startDate: Int,
    val archived: Boolean = false,
    val createdAt: Long,
)

@Serializable
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["habitId", "date"], unique = true)],
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: Int,
)

@Serializable
@Entity(tableName = "goals", indices = [Index("status"), Index("title")])
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val targetValue: Double = 100.0,
    val currentValue: Double = 0.0,
    val unit: String = "",
    val startDate: Int? = null,
    val targetDate: Int? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ReminderEntity(
    @PrimaryKey val taskId: String,
    val triggerAt: Long,
    val fired: Boolean = false,
)
