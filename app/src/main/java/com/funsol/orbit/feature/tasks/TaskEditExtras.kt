package com.funsol.orbit.feature.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.TasksEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.WeekdayMask
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitChipColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@Composable
internal fun SubtasksSection(
    form: TaskForm,
    onAdd: (String) -> Boolean,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val context = LocalContext.current
    var draft by rememberSaveable { mutableStateOf("") }
    val submit = { context.postAnalytic(TasksEvents.TASK_EDIT_SUBTASK_ADD_CLICK); if (onAdd(draft)) draft = "" }
    FormSection(stringResource(R.string.tasks_section_subtasks), Icons.Rounded.Checklist, FeatureAccent.HABITS) {
        if (form.subtasks.isEmpty()) HintText(stringResource(R.string.tasks_subtasks_empty))
        form.subtasks.forEachIndexed { index, sub ->
            key(sub.id) {
                SubtaskItem(
                    sub = sub,
                    canMoveUp = index > 0,
                    canMoveDown = index < form.subtasks.lastIndex,
                    onToggle = onToggle,
                    onDelete = onDelete,
                    onMove = onMove,
                )
            }
        }
        OrbitWell(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it.take(TaskEditViewModel.MAX_SUBTASK_TITLE) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.tasks_subtask_hint)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                val canAdd = draft.isNotBlank()
                val addDescription = stringResource(R.string.tasks_subtask_add)
                Box(
                    Modifier
                        .size(44.dp)
                        .alpha(if (canAdd) 1f else 0.4f)
                        .clip(CircleShape)
                        .background(Orbit.extras.accentGradient(FeatureAccent.HABITS))
                        .clickable(enabled = canAdd, role = Role.Button) { submit() }
                        .semantics { contentDescription = addDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SubtaskItem(
    sub: SubtaskDraft,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        OrbitWell(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CompletionCheck(
                    checked = sub.done,
                    title = sub.title,
                    onCheckedChange = { context.postAnalytic(TasksEvents.TASK_EDIT_SUBTASK_TOGGLE); onToggle(sub.id, it) },
                    accent = FeatureAccent.HABITS,
                )
                Text(
                    sub.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(vertical = Spacing.sm),
                    color = if (sub.done) Orbit.extras.textMuted else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (sub.done) TextDecoration.LineThrough else null,
                )
                if (canMoveUp || canMoveDown) {
                    Box {
                        IconButton(onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_SUBTASK_MENU_CLICK); menuOpen = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.tasks_row_menu, sub.title),
                                tint = Orbit.extras.textMuted,
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (canMoveUp) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tasks_subtask_move_up, sub.title), maxLines = 1) },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                                    onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_SUBTASK_MOVE_UP_CLICK); menuOpen = false; onMove(sub.id, -1) },
                                )
                            }
                            if (canMoveDown) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.tasks_subtask_move_down, sub.title), maxLines = 1) },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                                    onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_SUBTASK_MOVE_DOWN_CLICK); menuOpen = false; onMove(sub.id, 1) },
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = {
                    context.postAnalytic(TasksEvents.TASK_EDIT_SUBTASK_DELETE_CLICK)
                    scope.launch {
                        visibility.targetState = false
                        delay(220)
                        onDelete(sub.id)
                    }
                }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.tasks_subtask_delete, sub.title),
                        tint = Orbit.extras.textMuted,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecurrenceSection(
    form: TaskForm,
    showOccurrenceHint: Boolean,
    onType: (RecurrenceType) -> Unit,
    onInterval: (Int) -> Unit,
    onWeekday: (DayOfWeek) -> Unit,
    onEnd: (Int?) -> Unit,
) {
    val context = LocalContext.current
    var showEnd by rememberSaveable { mutableStateOf(false) }
    val types = listOf(
        RecurrenceType.NONE to R.string.tasks_repeat_none,
        RecurrenceType.DAILY to R.string.tasks_repeat_daily,
        RecurrenceType.WEEKLY to R.string.tasks_repeat_weekly,
        RecurrenceType.MONTHLY to R.string.tasks_repeat_monthly,
        RecurrenceType.YEARLY to R.string.tasks_repeat_yearly,
    )
    FormSection(stringResource(R.string.tasks_section_repeat), Icons.Rounded.Repeat, FeatureAccent.ANALYTICS) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            types.forEach { (type, label) ->
                FilterChip(
                    selected = form.recurrenceType == type,
                    onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_REPEAT_TYPE_SELECT); onType(type) },
                    label = { Text(stringResource(label)) },
                    colors = orbitChipColors(),
                    shape = RoundedCornerShape(Radius.chip),
                )
            }
        }
        if (form.recurrenceType == RecurrenceType.NONE) {
            HintText(stringResource(R.string.tasks_repeat_needs_date))
        } else {
            IntervalField(form, onInterval)
            if (form.recurrenceType == RecurrenceType.WEEKLY) {
                Text(
                    stringResource(R.string.tasks_repeat_weekdays),
                    style = MaterialTheme.typography.labelMedium,
                    color = Orbit.extras.textMuted,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = WeekdayMask.contains(form.recurrenceWeekdays, day),
                            onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_REPEAT_WEEKDAY_TOGGLE); onWeekday(day) },
                            label = { Text(weekdayShort(day)) },
                            colors = orbitChipColors(),
                            shape = RoundedCornerShape(Radius.chip),
                        )
                    }
                }
            }
            WellRow(
                leading = { IconTile(Icons.Rounded.Event, FeatureAccent.ANALYTICS, size = 36.dp) },
                label = stringResource(R.string.tasks_repeat_ends),
                value = form.recurrenceEnd?.let { DateFormats.date(it) } ?: stringResource(R.string.tasks_repeat_never),
                isSet = form.recurrenceEnd != null,
                onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_REPEAT_END_CLICK); showEnd = true },
                clearDescription = stringResource(R.string.tasks_repeat_clear_end),
                onClear = { context.postAnalytic(TasksEvents.TASK_EDIT_REPEAT_END_CLEAR_CLICK); onEnd(null) },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Rounded.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    recurrenceSummaryText(form),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (showOccurrenceHint) HintText(stringResource(R.string.tasks_repeat_occurrence_hint))
    }

    if (showEnd) {
        TaskDatePickerDialog(
            initialEpochDay = form.recurrenceEnd ?: form.dueDate,
            onDismiss = { showEnd = false },
            onPick = { showEnd = false; onEnd(it) },
        )
    }
}

