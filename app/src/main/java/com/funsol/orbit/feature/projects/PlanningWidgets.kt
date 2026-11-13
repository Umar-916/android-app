package com.funsol.orbit.feature.projects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.core.designsystem.GroupTitle
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.94f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

fun Modifier.ringsDecoration(): Modifier = this.drawBehind {
    val rtl = layoutDirection == LayoutDirection.Rtl
    val center = Offset(if (rtl) size.width * 0.08f else size.width * 0.92f, size.height * 0.12f)
    val stroke = Stroke(width = 1.5.dp.toPx())
    drawCircle(Color.White.copy(alpha = 0.14f), radius = 56.dp.toPx(), center = center, style = stroke)
    drawCircle(Color.White.copy(alpha = 0.10f), radius = 96.dp.toPx(), center = center, style = stroke)
    drawCircle(Color.White.copy(alpha = 0.06f), radius = 140.dp.toPx(), center = center, style = stroke)
    drawCircle(
        Color.White.copy(alpha = 0.08f),
        radius = 34.dp.toPx(),
        center = Offset(if (rtl) size.width * 0.9f else size.width * 0.1f, size.height * 1.02f),
    )
}

@Composable
fun GradientHero(
    brush: Brush,
    glow: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .orbitGlow(shape, glow.copy(alpha = 0.35f), radius = 18.dp, offsetY = 8.dp)
            .clip(shape)
            .background(brush)
            .ringsDecoration()
            .padding(Spacing.md + Spacing.xs),
        content = content,
    )
}

@Composable
fun InfoChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onGradient: Boolean = false,
) {
    val content = if (onGradient) Color.White else color
    val container = if (onGradient) Color.White.copy(alpha = 0.20f) else color.copy(alpha = 0.14f)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
    }
}

@Composable
fun TintedStatTile(
    value: String,
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .heightIn(min = 96.dp)
            .orbitGlow(shape, color.copy(alpha = 0.14f), radius = 12.dp, offsetY = 4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.05f))))
            .border(Orbit.extras.cardStrokeWidth, color.copy(alpha = 0.30f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp)) }
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Orbit.extras.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AnimatedCheck(
    checked: Boolean,
    color: Color,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    circleSize: Dp = 28.dp,
    sparkle: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(220), label = "checkFill")
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (checked && !previous) {
            if (sparkle) launch { burst.snapTo(0f); burst.animateTo(1f, tween(600)) }
            pop.snapTo(0.78f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
        previous = checked
    }
    val boxSize = maxOf(Spacing.touchTarget, circleSize + 12.dp)
    Box(
        modifier
            .size(boxSize)
            .pressScale(interaction)
            .drawBehind {
                val t = burst.value
                if (sparkle && t < 1f) {
                    val c = center
                    val base = circleSize.toPx() / 2f
                    drawCircle(color.copy(alpha = (1f - t) * 0.6f), radius = base + t * 12.dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
                    for (i in 0 until 8) {
                        val angle = i * (PI / 4).toFloat()
                        val r = base + 3.dp.toPx() + t * 10.dp.toPx()
                        drawCircle(
                            color.copy(alpha = 1f - t),
                            radius = 2.6.dp.toPx() * (1f - t * 0.6f),
                            center = Offset(c.x + cos(angle) * r, c.y + sin(angle) * r),
                        )
                    }
                }
            }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = boxSize / 2),
                onValueChange = onCheckedChange,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(circleSize)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = if (enabled) 1f else 0.5f }
                .clip(CircleShape)
                .background(Orbit.extras.well)
                .border(2.dp, color.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = fill }.background(color.toGradient()))
            AnimatedVisibility(visible = checked, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(circleSize * 0.6f))
            }
        }
    }
}

@Composable
fun PlanningTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    supportingText: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    suffix: String? = null,
) {
    val shown = errorText ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorText != null,
        supportingText = if (shown != null) {
            { Text(shown) }
        } else null,
        suffix = if (suffix != null) {
            { Text(suffix) }
        } else null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Orbit.extras.well,
            unfocusedContainerColor = Orbit.extras.well,
            errorContainerColor = Orbit.extras.well,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun FormSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        GroupTitle(title)
        OrbitCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), content = content)
        }
    }
}
