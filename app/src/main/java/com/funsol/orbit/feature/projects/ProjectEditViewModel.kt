package com.funsol.orbit.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.newId
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectForm(
    val name: String = "",
    val description: String = "",
    val color: Int = ProjectColors.default,
    val startDate: Int? = null,
    val endDate: Int? = null,
    val status: ProjectStatus = ProjectStatus.ACTIVE,
)

data class ProjectEditUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val isNew: Boolean = true,
    val form: ProjectForm = ProjectForm(),
    val initial: ProjectForm = ProjectForm(),
    val showNameError: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val dirty: Boolean get() = form != initial
    val nameInvalid: Boolean get() = form.name.isBlank()
    val dateRangeInvalid: Boolean get() = !ProjectsLogic.isDateRangeValid(form.startDate, form.endDate)
}

class ProjectEditViewModel(
    private val projectId: String?,
    private val repository: ProjectRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private companion object {
        const val MAX_NAME = 100
        const val MAX_DESCRIPTION = 2000
    }
    private val _state = MutableStateFlow(ProjectEditUiState(loading = projectId != null))
    val state: StateFlow<ProjectEditUiState> = _state.asStateFlow()

    private var existing: ProjectEntity? = null

    init {
        if (projectId != null) {
            viewModelScope.launch {
                val project = try {
                    repository.get(projectId)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    null
                }
                existing = project
                if (project == null) {
                    _state.update { it.copy(loading = false, notFound = true) }
                } else {
                    val form = ProjectForm(
                        name = project.name,
                        description = project.description,
                        color = project.color,
                        startDate = project.startDate,
                        endDate = project.endDate,
                        status = project.status,
                    )
                    _state.update { it.copy(loading = false, isNew = false, form = form, initial = form) }
                }
            }
        }
    }

    fun update(transform: (ProjectForm) -> ProjectForm) = _state.update { s ->
        s.copy(form = transform(s.form).let { it.copy(name = it.name.take(MAX_NAME), description = it.description.take(MAX_DESCRIPTION)) })
    }

    fun save() {
        val current = _state.value
        if (current.saving || current.loading) return
        if (current.nameInvalid) {
            _state.update { it.copy(showNameError = true) }
            return
        }
        if (current.dateRangeInvalid) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val form = current.form
            val timestamp = now()
            try {
                repository.save(
                    ProjectEntity(
                        id = existing?.id ?: newId(),
                        name = form.name.trim(),
                        description = form.description.trim(),
                        color = form.color,
                        startDate = form.startDate,
                        endDate = form.endDate,
                        status = form.status,
                        createdAt = existing?.createdAt ?: timestamp,
                        updatedAt = timestamp,
                    ),
                )
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Throwable) {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}
