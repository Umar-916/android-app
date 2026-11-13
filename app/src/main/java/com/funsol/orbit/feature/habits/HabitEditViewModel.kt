package com.funsol.orbit.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.WeekdayMask
import com.funsol.orbit.core.common.newId
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.todayFlow
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.repository.HabitRepository
import com.funsol.orbit.feature.projects.ProjectColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class HabitForm(
    val name: String = "",
    val description: String = "",
    val color: Int = ProjectColors.default,
    val scheduleMask: Int = WeekdayMask.ALL,
    val customSchedule: Boolean = false,
    val startDate: Int? = null,
    val archived: Boolean = false,
)

data class HabitEditUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val loadError: Boolean = false,
    val failures: Int = 0,
    val isNew: Boolean = true,
    val form: HabitForm = HabitForm(),
    val initial: HabitForm = HabitForm(),
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val finished: Boolean = false,
) {
    val dirty: Boolean get() = form != initial
    val nameInvalid: Boolean get() = form.name.isBlank()
    val scheduleInvalid: Boolean get() = !HabitSchedule.isValid(form.scheduleMask)
    val selectedPreset: SchedulePreset
        get() = if (form.customSchedule) SchedulePreset.CUSTOM else HabitSchedule.presetOf(form.scheduleMask)
}

class HabitEditViewModel(
    private val habitId: String?,
    private val repository: HabitRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private companion object {
        const val MAX_NAME = 100
        const val MAX_DESCRIPTION = 2000
    }
    private val _state = MutableStateFlow(HabitEditUiState(loading = habitId != null))
    val state: StateFlow<HabitEditUiState> = _state.asStateFlow()

    private var existing: HabitEntity? = null

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = habitId != null, loadError = false) }
        viewModelScope.launch {
            try {
                if (habitId == null) {
                    val today = todayFlow().first().toEpochDayInt()
                    val form = HabitForm(startDate = today)
                    _state.update { it.copy(form = form, initial = form) }
                } else {
                    val habit = withContext(Dispatchers.IO) { repository.get(habitId) }
                    existing = habit
                    if (habit == null) {
                        _state.update { it.copy(loading = false, notFound = true) }
                    } else {
                        val form = HabitForm(
                            name = habit.name,
                            description = habit.description,
                            color = habit.color,
                            scheduleMask = habit.scheduleMask,
                            customSchedule = HabitSchedule.presetOf(habit.scheduleMask) == SchedulePreset.CUSTOM,
                            startDate = habit.startDate,
                            archived = habit.archived,
                        )
                        _state.update { it.copy(loading = false, isNew = false, form = form, initial = form) }
                    }
                }
                _state.update { it.copy(loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, loadError = true) }
            }
        }
    }

    fun update(transform: (HabitForm) -> HabitForm) = _state.update { s ->
        s.copy(form = transform(s.form).let { it.copy(name = it.name.take(MAX_NAME), description = it.description.take(MAX_DESCRIPTION)) })
    }

    fun selectPreset(preset: SchedulePreset) = update {
        it.copy(scheduleMask = HabitSchedule.maskOf(preset, it.scheduleMask), customSchedule = preset == SchedulePreset.CUSTOM)
    }

    fun toggleDay(day: DayOfWeek) = update {
        it.copy(scheduleMask = WeekdayMask.toggle(it.scheduleMask, day), customSchedule = true)
    }

    fun save() {
        val current = _state.value
        if (current.saving || current.loading || current.notFound) return
        if (current.nameInvalid || current.scheduleInvalid || current.form.startDate == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val form = current.form
            val startDate = form.startDate
            if (startDate == null) {
                _state.update { it.copy(saving = false) }
                return@launch
            }
            try {
                repository.save(
                    HabitEntity(
                        id = existing?.id ?: newId(),
                        name = form.name.trim(),
                        description = form.description.trim(),
                        color = form.color,
                        scheduleMask = form.scheduleMask,
                        startDate = startDate,
                        archived = form.archived,
                        createdAt = existing?.createdAt ?: now(),
                    ),
                )
                _state.update { it.copy(saving = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(saving = false, failures = it.failures + 1) }
            }
        }
    }

    fun delete() {
        val id = existing?.id ?: return
        if (_state.value.saving || _state.value.finished) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.delete(id)
                _state.update { it.copy(saving = false, finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(saving = false, failures = it.failures + 1) }
            }
        }
    }
}
