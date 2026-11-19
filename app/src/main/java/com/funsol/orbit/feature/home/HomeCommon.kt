package com.funsol.orbit.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal val HomeBottomSpace = 16.dp

@Composable
internal fun Modifier.staggerIn(index: Int): Modifier {
    var played by rememberSaveable(index) { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay((index.coerceAtMost(4) * 25).toLong())
            progress.animateTo(1f, tween(200))
            played = true
        }
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 10.dp.toPx()
    }
}

/**
 * One short, gentle pulse after the screen settles so the eye is drawn to the
 * card that needs attention. It runs once, never loops.
 */
@Composable
internal fun Modifier.attentionPulse(
    enabled: Boolean,
    delayMillis: Long = 550L,
    rounds: Int = 2,
    maxScale: Float = 1.012f,
    lift: Dp = 0.dp,
): Modifier {
    var played by rememberSaveable(enabled) { mutableStateOf(!enabled) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (!enabled || played) return@LaunchedEffect
        delay(delayMillis.milliseconds)
        repeat(rounds) {
            pulse.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            pulse.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        }
        played = true
    }
    return this.graphicsLayer {
        val progress = pulse.value
        val scale = 1f + (maxScale - 1f) * progress
        scaleX = scale
        scaleY = scale
        translationY = -lift.toPx() * progress
    }
}

/**
 * Adds a light press-in scale without owning the click gesture, so it can sit
 * on top of a card that already handles its own clicks.
 */
@Composable
internal fun Modifier.pressScale(pressedScale: Float = 0.975f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(110),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                try {
                    waitForUpOrCancellation()
                } finally {
                    pressed = false
                }
            }
        }
}

/** One card style for every home section so the screen reads consistently. */
@Composable
internal fun HomeCard(
    modifier: Modifier = Modifier,
    watermark: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Same look as the tiles on the More screen: tinted with the Home colour, flat,
    // with a faint oversized icon in the corner. Every section shares one colour.
    val homeColor = Orbit.extras.accent(FeatureAccent.HOME)
    OrbitCard(
        modifier = modifier.fillMaxWidth().animateContentSize(tween(220)),
        tint = homeColor,
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (watermark != null) {
                Icon(
                    watermark,
                    contentDescription = null,
                    tint = homeColor.copy(alpha = 0.09f),
                    modifier = Modifier.align(Alignment.TopEnd).size(84.dp).offset(x = 20.dp, y = (-16).dp),
                )
            }
            Column(Modifier.fillMaxWidth().padding(Spacing.md), content = content)
        }
    }
}

/**
 * Card-shaped action used by the New task / New habit / New project entry points.
 * Every instance shares the same height, radius and typography so the three
 * buttons always line up with each other.
 */
@Composable
internal fun HomeActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(Radius.button)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(110),
        label = "actionPressScale",
    )
    Row(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.92f else 1f
            }
            .heightIn(min = 46.dp)
            .then(
                if (primary) Modifier.orbitGlow(shape, extras.actionGlow, radius = 12.dp, offsetY = 3.dp)
                else Modifier,
            )
            .clip(shape)
            .background(if (primary) extras.brandGradient else extras.cardBrush)
            .then(
                if (primary) Modifier
                else Modifier.border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f), shape),
            )
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (primary) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun HomeOverline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Orbit.extras.sectionTitle,
    pulse: Boolean = false,
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        letterSpacing = 1.2.sp,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
internal fun HomeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp),
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
