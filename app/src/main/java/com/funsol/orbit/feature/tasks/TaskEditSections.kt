package com.funsol.orbit.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.designsystem.ColorDot
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing

@Composable
internal fun FormSection(
    title: String,
    icon: ImageVector,
    accent: FeatureAccent,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OrbitCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon, accent, size = 36.dp)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        Column(Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm), content = content)
    }
}

@Composable
internal fun orbitFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Orbit.extras.well,
    unfocusedContainerColor = Orbit.extras.well,
    errorContainerColor = Orbit.extras.well,
    unfocusedBorderColor = Color.Transparent,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
)

@Composable
internal fun WellRow(
    leading: @Composable () -> Unit,
    label: String,
    value: String,
    isSet: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dropdown: Boolean = false,
    clearDescription: String? = null,
    onClear: (() -> Unit)? = null,
    menu: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)) {
        OrbitWell(Modifier.fillMaxWidth(), onClick = if (enabled) onClick else null) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                leading()
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = Orbit.extras.textMuted)
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSet) MaterialTheme.colorScheme.onSurface else Orbit.extras.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (isSet && onClear != null && clearDescription != null) {
                    IconButton(onClick = onClear, enabled = enabled) {
                        Icon(Icons.Rounded.Close, contentDescription = clearDescription)
                    }
                } else if (dropdown) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = Orbit.extras.textMuted,
                        modifier = Modifier.padding(end = 8.dp).size(24.dp),
                    )
                } else {
                    Box(Modifier.size(8.dp))
                }
            }
        }
        menu?.invoke()
    }
}

@Composable
internal fun HintText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Orbit.extras.textMuted)
}

@Composable
internal fun ScheduleSection(
    form: TaskForm,
    onDate: (Int?) -> Unit,
    onTime: (Int?) -> Unit,
) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val notSet = stringResource(R.string.tasks_not_set)

    FormSection(stringResource(R.string.tasks_section_schedule), Icons.Rounded.Event, FeatureAccent.CALENDAR) {
        WellRow(
            leading = { IconTile(Icons.Rounded.Event, FeatureAccent.CALENDAR, size = 36.dp) },
            label = stringResource(R.string.tasks_field_due_date),
            value = form.dueDate?.let { DateFormats.date(it) } ?: notSet,
            isSet = form.dueDate != null,
            onClick = { showDate = true },
            clearDescription = stringResource(R.string.tasks_clear_date),
            onClear = { onDate(null) },
        )
        WellRow(
            leading = { IconTile(Icons.Rounded.AccessTime, FeatureAccent.ANALYTICS, size = 36.dp) },
            label = stringResource(R.string.tasks_field_due_time),
            value = form.dueTime?.let { DateFormats.time(it) } ?: notSet,
            isSet = form.dueTime != null,
            onClick = { showTime = true },
            clearDescription = stringResource(R.string.tasks_clear_time),
            onClear = { onTime(null) },
        )
    }

    if (showDate) {
        TaskDatePickerDialog(
            initialEpochDay = form.dueDate,
            onDismiss = { showDate = false },
            onPick = { showDate = false; onDate(it) },
        )
    }
    if (showTime) {
        TaskTimePickerDialog(
            initialMinuteOfDay = form.dueTime,
            onDismiss = { showTime = false },
            onPick = { showTime = false; onTime(it) },
        )
    }
}

@Composable
internal fun ReminderSection(
    form: TaskForm,
    notificationsOff: Boolean,
    onReminder: (Int?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(R.string.tasks_reminder_none)
    val hasDate = form.dueDate != null

    FormSection(stringResource(R.string.tasks_field_reminder), Icons.Rounded.NotificationsActive, FeatureAccent.NOTES) {
        WellRow(
            leading = { IconTile(Icons.Rounded.NotificationsActive, FeatureAccent.NOTES, size = 36.dp) },
            label = stringResource(R.string.tasks_field_reminder),
            value = form.reminderOffset?.let { reminderLabel(it) } ?: none,
            isSet = form.reminderOffset != null,
            enabled = hasDate,
            dropdown = true,
            onClick = { expanded = true },
            clearDescription = stringResource(R.string.tasks_clear_reminder),
            onClear = { onReminder(null) },
            menu = {
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    TaskEditViewModel.REMINDER_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option?.let { reminderLabel(it) } ?: none) },
                            onClick = { expanded = false; onReminder(option) },
                        )
                    }
                }
            },
        )
        if (!hasDate) {
            HintText(stringResource(R.string.tasks_reminder_needs_date))
        } else if (form.reminderOffset != null) {
            HintText(stringResource(R.string.tasks_reminder_note))
            if (notificationsOff) {
                Text(
                    stringResource(R.string.tasks_notif_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun ProjectSection(form: TaskForm, projects: List<ProjectEntity>, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val none = stringResource(R.string.tasks_project_none)
    val selected = projects.firstOrNull { it.id == form.projectId }

    FormSection(stringResource(R.string.tasks_field_project), Icons.Rounded.Folder, FeatureAccent.PROJECTS) {
        WellRow(
            leading = {
                if (selected != null) {
                    ColorDot(Color(selected.color), size = 36.dp)
                } else {
                    IconTile(Icons.Rounded.Folder, FeatureAccent.PROJECTS, size = 36.dp)
                }
            },
            label = stringResource(R.string.tasks_field_project),
            value = selected?.name ?: none,
            isSet = selected != null,
            dropdown = true,
            onClick = { expanded = true },
            menu = {
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(none) },
                        onClick = { expanded = false; onSelect(null) },
                    )
                    projects.forEach { project ->
                        DropdownMenuItem(
                            text = { Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { ColorDot(Color(project.color), size = 12.dp) },
                            onClick = { expanded = false; onSelect(project.id) },
                        )
                    }
                }
            },
        )
    }
}
