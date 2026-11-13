package com.funsol.orbit.core.ads

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.Orbit
import org.koin.compose.koinInject

@Suppress("DEPRECATION")
@Composable
fun BannerAdSlot(placement: BannerPlacement, modifier: Modifier = Modifier) {
    val gate = koinInject<AdGate>()
    val manager = koinInject<BannerAdManager>()
    val activity = LocalActivity.current
    val widthDp = bannerCardWidthDp(LocalConfiguration.current.screenWidthDp)

    val decisions = remember(gate, placement) { gate.decisionFlow(AdKind.BANNER, placement.key) }
    val decision by decisions.collectAsStateWithLifecycle(initialValue = gate.decision(AdKind.BANNER, placement.key))
    val state by manager.state.collectAsStateWithLifecycle()
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val mayLoad = decision == AdDecision.ALLOWED
    LaunchedEffect(mayLoad, widthDp) {
        if (mayLoad) manager.ensureLoaded(widthDp)
    }
    val hardBlocked = decision.isHardBlock
    LaunchedEffect(hardBlocked) {
        if (hardBlocked && decision != AdDecision.DISABLED_REMOTE) manager.clear()
    }

    val ready = state as? BannerState.Ready
    val reservedHeightDp = remember(widthDp) { runCatching { manager.heightFor(widthDp) }.getOrDefault(DEFAULT_BANNER_HEIGHT_DP) }
    val loading = state is BannerState.Loading || (state is BannerState.Idle && mayLoad)
    val visible = activity != null && !hardBlocked && !keyboardOpen && (ready != null || loading)
    val description = stringResource(R.string.ads_content_description)
    val height = (ready?.heightDp ?: reservedHeightDp).dp
    val shape = RoundedCornerShape(BANNER_CORNER_DP.dp)

    Box(modifier.fillMaxWidth().animateContentSize(tween(220))) {
        if (visible && activity != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = BANNER_INSET_DP.dp, end = BANNER_INSET_DP.dp, bottom = 4.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(Orbit.extras.cardStrokeWidth, Orbit.extras.cardStroke, shape)
                    .height(height)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                if (ready != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(height),
                        factory = { context -> FrameLayout(context) },
                        update = { container ->
                            val bannerView = ready.ad.getView(activity)
                            manager.bindActivity(activity)
                            if (bannerView.parent !== container) {
                                (bannerView.parent as? ViewGroup)?.removeView(bannerView)
                                container.removeAllViews()
                                container.addView(
                                    bannerView,
                                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER),
                                )
                            }
                        },
                        onRelease = { container -> container.removeAllViews() },
                    )
                } else {
                    Text(stringResource(R.string.ads_loading), style = MaterialTheme.typography.labelMedium, color = Orbit.extras.textMuted)
                }
            }
        }
    }
}

private const val BANNER_INSET_DP = 12

internal fun bannerCardWidthDp(screenWidthDp: Int): Int = (screenWidthDp - 2 * BANNER_INSET_DP).coerceAtLeast(MIN_BANNER_WIDTH_DP)

private const val MIN_BANNER_WIDTH_DP = 120
private const val BANNER_CORNER_DP = 12
private const val DEFAULT_BANNER_HEIGHT_DP = 50
