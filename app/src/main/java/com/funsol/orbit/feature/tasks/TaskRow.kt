package com.funsol.orbit.feature.tasks

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.TasksEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.TaskRules
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.designsystem.ColorDot
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitProgressBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.TagPill
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.WarningAmber as AmberWarning
import com.funsol.orbit.core.designsystem.priorityColor
import com.funsol.orbit.core.designsystem.priorityLabel
import java.time.LocalDate

internal class TaskRowActions(
    val onOpen: (TaskEntity) -> Unit,
    val onToggle: (TaskEntity, Boolean) -> Unit,
    val onMarkInProgress: (TaskEntity) -> Unit,
    val onReopen: (TaskEntity) -> Unit,
    val onArchive: (TaskEntity) -> Unit,
    val onRestore: (TaskEntity) -> Unit,
    val onDelete: (TaskEntity) -> Unit,
)

@Composable
internal fun dueColor(due: DueInfo, muted: Boolean): Color = when {
    muted -> MaterialTheme.colorScheme.onSurfaceVariant
    due.overdue -> MaterialTheme.colorScheme.error
    due.dueToday -> AmberWarning
    due.daysAway in 1..2 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskRow(
    item: TaskItem,
    today: LocalDate,
    actions: TaskRowActions,
    modifier: Modifier = Modifier,
) {
    val task = item.task
    val completed = task.status == TaskStatus.COMPLETED
    val archived = task.status == TaskStatus.ARCHIVED
    val dimmed = completed || archived
    val description = taskA11yDescription(item, today)
    val due = dueInfo(task, today)
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    val edge = priorityColor(task.priority)

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )

    OrbitCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        accent = if (dimmed) edge.copy(alpha = 0.35f) else edge,
        contentPadding = PaddingValues(start = Spacing.sm, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CompletionCheck(
                checked = completed,
                title = task.title,
                onCheckedChange = { actions.onToggle(task, it) },
            )
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (dimmed) 0.62f else 1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(
                        interactionSource = source,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = { actions.onOpen(task) },
                    )
                    .semantics(mergeDescendants = true) { contentDescription = description }
                    .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (dimmed) subtle else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Orbit.extras.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (due != null) {
                        MetaPill(
                            icon = if (due.overdue) Icons.Rounded.WarningAmber else Icons.Rounded.Event,
                            text = due.text,
                            color = dueColor(due, muted = dimmed),
                        )
                    }
                    TagPill(priorityLabel(task.priority), edge)
                    item.project?.let { ProjectPill(it) }
                    if (task.recurrenceType != RecurrenceType.NONE) {
                        Icon(Icons.Rounded.Repeat, null, Modifier.size(16.dp), tint = subtle)
                    }
                    if (task.reminderOffsetMinutes != null && TaskRules.isOpen(task)) {
                        Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(16.dp), tint = subtle)
                    }
                }
                if (item.subtasksTotal > 0) {
                    val fraction = item.subtasksDone.toFloat() / item.subtasksTotal
                    val progressText = stringResource(R.string.tasks_subtasks_progress, item.subtasksDone, item.subtasksTotal)
                    if (isLargeFontScale()) {
                        Text(progressText, style = MaterialTheme.typography.labelMedium, color = subtle)
                        OrbitProgressBar(fraction = fraction, height = 5.dp, color = edge)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Box(Modifier.weight(1f)) {
                                OrbitProgressBar(fraction = fraction, height = 5.dp, color = edge)
                            }
                            Text(progressText, style = MaterialTheme.typography.labelMedium, color = subtle, maxLines = 1)
                        }
                    }
                }
            }
            RowMenu(task, actions)
        }
    }
}

@Composable
internal fun MetaPill(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
    }
}

@Composable
private fun ProjectPill(project: ProjectTag) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Orbit.extras.well)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ColorDot(Color(project.color), size = 9.dp)
        Text(
            project.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 130.dp),
        )
    }
}

@Composable
private fun RowMenu(task: TaskEntity, actions: TaskRowActions) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box {
        IconButton(onClick = { context.postAnalytic(TasksEvents.TASKS_ROW_MENU_CLICK); expanded = true }) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.tasks_row_menu, task.title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { expanded = false; actions.onOpen(task) },
            )
            when (task.status) {
                TaskStatus.TODO -> DropdownMenuItem(
                    text = { Text(stringResource(R.string.tasks_menu_in_progress)) },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                    onClick = { expanded = false; actions.onMarkInProgress(task) },
                )
                TaskStatus.COMPLETED -> DropdownMenuItem(
                    text = { Text(stringResource(R.string.tasks_menu_reopen)) },
                    leadingIcon = { Icon(Icons.Rounded.Replay, null) },
                    onClick = { expanded = false; actions.onReopen(task) },
                )
                TaskStatus.IN_PROGRESS, TaskStatus.ARCHIVED -> Unit
            }
            if (task.status == TaskStatus.ARCHIVED) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_unarchive)) },
                    leadingIcon = { Icon(Icons.Rounded.Unarchive, null) },
                    onClick = { expanded = false; actions.onRestore(task) },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_archive)) },
                    leadingIcon = { Icon(Icons.Rounded.Archive, null) },
                    onClick = { expanded = false; actions.onArchive(task) },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; actions.onDelete(task) },
            )
        }
    }
}
