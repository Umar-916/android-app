package com.funsol.orbit.core.analytics

/** Analytics events for the Goals feature (list, edit, progress sheet). Fixed names only, no user content. */
object GoalsEvents {

    // Goals list
    const val GOALS_SCREEN_DISPLAY = "goals_screen_display"
    const val GOALS_BACK_CLICK = "goals_back_click"
    const val GOALS_ADD_FAB_CLICK = "goals_add_fab_click"
    const val GOALS_EMPTY_ADD_CLICK = "goals_empty_add_click"
    const val GOALS_RETRY_CLICK = "goals_retry_click"
    const val GOALS_CARD_CLICK = "goals_card_click"
    const val GOALS_UPDATE_PROGRESS_CLICK = "goals_update_progress_click"
    const val GOALS_ARCHIVED_SECTION_TOGGLE = "goals_archived_section_toggle"

    // Progress sheet
    const val GOALS_PROGRESS_SHEET_APPEAR = "goals_progress_sheet_appear"
    const val GOALS_PROGRESS_MODE_SET_SELECT = "goals_progress_mode_set_select"
    const val GOALS_PROGRESS_MODE_ADD_SELECT = "goals_progress_mode_add_select"
    const val GOALS_PROGRESS_CANCEL_CLICK = "goals_progress_cancel_click"
    const val GOALS_PROGRESS_CONFIRM_CLICK = "goals_progress_confirm_click"

    // Goal edit
    const val GOALS_EDIT_SCREEN_DISPLAY = "goals_edit_screen_display"
    const val GOALS_EDIT_BACK_CLICK = "goals_edit_back_click"
    const val GOALS_EDIT_RETRY_CLICK = "goals_edit_retry_click"
    const val GOALS_EDIT_TOP_SAVE_CLICK = "goals_edit_top_save_click"
    const val GOALS_EDIT_SAVE_CLICK = "goals_edit_save_click"
    const val GOALS_EDIT_START_DATE_SELECT = "goals_edit_start_date_select"
    const val GOALS_EDIT_TARGET_DATE_SELECT = "goals_edit_target_date_select"
    const val GOALS_EDIT_STATUS_SELECT = "goals_edit_status_select"
    const val GOALS_EDIT_DELETE_CLICK = "goals_edit_delete_click"

    // Dialogs
    const val GOALS_DISCARD_DIALOG_APPEAR = "goals_discard_dialog_appear"
    const val GOALS_DISCARD_CONFIRM_CLICK = "goals_discard_confirm_click"
    const val GOALS_DISCARD_KEEP_CLICK = "goals_discard_keep_click"
    const val GOALS_DELETE_DIALOG_APPEAR = "goals_delete_dialog_appear"
    const val GOALS_DELETE_CONFIRM_CLICK = "goals_delete_confirm_click"
    const val GOALS_DELETE_CANCEL_CLICK = "goals_delete_cancel_click"
}
