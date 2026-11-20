package com.funsol.orbit.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.AnalyticsEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.database.entity.Priority

fun Modifier.orbitGlow(
    shape: Shape,
    color: Color,
    radius: Dp = 12.dp,
    offsetY: Dp = 4.dp,
): Modifier =
    if (android.os.Build.VERSION.SDK_INT >= 31) {
        this.dropShadow(shape, Shadow(radius = radius, color = color, spread = 0.dp, offset = DpOffset(0.dp, offsetY)))
    } else {
        this.shadow(elevation = (radius.value / 2f).coerceAtLeast(2f).dp, shape = shape, ambientColor = color, spotColor = color)
    }

enum class CardStyle {
    Soft,
    Tinted,
    Glass,
}

enum class CardElevation { Flat, Soft, Lifted }

/**
 * Shared press-in feedback so every tappable surface (cards, buttons, FAB) answers a
 * touch the same way. Kept subtle so large cards stay steady while scrolling.
 */
@Composable
private fun Modifier.pressDown(interaction: MutableInteractionSource, pressedScale: Float = 0.97f): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(110),
        label = "pressDown",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    accent: Color? = null,
    tint: Color? = null,
    style: CardStyle = CardStyle.Soft,
    elevation: CardElevation = CardElevation.Soft,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(Radius.card)
    val glass = style == CardStyle.Glass
    val tintColor = tint ?: if (style == CardStyle.Tinted) MaterialTheme.colorScheme.primary else null
    val fill: Brush = when {
        glass -> Brush.linearGradient(listOf(extras.glassFill, extras.glassFill))
        tintColor != null -> extras.tintedBrush(tintColor)
        else -> extras.cardBrush
    }
    val strokeColor = if (glass) extras.glassStroke else if (tintColor != null) tintColor.copy(alpha = 0.20f) else extras.cardStroke
    val glowAlpha = when (elevation) {
        CardElevation.Flat -> 0f
        CardElevation.Soft -> 1f
        CardElevation.Lifted -> 1.6f
    }
    val interaction = remember { MutableInteractionSource() }
    val click = onClick
    val base = modifier
        .then(if (click != null) Modifier.pressDown(interaction, 0.985f) else Modifier)
        .let {
            if (glass || glowAlpha == 0f) it
            else it.orbitGlow(
                shape,
                extras.cardGlow.copy(alpha = (extras.cardGlow.alpha * glowAlpha).coerceAtMost(0.5f)),
                radius = if (elevation == CardElevation.Lifted) 20.dp else 16.dp,
                offsetY = if (elevation == CardElevation.Lifted) 8.dp else 5.dp,
            )
        }
        .clip(shape)
        .background(fill)
        .border(extras.cardStrokeWidth, strokeColor, shape)
        .let {
            if (click != null) it.clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = click,
            ) else it
        }
        .let { m -> if (accent != null) m.drawBehind { drawRect(accent, size = Size(4.dp.toPx(), size.height)) } else m }
    Column(base.padding(contentPadding), content = content)
}

@Composable
fun isLargeFontScale(): Boolean = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f

@Composable
fun OrbitWell(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val extras = Orbit.extras
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val interaction = remember { MutableInteractionSource() }
    val click = onClick
    Box(
        modifier
            .then(if (click != null) Modifier.pressDown(interaction, 0.985f) else Modifier)
            .clip(shape)
            .background(extras.well)
            .border(if (selected) 1.dp else 0.6.dp, borderColor, shape)
            .let {
                if (click != null) it.clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = click,
                ) else it
            },
        content = content,
    )
}

enum class IconTileStyle {
    Solid,
    Tinted,
    Soft,
}

