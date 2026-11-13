package com.funsol.orbit.core.analytics

/** Analytics events for the Projects feature (list, detail, edit, delete) and its shared planning widgets. */
object ProjectsEvents {

    // Projects list
    const val PROJECTS_SCREEN_DISPLAY = "projects_screen_display"
    const val PROJECTS_BACK_CLICK = "projects_back_click"
    const val PROJECTS_FILTER_SELECT = "projects_filter_select"
    const val PROJECTS_ITEM_CLICK = "projects_item_click"
    const val PROJECTS_NEW_CLICK = "projects_new_click"
    const val PROJECTS_RETRY_CLICK = "projects_retry_click"

    // Project detail
    const val PROJECT_DETAIL_SCREEN_DISPLAY = "project_detail_screen_display"
    const val PROJECT_DETAIL_BACK_CLICK = "project_detail_back_click"
    const val PROJECT_DETAIL_MENU_CLICK = "project_detail_menu_click"
    const val PROJECT_DETAIL_EDIT_CLICK = "project_detail_edit_click"
    const val PROJECT_DETAIL_ARCHIVE_CLICK = "project_detail_archive_click"
    const val PROJECT_DETAIL_RESTORE_CLICK = "project_detail_restore_click"
    const val PROJECT_DETAIL_COMPLETE_CLICK = "project_detail_complete_click"
    const val PROJECT_DETAIL_REOPEN_CLICK = "project_detail_reopen_click"
    const val PROJECT_DETAIL_DELETE_CLICK = "project_detail_delete_click"
    const val PROJECT_DETAIL_ADD_TASK_CLICK = "project_detail_add_task_click"
    const val PROJECT_DETAIL_TASK_CLICK = "project_detail_task_click"
    const val PROJECT_DETAIL_TASK_TOGGLE = "project_detail_task_toggle"
    const val PROJECT_DETAIL_RETRY_CLICK = "project_detail_retry_click"

    // Delete project sheet / dialog
    const val PROJECT_DELETE_DIALOG_APPEAR = "project_delete_dialog_appear"
    const val PROJECT_DELETE_SHEET_APPEAR = "project_delete_sheet_appear"
    const val PROJECT_DELETE_KEEP_SELECT = "project_delete_keep_select"
    const val PROJECT_DELETE_ALL_SELECT = "project_delete_all_select"
    const val PROJECT_DELETE_CANCEL_CLICK = "project_delete_cancel_click"
    const val PROJECT_DELETE_CONFIRM_CLICK = "project_delete_confirm_click"

    // Project edit
    const val PROJECT_EDIT_SCREEN_DISPLAY = "project_edit_screen_display"
    const val PROJECT_EDIT_BACK_CLICK = "project_edit_back_click"
    const val PROJECT_EDIT_SAVE_CLICK = "project_edit_save_click"
    const val PROJECT_EDIT_COLOR_SELECT = "project_edit_color_select"
    const val PROJECT_EDIT_STATUS_SELECT = "project_edit_status_select"

    // Shared planning widgets (also used by tasks / goals / habits forms)
    const val PLANNING_DATE_FIELD_CLICK = "planning_date_field_click"
    const val PLANNING_DATE_CLEAR_CLICK = "planning_date_clear_click"
    const val PLANNING_DATE_SHEET_APPEAR = "planning_date_sheet_appear"
    const val PLANNING_DATE_CANCEL_CLICK = "planning_date_cancel_click"
    const val PLANNING_DATE_DONE_CLICK = "planning_date_done_click"
    const val PLANNING_DISCARD_DIALOG_APPEAR = "planning_discard_dialog_appear"
    const val PLANNING_DISCARD_CONFIRM_CLICK = "planning_discard_confirm_click"
    const val PLANNING_DISCARD_KEEP_CLICK = "planning_discard_keep_click"
}
