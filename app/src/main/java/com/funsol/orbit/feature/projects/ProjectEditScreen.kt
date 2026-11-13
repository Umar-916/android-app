package com.funsol.orbit.feature.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.ProjectsEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProjectEditScreen(projectId: String?, nav: OrbitNavigator) {
    val vm: ProjectEditViewModel = koinViewModel(key = "project_edit_$projectId", parameters = { parametersOf(projectId) })
    val state by vm.state.collectAsStateWithLifecycle()
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    PostAnalytic(ProjectsEvents.PROJECT_EDIT_SCREEN_DISPLAY)

    LaunchedEffect(state.saved) { if (state.saved) nav.back() }

    val guard = rememberClickGuard()
    val requestBack: () -> Unit = {
        context.postAnalytic(ProjectsEvents.PROJECT_EDIT_BACK_CLICK)
        if (state.dirty && !state.saved) {
            showDiscard = true
        } else {
            guard.run { nav.back() }
        }
    }
    BackHandler(enabled = state.dirty && !state.saved) { showDiscard = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                title = stringResource(if (state.isNew) R.string.projects_edit_new_title else R.string.projects_edit_title),
                onBack = requestBack,
                accent = FeatureAccent.PROJECTS,
            ) {
                TextButton(
                    onClick = {
                        context.postAnalytic(ProjectsEvents.PROJECT_EDIT_SAVE_CLICK)
                        vm.save()
                    },
                    enabled = !state.saving && !state.loading) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingState()
                state.notFound -> ErrorState(stringResource(R.string.projects_not_found))
                else -> ProjectFormContent(state, vm::update, onSave = {
                    context.postAnalytic(ProjectsEvents.PROJECT_EDIT_SAVE_CLICK)
                    vm.save()
                })
            }
        }
    }

    if (showDiscard) {
        DiscardChangesDialog(
            onDiscard = { showDiscard = false; nav.back() },
            onKeepEditing = { showDiscard = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectFormContent(
    state: ProjectEditUiState,
    onChange: ((ProjectForm) -> ProjectForm) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val form = state.form
    val nameError = state.showNameError && state.nameInvalid
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        FormSection(stringResource(R.string.projects_section_details)) {
            PlanningTextField(
                value = form.name,
                onValueChange = { v -> onChange { it.copy(name = v) } },
                label = stringResource(R.string.projects_field_name),
                errorText = if (nameError) stringResource(R.string.projects_error_name_required) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            PlanningTextField(
                value = form.description,
                onValueChange = { v -> onChange { it.copy(description = v) } },
                label = stringResource(R.string.projects_field_description),
                minLines = 2,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        FormSection(stringResource(R.string.projects_section_appearance)) {
            ColorPickerRow(selected = form.color, onSelect = { c ->
                    context.postAnalytic(ProjectsEvents.PROJECT_EDIT_COLOR_SELECT)
                    onChange { it.copy(color = c) }
                },)
        }
        FormSection(stringResource(R.string.projects_section_schedule)) {
            OrbitDateField(
                label = stringResource(R.string.projects_field_start),
                epochDay = form.startDate,
                onChange = { d -> onChange { it.copy(startDate = d) } },
            )
            OrbitDateField(
                label = stringResource(R.string.projects_field_end),
                epochDay = form.endDate,
                onChange = { d -> onChange { it.copy(endDate = d) } },
                errorText = if (state.dateRangeInvalid) stringResource(R.string.projects_error_end_before_start) else null,
            )
        }
        FormSection(stringResource(R.string.projects_field_status)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ProjectStatus.entries.forEach { status ->
                    FilterChip(
                        selected = form.status == status,
                        onClick = {
                            context.postAnalytic(ProjectsEvents.PROJECT_EDIT_STATUS_SELECT)
                            onChange { it.copy(status = status) }
                        },
                        label = { Text(projectStatusLabel(status)) },
                        colors = orbitChipColors(),
                        border = null,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
        }
        GradientButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            brush = Color(form.color).toGradient(),
        )
        Spacer(Modifier.height(Spacing.xl))
    }
}
