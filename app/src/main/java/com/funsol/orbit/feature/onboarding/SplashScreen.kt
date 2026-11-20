package com.funsol.orbit.feature.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.AnalyticsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.HideSystemBars
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitCoral
import com.funsol.orbit.core.designsystem.OrbitCyan
import com.funsol.orbit.core.designsystem.OrbitIndigo
import com.funsol.orbit.core.designsystem.OrbitPurple
import com.funsol.orbit.core.designsystem.OrbitVioletSoft
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.core.network.ConnectivityNetworkObserver
import com.funsol.orbit.core.startup.SplashViewModel
import com.funsol.orbit.core.startup.continueTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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

    val arrival = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val emblem = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val underline = remember { Animatable(0f) }
    val footer = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { arrival.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch {
            delay(470)
            launch { burst.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing)) }
            emblem.animateTo(1f, tween(2200, easing = FastOutSlowInEasing))
        }
        launch { delay(1700); title.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        launch { delay(2100); underline.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
        launch { delay(2100); footer.animateTo(1f, tween(700)) }
    }

    var emblemCenter by remember { mutableStateOf<Offset?>(null) }
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
                drawRect(
                    Brush.radialGradient(
                        listOf(OrbitVioletSoft.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.40f),
                        radius = size.width * 0.70f,
                    ),
                )
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, OrbitIndigo.copy(alpha = 0.70f)),
                        startY = size.height * 0.60f,
                        endY = size.height,
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
            val floatBob = rememberPulse(3_200)

            Box(
                Modifier
                    .graphicsLayer {
                        alpha = (emblem.value * 4f).coerceIn(0f, 1f)
                        translationY = (floatBob.value - 0.5f) * 8.dp.toPx() * emblem.value
                    }
                    .onGloballyPositioned { emblemCenter = it.boundsInRoot().center },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val r = size.minDimension * (0.46f + 0.04f * glowPulse.value)
                    drawCircle(
                        Brush.radialGradient(
                            0f to OrbitVioletSoft.copy(alpha = 0.42f * emblem.value),
                            0.55f to OrbitPurple.copy(alpha = 0.20f * emblem.value),
                            1f to Color.Transparent,
                            center = center,
                            radius = r,
                        ),
                        radius = r,
                    )
                }
                OrbitEmblem(
                    Modifier
                        .fillMaxWidth(0.66f)
                        .widthIn(max = 280.dp)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            val s = 0.94f + 0.06f * emblem.value
                            scaleX = s
                            scaleY = s
                        },
                    build = { emblem.value },
                )
            }
            Spacer(Modifier.height(26.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        val t = title.value
                        alpha = t.coerceIn(0f, 1f)
                        translationX = (1f - t) * -80.dp.toPx()
                        val s = 0.90f + 0.10f * t
                        scaleX = s
                        scaleY = s
                    },
            ) {
                OrbitWordmark(
                    brand = stringResource(R.string.startup_brand),
                    product = stringResource(R.string.startup_tagline),
                    reveal = { title.value },
                )
            }
        }

        SparkArrival(
            target = emblemCenter,
            arrival = { arrival.value },
            burst = { burst.value },
            modifier = Modifier.matchParentSize(),
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 32.dp, end = 32.dp, bottom = 40.dp)
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

/**
 * Full-screen overlay that draws the incoming spark and its burst at the emblem
 * position. Reads its animated values inside the draw scope so no recomposition
 * happens while the spark travels.
 */
@Composable
private fun SparkArrival(
    target: Offset?,
    arrival: () -> Float,
    burst: () -> Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val landing = target ?: Offset(size.width / 2f, size.height * 0.42f)
        val travelled = arrival()
        val explosion = burst()
        if (travelled > 0f && explosion < 1f) drawComet(landing, travelled)
        if (explosion > 0f) drawBurst(landing, explosion)
    }
}

