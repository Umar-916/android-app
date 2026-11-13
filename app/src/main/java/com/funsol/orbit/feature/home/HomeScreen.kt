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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
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

    val completedMessage = stringResource(R.string.home_task_completed)
    val failedMessage = stringResource(R.string.error_generic)
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeEvent.TaskCompleted -> {
                    val result = snackbar.showSnackbar(completedMessage, undoLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.reopenTask(event.taskId)
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
            state.failed || today == null -> ErrorState(stringResource(R.string.error_generic), Modifier.padding(padding), viewModel::retry)
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
    val allHidden = state.hiddenWidgets.size >= HomeWidget.entries.size
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
                    onSearch = { go(AppDestination.Search) },
                    onCustomize = { go(AppDestination.DashboardCustomize) },
                    modifier = Modifier.staggerIn(0),
                )
            }
            if (data.isBlank) {
                item(key = HomeItem.WELCOME, contentType = HomeItem.WELCOME) {
                    WelcomeCard(
                        onTask = { go(AppDestination.TaskEdit()) },
                        onProject = { go(AppDestination.ProjectEdit()) },
                        onHabit = { go(AppDestination.HabitEdit()) },
                        modifier = Modifier.staggerIn(1),
                    )
                }
            }
            item(key = HomeItem.QUICK_ADD, contentType = HomeItem.QUICK_ADD) {
                QuickAddRow(
                    onTask = { go(AppDestination.TaskEdit()) },
                    onNote = { go(AppDestination.NoteEdit()) },
                    onHabit = { go(AppDestination.HabitEdit()) },
                    onProject = { go(AppDestination.ProjectEdit()) },
                    onGoal = { go(AppDestination.GoalEdit()) },
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
                        onAction = { go(AppDestination.DashboardCustomize) },
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
                            onToggle = { vm.completeTask(it.id) },
                            onOpen = { openTask(it.id) },
                            onViewAll = goTasks,
                            modifier = stagger,
                        )
                        HomeWidget.TODAY -> TodayCard(
                            tasks = data.todayTasks,
                            totalCount = data.todayCount,
                            onToggle = { vm.completeTask(it.id) },
                            onOpen = { openTask(it.id) },
                            onViewAll = goTasks,
                            onAddTask = { go(AppDestination.TaskEdit()) },
                            modifier = stagger,
                        )
                        HomeWidget.UPCOMING -> UpcomingCard(
                            groups = data.upcomingGroups,
                            totalCount = data.upcomingCount,
                            today = today,
                            onToggle = { vm.completeTask(it.id) },
                            onOpen = { openTask(it.id) },
                            onViewAll = goTasks,
                            modifier = stagger,
                        )
                        HomeWidget.HABITS -> HabitsCard(
                            habits = data.habits,
                            doneCount = data.habitsDone,
                            onToggle = { item, checked -> vm.setHabitChecked(item.habit.id, checked) },
                            onNewHabit = { go(AppDestination.HabitEdit()) },
                            onViewAll = { go(AppDestination.Habits) },
                            modifier = stagger,
                        )
                        HomeWidget.PROJECTS -> ProjectsCard(
                            projects = data.projects,
                            totalActive = data.activeProjectCount,
                            onOpen = { go(AppDestination.ProjectDetail(it)) },
                            onNewProject = { go(AppDestination.ProjectEdit()) },
                            onViewAll = { go(AppDestination.Projects) },
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
