package com.funsol.orbit.core.analytics

/** Analytics events for the Notes feature (list screen, editor, folder dialogs and sheets). */
object NotesEvents {

    // Notes list screen
    const val NOTES_SCREEN_DISPLAY = "notes_screen_display"
    const val NOTES_BACK_CLICK = "notes_back_click"
    const val NOTES_ARCHIVE_VIEW_CLICK = "notes_archive_view_click"
    const val NOTES_MENU_CLICK = "notes_menu_click"
    const val NOTES_NEW_FAB_CLICK = "notes_new_fab_click"
    const val NOTES_EMPTY_CREATE_CLICK = "notes_empty_create_click"
    const val NOTES_RETRY_CLICK = "notes_retry_click"
    const val NOTES_SEARCH_CLEAR_CLICK = "notes_search_clear_click"
    const val NOTES_FILTER_ALL_SELECT = "notes_filter_all_select"
    const val NOTES_FILTER_NONE_SELECT = "notes_filter_none_select"
    const val NOTES_FILTER_FOLDER_SELECT = "notes_filter_folder_select"
    const val NOTES_UNDO_DELETE_CLICK = "notes_undo_delete_click"

    // Note card
    const val NOTES_CARD_CLICK = "notes_card_click"
    const val NOTES_CARD_OPTIONS_CLICK = "notes_card_options_click"

    // Note actions sheet
    const val NOTES_ACTIONS_SHEET_APPEAR = "notes_actions_sheet_appear"
    const val NOTES_ACTION_PIN_CLICK = "notes_action_pin_click"
    const val NOTES_ACTION_ARCHIVE_CLICK = "notes_action_archive_click"
    const val NOTES_ACTION_MOVE_CLICK = "notes_action_move_click"
    const val NOTES_ACTION_DELETE_CLICK = "notes_action_delete_click"

    // Notes menu sheet
    const val NOTES_MENU_SHEET_APPEAR = "notes_menu_sheet_appear"
    const val NOTES_MENU_NEW_FOLDER_CLICK = "notes_menu_new_folder_click"
    const val NOTES_MENU_RENAME_FOLDER_CLICK = "notes_menu_rename_folder_click"
    const val NOTES_MENU_DELETE_FOLDER_CLICK = "notes_menu_delete_folder_click"

    // Folder dialogs
    const val NOTES_FOLDER_DIALOG_APPEAR = "notes_folder_dialog_appear"
    const val NOTES_FOLDER_CONFIRM_CLICK = "notes_folder_confirm_click"
    const val NOTES_FOLDER_CANCEL_CLICK = "notes_folder_cancel_click"
    const val NOTES_FOLDER_DELETE_DIALOG_APPEAR = "notes_folder_delete_dialog_appear"
    const val NOTES_FOLDER_DELETE_CONFIRM_CLICK = "notes_folder_delete_confirm_click"
    const val NOTES_FOLDER_DELETE_CANCEL_CLICK = "notes_folder_delete_cancel_click"

    // Folder picker
    const val NOTES_PICKER_DIALOG_APPEAR = "notes_picker_dialog_appear"
    const val NOTES_PICKER_SELECT = "notes_picker_select"
    const val NOTES_PICKER_NEW_FOLDER_CLICK = "notes_picker_new_folder_click"
    const val NOTES_PICKER_CANCEL_CLICK = "notes_picker_cancel_click"

    // Note editor
    const val NOTE_EDIT_SCREEN_DISPLAY = "note_edit_screen_display"
    const val NOTE_EDIT_BACK_CLICK = "note_edit_back_click"
    const val NOTE_EDIT_PIN_CLICK = "note_edit_pin_click"
    const val NOTE_EDIT_ARCHIVE_CLICK = "note_edit_archive_click"
    const val NOTE_EDIT_FOLDER_CLICK = "note_edit_folder_click"
    const val NOTE_EDIT_DELETE_CLICK = "note_edit_delete_click"
    const val NOTE_DELETE_DIALOG_APPEAR = "note_delete_dialog_appear"
    const val NOTE_DELETE_CONFIRM_CLICK = "note_delete_confirm_click"
    const val NOTE_DELETE_CANCEL_CLICK = "note_delete_cancel_click"
}