private fun DrawScope.drawComet(target: Offset, arrival: Float) {
    val start = Offset(size.width * 1.06f, -size.height * 0.04f)
    val control = Offset(size.width * 0.58f, target.y - size.height * 0.30f)
    val t = arrival * arrival

    fun pointAt(at: Float): Offset {
        val u = 1f - at
        return Offset(
            u * u * start.x + 2f * u * at * control.x + at * at * target.x,
            u * u * start.y + 2f * u * at * control.y + at * at * target.y,
        )
    }

    val head = pointAt(t)
    val tail = pointAt((t - 0.16f).coerceAtLeast(0f))
    drawLine(
        brush = Brush.linearGradient(
            listOf(Color.Transparent, OrbitAmber.copy(alpha = 0.55f), Color.White.copy(alpha = 0.9f)),
            start = tail,
            end = head,
        ),
        start = tail,
        end = head,
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
    for (i in 0 until 14) {
        val at = t - i * 0.012f
        if (at <= 0f) continue
        val fade = 1f - i / 14f
        drawCircle(
            color = OrbitAmber.copy(alpha = 0.30f * fade),
            radius = (2.4f + 2.4f * fade).dp.toPx(),
            center = pointAt(at),
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White, OrbitAmber.copy(alpha = 0.72f), OrbitCoral.copy(alpha = 0.22f), Color.Transparent),
            center = head,
            radius = 22.dp.toPx(),
        ),
        radius = 22.dp.toPx(),
        center = head,
    )
    drawCircle(Color.White, 4.dp.toPx(), head)
}

private fun DrawScope.drawBurst(target: Offset, burst: Float) {
    val fade = (1f - burst).coerceIn(0f, 1f)
    val bloom = (30f + 70f * burst).dp.toPx()
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.55f * fade), OrbitAmber.copy(alpha = 0.22f * fade), Color.Transparent),
            center = target,
            radius = bloom,
        ),
        radius = bloom,
        center = target,
    )
}

@Composable
private fun OrbitWordmark(
    brand: String,
    product: String,
    reveal: () -> Float,
    modifier: Modifier = Modifier,
) {
    val accent = Brush.horizontalGradient(listOf(OrbitVioletSoft, OrbitCyan))
    val text = androidx.compose.ui.text.buildAnnotatedString {
        append(brand)
        append(" ")
        withStyle(androidx.compose.ui.text.SpanStyle(brush = accent)) { append(product) }
    }
    Text(
        text,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
        ),
        color = Color.White,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        modifier = modifier.graphicsLayer {
            val t = reveal()
            alpha = t.coerceIn(0f, 1f)
            val s = 0.85f + 0.15f * t
            scaleX = s
            scaleY = s
        },
    )
}

@Composable
private fun SplashProgress(progress: Float) {
    val percent = (progress.coerceIn(0f, 1f) * 100).roundToInt()
    val description = stringResource(R.string.startup_progress_description, percent)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        SplashProgressBar(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clearAndSetSemantics {
                    contentDescription = description
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
        )
    }
}

@Composable
private fun SplashProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "splashProgress",
    )
    val flow = rememberLoop(2_400)
    Canvas(modifier.fillMaxWidth().height(20.dp)) {
        val barHeight = 9.dp.toPx()
        val top = (size.height - barHeight) / 2f
        val radius = barHeight / 2f
        val corner = CornerRadius(radius, radius)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(0f, top),
            size = Size(size.width, barHeight),
            cornerRadius = corner,
        )

        val filled = size.width * animated
        if (filled > 1f) {
            val shift = flow.value * (size.width + filled) - filled
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(OrbitCyan, OrbitPurple, OrbitVioletSoft, OrbitAmber),
                    start = Offset(shift, top),
                    end = Offset(shift + filled, top + barHeight),
                ),
                topLeft = Offset(0f, top),
                size = Size(filled, barHeight),
                cornerRadius = corner,
            )

            val sheenX = (flow.value * 2f - 0.5f) * filled
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.50f), Color.Transparent),
                    startX = sheenX - 24.dp.toPx(),
                    endX = sheenX + 24.dp.toPx(),
                ),
                topLeft = Offset(0f, top),
                size = Size(filled, barHeight),
                cornerRadius = corner,
            )
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
