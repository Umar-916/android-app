package com.funsol.orbit.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.NotesEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.designsystem.ConfirmDialog
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientFab
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel

private sealed interface FolderDialog {
    data object Create : FolderDialog
    data class Rename(val folder: NoteFolderEntity) : FolderDialog
    data class Delete(val folder: NoteFolderEntity) : FolderDialog
}

@Composable
fun NotesScreen(nav: OrbitNavigator) {
    val vm: NotesViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    PostAnalytic(NotesEvents.NOTES_SCREEN_DISPLAY)

    val deletedMessage = stringResource(R.string.notes_note_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val failedMessage = stringResource(R.string.error_generic)
    LaunchedEffect(vm) {
        vm.eventFlow.collect { event ->
            when (event) {
                NotesEvent.ActionFailed -> {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(failedMessage)
                }
                NotesEvent.NoteDeleted -> {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) {
                        context.postAnalytic(NotesEvents.NOTES_UNDO_DELETE_CLICK)
                        vm.undoDelete()
                    }
                }
            }
        }
    }

    var dialog by remember { mutableStateOf<FolderDialog?>(null) }
    var moveNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(vm) { vm.setQuery(query) }
    BackHandler(enabled = state.showArchived) { vm.setShowArchived(false) }

    val currentFolder = (state.filter as? FolderFilter.Folder)?.let { f -> state.folders.firstOrNull { it.id == f.id } }
    val actions = remember(vm, nav) {
        NoteCardActions(
            onOpen = { nav.navigate(AppDestination.NoteEdit(noteId = it.id)) },
            onTogglePin = vm::togglePin,
            onToggleArchive = vm::toggleArchive,
            onMove = { moveNoteId = it.id },
            onDelete = { vm.delete(it.id) },
        )
    }
    val newNote = { nav.navigate(AppDestination.NoteEdit(presetFolderId = (state.filter as? FolderFilter.Folder)?.id)) }

    val gridState = rememberLazyStaggeredGridState()
    val scrolled by remember { derivedStateOf { gridState.canScrollBackward } }

    // The empty state offers "New note" itself, so the FAB is hidden there to keep one
    // create entry per screen. Search-empty and archived views keep their own behavior.
    val emptyActionHandledInState = remember(state.loading, state.error, state.items.isEmpty(), state.appliedQuery, state.filter) {
        !state.loading && !state.error && state.items.isEmpty() && !state.showArchived && state.appliedQuery.isBlank()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            OrbitTopBar(
                scrolled = scrolled,
                title = stringResource(if (state.showArchived) R.string.notes_archived_title else R.string.notes_title),
                onBack = {
                    context.postAnalytic(NotesEvents.NOTES_BACK_CLICK)
                    if (state.showArchived) vm.setShowArchived(false) else nav.back()
                },
                accent = FeatureAccent.NOTES,
                icon = if (state.showArchived) Icons.Rounded.Archive else Icons.AutoMirrored.Rounded.StickyNote2,
                actions = {
                    if (!state.showArchived) {
                        IconButton(onClick = {
                            context.postAnalytic(NotesEvents.NOTES_ARCHIVE_VIEW_CLICK)
                            vm.setShowArchived(true)
                        }) {
                            Icon(Icons.Rounded.Archive, contentDescription = stringResource(R.string.notes_show_archived))
                        }
                    }
                    IconButton(onClick = {
                        context.postAnalytic(NotesEvents.NOTES_MENU_CLICK)
                        menuOpen = true
                    }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more_options))
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.showArchived && !emptyActionHandledInState,
                enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.85f),
                exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.85f),
            ) {
                GradientFab(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.notes_new),
                    onClick = {
                        context.postAnalytic(NotesEvents.NOTES_NEW_FAB_CLICK)
                        newNote()
                    },
                    label = if (scrolled) null else stringResource(R.string.notes_new),
                    brush = Orbit.extras.accentGradient(FeatureAccent.NOTES),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 1000.dp).fillMaxSize()) {
                SearchField(query, onChange = { query = it; vm.setQuery(it) })
                FolderChips(state.folders, state.filter, onSelect = { filter ->
                    context.postAnalytic(
                        when (filter) {
                            FolderFilter.All -> NotesEvents.NOTES_FILTER_ALL_SELECT
                            FolderFilter.NoFolder -> NotesEvents.NOTES_FILTER_NONE_SELECT
                            is FolderFilter.Folder -> NotesEvents.NOTES_FILTER_FOLDER_SELECT
                        },
                    )
                    vm.setFilter(filter)
                })
                when {
                    state.loading -> LoadingState()
                    state.error -> ErrorState(stringResource(R.string.error_generic), onRetry = {
                        context.postAnalytic(NotesEvents.NOTES_RETRY_CLICK)
                        vm.retry()
                    })
                    state.items.isEmpty() -> EmptyContent(state, onCreate = {
                        context.postAnalytic(NotesEvents.NOTES_EMPTY_CREATE_CLICK)
                        newNote()
                    })
                    else -> LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Adaptive(minSize = 168.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = 88.dp),
                        verticalItemSpacing = Spacing.sm + Spacing.xs,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                    ) {
                        items(state.items, key = { it.id }, contentType = { "note" }) { item ->
                            NoteCard(item, actions, Modifier.animateItem())
                        }
                    }
                }
            }
        }
    }

    if (menuOpen) {
        NotesMenuSheet(
            canEditFolder = currentFolder != null,
            onNewFolder = { dialog = FolderDialog.Create },
            onRenameFolder = { currentFolder?.let { dialog = FolderDialog.Rename(it) } },
            onDeleteFolder = { currentFolder?.let { dialog = FolderDialog.Delete(it) } },
            onDismiss = { menuOpen = false },
        )
    }

    when (val d = dialog) {
        FolderDialog.Create -> FolderNameDialog(
            title = stringResource(R.string.notes_folder_new_title),
            confirmLabel = stringResource(R.string.notes_folder_create),
            initialName = "",
            onConfirm = { vm.createFolder(it); dialog = null },
            onDismiss = { dialog = null },
        )
        is FolderDialog.Rename -> FolderNameDialog(
            title = stringResource(R.string.notes_folder_rename_title),
            confirmLabel = stringResource(R.string.notes_folder_rename_confirm),
            initialName = d.folder.name,
            onConfirm = { vm.renameFolder(d.folder.id, it); dialog = null },
            onDismiss = { dialog = null },
        )
        is FolderDialog.Delete -> {
            PostAnalytic(NotesEvents.NOTES_FOLDER_DELETE_DIALOG_APPEAR)
            ConfirmDialog(
                title = stringResource(R.string.notes_folder_delete_title, d.folder.name),
                message = stringResource(R.string.notes_folder_delete_msg),
                confirmLabel = stringResource(R.string.notes_folder_delete_confirm),
                destructive = true,
                onConfirm = {
                    context.postAnalytic(NotesEvents.NOTES_FOLDER_DELETE_CONFIRM_CLICK)
                    vm.deleteFolder(d.folder.id); dialog = null
                },
                onDismiss = {
                    context.postAnalytic(NotesEvents.NOTES_FOLDER_DELETE_CANCEL_CLICK)
                    dialog = null
                },
            )
        }
        null -> Unit
    }

    moveNoteId?.let { id ->
        val current = state.items.firstOrNull { it.id == id }
        FolderPickerDialog(
            folders = state.folders,
            selectedId = current?.folderId,
            onSelect = { vm.moveToFolder(id, it); moveNoteId = null },
            onNewFolder = { moveNoteId = null; dialog = FolderDialog.Create },
            onDismiss = { moveNoteId = null },
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    OrbitWell(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.xs),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        TextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.notes_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Orbit.extras.accent(FeatureAccent.NOTES))
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        context.postAnalytic(NotesEvents.NOTES_SEARCH_CLEAR_CLICK)
                        onChange("")
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        )
    }
}

