package com.funsol.orbit.feature.premium

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitCoral
import com.funsol.orbit.core.designsystem.OrbitCyan
import com.funsol.orbit.core.designsystem.OrbitPurple
import com.funsol.orbit.core.designsystem.OrbitViolet
import com.funsol.orbit.core.designsystem.OrbitVioletSoft
import com.funsol.orbit.feature.onboarding.glowDot
import com.funsol.orbit.feature.onboarding.rememberLoop
import com.funsol.orbit.feature.onboarding.rememberPulse
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun orbitPointAt(center: Offset, radius: Float, degrees: Float): Offset {
    val rad = degrees * (PI / 180.0)
    return Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
}

internal fun DrawScope.drawOrbitMark(
    c: Offset,
    r: Float,
    loop: Float,
    pulse: Float,
    orbitColor: Color = OrbitViolet.copy(alpha = 0.40f),
    glow: Color = OrbitViolet,
    glowAlpha: Float = 0.20f,
    discColors: List<Color> = listOf(OrbitViolet, OrbitPurple),
    markColor: Color = Color.White,
    satellite: Color = OrbitAmber,
) {
    drawCircle(
        brush = Brush.radialGradient(listOf(glow.copy(alpha = glowAlpha + 0.18f * pulse), Color.Transparent), center = c, radius = r),
        radius = r,
        center = c,
    )
    val orbit = r * 0.78f
    drawCircle(orbitColor, orbit, c, style = Stroke(1.5.dp.toPx()))
    val angle = loop * 360f - 40f
    glowDot(orbitPointAt(c, orbit, angle), r * 0.07f, satellite)
    drawCircle(orbitColor.copy(alpha = 0.9f), r * 0.028f, orbitPointAt(c, orbit, angle + 180f))
    val d = r * 0.52f * (0.975f + 0.04f * pulse)
    drawCircle(
        brush = Brush.linearGradient(discColors, start = Offset(c.x - d, c.y - d), end = Offset(c.x + d, c.y + d)),
        radius = d,
        center = c,
    )
    drawCircle(markColor.copy(alpha = 0.55f), d * 0.80f, c, style = Stroke(d * 0.07f))
    val check = Path().apply {
        moveTo(c.x - d * 0.30f, c.y + d * 0.02f)
        lineTo(c.x - d * 0.07f, c.y + d * 0.26f)
        lineTo(c.x + d * 0.32f, c.y - d * 0.22f)
    }
    drawPath(check, markColor, style = Stroke(d * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
internal fun PremiumEmblem(modifier: Modifier = Modifier, size: Dp = 84.dp, glowAlpha: Float = 0.20f) {
    val loop = rememberLoop(14_000)
    val pulse = rememberPulse(2_400)
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        drawOrbitMark(
            Offset(this.size.width / 2f, this.size.height / 2f),
            this.size.minDimension / 2f,
            loop.value,
            pulse.value,
            glowAlpha = glowAlpha,
        )
    }
}

private fun DrawScope.drawBadge(center: Offset, radius: Float, fill: Color, accent: Color, painter: Painter) {
    drawCircle(
        brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.32f), Color.Transparent), center = center, radius = radius * 1.5f),
        radius = radius * 1.5f,
        center = center,
    )
    drawCircle(fill, radius, center)
    drawCircle(accent.copy(alpha = 0.60f), radius, center, style = Stroke(1.2.dp.toPx()))
    val s = radius * 1.12f
    translate(center.x - s / 2f, center.y - s / 2f) {
        with(painter) { draw(Size(s, s), colorFilter = ColorFilter.tint(accent)) }
    }
}

private fun DrawScope.drawSparkle(center: Offset, size: Float, tint: Color, painter: Painter) {
    translate(center.x - size / 2f, center.y - size / 2f) {
        with(painter) { draw(Size(size, size), colorFilter = ColorFilter.tint(tint)) }
    }
}

