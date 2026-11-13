package com.funsol.orbit.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.funsol.orbit.core.common.tryCatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

interface NetworkObserver {
    val online: StateFlow<Boolean>

    fun isOnline(): Boolean = online.value

    fun start() = Unit

    fun stop() = Unit
}

suspend fun NetworkObserver.awaitOnline(timeoutMs: Long): Boolean {
    if (isOnline()) return true
    return withTimeoutOrNull(timeoutMs) { online.first { it } } != null
}

internal fun hasUsableInternet(hasInternetCapability: Boolean): Boolean = hasInternetCapability

class ConnectivityNetworkObserver(context: Context) : NetworkObserver {
    private val manager: ConnectivityManager? =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val state = MutableStateFlow(readCurrent())
    override val online: StateFlow<Boolean> = state.asStateFlow()

    private var callback: ConnectivityManager.NetworkCallback? = null

    @Synchronized
    override fun start() {
        if (callback != null || manager == null) return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                state.value = true
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                state.value = hasUsableInternet(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }

            override fun onLost(network: Network) {
                state.value = false
            }
        }
        tryCatch {
            manager.registerDefaultNetworkCallback(cb)
            callback = cb
        }
        state.value = readCurrent()
    }

    @Synchronized
    override fun stop() {
        val cb = callback ?: return
        callback = null
        tryCatch { manager?.unregisterNetworkCallback(cb) }
    }

    private fun readCurrent(): Boolean {
        var result = false
        tryCatch {
            val m = manager ?: return@tryCatch
            val caps = m.getNetworkCapabilities(m.activeNetwork)
            result = caps != null && hasUsableInternet(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        }
        return result
    }
}
