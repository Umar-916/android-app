package com.funsol.orbit.feature.habits

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.feature.projects.toGradient
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private data class MonthPage(val month: YearMonth, val weeks: List<List<DayCell?>>)

@Composable
fun HabitCalendarCard(
    state: HabitDetailUiState,
    accent: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggle: (DayCell) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val navColors = IconButtonDefaults.iconButtonColors(
        containerColor = Orbit.extras.well,
        disabledContainerColor = Orbit.extras.well.copy(alpha = 0.5f),
    )
    OrbitCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = state.canGoPrevious, colors = navColors) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.habits_cal_previous))
            }
            Text(
                DateFormats.monthYear(state.month.atDay(1), locale),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = onNext, enabled = state.canGoNext, colors = navColors) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.habits_cal_next))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cellWidth = maxWidth / 7
            val circle = (cellWidth - 2.dp).coerceIn(24.dp, 38.dp)
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(top = Spacing.xs).clearAndSetSemantics { }) {
                    HabitCalendarMath.weekdayOrder(state.firstDayOfWeek).forEach { day ->
                        Text(
                            day.getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelMedium,
                            color = Orbit.extras.textMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                AnimatedContent(
                    targetState = MonthPage(state.month, state.weeks),
                    contentKey = { it.month },
                    transitionSpec = {
                        val forward = targetState.month.isAfter(initialState.month)
                        val direction = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
                        (slideInHorizontally(tween(280)) { it * direction } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(280)) { -it * direction } + fadeOut(tween(160)))
                    },
                    label = "habitMonth",
                ) { page ->
                    Column(Modifier.fillMaxWidth()) {
                        page.weeks.forEach { week ->
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { cell ->
                                    Box(Modifier.weight(1f).height(Spacing.touchTarget), contentAlignment = Alignment.Center) {
                                        if (cell != null) DayCellView(cell, accent, cellWidth, circle, onClick = { onToggle(cell) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Legend(accent)
    }
}

@Composable
private fun DayCellView(cell: DayCell, accent: Color, cellWidth: Dp, circle: Dp, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val statusText = stringResource(
        when (cell.status) {
            DayStatus.DONE -> R.string.habits_cal_done
            DayStatus.MISSED -> R.string.habits_cal_missed
            DayStatus.SCHEDULED -> R.string.habits_cal_scheduled
            DayStatus.NOT_SCHEDULED -> R.string.habits_cal_not_scheduled
        },
    )
    val description = stringResource(R.string.habits_cal_day_desc, DateFormats.fullDate(cell.date), statusText)
    val done = cell.status == DayStatus.DONE
    val fill by animateFloatAsState(if (done) 1f else 0f, tween(220), label = "dayFill")
    val ring: Color? = when (cell.status) {
        DayStatus.MISSED -> scheme.error.copy(alpha = 0.75f)
        DayStatus.SCHEDULED -> accent.copy(alpha = 0.6f)
        else -> null
    }
    val tint: Color = when (cell.status) {
        DayStatus.MISSED -> scheme.error.copy(alpha = 0.10f)
        DayStatus.SCHEDULED -> accent.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val textColor = when (cell.status) {
        DayStatus.DONE -> Color.White
        DayStatus.NOT_SCHEDULED -> scheme.onSurface.copy(alpha = 0.34f)
        else -> scheme.onSurface
    }
    Box(
        Modifier
            .size(width = cellWidth, height = Spacing.touchTarget)
            .then(if (cell.toggleable) Modifier.clip(CircleShape).clickable(role = Role.Checkbox, onClick = onClick) else Modifier)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(circle)
                .clip(CircleShape)
                .background(tint)
                .then(if (ring != null) Modifier.border(BorderStroke(if (cell.isToday) 2.5.dp else 1.5.dp, ring), CircleShape) else Modifier)
                .then(
                    if (cell.isToday && ring == null && !done) Modifier.border(BorderStroke(1.dp, scheme.outline), CircleShape) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = fill; scaleX = 0.8f + 0.2f * fill; scaleY = 0.8f + 0.2f * fill }.background(accent.toGradient()))
            Text(
                cell.date.dayOfMonth.toString(),
                style = if (isLargeFontScale()) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                fontWeight = if (cell.isToday || done) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(accent: Color) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        Modifier.fillMaxWidth().padding(top = Spacing.xs).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        LegendItem(Modifier.background(accent.toGradient()), stringResource(R.string.habits_cal_done))
        LegendItem(
            Modifier.background(scheme.error.copy(alpha = 0.10f)).border(1.5.dp, scheme.error.copy(alpha = 0.75f), CircleShape),
            stringResource(R.string.habits_cal_missed),
        )
        LegendItem(Modifier.border(1.5.dp, accent.copy(alpha = 0.6f), CircleShape), stringResource(R.string.habits_cal_scheduled))
        LegendItem(Modifier.alpha(0.34f).background(scheme.onSurface), stringResource(R.string.habits_cal_not_scheduled))
    }
}

@Composable
private fun LegendItem(swatch: Modifier, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(Modifier.size(10.dp).clip(CircleShape).then(swatch))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Orbit.extras.textMuted)
    }
}
