package com.funsol.orbit.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.repository.NoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val items: List<NoteItem> = emptyList(),
    val folders: List<NoteFolderEntity> = emptyList(),
    val filter: FolderFilter = FolderFilter.All,
    val showArchived: Boolean = false,
    val appliedQuery: String = "",
    val archivedCount: Int = 0,
)

sealed interface NotesEvent {
    data object NoteDeleted : NotesEvent
    data object ActionFailed : NotesEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repo: NoteRepository) : ViewModel() {
    private val filter = MutableStateFlow<FolderFilter>(FolderFilter.All)
    private val showArchived = MutableStateFlow(false)
    private val query = MutableStateFlow("")
    private val retryTrigger = MutableStateFlow(0)

    private val events = Channel<NotesEvent>(Channel.BUFFERED)
    val eventFlow: Flow<NotesEvent> = events.receiveAsFlow()

    @Volatile private var lastDeleted: NoteEntity? = null

    private val debouncedQuery: Flow<String> = query.debounce { if (it.isEmpty()) 0L else 250L }

    val state: StateFlow<NotesUiState> = retryTrigger.flatMapLatest { combine(
        repo.observeAll(),
        repo.observeFolders(),
        filter,
        showArchived,
        debouncedQuery,
    ) { notes, folders, f, archived, q ->
        NotesUiState(
            loading = false,
            items = buildNoteItems(notes, folders, f, archived, q),
            folders = folders,
            filter = f,
            showArchived = archived,
            appliedQuery = q,
            archivedCount = notes.count { it.archived },
        )
        }
            .flowOn(Dispatchers.Default)
            .catch { emit(NotesUiState(loading = false, error = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                events.trySend(NotesEvent.ActionFailed)
            }
        }
    }

    fun retry() {
        retryTrigger.value += 1
    }

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: FolderFilter) { filter.value = value }
    fun setShowArchived(value: Boolean) { showArchived.value = value }

    fun togglePin(item: NoteItem) {
        launchSafely { repo.setPinned(item.id, !item.pinned) }
    }

    fun toggleArchive(item: NoteItem) {
        launchSafely { repo.setArchived(item.id, !item.archived) }
    }

    fun moveToFolder(noteId: String, folderId: String?) {
        launchSafely {
            val note = repo.get(noteId) ?: return@launchSafely
            if (note.folderId != folderId) repo.save(note.copy(folderId = folderId))
        }
    }

    fun delete(noteId: String) {
        launchSafely {
            val note = repo.get(noteId) ?: return@launchSafely
            lastDeleted = note
            repo.delete(noteId)
            events.send(NotesEvent.NoteDeleted)
        }
    }

    fun undoDelete() {
        val note = lastDeleted ?: return
        lastDeleted = null
        launchSafely { repo.save(note) }
    }

    fun createFolder(name: String) {
        launchSafely { repo.createFolder(name)?.let { filter.value = FolderFilter.Folder(it.id) } }
    }

    fun renameFolder(id: String, name: String) {
        launchSafely { repo.renameFolder(id, name) }
    }

    fun deleteFolder(id: String) {
        launchSafely {
            repo.deleteFolder(id)
            if ((filter.value as? FolderFilter.Folder)?.id == id) filter.value = FolderFilter.All
        }
    }
}
