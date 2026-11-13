package com.funsol.orbit.feature.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.core.designsystem.OrbitCoral
import com.funsol.orbit.core.designsystem.OrbitCyan
import com.funsol.orbit.core.designsystem.OrbitPurple
import com.funsol.orbit.core.designsystem.OrbitViolet
import com.funsol.orbit.core.designsystem.OrbitVioletSoft
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
internal fun rememberLoop(durationMs: Int): State<Float> =
    rememberInfiniteTransition(label = "loop").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing)),
        label = "loopValue",
    )

@Composable
internal fun rememberPulse(durationMs: Int = 1800): State<Float> =
    rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseValue",
    )

private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

@Composable
internal fun Starfield(modifier: Modifier = Modifier, count: Int = 46, color: Color = Color.White, seed: Int = 7) {
    val stars = remember(count, seed) {
        val random = Random(seed)
        List(count) { Star(random.nextFloat(), random.nextFloat(), 0.6f + random.nextFloat() * 1.5f, random.nextFloat()) }
    }
    val time = rememberLoop(4200)
    Canvas(modifier.clearAndSetSemantics { }) {
        val t = time.value
        stars.forEach { star ->
            val twinkle = 0.5f + 0.5f * sin(2.0 * PI * (t + star.phase)).toFloat()
            drawCircle(
                color = color.copy(alpha = 0.2f + 0.8f * twinkle),
                radius = star.radius.dp.toPx(),
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

internal fun DrawScope.glowDot(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.7f), Color.Transparent), center = center, radius = radius * 3.2f),
        radius = radius * 3.2f,
        center = center,
    )
    drawCircle(color, radius, center)
    drawCircle(Color.White.copy(alpha = 0.85f), radius * 0.45f, center)
}

private fun orbitPoint(center: Offset, radius: Float, degrees: Float): Offset {
    val rad = degrees * (PI / 180.0)
    return Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
}

@Composable
internal fun OrbitEmblem(modifier: Modifier = Modifier) {
    val slow = rememberLoop(18_000)
    val fast = rememberLoop(9_000)
    val pulse = rememberPulse(1_800)
    Canvas(modifier.clearAndSetSemantics { }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val p = pulse.value

        drawCircle(
            brush = Brush.radialGradient(
                listOf(OrbitVioletSoft.copy(alpha = 0.50f + 0.20f * p), OrbitPurple.copy(alpha = 0.16f), Color.Transparent),
                center = c,
                radius = r,
            ),
            radius = r,
            center = c,
        )

        val outer = r * 0.90f
        rotate(slow.value * 360f, c) {
            drawCircle(
                color = Color.White.copy(alpha = 0.30f),
                radius = outer,
                center = c,
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 9.dp.toPx()))),
            )
            glowDot(orbitPoint(c, outer, -35f), 4.5.dp.toPx(), OrbitCyan)
            glowDot(orbitPoint(c, outer, 145f), 3.dp.toPx(), Color.White)
        }

        val mid = r * 0.68f
        rotate(-fast.value * 360f, c) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, OrbitCyan, OrbitPurple, Color.Transparent), center = c),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(c.x - mid, c.y - mid),
                size = Size(mid * 2f, mid * 2f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
            glowDot(orbitPoint(c, mid, 8f), 4.dp.toPx(), OrbitCoral)
        }

        val inner = r * 0.46f
        drawCircle(Color.White.copy(alpha = 0.35f), inner, c, style = Stroke(1.dp.toPx()))
        glowDot(orbitPoint(c, inner, fast.value * 720f + 60f), 2.5.dp.toPx(), Color.White)

        val core = r * 0.27f * (0.94f + 0.12f * p)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White, OrbitVioletSoft, OrbitViolet), center = c, radius = core * 1.05f),
            radius = core,
            center = c,
        )
        drawCircle(Color.White.copy(alpha = 0.85f), core * 0.26f, Offset(c.x - core * 0.28f, c.y - core * 0.32f))
    }
}

@Composable
internal fun SegmentedProgress(progress: Float, modifier: Modifier = Modifier, segments: Int = 14) {
    val animated = animateFloatAsState(progress.coerceIn(0f, 1f), tween(180, easing = LinearEasing), label = "segProgress")
    Row(modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(segments) { index ->
            val tint = lerp(OrbitCyan, OrbitCoral, index / (segments - 1f))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .drawBehind {
                        val fill = (animated.value * segments - index).coerceIn(0f, 1f)
                        if (fill > 0f) {
                            val width = size.width * fill
                            val left = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                            drawRect(tint, topLeft = Offset(left, 0f), size = Size(width, size.height))
                        }
                    },
            )
        }
    }
}
