package com.funsol.orbit.feature.tasks

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.TasksEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private fun Int.toPickerMillis(): Long =
    toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toPickerEpochDay(): Int =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toEpochDayInt()

@Composable
private fun PickerSheet(
    title: String,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    OrbitSheet(onDismiss) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        content()
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WellButton(stringResource(R.string.action_cancel), { context.postAnalytic(TasksEvents.TASKS_PICKER_CANCEL_CLICK); onDismiss() }, Modifier.weight(1f))
            GradientButton(stringResource(R.string.action_done), { context.postAnalytic(TasksEvents.TASKS_PICKER_DONE_CLICK); onConfirm() }, Modifier.weight(1f), enabled = confirmEnabled)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDatePickerDialog(
    initialEpochDay: Int?,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    PostAnalytic(TasksEvents.TASKS_DATE_PICKER_SHEET_APPEAR)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initialEpochDay ?: LocalDate.now().toEpochDayInt()).toPickerMillis(),
    )
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    PickerSheet(
        title = stringResource(R.string.tasks_pick_date),
        confirmEnabled = state.selectedDateMillis != null,
        onDismiss = onDismiss,
        onConfirm = { state.selectedDateMillis?.let { onPick(it.toPickerEpochDay()) } },
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DatePicker(
                state = state,
                title = null,
                colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskDateRangePickerDialog(
    initialStart: Int?,
    initialEnd: Int?,
    onDismiss: () -> Unit,
    onPick: (start: Int?, end: Int?) -> Unit,
) {
    PostAnalytic(TasksEvents.TASKS_DATE_RANGE_SHEET_APPEAR)
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart?.toPickerMillis(),
        initialSelectedEndDateMillis = initialEnd?.toPickerMillis(),
    )
    val height = minOf(460f, LocalConfiguration.current.screenHeightDp * 0.6f).dp
    PickerSheet(
        title = stringResource(R.string.tasks_pick_range),
        confirmEnabled = state.selectedStartDateMillis != null,
        onDismiss = onDismiss,
        onConfirm = {
            onPick(
                state.selectedStartDateMillis?.toPickerEpochDay(),
                state.selectedEndDateMillis?.toPickerEpochDay(),
            )
        },
    ) {
        DateRangePicker(
            state = state,
            title = null,
            modifier = Modifier.fillMaxWidth().height(height),
            colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskTimePickerDialog(
    initialMinuteOfDay: Int?,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    PostAnalytic(TasksEvents.TASKS_TIME_PICKER_SHEET_APPEAR)
    val context = LocalContext.current
    val initial = initialMinuteOfDay ?: (9 * 60)
    val state = rememberTimePickerState(
        initialHour = initial / 60,
        initialMinute = initial % 60,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    PickerSheet(
        title = stringResource(R.string.tasks_pick_time),
        confirmEnabled = true,
        onDismiss = onDismiss,
        onConfirm = { onPick(state.hour * 60 + state.minute) },
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(top = Spacing.md).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = state, colors = TimePickerDefaults.colors(clockDialColor = Orbit.extras.well))
        }
    }
}
