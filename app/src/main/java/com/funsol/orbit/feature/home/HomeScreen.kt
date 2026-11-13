package com.funsol.orbit.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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

private enum class HomeItem { HEADER, WELCOME, ALL_HIDDEN, QUICK_ADD }

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
    val visible = order.filter { state.isVisible(it) && (it != HomeWidget.OVERDUE || data.overdueCount > 0) }
    val guard = rememberClickGuard()
    val go: (AppDestination) -> Unit = { destination -> guard.run { nav.navigate(destination) } }
    val goTasks: () -> Unit = { guard.run { nav.switchTab(AppDestination.Tasks) } }
    val openTask: (String) -> Unit = { id -> go(AppDestination.TaskEdit(taskId = id)) }
    val allHidden = visible.isEmpty()
    val welcomeShift = if (data.isBlank) 1 else 0

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = padding.calculateTopPadding() + Spacing.sm,
                bottom = padding.calculateBottomPadding() + HomeBottomSpace,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item(key = HomeItem.HEADER, contentType = HomeItem.HEADER) {
                HomeHero(
                    today = today,
                    progress = data.progress.takeIf { state.isVisible(HomeWidget.PROGRESS) },
                    onSearch = { context.postAnalytic(HomeEvents.HOME_SEARCH_CLICK); go(AppDestination.Search) },
                    onCustomize = { context.postAnalytic(HomeEvents.HOME_CUSTOMIZE_CLICK); go(AppDestination.DashboardCustomize) },
                    modifier = Modifier.staggerIn(0),
                )
            }
            if (data.isBlank) {
                item(key = HomeItem.WELCOME, contentType = HomeItem.WELCOME) {
                    WelcomeCard(
                        onTask = { context.postAnalytic(HomeEvents.HOME_WELCOME_TASK_CLICK); go(AppDestination.TaskEdit()) },
                        onProject = { context.postAnalytic(HomeEvents.HOME_WELCOME_PROJECT_CLICK); go(AppDestination.ProjectEdit()) },
                        onHabit = { context.postAnalytic(HomeEvents.HOME_WELCOME_HABIT_CLICK); go(AppDestination.HabitEdit()) },
                        modifier = Modifier.staggerIn(1),
                    )
                }
            }
            item(key = HomeItem.QUICK_ADD, contentType = HomeItem.QUICK_ADD) {
                QuickAddRow(
                    onTask = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_TASK_CLICK); go(AppDestination.TaskEdit()) },
                    onNote = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_NOTE_CLICK); go(AppDestination.NoteEdit()) },
                    onHabit = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_HABIT_CLICK); go(AppDestination.HabitEdit()) },
                    onProject = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_PROJECT_CLICK); go(AppDestination.ProjectEdit()) },
                    onGoal = { context.postAnalytic(HomeEvents.HOME_QUICK_ADD_GOAL_CLICK); go(AppDestination.GoalEdit()) },
                    modifier = Modifier.staggerIn(1 + welcomeShift),
                )
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
                    )
                }
            }
            items(visible, key = { it }, contentType = { it }) { widget ->
                val index = visible.indexOf(widget)
                val stagger = Modifier.staggerIn(index + 2 + welcomeShift)
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
                        HomeWidget.OVERVIEW -> OverviewCard(data.overview, stagger)
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
