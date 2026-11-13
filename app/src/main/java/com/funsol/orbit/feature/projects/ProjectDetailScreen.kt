package com.funsol.orbit.feature.projects

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.ProjectsEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.InfoBlue
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.ProgressRing
import com.funsol.orbit.core.designsystem.SectionHeader
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.SuccessGreen
import com.funsol.orbit.core.designsystem.WarningAmber
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProjectDetailScreen(projectId: String, nav: OrbitNavigator) {
    val vm: ProjectDetailViewModel = koinViewModel(key = "project_$projectId", parameters = { parametersOf(projectId) })
    val state by vm.state.collectAsStateWithLifecycle()
    val finished by vm.finished.collectAsStateWithLifecycle()
    LaunchedEffect(finished) { if (finished) nav.back() }

    var showDelete by rememberSaveable { mutableStateOf(false) }
    val project = state.project
    val guard = rememberClickGuard()
    val context = LocalContext.current
    PostAnalytic(ProjectsEvents.PROJECT_DETAIL_SCREEN_DISPLAY)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                title = stringResource(R.string.projects_detail_title),
                onBack = {
                    context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_BACK_CLICK)
                    guard.run { nav.back() }
                },
                accent = FeatureAccent.PROJECTS,
            ) {
                if (project != null) {
                    ProjectMenu(
                        project = project,
                        onEdit = { guard.run { nav.navigate(AppDestination.ProjectEdit(project.id)) } },
                        onSetStatus = vm::setStatus,
                        onDelete = { showDelete = true },
                    )
                }
            }
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingState()
                state.error -> ErrorState(stringResource(R.string.error_generic), onRetry = {
                    context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_RETRY_CLICK)
                    vm.retry()
                })
                project == null -> ErrorState(stringResource(R.string.projects_not_found))
                else -> DetailList(
                    state = state,
                    project = project,
                    onToggle = { id, done ->
                        context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_TASK_TOGGLE)
                        vm.setTaskCompleted(id, done)
                    },
                    onOpenTask = { id ->
                        context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_TASK_CLICK)
                        guard.run { nav.navigate(AppDestination.TaskEdit(taskId = id)) }
                    },
                    onAddTask = {
                        context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_ADD_TASK_CLICK)
                        guard.run { nav.navigate(AppDestination.TaskEdit(presetProjectId = project.id)) }
                    },
                )
            }
        }
    }

    if (showDelete && project != null) {
        DeleteProjectSheet(
            name = project.name,
            taskCount = state.linkedTaskCount,
            onKeepTasks = { showDelete = false; vm.delete(deleteTasks = false) },
            onDeleteAll = { showDelete = false; vm.delete(deleteTasks = true) },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun ProjectMenu(
    project: ProjectEntity,
    onEdit: () -> Unit,
    onSetStatus: (ProjectStatus) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = {
            context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_MENU_CLICK)
            expanded = true
        }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_EDIT_CLICK)
                    expanded = false
                    onEdit()
                },
            )
            val archived = project.status == ProjectStatus.ARCHIVED
            DropdownMenuItem(
                text = { Text(stringResource(if (archived) R.string.projects_restore else R.string.action_archive)) },
                leadingIcon = { Icon(if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, contentDescription = null) },
                onClick = {
                    context.postAnalytic(
                        if (archived) ProjectsEvents.PROJECT_DETAIL_RESTORE_CLICK else ProjectsEvents.PROJECT_DETAIL_ARCHIVE_CLICK,
                    )
                    expanded = false
                    onSetStatus(if (archived) ProjectStatus.ACTIVE else ProjectStatus.ARCHIVED)
                },
            )
            if (!archived) {
                val completed = project.status == ProjectStatus.COMPLETED
                DropdownMenuItem(
                    text = { Text(stringResource(if (completed) R.string.projects_mark_active else R.string.projects_mark_completed)) },
                    leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                    onClick = {
                        context.postAnalytic(
                            if (completed) ProjectsEvents.PROJECT_DETAIL_REOPEN_CLICK else ProjectsEvents.PROJECT_DETAIL_COMPLETE_CLICK,
                        )
                        expanded = false
                        onSetStatus(if (completed) ProjectStatus.ACTIVE else ProjectStatus.COMPLETED)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    context.postAnalytic(ProjectsEvents.PROJECT_DETAIL_DELETE_CLICK)
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun DetailList(
    state: ProjectDetailUiState,
    project: ProjectEntity,
    onToggle: (String, Boolean) -> Unit,
    onOpenTask: (String) -> Unit,
    onAddTask: () -> Unit,
) {
    val accent = Color(project.color)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
    ) {
        item(key = "header", contentType = "header") { HeroCard(project, state.stats, accent) }
        item(key = "stats", contentType = "stats") { StatsGrid(state.stats) }
        item(key = "tasks_header", contentType = "section") { SectionHeader(stringResource(R.string.projects_tasks_section)) }
        item(key = "add_task", contentType = "add") {
            GradientButton(
                text = stringResource(R.string.projects_add_task),
                onClick = onAddTask,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Add,
                brush = accent.toGradient(),
            )
        }
        if (state.tasks.isEmpty()) {
            item(key = "tasks_empty", contentType = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Checklist,
                    title = stringResource(R.string.projects_tasks_empty_title),
                    message = stringResource(R.string.projects_tasks_empty_message),
                    accent = FeatureAccent.PROJECTS,
                )
            }
        } else {
            items(state.tasks, key = { it.id }, contentType = { "task" }) { task ->
                TaskRow(task, accent, state.todayEpochDay, onToggle, onOpenTask, Modifier.animateItem())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroCard(project: ProjectEntity, stats: ProjectStats, accent: Color) {
    val progressDescription = stringResource(R.string.projects_progress_desc, stats.percent)
    GradientHero(brush = accent.toGradient(), glow = accent, modifier = Modifier.animateContentSize()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            InfoChip(projectStatusLabel(project.status), Color.White, onGradient = true)
            dateRangeText(project.startDate, project.endDate)?.let {
                InfoChip(it, Color.White, icon = Icons.Rounded.CalendarMonth, onGradient = true)
            }
        }
        Text(
            project.name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        if (project.description.isNotBlank()) {
            Text(
                project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                ProgressRing(
                    fraction = stats.percent / 100f,
                    label = stringResource(R.string.projects_percent, stats.percent),
                    size = 84.dp,
                    color = accent,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = progressDescription },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    if (stats.total == 0) stringResource(R.string.projects_no_tasks)
                    else pluralStringResource(R.plurals.projects_tasks_progress, stats.total, stats.completed, stats.total),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
                LightProgressBar(stats.percent / 100f)
                if (stats.overdue > 0) {
                    InfoChip(
                        pluralStringResource(R.plurals.projects_overdue_count, stats.overdue, stats.overdue),
                        Color.White,
                        icon = Icons.Rounded.ErrorOutline,
                        onGradient = true,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsGrid(stats: ProjectStats) {
    val overdueColor = if (stats.overdue > 0) MaterialTheme.colorScheme.error else Orbit.extras.textMuted
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        maxItemsInEachRow = 2,
    ) {
        TintedStatTile(stats.total.toString(), stringResource(R.string.projects_stat_total), MaterialTheme.colorScheme.primary, Icons.Rounded.Checklist, Modifier.weight(1f))
        TintedStatTile(stats.completed.toString(), stringResource(R.string.projects_stat_completed), SuccessGreen, Icons.Rounded.TaskAlt, Modifier.weight(1f))
        TintedStatTile(stats.inProgress.toString(), stringResource(R.string.projects_stat_in_progress), InfoBlue, Icons.Rounded.Autorenew, Modifier.weight(1f))
        TintedStatTile(stats.todo.toString(), stringResource(R.string.projects_stat_todo), WarningAmber, Icons.Rounded.RadioButtonUnchecked, Modifier.weight(1f))
        TintedStatTile(stats.overdue.toString(), stringResource(R.string.projects_stat_overdue), overdueColor, Icons.Rounded.ErrorOutline, Modifier.weight(1f))
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    accent: Color,
    todayEpochDay: Int,
    onToggle: (String, Boolean) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = task.status == TaskStatus.COMPLETED
    val overdue = !done && task.status != TaskStatus.ARCHIVED && task.dueDate != null && task.dueDate < todayEpochDay
    val checkLabel = stringResource(if (done) R.string.projects_task_mark_open else R.string.projects_task_mark_done, task.title)
    OrbitCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onOpen(task.id) },
        contentPadding = PaddingValues(start = Spacing.xs, end = Spacing.md, top = Spacing.xxs, bottom = Spacing.xxs),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            AnimatedCheck(
                checked = done,
                color = accent,
                onCheckedChange = { onToggle(task.id, it) },
                contentDescription = checkLabel,
                circleSize = 26.dp,
            )
            Column(Modifier.weight(1f).padding(start = Spacing.xs)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                    color = if (done) Orbit.extras.textMuted else MaterialTheme.colorScheme.onSurface,
                )
                task.dueDate?.let {
                    val text = if (overdue) stringResource(R.string.projects_task_overdue, DateFormats.date(it))
                    else stringResource(R.string.projects_task_due, DateFormats.date(it))
                    Text(
                        text,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdue) MaterialTheme.colorScheme.error else Orbit.extras.textMuted,
                    )
                }
            }
        }
    }
}
