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
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            OrbitCard(
                modifier = Modifier.fillMaxWidth().animateContentSize(tween(220)),
                tint = MaterialTheme.colorScheme.primary,
                style = CardStyle.Tinted,
                elevation = CardElevation.Flat,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                HeroProgress(progress)
            }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeOverline(
            stringResource(R.string.home_today_title),
            color = MaterialTheme.colorScheme.primary,
            pulse = true,
        )
        if (progress.total == 0) {
            Text(
                stringResource(R.string.home_progress_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            Text(
                stringResource(R.string.home_progress_empty_message),
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        } else {
            Row(
                modifier = Modifier.padding(top = Spacing.xs),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    stringResource(R.string.home_percent, progress.percent),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    pluralStringResource(R.plurals.home_tasks_done_of, progress.total, progress.done, progress.total),
                    style = MaterialTheme.typography.titleSmall,
                    color = Orbit.extras.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            val animatedFraction by animateFloatAsState(
                targetValue = progress.fraction,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                label = "todayProgress",
            )
            OrbitProgressBar(
                animatedFraction,
                modifier = Modifier.padding(top = Spacing.sm),
                height = 6.dp,
            )
            if (progress.done >= progress.total) {
                Text(
                    stringResource(R.string.home_progress_all_done),
                    style = MaterialTheme.typography.bodySmall,
                    color = Orbit.extras.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}
