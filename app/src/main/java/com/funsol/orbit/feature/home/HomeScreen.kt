package com.funsol.orbit.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.AnalyticsEvents
import com.funsol.orbit.core.analytics.HomeEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.datastore.HomeWidget
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate

@Composable
fun HomeScreen(nav: OrbitNavigator) {
    val viewModel: HomeViewModel = koinViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    PostAnalytic(AnalyticsEvents.HOME_SCREEN_DISPLAY)

    val completedMessage = stringResource(R.string.home_task_completed)
    val failedMessage = stringResource(R.string.error_generic)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeEvent.TaskCompleted -> {
                    val result = snackbar.showSnackbar(completedMessage, undoLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) {
                        context.postAnalytic(HomeEvents.HOME_TASK_UNDO_CLICK)
                        viewModel.reopenTask(event.taskId)
                    }
                }
                HomeEvent.ActionFailed -> snackbar.showSnackbar(failedMessage)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = Spacing.sm)) },
    ) { padding ->
        val today = state.today
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.failed || today == null -> ErrorState(stringResource(R.string.error_generic), Modifier.padding(padding)) {
                context.postAnalytic(HomeEvents.HOME_RETRY_CLICK)
                viewModel.retry()
            }
            else -> HomeContent(state, today, nav, viewModel, padding)
        }
    }
}

private enum class HomeItem { HEADER, CREATE, ALL_HIDDEN, PROGRESS_ONLY }

private const val AD_AFTER_WIDGET_INDEX = 2

