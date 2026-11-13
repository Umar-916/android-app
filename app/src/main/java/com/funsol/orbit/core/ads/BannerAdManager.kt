package com.funsol.orbit.core.ads

import android.app.Activity
import android.content.Context
import com.funsol.orbit.core.common.tryCatch
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

sealed interface BannerState {
    data object Idle : BannerState
    data object Loading : BannerState

    data class Ready(val ad: BannerAd, val widthDp: Int, val heightDp: Int) : BannerState
    data object Failed : BannerState
}

class BannerAdManager(
    private val appContext: Context,
    private val gate: AdGate,
    private val ids: AdUnitIds,
    tracker: ActivityTracker,
) {
    private val _state = MutableStateFlow<BannerState>(BannerState.Idle)
    val state: StateFlow<BannerState> = _state.asStateFlow()

    private var loadingWidthDp = 0
    private var viewActivity: WeakReference<Activity>? = null

    init {
        tracker.addDestroyListener { activity ->
            if (viewActivity?.get() === activity) clear()
        }
    }

    fun heightFor(widthDp: Int): Int =
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(appContext, widthDp).height

    fun ensureLoaded(widthDp: Int) = runOnMain {
        if (!gate.allows(AdKind.BANNER)) return@runOnMain
        val current = _state.value
        if (current is BannerState.Ready && current.widthDp == widthDp) return@runOnMain
        if (current is BannerState.Loading && loadingWidthDp == widthDp) return@runOnMain
        (current as? BannerState.Ready)?.let { tryCatch { it.ad.destroy() } }
        loadingWidthDp = widthDp
        _state.value = BannerState.Loading
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(appContext, widthDp)
        try {
            BannerAd.load(
                BannerAdRequest.Builder(ids.banner, size).build(),
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) = runOnMain {
                        if (loadingWidthDp != widthDp || gate.isPremium) {
                            tryCatch { ad.destroy() }
                            if (gate.isPremium) _state.value = BannerState.Idle
                        } else {
                            _state.value = BannerState.Ready(ad, widthDp, size.height)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                        if (loadingWidthDp == widthDp) _state.value = BannerState.Failed
                    }
                },
            )
        } catch (_: Throwable) {
            _state.value = BannerState.Failed
        }
    }

    internal fun bindActivity(activity: Activity) {
        viewActivity = WeakReference(activity)
    }

    fun clear() = runOnMain {
        (_state.value as? BannerState.Ready)?.let { tryCatch { it.ad.destroy() } }
        loadingWidthDp = 0
        viewActivity = null
        _state.value = BannerState.Idle
    }

    fun resetFailure() = runOnMain {
        if (_state.value is BannerState.Failed) _state.value = BannerState.Idle
    }
}
