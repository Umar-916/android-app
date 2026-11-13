package com.funsol.orbit.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.notifications.NotificationHelper
import java.time.LocalDate

/**
 * Debug-only hook to preview the reminder notification without waiting for a scheduled task:
 *
 * adb shell am broadcast -n com.funsol.orbit.planner/com.funsol.orbit.debug.NotificationPreviewReceiver
 */
class NotificationPreviewReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val now = System.currentTimeMillis()
        NotificationHelper.showTaskReminder(
            context,
            TaskEntity(
                id = "debug-notification-preview",
                title = "Design review with the team",
                description = "",
                createdAt = now,
                updatedAt = now,
                dueDate = LocalDate.now().toEpochDay().toInt(),
                dueTime = 17 * 60 + 30,
                reminderOffsetMinutes = 0,
            ),
        )
    }
}
