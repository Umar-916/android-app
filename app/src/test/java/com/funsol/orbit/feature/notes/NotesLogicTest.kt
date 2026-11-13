package com.funsol.orbit.feature.notes

import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesLogicTest {
    private val work = NoteFolderEntity("f1", "Work", 0)
    private val home = NoteFolderEntity("f2", "Home", 0)
    private val folders = listOf(work, home)

    private fun note(id: String, title: String = "", body: String = "", folder: String? = null, archived: Boolean = false, pinned: Boolean = false) =
        NoteEntity(id, title, body, folder, pinned, archived, createdAt = 0, updatedAt = 0)

    private val notes = listOf(
        note("1", "Plan", "quarterly goals", "f1", pinned = true),
        note("2", "Groceries", "milk and eggs", "f2"),
        note("3", "Loose idea", "no folder here", null),
        note("4", "Old", "archived note", "f1", archived = true),
    )

    private fun ids(filter: FolderFilter = FolderFilter.All, archived: Boolean = false, query: String = "") =
        buildNoteItems(notes, folders, filter, archived, query).map { it.id }

    @Test fun allHidesArchivedAndKeepsRepositoryOrder() = assertEquals(listOf("1", "2", "3"), ids())

    @Test fun archivedViewShowsOnlyArchived() = assertEquals(listOf("4"), ids(archived = true))

    @Test fun noFolderFilter() = assertEquals(listOf("3"), ids(FolderFilter.NoFolder))

    @Test fun specificFolderFilter() = assertEquals(listOf("2"), ids(FolderFilter.Folder("f2")))

    @Test fun deletedFolderFilterFallsBackToAll() = assertEquals(listOf("1", "2", "3"), ids(FolderFilter.Folder("gone")))

    @Test fun searchMatchesTitleAndBodyCaseInsensitively() {
        assertEquals(listOf("1"), ids(query = "QUARTERLY"))
        assertEquals(listOf("2"), ids(query = "grocer"))
        assertTrue(ids(query = "zzz").isEmpty())
    }

    @Test fun blankQueryMatchesEverything() = assertEquals(listOf("1", "2", "3"), ids(query = "   "))

    @Test fun folderNameIsResolved() {
        val item = buildNoteItems(notes, folders, FolderFilter.All, false, "").first { it.id == "1" }
        assertEquals("Work", item.folderName)
        assertEquals("f1", item.folderId)
        assertTrue(item.pinned)
    }

    @Test fun previewCollapsesWhitespaceAndTruncates() {
        assertEquals("a b c", makePreview("  a\n\n b \t c  "))
        val long = "x".repeat(1000)
        val preview = makePreview(long)
        assertEquals(PREVIEW_MAX_CHARS + 1, preview.length)
        assertTrue(preview.endsWith("…"))
    }
}