@Composable
private fun FolderChips(folders: List<NoteFolderEntity>, filter: FolderFilter, onSelect: (FolderFilter) -> Unit) {
    val shape = RoundedCornerShape(Radius.chip)
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "all", contentType = "chip") {
            FilterChip(
                selected = filter == FolderFilter.All,
                onClick = { onSelect(FolderFilter.All) },
                label = { Text(stringResource(R.string.notes_filter_all)) },
                colors = orbitChipColors(),
                shape = shape,
                border = null,
            )
        }
        item(key = "none", contentType = "chip") {
            FilterChip(
                selected = filter == FolderFilter.NoFolder,
                onClick = { onSelect(FolderFilter.NoFolder) },
                label = { Text(stringResource(R.string.notes_filter_none)) },
                colors = orbitChipColors(),
                shape = shape,
                border = null,
            )
        }
        items(folders, key = { "folder_${it.id}" }, contentType = { "chip" }) { folder ->
            FilterChip(
                selected = (filter as? FolderFilter.Folder)?.id == folder.id,
                onClick = { onSelect(FolderFilter.Folder(folder.id)) },
                label = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = orbitChipColors(),
                shape = shape,
                border = null,
            )
        }
    }
}

@Composable
private fun EmptyContent(state: NotesUiState, onCreate: () -> Unit) {
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
        when {
            state.appliedQuery.isNotBlank() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = stringResource(R.string.notes_empty_search_title),
                message = stringResource(R.string.notes_empty_search_msg),
                accent = FeatureAccent.NOTES,
            )
            state.showArchived -> EmptyState(
                icon = Icons.Rounded.Archive,
                title = stringResource(R.string.notes_empty_archive_title),
                message = stringResource(R.string.notes_empty_archive_msg),
                accent = FeatureAccent.NOTES,
            )
            state.filter != FolderFilter.All -> EmptyState(
                icon = Icons.Rounded.Folder,
                title = stringResource(R.string.notes_empty_folder_title),
                message = stringResource(R.string.notes_empty_folder_msg),
                actionLabel = stringResource(R.string.notes_empty_action),
                onAction = onCreate,
                accent = FeatureAccent.NOTES,
            )
            else -> EmptyState(
                icon = Icons.AutoMirrored.Rounded.StickyNote2,
                title = stringResource(R.string.notes_empty_title),
                message = stringResource(R.string.notes_empty_msg),
                actionLabel = stringResource(R.string.notes_empty_action),
                onAction = onCreate,
                accent = FeatureAccent.NOTES,
            )
        }
    }
}
