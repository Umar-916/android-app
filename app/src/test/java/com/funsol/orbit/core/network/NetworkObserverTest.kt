package com.funsol.orbit.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNetworkObserver(initiallyOnline: Boolean) : NetworkObserver {
    private val state = MutableStateFlow(initiallyOnline)
    override val online: StateFlow<Boolean> = state
    var started = false
        private set

    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
    }

    fun set(online: Boolean) {
        state.value = online
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkObserverTest {
    @Test
    fun isOnline_followsTheFlow() {
        val net = FakeNetworkObserver(initiallyOnline = false)
        assertFalse(net.isOnline())
        net.set(true)
        assertTrue(net.isOnline())
    }

    @Test
    fun awaitOnline_returnsImmediatelyWhenOnline() = runTest {
        assertTrue(FakeNetworkObserver(true).awaitOnline(1_000))
    }

    @Test
    fun awaitOnline_timesOutWhenOffline() = runTest {
        assertFalse(FakeNetworkObserver(false).awaitOnline(500))
    }

    @Test
    fun awaitOnline_resumesWhenTheNetworkReturns() = runTest {
        val net = FakeNetworkObserver(false)
        var result: Boolean? = null
        val job = launch { result = net.awaitOnline(5_000) }
        advanceTimeBy(1_000)
        assertNull(result)
        net.set(true)
        job.join()
        assertTrue(result == true)
    }

    @Test
    fun usableInternet_requiresTheInternetCapability() {
        assertTrue(hasUsableInternet(true))
        assertFalse(hasUsableInternet(false))
    }

    @Test
    fun startAndStop_areSafeToCallRepeatedly() {
        val net = FakeNetworkObserver(true)
        net.stop()
        net.start()
        net.start()
        assertTrue(net.started)
        net.stop()
        net.stop()
        assertFalse(net.started)
    }
}
