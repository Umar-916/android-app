package com.funsol.orbit.core.ads

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.designsystem.Orbit
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.compose.koinInject
import java.lang.ref.WeakReference

private const val RECT_WIDTH_DP = 300
private const val RECT_HEIGHT_DP = 250

class RectBannerAdManager(
    private val gate: AdGate,
    private val ids: AdUnitIds,
    tracker: ActivityTracker,
) {
    private val _state = MutableStateFlow<BannerState>(BannerState.Idle)
    val state: StateFlow<BannerState> = _state.asStateFlow()
    private var viewActivity: WeakReference<Activity>? = null

    init {
        tracker.addDestroyListener { activity -> if (viewActivity?.get() === activity) clear() }
    }

    fun ensureLoaded() = runOnMain {
        if (!gate.allows(AdKind.RECT_BANNER, LANGUAGE_KEY)) return@runOnMain
        if (_state.value is BannerState.Ready || _state.value is BannerState.Loading) return@runOnMain
        _state.value = BannerState.Loading
        try {
            BannerAd.load(
                BannerAdRequest.Builder(ids.rectBanner, AdSize.MEDIUM_RECTANGLE).build(),
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) = runOnMain {
                        if (gate.isPremium || _state.value !is BannerState.Loading) {
                            tryCatch { ad.destroy() }
                            if (gate.isPremium) _state.value = BannerState.Idle
                        } else {
                            _state.value = BannerState.Ready(ad, RECT_WIDTH_DP, RECT_HEIGHT_DP)
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                        if (_state.value is BannerState.Loading) _state.value = BannerState.Failed
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
        viewActivity = null
        _state.value = BannerState.Idle
    }

    fun resetFailure() = runOnMain {
        if (_state.value is BannerState.Failed) _state.value = BannerState.Idle
    }

    companion object {
        const val LANGUAGE_KEY = "language"
    }
}

@Composable
fun RectBannerAdSlot(modifier: Modifier = Modifier) {
    val gate = koinInject<AdGate>()
    val manager = koinInject<RectBannerAdManager>()
    val activity = LocalActivity.current

    val decisions = remember(gate) { gate.decisionFlow(AdKind.RECT_BANNER, RectBannerAdManager.LANGUAGE_KEY) }
    val decision by decisions.collectAsStateWithLifecycle(initialValue = gate.decision(AdKind.RECT_BANNER, RectBannerAdManager.LANGUAGE_KEY))
    val state by manager.state.collectAsStateWithLifecycle()

    val mayLoad = decision == AdDecision.ALLOWED
    LaunchedEffect(mayLoad) { if (mayLoad) manager.ensureLoaded() }
    val hardBlocked = decision.isHardBlock
    LaunchedEffect(hardBlocked) { if (hardBlocked) manager.clear() }

    DisposableEffect(manager) { onDispose { manager.clear() } }
    val ready = state as? BannerState.Ready
    val loading = state is BannerState.Loading || (state is BannerState.Idle && mayLoad)
    val roomy = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp >= MIN_SCREEN_HEIGHT_DP
    val visible = roomy && activity != null && !hardBlocked && (ready != null || loading)
    val description = stringResource(R.string.ads_content_description)

    Box(modifier.fillMaxWidth().animateContentSize()) {
        if (visible) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .semantics { contentDescription = description },
            ) {
                HorizontalDivider(color = Orbit.extras.cardStroke, thickness = Orbit.extras.cardStrokeWidth)
                Box(
                    Modifier.fillMaxWidth().height(RECT_HEIGHT_DP.dp + 16.dp).padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (ready != null && activity != null) {
                        AndroidView(
                            modifier = Modifier.width(RECT_WIDTH_DP.dp).height(RECT_HEIGHT_DP.dp),
                            factory = { context -> FrameLayout(context) },
                            update = { container ->
                                val bannerView = ready.ad.getView(activity)
                                manager.bindActivity(activity)
                                if (bannerView.parent !== container) {
                                    (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                                    container.removeAllViews()
                                    container.addView(bannerView, FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER))
                                }
                            },
                            onRelease = { container -> container.removeAllViews() },
                        )
                    } else {
                        Text(
                            stringResource(R.string.ads_loading),
                            style = MaterialTheme.typography.labelMedium,
                            color = Orbit.extras.textMuted,
                        )
                    }
                }
            }
        }
    }
}

private const val MIN_SCREEN_HEIGHT_DP = 640
