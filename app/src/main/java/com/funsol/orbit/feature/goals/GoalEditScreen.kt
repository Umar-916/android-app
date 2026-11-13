package com.funsol.orbit.feature.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.analytics.GoalsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.designsystem.ConfirmDialog
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.feature.projects.CenteredContent
import com.funsol.orbit.feature.projects.DiscardChangesDialog
import com.funsol.orbit.feature.projects.FormSection
import com.funsol.orbit.feature.projects.OrbitDateField
import com.funsol.orbit.feature.projects.PlanningTextField
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GoalEditScreen(goalId: String?, nav: OrbitNavigator) {
    PostAnalytic(GoalsEvents.GOALS_EDIT_SCREEN_DISPLAY)
    val context = LocalContext.current
    val vm: GoalEditViewModel = koinViewModel(key = "goal_edit_$goalId", parameters = { parametersOf(goalId) })
    val state by vm.state.collectAsStateWithLifecycle()
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }

    val guard = rememberClickGuard()
    val snackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.error_generic)
    var handledFailures by rememberSaveable { mutableIntStateOf(state.failures) }
    LaunchedEffect(state.failures) {
        if (state.failures != handledFailures) {
            handledFailures = state.failures
            snackbar.showSnackbar(failedMessage)
        }
    }

    LaunchedEffect(state.finished) { if (state.finished) nav.back() }
    val guarded = state.dirty && !state.finished
    val requestBack: () -> Unit = {
        context.postAnalytic(GoalsEvents.GOALS_EDIT_BACK_CLICK)
        if (guarded) {
            showDiscard = true
        } else {
            guard.run(nav::back)
        }
    }
    BackHandler(enabled = guarded) { showDiscard = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            OrbitTopBar(
                title = stringResource(if (state.isNew) R.string.goals_edit_new_title else R.string.goals_edit_title),
                onBack = requestBack,
                accent = FeatureAccent.GOALS,
            ) {
                TextButton(onClick = {
                    context.postAnalytic(GoalsEvents.GOALS_EDIT_TOP_SAVE_CLICK)
                    vm.save()
                }, enabled = !state.saving && !state.loading && !state.loadError) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingState()
                state.loadError -> ErrorState(
                    stringResource(R.string.error_generic),
                    onRetry = {
                        context.postAnalytic(GoalsEvents.GOALS_EDIT_RETRY_CLICK)
                        vm.load()
                    },
                )
                state.notFound -> ErrorState(stringResource(R.string.goals_not_found))
                else -> GoalFormContent(
                    state,
                    vm::update,
                    onSave = {
                        context.postAnalytic(GoalsEvents.GOALS_EDIT_SAVE_CLICK)
                        vm.save()
                    },
                    onDelete = {
                        context.postAnalytic(GoalsEvents.GOALS_EDIT_DELETE_CLICK)
                        showDelete = true
                    },
                )
            }
        }
    }

    if (showDiscard) {
        PostAnalytic(GoalsEvents.GOALS_DISCARD_DIALOG_APPEAR)
        DiscardChangesDialog(
            onDiscard = {
                context.postAnalytic(GoalsEvents.GOALS_DISCARD_CONFIRM_CLICK)
                showDiscard = false
                guard.run(nav::back)
            },
            onKeepEditing = {
                context.postAnalytic(GoalsEvents.GOALS_DISCARD_KEEP_CLICK)
                showDiscard = false
            },
        )
    }
    if (showDelete) {
        PostAnalytic(GoalsEvents.GOALS_DELETE_DIALOG_APPEAR)
        ConfirmDialog(
            title = stringResource(R.string.goals_delete_title, state.initial.title),
            message = stringResource(R.string.goals_delete_message, state.initial.title),
            confirmLabel = stringResource(R.string.goals_delete_confirm),
            onConfirm = {
                context.postAnalytic(GoalsEvents.GOALS_DELETE_CONFIRM_CLICK)
                showDelete = false
                guard.run(vm::delete)
            },
            onDismiss = {
                context.postAnalytic(GoalsEvents.GOALS_DELETE_CANCEL_CLICK)
                showDelete = false
            },
            destructive = true,
        )
    }
}

