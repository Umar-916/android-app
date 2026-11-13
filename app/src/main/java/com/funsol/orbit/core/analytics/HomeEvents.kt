package com.funsol.orbit.core.analytics

/** Analytics events for the Home dashboard. The screen display itself is [AnalyticsEvents.HOME_SCREEN_DISPLAY]. */
object HomeEvents {

    const val HOME_RETRY_CLICK = "home_retry_click"
    const val HOME_SEARCH_CLICK = "home_search_click"
    const val HOME_CUSTOMIZE_CLICK = "home_customize_click"
    const val HOME_TASK_UNDO_CLICK = "home_task_undo_click"

    // Welcome card (blank state)
    const val HOME_WELCOME_TASK_CLICK = "home_welcome_task_click"
    const val HOME_WELCOME_PROJECT_CLICK = "home_welcome_project_click"
    const val HOME_WELCOME_HABIT_CLICK = "home_welcome_habit_click"

    // Quick add row
    const val HOME_QUICK_ADD_TASK_CLICK = "home_quick_add_task_click"
    const val HOME_QUICK_ADD_NOTE_CLICK = "home_quick_add_note_click"
    const val HOME_QUICK_ADD_HABIT_CLICK = "home_quick_add_habit_click"
    const val HOME_QUICK_ADD_PROJECT_CLICK = "home_quick_add_project_click"
    const val HOME_QUICK_ADD_GOAL_CLICK = "home_quick_add_goal_click"

    // All widgets hidden
    const val HOME_ALL_HIDDEN_CUSTOMIZE_CLICK = "home_all_hidden_customize_click"

    // Overdue card
    const val HOME_OVERDUE_TASK_CLICK = "home_overdue_task_click"
    const val HOME_OVERDUE_COMPLETE_CLICK = "home_overdue_complete_click"
    const val HOME_OVERDUE_VIEW_ALL_CLICK = "home_overdue_view_all_click"

    // Today card
    const val HOME_TODAY_TASK_CLICK = "home_today_task_click"
    const val HOME_TODAY_COMPLETE_CLICK = "home_today_complete_click"
    const val HOME_TODAY_VIEW_ALL_CLICK = "home_today_view_all_click"
    const val HOME_TODAY_ADD_CLICK = "home_today_add_click"

    // Upcoming card
    const val HOME_UPCOMING_TASK_CLICK = "home_upcoming_task_click"
    const val HOME_UPCOMING_COMPLETE_CLICK = "home_upcoming_complete_click"
    const val HOME_UPCOMING_VIEW_ALL_CLICK = "home_upcoming_view_all_click"

    // Habits card
    const val HOME_HABIT_CHECK_TOGGLE = "home_habit_check_toggle"
    const val HOME_HABITS_NEW_CLICK = "home_habits_new_click"
    const val HOME_HABITS_VIEW_ALL_CLICK = "home_habits_view_all_click"

    // Projects card
    const val HOME_PROJECT_CLICK = "home_project_click"
    const val HOME_PROJECTS_NEW_CLICK = "home_projects_new_click"
    const val HOME_PROJECTS_VIEW_ALL_CLICK = "home_projects_view_all_click"
}
