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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitProgressBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.isLargeFontScale

@Composable
internal fun ProjectsCard(
    projects: List<HomeProjectItem>,
    totalActive: Int,
    onOpen: (String) -> Unit,
    onNewProject: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeCard(modifier) {
        HomeSectionTitle(
            title = stringResource(R.string.home_projects_title),
            icon = Icons.Rounded.Folder,
            actionLabel = stringResource(R.string.home_view_all).takeIf { projects.isNotEmpty() },
            onAction = onViewAll.takeIf { projects.isNotEmpty() },
        )
        if (projects.isEmpty()) {
            Text(
                stringResource(R.string.home_projects_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            HomeActionButton(
                label = stringResource(R.string.home_new_project),
                icon = Icons.Rounded.Add,
                onClick = onNewProject,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
                    .attentionPulse(
                        enabled = true,
                        delayMillis = 900L,
                        rounds = 1,
                        maxScale = 1.02f,
                        lift = 2.dp,
                    ),
            )
        } else {
            projects.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    )
                }
                ProjectRow(item) { onOpen(item.project.id) }
            }
            if (totalActive > projects.size) {
                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                ) {
                    Text(
                        stringResource(R.string.home_n_more, totalActive - projects.size),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(item: HomeProjectItem, onClick: () -> Unit) {
    val progress = item.progress
    val accent = Color(item.project.color)
    val summary = if (progress.total == 0) {
        stringResource(R.string.home_project_no_tasks)
    } else {
        pluralStringResource(R.plurals.home_tasks_done_of, progress.total, progress.done, progress.total)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.project.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.home_percent, progress.percent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        OrbitProgressBar(progress.fraction, height = 4.dp, color = accent)
        Text(
            summary,
            style = MaterialTheme.typography.bodySmall,
            color = Orbit.extras.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    HomeCard(modifier) {
        HomeSectionTitle(
            title = stringResource(R.string.home_habits_title),
            icon = Icons.Rounded.Loop,
            actionLabel = stringResource(R.string.home_view_all),
            onAction = onViewAll,
        )
        if (habits.isEmpty()) {
            Text(
                stringResource(R.string.home_habits_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            HomeActionButton(
                label = stringResource(R.string.home_new_habit),
                icon = Icons.Rounded.Add,
                onClick = onNewHabit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
                    .attentionPulse(
                        enabled = true,
                        delayMillis = 900L,
                        rounds = 1,
                        maxScale = 1.02f,
                        lift = 2.dp,
                    ),
            )
        } else {
            Text(
                stringResource(R.string.home_habits_summary, doneCount, habits.size),
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            LazyRow(
                modifier = Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
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
            .width(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChecked)
            .semantics { contentDescription = item.habit.name }
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (checked) color else color.copy(alpha = 0.12f))
                .border(1.4.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
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
            val streakDesc = pluralStringResource(R.plurals.home_streak_days, item.currentStreak, item.currentStreak)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.semantics { contentDescription = streakDesc },
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = Orbit.extras.textMuted,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    item.currentStreak.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Orbit.extras.textMuted,
                )
            }
        }
    }
}

@Composable
internal fun OverviewCard(
    overview: HomeOverview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = listOf(
        overview.completedLast7Days to R.string.home_ov_short_done,
        overview.openTasks to R.string.home_ov_short_open,
        overview.activeProjects to R.string.home_ov_short_projects,
        overview.bestStreak to R.string.home_ov_short_streak,
    )
    OrbitCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardElevation.Soft,
        contentPadding = PaddingValues(Spacing.md),
    ) {
        HomeSectionTitle(
            stringResource(R.string.home_overview_title),
            icon = Icons.Rounded.Insights,
            actionLabel = stringResource(R.string.home_view_all),
            onAction = onClick,
        )
        if (isLargeFontScale()) {
            stats.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    pair.forEach { stat ->
                        OverviewStat(stat.first, stringResource(stat.second), Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(top = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                stats.forEachIndexed { index, stat ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                    OverviewStat(stat.first, stringResource(stat.second), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OverviewStat(value: Int, label: String, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.dh_stat_desc, label, value)
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Orbit.extras.textMuted,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

private class CreateAction(
    val icon: ImageVector,
    val accent: FeatureAccent,
    val label: Int,
    val onClick: () -> Unit,
)

/**
 * The single quick-create entry for the app, pinned to the top of Home. Adding a
 * task/note/habit/project/goal opens its editor and the new item appears in the
 * sections below this grid.
 */
@Composable
internal fun HomeCreateGrid(
    onTask: () -> Unit,
    onNote: () -> Unit,
    onHabit: () -> Unit,
    onProject: () -> Unit,
    onGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        CreateAction(Icons.Rounded.TaskAlt, FeatureAccent.TASKS, R.string.dv3_new_task, onTask),
        CreateAction(Icons.AutoMirrored.Rounded.StickyNote2, FeatureAccent.NOTES, R.string.dv3_new_note, onNote),
        CreateAction(Icons.Rounded.LocalFireDepartment, FeatureAccent.HABITS, R.string.dv3_new_habit, onHabit),
        CreateAction(Icons.Rounded.FolderCopy, FeatureAccent.PROJECTS, R.string.dv3_new_project, onProject),
        CreateAction(Icons.Rounded.Flag, FeatureAccent.GOALS, R.string.dv3_new_goal, onGoal),
    )
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        actions.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                pair.forEach { action ->
                    CreateActionTile(action, Modifier.weight(1f).fillMaxHeight())
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CreateActionTile(action: CreateAction, modifier: Modifier = Modifier) {
    val accentColor = Orbit.extras.accent(action.accent)
    OrbitCard(
        onClick = action.onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        tint = accentColor,
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconTile(action.icon, action.accent, size = 32.dp, style = IconTileStyle.Tinted)
            Text(
                stringResource(action.label),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
