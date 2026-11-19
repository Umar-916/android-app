package com.funsol.orbit.feature.tasks

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.TasksEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.TaskStatus
import com.funsol.orbit.core.designsystem.ConfirmDialog
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitFieldLabel
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.designsystem.priorityColor
import com.funsol.orbit.core.designsystem.priorityLabel
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TaskEditScreen(taskId: String?, presetProjectId: String?, presetDate: Int?, nav: OrbitNavigator) {
    PostAnalytic(TasksEvents.TASK_EDIT_SCREEN_DISPLAY)
    val vm: TaskEditViewModel = koinViewModel(
        key = "task_edit_${taskId}_${presetProjectId}_$presetDate",
        parameters = { parametersOf(taskId, presetProjectId, presetDate) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbar = remember { SnackbarHostState() }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var permissionTick by remember { mutableIntStateOf(0) }

    val guard = rememberClickGuard()
    val withInterstitial = com.funsol.orbit.core.ads.rememberNaturalTransitionAd()
    val leave = rememberUpdatedState { withInterstitial { nav.back() } }
    LaunchedEffect(vm) { vm.closeEvents.collect { leave.value() } }

    val duplicateMsg = stringResource(R.string.tasks_error_duplicate)
    val saveFailedMsg = stringResource(R.string.tasks_error_save)
    LaunchedEffect(state.saveError) {
        when (state.saveError) {
            SaveError.DUPLICATE_OCCURRENCE -> snackbar.showSnackbar(duplicateMsg)
            SaveError.GENERIC -> snackbar.showSnackbar(saveFailedMsg)
            null -> Unit
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionTick++
    }
    val notificationsOff = permissionTick >= 0 && !NotificationManagerCompat.from(context).areNotificationsEnabled()

    val requestBack: () -> Unit = {
        context.postAnalytic(TasksEvents.TASK_EDIT_BACK_CLICK)
        if (state.dirty) {
            showDiscard = true
        } else {
            guard.run { nav.back() }
        }
    }
    BackHandler(enabled = state.dirty) { context.postAnalytic(TasksEvents.TASK_EDIT_BACK_CLICK); showDiscard = true }

    val canEdit = !state.loading && !state.notFound && !state.loadFailed

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                title = stringResource(if (state.isNew) R.string.tasks_edit_new_title else R.string.tasks_edit_title),
                onBack = requestBack,
                accent = FeatureAccent.TASKS,
                icon = Icons.Rounded.EditNote,
                actions = {
                    if (!state.isNew && canEdit) {
                        OverflowMenu(
                            enabled = !state.saving,
                            onArchive = { context.postAnalytic(TasksEvents.TASK_EDIT_ARCHIVE_CLICK); vm.archive() },
                            onDelete = { context.postAnalytic(TasksEvents.TASK_EDIT_DELETE_CLICK); showDelete = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (canEdit) {
                SaveBar(saving = state.saving, onSave = { context.postAnalytic(TasksEvents.TASK_EDIT_SAVE_CLICK); vm.save() })
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                state.loading -> LoadingState()
                state.notFound -> ErrorState(stringResource(R.string.tasks_not_found))
                state.loadFailed -> ErrorState(stringResource(R.string.tasks_error_load), onRetry = { context.postAnalytic(TasksEvents.TASK_EDIT_RETRY_CLICK); vm.load() })
                else -> TaskFormContent(
                    state = state,
                    vm = vm,
                    notificationsOff = notificationsOff,
                    onReminder = { offset ->
                        vm.onReminderChange(offset)
                        if (offset != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
    }

    if (showDiscard) {
        PostAnalytic(TasksEvents.TASK_EDIT_DISCARD_DIALOG_APPEAR)
        ConfirmDialog(
            title = stringResource(R.string.tasks_discard_title),
            message = stringResource(R.string.tasks_discard_message),
            confirmLabel = stringResource(R.string.action_discard),
            destructive = true,
            dismissLabel = stringResource(R.string.action_keep_editing),
            onConfirm = { context.postAnalytic(TasksEvents.TASK_EDIT_DISCARD_CONFIRM_CLICK); showDiscard = false; guard.run { nav.back() } },
            onDismiss = { context.postAnalytic(TasksEvents.TASK_EDIT_DISCARD_KEEP_CLICK); showDiscard = false },
        )
    }
    if (showDelete) {
        PostAnalytic(TasksEvents.TASK_EDIT_DELETE_DIALOG_APPEAR)
        ConfirmDialog(
            title = stringResource(R.string.tasks_delete_title),
            message = stringResource(R.string.tasks_delete_message, state.originalTitle),
            confirmLabel = stringResource(R.string.tasks_delete_confirm),
            destructive = true,
            onConfirm = { context.postAnalytic(TasksEvents.TASK_EDIT_DELETE_CONFIRM_CLICK); showDelete = false; vm.delete() },
            onDismiss = { context.postAnalytic(TasksEvents.TASK_EDIT_DELETE_CANCEL_CLICK); showDelete = false },
        )
    }
}

@Composable
private fun OverflowMenu(enabled: Boolean, onArchive: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_MENU_CLICK); expanded = true }, enabled = enabled) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_archive)) },
                leadingIcon = { Icon(Icons.Rounded.Archive, null) },
                onClick = { expanded = false; onArchive() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() },
            )
        }
    }
}

@Composable
private fun SaveBar(saving: Boolean, onSave: () -> Unit) {
    val extras = Orbit.extras
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(extras.cardStrokeWidth, extras.cardStroke)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.md, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        GradientButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            brush = extras.accentGradient(FeatureAccent.TASKS),
        )
    }
}

