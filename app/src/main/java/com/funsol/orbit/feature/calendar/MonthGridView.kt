package com.funsol.orbit.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.priorityColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun MonthCard(
    state: CalendarUiState,
    firstDayOfWeek: DayOfWeek,
    onSelect: (LocalDate) -> Unit,
    onShift: (Long) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var drag by remember { mutableFloatStateOf(0f) }
    val showTodayChip = state.selected != state.today || state.month != YearMonth.from(state.today)

    OrbitCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        MonthHeader(
            month = state.month,
            showToday = showTodayChip,
            onPrev = { onShift(-1) },
            onNext = { onShift(1) },
            onToday = onToday,
        )
        Column(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm)) {
            WeekdayHeader(firstDayOfWeek)
            Column(
                Modifier.pointerInput(rtl) {
                    val threshold = 64.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragCancel = { drag = 0f },
                        onDragEnd = {
                            if (abs(drag) > threshold) {
                                val forward = if (rtl) drag > 0 else drag < 0
                                onShift(if (forward) 1 else -1)
                            }
                            drag = 0f
                        },
                        onHorizontalDrag = { _, dx -> drag += dx },
                    )
                },
            ) {
                AnimatedContent(
                    targetState = state.month,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val sign = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
                        (slideInHorizontally(tween(260)) { w -> sign * w / 3 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(260)) { w -> -sign * w / 3 } + fadeOut(tween(120))) using
                            SizeTransform(clip = false)
                    },
                    label = "month",
                ) { month ->
                    val weeks = remember(month, firstDayOfWeek) { CalendarGrid.build(month, firstDayOfWeek) }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        weeks.forEach { week ->
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { date ->
                                    DayCell(
                                        date = date,
                                        inMonth = CalendarGrid.isInMonth(date, month),
                                        isToday = date == state.today,
                                        isSelected = date == state.selected,
                                        info = state.days[date.toEpochDayInt()],
                                        onClick = { onSelect(date) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    showToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Orbit.extras.accentGradient(FeatureAccent.CALENDAR))
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundNavButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            description = stringResource(R.string.calendar_prev_month),
            onClick = onPrev,
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                targetState = month,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "monthTitle",
            ) { m ->
                Text(
                    DateFormats.monthYear(m.atDay(1)),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (showToday) {
                val description = stringResource(R.string.calendar_today)
                Text(
                    stringResource(R.string.calendar_today_chip),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = Spacing.xs)
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .clickable(role = Role.Button, onClick = onToday)
                        .semantics { contentDescription = description }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                )
            }
        }
        RoundNavButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            description = stringResource(R.string.calendar_next_month),
            onClick = onNext,
        )
    }
}

@Composable
private fun RoundNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(Spacing.touchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = Color.White)
        }
    }
}

@Composable
private fun WeekdayHeader(first: DayOfWeek) {
    val locale = Locale.getDefault()
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs).clearAndSetSemantics { }) {
        CalendarGrid.weekdayOrder(first).forEach { day ->
            Text(
                day.getDisplayName(TextStyle.SHORT, locale),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = Orbit.extras.textMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    info: DayInfo?,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val extras = Orbit.extras
    val gradient = extras.accentGradient(FeatureAccent.CALENDAR)
    val coral = extras.accent(FeatureAccent.CALENDAR)
    val shape = RoundedCornerShape(14.dp)
    val numberColor = when {
        isSelected -> Color.White
        isToday -> coral
        inMonth -> scheme.onSurface
        else -> extras.textMuted.copy(alpha = 0.5f)
    }

    val parts = ArrayList<String>(4)
    parts += DateFormats.fullDate(date)
    if (isToday) parts += stringResource(R.string.calendar_cell_today)
    if (info != null && info.total > 0) {
        parts += pluralStringResource(R.plurals.calendar_cell_tasks, info.total, info.total)
        if (info.overdue > 0) parts += stringResource(R.string.calendar_cell_overdue, info.overdue)
    }
    val description = parts.joinToString(", ")

    Box(
        Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .padding(1.dp)
            .clip(shape)
            .then(if (isSelected) Modifier.background(gradient) else Modifier)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, gradient, shape) else Modifier)
            .selectable(selected = isSelected, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = numberColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            Row(
                Modifier.heightIn(min = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                info?.dots?.forEach { dot -> TaskDot(dot, isSelected) }
                val overflow = (info?.total ?: 0) - (info?.dots?.size ?: 0)
                if (overflow > 0) {
                    Text(
                        stringResource(R.string.calendar_cell_more, overflow),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else extras.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskDot(dot: DayDot, onSelected: Boolean) {
    val base = when {
        onSelected -> Color.White
        dot.kind == DotKind.OVERDUE -> MaterialTheme.colorScheme.error
        else -> priorityColor(dot.priority)
    }
    if (dot.kind == DotKind.COMPLETED) {
        Box(Modifier.size(6.dp).clip(CircleShape).border(1.dp, base.copy(alpha = 0.8f), CircleShape))
    } else {
        Box(Modifier.size(6.dp).clip(CircleShape).background(base))
    }
}
