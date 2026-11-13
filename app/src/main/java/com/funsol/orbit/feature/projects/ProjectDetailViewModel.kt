package com.funsol.orbit.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.launchSafely
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.database.entity.TaskEntity
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val project: ProjectEntity? = null,
    val tasks: List<TaskEntity> = emptyList(),
    val stats: ProjectStats = ProjectStats(0, 0, 0, 0, 0, 0),
    val linkedTaskCount: Int = 0,
    val todayEpochDay: Int = 0,
) {
    val notFound: Boolean get() = !loading && !error && project == null
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModel(
    private val projectId: String,
    private val projects: ProjectRepository,
    private val tasks: TaskRepository,
) : ViewModel() {
    private val deleted = MutableStateFlow(false)

    val finished: StateFlow<Boolean> = deleted

    private val reload = MutableStateFlow(0)

    val state: StateFlow<ProjectDetailUiState> = reload.flatMapLatest {
        combine(
            projects.observe(projectId),
            tasks.observeByProject(projectId),
            todayFlow(),
        ) { project, taskList, today ->
            ProjectDetailUiState(
                loading = false,
                project = project,
                tasks = ProjectsLogic.sortTasks(taskList),
                stats = ProjectsLogic.stats(taskList, today),
                linkedTaskCount = taskList.size,
                todayEpochDay = today.toEpochDayInt(),
            )
        }.catch { emit(ProjectDetailUiState(loading = false, error = true)) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectDetailUiState())

    fun retry() = reload.update { it + 1 }

    fun setTaskCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launchSafely { tasks.setCompleted(taskId, completed) }
    }

    fun setStatus(status: ProjectStatus) {
        viewModelScope.launchSafely { projects.setStatus(projectId, status) }
    }

    private var deleting = false

    fun delete(deleteTasks: Boolean) {
        if (deleting || deleted.value) return
        deleting = true
        viewModelScope.launch {
            try {
                projects.delete(projectId, deleteTasks)
                deleted.value = true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Throwable) {
                deleting = false
            }
        }
    }
}
