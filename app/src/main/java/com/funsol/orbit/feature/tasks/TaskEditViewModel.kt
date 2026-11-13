package com.funsol.orbit.feature.tasks

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.RecurrenceType
import com.funsol.orbit.core.common.WeekdayMask
import com.funsol.orbit.core.common.newId
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.SubtaskEntity
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.repository.ProjectRepository
import com.funsol.orbit.core.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class SubtaskDraft(val id: String, val title: String, val done: Boolean)

data class TaskForm(
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: Int? = null,
    val dueTime: Int? = null,
    val reminderOffset: Int? = null,
    val projectId: String? = null,
    val subtasks: List<SubtaskDraft> = emptyList(),
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceWeekdays: Int = 0,
    val recurrenceEnd: Int? = null,
)

enum class TitleError { REQUIRED, TOO_LONG }
enum class SaveError { DUPLICATE_OCCURRENCE, GENERIC }

data class TaskEditUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val notFound: Boolean = false,
    val isNew: Boolean = true,
    val isOccurrenceOfSeries: Boolean = false,
    val originalTitle: String = "",
    val form: TaskForm = TaskForm(),
    val dirty: Boolean = false,
    val titleError: TitleError? = null,
    val projects: List<ProjectEntity> = emptyList(),
    val saving: Boolean = false,
    val saveError: SaveError? = null,
)

