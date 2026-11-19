package com.funsol.orbit.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.funsol.orbit.core.designsystem.orbitGlow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.Progress
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitProgressBar
import com.funsol.orbit.core.designsystem.Spacing
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun HomeHero(
    today: LocalDate,
    progress: Progress?,
    onSearch: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
    showCustomize: Boolean = true,
) {
    val hour = remember(today) { LocalTime.now().hour }
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 18 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
    val extras = Orbit.extras
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Greeting, search and customize stay in their original place above the card.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    DateFormats.fullDate(today),
                    style = MaterialTheme.typography.labelLarge,
                    color = Orbit.extras.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            HeroIcon(
                Icons.Rounded.Search,
                stringResource(R.string.home_search_hint),
                onClick = onSearch,
                tint = MaterialTheme.colorScheme.onPrimary,
                container = MaterialTheme.colorScheme.primary,
            )
            AnimatedVisibility(
                visible = showCustomize,
                enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.85f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.85f),
            ) {
                HeroIcon(
                    Icons.Rounded.Tune,
                    stringResource(R.string.home_customize),
                    onClick = onCustomize,
                    tint = MaterialTheme.colorScheme.primary,
                    container = Orbit.extras.well,
                    border = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                )
            }
        }
        if (progress != null) {
            // Brand hero (same gradient and rings as the More screen) holding the Today panel.
            Box(
                Modifier
                    .fillMaxWidth()
                    .orbitGlow(shape, extras.actionGlow.copy(alpha = 0.22f), radius = 14.dp, offsetY = 5.dp)
                    .clip(shape)
                    .background(extras.heroGradient)
                    .animateContentSize(tween(220)),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    val c = Offset(if (rtl) size.width * 0.06f else size.width * 0.94f, size.height * 0.10f)
                    listOf(44.dp, 74.dp, 104.dp, 138.dp).forEachIndexed { i, r ->
                        drawCircle(
                            Color.White.copy(alpha = 0.16f - i * 0.025f),
                            radius = r.toPx(),
                            center = c,
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().padding(12.dp)) { HeroProgress(progress) }
            }
        }
    }
}

/** "TODAY" pill that keeps gently floating and breathing so it always draws the eye. */
@Composable
private fun TodayPill() {
    val transition = rememberInfiniteTransition(label = "todayPill")
    val float by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "todayFloat",
    )
    val halo by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "todayHalo",
    )
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.graphicsLayer {
            translationY = (float - 0.5f) * -4.dp.toPx()
            val s = 1f + 0.05f * float
            scaleX = s
            scaleY = s
        },
        contentAlignment = Alignment.Center,
    ) {
        // Expanding halo behind the pill.
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val s = 1f + 0.28f * halo
                    scaleX = s
                    scaleY = s
                    alpha = 0.45f * (1f - halo)
                }
                .clip(shape)
                .background(Color.White),
        )
        Box(
            Modifier
                .clip(shape)
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                stringResource(R.string.home_today_title).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HeroIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color,
    container: Color,
    border: Color? = null,
) {
    Box(
        Modifier
            .size(42.dp)
            .pressScale(0.92f)
            .clip(CircleShape)
            .background(container)
            .then(if (border != null) Modifier.border(0.9.dp, border, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HeroProgress(progress: Progress) {
    // Frosted panel inside the hero so "Today" and the day's progress read as the focus.
    val panelShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(0.8.dp, Color.White.copy(alpha = 0.30f), panelShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TodayPill()
            if (progress.total > 0) {
                Text(
                    pluralStringResource(R.plurals.home_tasks_done_of, progress.total, progress.done, progress.total),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (progress.total == 0) {
            Text(
                stringResource(R.string.home_progress_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Text(
                stringResource(R.string.home_progress_empty_message),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        } else {
            val animatedFraction by animateFloatAsState(
                targetValue = progress.fraction,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                label = "todayProgress",
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    stringResource(R.string.home_percent, progress.percent),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.28f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White),
                    )
                }
            }
            if (progress.done >= progress.total) {
                Text(
                    stringResource(R.string.home_progress_all_done),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
