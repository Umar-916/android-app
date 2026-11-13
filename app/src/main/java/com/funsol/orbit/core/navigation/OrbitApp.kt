package com.funsol.orbit.core.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.funsol.orbit.core.designsystem.ConfirmDialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.AnalyticsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.feature.analytics.AnalyticsScreen
import com.funsol.orbit.feature.calendar.CalendarScreen
import com.funsol.orbit.feature.goals.GoalEditScreen
import com.funsol.orbit.feature.goals.GoalsScreen
import com.funsol.orbit.feature.habits.HabitDetailScreen
import com.funsol.orbit.feature.habits.HabitEditScreen
import com.funsol.orbit.feature.habits.HabitsScreen
import com.funsol.orbit.feature.home.HomeScreen
import com.funsol.orbit.feature.more.MoreScreen
import com.funsol.orbit.feature.notes.NoteEditScreen
import com.funsol.orbit.feature.notes.NotesScreen
import com.funsol.orbit.feature.onboarding.LanguageScreen
import com.funsol.orbit.feature.onboarding.OnboardingScreen
import com.funsol.orbit.feature.premium.PremiumScreen
import com.funsol.orbit.feature.settings.FeedbackScreen
import com.funsol.orbit.feature.onboarding.SplashScreen
import com.funsol.orbit.feature.projects.ProjectDetailScreen
import com.funsol.orbit.feature.projects.ProjectEditScreen
import com.funsol.orbit.feature.projects.ProjectsScreen
import com.funsol.orbit.feature.search.SearchScreen
import com.funsol.orbit.feature.settings.AboutScreen
import com.funsol.orbit.feature.settings.BackupScreen
import com.funsol.orbit.feature.settings.DashboardCustomizeScreen
import com.funsol.orbit.feature.settings.PrivacyScreen
import com.funsol.orbit.feature.settings.SettingsScreen
import com.funsol.orbit.feature.tasks.TaskEditScreen
import com.funsol.orbit.feature.tasks.TasksScreen

@Composable
fun OrbitApp() {
    val backStack = rememberNavBackStack(AppDestination.Splash)
    val nav = remember(backStack) { OrbitNavigator(backStack) }
    val current = backStack.lastOrNull()
    val showBottomBar = current != null && current in BOTTOM_TABS
    var showExit by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    val settingsStore = org.koin.compose.koinInject<com.funsol.orbit.core.datastore.SettingsStore>()
    val appSettings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
    val hintScope = rememberCoroutineScope()
    val dismissHint: () -> Unit = { hintScope.launch { settingsStore.setQuickAddHintDismissed(true) } }
    val activity = LocalActivity.current
    val appContext = androidx.compose.ui.platform.LocalContext.current
    BackHandler(enabled = current == AppDestination.Home && !showExit) { showExit = true }
    if (showExit) {
        ExitSheet(onExit = { showExit = false; activity?.finish() }, onStay = { showExit = false })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                androidx.compose.foundation.layout.Column(Modifier.navigationBarsPadding()) {
                    OrbitBottomBar(
                        current = current,
                        onSelect = { nav.switchTab(it) },
                        onQuickAdd = { appContext.postAnalytic(AnalyticsEvents.QUICK_ADD_FAB_CLICK); dismissHint(); showQuickAdd = true },
                        showHint = appSettings?.let { !it.quickAddHintDismissed } == true && !showQuickAdd && !showExit,
                        onDismissHint = dismissHint,
                    )
                    bannerPlacementFor(current)?.let { p ->
                        com.funsol.orbit.core.ads.BannerAdSlot(p)
                    }
                }
            }
        },
    ) { inner ->
        if (showQuickAdd) QuickAddSheet(nav = nav, onDismiss = { showQuickAdd = false })
        val bottom = inner.calculateBottomPadding()
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        NavDisplay(
            backStack = backStack,
            onBack = { nav.back() },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottom)
                .consumeWindowInsets(PaddingValues(bottom = bottom)),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = {
                slideInHorizontally(initialOffsetX = { if (rtl) -it else it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { if (rtl) it / 3 else -it / 3 })
            },
            popTransitionSpec = {
                slideInHorizontally(initialOffsetX = { if (rtl) it / 3 else -it / 3 }) togetherWith
                    slideOutHorizontally(targetOffsetX = { if (rtl) -it else it })
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(initialOffsetX = { if (rtl) it / 3 else -it / 3 }) togetherWith
                    slideOutHorizontally(targetOffsetX = { if (rtl) -it else it })
            },
            entryProvider = { route -> entryFor(route, nav) },
        )
    }
}

