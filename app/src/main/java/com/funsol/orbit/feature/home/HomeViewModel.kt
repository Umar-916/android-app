package com.funsol.orbit.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.datastore.HomeWidget
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.repository.HabitRepository
import com.funsol.orbit.core.repository.ProjectRepository
import com.funsol.orbit.core.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val today: LocalDate? = null,
    val data: HomeData = HomeData(),
    val hiddenWidgets: Set<HomeWidget> = emptySet(),
) {
    fun isVisible(widget: HomeWidget) = widget !in hiddenWidgets
}

sealed interface HomeEvent {
    data class TaskCompleted(val taskId: String) : HomeEvent
    data object ActionFailed : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
    private val habits: HabitRepository,
    settings: SettingsStore,
) : ViewModel() {
    private val retryTrigger = MutableStateFlow(0)
    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = eventChannel.receiveAsFlow()

    private val dashboard: Flow<Pair<LocalDate, HomeData>> = combine(
        tasks.observeAll(),
        projects.observeAll(),
        habits.observeAll(),
        habits.observeAllLogs(),
        todayFlow(),
    ) { t, p, h, l, today -> today to HomeAggregator.aggregate(t, p, h, l, today) }
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> = retryTrigger
        .flatMapLatest {
            combine(dashboard, settings.settings) { (today, data), s ->
                HomeUiState(loading = false, today = today, data = data, hiddenWidgets = s.hiddenWidgets)
            }.catch { emit(HomeUiState(loading = false, failed = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun retry() {
        retryTrigger.value += 1
    }

    fun completeTask(taskId: String) = launchAction(onSuccess = HomeEvent.TaskCompleted(taskId)) {
        tasks.setCompleted(taskId, true)
    }

    fun reopenTask(taskId: String) = launchAction { tasks.setCompleted(taskId, false) }

    fun setHabitChecked(habitId: String, checked: Boolean) {
        val day = uiState.value.today ?: return
        launchAction { habits.setChecked(habitId, day.toEpochDay().toInt(), checked) }
    }

    private fun launchAction(onSuccess: HomeEvent? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                onSuccess?.let { eventChannel.send(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                eventChannel.send(HomeEvent.ActionFailed)
            }
        }
    }
}
