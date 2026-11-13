package com.funsol.orbit.feature.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.IconButton
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import com.funsol.orbit.core.common.rememberClickGuard
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientFab
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.TagPill
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private val TIMELINE_LABEL_WIDTH = 60.dp

@Composable
fun CalendarScreen(nav: OrbitNavigator) {
    val vm: CalendarViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val firstDay = remember(configuration) { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

    val snackbar = remember { SnackbarHostState() }
    val failedMsg = stringResource(R.string.error_generic)
    val currentFailedMsg by rememberUpdatedState(failedMsg)
    LaunchedEffect(vm) { vm.failures.collect { snackbar.showSnackbar(currentFailedMsg) } }

    val guard = rememberClickGuard()
    val addTask: () -> Unit = { guard.run { nav.navigate(AppDestination.TaskEdit(presetDate = state.selected.toEpochDayInt())) } }
    val openTask: (TaskEntity) -> Unit = { task -> guard.run { nav.navigate(AppDestination.TaskEdit(taskId = task.id)) } }

    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.canScrollBackward } }
    val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex >= 2 } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            OrbitTopBar(
                title = stringResource(R.string.nav_calendar),
                accent = FeatureAccent.CALENDAR,
                icon = Icons.Rounded.CalendarMonth,
                scrolled = scrolled,
                actions = {
                    IconButton(onClick = addTask) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.calendar_add_task))
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = showFab, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                GradientFab(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.calendar_add_task),
                    onClick = addTask,
                    brush = Orbit.extras.accentGradient(FeatureAccent.CALENDAR),
                    modifier = Modifier.padding(end = Spacing.md, bottom = Spacing.md),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                state.loading -> LoadingState()
                state.error -> ErrorState(stringResource(R.string.calendar_error), onRetry = vm::retry)
                else -> CalendarContent(
                    state = state,
                    firstDay = firstDay,
                    wide = configuration.screenWidthDp >= 600,
                    vm = vm,
                    listState = listState,
                    onOpen = openTask,
                    onAdd = addTask,
                )
            }
        }
    }
}

@Composable
private fun CalendarContent(
    state: CalendarUiState,
    firstDay: java.time.DayOfWeek,
    wide: Boolean,
    vm: CalendarViewModel,
    listState: LazyListState,
    onOpen: (TaskEntity) -> Unit,
    onAdd: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.widthIn(max = if (wide) 720.dp else androidx.compose.ui.unit.Dp.Infinity).fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "month", contentType = "month") {
            MonthCard(state, firstDay, onSelect = vm::selectDay, onShift = vm::shiftMonth, onToday = vm::goToToday)
        }
        item(key = "mode", contentType = "mode") {
            ModeSwitcher(state.mode, vm::setMode)
        }
        when (state.mode) {
            CalendarMode.DAY -> dayItems(state, onOpen, onAdd, vm::onToggleComplete)
            CalendarMode.AGENDA -> agendaItems(state, onOpen, vm::onToggleComplete)
        }
    }
}

@Composable
private fun ModeSwitcher(mode: CalendarMode, onChange: (CalendarMode) -> Unit) {
    val options = listOf(
        CalendarMode.DAY to R.string.calendar_mode_day,
        CalendarMode.AGENDA to R.string.calendar_mode_agenda,
    )
    val label = stringResource(R.string.calendar_mode_label)
    val gradient = Orbit.extras.accentGradient(FeatureAccent.CALENDAR)
    val pill = RoundedCornerShape(50)
    OrbitWell(Modifier.fillMaxWidth().padding(top = Spacing.xs), shape = pill) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .selectableGroup()
                .semantics { contentDescription = label },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (value, text) ->
                val selected = mode == value
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(pill)
                        .then(if (selected) Modifier.background(gradient) else Modifier)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onChange(value) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(text),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                    )
                }
            }
        }
    }
}