private fun entryFor(route: NavKey, nav: OrbitNavigator): NavEntry<NavKey> = when (route) {
    is AppDestination.Splash -> NavEntry(route) { SplashScreen(nav) }
    is AppDestination.Language -> NavEntry(route) { LanguageScreen(route.fromOnboarding, nav) }
    is AppDestination.Onboarding -> NavEntry(route) { OnboardingScreen(route.replay, nav) }
    is AppDestination.Premium -> NavEntry(route) {
        if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) PremiumScreen(route.variant, route.fromOnboarding, nav)
        else androidx.compose.runtime.LaunchedEffect(Unit) { nav.enterHome() }
    }

    is AppDestination.Home -> NavEntry(route) { HomeScreen(nav) }
    is AppDestination.Tasks -> NavEntry(route) { TasksScreen(nav) }
    is AppDestination.Projects -> NavEntry(route) { ProjectsScreen(nav) }
    is AppDestination.Calendar -> NavEntry(route) { CalendarScreen(nav) }
    is AppDestination.More -> NavEntry(route) { MoreScreen(nav) }

    is AppDestination.TaskEdit -> NavEntry(route) { TaskEditScreen(route.taskId, route.presetProjectId, route.presetDate, nav) }
    is AppDestination.ProjectDetail -> NavEntry(route) { ProjectDetailScreen(route.projectId, nav) }
    is AppDestination.ProjectEdit -> NavEntry(route) { ProjectEditScreen(route.projectId, nav) }

    is AppDestination.Notes -> NavEntry(route) { NotesScreen(nav) }
    is AppDestination.NoteEdit -> NavEntry(route) { NoteEditScreen(route.noteId, route.presetFolderId, nav) }
    is AppDestination.Habits -> NavEntry(route) { HabitsScreen(nav) }
    is AppDestination.HabitEdit -> NavEntry(route) { HabitEditScreen(route.habitId, nav) }
    is AppDestination.HabitDetail -> NavEntry(route) { HabitDetailScreen(route.habitId, nav) }
    is AppDestination.Goals -> NavEntry(route) { GoalsScreen(nav) }
    is AppDestination.GoalEdit -> NavEntry(route) { GoalEditScreen(route.goalId, nav) }
    is AppDestination.Analytics -> NavEntry(route) { AnalyticsScreen(nav) }
    is AppDestination.Search -> NavEntry(route) { SearchScreen(nav) }

    is AppDestination.Settings -> NavEntry(route) { SettingsScreen(nav) }
    is AppDestination.DashboardCustomize -> NavEntry(route) { DashboardCustomizeScreen(nav) }
    is AppDestination.Backup -> NavEntry(route) { BackupScreen(nav) }
    is AppDestination.Privacy -> NavEntry(route) { PrivacyScreen(nav) }
    is AppDestination.About -> NavEntry(route) { AboutScreen(nav) }
    is AppDestination.Feedback -> NavEntry(route) { FeedbackScreen(nav) }

    else -> NavEntry(route) { }
}

private fun bannerPlacementFor(current: Any?): com.funsol.orbit.core.ads.BannerPlacement? = when (current) {
    AppDestination.Home -> com.funsol.orbit.core.ads.BannerPlacement.HOME
    AppDestination.Tasks -> com.funsol.orbit.core.ads.BannerPlacement.TASKS
    AppDestination.Calendar -> com.funsol.orbit.core.ads.BannerPlacement.CALENDAR
    AppDestination.More -> com.funsol.orbit.core.ads.BannerPlacement.MORE
    else -> null
}

@Composable
private fun ExitSheet(onExit: () -> Unit, onStay: () -> Unit) {
    PostAnalytic(AnalyticsEvents.EXIT_DIALOG_APPEAR)
    val context = androidx.compose.ui.platform.LocalContext.current
    val maxHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.86f).dp
    com.funsol.orbit.core.designsystem.OrbitSheet(onDismiss = { context.postAnalytic(AnalyticsEvents.EXIT_STAY_CLICK); onStay() }) {
        androidx.compose.foundation.layout.Column(
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Text(
                stringResource(R.string.exit_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Text(
                stringResource(R.string.exit_message),
                style = MaterialTheme.typography.bodyMedium,
                color = com.funsol.orbit.core.designsystem.Orbit.extras.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            com.funsol.orbit.core.ads.NativeAdSlot(
                com.funsol.orbit.core.ads.NativePlacement.EXIT,
                style = com.funsol.orbit.core.ads.NativeAdStyle.Large,
                verticalPadding = 12.dp,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
            com.funsol.orbit.core.designsystem.GradientButton(stringResource(R.string.exit_confirm), { context.postAnalytic(AnalyticsEvents.EXIT_CONFIRM_CLICK); onExit() }, Modifier.fillMaxWidth())
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        }
    }
}
