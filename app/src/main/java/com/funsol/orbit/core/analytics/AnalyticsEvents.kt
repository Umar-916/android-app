package com.funsol.orbit.core.analytics

object AnalyticsEvents {

    // App lifecycle
    const val SPLASH_SCREEN_DISPLAY = "splash_screen_display"
    const val SESSION_START = "session_start_app"
    const val SESSION_START_WITH_INTERNET_ON = "session_start_with_internet_on"
    const val SPLASH_RETRY_CLICK = "splash_retry_click"
    const val HOME_SCREEN_DISPLAY = "home_screen_display"

    // Bottom navigation
    const val NAV_HOME_CLICK = "nav_home_click"
    const val NAV_TASKS_CLICK = "nav_tasks_click"
    const val NAV_PROJECTS_CLICK = "nav_projects_click"
    const val NAV_CALENDAR_CLICK = "nav_calendar_click"
    const val NAV_MORE_CLICK = "nav_more_click"
    const val NAV_SETTINGS_CLICK = "nav_settings_click"
    const val QUICK_ADD_FAB_CLICK = "quick_add_fab_click"

    const val QUICK_ADD_SHEET_APPEAR = "quick_add_sheet_appear"
    const val QUICK_ADD_TASK_CLICK = "quick_add_task_click"
    const val QUICK_ADD_NOTE_CLICK = "quick_add_note_click"
    const val QUICK_ADD_HABIT_CLICK = "quick_add_habit_click"
    const val QUICK_ADD_PROJECT_CLICK = "quick_add_project_click"
    const val QUICK_ADD_GOAL_CLICK = "quick_add_goal_click"
    const val QUICK_ADD_SEARCH_CLICK = "quick_add_search_click"

    // Exit
    const val EXIT_DIALOG_APPEAR = "exit_dialog_appear"
    const val EXIT_CONFIRM_CLICK = "exit_confirm_click"
    const val EXIT_STAY_CLICK = "exit_stay_click"

    // Back
    const val BACK_CLICK = "back_click"
}
