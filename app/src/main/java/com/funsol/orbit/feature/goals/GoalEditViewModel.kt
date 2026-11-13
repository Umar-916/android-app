package com.funsol.orbit.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.newId
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.repository.GoalRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalForm(
    val title: String = "",
    val description: String = "",
    val targetText: String = "100",
    val currentText: String = "0",
    val unit: String = "",
    val startDate: Int? = null,
    val targetDate: Int? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
)

data class GoalEditUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val loadError: Boolean = false,
    val failures: Int = 0,
    val isNew: Boolean = true,
    val form: GoalForm = GoalForm(),
    val initial: GoalForm = GoalForm(),
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val finished: Boolean = false,
) {
    val dirty: Boolean get() = form != initial
    val titleInvalid: Boolean get() = form.title.isBlank()
    val targetResult: NumberParse get() = GoalNumbers.parseTarget(form.targetText)
    val currentResult: NumberParse get() = GoalNumbers.parseCurrent(form.currentText)
    val dateRangeInvalid: Boolean get() = !GoalsLogic.isDateRangeValid(form.startDate, form.targetDate)
    val valid: Boolean
        get() = !titleInvalid && targetResult is NumberParse.Ok && currentResult is NumberParse.Ok && !dateRangeInvalid
}

class GoalEditViewModel(
    private val goalId: String?,
    private val repository: GoalRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private companion object {
        const val MAX_NAME = 100
        const val MAX_DESCRIPTION = 2000
        const val MAX_UNIT = 32
    }
    private val _state = MutableStateFlow(GoalEditUiState(loading = goalId != null))
    val state: StateFlow<GoalEditUiState> = _state.asStateFlow()

    private var existing: GoalEntity? = null

    init {
        load()
    }

    fun load() {
        if (goalId == null) return
        _state.update { it.copy(loading = true, loadError = false) }
        viewModelScope.launch {
            try {
                val goal = repository.get(goalId)
                existing = goal
                if (goal == null) {
                    _state.update { it.copy(loading = false, notFound = true) }
                } else {
                    val form = GoalForm(
                        title = goal.title,
                        description = goal.description,
                        targetText = GoalNumbers.toEditText(goal.targetValue),
                        currentText = GoalNumbers.toEditText(goal.currentValue),
                        unit = goal.unit,
                        startDate = goal.startDate,
                        targetDate = goal.targetDate,
                        status = goal.status,
                    )
                    _state.update { it.copy(loading = false, isNew = false, form = form, initial = form) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, loadError = true) }
            }
        }
    }

    fun update(transform: (GoalForm) -> GoalForm) = _state.update { s ->
        s.copy(form = transform(s.form).let { it.copy(title = it.title.take(MAX_NAME), description = it.description.take(MAX_DESCRIPTION), unit = it.unit.take(MAX_UNIT)) })
    }

    fun save() {
        val current = _state.value
        if (current.saving || current.loading || current.notFound) return
        val target = (current.targetResult as? NumberParse.Ok)?.value
        val entered = (current.currentResult as? NumberParse.Ok)?.value
        if (current.titleInvalid || target == null || entered == null || current.dateRangeInvalid) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        val value = if (current.form.status == GoalStatus.COMPLETED && entered < target) target else entered
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val form = current.form
            val timestamp = now()
            try {
                repository.save(
                    GoalEntity(
                        id = existing?.id ?: newId(),
                        title = form.title.trim(),
                        description = form.description.trim(),
                        targetValue = target,
                        currentValue = value,
                        unit = form.unit.trim(),
                        startDate = form.startDate,
                        targetDate = form.targetDate,
                        status = form.status,
                        createdAt = existing?.createdAt ?: timestamp,
                        updatedAt = timestamp,
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
