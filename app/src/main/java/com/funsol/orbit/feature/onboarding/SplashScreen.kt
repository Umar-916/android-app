package com.funsol.orbit.feature.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.AnalyticsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.network.ConnectivityNetworkObserver
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.common.HideSystemBars
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCyan
import com.funsol.orbit.core.designsystem.OrbitIndigo
import com.funsol.orbit.core.designsystem.OrbitPurple
import com.funsol.orbit.core.designsystem.OrbitVioletSoft
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.core.startup.SplashViewModel
import com.funsol.orbit.core.startup.continueTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Guards SESSION_START so it fires once per process launch even if the splash is recreated. */
@Volatile
private var sessionStartLogged = false

@Composable
fun SplashScreen(nav: OrbitNavigator) {
    HideSystemBars()
    PostAnalytic(AnalyticsEvents.SPLASH_SCREEN_DISPLAY)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (!sessionStartLogged) {
            sessionStartLogged = true
            context.postAnalytic(AnalyticsEvents.SESSION_START)
            if (ConnectivityNetworkObserver(context).isOnline()) {
                context.postAnalytic(AnalyticsEvents.SESSION_START_WITH_INTERNET_ON)
            }
        }
    }
    val vm: SplashViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    var navigated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isLoadingComplete, state.destination) {
        val destination = state.destination
        if (state.isLoadingComplete && destination != null && !navigated) {
            navigated = true
            nav.continueTo(destination)
        }
    }

    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val footer = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow)) }
        launch { delay(250); title.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { delay(450); tagline.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { delay(500); footer.animateTo(1f, tween(500)) }
    }

    val hero = Orbit.extras.heroGradient
    Box(
        Modifier
            .fillMaxSize()
            .background(OrbitIndigo)
            .drawBehind {
                drawRect(hero, alpha = 0.55f)
                drawRect(
                    Brush.radialGradient(
                        listOf(OrbitCyan.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.12f),
                        radius = size.width * 0.8f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(OrbitPurple.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                        radius = size.width * 0.95f,
                    ),
                )
            },
    ) {
        Starfield(Modifier.fillMaxSize())

        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val glowPulse = rememberPulse(2_600)
            val spin = rememberLoop(6_000)
            val ripple = rememberLoop(2_400)
            Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = logo.value.coerceIn(0f, 1f) },
            ) {
                val r = size.minDimension * (0.78f + 0.08f * glowPulse.value)
                drawCircle(
                    Brush.radialGradient(
                        0f to OrbitVioletSoft.copy(alpha = 0.34f),
                        0.5f to OrbitPurple.copy(alpha = 0.16f),
                        1f to Color.Transparent,
                        center = center,
                        radius = r,
                    ),
                    radius = r,
                )
            }
            // Orbiting satellites + expanding ring: simple, smooth motion around the emblem.
            Canvas(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = logo.value.coerceIn(0f, 1f) },
            ) {
                val c = center
                val base = size.minDimension * 0.5f
                val ringR = base * (0.92f + 0.10f * ripple.value)
                drawCircle(
                    Color.White.copy(alpha = 0.22f * (1f - ripple.value)),
                    radius = ringR,
                    center = c,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()),
                )
                val orbitR = base * 0.98f
                val a1 = spin.value * 2f * Math.PI.toFloat()
                val a2 = a1 + Math.PI.toFloat()
                drawCircle(OrbitCyan, 5.dp.toPx(), Offset(c.x + orbitR * kotlin.math.cos(a1), c.y + orbitR * kotlin.math.sin(a1) * 0.92f))
                drawCircle(Color.White.copy(alpha = 0.85f), 3.5.dp.toPx(), Offset(c.x + orbitR * kotlin.math.cos(a2), c.y + orbitR * kotlin.math.sin(a2) * 0.92f))
            }
            OrbitEmblem(
                Modifier
                    .fillMaxWidth(0.66f)
                    .widthIn(max = 280.dp)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        alpha = logo.value.coerceIn(0f, 1f)
                        scaleX = 0.6f + 0.4f * logo.value
                        scaleY = 0.6f + 0.4f * logo.value
                    },
            )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.startup_brand),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                letterSpacing = 8.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .graphicsLayer {
                        alpha = title.value
                        translationY = (1f - title.value) * 16.dp.toPx()
                    }
                    .semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .graphicsLayer {
                        alpha = tagline.value
                        translationY = (1f - tagline.value) * 10.dp.toPx()
                    },
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 32.dp, end = 32.dp, bottom = 32.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer { alpha = footer.value },
            contentAlignment = Alignment.Center,
        ) {
            if (state.error) {
                SplashError(onRetry = {
                    context.postAnalytic(AnalyticsEvents.SPLASH_RETRY_CLICK)
                    vm.retry()
                })
            } else {
                SplashProgress(progress = state.progress)
            }
        }
        if (state.showingAd) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.ads_loading), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SplashProgress(progress: Float) {
    val almost = progress >= 0.8f
    val description = stringResource(R.string.startup_progress_description, (progress * 100).toInt())
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SegmentedProgress(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
        )
        Box(Modifier.fillMaxWidth().heightIn(min = 44.dp), contentAlignment = Alignment.TopCenter) {
            Crossfade(targetState = almost, animationSpec = tween(300), label = "status") { isAlmost ->
                Text(
                    stringResource(if (isAlmost) R.string.startup_status_4 else R.string.startup_status_1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun SplashError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        Text(
            stringResource(R.string.startup_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.startup_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        GradientButton(text = stringResource(R.string.action_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
    }
}
