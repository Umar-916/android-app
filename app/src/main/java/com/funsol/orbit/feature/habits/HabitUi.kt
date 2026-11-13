package com.funsol.orbit.feature.habits

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.WeekdayMask
import com.funsol.orbit.feature.projects.toGradient
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

fun weekdayShortRes(day: DayOfWeek): Int = when (day) {
    DayOfWeek.MONDAY -> R.string.weekday_mon
    DayOfWeek.TUESDAY -> R.string.weekday_tue
    DayOfWeek.WEDNESDAY -> R.string.weekday_wed
    DayOfWeek.THURSDAY -> R.string.weekday_thu
    DayOfWeek.FRIDAY -> R.string.weekday_fri
    DayOfWeek.SATURDAY -> R.string.weekday_sat
    DayOfWeek.SUNDAY -> R.string.weekday_sun
}

fun weekdayFullName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Locale.getDefault())

fun isDayScheduled(mask: Int, day: DayOfWeek): Boolean = (mask shr (day.value - 1)) and 1 == 1

@Composable
fun scheduleSummary(mask: Int): String = when (HabitSchedule.presetOf(mask)) {
    SchedulePreset.DAILY -> stringResource(R.string.habits_schedule_daily_summary)
    SchedulePreset.WEEKDAYS -> stringResource(R.string.habits_schedule_weekdays_summary)
    SchedulePreset.WEEKENDS -> stringResource(R.string.habits_schedule_weekends_summary)
    SchedulePreset.CUSTOM -> {
        val separator = stringResource(R.string.habits_list_separator)
        WeekdayMask.toDays(mask).map { stringResource(weekdayShortRes(it)) }.joinToString(separator)
    }
}

@Composable
fun WeekdayDots(
    mask: Int,
    color: Color,
    modifier: Modifier = Modifier,
    today: DayOfWeek? = null,
    offColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
    ringColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    dot: Dp = 9.dp,
) {
    Row(modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        DayOfWeek.entries.forEach { day ->
            val on = isDayScheduled(mask, day)
            Box(
                Modifier
                    .size(dot)
                    .clip(CircleShape)
                    .background(if (on) color else offColor)
                    .then(if (day == today) Modifier.border(1.5.dp, ringColor, CircleShape) else Modifier),
            )
        }
    }
}

@Composable
fun MiniRing(
    fraction: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "miniRing")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val ringSize = size * LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(ringSize)) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            if (animated > 0f) {
                drawArc(
                    brush = color.toGradient(),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
    }
}
