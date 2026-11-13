package com.funsol.orbit.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.repository.NoteRepository
import com.funsol.orbit.core.repository.ProjectRepository
import com.funsol.orbit.core.repository.SearchRepository
import com.funsol.orbit.core.repository.SearchResults
import com.funsol.orbit.core.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class SearchPhase { IDLE, LOADING, RESULTS, EMPTY, ERROR }

enum class SearchSectionType { TASKS, PROJECTS, NOTES, HABITS, GOALS }

data class SearchSection(val type: SearchSectionType, val results: SearchResults) {
    val count: Int
        get() = when (type) {
            SearchSectionType.TASKS -> results.tasks.size
            SearchSectionType.PROJECTS -> results.projects.size
            SearchSectionType.NOTES -> results.notes.size
            SearchSectionType.HABITS -> results.habits.size
            SearchSectionType.GOALS -> results.goals.size
        }
}

data class SearchUiState(
    val phase: SearchPhase = SearchPhase.IDLE,
    val query: String = "",
    val sections: List<SearchSection> = emptyList(),
) {
    val totalCount: Int get() = sections.sumOf { it.count }
}

data class SearchSuggestion(val text: String, val type: SearchSectionType)

data class SearchSuggestionsState(val loaded: Boolean = false, val items: List<SearchSuggestion> = emptyList())

object SearchLogic {
    const val DEBOUNCE_MILLIS = 300L
    const val MAX_SUGGESTIONS = 6

    fun isSearchable(query: String): Boolean = query.isNotBlank()

    fun sectionsOf(results: SearchResults): List<SearchSection> =
        SearchSectionType.entries.map { SearchSection(it, results) }.filter { it.count > 0 }

    fun suggestions(
        tasks: List<TaskEntity>,
        projects: List<ProjectEntity>,
        notes: List<NoteEntity>,
        max: Int = MAX_SUGGESTIONS,
    ): List<SearchSuggestion> {
        val taskItems = tasks
            .filter { it.status != TaskStatus.ARCHIVED && it.title.isNotBlank() }
            .sortedByDescending { it.updatedAt }
            .map { SearchSuggestion(it.title.trim(), SearchSectionType.TASKS) }
        val projectItems = projects
            .filter { it.status == ProjectStatus.ACTIVE && it.name.isNotBlank() }
            .sortedByDescending { it.updatedAt }
            .map { SearchSuggestion(it.name.trim(), SearchSectionType.PROJECTS) }
        val noteItems = notes
            .filter { !it.archived && it.title.isNotBlank() }
            .sortedByDescending { it.updatedAt }
            .map { SearchSuggestion(it.title.trim(), SearchSectionType.NOTES) }
        val lists = listOf(projectItems, taskItems, noteItems)
        val out = mutableListOf<SearchSuggestion>()
        val seen = HashSet<String>()
        var round = 0
        while (out.size < max && lists.any { round < it.size }) {
            for (list in lists) {
                val item = list.getOrNull(round) ?: continue
                if (out.size < max && seen.add(item.text.lowercase())) out += item
            }
            round++
        }
        return out
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: SearchRepository,
    tasks: TaskRepository,
    projects: ProjectRepository,
    notes: NoteRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val retryTrigger = MutableStateFlow(0)

    val currentQuery: String get() = query.value

    val uiState: StateFlow<SearchUiState> = combine(query, retryTrigger) { q, _ -> q }
        .flatMapLatest { q -> searchFlow(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    val suggestions: StateFlow<SearchSuggestionsState> = combine(
        tasks.observeAll(),
        projects.observeAll(),
        notes.observeAll(),
    ) { t, p, n -> SearchLogic.suggestions(t, p, n) }
        .map { SearchSuggestionsState(loaded = true, items = it) }
        .flowOn(Dispatchers.Default)
        .catch { emit(SearchSuggestionsState(loaded = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchSuggestionsState())

    fun retry() {
        retryTrigger.value += 1
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    private fun searchFlow(q: String): Flow<SearchUiState> = flow {
        if (!SearchLogic.isSearchable(q)) {
            emit(SearchUiState(SearchPhase.IDLE, q))
            return@flow
        }
        emit(SearchUiState(SearchPhase.LOADING, q))
        delay(SearchLogic.DEBOUNCE_MILLIS)
        val sections = try {
            SearchLogic.sectionsOf(repository.search(q))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        emit(
            when {
                sections == null -> SearchUiState(SearchPhase.ERROR, q)
                sections.isEmpty() -> SearchUiState(SearchPhase.EMPTY, q)
                else -> SearchUiState(SearchPhase.RESULTS, q, sections)
            },
        )
    }
}
