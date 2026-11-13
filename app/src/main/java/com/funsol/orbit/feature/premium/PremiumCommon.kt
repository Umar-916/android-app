package com.funsol.orbit.feature.premium

import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.analytics.PremiumEvents
import com.funsol.orbit.core.analytics.postAnalytic
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import kotlinx.coroutines.delay

internal val PremiumContentMaxWidth = 560.dp

internal class PremiumActions(
    val state: PremiumUiState,
    val close: CloseControl,
    val onClose: () -> Unit,
    val onPurchase: () -> Unit,
    val onRestore: () -> Unit,
    val onToggleTrial: () -> Unit,
)

internal class PremiumFeature(
    val icon: ImageVector,
    val accent: FeatureAccent,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

internal val premiumFeatures = listOf(
    PremiumFeature(Icons.Rounded.Block, FeatureAccent.CALENDAR, R.string.prem_feat_ads_title, R.string.prem_feat_ads_body),
    PremiumFeature(Icons.Rounded.CenterFocusStrong, FeatureAccent.PROJECTS, R.string.prem_feat_focus_title, R.string.prem_feat_focus_body),
    PremiumFeature(Icons.Rounded.Favorite, FeatureAccent.HABITS, R.string.prem_feat_support_title, R.string.prem_feat_support_body),
)

@Composable
internal fun Modifier.revealed(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

@Composable
internal fun BoxScope.CloseOverlay(close: CloseControl, onClose: () -> Unit, plain: Boolean = false, tint: Color = Color.White) {
    val analyticsContext = LocalContext.current
    AnimatedVisibility(
        visible = close.visible,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(150)),
        modifier = Modifier
            .align(if (close.side == CloseSide.START) Alignment.TopStart else Alignment.TopEnd)
            .statusBarsPadding()
            .padding(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(Spacing.touchTarget)
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                    analyticsContext.postAnalytic(PremiumEvents.PREMIUM_CLOSE_CLICK)
                    onClose()
                },
            contentAlignment = Alignment.Center,
        ) {
            val disc = if (plain) Modifier.size(24.dp) else Modifier.size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.20f))
            Box(disc, contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = tint,
                    modifier = Modifier.size(if (plain) 22.dp else 14.dp),
                )
            }
        }
    }
}

@Composable
internal fun NudgeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    glow: Color? = null,
    height: Dp = 54.dp,
    radius: Dp = 30.dp,
    glowRadius: Dp = 12.dp,
    arrow: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    contentColor: Color = Color.White,
) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(radius)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val nudge = rememberInfiniteTransition(label = "nudge").animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "nudgeValue",
    )
    Box(
        modifier
            .orbitGlow(shape, glow ?: extras.actionGlow, radius = glowRadius, offsetY = 4.dp)
            .clip(shape)
            .background(brush ?: extras.brandGradient)
            .heightIn(min = height)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(text, style = textStyle, color = contentColor, textAlign = TextAlign.Center)
            if (arrow) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp).graphicsLayer { translationX = nudge.value.dp.toPx() * (if (rtl) -1f else 1f) },
                )
            }
        }
    }
}

@Composable
internal fun MaybeLaterButton(
    onClick: () -> Unit,
    dark: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    radius: Dp = 30.dp,
) {
    val shape = RoundedCornerShape(radius)
    val extras = Orbit.extras
    val analyticsContext = LocalContext.current
    Box(
        modifier
            .clip(shape)
            .background(if (dark) Color.White.copy(alpha = 0.10f) else extras.well)
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.24f) else extras.cardStroke, shape)
            .heightIn(min = height)
            .clickable(role = Role.Button) {
                analyticsContext.postAnalytic(PremiumEvents.PREMIUM_MAYBE_LATER_CLICK)
                onClick()
            }
            .padding(horizontal = Spacing.lg, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.prem_maybe_later),
            style = MaterialTheme.typography.titleMedium,
            color = if (dark) Color.White else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun UnavailableNote(dark: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val textColor = if (dark) Color.White else MaterialTheme.colorScheme.onSurface
    val muted = if (dark) Color.White.copy(alpha = 0.75f) else Orbit.extras.textMuted
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (dark) Color.White.copy(alpha = 0.08f) else Orbit.extras.well)
            .border(1.dp, if (dark) Color.White.copy(alpha = 0.20f) else Orbit.extras.cardStroke, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = if (dark) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.prem_unavailable_title), style = MaterialTheme.typography.titleSmall, color = textColor)
            Text(stringResource(R.string.prem_unavailable_body), style = MaterialTheme.typography.bodySmall, color = muted)
        }
    }
}

internal class BottomBarStyle(
    val dark: Boolean,
    val brush: Brush? = null,
    val glow: Color? = null,
    val buttonHeight: Dp = 54.dp,
    val buttonRadius: Dp = 30.dp,
    val glowRadius: Dp = 12.dp,
    val arrow: Boolean = true,
    val gap: Dp = 12.dp,
    val showTerms: Boolean = true,
    val ctaStyle: TextStyle? = null,
    val contentColor: Color = Color.White,
)

@Composable
internal fun PremiumBottomBar(
    actions: PremiumActions,
    style: BottomBarStyle,
    modifier: Modifier = Modifier,
    above: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    ctaText: String? = null,
) {
    val state = actions.state
    val muted = if (style.dark) Color.White.copy(alpha = 0.72f) else Orbit.extras.textMuted
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(style.gap),
    ) {
        if (state.canPurchase) {
            above?.invoke()
            val label = ctaText ?: stringResource(R.string.action_continue)
            NudgeButton(
                label,
                actions.onPurchase,
                Modifier.fillMaxWidth(),
                style.brush,
                style.glow,
                style.buttonHeight,
                style.buttonRadius,
                style.glowRadius,
                style.arrow,
                style.ctaStyle ?: MaterialTheme.typography.titleMedium,
            style.contentColor,
            )
            if (state.offlineNotice) {
                Text(
                    stringResource(R.string.prem_offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (style.dark) Color.White else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            below?.invoke()
            TextButton(onClick = actions.onRestore) {
                Text(
                    stringResource(R.string.prem_restore),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (style.dark) Color.White else MaterialTheme.colorScheme.primary,
                )
            }
            if (style.showTerms) {
            Text(
                stringResource(R.string.prem_terms),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            }
        } else {
            MaybeLaterButton(actions.onClose, style.dark, Modifier.fillMaxWidth(), style.buttonHeight, style.buttonRadius)
        }
    }
}

@Composable
internal fun PremiumOutlinedCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val extras = Orbit.extras
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, extras.cardStroke, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        content()
    }
}

@Composable
internal fun PremiumTrialSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val color = if (checked) MaterialTheme.colorScheme.primary else Orbit.extras.textMuted
    val bias by animateFloatAsState(if (checked) 1f else -1f, tween(180), label = "switchBias")
    Box(modifier.width(35.dp).height(26.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(30.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = 0.28f)))
        Box(
            Modifier
                .align(BiasAlignment(bias, 0f))
                .size(18.dp)
                .orbitGlow(CircleShape, Color.Black.copy(alpha = 0.25f), radius = 3.dp, offsetY = 1.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
