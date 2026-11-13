package com.funsol.orbit.feature.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.ColorDot
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.LoadingState
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitProgressBar
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel
import java.time.format.TextStyle as DayStyle
import java.util.Locale

@Composable
fun AnalyticsScreen(nav: OrbitNavigator) {
    val viewModel: AnalyticsViewModel = koinViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.canScrollBackward } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                stringResource(R.string.analytics_title),
                onBack = nav::back,
                accent = FeatureAccent.ANALYTICS,
                scrolled = scrolled,
                icon = Icons.Rounded.Insights,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                state.loading -> LoadingState()
                state.failed -> ErrorState(stringResource(R.string.error_generic), onRetry = viewModel::retry)
                state.data.isEmpty -> EmptyState(
                    icon = Icons.Rounded.Insights,
                    title = stringResource(R.string.analytics_empty_title),
                    message = stringResource(R.string.analytics_empty_message),
                    actionLabel = stringResource(R.string.analytics_empty_action),
                    onAction = { nav.navigate(AppDestination.TaskEdit()) },
                    accent = FeatureAccent.ANALYTICS,
                )
                else -> AnalyticsContent(state, viewModel::setRange, listState)
            }
        }
    }
}

@Composable
private fun AnalyticsContent(state: AnalyticsUiState, onRange: (AnalyticsRange) -> Unit, listState: LazyListState) {
    val data = state.data
    LazyColumn(
        state = listState,
        modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item(key = "range", contentType = "range") { RangeSelector(state.range, onRange) }
        item(key = "tiles", contentType = "tiles") { SummaryTiles(data, state.range) }
        item(key = "daily", contentType = "chart") {
            ChartCard(
                title = stringResource(R.string.analytics_daily_title),
                subtitle = stringResource(R.string.analytics_daily_subtitle),
                icon = Icons.Rounded.Insights,
                accent = FeatureAccent.ANALYTICS,
                values = data.daily.map { it.value },
                labels = data.daily.map { it.start.dayOfWeek.getDisplayName(DayStyle.SHORT, Locale.getDefault()) },
                pointLabels = data.daily.map { DateFormats.weekdayLong(it.start) },
            )
        }
        item(key = "weekly", contentType = "chart") {
            ChartCard(
                title = stringResource(R.string.analytics_weekly_title),
                subtitle = stringResource(R.string.analytics_weekly_subtitle),
                icon = Icons.Rounded.Timeline,
                accent = FeatureAccent.PROJECTS,
                values = data.weekly.map { it.value },
                labels = data.weekly.map { DateFormats.dateShort(it.start.toEpochDayInt()) },
                pointLabels = data.weekly.map {
                    stringResource(R.string.analytics_week_range, DateFormats.dateShort(it.start.toEpochDayInt()), DateFormats.dateShort(it.end.toEpochDayInt()))
                },
            )
        }
        item(key = "projects", contentType = "list") { ProjectsCard(data.projects) }
        item(key = "habits", contentType = "list") { HabitsCard(data.habits) }
        item(key = "goals", contentType = "list") { GoalsCard(data.goals) }
    }
}

@Composable
private fun RangeSelector(selected: AnalyticsRange, onRange: (AnalyticsRange) -> Unit) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(30.dp)
    val options = listOf(
        AnalyticsRange.WEEK to stringResource(R.string.analytics_range_7),
        AnalyticsRange.MONTH to stringResource(R.string.analytics_range_30),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(extras.well)
            .border(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), shape)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (range, label) ->
            val isSelected = selected == range
            val textColor by animateColorAsState(
                if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segmentText",
            )
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .then(if (isSelected) Modifier.background(extras.accentGradient(FeatureAccent.ANALYTICS)) else Modifier)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onRange(range) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = textColor, maxLines = 1)
            }
        }
    }
}

private class StatTile(
    val icon: ImageVector,
    val accent: FeatureAccent,
    val value: String,
    val label: String,
    val danger: Boolean = false,
)

