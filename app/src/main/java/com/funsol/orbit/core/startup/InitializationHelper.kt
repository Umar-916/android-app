package com.funsol.orbit.core.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean

enum class InitStep {
    LOAD_SETTINGS,
    WARM_DATABASE,
    RECONCILE_REMINDERS,
    LOCALE_READY,
    FIREBASE,
    CHECK_NETWORK,
    CONSENT,
    REMOTE_CONFIG,
    MOBILE_ADS,
    PRELOAD_ADS,
}

sealed interface InitState {
    data object Idle : InitState

    data class Running(val step: InitStep, val progress: Float) : InitState

    data object Completed : InitState

    data class Failed(val step: InitStep) : InitState
}

class StartupTask(
    val step: InitStep,
    val critical: Boolean,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    val block: suspend () -> Unit,
) {
    companion object {
        const val DEFAULT_TIMEOUT_MS = 5_000L
    }
}

class InitializationHelper(private val tasks: List<StartupTask>) {
    private val running = AtomicBoolean(false)
    private val _state = MutableStateFlow<InitState>(InitState.Idle)
    val state: StateFlow<InitState> = _state.asStateFlow()

    suspend fun initialize() {
        if (_state.value is InitState.Completed) return
        if (!running.compareAndSet(false, true)) return
        try {
            tasks.forEachIndexed { index, task ->
                _state.value = InitState.Running(task.step, index / tasks.size.toFloat())
                val ok = runTask(task)
                if (!ok && task.critical) {
                    _state.value = InitState.Failed(task.step)
                    return
                }
            }
            _state.value = InitState.Completed
        } finally {
            running.set(false)
        }
    }

    private suspend fun runTask(task: StartupTask): Boolean = try {
        withTimeout(task.timeoutMs) { task.block() }
        true
    } catch (e: TimeoutCancellationException) {
        false
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        false
    }
}
