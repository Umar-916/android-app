package com.funsol.orbit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.notifications.ReminderScheduler

fun inMemoryDb(): OrbitDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), OrbitDatabase::class.java)
        .allowMainThreadQueries()
        .build()

class FakeReminderScheduler : ReminderScheduler {
    val synced = mutableListOf<String>()
    val cancelled = mutableListOf<String>()
    var reconciled = 0
    override suspend fun sync(task: TaskEntity) { synced += task.id }
    override suspend fun cancel(taskId: String) { cancelled += taskId }
    override suspend fun reconcileAll() { reconciled++ }
}

fun task(id: String, title: String = id, projectId: String? = null, dueDate: Int? = null) =
    TaskEntity(id = id, title = title, projectId = projectId, dueDate = dueDate, createdAt = 1, updatedAt = 1)

fun project(id: String) = ProjectEntity(id = id, name = id, createdAt = 1, updatedAt = 1)

fun habit(id: String) = HabitEntity(id = id, name = id, startDate = 20000, createdAt = 1)
