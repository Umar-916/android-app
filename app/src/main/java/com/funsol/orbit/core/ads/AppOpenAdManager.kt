package com.funsol.orbit.core.ads

import android.os.SystemClock
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.remote.RemoteConfigRepository
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppOpenAdManager(
    private val tracker: ActivityTracker,
    private val gate: AdGate,
    private val ids: AdUnitIds,
    private val remote: RemoteConfigRepository,
    private val settings: SettingsStore,
    private val capping: InterstitialCapping,
    private val fullScreen: FullScreenAdState,
    private val scope: CoroutineScope,
) : ActivityTracker.ForegroundListener {
    private var ad: AppOpenAd? = null
    private var loadedAtMs = 0L
    private var loading = false

    @Volatile
    private var suppressNext = false

    fun start() {
        tracker.addForegroundListener(this)
    }

    fun suppressNext() {
        suppressNext = true
    }

    fun preload() = runOnMain {
        if (loading || isLoaded() || !gate.allows(AdKind.APP_OPEN)) return@runOnMain
        loading = true
        try {
            AppOpenAd.load(
                AdRequest.Builder(ids.appOpen).build(),
                object : AdLoadCallback<AppOpenAd> {
                    override fun onAdLoaded(ad: AppOpenAd) = runOnMain {
                        if (gate.isPremium) {
                            tryCatch { ad.destroy() }
                            loading = false
                            return@runOnMain
                        }
                        this@AppOpenAdManager.ad = ad
                        loadedAtMs = SystemClock.elapsedRealtime()
                        loading = false
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                        loading = false
                    }
                },
            )
        } catch (_: Throwable) {
            loading = false
        }
    }

    fun clear() = runOnMain {
        ad?.let { tryCatch { it.destroy() } }
        ad = null
    }

    private fun isLoaded() = ad != null && SystemClock.elapsedRealtime() - loadedAtMs < EXPIRY_MS

    override fun onEnteredForeground(backgroundedForMs: Long, isFirst: Boolean) {
        if (isFirst) return
        val config = remote.config.value
        if (!config.appOpenEnabled || backgroundedForMs < config.appOpenMinBackgroundSeconds * 1_000L) return
        if (suppressNext) {
            suppressNext = false
            return
        }
        scope.launch {
            val s = settings.settings.first()
            if (!s.languageChosen || !s.onboardingDone) return@launch
            runOnMain { showIfPossible() }
        }
    }

    private fun showIfPossible() {
        val activity = tracker.current() ?: return
        if (!gate.allows(AdKind.APP_OPEN) || fullScreen.isShowing) return
        if (!capping.canShowByInterval(remote.config.value.interstitialMinIntervalSeconds)) return
        val current = ad
        if (current == null || !isLoaded()) {
            ad = null
            preload()
            return
        }
        if (!fullScreen.tryEnter()) return
        ad = null
        current.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() = runOnMain {
                fullScreen.leave()
                capping.recordShown()
                tryCatch { current.destroy() }
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = runOnMain {
                fullScreen.leave()
                tryCatch { current.destroy() }
                preload()
            }
        }
        try {
            current.show(activity)
        } catch (_: Throwable) {
            fullScreen.leave()
            tryCatch { current.destroy() }
        }
    }

    private companion object {
        const val EXPIRY_MS = 3L * 60L * 60L * 1000L
    }
}
