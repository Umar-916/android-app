package com.funsol.orbit.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.backup.BackupFile
import com.funsol.orbit.core.backup.BackupManager
import com.funsol.orbit.core.backup.BackupParseResult
import com.funsol.orbit.core.backup.BackupSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate

enum class BackupAction { EXPORT, EXPORT_TASKS_CSV, EXPORT_HABITS_CSV, RESTORE }

sealed interface BackupMessage {
    data class Success(val action: BackupAction, val restored: BackupSummary? = null) : BackupMessage
    data class Failure(val action: BackupAction) : BackupMessage
    data class Invalid(val reason: BackupParseResult.Reason) : BackupMessage
    data object TooLarge : BackupMessage
}

data class BackupUiState(
    val busy: BackupAction? = null,
    val pendingRestore: BackupSummary? = null,
    val message: BackupMessage? = null,
)

object BackupIo {
    const val MAX_RESTORE_BYTES = 25 * 1024 * 1024

    fun backupFileName(date: LocalDate) = "orbit-backup-$date.json"
    fun tasksCsvFileName(date: LocalDate) = "orbit-tasks-$date.csv"
    fun habitsCsvFileName(date: LocalDate) = "orbit-habits-$date.csv"

    fun readLimited(input: InputStream, limit: Int = MAX_RESTORE_BYTES): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}

class BackupViewModel(
    context: Context,
    private val manager: BackupManager,
) : ViewModel() {
    private val resolver = context.applicationContext.contentResolver

    private val _state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _state.asStateFlow()

    private var pendingFile: BackupFile? = null

    fun exportBackup(uri: Uri) = write(uri, BackupAction.EXPORT) { manager.export() }

    fun exportTasksCsv(uri: Uri) = write(uri, BackupAction.EXPORT_TASKS_CSV) { manager.exportTasksCsv() }

    fun exportHabitsCsv(uri: Uri) = write(uri, BackupAction.EXPORT_HABITS_CSV) { manager.exportHabitsCsv() }

    private fun write(uri: Uri, action: BackupAction, produce: suspend () -> String) {
        if (_state.value.busy != null) return
        _state.update { it.copy(busy = action, message = null) }
        viewModelScope.launch {
            val ok = try {
                withContext(Dispatchers.IO) {
                    val text = produce()
                    val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("no output stream")
                    stream.use { it.write(text.toByteArray(Charsets.UTF_8)); it.flush() }
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            _state.update { it.copy(busy = null, message = if (ok) BackupMessage.Success(action) else BackupMessage.Failure(action)) }
        }
    }

    fun onRestoreFilePicked(uri: Uri) {
        if (_state.value.busy != null) return
        pendingFile = null
        _state.update { it.copy(busy = BackupAction.RESTORE, message = null, pendingRestore = null) }
        viewModelScope.launch {
            val outcome: Any = try {
                withContext(Dispatchers.IO) {
                    val stream = resolver.openInputStream(uri)
                    if (stream == null) {
                        BackupMessage.Failure(BackupAction.RESTORE)
                    } else {
                        val bytes = stream.use { BackupIo.readLimited(it) }
                        if (bytes == null) BackupMessage.TooLarge else manager.parse(String(bytes, Charsets.UTF_8))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BackupMessage.Failure(BackupAction.RESTORE)
            }
            when (outcome) {
                is BackupParseResult.Valid -> {
                    pendingFile = outcome.file
                    _state.update { it.copy(busy = null, pendingRestore = outcome.summary) }
                }
                is BackupParseResult.Invalid -> _state.update { it.copy(busy = null, message = BackupMessage.Invalid(outcome.reason)) }
                is BackupMessage -> _state.update { it.copy(busy = null, message = outcome) }
                else -> _state.update { it.copy(busy = null, message = BackupMessage.Failure(BackupAction.RESTORE)) }
            }
        }
    }

    fun confirmRestore() {
        val file = pendingFile ?: return
        pendingFile = null
        _state.update { it.copy(busy = BackupAction.RESTORE, pendingRestore = null) }
        viewModelScope.launch {
            val ok = try {
                withContext(Dispatchers.IO) { manager.restore(file) }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            val restored = BackupSummary(file.tasks.size, file.projects.size, file.notes.size, file.habits.size, file.goals.size, file.exportedAt)
            _state.update {
                it.copy(
                    busy = null,
                    message = if (ok) BackupMessage.Success(BackupAction.RESTORE, restored) else BackupMessage.Failure(BackupAction.RESTORE),
                )
            }
        }
    }

    fun cancelRestore() {
        pendingFile = null
        _state.update { it.copy(pendingRestore = null) }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }
}
