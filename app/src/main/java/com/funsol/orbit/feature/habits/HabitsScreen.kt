package com.funsol.orbit.feature.habits

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.analytics.HabitsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientFab
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WarningAmber
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.feature.projects.AnimatedCheck
import com.funsol.orbit.feature.projects.CenteredContent
import com.funsol.orbit.feature.projects.GradientHero
import com.funsol.orbit.feature.projects.InfoChip
import com.funsol.orbit.feature.projects.toGradient
import com.funsol.orbit.feature.projects.LightProgressBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HabitsScreen(nav: OrbitNavigator) {
    PostAnalytic(HabitsEvents.HABITS_SCREEN_DISPLAY)
    val context = LocalContext.current
    val vm: HabitsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var archivedExpanded by rememberSaveable { mutableStateOf(false) }
    val guard = rememberClickGuard()
    val snackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.error_generic)
    val failures by vm.writeFailures.collectAsStateWithLifecycle()
    var handledFailures by rememberSaveable { mutableIntStateOf(failures) }
    LaunchedEffect(failures) {
        if (failures != handledFailures) {
            handledFailures = failures
            snackbar.showSnackbar(failedMessage)
        }
    }

    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.canScrollBackward } }

    // The empty state already offers "New habit"; hide the FAB in that case so the same
    // create action is not presented twice on one screen.
    val emptyActionHandledInState = remember(state.loading, state.error, state.active.isEmpty(), state.archived.isEmpty()) {
        !state.loading && !state.error && state.active.isEmpty() && state.archived.isEmpty()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            OrbitTopBar(
                scrolled = scrolled,
                title = stringResource(R.string.habits_title),
                onBack = {
                    context.postAnalytic(HabitsEvents.HABITS_BACK_CLICK)
                    guard.run(nav::back)
                },
                accent = FeatureAccent.HABITS,
                icon = Icons.Rounded.Repeat,
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
                    contentDescription = stringResource(R.string.habits_new),
                    onClick = {
                        context.postAnalytic(HabitsEvents.HABITS_ADD_FAB_CLICK)
                        guard.run { nav.navigate(AppDestination.HabitEdit()) }
                    },
                    brush = Orbit.extras.accentGradient(FeatureAccent.HABITS),
                )
            }
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingState()
                state.error -> ErrorState(
                    stringResource(R.string.error_generic),
                    onRetry = {
                        context.postAnalytic(HabitsEvents.HABITS_RETRY_CLICK)
                        vm.retry()
                    },
                )
                state.active.isEmpty() && state.archived.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.Repeat,
                    title = stringResource(R.string.habits_empty_title),
                    message = stringResource(R.string.habits_empty_message),
                    actionLabel = stringResource(R.string.habits_new),
                    onAction = {
                        context.postAnalytic(HabitsEvents.HABITS_EMPTY_ADD_CLICK)
                        guard.run { nav.navigate(AppDestination.HabitEdit()) }
                    },
                    accent = FeatureAccent.HABITS,
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (state.active.isEmpty()) {
                        item(key = "no_active", contentType = "empty") {
                            EmptyState(
                                icon = Icons.Rounded.Repeat,
                                title = stringResource(R.string.habits_no_active_title),
                                message = stringResource(R.string.habits_no_active_message),
                                accent = FeatureAccent.HABITS,
                            )
                        }
                    } else {
                        item(key = "today_hero", contentType = "hero") {
                            TodayHero(
                                scheduled = state.active.count { it.scheduledToday },
                                done = state.active.count { it.scheduledToday && it.checkedToday },
                                modifier = Modifier.padding(bottom = Spacing.sm),
                            )
                        }
                    }
                    items(state.active, key = { it.habit.id }, contentType = { "habit" }) { item ->
                        HabitCard(
                            item = item,
                            todayEpochDay = state.todayEpochDay,
                            onOpen = {
                                context.postAnalytic(HabitsEvents.HABITS_CARD_CLICK)
                                guard.run { nav.navigate(AppDestination.HabitDetail(item.habit.id)) }
                            },
                            onToggle = {
                                context.postAnalytic(HabitsEvents.HABITS_CHECK_TOGGLE)
                                vm.setChecked(item.habit.id, state.todayEpochDay, it)
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (state.archived.isNotEmpty()) {
                        item(key = "archived_header", contentType = "archived_header") {
                            ArchivedHeader(
                                state.archived.size,
                                archivedExpanded,
                                Modifier.padding(top = Spacing.sm),
                            ) {
                                context.postAnalytic(HabitsEvents.HABITS_ARCHIVED_SECTION_TOGGLE)
                                archivedExpanded = !archivedExpanded
                            }
                        }
                        if (archivedExpanded) {
                            items(state.archived, key = { "archived_${it.habit.id}" }, contentType = { "habit" }) { item ->
                                HabitCard(
                                    item = item,
                                    todayEpochDay = state.todayEpochDay,
                                    onOpen = {
                                        context.postAnalytic(HabitsEvents.HABITS_CARD_CLICK)
                                        guard.run { nav.navigate(AppDestination.HabitDetail(item.habit.id)) }
                                    },
                                    onToggle = null,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHero(scheduled: Int, done: Int, modifier: Modifier = Modifier) {
    val accent = Orbit.extras.accent(FeatureAccent.HABITS)
    val fraction = if (scheduled <= 0) 0f else done.toFloat() / scheduled
    val headline = when {
        scheduled == 0 -> stringResource(R.string.habits_hero_none)
        done >= scheduled -> stringResource(R.string.habits_hero_all_done)
        else -> stringResource(R.string.habits_hero_progress, done, scheduled)
    }
    GradientHero(
        brush = Orbit.extras.accentGradient(FeatureAccent.HABITS),
        glow = accent,
        modifier = modifier.animateContentSize(),
    ) {
        Text(
            stringResource(R.string.habits_hero_title),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.86f),
        )
        Text(
            headline,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(top = Spacing.xxs),
        )
        if (scheduled > 0) {
            LightProgressBar(fraction, Modifier.padding(top = Spacing.sm + Spacing.xs))
        }
    }
}

@Composable
private fun ArchivedHeader(count: Int, expanded: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val label = pluralStringResource(R.plurals.habits_archived_section, count, count)
    val stateText = stringResource(if (expanded) R.string.habits_collapse else R.string.habits_expand)
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "archivedChevron")
    OrbitWell(
        modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget)
            .clearAndSetSemantics { contentDescription = "$label, $stateText" },
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitCard(
    item: HabitListItem,
    todayEpochDay: Int,
    onOpen: () -> Unit,
    onToggle: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val habit = item.habit
    val accent = Color(habit.color)
    val muted = Orbit.extras.textMuted
    val schedule = scheduleSummary(habit.scheduleMask)
    val streakText = pluralStringResource(R.plurals.habits_streak_days, item.stats.currentStreak, item.stats.currentStreak)
    val todayText = when {
        habit.archived -> stringResource(R.string.habits_state_archived)
        !item.scheduledToday -> stringResource(R.string.habits_not_scheduled_today)
        item.checkedToday -> stringResource(R.string.habits_done_today)
        else -> stringResource(R.string.habits_not_done_today)
    }
    val description = stringResource(R.string.habits_card_desc, habit.name, streakText, todayText)
    val muteCard = habit.archived || (!item.scheduledToday)
    val edgeBrush = accent.toGradient()
    val today = todayEpochDay.takeIf { it > 0 }?.toLocalDate()?.dayOfWeek
    val largeFont = isLargeFontScale()
    val showToggle = onToggle != null && item.scheduledToday

    OrbitCard(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        contentPadding = PaddingValues(0.dp),
        tint = if (muteCard) null else accent,
        elevation = if (muteCard) CardElevation.Flat else CardElevation.Soft,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = largeFont || maxWidth < 340.dp
            Row(
                Modifier
                    .fillMaxWidth()
                    .alpha(if (muteCard) 0.78f else 1f)
                    .drawBehind {
                        val w = 6.dp.toPx()
                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - w else 0f
                        drawRect(edgeBrush, topLeft = Offset(x, 0f), size = Size(w, size.height))
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(role = Role.Button, onClick = onOpen)
                        .padding(start = Spacing.md + Spacing.xs, top = Spacing.md, bottom = Spacing.md, end = Spacing.sm)
                        .clearAndSetSemantics { contentDescription = description },
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        WeekdayDots(habit.scheduleMask, accent, today = today)
                        Text(
                            schedule,
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (stacked) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            HabitChips(item, streakText, muted, Modifier.weight(1f))
                            MiniRing(
                                fraction = item.stats.completionPercent / 100f,
                                label = stringResource(R.string.habits_percent_value, item.stats.completionPercent),
                                color = accent,
                            )
                        }
                    } else {
                        HabitChips(item, streakText, muted, Modifier.fillMaxWidth())
                    }
                }
                if (!stacked) {
                    MiniRing(
                        fraction = item.stats.completionPercent / 100f,
                        label = stringResource(R.string.habits_percent_value, item.stats.completionPercent),
                        color = accent,
                        modifier = Modifier.padding(end = Spacing.sm),
                    )
                }
                if (showToggle) {
                    HabitToggle(habit.name, item.checkedToday, accent, onToggle)
                } else {
                    Box(Modifier.width(Spacing.md))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitChips(item: HabitListItem, streakText: String, muted: Color, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        InfoChip(
            streakText,
            if (item.stats.currentStreak > 0) WarningAmber else muted,
            icon = Icons.Rounded.LocalFireDepartment,
        )
        if (!item.habit.archived && !item.scheduledToday) {
            InfoChip(stringResource(R.string.habits_not_scheduled_today), muted, icon = Icons.Rounded.EventBusy)
        }
    }
}

@Composable
private fun HabitToggle(name: String, checked: Boolean, accent: Color, onToggle: ((Boolean) -> Unit)?) {
    if (onToggle == null) return
    val toggleLabel = stringResource(if (checked) R.string.habits_uncheck_today else R.string.habits_check_today, name)
    AnimatedCheck(
        checked = checked,
        color = accent,
        onCheckedChange = onToggle,
        contentDescription = toggleLabel,
        circleSize = 40.dp,
        sparkle = true,
        modifier = Modifier.padding(end = Spacing.sm),
    )
}