private fun LazyListScope.dayItems(
    state: CalendarUiState,
    onOpen: (TaskEntity) -> Unit,
    onAdd: () -> Unit,
    onToggle: (TaskEntity, Boolean) -> Unit,
) {
    val schedule = state.schedule
    item(key = "day_header", contentType = "header") {
        Column(Modifier.fillMaxWidth().padding(top = Spacing.xs)) {
            Text(
                DateFormats.fullDate(state.selected),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (!schedule.isEmpty) {
                Text(
                    pluralStringResource(R.plurals.calendar_day_tasks, schedule.count, schedule.count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                )
            }
        }
    }
    if (schedule.isEmpty) {
        item(key = "day_empty", contentType = "empty") {
            EmptyState(
                icon = Icons.Rounded.EventAvailable,
                title = stringResource(R.string.calendar_empty_day_title),
                message = stringResource(R.string.calendar_empty_day_message, DateFormats.fullDate(state.selected)),
                actionLabel = stringResource(R.string.calendar_add_task),
                onAction = onAdd,
                accent = FeatureAccent.CALENDAR,
            )
        }
        return
    }
    if (schedule.allDay.isNotEmpty()) {
        item(key = "allday_header", contentType = "subheader") {
            SectionLabel(Icons.Rounded.EventAvailable, stringResource(R.string.calendar_all_day))
        }
        items(schedule.allDay, key = { "a_" + it.id }, contentType = { "task" }) { task ->
            CalendarTaskRow(task, state.today, showDate = false, onOpen = onOpen, onToggle = onToggle, modifier = Modifier.animateItem())
        }
    }
    if (schedule.timed.isNotEmpty()) {
        item(key = "timed_header", contentType = "subheader") {
            SectionLabel(Icons.Rounded.Schedule, stringResource(R.string.calendar_scheduled))
        }
        val byHour = schedule.timed.groupBy { (it.dueTime ?: 0) / 60 }.toList()
        byHour.forEachIndexed { index, (hour, tasks) ->
            item(key = "hour_$hour", contentType = "hour") {
                TimelineStop(
                    label = DateFormats.time(hour * 60),
                    first = index == 0,
                    last = index == byHour.lastIndex,
                    modifier = Modifier.animateItem(),
                ) {
                    tasks.forEach { task ->
                        CalendarTaskRow(task, state.today, showDate = false, onOpen = onOpen, onToggle = onToggle)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineStop(
    label: String,
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val dot = Orbit.extras.accent(FeatureAccent.CALENDAR)
    val labelWidth = TIMELINE_LABEL_WIDTH * LocalDensity.current.fontScale.coerceIn(1f, 2f)
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                val rtl = layoutDirection == LayoutDirection.Rtl
                val offset = (labelWidth + 12.dp).toPx()
                val cx = if (rtl) size.width - offset else offset
                val dotY = 16.dp.toPx()
                drawLine(
                    color = line,
                    start = Offset(cx, if (first) dotY else 0f),
                    end = Offset(cx, if (last) dotY else size.height),
                    strokeWidth = 2.dp.toPx(),
                )
                drawCircle(dot, radius = 5.dp.toPx(), center = Offset(cx, dotY))
            },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Orbit.extras.textMuted,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(labelWidth).padding(top = 8.dp),
        )
        Column(
            Modifier.weight(1f).padding(start = 24.dp, bottom = if (last) 0.dp else Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) { content() }
    }
}

@Composable
private fun SectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        Modifier.padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = Orbit.extras.accent(FeatureAccent.CALENDAR), modifier = Modifier.size(18.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = Orbit.extras.sectionTitle,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.agendaItems(
    state: CalendarUiState,
    onOpen: (TaskEntity) -> Unit,
    onToggle: (TaskEntity, Boolean) -> Unit,
) {
    if (state.agenda.isEmpty()) {
        item(key = "agenda_empty", contentType = "empty") {
            EmptyState(
                icon = Icons.Rounded.EventBusy,
                title = stringResource(R.string.calendar_empty_agenda_title),
                message = stringResource(R.string.calendar_empty_agenda_message),
                accent = FeatureAccent.CALENDAR,
            )
        }
        return
    }
    for (group in state.agenda) {
        val keyPrefix = when (val k = group.key) {
            AgendaKey.Overdue -> "overdue"
            is AgendaKey.Day -> "d${k.epochDay}"
        }
        stickyHeader(key = "h_$keyPrefix", contentType = "subheader") {
            AgendaHeader(group.key, state.today, group.tasks.size)
        }
        items(group.tasks, key = { "${keyPrefix}_${it.id}" }, contentType = { "task" }) { task ->
            CalendarTaskRow(
                task = task,
                today = state.today,
                showDate = group.key == AgendaKey.Overdue,
                onOpen = onOpen,
                onToggle = onToggle,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun AgendaHeader(key: AgendaKey, today: LocalDate, count: Int) {
    val extras = Orbit.extras
    val scheme = MaterialTheme.colorScheme
    val countText = pluralStringResource(R.plurals.calendar_cell_tasks, count, count)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    when (key) {
                        AgendaKey.Overdue -> scheme.error.copy(alpha = 0.12f)
                        is AgendaKey.Day -> extras.accent(FeatureAccent.CALENDAR).copy(alpha = 0.10f)
                    },
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (key) {
                AgendaKey.Overdue -> {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = scheme.error)
                    Text(
                        stringResource(R.string.calendar_overdue_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = scheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TagPill(countText, scheme.error)
                }
                is AgendaKey.Day -> {
                    val date = key.epochDay.toLocalDate()
                    val away = key.epochDay - today.toEpochDayInt()
                    val coral = extras.accent(FeatureAccent.CALENDAR)
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(extras.accentGradient(FeatureAccent.CALENDAR)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        when (away) {
                            0 -> Text(stringResource(R.string.tasks_due_today), style = MaterialTheme.typography.titleSmall, color = coral)
                            1 -> Text(stringResource(R.string.tasks_due_tomorrow), style = MaterialTheme.typography.titleSmall, color = coral)
                        }
                        Text(
                            DateFormats.fullDate(date),
                            style = if (away in 0..1) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall,
                            color = if (away in 0..1) extras.textMuted else scheme.onSurface,
                        )
                    }
                    TagPill(countText, coral)
                }
            }
        }
    }
}
