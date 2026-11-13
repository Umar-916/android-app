package com.funsol.orbit.feature.habits

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.common.rememberClickGuard
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
import com.funsol.orbit.core.designsystem.orbitSwitchColors
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.feature.projects.CenteredContent
import com.funsol.orbit.feature.projects.ColorPickerRow
import com.funsol.orbit.feature.projects.DiscardChangesDialog
import com.funsol.orbit.feature.projects.FormSection
import com.funsol.orbit.feature.projects.OrbitDateField
import com.funsol.orbit.feature.projects.PlanningTextField
import com.funsol.orbit.feature.projects.toGradient
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.DayOfWeek

@Composable
fun HabitEditScreen(habitId: String?, nav: OrbitNavigator) {
    val vm: HabitEditViewModel = koinViewModel(key = "habit_edit_$habitId", parameters = { parametersOf(habitId) })
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
                title = stringResource(if (state.isNew) R.string.habits_edit_new_title else R.string.habits_edit_title),
                onBack = requestBack,
                accent = FeatureAccent.HABITS,
            ) {
                TextButton(onClick = vm::save, enabled = !state.saving && !state.loading && !state.loadError) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            when {
                state.loading -> LoadingState()
                state.loadError -> ErrorState(stringResource(R.string.error_generic), onRetry = vm::load)
                state.notFound -> ErrorState(stringResource(R.string.habits_not_found))
                else -> HabitFormContent(
                    state = state,
                    onChange = vm::update,
                    onPreset = vm::selectPreset,
                    onToggleDay = vm::toggleDay,
                    onSave = vm::save,
                    onDelete = { showDelete = true },
                )
            }
        }
    }

    if (showDiscard) {
        DiscardChangesDialog(onDiscard = { showDiscard = false; guard.run(nav::back) }, onKeepEditing = { showDiscard = false })
    }
    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.habits_delete_title, state.initial.name),
            message = stringResource(R.string.habits_delete_message, state.initial.name),
            confirmLabel = stringResource(R.string.habits_delete_confirm),
            onConfirm = { showDelete = false; guard.run(vm::delete) },
            onDismiss = { showDelete = false },
            destructive = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitFormContent(
    state: HabitEditUiState,
    onChange: ((HabitForm) -> HabitForm) -> Unit,
    onPreset: (SchedulePreset) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val form = state.form
    val nameError = state.showErrors && state.nameInvalid
    val accent = Color(form.color)
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        FormSection(stringResource(R.string.habits_section_details)) {
            PlanningTextField(
                value = form.name,
                onValueChange = { v -> onChange { it.copy(name = v) } },
                label = stringResource(R.string.habits_field_name),
                errorText = if (nameError) stringResource(R.string.habits_error_name_required) else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            PlanningTextField(
                value = form.description,
                onValueChange = { v -> onChange { it.copy(description = v) } },
                label = stringResource(R.string.habits_field_description),
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }
        FormSection(stringResource(R.string.habits_section_appearance)) {
            ColorPickerRow(selected = form.color, onSelect = { c -> onChange { it.copy(color = c) } })
        }
        FormSection(stringResource(R.string.habits_section_schedule)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(R.string.habits_field_schedule),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SchedulePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.selectedPreset == preset,
                            onClick = { onPreset(preset) },
                            label = { Text(stringResource(presetLabel(preset))) },
                            colors = orbitChipColors(),
                            border = null,
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val circle = (maxWidth / 7 - 4.dp).coerceIn(28.dp, 40.dp)
                    Row(Modifier.fillMaxWidth()) {
                        DayOfWeek.entries.forEach { day ->
                            WeekdayCircle(
                                day = day,
                                selected = isDayScheduled(form.scheduleMask, day),
                                color = accent,
                                circle = circle,
                                onClick = { onToggleDay(day) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                if (state.showErrors && state.scheduleInvalid) {
                    Text(
                        stringResource(R.string.habits_error_schedule_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OrbitDateField(
                label = stringResource(R.string.habits_field_start),
                epochDay = form.startDate,
                onChange = { d -> if (d != null) onChange { it.copy(startDate = d) } },
            )
        }

        if (!state.isNew) {
            FormSection(stringResource(R.string.habits_section_status)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = form.archived,
                            role = Role.Switch,
                            onValueChange = { v -> onChange { it.copy(archived = v) } },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Column(Modifier.weight(1f).padding(vertical = Spacing.xs)) {
                        Text(stringResource(R.string.habits_field_archive), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.habits_field_archive_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Orbit.extras.textMuted,
                        )
                    }
                    Switch(checked = form.archived, onCheckedChange = null, colors = orbitSwitchColors())
                }
            }
        }

        GradientButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth(),
            brush = accent.toGradient(),
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
                        stringResource(R.string.habits_delete_confirm),
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

@Composable
private fun WeekdayCircle(
    day: DayOfWeek,
    selected: Boolean,
    color: Color,
    circle: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "weekdayFill")
    val full = weekdayFullName(day)
    val letter = stringResource(weekdayShortRes(day)).take(1)
    Box(
        modifier
            .heightIn(min = Spacing.touchTarget)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics { contentDescription = full },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(circle)
                .clip(CircleShape)
                .background(Orbit.extras.well)
                .border(1.dp, if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = fill }.background(color.toGradient()))
            Text(
                letter,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

private fun presetLabel(preset: SchedulePreset): Int = when (preset) {
    SchedulePreset.DAILY -> R.string.habits_preset_daily
    SchedulePreset.WEEKDAYS -> R.string.habits_preset_weekdays
    SchedulePreset.WEEKENDS -> R.string.habits_preset_weekends
    SchedulePreset.CUSTOM -> R.string.habits_preset_custom
}
