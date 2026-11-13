package com.funsol.orbit.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.TasksEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.designsystem.ColorDot
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.designsystem.orbitSwitchColors
import com.funsol.orbit.core.designsystem.priorityColor
import com.funsol.orbit.core.designsystem.priorityLabel

private val FILTER_STATUSES = listOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED)

@Composable
internal fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OrbitWell(
        modifier.fillMaxWidth().semantics { this.selected = selected },
        onClick = onClick,
        selected = selected,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun TaskSortSheet(
    sort: TaskSort,
    onField: (TaskSortField) -> Unit,
    onReverse: () -> Unit,
    onDismiss: () -> Unit,
) {
    PostAnalytic(TasksEvents.TASKS_SORT_SHEET_APPEAR)
    val context = LocalContext.current
    val fields = listOf(
        TaskSortField.DUE_DATE to R.string.tasks_sort_due,
        TaskSortField.PRIORITY to R.string.tasks_sort_priority,
        TaskSortField.CREATED to R.string.tasks_sort_created,
        TaskSortField.TITLE to R.string.tasks_sort_title,
    )
    OrbitSheet(onDismiss) {
        Text(
            stringResource(R.string.tasks_sort_action),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Column(
            Modifier.fillMaxWidth().padding(top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            fields.forEach { (field, label) ->
                OptionRow(stringResource(label), selected = sort.field == field, onClick = { context.postAnalytic(TasksEvents.TASKS_SORT_FIELD_SELECT); onField(field) })
            }
            OrbitWell(Modifier.fillMaxWidth(), onClick = { context.postAnalytic(TasksEvents.TASKS_SORT_REVERSE_TOGGLE); onReverse() }) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.tasks_sort_reverse), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = sort.reversed, onCheckedChange = null, colors = orbitSwitchColors())
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.xs)) {
            GradientButton(stringResource(R.string.action_done), onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskFilterSheet(
    filters: TaskFilters,
    projects: List<ProjectEntity>,
    onChange: (TaskFilters) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    PostAnalytic(TasksEvents.TASKS_FILTER_SHEET_APPEAR)
    val context = LocalContext.current
    var showRange by rememberSaveable { mutableStateOf(false) }
    val maxScroll = (LocalConfiguration.current.screenHeightDp * 0.6f).dp

    OrbitSheet(onDismiss) {
        Text(
            stringResource(R.string.tasks_filter_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxScroll)
                .padding(top = Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FilterGroup(stringResource(R.string.tasks_filter_date)) {
                val options = listOf(
                    DateFilter.ANY to R.string.tasks_filter_date_any,
                    DateFilter.TODAY to R.string.tasks_filter_date_today,
                    DateFilter.NEXT_7_DAYS to R.string.tasks_filter_date_next7,
                    DateFilter.THIS_MONTH to R.string.tasks_filter_date_month,
                    DateFilter.NO_DATE to R.string.tasks_filter_date_none,
                )
                options.forEach { (opt, label) ->
                    FilterChip(
                        selected = filters.date == opt,
                        onClick = { context.postAnalytic(TasksEvents.TASKS_FILTER_DATE_SELECT); onChange(filters.copy(date = opt, customStart = null, customEnd = null)) },
                        label = { Text(stringResource(label)) },
                        colors = orbitChipColors(),
                        shape = RoundedCornerShape(Radius.chip),
                    )
                }
                FilterChip(
                    selected = filters.date == DateFilter.CUSTOM,
                    onClick = { context.postAnalytic(TasksEvents.TASKS_FILTER_DATE_CUSTOM_CLICK); showRange = true },
                    colors = orbitChipColors(),
                    shape = RoundedCornerShape(Radius.chip),
                    label = {
                        Text(
                            if (filters.date == DateFilter.CUSTOM) {
                                stringResource(
                                    R.string.tasks_filter_date_range,
                                    filters.customStart?.let { DateFormats.dateShort(it) } ?: "…",
                                    filters.customEnd?.let { DateFormats.dateShort(it) } ?: "…",
                                )
                            } else {
                                stringResource(R.string.tasks_filter_date_custom)
                            },
                        )
                    },
                )
            }

            FilterGroup(stringResource(R.string.tasks_filter_priority)) {
                Priority.entries.reversed().forEach { p ->
                    FilterChip(
                        selected = p in filters.priorities,
                        onClick = {
                            context.postAnalytic(TasksEvents.TASKS_FILTER_PRIORITY_SELECT)
                            val next = if (p in filters.priorities) filters.priorities - p else filters.priorities + p
                            onChange(filters.copy(priorities = next))
                        },
                        leadingIcon = { ColorDot(priorityColor(p), size = 10.dp) },
                        label = { Text(priorityLabel(p)) },
                        colors = orbitChipColors(),
                        shape = RoundedCornerShape(Radius.chip),
                    )
                }
            }

            FilterGroup(stringResource(R.string.tasks_filter_status)) {
                FILTER_STATUSES.forEach { s ->
                    FilterChip(
                        selected = s in filters.statuses,
                        onClick = {
                            context.postAnalytic(TasksEvents.TASKS_FILTER_STATUS_SELECT)
                            val next = if (s in filters.statuses) filters.statuses - s else filters.statuses + s
                            onChange(filters.copy(statuses = next))
                        },
                        label = { Text(statusLabel(s)) },
                        colors = orbitChipColors(),
                        shape = RoundedCornerShape(Radius.chip),
                    )
                }
            }

            FilterGroup(stringResource(R.string.tasks_filter_project)) {
                FilterChip(
                    selected = filters.project == ProjectFilter.AnyProject,
                    onClick = { context.postAnalytic(TasksEvents.TASKS_FILTER_PROJECT_SELECT); onChange(filters.copy(project = ProjectFilter.AnyProject)) },
                    label = { Text(stringResource(R.string.tasks_filter_project_any)) },
                    colors = orbitChipColors(),
                    shape = RoundedCornerShape(Radius.chip),
                )
                FilterChip(
                    selected = filters.project == ProjectFilter.NoProject,
                    onClick = { context.postAnalytic(TasksEvents.TASKS_FILTER_PROJECT_SELECT); onChange(filters.copy(project = ProjectFilter.NoProject)) },
                    label = { Text(stringResource(R.string.tasks_filter_project_none)) },
                    colors = orbitChipColors(),
                    shape = RoundedCornerShape(Radius.chip),
                )
                projects.forEach { project ->
                    FilterChip(
                        selected = filters.project == ProjectFilter.OneProject(project.id),
                        onClick = { context.postAnalytic(TasksEvents.TASKS_FILTER_PROJECT_SELECT); onChange(filters.copy(project = ProjectFilter.OneProject(project.id))) },
                        leadingIcon = { ColorDot(Color(project.color), size = 10.dp) },
                        label = { Text(project.name, maxLines = 1) },
                        colors = orbitChipColors(),
                        shape = RoundedCornerShape(Radius.chip),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WellButton(stringResource(R.string.tasks_filters_clear), { context.postAnalytic(TasksEvents.TASKS_FILTER_SHEET_CLEAR_CLICK); onClear() }, Modifier.weight(1f), enabled = filters.isActive)
            GradientButton(stringResource(R.string.tasks_filter_apply), onDismiss, Modifier.weight(1f))
        }
    }

    if (showRange) {
        TaskDateRangePickerDialog(
            initialStart = filters.customStart,
            initialEnd = filters.customEnd,
            onDismiss = { showRange = false },
            onPick = { start, end ->
                showRange = false
                onChange(filters.copy(date = DateFilter.CUSTOM, customStart = start, customEnd = end ?: start))
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    OrbitWell(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = Orbit.extras.textMuted,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) { content() }
        }
    }
}
