package com.funsol.orbit.core.ads

import android.os.SystemClock
import com.funsol.orbit.core.common.tryCatch
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class NativeAdPool(
    private val gate: AdGate,
    private val ids: AdUnitIds,
) {
    private class Entry(val ad: NativeAd, val loadedAtMs: Long)

    private val ready = HashMap<NativePlacement, ArrayDeque<Entry>>()
    private val inFlight = HashSet<NativePlacement>()
    private val changes = MutableStateFlow(0)

    fun preload(placement: NativePlacement) = runOnMain { startLoadIfNeeded(placement) }

    suspend fun acquire(placement: NativePlacement, timeoutMs: Long = ACQUIRE_TIMEOUT_MS): NativeAd? =
        withContext(Dispatchers.Main.immediate) {
            takeReady(placement)?.let { taken ->
                topUp(placement)
                return@withContext taken
            }
            if (!gate.allows(AdKind.NATIVE, placement.key)) return@withContext null
            startLoadIfNeeded(placement)
            withTimeoutOrNull(timeoutMs) {
                changes.first { !queue(placement).isNullOrEmpty() || placement !in inFlight }
            }
            takeReady(placement)?.also { topUp(placement) }
        }

    fun release(ad: NativeAd) = runOnMain { tryCatch { ad.destroy() } }

    fun clear() = runOnMain {
        ready.values.forEach { q -> q.forEach { e -> tryCatch { e.ad.destroy() } } }
        ready.clear()
        changes.value++
    }

    private fun queue(placement: NativePlacement): ArrayDeque<Entry>? = ready[placement]

    private fun takeReady(placement: NativePlacement): NativeAd? {
        val q = ready[placement] ?: return null
        val now = SystemClock.elapsedRealtime()
        while (q.isNotEmpty()) {
            val entry = q.removeFirst()
            if (now - entry.loadedAtMs < EXPIRY_MS) return entry.ad
            tryCatch { entry.ad.destroy() }
        }
        return null
    }

    private fun topUp(placement: NativePlacement) {
        if (placement == NativePlacement.ONBOARDING) startLoadIfNeeded(placement)
    }

    private fun startLoadIfNeeded(placement: NativePlacement) {
        if (placement in inFlight || !queue(placement).isNullOrEmpty()) return
        if (!gate.allows(AdKind.NATIVE, placement.key)) return
        inFlight.add(placement)
        try {
            NativeAdLoader.load(
                NativeAdRequest.Builder(ids.native, listOf(NativeAd.NativeAdType.NATIVE)).build(),
                object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) = runOnMain {
                        inFlight.remove(placement)
                        if (gate.isPremium) {
                            tryCatch { nativeAd.destroy() }
                        } else {
                            ready.getOrPut(placement) { ArrayDeque() }.addLast(Entry(nativeAd, SystemClock.elapsedRealtime()))
                        }
                        changes.value++
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                        inFlight.remove(placement)
                        changes.value++
                    }
                },
            )
        } catch (_: Throwable) {
            inFlight.remove(placement)
            changes.value++
        }
    }

    private companion object {
        const val ACQUIRE_TIMEOUT_MS = 12_000L

        const val EXPIRY_MS = 55L * 60L * 1000L
    }
}
