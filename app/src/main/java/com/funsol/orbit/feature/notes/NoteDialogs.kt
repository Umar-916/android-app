package com.funsol.orbit.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.NotesEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton

@Composable
fun FolderNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    val valid = name.isNotBlank()
    val context = LocalContext.current
    val confirm = { context.postAnalytic(NotesEvents.NOTES_FOLDER_CONFIRM_CLICK); onConfirm(name.trim()) }
    val cancel = { context.postAnalytic(NotesEvents.NOTES_FOLDER_CANCEL_CLICK); onDismiss() }
    OrbitSheet(onDismiss) {
        PostAnalytic(NotesEvents.NOTES_FOLDER_DIALOG_APPEAR)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))
        OrbitWell(Modifier.fillMaxWidth()) {
            TextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text(stringResource(R.string.notes_folder_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) confirm() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WellButton(stringResource(R.string.action_cancel), cancel, Modifier.weight(1f))
            GradientButton(
                confirmLabel,
                onClick = confirm,
                modifier = Modifier.weight(1f),
                enabled = valid,
                brush = Orbit.extras.accentGradient(FeatureAccent.NOTES),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
fun FolderPickerDialog(
    folders: List<NoteFolderEntity>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onNewFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    OrbitSheet(onDismiss) {
        PostAnalytic(NotesEvents.NOTES_PICKER_DIALOG_APPEAR)
        Text(
            stringResource(R.string.notes_move_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "none", contentType = "folder") {
                FolderOption(stringResource(R.string.notes_filter_none), selectedId == null) {
                    context.postAnalytic(NotesEvents.NOTES_PICKER_SELECT)
                    onSelect(null)
                }
            }
            items(folders, key = { it.id }, contentType = { "folder" }) { folder ->
                FolderOption(folder.name, selectedId == folder.id) {
                    context.postAnalytic(NotesEvents.NOTES_PICKER_SELECT)
                    onSelect(folder.id)
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WellButton(
                stringResource(R.string.action_cancel),
                { context.postAnalytic(NotesEvents.NOTES_PICKER_CANCEL_CLICK); onDismiss() },
                Modifier.weight(1f),
            )
            GradientButton(
                stringResource(R.string.notes_new_folder_item),
                { context.postAnalytic(NotesEvents.NOTES_PICKER_NEW_FOLDER_CLICK); onNewFolder() },
                Modifier.weight(1f),
                icon = Icons.Rounded.CreateNewFolder,
                brush = Orbit.extras.accentGradient(FeatureAccent.NOTES),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
private fun FolderOption(label: String, selected: Boolean, onClick: () -> Unit) {
    OrbitWell(
        Modifier
            .fillMaxWidth()
            // Clip the ripple to the well shape: the indication is applied on the outer
            // modifier chain, before OrbitWell's own clip, so it would otherwise be square.
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        selected = selected,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Orbit.extras.accent(FeatureAccent.NOTES))
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, accent: FeatureAccent, label: String, onClick: () -> Unit) {
    OrbitWell(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
        ) {
            IconTile(icon, accent, size = 36.dp)
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun NoteActionsSheet(item: NoteItem, displayTitle: String, actions: NoteCardActions, onDismiss: () -> Unit) {
    val context = LocalContext.current
    OrbitSheet(onDismiss) {
        PostAnalytic(NotesEvents.NOTES_ACTIONS_SHEET_APPEAR)
        Text(
            displayTitle,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Column2 {
            if (!item.archived) {
                SheetAction(
                    Icons.Rounded.PushPin,
                    FeatureAccent.NOTES,
                    stringResource(if (item.pinned) R.string.notes_unpin else R.string.notes_pin),
                ) {
                    context.postAnalytic(NotesEvents.NOTES_ACTION_PIN_CLICK)
                    onDismiss(); actions.onTogglePin(item)
                }
            }
            SheetAction(
                if (item.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                FeatureAccent.PROJECTS,
                stringResource(if (item.archived) R.string.action_unarchive else R.string.action_archive),
            ) {
                context.postAnalytic(NotesEvents.NOTES_ACTION_ARCHIVE_CLICK)
                onDismiss(); actions.onToggleArchive(item)
            }
            SheetAction(Icons.Rounded.Folder, FeatureAccent.TASKS, stringResource(R.string.notes_move_to_folder)) {
                context.postAnalytic(NotesEvents.NOTES_ACTION_MOVE_CLICK)
                onDismiss(); actions.onMove(item)
            }
            SheetAction(Icons.Rounded.Delete, FeatureAccent.CALENDAR, stringResource(R.string.action_delete)) {
                context.postAnalytic(NotesEvents.NOTES_ACTION_DELETE_CLICK)
                onDismiss(); actions.onDelete(item)
            }
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
fun NotesMenuSheet(
    canEditFolder: Boolean,
    onNewFolder: () -> Unit,
    onRenameFolder: () -> Unit,
    onDeleteFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    OrbitSheet(onDismiss) {
        PostAnalytic(NotesEvents.NOTES_MENU_SHEET_APPEAR)
        Text(
            stringResource(R.string.action_more_options),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Column2 {
            SheetAction(Icons.Rounded.CreateNewFolder, FeatureAccent.NOTES, stringResource(R.string.notes_menu_new_folder)) {
                context.postAnalytic(NotesEvents.NOTES_MENU_NEW_FOLDER_CLICK)
                onDismiss(); onNewFolder()
            }
            if (canEditFolder) {
                SheetAction(Icons.Rounded.DriveFileRenameOutline, FeatureAccent.PROJECTS, stringResource(R.string.notes_menu_rename_folder)) {
                    context.postAnalytic(NotesEvents.NOTES_MENU_RENAME_FOLDER_CLICK)
                    onDismiss(); onRenameFolder()
                }
                SheetAction(Icons.Rounded.Delete, FeatureAccent.CALENDAR, stringResource(R.string.notes_menu_delete_folder)) {
                    context.postAnalytic(NotesEvents.NOTES_MENU_DELETE_FOLDER_CLICK)
                    onDismiss(); onDeleteFolder()
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
private fun Column2(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm), content = content)
}
