package com.funsol.orbit.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.funsol.orbit.R
import com.funsol.orbit.core.common.toLocalTime
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.MainActivity
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object NotificationHelper {
    const val CHANNEL_REMINDERS = "task_reminders"
    const val EXTRA_TASK_ID = "task_id"
    const val ACTION_DONE = "com.funsol.orbit.action.TASK_DONE"
    const val ACTION_SNOOZE = "com.funsol.orbit.action.TASK_SNOOZE"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notif_channel_reminders_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showTaskReminder(context: Context, task: TaskEntity): Boolean {
        if (!canPost(context)) return false
        ensureChannels(context)
        val id = notificationId(task.id)

        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = action(context, id, task.id, ACTION_DONE)
        val snooze = action(context, id + 1, task.id, ACTION_SNOOZE)

        val whenText = task.dueTime?.let { it.toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) }
        val text = whenText?.let { context.getString(R.string.notif_due_at, it) } ?: context.getString(R.string.notif_due_today)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_orbit)
            .setContentTitle(task.title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.notif_action_done), done)
            .addAction(0, context.getString(R.string.notif_action_snooze), snooze)
            .build()
        @Suppress("MissingPermission") // checked in canPost()
        NotificationManagerCompat.from(context).notify(id, notification)
        return true
    }

    fun cancel(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId))
    }

    fun notificationId(taskId: String): Int = taskId.hashCode() and 0x7FFFFFF0

    private fun action(context: Context, requestCode: Int, taskId: String, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderActionReceiver::class.java).setAction(action).putExtra(EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
