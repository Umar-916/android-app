package com.funsol.orbit.core.analytics

/** Analytics events for the Habits feature (list, detail, edit). Fixed names only, no user content. */
object HabitsEvents {

    // Habits list
    const val HABITS_SCREEN_DISPLAY = "habits_screen_display"
    const val HABITS_BACK_CLICK = "habits_back_click"
    const val HABITS_ADD_FAB_CLICK = "habits_add_fab_click"
    const val HABITS_EMPTY_ADD_CLICK = "habits_empty_add_click"
    const val HABITS_RETRY_CLICK = "habits_retry_click"
    const val HABITS_CARD_CLICK = "habits_card_click"
    const val HABITS_CHECK_TOGGLE = "habits_check_toggle"
    const val HABITS_ARCHIVED_SECTION_TOGGLE = "habits_archived_section_toggle"

    // Habit detail
    const val HABITS_DETAIL_SCREEN_DISPLAY = "habits_detail_screen_display"
    const val HABITS_DETAIL_BACK_CLICK = "habits_detail_back_click"
    const val HABITS_DETAIL_EDIT_CLICK = "habits_detail_edit_click"
    const val HABITS_DETAIL_RETRY_CLICK = "habits_detail_retry_click"
    const val HABITS_DETAIL_CHECKIN_CLICK = "habits_detail_checkin_click"
    const val HABITS_DETAIL_CAL_PREVIOUS_CLICK = "habits_detail_cal_previous_click"
    const val HABITS_DETAIL_CAL_NEXT_CLICK = "habits_detail_cal_next_click"
    const val HABITS_DETAIL_CAL_DAY_CLICK = "habits_detail_cal_day_click"

    // Habit edit
    const val HABITS_EDIT_SCREEN_DISPLAY = "habits_edit_screen_display"
    const val HABITS_EDIT_BACK_CLICK = "habits_edit_back_click"
    const val HABITS_EDIT_RETRY_CLICK = "habits_edit_retry_click"
    const val HABITS_EDIT_TOP_SAVE_CLICK = "habits_edit_top_save_click"
    const val HABITS_EDIT_SAVE_CLICK = "habits_edit_save_click"
    const val HABITS_EDIT_COLOR_SELECT = "habits_edit_color_select"
    const val HABITS_EDIT_PRESET_SELECT = "habits_edit_preset_select"
    const val HABITS_EDIT_DAY_TOGGLE = "habits_edit_day_toggle"
    const val HABITS_EDIT_START_DATE_SELECT = "habits_edit_start_date_select"
    const val HABITS_EDIT_ARCHIVE_TOGGLE = "habits_edit_archive_toggle"
    const val HABITS_EDIT_DELETE_CLICK = "habits_edit_delete_click"

    // Dialogs
    const val HABITS_DISCARD_DIALOG_APPEAR = "habits_discard_dialog_appear"
    const val HABITS_DISCARD_CONFIRM_CLICK = "habits_discard_confirm_click"
    const val HABITS_DISCARD_KEEP_CLICK = "habits_discard_keep_click"
    const val HABITS_DELETE_DIALOG_APPEAR = "habits_delete_dialog_appear"
    const val HABITS_DELETE_CONFIRM_CLICK = "habits_delete_confirm_click"
    const val HABITS_DELETE_CANCEL_CLICK = "habits_delete_cancel_click"
}