@Composable
fun IconTile(
    icon: ImageVector,
    accent: FeatureAccent,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    contentDescription: String? = null,
    style: IconTileStyle = IconTileStyle.Solid,
) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(Radius.tile)
    val accentColor = extras.accent(accent)
    val base = modifier.size(size)
    when (style) {
        IconTileStyle.Solid -> Box(
            base
                .orbitGlow(shape, accentColor.copy(alpha = 0.28f), radius = 6.dp, offsetY = 2.dp)
                .clip(shape)
                .background(extras.accentGradient(accent)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(size * 0.52f))
        }
        IconTileStyle.Tinted, IconTileStyle.Soft -> {
            val strong = style == IconTileStyle.Tinted
            Box(
                base
                    .clip(shape)
                    .background(if (strong) extras.softGradient(accentColor, 0.32f, 0.14f) else extras.softGradient(accentColor, 0.20f, 0.07f))
                    .border(0.8.dp, accentColor.copy(alpha = if (strong) 0.30f else 0.20f), shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = contentDescription, tint = accentColor, modifier = Modifier.size(size * 0.54f))
            }
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    brush: Brush? = null,
) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(Radius.button)
    val fill = if (enabled) (brush ?: extras.brandGradient) else Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant),
    )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .then(if (enabled) Modifier.pressDown(interaction) else Modifier)
            .heightIn(min = 46.dp)
            .then(if (enabled) Modifier.orbitGlow(shape, extras.actionGlow, radius = 12.dp, offsetY = 4.dp) else Modifier)
            .clip(shape)
            .background(fill)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.md, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (icon != null) Icon(icon, contentDescription = null, tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
fun WellButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OrbitWell(
        modifier.heightIn(min = 46.dp),
        onClick = if (enabled) onClick else null,
        shape = RoundedCornerShape(Radius.button),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = Spacing.md, vertical = 11.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun GradientFab(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    brush: Brush? = null,
) {
    val extras = Orbit.extras
    val shape = if (label == null) CircleShape else RoundedCornerShape(30.dp)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .pressDown(interaction, 0.96f)
            .heightIn(min = 52.dp)
            .orbitGlow(shape, extras.actionGlow, radius = 12.dp, offsetY = 4.dp)
            .clip(shape)
            .background(brush ?: extras.brandGradient)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (label == null) 16.dp else 20.dp, vertical = 12.dp)
            .let { if (label == null) it.width(52.dp) else it },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = if (label == null) contentDescription else null, tint = Color.White)
        if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    accent: FeatureAccent? = null,
    icon: ImageVector? = null,
    scrolled: Boolean = false,
    backEvent: String? = null,
    actions: @Composable () -> Unit = {},
) {
    val backContext = androidx.compose.ui.platform.LocalContext.current
    val extras = Orbit.extras
    val wash = (accent?.let { extras.accent(it) } ?: MaterialTheme.colorScheme.primary).copy(alpha = if (extras.isDark) 0.16f else 0.06f)
    val dividerAlpha by animateFloatAsState(if (scrolled) 1f else 0f, tween(150), label = "topBarDivider")
    val dividerColor = extras.cardStroke
    TopAppBar(
        modifier = Modifier
            .background(Brush.verticalGradient(listOf(wash, Color.Transparent)))
            .drawBehind {
                if (dividerAlpha > 0f) {
                    drawRect(
                        dividerColor.copy(alpha = (dividerColor.alpha * 1.6f).coerceAtMost(1f) * dividerAlpha),
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
            },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (icon != null && accent != null) {
                    IconTile(icon, accent, size = 32.dp, style = if (onBack != null) IconTileStyle.Soft else IconTileStyle.Tinted)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                )
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = {
                    // Per-screen back event (passed by the screen), same style as the other Funsol apps.
                    backEvent?.let { backContext.postAnalytic(it) }
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Orbit.extras.sectionTitle,
        modifier = modifier.padding(start = 4.dp, top = Spacing.xs, bottom = Spacing.xs).semantics { heading() },
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    accent: FeatureAccent = FeatureAccent.HOME,
) {
    val extras = Orbit.extras
    val accentColor = extras.accent(accent)
    val partner = accent.to
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(Modifier.size(96.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension / 2f
                drawCircle(accentColor.copy(alpha = 0.06f), radius = r * 0.98f, center = c)
                drawCircle(accentColor.copy(alpha = 0.10f), radius = r * 0.74f, center = c)
                drawCircle(accentColor.copy(alpha = 0.15f), radius = r * 0.54f, center = c)
                drawCircle(
                    color = accentColor.copy(alpha = 0.32f),
                    radius = r * 0.90f,
                    center = c,
                    style = Stroke(1.2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 7.dp.toPx()))),
                )
                val sat = r * 0.90f * 0.7071f
                drawCircle(partner.copy(alpha = 0.9f), radius = 3.5.dp.toPx(), center = Offset(c.x + sat, c.y - sat))
                drawCircle(extras.amber, radius = 2.5.dp.toPx(), center = Offset(c.x - sat, c.y + sat))
            }
            IconTile(icon, accent, size = 44.dp)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.sm))
            GradientButton(actionLabel, onAction)
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(Spacing.sm))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(Spacing.md))
            GradientButton(stringResource(R.string.action_retry), onRetry)
        }
    }
}

