package com.funsol.orbit.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.Priority
import com.funsol.orbit.core.database.entity.TaskEntity
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
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
    val tasksColor = Orbit.extras.accent(FeatureAccent.TASKS)
    OrbitCard(modifier, accent = tasksColor, tint = tasksColor, style = CardStyle.Tinted, elevation = CardElevation.Soft) {
        HomeSectionHeader(
            title = stringResource(R.string.home_today_title),
            icon = Icons.Rounded.Today,
            accent = FeatureAccent.TASKS,
            actionLabel = stringResource(R.string.home_view_all).takeIf { tasks.isNotEmpty() },
            onAction = onViewAll.takeIf { tasks.isNotEmpty() },
        )
        if (tasks.isEmpty()) {
            Text(
                stringResource(R.string.home_today_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
            GradientButton(
                stringResource(R.string.home_add_task),
                onAddTask,
                Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Add,
            )
        } else {
            tasks.forEachIndexed { index, task ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                HomeTaskRow(
                    task = task,
                    supporting = task.dueTime?.let { DateFormats.time(it) },
                    overdue = false,
                    onToggle = { onToggle(task) },
                    onOpen = { onOpen(task) },
                )
            }
            if (totalCount > tasks.size) {
                Text(
                    stringResource(R.string.home_n_more, totalCount - tasks.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
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
    OrbitCard(modifier, contentPadding = PaddingValues(0.dp), accent = error) {
        Column(Modifier.fillMaxWidth().background(error.copy(alpha = 0.06f)).padding(14.dp)) {
            HomeSectionHeader(
                title = pluralStringResource(R.plurals.home_overdue_title, totalCount, totalCount),
                icon = Icons.Rounded.WarningAmber,
                accent = FeatureAccent.PREMIUM,
                actionLabel = stringResource(R.string.home_view_all),
                onAction = onViewAll,
                titleColor = error,
            )
            tasks.forEachIndexed { index, task ->
                if (index > 0) HorizontalDivider(color = error.copy(alpha = 0.18f))
                HomeTaskRow(
                    task = task,
                    supporting = task.dueDate?.let { stringResource(R.string.home_due_on, DateFormats.dateShort(it)) },
                    overdue = true,
                    onToggle = { onToggle(task) },
                    onOpen = { onOpen(task) },
                )
            }
            if (totalCount > tasks.size) {
                Text(
                    stringResource(R.string.home_n_more, totalCount - tasks.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = error,
                    modifier = Modifier.padding(top = Spacing.xs),
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
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_upcoming_title),
            icon = Icons.Rounded.Event,
            accent = FeatureAccent.CALENDAR,
            actionLabel = stringResource(R.string.home_view_all).takeIf { groups.isNotEmpty() },
            onAction = onViewAll.takeIf { groups.isNotEmpty() },
        )
        if (groups.isEmpty()) {
            OrbitCard(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.home_upcoming_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                )
            }
        } else {
            val entries = groups.flatMap { g -> g.tasks.map { g.epochDay to it } }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                contentPadding = PaddingValues(vertical = Spacing.sm),
            ) {
                items(entries, key = { it.second.id }, contentType = { "upcoming" }) { (day, task) ->
                    UpcomingChipCard(day, task, today, onToggle = { onToggle(task) }, onOpen = { onOpen(task) })
                }
                if (totalCount > entries.size) {
                    item(key = "more", contentType = "more") {
                        Box(
                            Modifier.heightIn(min = 92.dp).width(84.dp).clip(RoundedCornerShape(16.dp))
                                .background(Orbit.extras.well)
                                .padding(Spacing.sm)
                                .clickable(role = Role.Button, onClick = onViewAll),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.home_n_more, totalCount - entries.size),
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
private fun UpcomingChipCard(day: Int, task: TaskEntity, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit) {
    val coral = Orbit.extras.accent(FeatureAccent.CALENDAR)
    val doneLabel = stringResource(R.string.home_task_mark_done, task.title)
    OrbitCard(
        Modifier.width(152.dp),
        onClick = onOpen,
        tint = coral,
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f, fill = false)
                    .clip(RoundedCornerShape(50))
                    .background(Orbit.extras.accentGradient(FeatureAccent.CALENDAR))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    dayLabel(day, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            task.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.sm).heightIn(min = 34.dp),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                task.dueTime?.let { DateFormats.time(it) } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            CheckCircle(onClick = onToggle, tint = coral, description = doneLabel)
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
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(onClick = onToggle, tint = tint, description = doneLabel)
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(onClick = onOpen)
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
