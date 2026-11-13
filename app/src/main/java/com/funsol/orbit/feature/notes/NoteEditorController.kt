package com.funsol.orbit.feature.notes

import com.funsol.orbit.core.common.newId
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.repository.NoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal val noteSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

enum class SaveStatus { IDLE, SAVING, SAVED }

data class NoteEditorState(
    val loaded: Boolean = false,
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val folderId: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val persisted: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val deleted: Boolean = false,
)

private data class Snapshot(val title: String, val body: String, val folderId: String?, val pinned: Boolean, val archived: Boolean)

private fun NoteEditorState.snapshot() = Snapshot(title, body, folderId, pinned, archived)

class NoteEditorController(
    private val repo: NoteRepository,
    private val scope: CoroutineScope,
    private val debounceMs: Long = 600,
    private val now: () -> Long = System::currentTimeMillis,
    private val idGenerator: () -> String = ::newId,
) {
    private val _state = MutableStateFlow(NoteEditorState())
    val state: StateFlow<NoteEditorState> = _state

    private val mutex = Mutex()
    private var debounceJob: Job? = null
    private var lastSaved: Snapshot? = null

    suspend fun start(noteId: String?, presetFolderId: String?) {
        val existing = noteId?.let { repo.get(it) }
        _state.value = if (existing != null) {
            lastSaved = Snapshot(existing.title, existing.body, existing.folderId, existing.pinned, existing.archived)
            NoteEditorState(
                loaded = true, id = existing.id, title = existing.title, body = existing.body, folderId = existing.folderId,
                pinned = existing.pinned, archived = existing.archived, createdAt = existing.createdAt,
                updatedAt = existing.updatedAt, persisted = true, saveStatus = SaveStatus.IDLE,
            )
        } else {
            NoteEditorState(loaded = true, id = idGenerator(), folderId = presetFolderId)
        }
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }
    fun onBodyChange(value: String) = edit { it.copy(body = value) }
    fun onFolderChange(folderId: String?) = edit { it.copy(folderId = folderId) }
    fun onPinnedChange(pinned: Boolean) = edit { it.copy(pinned = pinned, archived = if (pinned) false else it.archived) }
    fun onArchivedChange(archived: Boolean) = edit { it.copy(archived = archived, pinned = if (archived) false else it.pinned) }

    private fun edit(transform: (NoteEditorState) -> NoteEditorState) {
        var changed = false
        _state.update { s ->
            if (!s.loaded || s.deleted) return@update s
            val next = transform(s)
            changed = next.snapshot() != s.snapshot()
            if (changed) next.copy(saveStatus = SaveStatus.SAVING) else s
        }
        if (changed) scheduleSave()
    }

    private fun scheduleSave() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMs)
            saveSafely(deleteWhenBlank = false)
        }
    }

    fun flush(): Job {
        debounceJob?.cancel()
        return scope.launch { saveSafely(deleteWhenBlank = true) }
    }

    fun delete(): Job {
        debounceJob?.cancel()
        return scope.launch {
            withContext(NonCancellable) {
                mutex.withLock {
                    val s = _state.value
                    _state.update { it.copy(deleted = true) }
                    if (s.persisted) {
                        try {
                            repo.delete(s.id)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
    }

    private suspend fun saveSafely(deleteWhenBlank: Boolean) {
        try {
            withContext(NonCancellable) { saveNow(deleteWhenBlank) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(saveStatus = SaveStatus.IDLE) }
        }
    }

    private suspend fun saveNow(deleteWhenBlank: Boolean) = mutex.withLock {
        val s = _state.value
        if (!s.loaded || s.deleted) return@withLock
        val snap = s.snapshot()
        val blank = s.title.isBlank() && s.body.isBlank()
        if (blank) {
            if (s.persisted && deleteWhenBlank) {
                repo.delete(s.id)
                lastSaved = null
                _state.update { it.copy(persisted = false, createdAt = null, updatedAt = null, saveStatus = SaveStatus.IDLE) }
            } else {
                _state.update { it.copy(saveStatus = SaveStatus.IDLE) }
            }
            return@withLock
        }
        if (s.persisted && snap == lastSaved) {
            _state.update { it.copy(saveStatus = SaveStatus.SAVED) }
            return@withLock
        }
        val timestamp = now()
        val saved = repo.save(
            NoteEntity(
                id = s.id, title = s.title, body = s.body, folderId = s.folderId, pinned = s.pinned, archived = s.archived,
                createdAt = s.createdAt ?: timestamp, updatedAt = timestamp,
            ),
        )
        lastSaved = snap
        _state.update { cur ->
            cur.copy(
                persisted = true,
                createdAt = saved.createdAt,
                updatedAt = saved.updatedAt,
                saveStatus = if (cur.snapshot() == snap) SaveStatus.SAVED else SaveStatus.SAVING,
            )
        }
    }
}
