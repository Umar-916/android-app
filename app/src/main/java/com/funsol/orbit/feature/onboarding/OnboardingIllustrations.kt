package com.funsol.orbit.feature.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.orbitGlow
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun OnboardingArt(page: Int, accent: FeatureAccent, offset: () -> Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        val scale = (minOf(maxWidth, maxHeight) / ART_CANVAS_SIZE).coerceIn(0.1f, 1f)
        Box(
            Modifier
                .requiredSize(ART_CANVAS_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center,
        ) {
            ArtBackdrop(accent, offset)
            when (page) {
                0 -> TasksArt(accent)
                1 -> OrganizeArt()
                2 -> HabitsArt(accent)
                else -> PrivacyArt(accent)
            }
        }
    }
}

internal val ART_CANVAS_SIZE = 256.dp

private val CARD_RADIUS = 26.dp
private val BADGE_SIZE = 38.dp

private fun Modifier.fromCentre(x: Dp, y: Dp): Modifier = this.offset(x, y)

@Composable
private fun ArtBackdrop(accent: FeatureAccent, offset: () -> Float) {
    val extras = Orbit.extras
    val color = extras.accent(accent)
    val spin = rememberArtLoop(40_000, 0.3f)
    val glowAlpha = if (extras.isDark) 0.26f else 0.20f
    val ringAlpha = if (extras.isDark) 0.28f else 0.26f
    Canvas(Modifier.fillMaxSize().graphicsLayer { translationX = offset() * size.width * 0.06f }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val glowRadius = size.minDimension * 0.5f
        drawCircle(
            Brush.radialGradient(
                0f to color.copy(alpha = glowAlpha),
                0.45f to color.copy(alpha = glowAlpha * 0.62f),
                0.75f to color.copy(alpha = glowAlpha * 0.2f),
                1f to Color.Transparent,
                center = c,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = c,
        )
        val ringRadius = size.minDimension * 0.40f
        drawCircle(
            color.copy(alpha = ringAlpha), ringRadius, c,
            style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 9.dp.toPx()))),
        )
        val rad = (spin.value * 360f - 40f) * PI / 180.0
        val dot = Offset(c.x + ringRadius * cos(rad).toFloat(), c.y + ringRadius * sin(rad).toFloat())
        drawCircle(color.copy(alpha = 0.18f), 7.dp.toPx(), dot)
        drawCircle(color, 3.5.dp.toPx(), dot)
    }
}

@Composable
private fun Badge(
    icon: ImageVector,
    accent: FeatureAccent,
    modifier: Modifier = Modifier,
    reverse: Boolean = false,
    durationMs: Int = 2400,
) {
    val extras = Orbit.extras
    val tint = extras.accent(accent)
    val bob = rememberArtPulse(durationMs)
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .graphicsLayer {
                val v = if (reverse) 1f - bob.value else bob.value
                translationY = (v - 0.5f) * 12.dp.toPx()
            }
            .size(BADGE_SIZE)
            .orbitGlow(shape, extras.cardGlow, radius = 10.dp, offsetY = 3.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.06f))))
            .border(0.8.dp, tint.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ArtCard(
    modifier: Modifier = Modifier,
    radius: Dp = CARD_RADIUS,
    padding: Dp = 12.dp,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .orbitGlow(shape, extras.cardGlow, radius = 18.dp, offsetY = 8.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(extras.cardStrokeWidth, extras.cardStroke, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
private fun ImagePanel(
    accent: FeatureAccent,
    height: Dp,
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    shimmer: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val extras = Orbit.extras
    val tint = extras.accent(accent)
    val sweep = rememberArtLoop(7_000, 2f)
    val shape = RoundedCornerShape(radius)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.06f))))
            .drawBehind {
                drawCircle(tint.copy(alpha = 0.10f), size.height * 0.52f, Offset(size.width * 0.16f, size.height * 0.88f))
                drawCircle(tint.copy(alpha = 0.08f), size.height * 0.38f, Offset(size.width * 0.86f, size.height * 0.14f))
                if (shimmer) {
                    val band = size.width * 0.35f
                    val x = (sweep.value * 1.6f - 0.3f) * size.width
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = if (extras.isDark) 0.08f else 0.55f), Color.Transparent),
                            startX = x - band,
                            endX = x + band,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun PanelGlyph(icon: ImageVector, accent: FeatureAccent, size: Dp) {
    Icon(icon, contentDescription = null, tint = Orbit.extras.accent(accent).copy(alpha = 0.6f), modifier = Modifier.size(size))
}

@Composable
private fun SkeletonBar(fraction: Float, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun BoxScope.TasksArt(accent: FeatureAccent) {
    val loop = rememberArtLoop(4_800)
    val first by remember { derivedStateOf { loop.value > 0.18f && loop.value < 0.96f } }
    val second by remember { derivedStateOf { loop.value > 0.40f && loop.value < 0.96f } }
    val progress = animateFloatAsState(
        0.12f + 0.44f * ((if (first) 1 else 0) + (if (second) 1 else 0)),
        tween(600, easing = FastOutSlowInEasing),
        label = "tasksProgress",
    )
    ArtCard(Modifier.width(168.dp).align(Alignment.Center)) {
        ImagePanel(accent, 64.dp, shimmer = true) { PanelGlyph(Icons.Rounded.TaskAlt, accent, 34.dp) }
        SkeletonBar(0.62f, 10.dp)
        TaskRow(first, accent, 0.95f)
        TaskRow(second, accent, 0.82f)
        ProgressPill(progress, accent)
    }
    Badge(Icons.Rounded.Notifications, FeatureAccent.CALENDAR, Modifier.align(Alignment.Center).fromCentre(108.dp, (-84).dp), false, 2_300)
    Badge(Icons.Rounded.Flag, FeatureAccent.GOALS, Modifier.align(Alignment.Center).fromCentre((-108).dp, 84.dp), true, 2_300)
}

@Composable
private fun TaskRow(checked: Boolean, accent: FeatureAccent, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CheckBubble(checked, accent)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
            SkeletonBar(fraction, 8.dp)
            SkeletonBar(0.5f, 6.dp)
        }
    }
}

