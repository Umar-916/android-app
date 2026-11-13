package com.funsol.orbit.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitProgressBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.core.designsystem.orbitGlow

@Composable
internal fun ProjectsCard(
    projects: List<HomeProjectItem>,
    totalActive: Int,
    onOpen: (String) -> Unit,
    onNewProject: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_projects_title),
            icon = Icons.Rounded.Folder,
            accent = FeatureAccent.PROJECTS,
            actionLabel = stringResource(R.string.home_view_all).takeIf { projects.isNotEmpty() },
            onAction = onViewAll.takeIf { projects.isNotEmpty() },
        )
        if (projects.isEmpty()) {
            OrbitCard(Modifier.fillMaxWidth(), tint = Orbit.extras.accent(FeatureAccent.PROJECTS), style = CardStyle.Tinted, elevation = CardElevation.Flat) {
                Text(
                    stringResource(R.string.home_projects_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
                GradientButton(
                    stringResource(R.string.home_new_project),
                    onNewProject,
                    Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Add,
                    brush = Orbit.extras.accentGradient(FeatureAccent.PROJECTS),
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(vertical = Spacing.xs),
            ) {
                items(projects, key = { it.project.id }, contentType = { "project" }) { item ->
                    ProjectGradientCard(item) { onOpen(item.project.id) }
                }
                if (totalActive > projects.size) {
                    item(key = "more", contentType = "more") {
                        Box(
                            Modifier.width(84.dp).height(92.dp).clip(RoundedCornerShape(16.dp))
                                .background(Orbit.extras.well)
                                .clickable(role = Role.Button, onClick = onViewAll),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.home_n_more, totalActive - projects.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectGradientCard(item: HomeProjectItem, onClick: () -> Unit) {
    val progress = item.progress
    val base = Color(item.project.color)
    val shape = RoundedCornerShape(16.dp)
    val summary = if (progress.total == 0) {
        stringResource(R.string.home_project_no_tasks)
    } else {
        pluralStringResource(R.plurals.home_tasks_done_of, progress.total, progress.done, progress.total)
    }
    Column(
        Modifier
            .width(140.dp)
            .heightIn(min = 92.dp)
            .orbitGlow(shape, base.copy(alpha = 0.18f), radius = 8.dp, offsetY = 3.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(base, lerp(base, Color.Black, 0.28f))))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            item.project.name,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(Modifier.padding(top = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.home_percent, progress.percent),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
            }
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.28f))) {
                Box(Modifier.fillMaxWidth(progress.fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Color.White))
            }
            Text(
                summary,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun HabitsCard(
    habits: List<HomeHabitItem>,
    doneCount: Int,
    onToggle: (HomeHabitItem, Boolean) -> Unit,
    onNewHabit: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OrbitCard(modifier, tint = Orbit.extras.accent(FeatureAccent.HABITS), style = CardStyle.Tinted) {
        HomeSectionHeader(
            title = stringResource(R.string.home_habits_title),
            icon = Icons.Rounded.Loop,
            accent = FeatureAccent.HABITS,
            actionLabel = stringResource(R.string.home_view_all),
            onAction = onViewAll,
        )
        if (habits.isEmpty()) {
            Text(
                stringResource(R.string.home_habits_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
            GradientButton(
                stringResource(R.string.home_new_habit),
                onNewHabit,
                Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Add,
                brush = Orbit.extras.accentGradient(FeatureAccent.HABITS),
            )
        } else {
            Text(
                stringResource(R.string.home_habits_summary, doneCount, habits.size),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            OrbitProgressBar(
                (doneCount.toFloat() / habits.size).coerceIn(0f, 1f),
                modifier = Modifier.padding(vertical = Spacing.sm),
                color = Orbit.extras.accentText(FeatureAccent.HABITS),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(habits, key = { it.habit.id }, contentType = { "habit" }) { item ->
                    HabitCheckIn(item) { checked -> onToggle(item, checked) }
                }
            }
        }
    }
}

@Composable
private fun HabitCheckIn(item: HomeHabitItem, onChecked: (Boolean) -> Unit) {
    val color = Color(item.habit.color)
    val checked = item.checkedToday
    Column(
        Modifier
            .width(68.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChecked)
            .semantics { contentDescription = item.habit.name }
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .then(if (checked) Modifier.orbitGlow(CircleShape, color.copy(alpha = 0.35f), radius = 8.dp, offsetY = 3.dp) else Modifier)
                .clip(CircleShape)
                .background(if (checked) color else color.copy(alpha = 0.12f))
                .border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    item.habit.name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
        Text(
            item.habit.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { },
        )
        if (item.currentStreak > 0) {
            val warm = Orbit.extras.accent(FeatureAccent.NOTES)
            val streakDesc = pluralStringResource(R.plurals.home_streak_days, item.currentStreak, item.currentStreak)
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(warm.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .semantics { contentDescription = streakDesc },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = warm, modifier = Modifier.size(14.dp))
                Text(item.currentStreak.toString(), style = MaterialTheme.typography.labelSmall, color = warm)
            }
        }
    }
}

@Composable
internal fun OverviewCard(overview: HomeOverview, modifier: Modifier = Modifier) {
    val stacked = isLargeFontScale()
    val tiles = listOf<@Composable (Modifier) -> Unit>(
        { m -> OverviewTile(Icons.Rounded.TaskAlt, FeatureAccent.HABITS, overview.completedLast7Days, stringResource(R.string.home_ov_completed), m) },
        { m -> OverviewTile(Icons.Rounded.Checklist, FeatureAccent.TASKS, overview.openTasks, stringResource(R.string.home_ov_open), m) },
        { m -> OverviewTile(Icons.Rounded.Folder, FeatureAccent.PROJECTS, overview.activeProjects, stringResource(R.string.home_ov_projects), m) },
        { m -> OverviewTile(Icons.Rounded.LocalFireDepartment, FeatureAccent.NOTES, overview.bestStreak, stringResource(R.string.home_ov_streak), m) },
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HomeSectionHeader(stringResource(R.string.home_overview_title), Icons.Rounded.Insights, FeatureAccent.ANALYTICS)
        if (stacked) {
            tiles.forEach { tile -> tile(Modifier.fillMaxWidth()) }
        } else {
            tiles.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    pair.forEach { tile -> tile(Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}

@Composable
private fun OverviewTile(icon: ImageVector, accent: FeatureAccent, value: Int, label: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.dh_stat_desc, label, value)
    val accentColor = Orbit.extras.accent(accent)
    OrbitCard(
        modifier.heightIn(min = 80.dp).semantics(mergeDescendants = true) { contentDescription = description },
        contentPadding = PaddingValues(12.dp),
        tint = accentColor,
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconTile(icon, accent, size = 34.dp, style = IconTileStyle.Tinted)
            Column(Modifier.weight(1f)) {
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Orbit.extras.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

@Composable
internal fun QuickAddRow(
    onTask: () -> Unit,
    onNote: () -> Unit,
    onHabit: () -> Unit,
    onProject: () -> Unit,
    onGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(vertical = 0.dp, horizontal = Spacing.xxs),
    ) {
        item(key = "task") { QuickPill(Icons.Rounded.CheckBox, stringResource(R.string.home_qa_task), FeatureAccent.TASKS, onTask) }
        item(key = "note") { QuickPill(Icons.Rounded.StickyNote2, stringResource(R.string.home_qa_note), FeatureAccent.NOTES, onNote) }
        item(key = "habit") { QuickPill(Icons.Rounded.Loop, stringResource(R.string.home_qa_habit), FeatureAccent.HABITS, onHabit) }
        item(key = "project") { QuickPill(Icons.Rounded.Folder, stringResource(R.string.home_qa_project), FeatureAccent.PROJECTS, onProject) }
        item(key = "goal") { QuickPill(Icons.Rounded.EmojiEvents, stringResource(R.string.home_qa_goal), FeatureAccent.GOALS, onGoal) }
    }
}

@Composable
private fun QuickPill(icon: ImageVector, label: String, accent: FeatureAccent, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val extras = Orbit.extras
    val accentColor = extras.accent(accent)
    Box(
        Modifier
            .heightIn(min = 34.5.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .background(extras.tinted(accentColor, 0.10f))
            .border(0.8.dp, accentColor.copy(alpha = 0.22f), shape)
            .padding(horizontal = 12.dp, vertical = 4.5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
internal fun WelcomeCard(onTask: () -> Unit, onProject: () -> Unit, onHabit: () -> Unit, modifier: Modifier = Modifier) {
    OrbitCard(modifier, tint = Orbit.extras.accent(FeatureAccent.HOME), style = CardStyle.Tinted, elevation = CardElevation.Soft) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
            IconTile(Icons.Rounded.EmojiEvents, FeatureAccent.HOME)
            Text(
                stringResource(R.string.home_welcome_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            stringResource(R.string.home_welcome_message),
            style = MaterialTheme.typography.bodyMedium,
            color = Orbit.extras.textMuted,
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
        )
        GradientButton(stringResource(R.string.home_qa_task), onTask, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
        if (isLargeFontScale()) {
            Column(Modifier.fillMaxWidth().padding(top = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                WellButton(stringResource(R.string.home_qa_project), onProject, Modifier.fillMaxWidth())
                WellButton(stringResource(R.string.home_qa_habit), onHabit, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                WellButton(stringResource(R.string.home_qa_project), onProject, Modifier.weight(1f))
                WellButton(stringResource(R.string.home_qa_habit), onHabit, Modifier.weight(1f))
            }
        }
    }
}
