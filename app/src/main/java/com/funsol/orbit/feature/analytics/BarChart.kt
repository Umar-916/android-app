package com.funsol.orbit.feature.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    description: String,
    modifier: Modifier = Modifier,
    barTop: Color = MaterialTheme.colorScheme.primary,
    barBottom: Color = MaterialTheme.colorScheme.secondary,
    height: Dp = 180.dp,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val baseline = MaterialTheme.colorScheme.outlineVariant
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val grow = remember { Animatable(0f) }
    LaunchedEffect(values) {
        grow.snapTo(0f)
        grow.animateTo(1f, tween(650))
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        val n = values.size
        if (n == 0) return@Canvas
        val textH = measurer.measure("0", labelStyle).size.height.toFloat()
        val valueH = measurer.measure("0", valueStyle).size.height.toFloat()
        val gap = 4.dp.toPx()
        val chartTop = valueH + gap
        val chartBottom = size.height - textH - gap * 2
        val usable = max(chartBottom - chartTop, 1f)
        val slot = size.width / n
        val barW = min(slot * 0.55f, 36.dp.toPx())
        val peak = max(values.maxOrNull() ?: 0, 1)
        val topRadius = CornerRadius(min(10.dp.toPx(), barW / 2f))
        val none = CornerRadius.Zero

        listOf(0.5f, 1f).forEach { f ->
            val y = chartBottom - usable * f
            drawLine(baseline.copy(alpha = if (f == 1f) 0.35f else 0.6f), Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8.dp.toPx())
        }
        drawLine(baseline, Offset(0f, chartBottom), Offset(size.width, chartBottom), strokeWidth = 1.dp.toPx())

        values.forEachIndexed { i, v ->
            val slotIndex = if (rtl) n - 1 - i else i
            val cx = slot * slotIndex + slot / 2f
            val barH = if (v > 0) max(usable * (v / peak.toFloat()) * grow.value, 4.dp.toPx()) else 0f
            if (v > 0) {
                val top = chartBottom - barH
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = cx - barW / 2f,
                            top = top,
                            right = cx + barW / 2f,
                            bottom = chartBottom,
                            topLeftCornerRadius = topRadius,
                            topRightCornerRadius = topRadius,
                            bottomRightCornerRadius = none,
                            bottomLeftCornerRadius = none,
                        ),
                    )
                }
                drawPath(path, Brush.verticalGradient(listOf(barTop, barBottom), startY = top, endY = chartBottom))
            } else {
                drawRoundRect(
                    color = baseline,
                    topLeft = Offset(cx - barW / 2f, chartBottom - 3.dp.toPx()),
                    size = Size(barW, 3.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            val valueLayout = measurer.measure(v.toString(), valueStyle)
            drawText(
                valueLayout,
                topLeft = Offset(cx - valueLayout.size.width / 2f, chartBottom - max(barH, 3.dp.toPx()) - valueLayout.size.height - 2.dp.toPx()),
            )
            val labelLayout = measurer.measure(
                labels.getOrElse(i) { "" },
                labelStyle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = max(slot.toInt(), 1)),
            )
            drawText(labelLayout, topLeft = Offset(cx - labelLayout.size.width / 2f, chartBottom + gap))
        }
    }
}