class TaskEditViewModel(
    private val taskId: String?,
    private val presetProjectId: String?,
    private val presetDate: Int?,
    private val tasks: TaskRepository,
    private val projects: ProjectRepository,
) : ViewModel() {
    companion object {
        const val MAX_TITLE = 200
        const val MAX_SUBTASK_TITLE = 200
        val REMINDER_OPTIONS: List<Int?> = listOf(null, 0, 10, 30, 60, 1440)

        fun validateTitle(title: String): TitleError? = when {
            title.isBlank() -> TitleError.REQUIRED
            title.trim().length > MAX_TITLE -> TitleError.TOO_LONG
            else -> null
        }
    }

    private val _state = MutableStateFlow(TaskEditUiState())
    val state: StateFlow<TaskEditUiState> = _state

    private val closeChannel = Channel<Unit>(Channel.BUFFERED)
    val closeEvents: Flow<Unit> = closeChannel.receiveAsFlow()

    private var original: TaskEntity? = null
    private var initialForm = TaskForm()
    private var titleTouched = false

    init {
        viewModelScope.launch {
            projects.observeAll().catch { }.collect { list ->
                _state.update { it.copy(projects = list.sortedBy { p -> p.name.lowercase() }) }
            }
        }
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadFailed = false, notFound = false) }
        viewModelScope.launch {
            if (taskId == null) {
                val form = TaskForm(dueDate = presetDate, projectId = presetProjectId)
                initialForm = form
                _state.update { it.copy(loading = false, isNew = true, form = form, dirty = false) }
                return@launch
            }
            try {
                val task = tasks.get(taskId)
                if (task == null) {
                    _state.update { it.copy(loading = false, notFound = true) }
                    return@launch
                }
                val subs = tasks.getSubtasks(taskId)
                original = task
                val form = task.toForm(subs)
                initialForm = form
                _state.update {
                    it.copy(
                        loading = false,
                        isNew = false,
                        isOccurrenceOfSeries = task.seriesId != null,
                        originalTitle = task.title,
                        form = form,
                        dirty = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    private fun edit(transform: (TaskForm) -> TaskForm) {
        _state.update { s ->
            val form = transform(s.form)
            s.copy(
                form = form,
                dirty = form != initialForm,
                saveError = null,
                titleError = if (titleTouched) validateTitle(form.title) else null,
            )
        }
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value.take(MAX_TITLE)) }

    fun onTitleBlur() {
        titleTouched = true
        _state.update { it.copy(titleError = validateTitle(it.form.title)) }
    }

    fun onDescriptionChange(value: String) = edit { it.copy(description = value) }
    fun onPriorityChange(value: Priority) = edit { it.copy(priority = value) }
    fun onStatusChange(value: TaskStatus) = edit { it.copy(status = value) }
    fun onProjectChange(id: String?) = edit { it.copy(projectId = id) }

    fun onDueDateChange(epochDay: Int?) = edit { f ->
        if (epochDay == null) {
            f.copy(
                dueDate = null,
                dueTime = null,
                reminderOffset = null,
                recurrenceType = RecurrenceType.NONE,
                recurrenceEnd = null,
            )
        } else {
            f.copy(dueDate = epochDay)
        }
    }

    fun onDueTimeChange(minuteOfDay: Int?) = edit { f ->
        if (minuteOfDay != null && f.dueDate == null) {
            f.copy(dueDate = LocalDate.now().toEpochDayInt(), dueTime = minuteOfDay)
        } else {
            f.copy(dueTime = minuteOfDay)
        }
    }

    fun onReminderChange(offsetMinutes: Int?) = edit { f ->
        if (f.dueDate == null) f.copy(reminderOffset = null) else f.copy(reminderOffset = offsetMinutes)
    }

    fun onRecurrenceTypeChange(type: RecurrenceType) = edit { f ->
        if (type == RecurrenceType.NONE) {
            f.copy(recurrenceType = type, recurrenceEnd = null)
        } else {
            val date = f.dueDate ?: LocalDate.now().toEpochDayInt()
            val mask = if (type == RecurrenceType.WEEKLY && f.recurrenceWeekdays == 0) {
                WeekdayMask.bit(date.toLocalDate().dayOfWeek)
            } else {
                f.recurrenceWeekdays
            }
            f.copy(recurrenceType = type, dueDate = date, recurrenceWeekdays = mask)
        }
    }

    fun onIntervalChange(value: Int) = edit { it.copy(recurrenceInterval = value.coerceIn(1, RecurrenceSummary.MAX_INTERVAL)) }

    fun onWeekdayToggle(day: DayOfWeek) = edit { it.copy(recurrenceWeekdays = WeekdayMask.toggle(it.recurrenceWeekdays, day)) }

    fun onRecurrenceEndChange(epochDay: Int?) = edit { it.copy(recurrenceEnd = epochDay) }

    fun addSubtask(title: String): Boolean {
        val clean = title.trim().take(MAX_SUBTASK_TITLE)
        if (clean.isEmpty()) return false
        edit { it.copy(subtasks = it.subtasks + SubtaskDraft(newId(), clean, false)) }
        return true
    }

    fun toggleSubtask(id: String, done: Boolean) =
        edit { f -> f.copy(subtasks = f.subtasks.map { if (it.id == id) it.copy(done = done) else it }) }

    fun deleteSubtask(id: String) = edit { f -> f.copy(subtasks = f.subtasks.filterNot { it.id == id }) }

    fun moveSubtask(id: String, delta: Int) = edit { f ->
        val index = f.subtasks.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target !in f.subtasks.indices) {
            f
        } else {
            val list = f.subtasks.toMutableList()
            list.add(target, list.removeAt(index))
            f.copy(subtasks = list)
        }
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.loading) return
        val f = s.form
        val error = validateTitle(f.title)
        if (error != null) {
            titleTouched = true
            _state.update { it.copy(titleError = error) }
            return
        }
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val base = original
                val wantsComplete = f.status == TaskStatus.COMPLETED && base?.status != TaskStatus.COMPLETED
                val storedStatus = when {
                    wantsComplete -> if (base?.status == TaskStatus.IN_PROGRESS) TaskStatus.IN_PROGRESS else TaskStatus.TODO
                    else -> f.status
                }
                val hasDate = f.dueDate != null
                val repeating = hasDate && f.recurrenceType != RecurrenceType.NONE
                val entity = (base ?: TaskEntity(id = newId(), title = "", createdAt = now, updatedAt = now)).copy(
                    title = f.title.trim(),
                    description = f.description.trim(),
                    priority = f.priority,
                    status = storedStatus,
                    completedAt = if (storedStatus == TaskStatus.COMPLETED) base?.completedAt else null,
                    dueDate = f.dueDate,
                    dueTime = if (hasDate) f.dueTime else null,
                    reminderOffsetMinutes = if (hasDate) f.reminderOffset else null,
                    projectId = f.projectId,
                    recurrenceType = if (repeating) f.recurrenceType else RecurrenceType.NONE,
                    recurrenceInterval = f.recurrenceInterval.coerceAtLeast(1),
                    recurrenceWeekdays = when {
                        !repeating || f.recurrenceType != RecurrenceType.WEEKLY -> 0
                        f.recurrenceWeekdays == 0 -> WeekdayMask.bit(f.dueDate!!.toLocalDate().dayOfWeek)
                        else -> f.recurrenceWeekdays
                    },
                    recurrenceEndDate = if (repeating) f.recurrenceEnd?.coerceAtLeast(f.dueDate!!) else null,
                    updatedAt = now,
                )
                val subs = f.subtasks.map { SubtaskEntity(id = it.id, taskId = entity.id, title = it.title, done = it.done) }
                val saved = tasks.save(entity, subs)
                if (wantsComplete) tasks.setCompleted(saved.id, true)
                closeChannel.trySend(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SQLiteConstraintException) {
                _state.update { it.copy(saving = false, saveError = SaveError.DUPLICATE_OCCURRENCE) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, saveError = SaveError.GENERIC) }
            }
        }
    }

    fun delete() {
        val id = taskId ?: return
        runThenClose { tasks.delete(id) }
    }

    fun archive() {
        val id = taskId ?: return
        runThenClose { tasks.setStatus(id, TaskStatus.ARCHIVED) }
    }

    private fun runThenClose(block: suspend () -> Unit) {
        if (_state.value.saving || _state.value.loading) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            try {
                block()
                closeChannel.trySend(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, saveError = SaveError.GENERIC) }
            }
        }
    }

    private fun TaskEntity.toForm(subs: List<SubtaskEntity>) = TaskForm(
        title = title,
        description = description,
        priority = priority,
        status = status,
        dueDate = dueDate,
        dueTime = dueTime,
        reminderOffset = reminderOffsetMinutes,
        projectId = projectId,
        subtasks = subs.sortedBy { it.position }.map { SubtaskDraft(it.id, it.title, it.done) },
        recurrenceType = recurrenceType,
        recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
        recurrenceWeekdays = recurrenceWeekdays,
        recurrenceEnd = recurrenceEndDate,
    )
}
