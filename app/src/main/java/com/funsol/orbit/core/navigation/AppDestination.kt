package com.funsol.orbit.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class AppDestination : NavKey {
    @Serializable data object Splash : AppDestination()
    @Serializable data class Language(val fromOnboarding: Boolean = false) : AppDestination()
    @Serializable data class Onboarding(val replay: Boolean = false) : AppDestination()
    @Serializable data class Premium(val variant: Int = 1, val fromOnboarding: Boolean = false) : AppDestination()

    @Serializable data object Home : AppDestination()
    @Serializable data object Tasks : AppDestination()
    @Serializable data object Projects : AppDestination()
    @Serializable data object Calendar : AppDestination()
    @Serializable data object More : AppDestination()

    @Serializable data class TaskEdit(
        val taskId: String? = null,
        val presetProjectId: String? = null,
        val presetDate: Int? = null,
    ) : AppDestination()

    @Serializable data class ProjectDetail(val projectId: String) : AppDestination()
    @Serializable data class ProjectEdit(val projectId: String? = null) : AppDestination()

    @Serializable data object Notes : AppDestination()
    @Serializable data class NoteEdit(val noteId: String? = null, val presetFolderId: String? = null) : AppDestination()
    @Serializable data object Habits : AppDestination()
    @Serializable data class HabitEdit(val habitId: String? = null) : AppDestination()
    @Serializable data class HabitDetail(val habitId: String) : AppDestination()
    @Serializable data object Goals : AppDestination()
    @Serializable data class GoalEdit(val goalId: String? = null) : AppDestination()
    @Serializable data object Analytics : AppDestination()
    @Serializable data object Search : AppDestination()

    @Serializable data object Settings : AppDestination()
    @Serializable data object DashboardCustomize : AppDestination()
    @Serializable data object Backup : AppDestination()
    @Serializable data object Privacy : AppDestination()
    @Serializable data object About : AppDestination()
    @Serializable data object Feedback : AppDestination()
}

val BOTTOM_TABS: List<AppDestination> = listOf(
    AppDestination.Home,
    AppDestination.Tasks,
    AppDestination.Calendar,
    AppDestination.More,
)

class OrbitNavigator(private val backStack: NavBackStack<NavKey>) {
    val current: NavKey? get() = backStack.lastOrNull()

    val isTab: Boolean get() = current in BOTTOM_TABS

    fun navigate(destination: AppDestination) {
        if (backStack.lastOrNull() == destination) return
        backStack.add(destination)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun replaceTop(destination: AppDestination) {
        backStack.removeLastOrNull()
        backStack.add(destination)
    }

    fun switchTab(tab: AppDestination) {
        if (backStack.lastOrNull() == tab) return
        backStack.clear()
        backStack.add(AppDestination.Home)
        if (tab != AppDestination.Home) backStack.add(tab)
    }

    fun enterHome() {
        backStack.clear()
        backStack.add(AppDestination.Home)
    }
}
