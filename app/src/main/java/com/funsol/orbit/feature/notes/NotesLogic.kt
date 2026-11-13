package com.funsol.orbit.feature.notes

import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.NoteFolderEntity

sealed interface FolderFilter {
    data object All : FolderFilter
    data object NoFolder : FolderFilter
    data class Folder(val id: String) : FolderFilter
}

data class NoteItem(
    val id: String,
    val title: String,
    val preview: String,
    val folderId: String?,
    val folderName: String?,
    val pinned: Boolean,
    val archived: Boolean,
    val updatedAt: Long,
)

const val PREVIEW_MAX_CHARS = 200

fun makePreview(body: String, max: Int = PREVIEW_MAX_CHARS): String {
    val head = if (body.length > max * 3) body.substring(0, max * 3) else body
    val collapsed = head.trim().replace(Regex("\\s+"), " ")
    return if (collapsed.length > max) collapsed.substring(0, max).trimEnd() + "…" else collapsed
}

fun noteMatches(note: NoteEntity, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return note.title.contains(q, ignoreCase = true) || note.body.contains(q, ignoreCase = true)
}

fun buildNoteItems(
    notes: List<NoteEntity>,
    folders: List<NoteFolderEntity>,
    filter: FolderFilter,
    showArchived: Boolean,
    query: String,
): List<NoteItem> {
    val names = folders.associate { it.id to it.name }
    val effective = if (filter is FolderFilter.Folder && filter.id !in names) FolderFilter.All else filter
    val out = ArrayList<NoteItem>()
    for (n in notes) {
        if (n.archived != showArchived) continue
        val inFolder = when (effective) {
            FolderFilter.All -> true
            FolderFilter.NoFolder -> n.folderId == null || n.folderId !in names
            is FolderFilter.Folder -> n.folderId == effective.id
        }
        if (!inFolder || !noteMatches(n, query)) continue
        out += NoteItem(
            id = n.id,
            title = n.title.trim(),
            preview = makePreview(n.body),
            folderId = n.folderId?.takeIf { it in names },
            folderName = n.folderId?.let { names[it] },
            pinned = n.pinned,
            archived = n.archived,
            updatedAt = n.updatedAt,
        )
    }
    return out
}
