package com.funsol.orbit.core.startup

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InitializationHelperTest {
    @Test
    fun happyPath_runsEveryStepOnceInOrderAndCompletes() = runTest {
        val order = mutableListOf<InitStep>()
        val helper = InitializationHelper(
            InitStep.entries.map { step -> StartupTask(step, critical = step == InitStep.WARM_DATABASE) { order += step } },
        )
        assertEquals(InitState.Idle, helper.state.value)
        helper.initialize()
        assertEquals(InitState.Completed, helper.state.value)
        assertEquals(InitStep.entries.toList(), order)
    }

    @Test
    fun secondInitialize_afterCompletion_doesNotRunAgain() = runTest {
        var runs = 0
        val helper = InitializationHelper(listOf(StartupTask(InitStep.LOAD_SETTINGS, critical = false) { runs++ }))
        helper.initialize()
        helper.initialize()
        assertEquals(1, runs)
    }

    @Test
    fun concurrentInitialize_isGuarded() = runTest {
        var runs = 0
        val helper = InitializationHelper(listOf(StartupTask(InitStep.LOAD_SETTINGS, critical = false) { delay(100); runs++ }))
        List(3) { async { helper.initialize() } }.awaitAll()
        assertEquals(1, runs)
        assertEquals(InitState.Completed, helper.state.value)
    }

    @Test
    fun nonCriticalFailure_isTolerated() = runTest {
        val helper = InitializationHelper(
            listOf(
                StartupTask(InitStep.LOAD_SETTINGS, critical = false) { error("boom") },
                StartupTask(InitStep.WARM_DATABASE, critical = true) { },
            ),
        )
        helper.initialize()
        assertEquals(InitState.Completed, helper.state.value)
    }

    @Test
    fun criticalFailure_endsInFailed_andRetrySucceeds() = runTest {
        var attempts = 0
        val helper = InitializationHelper(
            listOf(
                StartupTask(InitStep.WARM_DATABASE, critical = true) {
                    attempts++
                    if (attempts == 1) error("cannot open")
                },
                StartupTask(InitStep.RECONCILE_REMINDERS, critical = false) { },
            ),
        )
        helper.initialize()
        assertEquals(InitState.Failed(InitStep.WARM_DATABASE), helper.state.value)
        helper.initialize()
        assertEquals(InitState.Completed, helper.state.value)
        assertEquals(2, attempts)
    }

    @Test
    fun timeout_ofNonCriticalStep_isSkipped_andOfCriticalStep_fails() = runTest {
        val tolerant = InitializationHelper(
            listOf(
                StartupTask(InitStep.RECONCILE_REMINDERS, critical = false, timeoutMs = 100) { delay(10_000) },
                StartupTask(InitStep.WARM_DATABASE, critical = true) { },
            ),
        )
        tolerant.initialize()
        assertEquals(InitState.Completed, tolerant.state.value)

        val strict = InitializationHelper(
            listOf(StartupTask(InitStep.WARM_DATABASE, critical = true, timeoutMs = 100) { delay(10_000) }),
        )
        strict.initialize()
        assertTrue(strict.state.value is InitState.Failed)
    }

    @Test
    fun progress_isReportedWhileRunning() = runTest {
        val seen = mutableListOf<Float>()
        lateinit var helper: InitializationHelper
        helper = InitializationHelper(
            listOf(
                StartupTask(InitStep.LOAD_SETTINGS, critical = false) { (helper.state.value as? InitState.Running)?.let { seen += it.progress } },
                StartupTask(InitStep.WARM_DATABASE, critical = true) { (helper.state.value as? InitState.Running)?.let { seen += it.progress } },
            ),
        )
        helper.initialize()
        assertEquals(listOf(0f, 0.5f), seen)
    }
}
