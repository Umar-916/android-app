package com.funsol.orbit.feature.habits

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.SuccessGreen
import com.funsol.orbit.core.designsystem.WarningAmber
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.feature.projects.CenteredContent
import com.funsol.orbit.feature.projects.GradientHero
import com.funsol.orbit.feature.projects.InfoChip
import com.funsol.orbit.feature.projects.TintedStatTile
import com.funsol.orbit.feature.projects.pressScale
import com.funsol.orbit.feature.projects.toGradient
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun HabitDetailScreen(habitId: String, nav: OrbitNavigator) {
    val vm: HabitDetailViewModel = koinViewModel(key = "habit_$habitId", parameters = { parametersOf(habitId) })
    val state by vm.state.collectAsStateWithLifecycle()

    val guard = rememberClickGuard()
    val snackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.error_generic)
    val failures by vm.writeFailures.collectAsStateWithLifecycle()
    var handledFailures by rememberSaveable { mutableIntStateOf(failures) }
    LaunchedEffect(failures) {
        if (failures != handledFailures) {
            handledFailures = failures
            snackbar.showSnackbar(failedMessage)
        }
    }

    LaunchedEffect(state.notFound) { if (state.notFound) nav.back() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            OrbitTopBar(title = stringResource(R.string.habits_detail_title), onBack = { guard.run(nav::back) }, accent = FeatureAccent.HABITS) {
                if (state.habit != null) {
                    IconButton(onClick = { guard.run { nav.navigate(AppDestination.HabitEdit(habitId)) } }) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                }
            }
        },
    ) { padding ->
        CenteredContent(Modifier.padding(padding)) {
            val habit = state.habit
            when {
                state.loading -> LoadingState()
                state.error -> ErrorState(stringResource(R.string.error_generic), onRetry = vm::retry)
                habit == null -> LoadingState()
                else -> DetailContent(state, habit.name, habit.description, habit.scheduleMask, habit.archived, Color(habit.color), vm)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    state: HabitDetailUiState,
    name: String,
    description: String,
    scheduleMask: Int,
    archived: Boolean,
    accent: Color,
    vm: HabitDetailViewModel,
) {
    val todayDow = state.todayEpochDay.takeIf { it > 0 }?.toLocalDate()?.dayOfWeek
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        GradientHero(brush = accent.toGradient(), glow = accent, modifier = Modifier.animateContentSize()) {
            if (archived || !state.scheduledToday) {
                InfoChip(
                    text = stringResource(if (archived) R.string.status_archived else R.string.habits_not_scheduled_today),
                    color = Color.White,
                    icon = if (archived) Icons.Rounded.Inventory2 else Icons.Rounded.EventBusy,
                    onGradient = true,
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            FlowRow(
                Modifier.padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                WeekdayDots(
                    mask = scheduleMask,
                    color = Color.White,
                    today = todayDow,
                    offColor = Color.White.copy(alpha = 0.28f),
                    ringColor = Color.White,
                    dot = 11.dp,
                )
                Text(
                    scheduleSummary(scheduleMask),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }
            if (!archived && state.scheduledToday) {
                CheckInButton(state.checkedToday) { vm.setChecked(state.todayEpochDay, !state.checkedToday) }
            }
        }

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            maxItemsInEachRow = if (isLargeFontScale()) 1 else 2,
        ) {
            val stats = state.stats
            TintedStatTile(
                pluralStringResource(R.plurals.habits_days_value, stats.currentStreak, stats.currentStreak),
                stringResource(R.string.habits_stat_current_streak),
                WarningAmber,
                Icons.Rounded.LocalFireDepartment,
                Modifier.weight(1f),
            )
            TintedStatTile(
                pluralStringResource(R.plurals.habits_days_value, stats.longestStreak, stats.longestStreak),
                stringResource(R.string.habits_stat_longest_streak),
                MaterialTheme.colorScheme.tertiary,
                Icons.Rounded.EmojiEvents,
                Modifier.weight(1f),
            )
            TintedStatTile(
                stringResource(R.string.habits_percent_value, stats.completionPercent),
                stringResource(R.string.habits_stat_completion),
                SuccessGreen,
                Icons.Rounded.DonutLarge,
                Modifier.weight(1f),
            )
            TintedStatTile(
                state.totalCheckIns.toString(),
                stringResource(R.string.habits_stat_total_checkins),
                MaterialTheme.colorScheme.primary,
                Icons.Rounded.TaskAlt,
                Modifier.weight(1f),
            )
        }

        HabitCalendarCard(
            state = state,
            accent = accent,
            onPrevious = vm::previousMonth,
            onNext = vm::nextMonth,
            onToggle = { cell -> vm.setChecked(cell.date.toEpochDay().toInt(), cell.status != DayStatus.DONE) },
        )
        Text(
            stringResource(R.string.habits_calendar_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Orbit.extras.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
        )
    }
}

@Composable
private fun CheckInButton(checked: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(Radius.button)
    Row(
        Modifier
            .padding(top = Spacing.md)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .pressScale(source)
            .clip(shape)
            .background(if (checked) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = source, indication = ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        Icon(
            if (checked) Icons.Rounded.Check else Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (checked) Color.White else MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(if (checked) R.string.habits_undo_today else R.string.habits_check_in_today),
            style = MaterialTheme.typography.titleMedium,
            color = if (checked) Color.White else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
