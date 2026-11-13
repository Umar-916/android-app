package com.funsol.orbit.core.analytics

/** Analytics events for the Settings area: settings home, theme, rate us, feedback, backup, about, privacy. */
object SettingsEvents {

    // Settings home
    const val SETTINGS_SCREEN_DISPLAY = "settings_screen_display"
    const val SETTINGS_PREMIUM_CARD_CLICK = "settings_premium_card_click"
    const val SETTINGS_LANGUAGE_CLICK = "settings_language_click"
    const val SETTINGS_THEME_CLICK = "settings_theme_click"
    const val SETTINGS_NOTIFICATIONS_TOGGLE = "settings_notifications_toggle"
    const val SETTINGS_NOTIFICATIONS_OPEN_CLICK = "settings_notifications_open_click"
    const val SETTINGS_RATE_US_CLICK = "settings_rate_us_click"
    const val SETTINGS_FEEDBACK_CLICK = "settings_feedback_click"
    const val SETTINGS_SHARE_APP_CLICK = "settings_share_app_click"
    const val SETTINGS_BACKUP_CLICK = "settings_backup_click"
    const val SETTINGS_PRIVACY_CLICK = "settings_privacy_click"
    const val SETTINGS_PRIVACY_OPTIONS_CLICK = "settings_privacy_options_click"
    const val SETTINGS_CUSTOMIZE_DASHBOARD_CLICK = "settings_customize_dashboard_click"
    const val SETTINGS_REPLAY_ONBOARDING_CLICK = "settings_replay_onboarding_click"
    const val SETTINGS_ABOUT_CLICK = "settings_about_click"

    // Theme dialog
    const val THEME_DIALOG_APPEAR = "theme_dialog_appear"
    const val THEME_SYSTEM_SELECT = "theme_system_select"
    const val THEME_LIGHT_SELECT = "theme_light_select"
    const val THEME_DARK_SELECT = "theme_dark_select"
    const val THEME_APPLY_CLICK = "theme_apply_click"
    const val THEME_CANCEL_CLICK = "theme_cancel_click"

    // Rate us dialog
    const val RATE_US_DIALOG_APPEAR = "rate_us_dialog_appear"
    const val RATE_US_STAR_1_SELECT = "rate_us_star_1_select"
    const val RATE_US_STAR_2_SELECT = "rate_us_star_2_select"
    const val RATE_US_STAR_3_SELECT = "rate_us_star_3_select"
    const val RATE_US_STAR_4_SELECT = "rate_us_star_4_select"
    const val RATE_US_STAR_5_SELECT = "rate_us_star_5_select"
    const val RATE_US_SUBMIT_CLICK = "rate_us_submit_click"
    const val RATE_US_CLOSE_CLICK = "rate_us_close_click"
    const val RATE_US_LOW_RATING_APPEAR = "rate_us_low_rating_appear"
    const val RATE_US_IMPROVE_CLICK = "rate_us_improve_click"
    const val RATE_US_LOW_RATING_CLOSE_CLICK = "rate_us_low_rating_close_click"

    fun rateStarEvent(stars: Int): String = when (stars) {
        1 -> RATE_US_STAR_1_SELECT
        2 -> RATE_US_STAR_2_SELECT
        3 -> RATE_US_STAR_3_SELECT
        4 -> RATE_US_STAR_4_SELECT
        else -> RATE_US_STAR_5_SELECT
    }

    // Feedback
    const val SETTINGS_BACK_CLICK = "settings_back_click"
    const val ABOUT_BACK_CLICK = "about_back_click"
    const val PRIVACY_BACK_CLICK = "privacy_back_click"
    const val FEEDBACK_BACK_CLICK = "feedback_back_click"
    const val BACKUP_BACK_CLICK = "backup_back_click"
    const val FEEDBACK_SCREEN_DISPLAY = "feedback_screen_display"
    const val FEEDBACK_ISSUES_TAB_SELECT = "feedback_issues_tab_select"
    const val FEEDBACK_SUGGESTIONS_TAB_SELECT = "feedback_suggestions_tab_select"
    const val FEEDBACK_TOPIC_TOGGLE = "feedback_topic_toggle"
    const val FEEDBACK_SUBMIT_CLICK = "feedback_submit_click"

    // Backup
    const val BACKUP_SCREEN_DISPLAY = "backup_screen_display"
    const val BACKUP_EXPORT_CLICK = "backup_export_click"
    const val BACKUP_RESTORE_CLICK = "backup_restore_click"
    const val BACKUP_CSV_TASKS_CLICK = "backup_csv_tasks_click"
    const val BACKUP_CSV_HABITS_CLICK = "backup_csv_habits_click"
    const val BACKUP_RESULT_CLOSE_CLICK = "backup_result_close_click"
    const val BACKUP_RESTORE_DIALOG_APPEAR = "backup_restore_dialog_appear"
    const val BACKUP_RESTORE_CONFIRM_CLICK = "backup_restore_confirm_click"
    const val BACKUP_RESTORE_CANCEL_CLICK = "backup_restore_cancel_click"

    // About / Privacy
    const val ABOUT_SCREEN_DISPLAY = "about_screen_display"
    const val ABOUT_PRIVACY_CLICK = "about_privacy_click"
    const val PRIVACY_SCREEN_DISPLAY = "privacy_screen_display"
}
