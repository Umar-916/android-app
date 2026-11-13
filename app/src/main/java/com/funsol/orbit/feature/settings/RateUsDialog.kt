package com.funsol.orbit.feature.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.R
import com.funsol.orbit.core.common.openInPlayStore
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitCoral
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.feature.onboarding.rememberPulse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class RateUsViewModel(private val store: SettingsStore) : ViewModel() {
    fun markRatePromptDone() {
        viewModelScope.launch { store.setRatePromptDone(true) }
    }
}

private const val STAR_COUNT = 5
private const val STORE_THRESHOLD = 4
private val BadgeSize = 92.dp
private val BadgeCore = 68.dp
private val StarGap = 10.dp

internal enum class RateAction { NONE, FEEDBACK, STORE }

internal fun rateActionFor(rating: Int): RateAction = when {
    rating <= 0 -> RateAction.NONE
    rating >= STORE_THRESHOLD -> RateAction.STORE
    else -> RateAction.FEEDBACK
}

@StringRes
private fun reactionFor(rating: Int): Int = when (rating) {
    1 -> R.string.rate_reaction_1
    2 -> R.string.rate_reaction_2
    3 -> R.string.rate_reaction_3
    4 -> R.string.rate_reaction_4
    5 -> R.string.rate_reaction_5
    else -> R.string.rate_hint_none
}

@Composable
fun RateUsDialog(onDismiss: () -> Unit, onOpenFeedback: (() -> Unit)?) {
    val vm: RateUsViewModel = koinViewModel()
    val context = LocalContext.current
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var lowRatingSubmitted by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (submitted) return
        when (rateActionFor(rating)) {
            RateAction.NONE -> Unit
            RateAction.STORE -> {
                submitted = true
                vm.markRatePromptDone()
                tryCatch { context.openInPlayStore() }
                onDismiss()
            }
            RateAction.FEEDBACK -> {
                submitted = true
                lowRatingSubmitted = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(8.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(tween(250)) + slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 3 },
            ) {
                RateSheet(thanks = lowRatingSubmitted, onClose = onDismiss) {
                    if (!lowRatingSubmitted) {
                        RateForm(
                            rating = rating,
                            enabled = !submitted,
                            onRate = { if (!submitted) rating = it },
                            onSubmit = ::submit,
                        )
                    } else {
                        RateThanks(onDismiss = onDismiss, onOpenFeedback = onOpenFeedback)
                    }
                }
            }
        }
    }
}

@Composable
private fun RateSheet(thanks: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(Radius.sheet)
    val extras = Orbit.extras
    Box(
        Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Column(
            Modifier
                .padding(top = BadgeSize / 2)
                .orbitGlow(shape, extras.actionGlow.copy(alpha = 0.18f), radius = 20.dp, offsetY = 0.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(extras.cardStrokeWidth, extras.cardStroke, shape),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.md, end = Spacing.md, top = BadgeSize / 2 + 12.dp, bottom = Spacing.md)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()
            }
        }
        RateBadge(thanks = thanks, modifier = Modifier.align(Alignment.TopCenter))
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = BadgeSize / 2 + 2.dp, end = 2.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close), tint = extras.textMuted)
        }
    }
}

@Composable
private fun RateForm(rating: Int, enabled: Boolean, onRate: (Int) -> Unit, onSubmit: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.rate_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            Text(
                stringResource(R.string.rate_tagline),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.rate_description),
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        StarRow(rating = rating, onRate = onRate)
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = rating, animationSpec = tween(220), label = "reaction") { value ->
                Text(
                    stringResource(reactionFor(value)),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (value > 0) MaterialTheme.colorScheme.primary else Orbit.extras.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
        RateSubmitButton(
            text = stringResource(R.string.rate_submit),
            onClick = onSubmit,
            enabled = rating > 0 && enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RateSubmitButton(text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(40.dp)
    val fade by animateFloatAsState(if (enabled) 1f else 0.45f, tween(200), label = "submitAlpha")
    Box(
        modifier
            .then(if (enabled) Modifier.orbitGlow(shape, extras.actionGlow, radius = 12.dp, offsetY = 4.dp) else Modifier)
            .graphicsLayer { alpha = fade }
            .clip(shape)
            .background(extras.brandGradient)
            .heightIn(min = 46.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun RateThanks(onDismiss: () -> Unit, onOpenFeedback: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                stringResource(R.string.rate_low_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            Text(
                stringResource(if (onOpenFeedback != null) R.string.rate_low_message_feedback else R.string.rate_low_message),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (onOpenFeedback != null) {
            RateSubmitButton(
                text = stringResource(R.string.rate_improve),
                onClick = {
                    onDismiss()
                    onOpenFeedback()
                },
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        WellButton(
            text = stringResource(R.string.action_close),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RateBadge(thanks: Boolean, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val pulse = rememberPulse(2_200)
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    val fill = Brush.linearGradient(listOf(lerp(OrbitAmber, Color.White, 0.25f), OrbitAmber, lerp(OrbitAmber, OrbitCoral, 0.7f)))
    Box(
        modifier
            .size(BadgeSize)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            drawCircle(surface, r, c)
            drawCircle(OrbitAmber.copy(alpha = 0.10f + 0.06f * pulse.value), r * 0.90f, c)
            drawCircle(OrbitAmber.copy(alpha = 0.30f + 0.15f * pulse.value), r * 0.92f, c, style = Stroke(1.5.dp.toPx()))
        }
        Box(
            Modifier
                .size(BadgeCore)
                .orbitGlow(CircleShape, OrbitAmber.copy(alpha = 0.40f), radius = 12.dp, offsetY = 4.dp)
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (thanks) Icons.Rounded.Favorite else Icons.Rounded.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun StarRow(rating: Int, onRate: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cell: Dp = ((maxWidth - StarGap * (STAR_COUNT - 1)) / STAR_COUNT).coerceIn(36.dp, 40.dp)
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(StarGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (value in 1..STAR_COUNT) {
                RateStar(
                    value = value,
                    filled = value <= rating,
                    selected = value == rating,
                    cell = cell,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRate(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun RateStar(value: Int, filled: Boolean, selected: Boolean, cell: Dp, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(filled) {
        if (filled) {
            delay((value - 1) * 25L)
            scale.snapTo(0.7f)
            scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        } else {
            scale.animateTo(1f)
        }
    }
    val description = pluralStringResource(R.plurals.rate_star_count, value, value)
    val goldTop = lerp(OrbitAmber, Color.White, 0.45f)
    val goldBottom = lerp(OrbitAmber, OrbitCoral, 0.35f)
    Box(
        Modifier
            .size(cell)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Star,
            contentDescription = null,
            tint = if (filled) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
            modifier = Modifier
                .size(cell - 4.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    if (filled) drawRect(Brush.verticalGradient(listOf(goldTop, OrbitAmber, goldBottom)), blendMode = BlendMode.SrcIn)
                },
        )
    }
}