@Composable
private fun numberFieldError(result: NumberParse, text: String, showErrors: Boolean): String? {
    val error = (result as? NumberParse.Error)?.error ?: return null
    if (text.isBlank() && !showErrors) return null
    return numberErrorText(error)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalFormContent(
    state: GoalEditUiState,
    onChange: ((GoalForm) -> GoalForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val form = state.form
    val titleError = state.showErrors && state.titleInvalid
    val targetError = numberFieldError(state.targetResult, form.targetText, state.showErrors)
    val currentError = numberFieldError(state.currentResult, form.currentText, state.showErrors)
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        FormSection(stringResource(R.string.goals_section_details)) {
            PlanningTextField(
                value = form.title,
                onValueChange = { v -> onChange { it.copy(title = v) } },
                label = stringResource(R.string.goals_field_title),
                errorText = if (titleError) stringResource(R.string.goals_error_title_required) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            PlanningTextField(
                value = form.description,
                onValueChange = { v -> onChange { it.copy(description = v) } },
                label = stringResource(R.string.goals_field_description),
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        FormSection(stringResource(R.string.goals_section_target)) {
            PlanningTextField(
                value = form.targetText,
                onValueChange = { v -> onChange { it.copy(targetText = v.take(32)) } },
                label = stringResource(R.string.goals_field_target),
                errorText = targetError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            PlanningTextField(
                value = form.currentText,
                onValueChange = { v -> onChange { it.copy(currentText = v.take(32)) } },
                label = stringResource(R.string.goals_field_current),
                errorText = currentError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            PlanningTextField(
                value = form.unit,
                onValueChange = { v -> onChange { it.copy(unit = v.take(24)) } },
                label = stringResource(R.string.goals_field_unit),
                supportingText = stringResource(R.string.goals_field_unit_hint),
                singleLine = true,
            )
        }
        FormSection(stringResource(R.string.goals_section_dates)) {
            OrbitDateField(
                label = stringResource(R.string.goals_field_start),
                epochDay = form.startDate,
                onChange = { d ->
                    context.postAnalytic(GoalsEvents.GOALS_EDIT_START_DATE_SELECT)
                    onChange { it.copy(startDate = d) }
                },
            )
            OrbitDateField(
                label = stringResource(R.string.goals_field_target_date),
                epochDay = form.targetDate,
                onChange = { d ->
                    context.postAnalytic(GoalsEvents.GOALS_EDIT_TARGET_DATE_SELECT)
                    onChange { it.copy(targetDate = d) }
                },
                errorText = if (state.dateRangeInvalid) stringResource(R.string.goals_error_target_before_start) else null,
            )
        }
        FormSection(stringResource(R.string.goals_field_status)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                GoalStatus.entries.forEach { status ->
                    FilterChip(
                        selected = form.status == status,
                        onClick = {
                            context.postAnalytic(GoalsEvents.GOALS_EDIT_STATUS_SELECT)
                            onChange { it.copy(status = status) }
                        },
                        label = { Text(stringResource(statusLabel(status))) },
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
            modifier = Modifier.fillMaxWidth(),
            brush = Orbit.extras.accentGradient(FeatureAccent.GOALS),
        )
        if (!state.isNew) {
            OrbitWell(
                Modifier.fillMaxWidth().heightIn(min = 52.dp),
                onClick = onDelete,
                shape = RoundedCornerShape(30.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.goals_delete_confirm),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

private fun statusLabel(status: GoalStatus): Int = when (status) {
    GoalStatus.ACTIVE -> R.string.status_active
    GoalStatus.COMPLETED -> R.string.status_completed
    GoalStatus.ARCHIVED -> R.string.status_archived
}
