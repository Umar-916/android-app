package com.funsol.orbit.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitIndigo
import com.funsol.orbit.core.designsystem.orbitGlow

private data class TabSpec(val destination: AppDestination, val label: Int, val icon: ImageVector, val accent: FeatureAccent)

private val LEFT_TABS = listOf(
    TabSpec(AppDestination.Home, R.string.nav_home, Icons.Rounded.Home, FeatureAccent.HOME),
    TabSpec(AppDestination.Tasks, R.string.nav_tasks, Icons.Rounded.Checklist, FeatureAccent.TASKS),
)
private val RIGHT_TABS = listOf(
    TabSpec(AppDestination.Calendar, R.string.nav_calendar, Icons.Rounded.CalendarMonth, FeatureAccent.CALENDAR),
    TabSpec(AppDestination.More, R.string.nav_more, Icons.Rounded.MoreHoriz, FeatureAccent.ANALYTICS),
)

private val BAR_HEIGHT = 60.dp
private val BUTTON_SIZE = 50.dp
private val BUTTON_OVER_BAR = 21.dp
private val NOTCH_RADIUS = 37.dp
private val NOTCH_FILLET = 30.dp

private class NotchedBarShape(
    private val notchRadius: Dp,
    private val fillet: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(buildPath(size, density))

    fun buildPath(size: Size, density: Density): Path {
        val nr = with(density) { notchRadius.toPx() }
        val f = with(density) { fillet.toPx() }
        val pill = size.height / 2f
        val cx = size.width / 2f
        val dx = kotlin.math.sqrt((nr + f) * (nr + f) - f * f)
        val shoulderEnd = Math.toDegrees(kotlin.math.atan2(f.toDouble(), dx.toDouble())).toFloat()
        val bowlStart = 180f - shoulderEnd
        val xl = cx - dx
        val xr = cx + dx
        return Path().apply {
            moveTo(pill, 0f)
            lineTo(xl, 0f)
            arcTo(Rect(xl - f, 0f, xl + f, 2 * f), -90f, 90f - shoulderEnd, false)
            arcTo(Rect(cx - nr, -nr, cx + nr, nr), bowlStart, -(2f * bowlStart - 180f), false)
            arcTo(Rect(xr - f, 0f, xr + f, 2 * f), 180f + shoulderEnd, 90f - shoulderEnd, false)
            lineTo(size.width - pill, 0f)
            arcTo(Rect(size.width - 2 * pill, 0f, size.width, size.height), -90f, 180f, false)
            lineTo(pill, size.height)
            arcTo(Rect(0f, 0f, 2 * pill, size.height), 90f, 180f, false)
            close()
        }
    }
}

private fun DrawScope.softPathShadow(path: Path, color: Color, spreadPx: Float, dyPx: Float, steps: Int = 9) {
    translate(0f, dyPx) {
        for (i in steps downTo 1) {
            val t = i / steps.toFloat()
            drawPath(
                path,
                color.copy(alpha = color.alpha * 0.16f * (1f - t * 0.55f)),
                style = Stroke(width = spreadPx * 2f * t),
            )
        }
        drawPath(path, color.copy(alpha = color.alpha * 0.4f))
    }
}

@Composable
fun OrbitBottomBar(
    current: Any?,
    onSelect: (AppDestination) -> Unit,
    onQuickAdd: () -> Unit,
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {},
    hintLift: Dp = 0.dp,
    bannerGap: Dp = 4.dp,
    bottomMargin: Dp = 8.dp,
) {
    val extras = Orbit.extras
    val shape = remember { NotchedBarShape(NOTCH_RADIUS, NOTCH_FILLET) }
    val config = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val labelSize = remember(config.screenWidthDp, config.locales.toLanguageTags(), fontScale) { mutableFloatStateOf(12f) }
    val shadowColor = if (extras.isDark) Color.Black.copy(alpha = 0.55f) else Color(0xFF3A4A8C).copy(alpha = 0.30f)
    Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = bannerGap, bottom = bottomMargin)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = BUTTON_OVER_BAR)
                .height(BAR_HEIGHT)
                .drawBehind {
                    val path = shape.buildPath(size, this)
                    softPathShadow(path, shadowColor, spreadPx = 10.dp.toPx(), dyPx = 3.dp.toPx())
                    drawPath(path, extras.cardBrush)
                    drawPath(path, extras.cardStroke, style = Stroke(extras.cardStrokeWidth.toPx()))
                }
                .padding(start = 8.dp, end = 8.dp, top = 6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LEFT_TABS.forEach { tab ->
                TabItem(tab, current == tab.destination, { onSelect(tab.destination) }, labelSize, Modifier.weight(1f))
            }
            Spacer(Modifier.weight(1f))
            RIGHT_TABS.forEach { tab ->
                TabItem(tab, current == tab.destination, { onSelect(tab.destination) }, labelSize, Modifier.weight(1f))
            }
        }
        CenterButton(onQuickAdd, Modifier.align(Alignment.TopCenter))
        if (showHint) QuickAddHint(onDismissHint, hintLift)
    }
}

