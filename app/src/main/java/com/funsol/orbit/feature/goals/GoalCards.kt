package com.funsol.orbit.feature.goals

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.GoalMath
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.database.entity.GoalStatus
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitProgressBar
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.SuccessGreen
import com.funsol.orbit.core.designsystem.WarningAmber
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.feature.projects.InfoChip

@Composable
internal fun goalTargetText(goal: GoalEntity): String {
    val target = GoalNumbers.format(goal.targetValue)
    return if (goal.unit.isBlank()) stringResource(R.string.goals_of_target, target)
    else stringResource(R.string.goals_of_target_unit, target, goal.unit)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalNumbersRow(current: Double, goal: GoalEntity, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Text(
            GoalNumbers.format(current),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            goalTargetText(goal),
            style = MaterialTheme.typography.bodyMedium,
            color = Orbit.extras.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

@Composable
internal fun goalCompletedBrush(): Brush = Brush.linearGradient(listOf(OrbitAmber, SuccessGreen))

@Composable
fun GoalCard(
    goal: GoalEntity,
    todayEpochDay: Int,
    onClick: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (goal.status == GoalStatus.COMPLETED) {
        CompletedGoalCard(goal, onClick, modifier)
    } else {
        OpenGoalCard(goal, todayEpochDay, onClick, onUpdate, modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenGoalCard(
    goal: GoalEntity,
    todayEpochDay: Int,
    onClick: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier,
) {
    val extras = Orbit.extras
    val accent = extras.accent(FeatureAccent.GOALS)
    val archived = goal.status == GoalStatus.ARCHIVED
    val fraction = GoalMath.fraction(goal.currentValue, goal.targetValue)
    val percent = GoalMath.percent(goal.currentValue, goal.targetValue)
    val deadline = GoalsLogic.deadline(goal, todayEpochDay)
    OrbitCard(
        modifier = modifier.fillMaxWidth().animateContentSize().alpha(if (archived) 0.78f else 1f),
        onClick = onClick,
        tint = if (archived) null else accent,
        elevation = if (archived) CardElevation.Flat else CardElevation.Soft,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
            IconTile(Icons.Rounded.Flag, FeatureAccent.GOALS)
            Text(
                goal.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        GoalNumbersRow(goal.currentValue, goal, Modifier.padding(top = Spacing.sm + Spacing.xs))
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
        ) {
            OrbitProgressBar(fraction, color = accent, modifier = Modifier.weight(1f), height = 10.dp)
            Text(
                stringResource(R.string.goals_percent, percent),
                style = MaterialTheme.typography.titleSmall,
                color = Orbit.extras.readable(accent),
                maxLines = 1,
                softWrap = false,
            )
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(top = Spacing.sm + Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            when (deadline.kind) {
                DeadlineKind.NONE -> Unit
                DeadlineKind.DAYS_LEFT -> InfoChip(
                    pluralStringResource(R.plurals.goals_days_left, deadline.days, deadline.days),
                    extras.textMuted,
                    icon = Icons.Rounded.Schedule,
                )
                DeadlineKind.TODAY -> InfoChip(stringResource(R.string.goals_due_today), WarningAmber, icon = Icons.Rounded.Schedule)
                DeadlineKind.OVERDUE -> InfoChip(
                    pluralStringResource(R.plurals.goals_overdue_days, deadline.days, deadline.days),
                    MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Rounded.ErrorOutline,
                )
            }
            if (archived) {
                InfoChip(stringResource(R.string.status_archived), extras.textMuted, icon = Icons.Rounded.Inventory2)
            } else {
                GradientButton(
                    text = stringResource(R.string.goals_update_progress),
                    onClick = onUpdate,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    brush = extras.accentGradient(FeatureAccent.GOALS),
                )
            }
        }
    }
}

@Composable
private fun CompletedGoalCard(goal: GoalEntity, onClick: () -> Unit, modifier: Modifier) {
    val percent = GoalMath.percent(goal.currentValue, goal.targetValue)
    val percentText = stringResource(R.string.goals_percent, percent)
    val stackPercent = isLargeFontScale()
    OrbitCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        tint = SuccessGreen,
        elevation = CardElevation.Lifted,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(
                Modifier
                    .size(52.dp)
                    .orbitGlow(CircleShape, OrbitAmber.copy(alpha = 0.45f), radius = 10.dp, offsetY = 3.dp)
                    .clip(CircleShape)
                    .background(goalCompletedBrush()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.status_completed),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                GoalNumbersRow(goal.currentValue, goal)
                Text(
                    stringResource(R.string.goals_celebrate),
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessGreen,
                    fontWeight = FontWeight.SemiBold,
                )
                if (stackPercent) {
                    Text(percentText, style = MaterialTheme.typography.titleMedium, color = SuccessGreen, maxLines = 1)
                }
            }
            if (!stackPercent) {
                Text(percentText, style = MaterialTheme.typography.titleMedium, color = SuccessGreen, maxLines = 1, softWrap = false)
            }
        }
    }
}
