package com.funsol.orbit.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.funsol.orbit.MainActivity
import com.funsol.orbit.R
import com.funsol.orbit.core.common.toLocalTime
import com.funsol.orbit.core.database.entity.TaskEntity
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object NotificationHelper {
    /**
     * Version-suffixed id: the bell sound and vibration live on the channel, and Android
     * does not let an existing channel change either, so a new channel is required.
     */
    const val CHANNEL_REMINDERS = "task_reminders_v2"
    private const val LEGACY_CHANNEL_REMINDERS = "task_reminders"

    const val EXTRA_TASK_ID = "task_id"
    const val ACTION_DONE = "com.funsol.orbit.action.TASK_DONE"
    const val ACTION_SNOOZE = "com.funsol.orbit.action.TASK_SNOOZE"

    private val VIBRATION_PATTERN = longArrayOf(0, 600, 250, 600, 250, 600, 250, 600, 250, 600)
    private const val BELL_LIGHT_COLOR = 0xFF8F78FF.toInt()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_reminders_desc)
            setSound(notificationSound(context), audioAttributes)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            enableLights(true)
            lightColor = BELL_LIGHT_COLOR
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)

        if (manager.getNotificationChannel(LEGACY_CHANNEL_REMINDERS) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_REMINDERS)
        }
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

        val collapsed = RemoteViews(context.packageName, R.layout.notification_reminder).apply {
            setTextViewText(R.id.notif_title, task.title)
            setTextViewText(R.id.notif_subtitle, text)
            setOnClickPendingIntent(R.id.notif_root, open)
        }
        val expanded = RemoteViews(context.packageName, R.layout.notification_reminder_expanded).apply {
            setTextViewText(R.id.notif_title_expanded, task.title)
            setTextViewText(R.id.notif_subtitle_expanded, text)
            setOnClickPendingIntent(R.id.notif_root_expanded, open)
            setOnClickPendingIntent(R.id.notif_action_done, done)
            setOnClickPendingIntent(R.id.notif_action_snooze, snooze)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_orbit)
            .setContentTitle(task.title)
            .setContentText(text)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(ContextCompat.getColor(context, R.color.orbit_violet))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setContentIntent(open)
            .setSound(notificationSound(context))
            .setVibrate(VIBRATION_PATTERN)
            // Fallback actions for surfaces that ignore the custom layout (e.g. some
            // Android Auto / Wear / accessibility presentations).
            .addAction(R.drawable.ic_notif_check, context.getString(R.string.notif_action_done), done)
            .addAction(R.drawable.ic_notif_snooze, context.getString(R.string.notif_action_snooze), snooze)
            .build()
        @Suppress("MissingPermission") // checked in canPost()
        NotificationManagerCompat.from(context).notify(id, notification)
        return true
    }

    fun cancel(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId))
    }

    fun notificationId(taskId: String): Int = taskId.hashCode() and 0x7FFFFFF0

    private fun notificationSound(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/reminder_bell")

    private fun action(context: Context, requestCode: Int, taskId: String, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, ReminderActionReceiver::class.java).setAction(action).putExtra(EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
