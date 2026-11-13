package com.funsol.orbit.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.repository.ProjectRepository
import com.funsol.orbit.core.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TasksUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val items: List<TaskItem> = emptyList(),
    val counts: Map<TaskView, Int> = emptyMap(),
    val view: TaskView = TaskView.ALL,
    val filters: TaskFilters = TaskFilters(),
    val sort: TaskSort = TaskSort(),
    val today: LocalDate = LocalDate.ofEpochDay(0),
    val projects: List<ProjectEntity> = emptyList(),
    val totalTasks: Int = 0,
    val hasQuery: Boolean = false,
)

sealed interface TasksEvent {
    data class Deleted(val task: TaskEntity, val subtasks: List<SubtaskEntity>) : TasksEvent
    data class Archived(val id: String, val previous: TaskStatus) : TasksEvent
    data object Failed : TasksEvent
}

private data class Controls(val view: TaskView, val filters: TaskFilters, val sort: TaskSort, val query: String)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TasksViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
) : ViewModel() {
    private val view = MutableStateFlow(TaskView.ALL)
    private val filters = MutableStateFlow(TaskFilters())
    private val sort = MutableStateFlow(TaskSort())
    private val _query = MutableStateFlow("")
    private val retry = MutableStateFlow(0)

    val query: StateFlow<String> = _query

    private val _events = Channel<TasksEvent>(Channel.BUFFERED)
    val events: Flow<TasksEvent> = _events.receiveAsFlow()

    private val controls: Flow<Controls> = combine(
        view,
        filters,
        sort,
        _query.debounce { if (it.isBlank()) 0L else 250L },
    ) { v, f, s, q -> Controls(v, f, s, q) }

    val uiState: StateFlow<TasksUiState> = retry.flatMapLatest {
        combine(
            tasks.observeAll(),
            tasks.observeAllSubtasks(),
            projects.observeAll(),
            todayFlow(),
            controls,
        ) { t, s, p, today, c -> buildState(t, s, p, today, c) }
            .flowOn(Dispatchers.Default)
            .catch { emit(TasksUiState(loading = false, error = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    private fun buildState(
        all: List<TaskEntity>,
        subtasks: List<SubtaskEntity>,
        projectList: List<ProjectEntity>,
        today: LocalDate,
        c: Controls,
    ): TasksUiState {
        val progress = HashMap<String, IntArray>()
        for (s in subtasks) {
            val a = progress.getOrPut(s.taskId) { IntArray(2) }
            if (s.done) a[0]++
            a[1]++
        }
        val projectById = projectList.associateBy { it.id }
        val visible = TaskListLogic.visibleTasks(all, c.view, c.filters, c.query, c.sort, today)
        val items = visible.map { task ->
            val p = progress[task.id]
            val project = task.projectId?.let { projectById[it] }
            TaskItem(
                task = task,
                project = project?.let { ProjectTag(it.name, it.color) },
                subtasksDone = p?.get(0) ?: 0,
                subtasksTotal = p?.get(1) ?: 0,
            )
        }
        return TasksUiState(
            loading = false,
            items = items,
            counts = TaskListLogic.counts(all, today),
            view = c.view,
            filters = c.filters,
            sort = c.sort,
            today = today,
            projects = projectList.sortedBy { it.name.lowercase() },
            totalTasks = all.size,
            hasQuery = c.query.isNotBlank(),
        )
    }

    fun setView(v: TaskView) { view.value = v }
    fun setQuery(q: String) { _query.value = q.take(100) }
    fun setFilters(f: TaskFilters) { filters.value = f }
    fun clearFilters() { filters.value = TaskFilters() }
    fun clearSearchAndFilters() { filters.value = TaskFilters(); _query.value = "" }
    fun setSortField(field: TaskSortField) { sort.update { TaskSort(field, reversed = false) } }
    fun toggleSortReversed() { sort.update { it.copy(reversed = !it.reversed) } }
    fun retry() { retry.update { it + 1 } }

    fun onToggleComplete(task: TaskEntity, checked: Boolean) = launchSafe { tasks.setCompleted(task.id, checked) }
    fun onMarkInProgress(task: TaskEntity) = launchSafe { tasks.setStatus(task.id, TaskStatus.IN_PROGRESS) }
    fun onReopen(task: TaskEntity) = launchSafe { tasks.setStatus(task.id, TaskStatus.TODO) }
    fun onRestore(task: TaskEntity) = launchSafe { tasks.setStatus(task.id, TaskStatus.TODO) }

    fun onArchive(task: TaskEntity) = launchSafe {
        tasks.setStatus(task.id, TaskStatus.ARCHIVED)
        _events.trySend(TasksEvent.Archived(task.id, task.status))
    }

    fun undoArchive(e: TasksEvent.Archived) = launchSafe { tasks.setStatus(e.id, e.previous) }

    fun onDelete(task: TaskEntity) = launchSafe {
        val subs = tasks.getSubtasks(task.id)
        tasks.delete(task.id)
        _events.trySend(TasksEvent.Deleted(task, subs))
    }

    fun undoDelete(e: TasksEvent.Deleted) = launchSafe { tasks.save(e.task, e.subtasks) }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.trySend(TasksEvent.Failed)
            }
        }
    }
}
