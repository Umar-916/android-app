package com.funsol.orbit.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.SettingsEvents
import com.funsol.orbit.core.analytics.postAnalytic
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.datastore.ThemeMode
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.core.designsystem.isLargeFontScale

private val optionShape = RoundedCornerShape(14.dp)

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Rounded.LightMode
    ThemeMode.DARK -> Icons.Rounded.DarkMode
}

private fun ThemeMode.accent(): FeatureAccent = when (this) {
    ThemeMode.SYSTEM -> FeatureAccent.PROJECTS
    ThemeMode.LIGHT -> FeatureAccent.NOTES
    ThemeMode.DARK -> FeatureAccent.TASKS
}

@Composable
fun ThemeDialog(current: ThemeMode, onApply: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    PostAnalytic(SettingsEvents.THEME_DIALOG_APPEAR)
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(current) }
    val guard = rememberClickGuard()
    val applyTheme: () -> Unit = {
        context.postAnalytic(SettingsEvents.THEME_APPLY_CLICK)
        guard.run { onApply(selected) }
    }
    val cancelTheme: () -> Unit = {
        context.postAnalytic(SettingsEvents.THEME_CANCEL_CLICK)
        onDismiss()
    }

    OrbitSheet(onDismiss = onDismiss) {
        Text(
            stringResource(R.string.settings_theme_dialog_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Spacer(Modifier.size(Spacing.md))
        Column(
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeOptionRow(mode = mode, isSelected = mode == selected, onSelect = {
                        context.postAnalytic(
                            when (mode) {
                                ThemeMode.SYSTEM -> SettingsEvents.THEME_SYSTEM_SELECT
                                ThemeMode.LIGHT -> SettingsEvents.THEME_LIGHT_SELECT
                                ThemeMode.DARK -> SettingsEvents.THEME_DARK_SELECT
                            },
                        )
                        selected = mode
                    })
            }
        }
        Spacer(Modifier.size(Spacing.md))
        val apply = stringResource(R.string.settings_apply)
        val cancel = stringResource(R.string.action_cancel)
        if (isLargeFontScale()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GradientButton(text = apply, onClick = applyTheme, modifier = Modifier.fillMaxWidth())
                WellButton(text = cancel, onClick = cancelTheme, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WellButton(text = cancel, onClick = cancelTheme, modifier = Modifier.weight(1f))
                GradientButton(
                    text = apply,
                    onClick = applyTheme,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.size(Spacing.xs))
    }
}

@Composable
private fun ThemeOptionRow(mode: ThemeMode, isSelected: Boolean, onSelect: () -> Unit) {
    OrbitWell(
        modifier = Modifier
            .fillMaxWidth()
            .clip(optionShape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect),
        selected = isSelected,
        shape = optionShape,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(mode.icon(), mode.accent(), size = 30.dp, style = com.funsol.orbit.core.designsystem.IconTileStyle.Soft)
            Text(
                themeLabel(mode),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            RadioMark(isSelected)
        }
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val ring = if (selected) primary else MaterialTheme.colorScheme.outline
    Canvas(Modifier.size(18.dp)) {
        val stroke = 1.6.dp.toPx()
        drawCircle(ring, radius = (size.minDimension - stroke) / 2, style = Stroke(width = stroke))
        if (selected) drawCircle(primary, radius = size.minDimension * 0.22f)
    }
}
