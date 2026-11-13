package com.funsol.orbit.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.database.dao.GoalDao
import com.funsol.orbit.core.database.dao.HabitDao
import com.funsol.orbit.core.database.dao.NoteDao
import com.funsol.orbit.core.database.dao.ProjectDao
import com.funsol.orbit.core.database.dao.ReminderDao
import com.funsol.orbit.core.database.dao.SubtaskDao
import com.funsol.orbit.core.database.dao.TaskDao
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.HabitLogEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.ReminderEntity
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus

class Converters {
    @TypeConverter fun priorityToString(v: Priority): String = v.name
    @TypeConverter fun stringToPriority(v: String): Priority = enumOrDefault(v, Priority.MEDIUM)

    @TypeConverter fun taskStatusToString(v: TaskStatus): String = v.name
    @TypeConverter fun stringToTaskStatus(v: String): TaskStatus = enumOrDefault(v, TaskStatus.TODO)

    @TypeConverter fun projectStatusToString(v: ProjectStatus): String = v.name
    @TypeConverter fun stringToProjectStatus(v: String): ProjectStatus = enumOrDefault(v, ProjectStatus.ACTIVE)

    @TypeConverter fun goalStatusToString(v: GoalStatus): String = v.name
    @TypeConverter fun stringToGoalStatus(v: String): GoalStatus = enumOrDefault(v, GoalStatus.ACTIVE)

    @TypeConverter fun recurrenceToString(v: RecurrenceType): String = v.name
    @TypeConverter fun stringToRecurrence(v: String): RecurrenceType = enumOrDefault(v, RecurrenceType.NONE)

    private inline fun <reified E : Enum<E>> enumOrDefault(value: String, default: E): E =
        enumValues<E>().firstOrNull { it.name == value } ?: default
}

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        ProjectEntity::class,
        NoteEntity::class,
        NoteFolderEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        GoalEntity::class,
        ReminderEntity::class,
    ],
    version = OrbitDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class OrbitDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun noteDao(): NoteDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val VERSION = 1
        const val NAME = "orbit.db"

        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): OrbitDatabase =
            Room.databaseBuilder(context.applicationContext, OrbitDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
