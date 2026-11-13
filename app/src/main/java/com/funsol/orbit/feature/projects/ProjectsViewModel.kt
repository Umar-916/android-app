package com.funsol.orbit.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.repository.ProjectRepository
import com.funsol.orbit.core.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ProjectsUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val filter: ProjectFilter = ProjectFilter.ACTIVE,
    val items: List<ProjectListItem> = emptyList(),
    val totalProjects: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModel(
    projects: ProjectRepository,
    tasks: TaskRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(ProjectFilter.ACTIVE)
    private val reload = MutableStateFlow(0)

    val state: StateFlow<ProjectsUiState> = reload.flatMapLatest {
        combine(projects.observeAll(), tasks.observeAll(), filter, todayFlow()) { p, t, f, today ->
            ProjectsUiState(
                loading = false,
                filter = f,
                items = ProjectsLogic.buildItems(p, t, f, today),
                totalProjects = p.size,
            )
        }.catch { emit(ProjectsUiState(loading = false, error = true, filter = filter.value)) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectsUiState())

    fun setFilter(value: ProjectFilter) = filter.update { value }

    fun retry() = reload.update { it + 1 }
}
