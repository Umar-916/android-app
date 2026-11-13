package com.funsol.orbit.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.database.entity.TaskEntity
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val today: LocalDate = LocalDate.ofEpochDay(0),
    val month: YearMonth = YearMonth.of(1970, 1),
    val selected: LocalDate = LocalDate.ofEpochDay(0),
    val mode: CalendarMode = CalendarMode.DAY,
    val days: Map<Int, DayInfo> = emptyMap(),
    val schedule: DaySchedule = DaySchedule(emptyList(), emptyList()),
    val agenda: List<AgendaGroup> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val tasks: TaskRepository) : ViewModel() {
    private val month = MutableStateFlow<YearMonth?>(null)
    private val selected = MutableStateFlow<LocalDate?>(null)
    private val mode = MutableStateFlow(CalendarMode.DAY)
    private val retry = MutableStateFlow(0)

    private val _failures = Channel<Unit>(Channel.BUFFERED)
    val failures: Flow<Unit> = _failures.receiveAsFlow()

    val uiState: StateFlow<CalendarUiState> = retry.flatMapLatest {
        combine(tasks.observeAll(), todayFlow(), month, selected, mode) { all, today, m, sel, md ->
            build(all, today, m, sel, md)
        }
            .flowOn(Dispatchers.Default)
            .catch { emit(CalendarUiState(loading = false, error = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    private fun build(
        all: List<TaskEntity>,
        today: LocalDate,
        monthOverride: YearMonth?,
        selectedOverride: LocalDate?,
        mode: CalendarMode,
    ): CalendarUiState {
        val sel = selectedOverride ?: today
        val visibleMonth = monthOverride ?: YearMonth.from(sel)
        val byDay = CalendarLogic.groupByDay(all)
        return CalendarUiState(
            loading = false,
            today = today,
            month = visibleMonth,
            selected = sel,
            mode = mode,
            days = byDay.mapValues { (_, list) -> CalendarLogic.dayInfo(list, today) },
            schedule = CalendarLogic.daySchedule(byDay[sel.toEpochDayInt()].orEmpty()),
            agenda = if (mode == CalendarMode.AGENDA) CalendarLogic.buildAgenda(all, today) else emptyList(),
        )
    }

    fun selectDay(date: LocalDate) {
        selected.value = date
        month.value = YearMonth.from(date)
    }

    fun shiftMonth(delta: Long) {
        val current = uiState.value
        if (current.loading) return
        val next = CalendarLogic.shiftSelection(current.selected, current.today, delta)
        selected.value = next
        month.value = YearMonth.from(current.month.plusMonths(delta))
    }

    fun goToToday() {
        selected.value = null
        month.value = null
    }

    fun setMode(value: CalendarMode) { mode.value = value }

    fun retry() { retry.update { it + 1 } }

    fun onToggleComplete(task: TaskEntity, checked: Boolean) {
        viewModelScope.launch {
            try {
                tasks.setCompleted(task.id, checked)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _failures.trySend(Unit)
            }
        }
    }
}
