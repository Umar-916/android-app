package com.funsol.orbit.feature.notes

import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import com.funsol.orbit.core.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeNoteRepository(var saveDelayMs: Long = 0) : NoteRepository {
    val notes = LinkedHashMap<String, NoteEntity>()
    var saveCount = 0
    var deleteCount = 0

    override fun observeAll(): Flow<List<NoteEntity>> = flowOf(notes.values.toList())
    override fun observeFolders(): Flow<List<NoteFolderEntity>> = flowOf(emptyList())
    override suspend fun get(id: String): NoteEntity? = notes[id]

    override suspend fun save(note: NoteEntity): NoteEntity {
        if (saveDelayMs > 0) delay(saveDelayMs)
        saveCount++
        val stored = note.copy(createdAt = notes[note.id]?.createdAt ?: note.createdAt)
        notes[stored.id] = stored
        return stored
    }

    override suspend fun delete(id: String) {
        deleteCount++
        notes.remove(id)
    }

    override suspend fun setPinned(id: String, pinned: Boolean) = Unit
    override suspend fun setArchived(id: String, archived: Boolean) = Unit
    override suspend fun createFolder(name: String): NoteFolderEntity? = null
    override suspend fun renameFolder(id: String, name: String) = Unit
    override suspend fun deleteFolder(id: String) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorControllerTest {
    private fun TestScope.controller(repo: NoteRepository) =
        NoteEditorController(repo, this, debounceMs = 600, now = { 1_000L + testScheduler.currentTime }, idGenerator = { "note-1" })

    @Test
    fun emptyUntouchedNoteIsNeverPersisted() = runTest {
        val repo = FakeNoteRepository()
        val c = controller(repo)
        c.start(null, null)
        c.flush().join()
        advanceUntilIdle()
        assertTrue(repo.notes.isEmpty())
        assertEquals(0, repo.saveCount)
    }

    @Test
    fun whitespaceOnlyNoteIsNeverPersisted() = runTest {
        val repo = FakeNoteRepository()
        val c = controller(repo)
        c.start(null, null)
        c.onBodyChange("   \n ")
        c.flush().join()
        assertTrue(repo.notes.isEmpty())
    }

    @Test
    fun rapidTypingIsCoalescedIntoOneSave() = runTest {
        val repo = FakeNoteRepository()
        val c = controller(repo)
        c.start(null, null)
        c.onBodyChange("a")
        advanceTimeBy(300)
        c.onBodyChange("ab")
        advanceTimeBy(300)
        runCurrent()
        assertEquals("nothing saved before the debounce elapsed", 0, repo.saveCount)
        advanceTimeBy(400)
        runCurrent()
        assertEquals(1, repo.saveCount)
        assertEquals("ab", repo.notes["note-1"]?.body)
        assertEquals(SaveStatus.SAVED, c.state.value.saveStatus)
    }

    @Test
    fun flushSavesImmediatelyAndCancelsPendingDebounce() = runTest {
        val repo = FakeNoteRepository()
        val c = controller(repo)
        c.start(null, "folder-1")
        c.onTitleChange("Shopping")
        c.flush().join()
        assertEquals(1, repo.saveCount)
        assertEquals("folder-1", repo.notes["note-1"]?.folderId)
        advanceUntilIdle()
        assertEquals("debounce must not save a second time", 1, repo.saveCount)
    }

    @Test
    fun slowEarlierSaveNeverClobbersNewerText() = runTest {
        val repo = FakeNoteRepository(saveDelayMs = 100)
        val c = controller(repo)
        c.start(null, null)
        c.onBodyChange("one")
        advanceTimeBy(601)
        c.onBodyChange("two")
        advanceUntilIdle()
        assertEquals("two", repo.notes["note-1"]?.body)
        assertEquals(SaveStatus.SAVED, c.state.value.saveStatus)
    }

    @Test
    fun clearingAPersistedNoteDeletesIt() = runTest {
        val repo = FakeNoteRepository()
        val c = controller(repo)
        c.start(null, null)
        c.onBodyChange("hello")
        c.flush().join()
        assertEquals(1, repo.notes.size)
        c.onBodyChange("")
        c.flush().join()
        assertTrue(repo.notes.isEmpty())
        assertFalse(c.state.value.persisted)
        assertNull(c.state.value.createdAt)
    }

    @Test
    fun openingAnExistingNoteWithoutEditsDoesNotRewriteIt() = runTest {
        val repo = FakeNoteRepository()
        repo.notes["n9"] = NoteEntity("n9", "T", "B", null, false, false, createdAt = 5, updatedAt = 6)
        val c = controller(repo)
        c.start("n9", null)
        c.flush().join()
        advanceUntilIdle()
        assertEquals(0, repo.saveCount)
        assertEquals("B", c.state.value.body)
        assertEquals(5L, c.state.value.createdAt)
    }

    @Test
    fun editingAnExistingNoteKeepsItsIdAndCreationTime() = runTest {
        val repo = FakeNoteRepository()
        repo.notes["n9"] = NoteEntity("n9", "T", "B", null, false, false, createdAt = 5, updatedAt = 6)
        val c = controller(repo)
        c.start("n9", null)
        c.onBodyChange("B2")
        c.flush().join()
        assertEquals(1, repo.notes.size)
        assertEquals("B2", repo.notes["n9"]?.body)
        assertEquals(5L, repo.notes["n9"]?.createdAt)
    }

    @Test
    fun deleteRemovesNoteAndIgnoresLaterEdits() = runTest {
        val repo = FakeNoteRepository()
        val c = controller(repo)
        c.start(null, null)
        c.onBodyChange("x")
        c.flush().join()
        c.delete().join()
        assertTrue(c.state.value.deleted)
        c.onBodyChange("y")
        c.flush().join()
        advanceUntilIdle()
        assertTrue(repo.notes.isEmpty())
    }

    @Test
    fun archivingUnpinsAndPinningUnarchives() = runTest {
        val repo = FakeNoteRepository()
        val c = controller(repo)
        c.start(null, null)
        c.onBodyChange("x")
        c.onPinnedChange(true)
        c.onArchivedChange(true)
        assertTrue(c.state.value.archived)
        assertFalse(c.state.value.pinned)
        c.onPinnedChange(true)
        assertFalse(c.state.value.archived)
        c.flush().join()
    }
}
