package com.funsol.orbit.feature.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.ProjectsEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientFab
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.ProgressRing
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProjectsScreen(nav: OrbitNavigator) {
    val vm: ProjectsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val guard = rememberClickGuard()
    val context = LocalContext.current
    PostAnalytic(ProjectsEvents.PROJECTS_SCREEN_DISPLAY)
    ProjectsContent(
        state = state,
        onBack = if (!nav.isTab) ({
            context.postAnalytic(ProjectsEvents.PROJECTS_BACK_CLICK)
            guard.run { nav.back() }
        }) else null,
        onFilter = {
            context.postAnalytic(ProjectsEvents.PROJECTS_FILTER_SELECT)
            vm.setFilter(it)
        },
        onOpen = { id ->
            context.postAnalytic(ProjectsEvents.PROJECTS_ITEM_CLICK)
            guard.run { nav.navigate(AppDestination.ProjectDetail(id)) }
        },
        onCreate = {
            context.postAnalytic(ProjectsEvents.PROJECTS_NEW_CLICK)
            guard.run { nav.navigate(AppDestination.ProjectEdit()) }
        },
        onRetry = {
            context.postAnalytic(ProjectsEvents.PROJECTS_RETRY_CLICK)
            vm.retry()
        },
    )
}

@Composable
private fun ProjectsContent(
    state: ProjectsUiState,
    onBack: (() -> Unit)?,
    onFilter: (ProjectFilter) -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
) {
    val extras = Orbit.extras
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.canScrollBackward } }

    // Empty state already shows "New project"; suppress the duplicate FAB there.
    val emptyActionHandledInState = remember(state.loading, state.error, state.totalProjects) {
        !state.loading && !state.error && state.totalProjects == 0
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            OrbitTopBar(
                scrolled = scrolled,
                title = stringResource(R.string.nav_projects),
                onBack = onBack,
                accent = FeatureAccent.PROJECTS,
                icon = Icons.Rounded.FolderCopy,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !emptyActionHandledInState,
                enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.85f),
                exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f),
            ) {
                GradientFab(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.projects_new),
                    onClick = onCreate,
                    brush = extras.accentGradient(FeatureAccent.PROJECTS),
                )
            }
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingState()
                state.error -> ErrorState(stringResource(R.string.error_generic), onRetry = onRetry)
                else -> Column(Modifier.fillMaxSize()) {
                    if (state.totalProjects > 0) FilterRow(state.filter, onFilter)
                    when {
                        state.totalProjects == 0 -> EmptyState(
                            icon = Icons.Rounded.FolderCopy,
                            title = stringResource(R.string.projects_empty_title),
                            message = stringResource(R.string.projects_empty_message),
                            actionLabel = stringResource(R.string.projects_new),
                            onAction = onCreate,
                            accent = FeatureAccent.PROJECTS,
                        )
                        state.items.isEmpty() -> EmptyState(
                            icon = Icons.Rounded.FolderCopy,
                            title = stringResource(R.string.projects_filter_empty_title),
                            message = stringResource(R.string.projects_filter_empty_message),
                            accent = FeatureAccent.PROJECTS,
                        )
                        else -> {
                            val featuredId = state.items.firstOrNull { it.project.status == ProjectStatus.ACTIVE }?.project?.id
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                            ) {
                                items(
                                    state.items,
                                    key = { it.project.id },
                                    contentType = { if (it.project.id == featuredId) "featured" else "project" },
                                ) { item ->
                                    if (item.project.id == featuredId) {
                                        FeaturedProjectCard(item, onClick = { onOpen(item.project.id) }, modifier = Modifier.animateItem())
                                    } else {
                                        ProjectCard(item, onClick = { onOpen(item.project.id) }, modifier = Modifier.animateItem())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: ProjectFilter, onSelect: (ProjectFilter) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(ProjectFilter.entries, key = { it.name }, contentType = { "chip" }) { filter ->
            val label = stringResource(
                when (filter) {
                    ProjectFilter.ACTIVE -> R.string.status_active
                    ProjectFilter.ON_HOLD -> R.string.status_on_hold
                    ProjectFilter.COMPLETED -> R.string.status_completed
                    ProjectFilter.ARCHIVED -> R.string.status_archived
                    ProjectFilter.ALL -> R.string.projects_filter_all
                },
            )
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(label) },
                colors = orbitChipColors(),
                border = null,
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

@Composable
private fun tasksText(item: ProjectListItem): String =
    if (item.progress.total == 0) stringResource(R.string.projects_no_tasks)
    else pluralStringResource(R.plurals.projects_tasks_progress, item.progress.total, item.progress.done, item.progress.total)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCard(item: ProjectListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val project = item.project
    val accent = Color(project.color)
    val percent = item.progress.percent
    val progressDescription = stringResource(R.string.projects_progress_desc, percent)
    val muted = Orbit.extras.textMuted
    OrbitCard(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(accent.toGradient())
                .ringsDecoration()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(Icons.Rounded.FolderCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            InfoChip(projectStatusLabel(project.status), Color.White, onGradient = true)
        }
        Row(
            Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (project.description.isNotBlank()) {
                    Text(
                        project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    InfoChip(tasksText(item), muted, icon = Icons.Rounded.TaskAlt)
                    dateRangeText(project.startDate, project.endDate)?.let { InfoChip(it, muted, icon = Icons.Rounded.CalendarMonth) }
                    if (item.overdue > 0) {
                        InfoChip(
                            pluralStringResource(R.plurals.projects_overdue_count, item.overdue, item.overdue),
                            MaterialTheme.colorScheme.error,
                            icon = Icons.Rounded.ErrorOutline,
                        )
                    }
                }
            }
            ProgressRing(
                fraction = item.progress.fraction,
                label = stringResource(R.string.projects_percent, percent),
                size = 68.dp,
                color = accent,
                modifier = Modifier.clearAndSetSemantics { contentDescription = progressDescription },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeaturedProjectCard(item: ProjectListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val project = item.project
    val accent = Color(project.color)
    val percent = item.progress.percent
    val shape = RoundedCornerShape(28.dp)
    val source = remember { MutableInteractionSource() }
    val progressDescription = stringResource(R.string.projects_progress_desc, percent)
    Column(
        modifier
            .fillMaxWidth()
            .pressScale(source, 0.97f)
            .orbitGlow(shape, accent.copy(alpha = 0.38f), radius = 18.dp, offsetY = 8.dp)
            .clip(shape)
            .background(accent.toGradient())
            .ringsDecoration()
            .clickable(interactionSource = source, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(Spacing.md + Spacing.xs)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            InfoChip(projectStatusLabel(project.status), Color.White, onGradient = true)
            dateRangeText(project.startDate, project.endDate)?.let {
                InfoChip(it, Color.White, icon = Icons.Rounded.CalendarMonth, onGradient = true)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (project.description.isNotBlank()) {
                    Text(
                        project.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.86f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                ProgressRing(
                    fraction = item.progress.fraction,
                    label = stringResource(R.string.projects_percent, percent),
                    size = 70.dp,
                    color = accent,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = progressDescription },
                )
            }
        }
        LightProgressBar(item.progress.fraction)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                tasksText(item),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (item.overdue > 0) {
                InfoChip(
                    pluralStringResource(R.plurals.projects_overdue_count, item.overdue, item.overdue),
                    Color.White,
                    icon = Icons.Rounded.ErrorOutline,
                    onGradient = true,
                )
            }
        }
    }
}

@Composable
internal fun LightProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "lightProgress")
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.28f))) {
        Box(Modifier.fillMaxWidth(animated).fillMaxHeight().clip(CircleShape).background(Color.White))
    }
}