private fun DrawScope.drawCalendarCard(c: Offset, s: Float, surface: Color, ink: Color, pulse: Float) {
    val w = 92f * s
    val h = 72f * s
    val left = c.x - w / 2f
    val top = c.y - h / 2f
    val corner = CornerRadius(10f * s)
    drawRoundRect(Color.Black.copy(alpha = 0.10f), Offset(left, top + 4f * s), Size(w, h), corner)
    drawRoundRect(surface, Offset(left, top), Size(w, h), corner)
    drawRoundRect(ink.copy(alpha = 0.16f), Offset(left, top), Size(w, h), corner, style = Stroke(1.dp.toPx()))
    val band = Path().apply {
        addRoundRect(RoundRect(left, top, left + w, top + 20f * s, CornerRadius(10f * s), CornerRadius(10f * s), CornerRadius.Zero, CornerRadius.Zero))
    }
    drawPath(band, Brush.horizontalGradient(listOf(OrbitViolet, OrbitPurple), startX = left, endX = left + w))
    listOf(-22f, 22f).forEach { dx ->
        drawRoundRect(Color.White, Offset(c.x + dx * s - 2.5f * s, top - 4f * s), Size(5f * s, 11f * s), CornerRadius(2.5f * s))
    }
    val cw = 15f * s
    val ch = 10f * s
    val gx = (w - 8f * s * 2 - cw * 4) / 3f
    val gy = 4f * s
    val gridTop = top + 26f * s
    val filled = setOf(1, 4, 6, 10)
    for (row in 0 until 3) for (col in 0 until 4) {
        val i = row * 4 + col
        val x = left + 8f * s + col * (cw + gx)
        val y = gridTop + row * (ch + gy)
        val color = when {
            i == 5 -> OrbitAmber
            i in filled -> OrbitViolet.copy(alpha = 0.85f)
            else -> ink.copy(alpha = 0.09f)
        }
        drawRoundRect(color, Offset(x, y), Size(cw, ch), CornerRadius(3f * s))
        if (i == 5) {
            val k = ch * (0.5f + 0.04f * pulse)
            val cx = x + cw / 2f
            val cy = y + ch / 2f
            drawPath(
                Path().apply {
                    moveTo(cx - k * 0.34f, cy)
                    lineTo(cx - k * 0.08f, cy + k * 0.26f)
                    lineTo(cx + k * 0.36f, cy - k * 0.24f)
                },
                Color.White,
                style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
internal fun PremiumHero(modifier: Modifier = Modifier) {
    val loop = rememberLoop(16_000)
    val pulse = rememberPulse(2_400)
    val bob = rememberPulse(3_000)
    val surface = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val block = rememberVectorPainter(Icons.Rounded.Block)
    val shield = rememberVectorPainter(Icons.Rounded.Shield)
    val sparkle = rememberVectorPainter(Icons.Rounded.AutoAwesome)
    val width = (LocalConfiguration.current.screenWidthDp.dp * 0.62f).coerceIn(200.dp, 260.dp)
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width, 132.dp).clearAndSetSemantics { }) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.height / 2f
            val s = r / 66f
            drawCircle(
                Brush.radialGradient(listOf(OrbitViolet.copy(alpha = 0.22f + 0.10f * pulse.value), Color.Transparent), center = c, radius = r),
                r,
                c,
            )
            val ring = r * 0.97f
            drawCircle(OrbitViolet.copy(alpha = 0.40f), ring, c, style = Stroke(1.5.dp.toPx()))
            drawCalendarCard(c, s, surface, ink, pulse.value)
            val angle = loop.value * 360f - 40f
            glowDot(orbitPointAt(c, ring, angle), r * 0.075f, OrbitCyan)
            drawCircle(OrbitViolet.copy(alpha = 0.7f), r * 0.03f, orbitPointAt(c, ring, angle + 180f))
            val dy = (bob.value - 0.5f) * 4.dp.toPx()
            val badge = r * 0.21f
            drawBadge(Offset(c.x - r * 1.25f, c.y - r * 0.42f + dy), badge, surface, OrbitCoral, block)
            drawBadge(Offset(c.x + r * 1.25f, c.y - r * 0.42f - dy), badge, surface, OrbitCyan, shield)
            drawSparkle(Offset(c.x - r * 1.25f, c.y + r * 0.50f - dy), r * 0.22f, OrbitAmber, sparkle)
            drawSparkle(Offset(c.x + r * 1.25f, c.y + r * 0.50f + dy), r * 0.22f, OrbitVioletSoft, sparkle)
        }
    }
}