@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(modifier.width(42.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
fun OrbitSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissable) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = dismissable, dismissOnClickOutside = dismissable),
    ) {
        // Sheets float over the live screen: keep the content behind fully visible instead of
        // darkening it with the dialog window's default dim layer.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.setDimAmount(0f) }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (dismissable) onDismiss() }
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val shape = RoundedCornerShape(Radius.sheet)
            Column(
                modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .orbitGlow(shape, Orbit.extras.actionGlow.copy(alpha = 0.18f), radius = 20.dp, offsetY = 0.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(Orbit.extras.cardStrokeWidth, Orbit.extras.cardStroke, shape)
                    .padding(horizontal = Spacing.md, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SheetHandle()
                Spacer(Modifier.height(Spacing.md))
                content()
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = stringResource(R.string.action_cancel),
) {
    OrbitSheet(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = Orbit.extras.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WellButton(dismissLabel, onDismiss, Modifier.weight(1f))
            GradientButton(
                confirmLabel,
                onConfirm,
                Modifier.weight(1f),
                brush = if (destructive) Brush.linearGradient(listOf(Color(0xFFE5484D), Color(0xFFFF6B6B))) else null,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
fun OrbitProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 6.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(450), label = "progress")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = size.height / 2
        drawRoundRect(track, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
        if (animated > 0f) {
            val w = (size.width * animated).coerceAtLeast(size.height)
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(color, OrbitPurple)),
                size = Size(w, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            )
        }
    }
}

@Composable
fun ProgressRing(
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = Color.Unspecified,
    sweep: List<Color>? = null,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(sweep ?: listOf(color, OrbitPurple, color)),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = textColor)
    }
}

@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Dp = 12.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color).clearAndSetSemantics { })
}

@Composable
fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.LOW -> InfoBlue
    Priority.MEDIUM -> SuccessGreen
    Priority.HIGH -> WarningAmber
    Priority.URGENT -> DangerRed
}

@Composable
fun priorityLabel(priority: Priority): String = stringResource(
    when (priority) {
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
        Priority.URGENT -> R.string.priority_urgent
    },
)

@Composable
fun TagPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(role = Role.Button, onClick = onClick)

@Composable
fun HorizontalGap(width: Dp = Spacing.sm) = Spacer(Modifier.width(width))

@Composable
fun VerticalGap(height: Dp = Spacing.sm) = Spacer(Modifier.height(height))

/**
 * Label for outlined text fields whose container color differs from the surface behind the field.
 *
 * Material's outlined cutout removes both the border and the container fill around the floating
 * label, so a tinted container (such as [OrbitExtras.well]) leaves a pale notch around the label.
 * Drawing the container color behind the part of the label that sits inside the field fills that
 * notch back in without moving the label or touching the rest of the field.
 *
 * Use [Color.Transparent] for fields that intentionally have no container fill.
 */
@Composable
fun OrbitFieldLabel(text: String, @Suppress("UNUSED_PARAMETER") containerColor: Color = Orbit.extras.well) {
    Text(
        text = text,
    )
}

@Composable
fun orbitSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Orbit.extras.sectionTitle.copy(alpha = 0.55f),
    uncheckedBorderColor = Color.Transparent,
)

@Composable
fun orbitChipColors(): androidx.compose.material3.SelectableChipColors {
    // A filled, solid selected state keeps the label readable in both themes. The previous
    // light-mode tint was almost the same value as the card behind it, so the white label
    // disappeared.
    val primary = MaterialTheme.colorScheme.primary
    val extras = Orbit.extras
    val selectedFill = if (extras.isDark) primary else OrbitViolet
    return FilterChipDefaults.filterChipColors(
        containerColor = extras.well,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedContainerColor = selectedFill,
        selectedLabelColor = Color.White,
        selectedLeadingIconColor = Color.White,
    )
}

@Composable
fun orbitButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = Color.White,
)
