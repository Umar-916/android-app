package com.funsol.orbit.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.repository.GoalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalsUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val sections: GoalSections = GoalSections(emptyList(), emptyList(), emptyList()),
    val todayEpochDay: Int = 0,
) {
    val isEmpty: Boolean
        get() = sections.inProgress.isEmpty() && sections.completed.isEmpty() && sections.archived.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModel(private val goals: GoalRepository) : ViewModel() {
    private val reload = MutableStateFlow(0)

    val state: StateFlow<GoalsUiState> = reload.flatMapLatest {
        combine(goals.observeAll(), todayFlow()) { list, today ->
            GoalsUiState(loading = false, sections = GoalsLogic.sections(list), todayEpochDay = today.toEpochDayInt())
        }.catch { emit(GoalsUiState(loading = false, error = true)) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun retry() = reload.update { it + 1 }

    private val _writeFailures = MutableStateFlow(0)
    val writeFailures: StateFlow<Int> = _writeFailures

    fun updateProgress(goalId: String, newValue: Double) {
        if (!newValue.isFinite() || newValue < 0.0 || newValue > GoalNumbers.MAX_VALUE) return
        viewModelScope.launch {
            try {
                goals.updateProgress(goalId, newValue)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _writeFailures.update { it + 1 }
            }
        }
    }
}