@Composable
private fun TaskFormContent(
    state: TaskEditUiState,
    vm: TaskEditViewModel,
    notificationsOff: Boolean,
    onReminder: (Int?) -> Unit,
) {
    val context = LocalContext.current
    val form = state.form
    var titleFocused by remember { mutableStateOf(false) }
    var titleWasFocused by remember { mutableStateOf(false) }

    Column(
        Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.section),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        FormSection(stringResource(R.string.tasks_section_details), Icons.Rounded.EditNote, FeatureAccent.TASKS) {
            OutlinedTextField(
                value = form.title,
                onValueChange = vm::onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        titleFocused = it.isFocused
                        if (titleWasFocused && !it.isFocused) vm.onTitleBlur()
                        if (it.isFocused) titleWasFocused = true
                    },
                shape = RoundedCornerShape(16.dp),
                colors = orbitFieldColors(),
                label = { OrbitFieldLabel(stringResource(R.string.tasks_field_title)) },
                singleLine = true,
                isError = state.titleError != null,
                supportingText = {
                    val err = state.titleError
                    if (err != null) {
                        Text(
                            when (err) {
                                TitleError.REQUIRED -> stringResource(R.string.tasks_error_title_required)
                                TitleError.TOO_LONG -> stringResource(R.string.tasks_error_title_long, TaskEditViewModel.MAX_TITLE)
                            },
                        )
                    } else if (titleFocused) {
                        Text(stringResource(R.string.tasks_title_counter, form.title.length, TaskEditViewModel.MAX_TITLE))
                    }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = vm::onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = orbitFieldColors(),
                label = { OrbitFieldLabel(stringResource(R.string.tasks_field_description)) },
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        }

        PrioritySection(form.priority, form.status, vm::onPriorityChange, vm::onStatusChange)

        ScheduleSection(form = form, onDate = vm::onDueDateChange, onTime = vm::onDueTimeChange)

        ReminderSection(form = form, notificationsOff = notificationsOff, onReminder = onReminder)

        ProjectSection(form, state.projects, vm::onProjectChange)

        SubtasksSection(
            form = form,
            onAdd = vm::addSubtask,
            onToggle = vm::toggleSubtask,
            onDelete = vm::deleteSubtask,
            onMove = vm::moveSubtask,
        )

        RecurrenceSection(
            form = form,
            showOccurrenceHint = state.isOccurrenceOfSeries,
            onType = vm::onRecurrenceTypeChange,
            onInterval = vm::onIntervalChange,
            onWeekday = vm::onWeekdayToggle,
            onEnd = vm::onRecurrenceEndChange,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrioritySection(
    priority: Priority,
    status: TaskStatus,
    onPriority: (Priority) -> Unit,
    onStatus: (TaskStatus) -> Unit,
) {
    val context = LocalContext.current
    val roomy = LocalDensity.current.fontScale <= 1.3f
    val rows = if (roomy) listOf(Priority.entries) else Priority.entries.chunked(2)

    FormSection(stringResource(R.string.tasks_field_priority), Icons.Rounded.Flag, FeatureAccent.GOALS) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    row.forEach { p ->
                        PriorityOption(p, selected = p == priority, onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_PRIORITY_SELECT); onPriority(p) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Text(
            stringResource(R.string.tasks_field_status),
            style = MaterialTheme.typography.labelMedium,
            color = Orbit.extras.textMuted,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            TaskStatus.entries.forEach { s ->
                FilterChip(
                    selected = s == status,
                    onClick = { context.postAnalytic(TasksEvents.TASK_EDIT_STATUS_SELECT); onStatus(s) },
                    label = { Text(statusLabel(s)) },
                    colors = orbitChipColors(),
                    shape = RoundedCornerShape(Radius.chip),
                )
            }
        }
    }
}

@Composable
private fun PriorityOption(priority: Priority, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = priorityColor(priority)
    val shape = RoundedCornerShape(16.dp)
    val border = if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.16f) else Orbit.extras.well)
            .border(if (selected) 1.5.dp else 0.6.dp, border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(Icons.Rounded.Flag, contentDescription = null, tint = color)
        Text(
            priorityLabel(priority),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
