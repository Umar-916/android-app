package com.funsol.orbit.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.toEpochDayInt
import com.funsol.orbit.core.common.toLocalDate
import com.funsol.orbit.core.database.entity.ProjectStatus
import com.funsol.orbit.core.designsystem.ConfirmDialog
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.SuccessGreen
import com.funsol.orbit.core.designsystem.WarningAmber
import com.funsol.orbit.core.designsystem.WellButton
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun CenteredContent(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) { content() }
    }
}

@Composable
fun OrbitDateField(
    label: String,
    epochDay: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val hasError = errorText != null
    val valueText = epochDay?.let { DateFormats.date(it) } ?: stringResource(R.string.planning_date_not_set)
    val tint = if (hasError) scheme.error else scheme.primary
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OrbitWell(
                Modifier.weight(1f).heightIn(min = 64.dp),
                onClick = { showPicker = true },
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.sm)
                        .semantics(mergeDescendants = true) { contentDescription = "$label, $valueText" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                ) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = tint) }
                    Column(Modifier.weight(1f)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasError) scheme.error else Orbit.extras.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            valueText,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (epochDay != null) scheme.onSurface else Orbit.extras.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (epochDay != null) {
                IconButton(onClick = { onChange(null) }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.planning_clear_date_named, label))
                }
            }
        }
        if (errorText != null) {
            Text(errorText, style = MaterialTheme.typography.bodySmall, color = scheme.error, modifier = Modifier.padding(start = Spacing.sm))
        }
    }
    if (showPicker) {
        OrbitDatePickerDialog(
            initialEpochDay = epochDay,
            onConfirm = { onChange(it); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitDatePickerDialog(initialEpochDay: Int?, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialEpochDay?.let { it.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
    )
    OrbitSheet(onDismiss) {
        Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DatePicker(
                    state = state,
                    title = null,
                    colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
        Spacer(Modifier.size(Spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WellButton(stringResource(R.string.action_cancel), onDismiss, Modifier.weight(1f))
            GradientButton(
                text = stringResource(R.string.action_done),
                onClick = {
                    state.selectedDateMillis?.let { ms ->
                        onConfirm(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toEpochDayInt())
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = state.selectedDateMillis != null,
            )
        }
        Spacer(Modifier.size(Spacing.xs))
    }
}

@Composable
fun DiscardChangesDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    ConfirmDialog(
        title = stringResource(R.string.planning_discard_title),
        message = stringResource(R.string.planning_discard_message),
        confirmLabel = stringResource(R.string.action_discard),
        onConfirm = onDiscard,
        onDismiss = onKeepEditing,
        destructive = true,
        dismissLabel = stringResource(R.string.action_keep_editing),
    )
}

@Composable
fun projectStatusLabel(status: ProjectStatus): String = stringResource(
    when (status) {
        ProjectStatus.ACTIVE -> R.string.status_active
        ProjectStatus.ON_HOLD -> R.string.status_on_hold
        ProjectStatus.COMPLETED -> R.string.status_completed
        ProjectStatus.ARCHIVED -> R.string.status_archived
    },
)

@Composable
fun projectStatusColor(status: ProjectStatus): Color = when (status) {
    ProjectStatus.ACTIVE -> SuccessGreen
    ProjectStatus.ON_HOLD -> WarningAmber
    ProjectStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    ProjectStatus.ARCHIVED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun dateRangeText(start: Int?, end: Int?): String? = when {
    start != null && end != null -> stringResource(R.string.planning_date_range, DateFormats.date(start), DateFormats.date(end))
    start != null -> stringResource(R.string.planning_date_from, DateFormats.date(start))
    end != null -> stringResource(R.string.planning_date_until, DateFormats.date(end))
    else -> null
}
