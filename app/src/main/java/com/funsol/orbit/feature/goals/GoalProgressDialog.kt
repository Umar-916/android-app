package com.funsol.orbit.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.analytics.GoalsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.GoalMath
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.database.entity.GoalEntity
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitProgressBar
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.SuccessGreen
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.feature.projects.PlanningTextField

@Composable
fun numberErrorText(error: NumberError): String = stringResource(
    when (error) {
        NumberError.EMPTY -> R.string.goals_error_number_empty
        NumberError.INVALID -> R.string.goals_error_number_invalid
        NumberError.NEGATIVE -> R.string.goals_error_number_negative
        NumberError.ZERO -> R.string.goals_error_number_zero
        NumberError.TOO_LARGE -> R.string.goals_error_number_too_large
    },
)

@Composable
fun GoalProgressDialog(goal: GoalEntity, onSubmit: (Double) -> Unit, onDismiss: () -> Unit) {
    PostAnalytic(GoalsEvents.GOALS_PROGRESS_SHEET_APPEAR)
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf(ProgressMode.SET) }
    var text by rememberSaveable { mutableStateOf(GoalNumbers.toEditText(goal.currentValue)) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val guard = rememberClickGuard()

    val result = GoalNumbers.resolveProgress(mode, text, goal.currentValue)
    val error = (result as? NumberParse.Error)?.error?.takeIf { text.isNotBlank() || attempted }
    val newTotal = (result as? NumberParse.Ok)?.value
    val previewValue = newTotal ?: goal.currentValue
    val previewFraction = GoalMath.fraction(previewValue, goal.targetValue)
    val previewPercent = GoalMath.percent(previewValue, goal.targetValue)
    val reaches = newTotal != null && GoalMath.isReached(newTotal, goal.targetValue)
    val accent = Orbit.extras.accent(FeatureAccent.GOALS)

    OrbitSheet(onDismiss) {
        Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
            ) {
                IconTile(Icons.Rounded.Flag, FeatureAccent.GOALS)
                Text(
                    stringResource(R.string.goals_update_progress_title, goal.title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ModeWell(
                    selected = mode == ProgressMode.SET,
                    icon = Icons.Rounded.Edit,
                    label = stringResource(R.string.goals_mode_set),
                    modifier = Modifier.weight(1f),
                ) {
                    context.postAnalytic(GoalsEvents.GOALS_PROGRESS_MODE_SET_SELECT)
                    if (mode != ProgressMode.SET) {
                        mode = ProgressMode.SET
                        text = GoalNumbers.toEditText(goal.currentValue)
                        attempted = false
                    }
                }
                ModeWell(
                    selected = mode == ProgressMode.ADD,
                    icon = Icons.Rounded.Add,
                    label = stringResource(R.string.goals_mode_add),
                    modifier = Modifier.weight(1f),
                ) {
                    context.postAnalytic(GoalsEvents.GOALS_PROGRESS_MODE_ADD_SELECT)
                    if (mode != ProgressMode.ADD) {
                        mode = ProgressMode.ADD
                        text = ""
                        attempted = false
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            PlanningTextField(
                value = text,
                onValueChange = { text = it.take(32) },
                label = stringResource(if (mode == ProgressMode.SET) R.string.goals_field_new_value else R.string.goals_field_increment),
                suffix = goal.unit.takeIf { it.isNotBlank() },
                errorText = error?.let { numberErrorText(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(Spacing.md))
            OrbitWell(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.goals_preview_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = Orbit.extras.textMuted,
                            )
                            GoalNumbersRow(previewValue, goal)
                        }
                        Text(
                            stringResource(R.string.goals_percent, previewPercent),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (reaches) SuccessGreen else accent,
                        )
                    }
                    OrbitProgressBar(previewFraction, color = if (reaches) SuccessGreen else accent, height = 10.dp)
                    if (reaches) {
                        Text(
                            stringResource(R.string.goals_preview_completes),
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WellButton(
                stringResource(R.string.action_cancel),
                {
                    context.postAnalytic(GoalsEvents.GOALS_PROGRESS_CANCEL_CLICK)
                    onDismiss()
                },
                Modifier.weight(1f),
            )
            GradientButton(
                text = stringResource(R.string.goals_update_confirm),
                onClick = {
                    context.postAnalytic(GoalsEvents.GOALS_PROGRESS_CONFIRM_CLICK)
                    attempted = true
                    (result as? NumberParse.Ok)?.let { ok -> guard.run { onSubmit(ok.value) } }
                },
                modifier = Modifier.weight(1f),
                brush = Orbit.extras.accentGradient(FeatureAccent.GOALS),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
private fun ModeWell(selected: Boolean, icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    OrbitWell(
        modifier.heightIn(min = Spacing.touchTarget).semantics { this.selected = selected; role = Role.RadioButton },
        onClick = onClick,
        selected = selected,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.sm, vertical = Spacing.sm + Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Orbit.extras.textMuted,
                modifier = Modifier.size(18.dp),
            )
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