@Composable
private fun SummaryTiles(data: AnalyticsData, range: AnalyticsRange) {
    val rangeLabel = when (range) {
        AnalyticsRange.WEEK -> stringResource(R.string.analytics_tile_completed_7)
        AnalyticsRange.MONTH -> stringResource(R.string.analytics_tile_completed_30)
    }
    val rate = data.completionRatePercent
    val tiles = listOf(
        StatTile(Icons.Rounded.TaskAlt, FeatureAccent.HABITS, data.completedInRange.toString(), rangeLabel),
        StatTile(
            Icons.Rounded.Insights,
            FeatureAccent.ANALYTICS,
            rate?.let { stringResource(R.string.analytics_percent, it) } ?: stringResource(R.string.analytics_none),
            stringResource(R.string.analytics_tile_rate),
        ),
        StatTile(Icons.Rounded.Checklist, FeatureAccent.TASKS, data.openTasks.toString(), stringResource(R.string.analytics_tile_open)),
        StatTile(
            Icons.Rounded.WarningAmber,
            FeatureAccent.CALENDAR,
            data.overdueNow.toString(),
            stringResource(R.string.analytics_tile_overdue),
            danger = data.overdueNow > 0,
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
        if (isLargeFontScale()) {
            tiles.forEach { Tile(it, Modifier.fillMaxWidth()) }
        } else {
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
                    pair.forEach { Tile(it, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
        WideTile(
            StatTile(Icons.Rounded.EmojiEvents, FeatureAccent.GOALS, data.completedAllTime.toString(), stringResource(R.string.analytics_tile_all_time)),
        )
        if (rate == null) {
            Text(
                stringResource(R.string.analytics_rate_note),
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
            )
        }
    }
}

@Composable
private fun Tile(tile: StatTile, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.dh_stat_desc, tile.label, tile.value)
    val valueColor = if (tile.danger) MaterialTheme.colorScheme.error else Orbit.extras.accent(tile.accent)
    OrbitCard(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        contentPadding = PaddingValues(Spacing.md),
        tint = if (tile.danger) MaterialTheme.colorScheme.error else Orbit.extras.accent(tile.accent),
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
    ) {
        IconTile(tile.icon, tile.accent, size = 38.dp, style = IconTileStyle.Tinted)
        Text(
            tile.value,
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
            maxLines = 1,
            modifier = Modifier.padding(top = Spacing.sm).clearAndSetSemantics { },
        )
        Text(
            tile.label,
            style = MaterialTheme.typography.bodySmall,
            color = Orbit.extras.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.heightIn(min = 32.dp).clearAndSetSemantics { },
        )
    }
}

@Composable
private fun WideTile(tile: StatTile) {
    val description = stringResource(R.string.dh_stat_desc, tile.label, tile.value)
    OrbitCard(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        accent = Orbit.extras.accent(tile.accent),
        tint = Orbit.extras.accent(tile.accent),
        style = CardStyle.Tinted,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconTile(tile.icon, tile.accent, size = 44.dp)
            Text(
                tile.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clearAndSetSemantics { },
            )
            Text(
                tile.value,
                style = MaterialTheme.typography.titleLarge,
                color = Orbit.extras.accentText(tile.accent),
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

@Composable
private fun CardTitle(title: String, icon: ImageVector, accent: FeatureAccent, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
        IconTile(icon, accent, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Orbit.extras.textMuted)
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: FeatureAccent,
    values: List<Int>,
    labels: List<String>,
    pointLabels: List<String>,
) {
    val points = values.indices.map { i -> stringResource(R.string.analytics_chart_point, pointLabels.getOrElse(i) { "" }, values[i]) }
    val description = "$title. $subtitle. " + points.joinToString(", ")
    OrbitCard(Modifier.fillMaxWidth()) {
        CardTitle(title, icon, accent, subtitle)
        if (values.all { it == 0 }) {
            Text(
                stringResource(R.string.analytics_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
        BarChart(
            values,
            labels,
            description,
            Modifier.padding(top = Spacing.md),
            barTop = Orbit.extras.accent(accent),
            barBottom = accent.to,
        )
    }
}

@Composable
private fun ProjectsCard(items: List<ProjectStat>) {
    OrbitCard(Modifier.fillMaxWidth()) {
        CardTitle(stringResource(R.string.analytics_projects_title), Icons.Rounded.Folder, FeatureAccent.PROJECTS)
        if (items.isEmpty()) {
            HintText(stringResource(R.string.analytics_projects_empty))
        }
        items.forEach { item ->
            val p = item.progress
            MeterRow(
                name = item.project.name,
                dotColor = Color(item.project.color),
                barColor = Orbit.extras.accent(FeatureAccent.PROJECTS),
                percent = p.percent,
                fraction = p.fraction,
                detail = if (p.total == 0) stringResource(R.string.analytics_project_no_tasks)
                else pluralStringResource(R.plurals.analytics_tasks_done_of, p.total, p.done, p.total),
            )
        }
    }
}

@Composable
private fun HabitsCard(items: List<HabitStat>) {
    OrbitCard(Modifier.fillMaxWidth(), tint = Orbit.extras.accent(FeatureAccent.HABITS), style = CardStyle.Tinted) {
        CardTitle(stringResource(R.string.analytics_habits_title), Icons.Rounded.Loop, FeatureAccent.HABITS)
        if (items.isEmpty()) {
            HintText(stringResource(R.string.analytics_habits_empty))
        }
        items.forEach { item ->
            val s = item.stats
            MeterRow(
                name = item.habit.name,
                dotColor = Color(item.habit.color),
                barColor = Orbit.extras.accent(FeatureAccent.HABITS),
                percent = s.completionPercent,
                fraction = s.completionPercent / 100f,
                detail = stringResource(R.string.analytics_habit_detail, s.currentStreak, s.longestStreak),
            )
        }
    }
}

@Composable
private fun GoalsCard(items: List<GoalStat>) {
    OrbitCard(Modifier.fillMaxWidth(), tint = Orbit.extras.accent(FeatureAccent.GOALS), style = CardStyle.Tinted) {
        CardTitle(stringResource(R.string.analytics_goals_title), Icons.Rounded.EmojiEvents, FeatureAccent.GOALS)
        if (items.isEmpty()) {
            HintText(stringResource(R.string.analytics_goals_empty))
        }
        items.forEach { item ->
            MeterRow(
                name = item.goal.title,
                dotColor = Orbit.extras.accent(FeatureAccent.GOALS),
                barColor = Orbit.extras.accent(FeatureAccent.GOALS),
                percent = item.percent,
                fraction = item.percent / 100f,
                detail = null,
            )
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Orbit.extras.textMuted,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(top = Spacing.sm),
    )
}

@Composable
private fun MeterRow(name: String, dotColor: Color, barColor: Color, percent: Int, fraction: Float, detail: String?) {
    val percentText = stringResource(R.string.analytics_percent, percent)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = Spacing.md)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ColorDot(dotColor)
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(percentText, style = MaterialTheme.typography.labelLarge, color = barColor)
        }
        OrbitProgressBar(fraction.coerceIn(0f, 1f), color = barColor)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Orbit.extras.textMuted)
        }
    }
}
