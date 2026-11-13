package com.funsol.orbit.core.repository

import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.HabitEntity
import com.funsol.orbit.core.database.entity.NoteEntity
import com.funsol.orbit.core.database.entity.ProjectEntity
import com.funsol.orbit.core.database.entity.TaskEntity

data class SearchResults(
    val tasks: List<TaskEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
) {
    val isEmpty get() = tasks.isEmpty() && projects.isEmpty() && notes.isEmpty() && habits.isEmpty() && goals.isEmpty()
}

interface SearchRepository {
    suspend fun search(query: String): SearchResults
}

class SearchRepositoryImpl(private val db: OrbitDatabase) : SearchRepository {
    override suspend fun search(query: String): SearchResults {
        val q = query.trim()
        if (q.isEmpty()) return SearchResults()
        val pattern = likePattern(q)
        return SearchResults(
            tasks = db.taskDao().search(pattern),
            projects = db.projectDao().search(pattern),
            notes = db.noteDao().search(pattern),
            habits = db.habitDao().search(pattern),
            goals = db.goalDao().search(pattern),
        )
    }

    companion object {
        fun likePattern(query: String): String =
            "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
    }
}
