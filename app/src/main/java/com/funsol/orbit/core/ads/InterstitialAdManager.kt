package com.funsol.orbit.core.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.remote.RemoteConfigRepository
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import java.util.concurrent.atomic.AtomicBoolean

enum class SplashAdState { NOT_APPLICABLE, LOADING, LOADED, FAILED;

    val isSettled: Boolean get() = this != LOADING
}

class InterstitialAdManager(
    private val appContext: Context,
    private val gate: AdGate,
    private val ids: AdUnitIds,
    private val remote: RemoteConfigRepository,
    private val capping: InterstitialCapping,
    private val fullScreen: FullScreenAdState,
) {
    private var ad: InterstitialAd? = null
    private var loadedAtMs = 0L
    private var loading = false
    private var loadGeneration = 0
    private var pendingResults = mutableListOf<(Boolean) -> Unit>()

    val isLoaded: Boolean get() = ad != null && !isExpired()

    private fun isExpired() = SystemClock.elapsedRealtime() - loadedAtMs > EXPIRY_MS

    fun preload(placement: String, onResult: ((Boolean) -> Unit)? = null) = runOnMain {
        if (isLoaded) {
            onResult?.invoke(true)
            return@runOnMain
        }
        if (!gate.allows(AdKind.INTERSTITIAL, placement)) {
            onResult?.invoke(false)
            return@runOnMain
        }
        onResult?.let { pendingResults.add(it) }
        if (loading) return@runOnMain
        ad?.let { tryCatch { it.destroy() } }
        ad = null
        loading = true
        val generation = ++loadGeneration
        val timeout = Runnable {
            if (loading && generation == loadGeneration) {
                loading = false
                finishPending(false)
            }
        }
        postDelayedOnMain(LOAD_TIMEOUT_MS, timeout)
        try {
            InterstitialAd.load(
                AdRequest.Builder(ids.interstitial).build(),
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(ad: InterstitialAd) = runOnMain {
                        cancelOnMain(timeout)
                        if (generation != loadGeneration) {
                            tryCatch { ad.destroy() }
                            return@runOnMain
                        }
                        if (gate.isPremium) {
                            tryCatch { ad.destroy() }
                            loading = false
                            finishPending(false)
                            return@runOnMain
                        }
                        this@InterstitialAdManager.ad = ad
                        loadedAtMs = SystemClock.elapsedRealtime()
                        loading = false
                        finishPending(true)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                        cancelOnMain(timeout)
                        if (generation == loadGeneration) {
                            loading = false
                            finishPending(false)
                        }
                    }
                },
            )
        } catch (_: Throwable) {
            cancelOnMain(timeout)
            loading = false
            finishPending(false)
        }
    }

    private fun finishPending(success: Boolean) {
        val callbacks = pendingResults
        pendingResults = mutableListOf()
        callbacks.forEach { it(success) }
    }

    fun clear() = runOnMain {
        ad?.let { tryCatch { it.destroy() } }
        ad = null
        loadGeneration++
        loading = false
        finishPending(false)
    }

    enum class Trigger { SPLASH, NAVIGATION }

    fun showThen(activity: Activity, trigger: Trigger, placement: String, onDone: () -> Unit) = runOnMain {
        val finished = AtomicBoolean(false)
        fun finish() {
            if (finished.compareAndSet(false, true)) onDone()
        }

        val config = remote.config.value
        if (trigger == Trigger.NAVIGATION) capping.recordAction()
        val cappingOk = when (trigger) {
            Trigger.NAVIGATION -> capping.canShowAfterAction(config.interstitialNavEveryNActions, config.interstitialMinIntervalSeconds)
            Trigger.SPLASH -> capping.canShowByInterval(config.interstitialMinIntervalSeconds)
        }
        val usable = !activity.isFinishing && !activity.isDestroyed
        if (!usable || !cappingOk || !gate.allows(AdKind.INTERSTITIAL, placement)) {
            finish()
            return@runOnMain
        }
        val current = ad
        if (current == null || isExpired()) {
            ad = null
            preload(placement)
            finish()
            return@runOnMain
        }
        if (!fullScreen.tryEnter()) {
            finish()
            return@runOnMain
        }

        val shown = AtomicBoolean(false)
        val cleaned = AtomicBoolean(false)
        fun cleanup(shownOk: Boolean) {
            if (cleaned.compareAndSet(false, true)) afterShow(current, shownOk)
        }
        val startTimeout = Runnable {
            if (!shown.get() && !finished.get()) {
                cleanup(shownOk = false)
                finish()
            }
        }
        current.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() = runOnMain {
                shown.set(true)
                cancelOnMain(startTimeout)
            }

            override fun onAdDismissedFullScreenContent() = runOnMain {
                cancelOnMain(startTimeout)
                cleanup(shownOk = true)
                finish()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = runOnMain {
                cancelOnMain(startTimeout)
                cleanup(shownOk = false)
                finish()
            }
        }
        ad = null
        postDelayedOnMain(SHOW_START_TIMEOUT_MS, startTimeout)
        try {
            current.show(activity)
        } catch (_: Throwable) {
            cancelOnMain(startTimeout)
            cleanup(shownOk = false)
            finish()
        }
    }

    private fun afterShow(shownAd: InterstitialAd, shownOk: Boolean = true) {
        fullScreen.leave()
        if (shownOk) capping.recordShown()
        tryCatch { shownAd.destroy() }
        preload(InterstitialPlacement.NAVIGATION)
    }

    private companion object {
        const val EXPIRY_MS = 60L * 60L * 1000L
        const val LOAD_TIMEOUT_MS = 10_000L
        const val SHOW_START_TIMEOUT_MS = 2_000L
    }
}
