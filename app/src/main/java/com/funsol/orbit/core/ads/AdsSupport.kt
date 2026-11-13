package com.funsol.orbit.core.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.datastore.SettingsStore
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

private val mainHandler = Handler(Looper.getMainLooper())

internal fun runOnMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
}

internal fun postDelayedOnMain(delayMs: Long, block: Runnable) {
    mainHandler.postDelayed(block, delayMs)
}

internal fun cancelOnMain(block: Runnable) {
    mainHandler.removeCallbacks(block)
}

class PremiumState(store: SettingsStore, scope: CoroutineScope) {
    val isPremium: StateFlow<Boolean> = store.settings
        .map { it.isPremium }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, true)
}

class FullScreenAdState {
    private val showing = AtomicBoolean(false)
    val isShowing: Boolean get() = showing.get()

    fun tryEnter(): Boolean = showing.compareAndSet(false, true)

    fun leave() {
        showing.set(false)
    }
}

class MobileAdsInitializer(context: Context, private val ids: AdUnitIds) {
    private val appContext = context.applicationContext
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    suspend fun initialize(timeoutMs: Long): Boolean {
        if (_ready.value) return true
        if (ids.appId.isBlank()) return false
        if (started.compareAndSet(false, true)) {
            withContext(Dispatchers.Default) {
                tryCatch {
                    MobileAds.initialize(appContext, InitializationConfig.Builder(ids.appId).build()) { _ready.value = true }
                }
            }
        }
        return withTimeoutOrNull(timeoutMs) { ready.first { it } } == true
    }
}
