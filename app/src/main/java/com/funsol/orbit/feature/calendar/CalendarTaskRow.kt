package com.funsol.orbit.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.TagPill
import com.funsol.orbit.core.designsystem.priorityColor
import com.funsol.orbit.core.designsystem.priorityLabel
import com.funsol.orbit.feature.tasks.CompletionCheck
import com.funsol.orbit.feature.tasks.MetaPill
import com.funsol.orbit.feature.tasks.dueInfo
import com.funsol.orbit.feature.tasks.reminderLabel
import com.funsol.orbit.feature.tasks.statusLabel
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CalendarTaskRow(
    task: TaskEntity,
    today: LocalDate,
    showDate: Boolean,
    onOpen: (TaskEntity) -> Unit,
    onToggle: (TaskEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val completed = task.status == TaskStatus.COMPLETED
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    val overdue = TaskRules.isOverdue(task, today)
    val due = if (showDate) dueInfo(task, today) else null
    val timeText = if (!showDate) task.dueTime?.let { DateFormats.time(it) } else null
    val reminderText = task.reminderOffsetMinutes?.takeIf { TaskRules.isOpen(task) }
        ?.let { stringResource(R.string.tasks_reminder_info, reminderLabel(it)) }
    val edge = priorityColor(task.priority)
    val coral = Orbit.extras.accent(FeatureAccent.CALENDAR)

    val parts = ArrayList<String>(6)
    parts += task.title
    if (task.status != TaskStatus.TODO) parts += statusLabel(task.status)
    (due?.text ?: timeText)?.let { parts += it }
    parts += stringResource(R.string.tasks_a11y_priority, priorityLabel(task.priority))
    reminderText?.let { parts += it }
    val description = parts.joinToString(", ")

    OrbitCard(
        modifier = modifier.fillMaxWidth(),
        accent = if (completed) edge.copy(alpha = 0.35f) else edge,
        contentPadding = PaddingValues(start = Spacing.sm, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CompletionCheck(
                checked = completed,
                title = task.title,
                onCheckedChange = { onToggle(task, it) },
                accent = FeatureAccent.CALENDAR,
            )
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (completed) 0.62f else 1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button) { onOpen(task) }
                    .semantics(mergeDescendants = true) { contentDescription = description }
                    .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (completed) subtle else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (timeText != null) {
                        MetaPill(Icons.Rounded.AccessTime, timeText, coral)
                    }
                    if (due != null) {
                        val color = if (due.overdue || overdue) MaterialTheme.colorScheme.error else subtle
                        MetaPill(if (overdue) Icons.Rounded.WarningAmber else Icons.Rounded.AccessTime, due.text, color)
                    }
                    TagPill(priorityLabel(task.priority), edge)
                    if (reminderText != null) {
                        MetaPill(
                            icon = Icons.Rounded.NotificationsActive,
                            text = reminderLabel(task.reminderOffsetMinutes ?: 0),
                            color = Orbit.extras.accentText(FeatureAccent.NOTES),
                        )
                    }
                    if (task.recurrenceType != RecurrenceType.NONE) {
                        Icon(Icons.Rounded.Repeat, null, Modifier.size(16.dp), tint = subtle)
                    }
                }
            }
        }
    }
}
