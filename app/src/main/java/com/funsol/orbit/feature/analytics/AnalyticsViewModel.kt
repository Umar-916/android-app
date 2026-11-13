package com.funsol.orbit.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.repository.GoalRepository
import com.funsol.orbit.core.repository.HabitRepository
import com.funsol.orbit.core.repository.ProjectRepository
import com.funsol.orbit.core.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class AnalyticsUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val today: LocalDate? = null,
    val data: AnalyticsData = AnalyticsData(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    tasks: TaskRepository,
    projects: ProjectRepository,
    habits: HabitRepository,
    goals: GoalRepository,
) : ViewModel() {
    private val range = MutableStateFlow(AnalyticsRange.WEEK)
    private val retryTrigger = MutableStateFlow(0)

    private val sources: Flow<Sources> = combine(
        tasks.observeAll(),
        projects.observeAll(),
        habits.observeAll(),
        habits.observeAllLogs(),
        goals.observeAll(),
    ) { t, p, h, l, g -> Sources(t, p, h, l, g) }

    val uiState: StateFlow<AnalyticsUiState> = retryTrigger
        .flatMapLatest {
            combine(sources, todayFlow(), range) { s, today, r ->
                AnalyticsUiState(
                    loading = false,
                    range = r,
                    today = today,
                    data = AnalyticsCalculator.calculate(s.tasks, s.projects, s.habits, s.logs, s.goals, today, r),
                )
            }
                .flowOn(Dispatchers.Default)
                .catch { emit(AnalyticsUiState(loading = false, failed = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    fun setRange(value: AnalyticsRange) {
        range.value = value
    }

    fun retry() {
        retryTrigger.value += 1
    }

    private class Sources(
        val tasks: List<com.funsol.orbit.core.database.entity.TaskEntity>,
        val projects: List<com.funsol.orbit.core.database.entity.ProjectEntity>,
        val habits: List<com.funsol.orbit.core.database.entity.HabitEntity>,
        val logs: List<com.funsol.orbit.core.database.entity.HabitLogEntity>,
        val goals: List<com.funsol.orbit.core.database.entity.GoalEntity>,
    )
}