@Composable
private fun CenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val extras = Orbit.extras
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "centerPress")
    val description = stringResource(R.string.dv3_quick_add)
    Box(
        modifier
            .size(BUTTON_SIZE)
            .scale(scale)
            .drawBehind {
                val c = Offset(size.width / 2f, size.height / 2f)
                val dark = Offset(c.x, c.y + 9.dp.toPx())
                drawCircle(
                    Brush.radialGradient(
                        0.0f to Color.Black.copy(alpha = if (extras.isDark) 0.30f else 0.12f),
                        0.55f to Color.Black.copy(alpha = if (extras.isDark) 0.12f else 0.05f),
                        1.0f to Color.Transparent,
                        center = dark,
                        radius = size.width * 0.62f,
                    ),
                    radius = size.width * 0.62f,
                    center = dark,
                )
                val glowCenter = Offset(c.x, c.y + 4.dp.toPx())
                val glowRadius = size.width * 0.78f
                drawCircle(
                    Brush.radialGradient(
                        0.0f to extras.actionGlow.copy(alpha = 0.26f),
                        0.5f to extras.actionGlow.copy(alpha = 0.14f),
                        0.78f to extras.actionGlow.copy(alpha = 0.04f),
                        1.0f to Color.Transparent,
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = glowCenter,
                )
            }
            .clip(CircleShape)
            .background(extras.brandGradient)
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun TabItem(
    tab: TabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    labelSize: MutableFloatState,
    modifier: Modifier,
) {
    val extras = Orbit.extras
    val label = stringResource(tab.label)
    val primary = MaterialTheme.colorScheme.primary
    val iconTint by animateColorAsState(if (selected) primary else extras.textMuted, tween(160), label = "tint")
    val pill by animateColorAsState(if (selected) primary.copy(alpha = 0.12f) else Color.Transparent, tween(160), label = "pill")
    Column(
        modifier
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected; contentDescription = label; role = Role.Tab },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier.width(50.dp).height(32.dp).clip(RoundedCornerShape(50)).background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(21.dp))
        }
        Text(
            label,
            fontSize = labelSize.floatValue.sp,
            lineHeight = (labelSize.floatValue + 3f).sp,
            color = iconTint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            onTextLayout = { if (it.hasVisualOverflow && labelSize.floatValue > 8f) labelSize.floatValue = (labelSize.floatValue - 0.5f).coerceAtLeast(8f) },
        )
    }
}

private class AboveAnchor(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (anchorBounds.top - popupContentSize.height - gapPx).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

@Composable
private fun QuickAddHint(onDismiss: () -> Unit, lift: androidx.compose.ui.unit.Dp) {
    androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(5_000); onDismiss() }
    val density = LocalDensity.current
    val provider = remember(density, lift) { AboveAnchor(with(density) { (2.dp + lift).roundToPx() }) }
    val dismissLabel = stringResource(R.string.dv3_quick_add_hint_dismiss)
    val bubble = RoundedCornerShape(16.dp)
    Popup(popupPositionProvider = provider, properties = PopupProperties(focusable = false)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier
                    .widthIn(max = 280.dp)
                    .heightIn(min = 48.dp)
                    .orbitGlow(bubble, Orbit.extras.actionGlow, radius = 14.dp, offsetY = 4.dp)
                    .clip(bubble)
                    .background(OrbitIndigo)
                    .border(0.8.dp, Orbit.extras.amber.copy(alpha = 0.6f), bubble)
                    .clickable(onClickLabel = dismissLabel, role = Role.Button, onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Orbit.extras.amber, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.dv3_quick_add_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
            Canvas(Modifier.size(width = 16.dp, height = 8.dp)) {
                val p = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(p, OrbitIndigo)
            }
        }
    }
}
