package com.funsol.orbit.core.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.funsol.orbit.core.common.reminderTriggerMillis
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.ReminderEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import java.util.concurrent.TimeUnit

interface ReminderScheduler {
    suspend fun sync(task: TaskEntity)

    suspend fun cancel(taskId: String)

    suspend fun reconcileAll()
}

class WorkManagerReminderScheduler(
    private val context: Context,
    private val db: OrbitDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : ReminderScheduler {
    private val work get() = WorkManager.getInstance(context)

    override suspend fun sync(task: TaskEntity) {
        val offset = task.reminderOffsetMinutes
        val due = task.dueDate
        if (offset == null || due == null || task.status !in ACTIVE) {
            cancel(task.id)
            return
        }
        val trigger = reminderTriggerMillis(due, task.dueTime, offset)
        val existing = db.reminderDao().get(task.id)
        val fired = existing != null && existing.triggerAt == trigger && existing.fired
        db.reminderDao().upsert(ReminderEntity(task.id, trigger, fired))
        val now = clock()
        when {
            fired -> work.cancelUniqueWork(workName(task.id))
            trigger <= now -> if (now - trigger <= LATE_DELIVERY_WINDOW_MS) enqueue(task.id, 0) else work.cancelUniqueWork(workName(task.id))
            else -> enqueue(task.id, trigger - now)
        }
    }

    override suspend fun cancel(taskId: String) {
        work.cancelUniqueWork(workName(taskId))
        work.cancelUniqueWork(snoozeName(taskId))
        db.reminderDao().delete(taskId)
        NotificationHelper.cancel(context, taskId)
    }

    override suspend fun reconcileAll() {
        val candidates = db.taskDao().getReminderCandidates()
        val candidateIds = candidates.map { it.id }.toSet()
        db.reminderDao().allTaskIds().filter { it !in candidateIds }.forEach { cancel(it) }
        candidates.forEach { sync(it) }
    }

    fun snooze(taskId: String, minutes: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(minutes, TimeUnit.MINUTES)
            .setInputData(Data.Builder().putString(ReminderWorker.KEY_TASK_ID, taskId).putBoolean(ReminderWorker.KEY_SNOOZED, true).build())
            .build()
        work.enqueueUniqueWork(snoozeName(taskId), ExistingWorkPolicy.REPLACE, request)
    }

    private fun enqueue(taskId: String, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(ReminderWorker.KEY_TASK_ID, taskId).build())
            .build()
        work.enqueueUniqueWork(workName(taskId), ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        private const val LATE_DELIVERY_WINDOW_MS = 24L * 60 * 60 * 1000
        private val ACTIVE =setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS)
        fun workName(taskId: String) = "reminder_$taskId"
        fun snoozeName(taskId: String) = "snooze_$taskId"
    }
}
