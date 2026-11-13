package com.funsol.orbit.core.ads

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Spacing
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import org.koin.compose.koinInject

private val NativeCtaFill = Color(0xFF0B8FAD)

private val NativeCardRadius = 14.dp

private val NativeCardPadding = 12.dp

@Stable
class NativeAdSlotState internal constructor(
    val ad: NativeAd?,
    val loading: Boolean,
    val hardBlocked: Boolean,
) {
    val available: Boolean get() = ad != null && !hardBlocked
}

@Composable
fun rememberNativeAdState(placement: NativePlacement): NativeAdSlotState {
    val gate = koinInject<AdGate>()
    val pool = koinInject<NativeAdPool>()

    val decisions = remember(gate, placement) { gate.decisionFlow(AdKind.NATIVE, placement.key) }
    val decision by decisions.collectAsStateWithLifecycle(initialValue = gate.decision(AdKind.NATIVE, placement.key))

    var ad by remember(placement) { mutableStateOf<NativeAd?>(null) }
    var loading by remember(placement) { mutableStateOf(false) }
    val latestAd by rememberUpdatedState(ad)

    DisposableEffect(placement) {
        onDispose { latestAd?.let { pool.release(it) } }
    }

    val mayLoad = decision == AdDecision.ALLOWED
    LaunchedEffect(placement, mayLoad) {
        if (mayLoad && ad == null) {
            loading = true
            try {
                ad = pool.acquire(placement)
            } finally {
                loading = false
            }
        }
    }
    val hardBlocked = decision.isHardBlock
    LaunchedEffect(hardBlocked) {
        if (hardBlocked) {
            ad?.let { pool.release(it) }
            ad = null
        }
    }
    return NativeAdSlotState(ad, loading, hardBlocked)
}

@Composable
fun rememberNativeAdAvailable(placement: NativePlacement): Boolean = rememberNativeAdState(placement).available

@Composable
fun NativeAdSlot(
    placement: NativePlacement,
    modifier: Modifier = Modifier,
    style: NativeAdStyle = NativeAdStyle.Large,
    state: NativeAdSlotState = rememberNativeAdState(placement),
    showSkeleton: Boolean = true,
    verticalPadding: Dp = Spacing.md,
) {
    val currentAd = state.ad
    val visible = !state.hardBlocked && (currentAd != null || (state.loading && showSkeleton))
    val description = stringResource(R.string.ads_content_description)
    Box(modifier.fillMaxWidth().animateContentSize()) {
        if (visible) {
            NativeAdCard(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = verticalPadding)
                    .semantics { contentDescription = description },
            ) {
                if (currentAd != null) {
                    key(currentAd) { NativeAdViewHost(ad = currentAd, large = style == NativeAdStyle.Large) }
                } else {
                    NativeAdSkeleton(style)
                }
            }
        }
    }
}

@Composable
private fun NativeAdCard(modifier: Modifier, content: @Composable () -> Unit) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(NativeCardRadius)
    Box(
        modifier
            .clip(shape)
            .background(extras.cardBrush)
            .border(extras.cardStrokeWidth, extras.cardStroke, shape)
            .padding(NativeCardPadding),
    ) { content() }
}

@Composable
internal fun NativeAdViewHost(ad: NativeAd, large: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val palette = NativeAdPalette(
        accent = scheme.primary.toArgb(),
        text = scheme.onSurface.toArgb(),
        muted = Orbit.extras.textMuted.toArgb(),
        ctaFill = NativeCtaFill.toArgb(),
        ctaText = Color.White.toArgb(),
    )
    val maxHeight = (if (large) NativeAdMetrics.LARGE_MAX_DP else NativeAdMetrics.COMPACT_MAX_DP).dp
    AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight),
        factory = { context -> buildNativeAdView(context, ad, large, palette) },
        onRelease = { view -> tryCatch { view.destroy() } },
    )
}

@Composable
private fun NativeAdSkeleton(style: NativeAdStyle) {
    val transition = rememberInfiniteTransition(label = "adSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "adSkeletonAlpha",
    )
    val bar = Orbit.extras.well.copy(alpha = alpha)
    val large = style == NativeAdStyle.Large
    val contentHeight = if (large) NativeAdMetrics.LARGE_CONTENT_DP else NativeAdMetrics.COMPACT_CONTENT_DP
    Column(Modifier.fillMaxWidth().height(contentHeight.dp)) {
        Row(Modifier.fillMaxWidth().height(NativeAdMetrics.HEADER_DP.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(NativeAdMetrics.ICON_DP.dp).clip(RoundedCornerShape(8.dp)).background(bar))
            Column(Modifier.weight(1f).height(NativeAdMetrics.HEADER_DP.dp), verticalArrangement = Arrangement.Center) {
                Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(bar))
                Box(Modifier.padding(top = 6.dp).fillMaxWidth(0.85f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(bar))
            }
        }
        if (large) {
            Box(
                Modifier
                    .padding(top = NativeAdMetrics.LARGE_GAP_DP.dp)
                    .fillMaxWidth()
                    .height(NativeAdMetrics.LARGE_MEDIA_DP.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bar),
            )
            Box(Modifier.padding(top = NativeAdMetrics.LARGE_GAP_DP.dp).fillMaxWidth(0.8f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(bar))
        }
        Box(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(NativeAdMetrics.CTA_DP.dp)
                .clip(RoundedCornerShape(NativeAdMetrics.CTA_RADIUS_DP.dp))
                .background(bar),
        )
    }
}
