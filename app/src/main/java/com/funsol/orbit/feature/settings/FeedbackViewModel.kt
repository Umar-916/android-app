package com.funsol.orbit.feature.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.funsol.orbit.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FeedbackTab { ISSUES, SUGGESTIONS }

enum class FeedbackItem(val tab: FeedbackTab, @StringRes val label: Int) {
    ISSUE_CRASH(FeedbackTab.ISSUES, R.string.feedback_issue_crash),
    ISSUE_REMINDERS(FeedbackTab.ISSUES, R.string.feedback_issue_reminders),
    ISSUE_NOTES(FeedbackTab.ISSUES, R.string.feedback_issue_notes),
    ISSUE_TASKS(FeedbackTab.ISSUES, R.string.feedback_issue_tasks),
    ISSUE_CALENDAR(FeedbackTab.ISSUES, R.string.feedback_issue_calendar),
    ISSUE_BACKUP(FeedbackTab.ISSUES, R.string.feedback_issue_backup),
    ISSUE_LANGUAGE(FeedbackTab.ISSUES, R.string.feedback_issue_language),
    ISSUE_OTHER(FeedbackTab.ISSUES, R.string.feedback_item_other),

    IDEA_FEATURE(FeedbackTab.SUGGESTIONS, R.string.feedback_idea_feature),
    IDEA_DESIGN(FeedbackTab.SUGGESTIONS, R.string.feedback_idea_design),
    IDEA_REMINDERS(FeedbackTab.SUGGESTIONS, R.string.feedback_idea_reminders),
    IDEA_TASKS(FeedbackTab.SUGGESTIONS, R.string.feedback_idea_tasks),
    IDEA_NOTES(FeedbackTab.SUGGESTIONS, R.string.feedback_idea_notes),
    IDEA_HABITS(FeedbackTab.SUGGESTIONS, R.string.feedback_idea_habits),
    IDEA_THEMES(FeedbackTab.SUGGESTIONS, R.string.feedback_idea_themes),
    IDEA_OTHER(FeedbackTab.SUGGESTIONS, R.string.feedback_item_other),
}

object FeedbackRules {
    const val MAX_SELECTION = 3
    const val MAX_TEXT_LENGTH = 500

    fun itemsFor(tab: FeedbackTab): List<FeedbackItem> = FeedbackItem.entries.filter { it.tab == tab }

    fun toggle(selected: Set<FeedbackItem>, item: FeedbackItem): Set<FeedbackItem>? = when {
        item in selected -> selected - item
        selected.size >= MAX_SELECTION -> null
        else -> selected + item
    }

    fun clampText(text: String): String = text.take(MAX_TEXT_LENGTH)

    fun canSubmit(selected: Set<FeedbackItem>, text: String): Boolean = selected.isNotEmpty() || text.isNotBlank()

    fun buildBody(userText: String, itemsLine: String?, footerLines: List<String>): String = buildString {
        val trimmed = userText.trim()
        if (trimmed.isNotEmpty()) {
            append(trimmed)
            append("\n\n")
        }
        if (!itemsLine.isNullOrBlank()) {
            append(itemsLine)
            append("\n\n")
        }
        append("---\n")
        append(footerLines.joinToString("\n"))
    }
}

data class FeedbackUiState(
    val tab: FeedbackTab = FeedbackTab.ISSUES,
    val selected: Set<FeedbackItem> = emptySet(),
    val text: String = "",
    val limitHits: Int = 0,
) {
    val items: List<FeedbackItem> get() = FeedbackRules.itemsFor(tab)
    val canSubmit: Boolean get() = FeedbackRules.canSubmit(selected, text)
}

class FeedbackViewModel : ViewModel() {
    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    fun selectTab(tab: FeedbackTab) {
        _state.update { if (it.tab == tab) it else FeedbackUiState(tab = tab, limitHits = it.limitHits) }
    }

    fun toggle(item: FeedbackItem) {
        _state.update { current ->
            val next = FeedbackRules.toggle(current.selected, item)
            if (next == null) current.copy(limitHits = current.limitHits + 1) else current.copy(selected = next)
        }
    }

    fun setText(text: String) {
        _state.update { it.copy(text = FeedbackRules.clampText(text)) }
    }
}
