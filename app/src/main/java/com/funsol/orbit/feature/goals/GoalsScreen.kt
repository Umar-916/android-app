package com.funsol.orbit.feature.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.analytics.GoalsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.ClickGuard
import com.funsol.orbit.core.common.GoalMath
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientFab
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.ProgressRing
import com.funsol.orbit.core.designsystem.SectionHeader
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.feature.projects.CenteredContent
import com.funsol.orbit.feature.projects.GradientHero
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GoalsScreen(nav: OrbitNavigator) {
    PostAnalytic(GoalsEvents.GOALS_SCREEN_DISPLAY)
    val context = LocalContext.current
    val vm: GoalsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var progressGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    val allGoals = state.sections.inProgress + state.sections.completed + state.sections.archived
    val progressGoal = allGoals.firstOrNull { it.id == progressGoalId }
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

    // Empty state carries its own "New goal" action, so suppress the duplicate FAB there.
    val emptyActionHandledInState = remember(state.loading, state.error, state.isEmpty) {
        !state.loading && !state.error && state.isEmpty
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            OrbitTopBar(
                scrolled = scrolled,
                title = stringResource(R.string.goals_title),
                onBack = {
                    context.postAnalytic(GoalsEvents.GOALS_BACK_CLICK)
                    guard.run(nav::back)
                },
                accent = FeatureAccent.GOALS,
                icon = Icons.Rounded.EmojiEvents,
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
                    contentDescription = stringResource(R.string.goals_new),
                    onClick = {
                        context.postAnalytic(GoalsEvents.GOALS_ADD_FAB_CLICK)
                        guard.run { nav.navigate(AppDestination.GoalEdit()) }
                    },
                    brush = Orbit.extras.accentGradient(FeatureAccent.GOALS),
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
                        context.postAnalytic(GoalsEvents.GOALS_RETRY_CLICK)
                        vm.retry()
                    },
                )
                state.isEmpty -> EmptyState(
                    icon = Icons.Rounded.EmojiEvents,
                    title = stringResource(R.string.goals_empty_title),
                    message = stringResource(R.string.goals_empty_message),
                    actionLabel = stringResource(R.string.goals_new),
                    onAction = {
                        context.postAnalytic(GoalsEvents.GOALS_EMPTY_ADD_CLICK)
                        guard.run { nav.navigate(AppDestination.GoalEdit()) }
                    },
                    accent = FeatureAccent.GOALS,
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    val sections = state.sections
                    item(key = "hero", contentType = "hero") {
                        GoalsHero(sections.inProgress, sections.completed.size)
                    }
                    item(key = "h_progress", contentType = "header") {
                        SectionHeader(stringResource(R.string.goals_section_in_progress), Modifier.padding(top = Spacing.sm))
                    }
                    if (sections.inProgress.isEmpty()) {
                        item(key = "empty_progress", contentType = "empty") {
                            Text(
                                stringResource(R.string.goals_section_in_progress_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Orbit.extras.textMuted,
                                modifier = Modifier.padding(bottom = Spacing.xs),
                            )
                        }
                    }
                    goalItems(sections.inProgress, state.todayEpochDay, nav, guard, onUpdate = { progressGoalId = it })
                    if (sections.completed.isNotEmpty()) {
                        item(key = "h_completed", contentType = "header") {
                            SectionHeader(stringResource(R.string.goals_section_completed), Modifier.padding(top = Spacing.sm))
                        }
                        goalItems(sections.completed, state.todayEpochDay, nav, guard, onUpdate = {})
                    }
                    if (sections.archived.isNotEmpty()) {
                        item(key = "archived_toggle", contentType = "toggle") {
                            ArchivedToggle(sections.archived.size, showArchived, Modifier.padding(top = Spacing.sm)) {
                                context.postAnalytic(GoalsEvents.GOALS_ARCHIVED_SECTION_TOGGLE)
                                showArchived = !showArchived
                            }
                        }
                        if (showArchived) goalItems(sections.archived, state.todayEpochDay, nav, guard, onUpdate = {})
                    }
                }
            }
        }
    }

    progressGoal?.let { goal ->
        GoalProgressDialog(
            goal = goal,
            onSubmit = { value -> vm.updateProgress(goal.id, value); progressGoalId = null },
            onDismiss = { progressGoalId = null },
        )
    }
}

private fun LazyListScope.goalItems(
    goals: List<GoalEntity>,
    todayEpochDay: Int,
    nav: OrbitNavigator,
    guard: ClickGuard,
    onUpdate: (String) -> Unit,
) {
    items(goals.size, key = { goals[it].id }, contentType = { "goal" }) { index ->
        val context = LocalContext.current
        val goal = goals[index]
        GoalCard(
            goal = goal,
            todayEpochDay = todayEpochDay,
            onClick = {
                context.postAnalytic(GoalsEvents.GOALS_CARD_CLICK)
                guard.run { nav.navigate(AppDestination.GoalEdit(goal.id)) }
            },
            onUpdate = {
                context.postAnalytic(GoalsEvents.GOALS_UPDATE_PROGRESS_CLICK)
                onUpdate(goal.id)
            },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun GoalsHero(inProgress: List<GoalEntity>, completedCount: Int) {
    val average = if (inProgress.isEmpty()) 0f
    else inProgress.map { GoalMath.fraction(it.currentValue, it.targetValue) }.average().toFloat().coerceIn(0f, 1f)
    val percent = (average * 100).toInt()
    val description = stringResource(R.string.goals_progress_desc, percent)
    GradientHero(
        brush = Orbit.extras.accentGradient(FeatureAccent.GOALS),
        glow = Orbit.extras.accent(FeatureAccent.GOALS),
        modifier = Modifier.animateContentSize(),
    ) {
        val stacked = isLargeFontScale()
        val texts: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    if (inProgress.isEmpty()) stringResource(R.string.goals_hero_none_active)
                    else pluralStringResource(R.plurals.goals_hero_active, inProgress.size, inProgress.size),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    pluralStringResource(R.plurals.goals_hero_completed, completedCount, completedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
        val ring: @Composable () -> Unit = {
            if (inProgress.isNotEmpty()) {
                val ringBox = 88.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
                Box(
                    Modifier.size(ringBox).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    ProgressRing(
                        fraction = average,
                        label = stringResource(R.string.goals_percent, percent),
                        size = ringBox - 14.dp,
                        color = Orbit.extras.accentText(FeatureAccent.GOALS),
                        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                    )
                }
            }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                texts(Modifier.fillMaxWidth())
                ring()
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                texts(Modifier.weight(1f))
                ring()
            }
        }
    }
}

@Composable
private fun ArchivedToggle(count: Int, expanded: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "archivedChevron")
    OrbitWell(
        modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                pluralStringResource(
                    if (expanded) R.plurals.goals_hide_archived else R.plurals.goals_show_archived,
                    count,
                    count,
                ),
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
