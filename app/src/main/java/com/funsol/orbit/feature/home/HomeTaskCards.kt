package com.funsol.orbit.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.heading
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.OrbitCard
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.TagPill
import com.funsol.orbit.core.designsystem.priorityColor
import com.funsol.orbit.core.designsystem.priorityLabel
import java.time.LocalDate

@Composable
internal fun TodayCard(
    tasks: List<TaskEntity>,
    totalCount: Int,
    onToggle: (TaskEntity) -> Unit,
    onOpen: (TaskEntity) -> Unit,
    onViewAll: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = Orbit.extras
    val tasksColor = extras.accent(FeatureAccent.TASKS)
    // Tinted card with a faint watermark (same language as the More screen cards) so Today stands out.
    OrbitCard(
        modifier = modifier.fillMaxWidth().animateContentSize(tween(220)),
        accent = tasksColor,
        tint = tasksColor,
        style = CardStyle.Tinted,
        elevation = CardElevation.Lifted,
        contentPadding = PaddingValues(Spacing.md),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Icon(
                Icons.Rounded.Today,
                contentDescription = null,
                tint = tasksColor.copy(alpha = 0.10f),
                modifier = Modifier.align(Alignment.TopEnd).size(88.dp).offset(x = 22.dp, y = (-18).dp),
            )
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconTile(Icons.Rounded.Today, FeatureAccent.TASKS, size = 36.dp)
                Text(
                    stringResource(R.string.home_today_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (tasks.isNotEmpty()) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(extras.accentGradient(FeatureAccent.TASKS))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            totalCount.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                    TextButton(
                        onClick = onViewAll,
                        contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 0.dp),
                    ) {
                        Text(stringResource(R.string.home_view_all), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.padding(start = Spacing.xs).size(16.dp),
                        )
                    }
                }
            }
        }
        if (tasks.isEmpty()) {
            Text(
                stringResource(R.string.home_today_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            HomeActionButton(
                label = stringResource(R.string.home_add_task),
                icon = Icons.Rounded.Add,
                onClick = onAddTask,
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
            tasks.forEachIndexed { index, task ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 44.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    )
                }
                HomeTaskRow(
                    task = task,
                    supporting = task.dueTime?.let { DateFormats.time(it) },
                    overdue = false,
                    onToggle = { onToggle(task) },
                    onOpen = { onOpen(task) },
                )
            }
            if (totalCount > tasks.size) {
                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                ) {
                    Text(
                        stringResource(R.string.home_n_more, totalCount - tasks.size),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
internal fun OverdueCard(
    tasks: List<TaskEntity>,
    totalCount: Int,
    onToggle: (TaskEntity) -> Unit,
    onOpen: (TaskEntity) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = MaterialTheme.colorScheme.error
    HomeCard(modifier) {
        HomeSectionTitle(
            title = pluralStringResource(R.plurals.home_overdue_title, totalCount, totalCount),
            icon = Icons.Rounded.WarningAmber,
            iconTint = error,
            actionLabel = stringResource(R.string.home_view_all),
            onAction = onViewAll,
            titleColor = error,
        )
        tasks.forEachIndexed { index, task ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 44.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                )
            }
            HomeTaskRow(
                task = task,
                supporting = task.dueDate?.let { stringResource(R.string.home_due_on, DateFormats.dateShort(it)) },
                overdue = true,
                onToggle = { onToggle(task) },
                onOpen = { onOpen(task) },
            )
        }
        if (totalCount > tasks.size) {
            TextButton(
                onClick = onViewAll,
                contentPadding = PaddingValues(horizontal = Spacing.sm),
            ) {
                Text(
                    stringResource(R.string.home_n_more, totalCount - tasks.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = error,
                )
            }
        }
    }
}

@Composable
internal fun UpcomingCard(
    groups: List<UpcomingDay>,
    totalCount: Int,
    today: LocalDate,
    onToggle: (TaskEntity) -> Unit,
    onOpen: (TaskEntity) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeCard(modifier) {
        HomeSectionTitle(
            title = stringResource(R.string.home_upcoming_title),
            icon = Icons.Rounded.Event,
            actionLabel = stringResource(R.string.home_view_all).takeIf { groups.isNotEmpty() },
            onAction = onViewAll.takeIf { groups.isNotEmpty() },
        )
        if (groups.isEmpty()) {
            Text(
                stringResource(R.string.home_upcoming_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        } else {
            groups.forEachIndexed { groupIndex, group ->
                Text(
                    dayLabel(group.epochDay, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = Orbit.extras.textMuted,
                    modifier = Modifier.padding(
                        top = if (groupIndex == 0) Spacing.sm else Spacing.md,
                        bottom = Spacing.xxs,
                    ),
                )
                group.tasks.forEachIndexed { index, task ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 44.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        )
                    }
                    HomeTaskRow(
                        task = task,
                        supporting = task.dueTime?.let { DateFormats.time(it) },
                        overdue = false,
                        onToggle = { onToggle(task) },
                        onOpen = { onOpen(task) },
                    )
                }
            }
            val shown = groups.sumOf { it.tasks.size }
            if (totalCount > shown) {
                TextButton(
                    onClick = onViewAll,
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                ) {
                    Text(
                        stringResource(R.string.home_n_more, totalCount - shown),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun dayLabel(epochDay: Int, today: LocalDate): String {
    val date = epochDay.toLocalDate()
    return if (date == today.plusDays(1)) {
        stringResource(R.string.home_tomorrow)
    } else {
        stringResource(R.string.home_date_label, DateFormats.weekdayLong(date), DateFormats.dateShort(epochDay))
    }
}

@Composable
private fun CheckCircle(onClick: () -> Unit, tint: Color, description: String) {
    Box(
        Modifier
            .size(Spacing.touchTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(22.dp).border(1.8.dp, tint, CircleShape))
    }
}

@Composable
internal fun HomeTaskRow(
    task: TaskEntity,
    supporting: String?,
    overdue: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneLabel = stringResource(R.string.home_task_mark_done, task.title)
    val tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(onClick = onToggle, tint = tint, description = doneLabel)
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clickable(onClick = onOpen)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else Orbit.extras.textMuted,
                    maxLines = 1,
                )
            }
        }
        if (task.priority == Priority.HIGH || task.priority == Priority.URGENT) {
            Spacer(Modifier.width(Spacing.sm))
            TagPill(priorityLabel(task.priority), priorityColor(task.priority))
        }
    }
}
