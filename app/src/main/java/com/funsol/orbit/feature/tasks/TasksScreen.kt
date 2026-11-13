package com.funsol.orbit.feature.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.TasksEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.designsystem.ConfirmDialog
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientFab
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TasksScreen(nav: OrbitNavigator) {
    PostAnalytic(TasksEvents.TASKS_SCREEN_DISPLAY)
    val context = LocalContext.current
    val vm: TasksViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.tasks_deleted)
    val archivedMsg = stringResource(R.string.tasks_archived_msg)
    val failedMsg = stringResource(R.string.error_generic)
    val undoLabel = stringResource(R.string.action_undo)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            snackbar.currentSnackbarData?.dismiss()
            scope.launch {
                when (event) {
                    is TasksEvent.Deleted ->
                        if (snackbar.showSnackbar(deletedMsg, undoLabel, withDismissAction = true, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) { context.postAnalytic(TasksEvents.TASKS_DELETE_UNDO_CLICK); vm.undoDelete(event) }
                    is TasksEvent.Archived ->
                        if (snackbar.showSnackbar(archivedMsg, undoLabel, withDismissAction = true, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) { context.postAnalytic(TasksEvents.TASKS_ARCHIVE_UNDO_CLICK); vm.undoArchive(event) }
                    TasksEvent.Failed -> snackbar.showSnackbar(failedMsg)
                }
            }
        }
    }

    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSort by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    val guard = rememberClickGuard()
    val actions = remember(vm, nav) {
        TaskRowActions(
            onOpen = { task -> context.postAnalytic(TasksEvents.TASKS_ROW_OPEN_CLICK); guard.run { nav.navigate(AppDestination.TaskEdit(taskId = task.id)) } },
            onToggle = { task, checked -> context.postAnalytic(TasksEvents.TASKS_ROW_COMPLETE_TOGGLE); vm.onToggleComplete(task, checked) },
            onMarkInProgress = { task -> context.postAnalytic(TasksEvents.TASKS_MENU_IN_PROGRESS_CLICK); vm.onMarkInProgress(task) },
            onReopen = { task -> context.postAnalytic(TasksEvents.TASKS_MENU_REOPEN_CLICK); vm.onReopen(task) },
            onArchive = { task -> context.postAnalytic(TasksEvents.TASKS_MENU_ARCHIVE_CLICK); vm.onArchive(task) },
            onRestore = { task -> context.postAnalytic(TasksEvents.TASKS_MENU_RESTORE_CLICK); vm.onRestore(task) },
            onDelete = { task ->
                context.postAnalytic(TasksEvents.TASKS_MENU_DELETE_CLICK)
                if (task.recurrenceType != RecurrenceType.NONE) pendingDeleteId = task.id else vm.onDelete(task)
            },
        )
    }

    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.canScrollBackward } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            OrbitTopBar(
                title = stringResource(R.string.nav_tasks),
                accent = FeatureAccent.TASKS,
                icon = Icons.Rounded.TaskAlt,
                scrolled = scrolled,
                actions = {
                    ToolbarButton(
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        description = stringResource(R.string.tasks_sort_action),
                        onClick = { context.postAnalytic(TasksEvents.TASKS_SORT_CLICK); showSort = true },
                    )
                    val active = state.filters.activeCount
                    ToolbarButton(
                        icon = Icons.Rounded.FilterList,
                        description = if (active > 0) {
                            pluralStringResource(R.plurals.tasks_filters_active, active, active)
                        } else {
                            stringResource(R.string.tasks_filter_action)
                        },
                        badge = active,
                        onClick = { context.postAnalytic(TasksEvents.TASKS_FILTER_CLICK); showFilters = true },
                    )
                },
            )
        },
        floatingActionButton = {
            GradientFab(
                icon = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.tasks_new),
                onClick = { context.postAnalytic(TasksEvents.TASKS_NEW_FAB_CLICK); guard.run { nav.navigate(AppDestination.TaskEdit()) } },
                label = if (scrolled) null else stringResource(R.string.tasks_new),
                brush = Orbit.extras.accentGradient(FeatureAccent.TASKS),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                SearchField(query, vm::setQuery)
                ViewChips(state) { context.postAnalytic(TasksEvents.TASKS_VIEW_SELECT); vm.setView(it) }
                SummaryStrip(state)
                FilterSummaryBar(state.filters.activeCount) { context.postAnalytic(TasksEvents.TASKS_FILTERS_CLEAR_CLICK); vm.clearFilters() }
                when {
                    state.loading -> LoadingState()
                    state.error -> ErrorState(stringResource(R.string.tasks_error_load), onRetry = { context.postAnalytic(TasksEvents.TASKS_RETRY_CLICK); vm.retry() })
                    state.items.isEmpty() -> TasksEmpty(
                        state = state,
                        hasQuery = state.hasQuery,
                        onNewTask = { context.postAnalytic(TasksEvents.TASKS_EMPTY_NEW_CLICK); guard.run { nav.navigate(AppDestination.TaskEdit()) } },
                        onNewTaskToday = { context.postAnalytic(TasksEvents.TASKS_EMPTY_NEW_TODAY_CLICK); guard.run { nav.navigate(AppDestination.TaskEdit(presetDate = state.today.toEpochDayInt())) } },
                        onClearFilters = { context.postAnalytic(TasksEvents.TASKS_EMPTY_CLEAR_CLICK); vm.clearSearchAndFilters() },
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                    ) {
                        items(state.items, key = { it.task.id }, contentType = { "task" }) { item ->
                            TaskRow(item = item, today = state.today, actions = actions, modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }

    if (showSort) {
        TaskSortSheet(
            sort = state.sort,
            onField = vm::setSortField,
            onReverse = vm::toggleSortReversed,
            onDismiss = { context.postAnalytic(TasksEvents.TASKS_SORT_DISMISS_CLICK); showSort = false },
        )
    }
    if (showFilters) {
        TaskFilterSheet(
            filters = state.filters,
            projects = state.projects,
            onChange = vm::setFilters,
            onClear = vm::clearFilters,
            onDismiss = { context.postAnalytic(TasksEvents.TASKS_FILTER_DISMISS_CLICK); showFilters = false },
        )
    }

    val pendingDelete = pendingDeleteId?.let { id -> state.items.firstOrNull { it.task.id == id }?.task }
    if (pendingDelete != null) {
        PostAnalytic(TasksEvents.TASKS_DELETE_RECURRING_DIALOG_APPEAR)
        ConfirmDialog(
            title = stringResource(R.string.tasks_delete_recurring_title),
            message = stringResource(R.string.tasks_delete_recurring_message, pendingDelete.title),
            confirmLabel = stringResource(R.string.tasks_delete_confirm),
            destructive = true,
            onConfirm = { context.postAnalytic(TasksEvents.TASKS_DELETE_RECURRING_CONFIRM_CLICK); pendingDeleteId = null; vm.onDelete(pendingDelete) },
            onDismiss = { context.postAnalytic(TasksEvents.TASKS_DELETE_RECURRING_CANCEL_CLICK); pendingDeleteId = null },
        )
    }
}

@Composable
private fun ToolbarButton(icon: ImageVector, description: String, onClick: () -> Unit, badge: Int = 0) {
    val tint = Orbit.extras.accent(FeatureAccent.TASKS)
    Box(
        Modifier
            .size(Spacing.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(Radius.tile)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        if (badge > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge.toString(), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    OrbitWell(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
        shape = RoundedCornerShape(Radius.button),
    ) {
        TextField(
            value = query,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.tasks_search_hint), color = Orbit.extras.textMuted) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Orbit.extras.accent(FeatureAccent.TASKS)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { context.postAnalytic(TasksEvents.TASKS_SEARCH_CLEAR_CLICK); onChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        )
    }
}

@Composable
private fun ViewChips(state: TasksUiState, onSelect: (TaskView) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(TaskView.entries, key = { it.name }, contentType = { "chip" }) { view ->
            val name = stringResource(viewLabel(view))
            val count = state.counts[view]
            val selected = state.view == view
            val danger = view == TaskView.OVERDUE
            val a11y = if (count != null) stringResource(R.string.tasks_view_with_count, name, count) else name
            val base = orbitChipColors()
            val colors = if (danger) {
                FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    labelColor = MaterialTheme.colorScheme.error,
                    selectedContainerColor = MaterialTheme.colorScheme.error,
                    selectedLabelColor = Color.White,
                )
            } else {
                base
            }
            FilterChip(
                selected = selected,
                onClick = { onSelect(view) },
                modifier = Modifier.semantics { contentDescription = a11y },
                shape = RoundedCornerShape(Radius.chip),
                colors = colors,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(name, maxLines = 1)
                        if (count != null) CountBadge(count, selected, danger)
                    }
                },
            )
        }
    }
}

@Composable
private fun CountBadge(count: Int, selected: Boolean, danger: Boolean) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .heightIn(min = 20.dp)
            .widthIn(min = 20.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color.White.copy(alpha = 0.26f) else tint.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else tint,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryStrip(state: TasksUiState) {
    val dueToday = state.counts[TaskView.TODAY] ?: 0
    val overdue = state.counts[TaskView.OVERDUE] ?: 0
    AnimatedVisibility(visible = !state.loading && !state.error && (dueToday > 0 || overdue > 0)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (dueToday > 0) {
                MetaPill(
                    icon = Icons.Rounded.Today,
                    text = pluralStringResource(R.plurals.tasks_summary_due_today, dueToday, dueToday),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (overdue > 0) {
                MetaPill(
                    icon = Icons.Rounded.WarningAmber,
                    text = pluralStringResource(R.plurals.tasks_summary_overdue, overdue, overdue),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FilterSummaryBar(activeCount: Int, onClear: () -> Unit) {
    AnimatedVisibility(visible = activeCount > 0) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                pluralStringResource(R.plurals.tasks_filters_active, activeCount.coerceAtLeast(1), activeCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear) { Text(stringResource(R.string.tasks_filters_clear)) }
        }
    }
}

private fun viewLabel(view: TaskView): Int = when (view) {
    TaskView.ALL -> R.string.tasks_view_all
    TaskView.TODAY -> R.string.tasks_view_today
    TaskView.UPCOMING -> R.string.tasks_view_upcoming
    TaskView.OVERDUE -> R.string.tasks_view_overdue
    TaskView.COMPLETED -> R.string.tasks_view_completed
    TaskView.ARCHIVED -> R.string.tasks_view_archived
}

@Composable
private fun TasksEmpty(
    state: TasksUiState,
    hasQuery: Boolean,
    onNewTask: () -> Unit,
    onNewTaskToday: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val filtered = hasQuery || state.filters.isActive
    var icon: ImageVector = Icons.Rounded.Inbox
    var title: Int = R.string.tasks_empty_all_title
    var message: Int = R.string.tasks_empty_all_message
    var actionLabel: Int? = null
    var action: (() -> Unit)? = null
    when {
        filtered && state.totalTasks > 0 -> {
            icon = Icons.Rounded.SearchOff; title = R.string.tasks_empty_filtered_title; message = R.string.tasks_empty_filtered_message
            actionLabel = R.string.tasks_filters_clear; action = onClearFilters
        }
        else -> when (state.view) {
            TaskView.ALL -> {
                icon = Icons.Rounded.Inbox; title = R.string.tasks_empty_all_title; message = R.string.tasks_empty_all_message
                actionLabel = R.string.tasks_new; action = onNewTask
            }
            TaskView.TODAY -> {
                icon = Icons.Rounded.Today; title = R.string.tasks_empty_today_title; message = R.string.tasks_empty_today_message
                actionLabel = R.string.tasks_empty_today_action; action = onNewTaskToday
            }
            TaskView.UPCOMING -> {
                icon = Icons.Rounded.Upcoming; title = R.string.tasks_empty_upcoming_title; message = R.string.tasks_empty_upcoming_message
            }
            TaskView.OVERDUE -> {
                icon = Icons.Rounded.EventAvailable; title = R.string.tasks_empty_overdue_title; message = R.string.tasks_empty_overdue_message
            }
            TaskView.COMPLETED -> {
                icon = Icons.Rounded.DoneAll; title = R.string.tasks_empty_completed_title; message = R.string.tasks_empty_completed_message
            }
            TaskView.ARCHIVED -> {
                icon = Icons.Rounded.Archive; title = R.string.tasks_empty_archived_title; message = R.string.tasks_empty_archived_message
            }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        EmptyState(
            icon = icon,
            title = stringResource(title),
            message = stringResource(message),
            actionLabel = actionLabel?.let { stringResource(it) },
            onAction = action,
            accent = FeatureAccent.TASKS,
        )
    }
}
