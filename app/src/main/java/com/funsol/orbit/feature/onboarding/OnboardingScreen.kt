package com.funsol.orbit.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.funsol.orbit.core.ads.NativeAdSlot
import com.funsol.orbit.core.ads.NativeAdSlotState
import com.funsol.orbit.core.ads.NativeAdStyle
import com.funsol.orbit.core.ads.NativePlacement
import com.funsol.orbit.core.ads.rememberNativeAdState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.OnboardingEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.core.startup.continueTo
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

private class OnboardingPage(val accent: FeatureAccent, val title: Int, val body: Int)

private val pages = listOf(
    OnboardingPage(FeatureAccent.TASKS, R.string.onb_page1_title, R.string.onb_page1_body),
    OnboardingPage(FeatureAccent.PROJECTS, R.string.onb_page2_title, R.string.onb_page2_body),
    OnboardingPage(FeatureAccent.HABITS, R.string.onb_page3_title, R.string.onb_page3_body),
    OnboardingPage(
        FeatureAccent.PRIVACY,
        R.string.onb_page4_title,
        if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) R.string.onb_page4_body else R.string.onb_page4_body_offline,
    ),
)

@Composable
fun OnboardingScreen(replay: Boolean, nav: OrbitNavigator) {
    PostAnalytic(OnboardingEvents.ONBOARDING_SCREEN_DISPLAY)
    val context = LocalContext.current
    val vm: OnboardingViewModel = koinViewModel()
    val pagerState = rememberPagerState { pages.size }
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> OnboardingEvents.ONBOARDING_PAGE1_DISPLAY
            1 -> OnboardingEvents.ONBOARDING_PAGE2_DISPLAY
            2 -> OnboardingEvents.ONBOARDING_PAGE3_DISPLAY
            else -> OnboardingEvents.ONBOARDING_PAGE4_DISPLAY
        }.let { context.postAnalytic(it) }
    }
    val scope = rememberCoroutineScope()
    val guard = rememberClickGuard(800L)
    val extras = Orbit.extras
    val accentColors = remember(extras) { pages.map { extras.accent(it.accent) } }
    val isLast = pagerState.currentPage == pages.lastIndex
    val adState = rememberNativeAdState(NativePlacement.ONBOARDING)

    fun finish() {
        guard.run {
            vm.finish(replay) { next -> if (next != null) nav.continueTo(next) else nav.back() }
        }
    }

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val isDark = extras.isDark
    val topColor = if (isDark) bg else lerp(bg, primary, 0.05f)
    val bottomColor = if (isDark) bg else lerp(bg, Color.White, 0.75f)
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .drawBehind {
                val page = pagerState.currentPage
                val fraction = pagerState.currentPageOffsetFraction
                val neighbour = (page + if (fraction >= 0f) 1 else -1).coerceIn(0, pages.lastIndex)
                val tint = lerp(accentColors[page], accentColors[neighbour], abs(fraction))
                drawRect(
                    Brush.radialGradient(
                        0f to tint.copy(alpha = if (isDark) 0.20f else 0.09f),
                        1f to Color.Transparent,
                        center = Offset(size.width / 2f, size.height * 0.24f),
                        radius = size.width * 1.0f,
                    ),
                )
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.sm),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isLast) {
                    TextButton(onClick = {
                        context.postAnalytic(OnboardingEvents.ONBOARDING_SKIP_CLICK)
                        finish()
                    }) {
                        Text(stringResource(R.string.action_skip), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { index ->
                PageContent(index, pagerState)
            }

            OnboardingBottom(
                adState = adState,
                pagerState = pagerState,
                buttonText = stringResource(if (isLast) R.string.onb_get_started else R.string.action_next),
                accent = pages[pagerState.currentPage].accent,
                onNext = {
                    context.postAnalytic(
                        if (isLast) OnboardingEvents.ONBOARDING_GET_STARTED_CLICK else OnboardingEvents.ONBOARDING_NEXT_CLICK,
                    )
                    if (isLast) finish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
            )
        }
    }
}

@Composable
private fun OnboardingBottom(
    adState: NativeAdSlotState,
    pagerState: PagerState,
    buttonText: String,
    accent: FeatureAccent,
    onNext: () -> Unit,
) {
    val hasAd = com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM && adState.available
    AnimatedContent(
        targetState = hasAd,
        modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
        label = "onboardingBottom",
    ) { withAd ->
        if (withAd) {
            Column(Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PageIndicator(pagerState)
                    OnboardingNextPill(buttonText, accent, onNext)
                }
                Spacer(Modifier.height(12.dp))
                NativeAdSlot(
                    NativePlacement.ONBOARDING,
                    style = NativeAdStyle.Compact,
                    state = adState,
                    showSkeleton = false,
                    verticalPadding = 0.dp,
                )
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PageIndicator(pagerState, Modifier.padding(vertical = Spacing.md))
                OnboardingButton(
                    text = buttonText,
                    accent = accent,
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    showArrow = pagerState.currentPage < pages.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun OnboardingNextPill(text: String, accent: FeatureAccent, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val extras = Orbit.extras
    val from by animateColorAsState(extras.accent(accent), tween(280), label = "pillFrom")
    val to by animateColorAsState(accent.to, tween(280), label = "pillTo")
    val shape = RoundedCornerShape(21.dp)
    Row(
        modifier
            .defaultMinSize(minWidth = 96.dp, minHeight = 42.dp)
            .orbitGlow(shape, from.copy(alpha = 0.22f), radius = 8.dp, offsetY = 2.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(from, to)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun OnboardingButton(text: String, accent: FeatureAccent, onClick: () -> Unit, modifier: Modifier = Modifier, showArrow: Boolean = false) {
    val extras = Orbit.extras
    val from by animateColorAsState(extras.accent(accent), tween(280), label = "ctaFrom")
    val to by animateColorAsState(accent.to, tween(280), label = "ctaTo")
    val shape = RoundedCornerShape(Radius.button)
    Box(
        modifier
            .heightIn(min = 52.dp)
            .orbitGlow(shape, from.copy(alpha = 0.22f), radius = 10.dp, offsetY = 3.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(from, to)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (showArrow) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PageContent(index: Int, pagerState: PagerState) {
    val page = pages[index]
    val offset = { pagerState.currentPage - index + pagerState.currentPageOffsetFraction }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artSize = minOf(ART_CANVAS_SIZE, maxWidth - Spacing.lg * 2, maxHeight * 0.46f).coerceAtLeast(112.dp)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OnboardingArt(
                page = index,
                accent = page.accent,
                offset = offset,
                modifier = Modifier
                    .size(artSize)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        val o = offset().coerceIn(-1f, 1f)
                        val s = 1f - 0.08f * abs(o)
                        scaleX = s
                        scaleY = s
                        alpha = 1f - 0.6f * abs(o)
                    },
            )
            Spacer(Modifier.height(Spacing.lg))
            Column(
                Modifier.graphicsLayer {
                    val o = offset().coerceIn(-1f, 1f)
                    translationX = o * size.width * 0.06f
                    alpha = 1f - abs(o)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(page.title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(page.body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Orbit.extras.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    val extras = Orbit.extras
    val inactive = MaterialTheme.colorScheme.outlineVariant
    val froms = remember(extras) { pages.map { extras.accent(it.accent) } }
    val tos = remember { pages.map { it.accent.to } }
    val description = stringResource(R.string.onb_page_indicator, pagerState.currentPage + 1, pages.size)
    val count = pages.size
    Box(
        modifier
            .clip(CircleShape)
            .background(inactive.copy(alpha = 0.28f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Canvas(Modifier.size(width = 28.dp + 8.dp * (count - 1) + 6.dp * (count - 1), height = 8.dp)) {
            val dot = 8.dp.toPx()
            val extra = 20.dp.toPx()
            val gap = 6.dp.toPx()
            val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val rtl = layoutDirection == LayoutDirection.Rtl
            var x = 0f
            for (i in 0 until count) {
                val a = (1f - abs(position - i)).coerceIn(0f, 1f)
                val w = dot + extra * a
                val left = if (rtl) size.width - x - w else x
                val c0 = lerp(inactive, froms[i], a)
                val c1 = lerp(inactive, tos[i], a)
                drawRoundRect(
                    Brush.horizontalGradient(if (rtl) listOf(c1, c0) else listOf(c0, c1), startX = left, endX = left + w),
                    topLeft = Offset(left, 0f),
                    size = Size(w, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
                x += w + gap
            }
        }
    }
}
