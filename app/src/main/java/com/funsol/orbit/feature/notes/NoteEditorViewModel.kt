package com.funsol.orbit.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.repository.NoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteEditorViewModel(
    private val repo: NoteRepository,
    noteId: String?,
    presetFolderId: String?,
) : ViewModel() {
    private val controller = NoteEditorController(repo, noteSaveScope)

    val state: StateFlow<NoteEditorState> = controller.state

    val folders: StateFlow<List<NoteFolderEntity>> = repo.observeFolders()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            try {
                controller.start(noteId, presetFolderId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                controller.start(null, presetFolderId)
            }
        }
    }

    fun onTitleChange(value: String) = controller.onTitleChange(value)
    fun onBodyChange(value: String) = controller.onBodyChange(value)
    fun onFolderChange(folderId: String?) = controller.onFolderChange(folderId)
    fun onPinnedChange(pinned: Boolean) = controller.onPinnedChange(pinned)
    fun onArchivedChange(archived: Boolean) = controller.onArchivedChange(archived)

    fun flush() {
        controller.flush()
    }

    fun delete() {
        controller.delete()
    }

    fun createFolderAndSelect(name: String) {
        noteSaveScope.launch {
            try {
                repo.createFolder(name)?.let { controller.onFolderChange(it.id) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
    }

    override fun onCleared() {
        controller.flush()
    }
}