@Composable
private fun HomeContent(
    state: HomeUiState,
    today: LocalDate,
    nav: OrbitNavigator,
    vm: HomeViewModel,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val data = state.data
    val order = listOf(
        HomeWidget.OVERDUE, HomeWidget.TODAY, HomeWidget.UPCOMING,
        HomeWidget.HABITS, HomeWidget.PROJECTS, HomeWidget.OVERVIEW,
    )
    // The quick-create grid is always first; below it only sections with real
    // content are rendered, so a new user is not shown five empty cards.
    val visible = if (data.isBlank) {
        emptyList<HomeWidget>()
    } else {
        order.filter { widget ->
            val hasContent = when (widget) {
                HomeWidget.OVERDUE -> data.overdueCount > 0
                HomeWidget.TODAY -> true
                HomeWidget.UPCOMING -> data.upcomingCount > 0
                HomeWidget.HABITS -> data.habits.isNotEmpty()
                HomeWidget.PROJECTS -> data.projects.isNotEmpty()
                HomeWidget.OVERVIEW -> data.overview.openTasks > 0 || data.overview.activeProjects > 0
                HomeWidget.PROGRESS -> false
            }
            state.isVisible(widget) && hasContent
        }
    }
    val progressEnabled = state.isVisible(HomeWidget.PROGRESS)
    val allHidden = !data.isBlank && visible.isEmpty() && !progressEnabled
    val progressOnly = !data.isBlank && visible.isEmpty() && progressEnabled
    // Only the first task card that actually has content gets the attention pulse,
    // so the eye is guided to one place instead of several cards moving at once.
    val attractWidget = visible.firstOrNull { widget ->
        when (widget) {
            HomeWidget.OVERDUE -> data.overdueCount > 0
            HomeWidget.TODAY -> data.todayCount > 0
            HomeWidget.UPCOMING -> data.upcomingCount > 0
            else -> false
        }
    }
    val guard = rememberClickGuard()
    val go: (AppDestination) -> Unit = { destination -> guard.run { nav.navigate(destination) } }
    val goTasks: () -> Unit = { guard.run { nav.switchTab(AppDestination.Tasks) } }
    val openTask: (String) -> Unit = { id -> go(AppDestination.TaskEdit(taskId = id)) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val wash = Orbit.extras.accent(FeatureAccent.HOME)
            .copy(alpha = if (Orbit.extras.isDark) 0.14f else 0.07f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Brush.verticalGradient(listOf(wash, Color.Transparent))),
        )
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = padding.calculateTopPadding() + Spacing.sm,
                bottom = padding.calculateBottomPadding() + HomeBottomSpace,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = HomeItem.HEADER, contentType = HomeItem.HEADER) {
                HomeHero(
                    today = today,
                    progress = data.progress.takeIf { state.isVisible(HomeWidget.PROGRESS) },
                    onSearch = { context.postAnalytic(HomeEvents.HOME_SEARCH_CLICK); go(AppDestination.Search) },
                    onCustomize = { context.postAnalytic(HomeEvents.HOME_CUSTOMIZE_CLICK); go(AppDestination.DashboardCustomize) },
                    // When the dashboard is empty the empty-state card below already offers
                    // "Customize dashboard", so the hero icon would be a duplicate entry.
                    showCustomize = !(allHidden || progressOnly),
                    modifier = Modifier.staggerIn(0),
                )
            }
            item(key = HomeItem.CREATE, contentType = HomeItem.CREATE) {
                HomeCreateGrid(
                    onTask = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_TASK_CLICK); go(AppDestination.TaskEdit()) },
                    onNote = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_NOTE_CLICK); go(AppDestination.NoteEdit()) },
                    onHabit = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_HABIT_CLICK); go(AppDestination.HabitEdit()) },
                    onProject = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_PROJECT_CLICK); go(AppDestination.ProjectEdit()) },
                    onGoal = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_GOAL_CLICK); go(AppDestination.GoalEdit()) },
                    modifier = Modifier.staggerIn(1).animateItem(),
                )
            }
            if (progressOnly) {
                item(key = HomeItem.PROGRESS_ONLY, contentType = HomeItem.PROGRESS_ONLY) {
                    EmptyState(
                        icon = Icons.Rounded.Insights,
                        title = stringResource(R.string.home_progress_only_title),
                        message = stringResource(R.string.home_progress_only_message),
                        actionLabel = stringResource(R.string.home_customize),
                        onAction = { context.postAnalytic(HomeEvents.HOME_ALL_HIDDEN_CUSTOMIZE_CLICK); go(AppDestination.DashboardCustomize) },
                        accent = FeatureAccent.HOME,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (allHidden) {
                item(key = HomeItem.ALL_HIDDEN, contentType = HomeItem.ALL_HIDDEN) {
                    EmptyState(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.home_all_hidden_title),
                        message = stringResource(R.string.home_all_hidden_message),
                        actionLabel = stringResource(R.string.home_customize),
                        onAction = { context.postAnalytic(HomeEvents.HOME_ALL_HIDDEN_CUSTOMIZE_CLICK); go(AppDestination.DashboardCustomize) },
                        accent = FeatureAccent.HOME,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(visible, key = { it }, contentType = { it }) { widget ->
                val index = visible.indexOf(widget)
                val stagger = Modifier
                    .staggerIn(index + 2)
                    .animateItem()
                    .attentionPulse(
                        enabled = widget == attractWidget,
                        delayMillis = 650L,
                        rounds = 2,
                        maxScale = 1.02f,
                    )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.section)) {
                    when (widget) {
                        HomeWidget.OVERDUE -> OverdueCard(
                            tasks = data.overdueTasks,
                            totalCount = data.overdueCount,
                            onToggle = { context.postAnalytic(HomeEvents.HOME_OVERDUE_COMPLETE_CLICK); vm.completeTask(it.id) },
                            onOpen = { context.postAnalytic(HomeEvents.HOME_OVERDUE_TASK_CLICK); openTask(it.id) },
                            onViewAll = { context.postAnalytic(HomeEvents.HOME_OVERDUE_VIEW_ALL_CLICK); goTasks() },
                            modifier = stagger,
                        )
                        HomeWidget.TODAY -> TodayCard(
                            tasks = data.todayTasks,
                            totalCount = data.todayCount,
                            onToggle = { context.postAnalytic(HomeEvents.HOME_TODAY_COMPLETE_CLICK); vm.completeTask(it.id) },
                            onOpen = { context.postAnalytic(HomeEvents.HOME_TODAY_TASK_CLICK); openTask(it.id) },
                            onViewAll = { context.postAnalytic(HomeEvents.HOME_TODAY_VIEW_ALL_CLICK); goTasks() },
                            onAddTask = { context.postAnalytic(HomeEvents.HOME_TODAY_ADD_CLICK); go(AppDestination.TaskEdit()) },
                            modifier = stagger,
                        )
                        HomeWidget.UPCOMING -> UpcomingCard(
                            groups = data.upcomingGroups,
                            totalCount = data.upcomingCount,
                            today = today,
                            onToggle = { context.postAnalytic(HomeEvents.HOME_UPCOMING_COMPLETE_CLICK); vm.completeTask(it.id) },
                            onOpen = { context.postAnalytic(HomeEvents.HOME_UPCOMING_TASK_CLICK); openTask(it.id) },
                            onViewAll = { context.postAnalytic(HomeEvents.HOME_UPCOMING_VIEW_ALL_CLICK); goTasks() },
                            modifier = stagger,
                        )
                        HomeWidget.HABITS -> HabitsCard(
                            habits = data.habits,
                            doneCount = data.habitsDone,
                            onToggle = { item, checked ->
                                context.postAnalytic(HomeEvents.HOME_HABIT_CHECK_TOGGLE)
                                vm.setHabitChecked(item.habit.id, checked)
                            },
                            onNewHabit = { context.postAnalytic(HomeEvents.HOME_HABITS_NEW_CLICK); go(AppDestination.HabitEdit()) },
                            onViewAll = { context.postAnalytic(HomeEvents.HOME_HABITS_VIEW_ALL_CLICK); go(AppDestination.Habits) },
                            modifier = stagger,
                        )
                        HomeWidget.PROJECTS -> ProjectsCard(
                            projects = data.projects,
                            totalActive = data.activeProjectCount,
                            onOpen = { context.postAnalytic(HomeEvents.HOME_PROJECT_CLICK); go(AppDestination.ProjectDetail(it)) },
                            onNewProject = { context.postAnalytic(HomeEvents.HOME_PROJECTS_NEW_CLICK); go(AppDestination.ProjectEdit()) },
                            onViewAll = { context.postAnalytic(HomeEvents.HOME_PROJECTS_VIEW_ALL_CLICK); go(AppDestination.Projects) },
                            modifier = stagger,
                        )
                        HomeWidget.OVERVIEW -> OverviewCard(
                            overview = data.overview,
                            onClick = { context.postAnalytic(HomeEvents.HOME_OVERVIEW_CLICK); go(AppDestination.Analytics) },
                            modifier = stagger,
                        )
                        HomeWidget.PROGRESS -> Unit
                    }
                    if (index == AD_AFTER_WIDGET_INDEX) HomeAdSlot()
                }
            }
        }
    }
}

@Composable
internal fun HomeAdSlot() {
    com.funsol.orbit.core.ads.NativeAdSlot(
        com.funsol.orbit.core.ads.NativePlacement.HOME,
        style = com.funsol.orbit.core.ads.NativeAdStyle.Compact,
    )
}