@Composable
private fun IntervalField(form: TaskForm, onInterval: (Int) -> Unit) {
    var text by rememberSaveable(form.recurrenceType) { mutableStateOf(form.recurrenceInterval.toString()) }
    val unitRes = when (form.recurrenceType) {
        RecurrenceType.DAILY -> R.plurals.tasks_unit_day
        RecurrenceType.WEEKLY -> R.plurals.tasks_unit_week
        RecurrenceType.MONTHLY -> R.plurals.tasks_unit_month
        else -> R.plurals.tasks_unit_year
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(3)
                text = digits
                digits.toIntOrNull()?.let { if (it >= 1) onInterval(it) }
            },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = orbitFieldColors(),
            label = { Text(stringResource(R.string.tasks_repeat_interval_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        )
        Text(pluralStringResource(unitRes, form.recurrenceInterval), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun weekdayShort(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.MONDAY -> R.string.weekday_mon
        DayOfWeek.TUESDAY -> R.string.weekday_tue
        DayOfWeek.WEDNESDAY -> R.string.weekday_wed
        DayOfWeek.THURSDAY -> R.string.weekday_thu
        DayOfWeek.FRIDAY -> R.string.weekday_fri
        DayOfWeek.SATURDAY -> R.string.weekday_sat
        DayOfWeek.SUNDAY -> R.string.weekday_sun
    },
)

@Composable
internal fun recurrenceSummaryText(form: TaskForm): String {
    val d = RecurrenceSummary.describe(form.recurrenceType, form.recurrenceInterval, form.recurrenceWeekdays, form.recurrenceEnd)
        ?: return ""
    val days = d.days.map { weekdayShort(it) }.joinToString(", ")
    val base = when (d.type) {
        RecurrenceType.DAILY -> pluralStringResource(R.plurals.tasks_summary_daily, d.interval, d.interval)
        RecurrenceType.WEEKLY ->
            if (d.days.isEmpty()) {
                pluralStringResource(R.plurals.tasks_summary_weekly, d.interval, d.interval)
            } else {
                pluralStringResource(R.plurals.tasks_summary_weekly_on, d.interval, d.interval, days)
            }
        RecurrenceType.MONTHLY -> pluralStringResource(R.plurals.tasks_summary_monthly, d.interval, d.interval)
        RecurrenceType.YEARLY -> pluralStringResource(R.plurals.tasks_summary_yearly, d.interval, d.interval)
        RecurrenceType.NONE -> ""
    }
    return d.endEpochDay?.let { stringResource(R.string.tasks_summary_until, base, DateFormats.date(it)) } ?: base
}
