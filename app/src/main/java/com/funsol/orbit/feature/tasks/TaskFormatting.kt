package com.funsol.orbit.feature.tasks

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.priorityLabel
import java.time.LocalDate

internal data class DueInfo(
    val text: String,
    val overdue: Boolean,
    val dueToday: Boolean = false,
    val daysAway: Int = 0,
)

@Composable
internal fun dueInfo(task: TaskEntity, today: LocalDate): DueInfo? {
    val due = task.dueDate ?: return null
    val todayStr = stringResource(R.string.tasks_due_today)
    val tomorrowStr = stringResource(R.string.tasks_due_tomorrow)
    val yesterdayStr = stringResource(R.string.tasks_due_yesterday)
    val away = due - today.toEpochDayInt()
    val dayText = when (away) {
        0 -> todayStr
        1 -> tomorrowStr
        -1 -> yesterdayStr
        else -> if (due.toLocalDate().year == today.year) DateFormats.dateShort(due) else DateFormats.date(due)
    }
    val full = task.dueTime?.let { stringResource(R.string.tasks_due_at, dayText, DateFormats.time(it)) } ?: dayText
    val overdue = TaskRules.isOverdue(task, today)
    return DueInfo(
        text = if (overdue) stringResource(R.string.tasks_overdue_label, full) else full,
        overdue = overdue,
        dueToday = away == 0 && TaskRules.isOpen(task),
        daysAway = away,
    )
}

@Composable
internal fun reminderLabel(minutes: Int): String = when (minutes) {
    0 -> stringResource(R.string.tasks_reminder_at_due)
    10 -> stringResource(R.string.tasks_reminder_10m)
    30 -> stringResource(R.string.tasks_reminder_30m)
    60 -> stringResource(R.string.tasks_reminder_1h)
    1440 -> stringResource(R.string.tasks_reminder_1d)
    else -> stringResource(R.string.tasks_reminder_custom, minutes)
}

@Composable
internal fun statusLabel(status: TaskStatus): String = stringResource(
    when (status) {
        TaskStatus.TODO -> R.string.status_todo
        TaskStatus.IN_PROGRESS -> R.string.status_in_progress
        TaskStatus.COMPLETED -> R.string.status_completed
        TaskStatus.ARCHIVED -> R.string.status_archived
    },
)

@Composable
internal fun taskA11yDescription(item: TaskItem, today: LocalDate): String {
    val task = item.task
    val parts = ArrayList<String>(8)
    parts += task.title
    if (task.status != TaskStatus.TODO) parts += statusLabel(task.status)
    dueInfo(task, today)?.let { parts += stringResource(R.string.tasks_a11y_due, it.text) }
    parts += stringResource(R.string.tasks_a11y_priority, priorityLabel(task.priority))
    item.project?.let { parts += it.name }
    if (item.subtasksTotal > 0) parts += stringResource(R.string.tasks_a11y_subtasks, item.subtasksDone, item.subtasksTotal)
    if (task.recurrenceType != RecurrenceType.NONE) parts += stringResource(R.string.tasks_a11y_recurring)
    task.reminderOffsetMinutes?.let { parts += stringResource(R.string.tasks_reminder_info, reminderLabel(it)) }
    return parts.joinToString(", ")
}

@Composable
internal fun CompletionCheck(
    checked: Boolean,
    title: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accent: FeatureAccent = FeatureAccent.TASKS,
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "completion",
    )
    val fill = Orbit.extras.accentGradient(accent)
    val ring = MaterialTheme.colorScheme.outline
    Box(
        modifier
            .size(Spacing.touchTarget)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .semantics { contentDescription = title },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .graphicsLayer {
                    val scale = 0.9f + 0.1f * progress
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(fill, alpha = progress.coerceIn(0f, 1f))
                .border(2.dp, ring.copy(alpha = (1f - progress).coerceIn(0f, 1f)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        val p = progress.coerceIn(0f, 1f)
                        alpha = p
                        scaleX = p
                        scaleY = p
                    },
            )
        }
    }
}

@Composable
internal fun taskCountText(count: Int): String = pluralStringResource(R.plurals.tasks_count, count, count)