@Composable
private fun CheckBubble(checked: Boolean, accent: FeatureAccent) {
    val extras = Orbit.extras
    val scale by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "check")
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(1.6.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = scale.coerceIn(0f, 1f) }
                .background(extras.accentGradient(accent), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ProgressPill(progress: State<Float>, accent: FeatureAccent) {
    val extras = Orbit.extras
    val from = extras.accent(accent)
    val to = accent.to
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            .drawBehind {
                val width = size.width * progress.value.coerceIn(0f, 1f)
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRoundRect(
                    Brush.horizontalGradient(listOf(from.copy(alpha = 0.75f), to.copy(alpha = 0.75f)), startX = left, endX = left + width),
                    topLeft = Offset(left, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            },
    )
}

@Composable
private fun BoxScope.OrganizeArt() {
    MiniCard(FeatureAccent.PROJECTS, Icons.Rounded.Folder, rotation = -10f, dx = (-56).dp, dy = 12.dp, bobMs = 2_700, reverse = false)
    MiniCard(FeatureAccent.NOTES, Icons.Rounded.StickyNote2, rotation = 10f, dx = 56.dp, dy = 12.dp, bobMs = 2_700, reverse = true)
    MiniCard(FeatureAccent.GOALS, Icons.Rounded.EmojiEvents, rotation = 0f, dx = 0.dp, dy = (-8).dp, bobMs = 3_100, reverse = false)
    Badge(Icons.Rounded.Search, FeatureAccent.ANALYTICS, Modifier.align(Alignment.Center).fromCentre(100.dp, (-98).dp), false, 2_500)
    Badge(Icons.Rounded.Notifications, FeatureAccent.CALENDAR, Modifier.align(Alignment.Center).fromCentre((-100).dp, (-98).dp), true, 2_500)
}

@Composable
private fun BoxScope.MiniCard(accent: FeatureAccent, icon: ImageVector, rotation: Float, dx: Dp, dy: Dp, bobMs: Int, reverse: Boolean) {
    val bob = rememberArtPulse(bobMs)
    ArtCard(
        Modifier
            .align(Alignment.Center)
            .offset(dx, dy)
            .graphicsLayer {
                rotationZ = rotation
                val v = if (reverse) 1f - bob.value else bob.value
                translationY = (v - 0.5f) * 8.dp.toPx()
            }
            .size(width = 92.dp, height = 120.dp),
        radius = 20.dp,
        padding = 8.dp,
        spacing = 8.dp,
    ) {
        ImagePanel(accent, 56.dp, radius = 14.dp) { PanelGlyph(icon, accent, 26.dp) }
        Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBar(0.85f, 8.dp)
            SkeletonBar(0.55f, 6.dp)
        }
    }
}

@Composable
private fun BoxScope.HabitsArt(accent: FeatureAccent) {
    val extras = Orbit.extras
    val pulse = rememberArtPulse(3_600)
    val percent by remember { derivedStateOf { (25f + 65f * pulse.value).roundToInt() } }
    val track = MaterialTheme.colorScheme.outlineVariant
    val from = extras.accent(accent)
    val to = accent.to
    val percentFormat = remember { NumberFormat.getPercentInstance() }
    val numberFormat = remember { NumberFormat.getInstance() }
    ArtCard(Modifier.width(168.dp).align(Alignment.Center), padding = 14.dp) {
        ImagePanel(accent, 136.dp, radius = 20.dp) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    rotate(-90f, c) {
                        drawArc(
                            brush = Brush.sweepGradient(listOf(from, to, from), center = c),
                            startAngle = 0f,
                            sweepAngle = 360f * (0.25f + 0.65f * pulse.value),
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Text(
                    percentFormat.format(percent / 100.0),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 1,
                )
            }
        }
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(7) { index ->
                val filled by remember { derivedStateOf { percent > 10 + index * 11 } }
                DayDot(filled, today = index == 3, accent = accent)
            }
        }
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(extras.amber.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = extras.amber.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
            Text(
                numberFormat.format(7),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
    Badge(Icons.Rounded.LocalFireDepartment, FeatureAccent.NOTES, Modifier.align(Alignment.Center).fromCentre((-108).dp, (-72).dp), false, 2_400)
    Badge(Icons.Rounded.EmojiEvents, FeatureAccent.GOALS, Modifier.align(Alignment.Center).fromCentre(108.dp, 72.dp), true, 2_400)
}

@Composable
private fun DayDot(filled: Boolean, today: Boolean, accent: FeatureAccent) {
    val scale by animateFloatAsState(if (filled) 1f else 0f, spring(dampingRatio = 0.55f), label = "dayDot")
    val tint = Orbit.extras.accent(accent)
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(
        Modifier
            .size(14.dp)
            .clip(CircleShape)
            .border(if (today) 1.5.dp else 1.2.dp, if (today) tint.copy(alpha = 0.85f) else outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val fillSize = if (today) 6.dp else 14.dp
        Box(
            Modifier
                .size(fillSize)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .background(Orbit.extras.accentGradient(accent), CircleShape),
        )
    }
}

@Composable
private fun BoxScope.PrivacyArt(accent: FeatureAccent) {
    val extras = Orbit.extras
    val tint = extras.accent(accent)
    val pulse = rememberArtPulse(2_400)
    ArtCard(Modifier.width(168.dp).align(Alignment.Center)) {
        ImagePanel(accent, 92.dp) {
            Canvas(Modifier.size(84.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = 28.dp.toPx() + 12.dp.toPx() * pulse.value
                drawCircle(tint.copy(alpha = 0.26f * (1f - pulse.value)), r, c, style = Stroke(1.5.dp.toPx()))
            }
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f))
                    .border(0.8.dp, tint.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = tint.copy(alpha = 0.65f), modifier = Modifier.size(26.dp))
            }
        }
        PrivacyRow(Icons.Rounded.PhoneAndroid, accent, 0.8f)
        PrivacyRow(Icons.Rounded.Shield, accent, 0.62f)
        PrivacyRow(Icons.Rounded.CloudOff, accent, 0.72f)
    }
    Badge(Icons.Rounded.CloudOff, FeatureAccent.ANALYTICS, Modifier.align(Alignment.Center).fromCentre(108.dp, (-72).dp), false, 2_600)
    Badge(Icons.Rounded.Shield, FeatureAccent.HABITS, Modifier.align(Alignment.Center).fromCentre((-108).dp, 72.dp), true, 2_600)
}

@Composable
private fun PrivacyRow(icon: ImageVector, accent: FeatureAccent, fraction: Float) {
    val tint = Orbit.extras.accent(accent)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            SkeletonBar(fraction, 8.dp)
            SkeletonBar(fraction * 0.55f, 5.dp)
        }
    }
}
