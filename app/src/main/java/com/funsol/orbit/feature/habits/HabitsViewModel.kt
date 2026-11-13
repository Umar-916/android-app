package com.funsol.orbit.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.repository.HabitRepository
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.launch

data class HabitsUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val active: List<HabitListItem> = emptyList(),
    val archived: List<HabitListItem> = emptyList(),
    val todayEpochDay: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(private val habits: HabitRepository) : ViewModel() {
    private val reload = MutableStateFlow(0)

    private val _writeFailures = MutableStateFlow(0)
    val writeFailures: StateFlow<Int> = _writeFailures

    val state: StateFlow<HabitsUiState> = reload.flatMapLatest {
        combine(habits.observeAll(), habits.observeAllLogs(), todayFlow()) { list, logs, today ->
            val items = HabitsLogic.buildItems(list, logs, today)
            HabitsUiState(
                loading = false,
                active = items.filter { !it.habit.archived },
                archived = items.filter { it.habit.archived },
                todayEpochDay = today.toEpochDayInt(),
            )
        }.catch { emit(HabitsUiState(loading = false, error = true)) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun retry() = reload.update { it + 1 }

    fun setChecked(habitId: String, epochDay: Int, checked: Boolean) {
        val current = state.value
        val habit = (current.active + current.archived).firstOrNull { it.habit.id == habitId }?.habit ?: return
        if (!HabitsLogic.canWrite(habit, epochDay, current.todayEpochDay, checked)) return
        viewModelScope.launch {
            try {
                habits.setChecked(habitId, epochDay, checked)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _writeFailures.update { it + 1 }
            }
        }
    }
}
