package com.funsol.orbit.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.HabitMath
import com.funsol.orbit.core.common.HabitStats
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.database.entity.HabitEntity
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

data class HabitDetailUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val habit: HabitEntity? = null,
    val stats: HabitStats = HabitStats(0, 0, 0, 0, 0),
    val month: YearMonth = YearMonth.of(1970, 1),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val weeks: List<List<DayCell?>> = emptyList(),
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val todayEpochDay: Int = 0,
    val scheduledToday: Boolean = false,
    val checkedToday: Boolean = false,
    val totalCheckIns: Int = 0,
) {
    val notFound: Boolean get() = !loading && !error && habit == null
}

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModel(
    private val habitId: String,
    private val habits: HabitRepository,
) : ViewModel() {
    private val shownMonth = MutableStateFlow<YearMonth?>(null)

    private val reload = MutableStateFlow(0)

    val state: StateFlow<HabitDetailUiState> = reload.flatMapLatest {
        combine(
            habits.observe(habitId),
            habits.observeLogs(habitId),
            todayFlow(),
            shownMonth,
        ) { habit, logs, today, requested ->
            if (habit == null) {
                HabitDetailUiState(loading = false)
            } else {
                build(habit, logs.mapTo(HashSet()) { it.date.toLocalDate() }, today, requested)
            }
        }.catch { emit(HabitDetailUiState(loading = false, error = true)) }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())

    fun retry() = reload.update { it + 1 }

    private fun build(habit: HabitEntity, logs: Set<LocalDate>, today: LocalDate, requested: YearMonth?): HabitDetailUiState {
        val start = habit.startDate.toLocalDate()
        val firstMonth = YearMonth.from(start)
        val lastMonth = YearMonth.from(today)
        val month = (requested ?: lastMonth).let { if (it.isBefore(firstMonth)) firstMonth else if (it.isAfter(lastMonth)) lastMonth else it }
        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        return HabitDetailUiState(
            loading = false,
            habit = habit,
            stats = HabitMath.stats(habit.scheduleMask, start, logs, today),
            month = month,
            firstDayOfWeek = firstDay,
            weeks = HabitCalendarMath.weeks(month, firstDay, habit.scheduleMask, start, logs, today),
            canGoPrevious = month.isAfter(firstMonth),
            canGoNext = month.isBefore(lastMonth),
            todayEpochDay = today.toEpochDayInt(),
            scheduledToday = !today.isBefore(start) && HabitMath.isScheduled(habit.scheduleMask, today),
            checkedToday = today in logs,
            totalCheckIns = logs.size,
        )
    }

    fun previousMonth() = shiftMonth(-1)

    fun nextMonth() = shiftMonth(1)

    private fun shiftMonth(delta: Long) {
        val current = state.value.month
        shownMonth.update { current.plusMonths(delta) }
    }

    private val _writeFailures = MutableStateFlow(0)
    val writeFailures: StateFlow<Int> = _writeFailures

    fun setChecked(epochDay: Int, checked: Boolean) {
        val current = state.value
        val habit = current.habit ?: return
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
