package com.funsol.orbit.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.funsol.orbit.core.common.reminderTriggerMillis
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params), KoinComponent {
    private val db: OrbitDatabase by inject()
    private val settings: SettingsStore by inject()

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.success()
        val snoozed = inputData.getBoolean(KEY_SNOOZED, false)
        if (!settings.settings.first().notificationsEnabled) return Result.success()

        val task = db.taskDao().get(taskId) ?: return Result.success()
        if (task.status != TaskStatus.TODO && task.status != TaskStatus.IN_PROGRESS) return Result.success()
        val due = task.dueDate
        val offset = task.reminderOffsetMinutes
        if (!snoozed) {
            if (due == null || offset == null) return Result.success()
            val reminder = db.reminderDao().get(taskId) ?: return Result.success()
            if (reminder.fired) return Result.success()
            if (reminder.triggerAt != reminderTriggerMillis(due, task.dueTime, offset)) return Result.success()
        }
        if (NotificationHelper.showTaskReminder(applicationContext, task)) db.reminderDao().markFired(taskId)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_SNOOZED = "snoozed"
    }
}

class ReminderActionReceiver : BroadcastReceiver(), KoinComponent {
    private val tasks: TaskRepository by inject()
    private val scheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_DONE -> tasks.setCompleted(taskId, true)
                    NotificationHelper.ACTION_SNOOZE -> {
                        (scheduler as? WorkManagerReminderScheduler)?.snooze(taskId, 10)
                        NotificationHelper.cancel(context, taskId)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ReminderReceiver", "Receiver work failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

class ReminderBootReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: ReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                scheduler.reconcileAll()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ReminderReceiver", "Receiver work failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
