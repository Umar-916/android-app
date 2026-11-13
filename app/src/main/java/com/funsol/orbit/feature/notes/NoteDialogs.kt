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
import com.funsol.orbit.R
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
    OrbitSheet(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))
        OrbitWell(Modifier.fillMaxWidth()) {
            TextField(
                value = name,
                onValueChange = { name = it.take(60) },
                label = { Text(stringResource(R.string.notes_folder_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) onConfirm(name.trim()) }),
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
            WellButton(stringResource(R.string.action_cancel), onDismiss, Modifier.weight(1f))
            GradientButton(
                confirmLabel,
                onClick = { onConfirm(name.trim()) },
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
    OrbitSheet(onDismiss) {
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
                FolderOption(stringResource(R.string.notes_filter_none), selectedId == null) { onSelect(null) }
            }
            items(folders, key = { it.id }, contentType = { "folder" }) { folder ->
                FolderOption(folder.name, selectedId == folder.id) { onSelect(folder.id) }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WellButton(stringResource(R.string.action_cancel), onDismiss, Modifier.weight(1f))
            GradientButton(
                stringResource(R.string.notes_new_folder_item),
                onNewFolder,
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
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
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
    OrbitSheet(onDismiss) {
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
                ) { onDismiss(); actions.onTogglePin(item) }
            }
            SheetAction(
                if (item.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                FeatureAccent.PROJECTS,
                stringResource(if (item.archived) R.string.action_unarchive else R.string.action_archive),
            ) { onDismiss(); actions.onToggleArchive(item) }
            SheetAction(Icons.Rounded.Folder, FeatureAccent.TASKS, stringResource(R.string.notes_move_to_folder)) {
                onDismiss(); actions.onMove(item)
            }
            SheetAction(Icons.Rounded.Delete, FeatureAccent.CALENDAR, stringResource(R.string.action_delete)) {
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
    OrbitSheet(onDismiss) {
        Text(
            stringResource(R.string.action_more_options),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Column2 {
            SheetAction(Icons.Rounded.CreateNewFolder, FeatureAccent.NOTES, stringResource(R.string.notes_menu_new_folder)) {
                onDismiss(); onNewFolder()
            }
            if (canEditFolder) {
                SheetAction(Icons.Rounded.DriveFileRenameOutline, FeatureAccent.PROJECTS, stringResource(R.string.notes_menu_rename_folder)) {
                    onDismiss(); onRenameFolder()
                }
                SheetAction(Icons.Rounded.Delete, FeatureAccent.CALENDAR, stringResource(R.string.notes_menu_delete_folder)) {
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
