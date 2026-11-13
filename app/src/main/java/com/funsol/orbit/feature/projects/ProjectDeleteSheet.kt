package com.funsol.orbit.feature.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FolderOff
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.ConfirmDialog
import com.funsol.orbit.core.designsystem.DangerRed
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton

private const val CHOICE_KEEP = 0
private const val CHOICE_DELETE_ALL = 1

@Composable
fun DeleteProjectSheet(
    name: String,
    taskCount: Int,
    onKeepTasks: () -> Unit,
    onDeleteAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (taskCount == 0) {
        ConfirmDialog(
            title = stringResource(R.string.projects_delete_title, name),
            message = stringResource(R.string.projects_delete_simple_message, name),
            confirmLabel = stringResource(R.string.projects_delete_confirm),
            onConfirm = onKeepTasks,
            onDismiss = onDismiss,
            destructive = true,
        )
        return
    }
    var choice by rememberSaveable { mutableStateOf(CHOICE_KEEP) }
    val danger = Brush.linearGradient(listOf(DangerRed, DangerRed.lighten()))
    OrbitSheet(onDismiss) {
        Text(
            stringResource(R.string.projects_delete_title, name),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            pluralStringResource(R.plurals.projects_delete_linked, taskCount, name, taskCount),
            style = MaterialTheme.typography.bodyMedium,
            color = Orbit.extras.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            DeleteOption(
                selected = choice == CHOICE_KEEP,
                destructive = false,
                icon = Icons.Rounded.FolderOff,
                title = stringResource(R.string.projects_delete_keep),
                hint = stringResource(R.string.projects_delete_keep_hint),
                onSelect = { choice = CHOICE_KEEP },
            )
            DeleteOption(
                selected = choice == CHOICE_DELETE_ALL,
                destructive = true,
                icon = Icons.Rounded.DeleteForever,
                title = pluralStringResource(R.plurals.projects_delete_all, taskCount, taskCount),
                hint = pluralStringResource(R.plurals.projects_delete_all_consequence, taskCount, taskCount),
                onSelect = { choice = CHOICE_DELETE_ALL },
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WellButton(stringResource(R.string.action_cancel), onDismiss, Modifier.weight(1f))
            GradientButton(
                text = stringResource(R.string.projects_delete_confirm),
                onClick = { if (choice == CHOICE_DELETE_ALL) onDeleteAll() else onKeepTasks() },
                modifier = Modifier.weight(1f),
                brush = danger,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
private fun DeleteOption(
    selected: Boolean,
    destructive: Boolean,
    icon: ImageVector,
    title: String,
    hint: String,
    onSelect: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tone = if (destructive) scheme.error else scheme.primary
    val shape = RoundedCornerShape(16.dp)
    val fill = if (destructive) scheme.error.copy(alpha = if (selected) 0.16f else 0.08f) else Orbit.extras.well
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(if (selected) 1.5.dp else 0.8.dp, if (selected) tone else tone.copy(alpha = 0.25f), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm + Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (selected) tone else scheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(tone))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(icon, contentDescription = null, tint = tone, modifier = Modifier.size(18.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (destructive) scheme.error else scheme.onSurface)
            }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Orbit.extras.textMuted, modifier = Modifier.padding(start = 0.dp))
        }
    }
}
