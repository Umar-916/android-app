package com.funsol.orbit.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.NotesEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.designsystem.ConfirmDialog
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.TagPill
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NoteEditScreen(noteId: String?, presetFolderId: String?, nav: OrbitNavigator) {
    val vm: NoteEditorViewModel = koinViewModel(
        key = "note_${noteId ?: "new"}_${presetFolderId ?: "none"}",
        parameters = { parametersOf(noteId, presetFolderId) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()

    val context = LocalContext.current
    PostAnalytic(NotesEvents.NOTE_EDIT_SCREEN_DISPLAY)

    var showDelete by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }

    fun leave() {
        vm.flush()
        nav.back()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.flush() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flush() }
    BackHandler { leave() }
    LaunchedEffect(state.deleted) { if (state.deleted) nav.back() }

    val untitled = stringResource(R.string.notes_untitled)
    val folderName = folders.firstOrNull { it.id == state.folderId }?.name

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                title = stringResource(R.string.notes_title),
                onBack = {
                    context.postAnalytic(NotesEvents.NOTE_EDIT_BACK_CLICK)
                    leave()
                },
                accent = FeatureAccent.NOTES,
                icon = Icons.AutoMirrored.Rounded.StickyNote2,
                actions = {
                    SaveIndicator(state.saveStatus)
                    Spacer(Modifier.size(Spacing.sm))
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (!state.loaded) {
                LoadingState()
            } else {
                Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                    EditorToolbar(
                        pinned = state.pinned,
                        archived = state.archived,
                        onPin = {
                            context.postAnalytic(NotesEvents.NOTE_EDIT_PIN_CLICK)
                            vm.onPinnedChange(!state.pinned)
                        },
                        onArchive = {
                            context.postAnalytic(NotesEvents.NOTE_EDIT_ARCHIVE_CLICK)
                            vm.onArchivedChange(!state.archived)
                        },
                        onFolder = {
                            context.postAnalytic(NotesEvents.NOTE_EDIT_FOLDER_CLICK)
                            showPicker = true
                        },
                        onDelete = {
                            context.postAnalytic(NotesEvents.NOTE_EDIT_DELETE_CLICK)
                            showDelete = true
                        },
                    )
                    EditorBody(
                        initialTitle = state.title,
                        initialBody = state.body,
                        isNew = !state.persisted,
                        folderName = folderName,
                        createdAt = state.createdAt,
                        updatedAt = state.updatedAt,
                        onTitleChange = vm::onTitleChange,
                        onBodyChange = vm::onBodyChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (showDelete) {
        PostAnalytic(NotesEvents.NOTE_DELETE_DIALOG_APPEAR)
        ConfirmDialog(
            title = stringResource(R.string.note_delete_title),
            message = stringResource(R.string.note_delete_msg, state.title.trim().ifEmpty { untitled }),
            confirmLabel = stringResource(R.string.note_delete_confirm),
            destructive = true,
            onConfirm = {
                context.postAnalytic(NotesEvents.NOTE_DELETE_CONFIRM_CLICK)
                showDelete = false; vm.delete()
            },
            onDismiss = {
                context.postAnalytic(NotesEvents.NOTE_DELETE_CANCEL_CLICK)
                showDelete = false
            },
        )
    }
    if (showPicker) {
        FolderPickerDialog(
            folders = folders,
            selectedId = state.folderId,
            onSelect = { vm.onFolderChange(it); showPicker = false },
            onNewFolder = { showPicker = false; showNewFolder = true },
            onDismiss = { showPicker = false },
        )
    }
    if (showNewFolder) {
        FolderNameDialog(
            title = stringResource(R.string.notes_folder_new_title),
            confirmLabel = stringResource(R.string.notes_folder_create),
            initialName = "",
            onConfirm = { vm.createFolderAndSelect(it); showNewFolder = false },
            onDismiss = { showNewFolder = false },
        )
    }
}

@Composable
private fun SaveIndicator(status: SaveStatus) {
    val green = Orbit.extras.accent(FeatureAccent.HABITS)
    val amber = Orbit.extras.accent(FeatureAccent.NOTES)
    AnimatedVisibility(visible = status != SaveStatus.IDLE, enter = fadeIn(), exit = fadeOut()) {
        val saved = status == SaveStatus.SAVED
        val tint = if (saved) green else amber
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(tint.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (saved) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            } else {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp, color = tint)
            }
            Text(
                stringResource(if (saved) R.string.note_saved else R.string.note_saving),
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    pinned: Boolean,
    archived: Boolean,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onFolder: () -> Unit,
    onDelete: () -> Unit,
) {
    val extras = Orbit.extras
    val amber = extras.accent(FeatureAccent.NOTES)
    OrbitWell(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.xs), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            ToolbarButton(
                icon = Icons.Rounded.PushPin,
                description = stringResource(if (pinned) R.string.notes_unpin else R.string.notes_pin),
                tint = if (pinned) amber else MaterialTheme.colorScheme.onSurfaceVariant,
                highlighted = pinned,
                highlightColor = amber,
                onClick = onPin,
            )
            ToolbarButton(
                icon = if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                description = stringResource(if (archived) R.string.action_unarchive else R.string.action_archive),
                tint = extras.accent(FeatureAccent.PROJECTS),
                highlighted = archived,
                highlightColor = extras.accent(FeatureAccent.PROJECTS),
                onClick = onArchive,
            )
            ToolbarButton(
                icon = Icons.Rounded.Folder,
                description = stringResource(R.string.note_choose_folder),
                tint = extras.accent(FeatureAccent.TASKS),
                highlighted = false,
                highlightColor = extras.accent(FeatureAccent.TASKS),
                onClick = onFolder,
            )
            ToolbarButton(
                icon = Icons.Rounded.Delete,
                description = stringResource(R.string.note_delete_action),
                tint = MaterialTheme.colorScheme.error,
                highlighted = false,
                highlightColor = MaterialTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    highlighted: Boolean,
    highlightColor: Color,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(Spacing.touchTarget)
            .clip(CircleShape)
            .background(if (highlighted) highlightColor.copy(alpha = 0.18f) else Color.Transparent),
    ) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

@Composable
private fun EditorBody(
    initialTitle: String,
    initialBody: String,
    isNew: Boolean,
    folderName: String?,
    createdAt: Long?,
    updatedAt: Long?,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initialTitle)) }
    var body by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initialBody)) }
    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (isNew && initialTitle.isEmpty() && initialBody.isEmpty()) titleFocus.requestFocus()
    }

    Column(
        modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md + Spacing.xs, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val textColor = MaterialTheme.colorScheme.onSurface
        val hintColor = Orbit.extras.textMuted
        val cursor = SolidColor(Orbit.extras.accent(FeatureAccent.NOTES))

        BasicTextField(
            value = title,
            onValueChange = { new ->
                if (new.text.contains('\n')) {
                    val cleaned = new.text.replace("\n", " ")
                    title = new.copy(text = cleaned)
                    onTitleChange(cleaned)
                    bodyFocus.requestFocus()
                } else {
                    title = new
                    onTitleChange(new.text)
                }
            },
            textStyle = MaterialTheme.typography.titleLarge.copy(color = textColor),
            cursorBrush = cursor,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().focusRequester(titleFocus).semantics { heading() },
            decorationBox = { inner ->
                Box {
                    if (title.text.isEmpty()) {
                        Text(stringResource(R.string.note_edit_title_hint), style = MaterialTheme.typography.titleLarge, color = hintColor)
                    }
                    inner()
                }
            },
        )

        MetaRow(folderName, createdAt, updatedAt)

        BasicTextField(
            value = body,
            onValueChange = { new ->
                body = new
                onBodyChange(new.text)
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
            cursorBrush = cursor,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp).focusRequester(bodyFocus),
            decorationBox = { inner ->
                Box {
                    if (body.text.isEmpty()) {
                        Text(stringResource(R.string.note_edit_body_hint), style = MaterialTheme.typography.bodyLarge, color = hintColor)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun MetaRow(folderName: String?, createdAt: Long?, updatedAt: Long?) {
    if (folderName == null && createdAt == null && updatedAt == null) return
    val muted = Orbit.extras.textMuted
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (folderName != null) {
            TagPill(folderName, Orbit.extras.accent(FeatureAccent.NOTES))
        }
        if (createdAt != null) {
            Text(
                stringResource(R.string.note_created, DateFormats.dateTime(createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
        if (updatedAt != null) {
            Text(
                stringResource(R.string.notes_modified, DateFormats.dateTime(updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}
