package com.funsol.orbit.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.backup.BackupParseResult
import com.funsol.orbit.core.backup.BackupSummary
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.designsystem.DangerRed
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitCoral
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.SuccessGreen
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate

private val RESTORE_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")

@Composable
fun BackupScreen(nav: OrbitNavigator) {
    val viewModel: BackupViewModel = koinViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val busy = state.busy != null
    val guard = rememberClickGuard()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportBackup)
    }
    val tasksCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportTasksCsv)
    }
    val habitsCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportHabitsCsv)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::onRestoreFilePicked)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                stringResource(R.string.backup_title),
                onBack = nav::back,
                accent = FeatureAccent.BACKUP,
                icon = Icons.Rounded.Save,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md)
                    .padding(top = Spacing.xs, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                InfoHero()

                AnimatedVisibility(visible = busy) { BusyCard(state.busy) }
                AnimatedVisibility(visible = state.message != null) {
                    state.message?.let { ResultCard(it, onDismiss = viewModel::dismissMessage) }
                }

                ActionCard(
                    icon = Icons.Rounded.FileDownload,
                    accent = FeatureAccent.BACKUP,
                    title = stringResource(R.string.backup_export_title),
                    description = stringResource(R.string.backup_export_desc),
                ) {
                    GradientButton(
                        stringResource(R.string.backup_export_button),
                        onClick = { guard.run { tryCatch { exportLauncher.launch(BackupIo.backupFileName(LocalDate.now())) } } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        icon = Icons.Rounded.FileDownload,
                        brush = Orbit.extras.accentGradient(FeatureAccent.BACKUP),
                    )
                }

                ActionCard(
                    icon = Icons.Rounded.FileUpload,
                    accent = FeatureAccent.PROJECTS,
                    title = stringResource(R.string.backup_restore_title),
                    description = stringResource(R.string.backup_restore_desc),
                ) {
                    GradientButton(
                        stringResource(R.string.backup_restore_button),
                        onClick = { guard.run { tryCatch { restoreLauncher.launch(RESTORE_MIME_TYPES) } } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        icon = Icons.Rounded.FileUpload,
                        brush = Orbit.extras.accentGradient(FeatureAccent.PROJECTS),
                    )
                }

                ActionCard(
                    icon = Icons.Rounded.TableChart,
                    accent = FeatureAccent.HABITS,
                    title = stringResource(R.string.backup_csv_title),
                    description = stringResource(R.string.backup_csv_desc),
                ) {
                    WellButton(
                        stringResource(R.string.backup_csv_tasks),
                        onClick = { guard.run { tryCatch { tasksCsvLauncher.launch(BackupIo.tasksCsvFileName(LocalDate.now())) } } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    )
                    WellButton(
                        stringResource(R.string.backup_csv_habits),
                        onClick = { guard.run { tryCatch { habitsCsvLauncher.launch(BackupIo.habitsCsvFileName(LocalDate.now())) } } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    )
                }
            }
        }
    }

    state.pendingRestore?.let { summary ->
        RestoreConfirmSheet(summary, onConfirm = viewModel::confirmRestore, onDismiss = viewModel::cancelRestore)
    }
}

@Composable
private fun InfoHero() {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .orbitGlow(shape, extras.accent(FeatureAccent.BACKUP).copy(alpha = 0.30f), radius = 16.dp, offsetY = 6.dp)
            .clip(shape)
            .background(extras.accentGradient(FeatureAccent.BACKUP)),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val rtl = layoutDirection == LayoutDirection.Rtl
            val c = Offset(if (rtl) size.width * 0.05f else size.width * 0.95f, size.height * 0.1f)
            listOf(56.dp to 0.18f, 96.dp to 0.12f, 136.dp to 0.08f).forEach { (r, a) ->
                drawCircle(Color.White.copy(alpha = a), radius = r.toPx(), center = c, style = Stroke(width = 1.2.dp.toPx()))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(Spacing.md + Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.backup_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.backup_info_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    accent: FeatureAccent,
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    OrbitCard(
        Modifier.fillMaxWidth().animateContentSize(),
        tint = Orbit.extras.accent(accent),
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)) {
            IconTile(icon, accent, style = IconTileStyle.Tinted)
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = Orbit.extras.textMuted,
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) { content() }
    }
}

@Composable
private fun BusyCard(action: BackupAction?) {
    val label = stringResource(if (action == BackupAction.RESTORE) R.string.backup_working_restore else R.string.backup_working_export)
    OrbitCard(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            Modifier.fillMaxWidth().padding(top = Spacing.sm),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ResultCard(message: BackupMessage, onDismiss: () -> Unit) {
    val ok = message is BackupMessage.Success
    val tint = if (ok) SuccessGreen else MaterialTheme.colorScheme.error
    OrbitCard(
        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        accent = tint,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Spacing.md, top = Spacing.sm, bottom = Spacing.sm, end = Spacing.sm,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, contentDescription = null, tint = tint)
            Text(messageText(message), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    }
}

@Composable
private fun messageText(message: BackupMessage): String = stringResource(
    when (message) {
        is BackupMessage.Success -> when (message.action) {
            BackupAction.EXPORT -> R.string.backup_success_export
            BackupAction.EXPORT_TASKS_CSV, BackupAction.EXPORT_HABITS_CSV -> R.string.backup_success_csv
            BackupAction.RESTORE -> R.string.backup_success_restore
        }
        is BackupMessage.Failure -> when (message.action) {
            BackupAction.RESTORE -> R.string.backup_failed_restore
            else -> R.string.backup_failed_export
        }
        is BackupMessage.Invalid -> when (message.reason) {
            BackupParseResult.Reason.NOT_A_BACKUP -> R.string.backup_invalid_not_backup
            BackupParseResult.Reason.UNSUPPORTED_VERSION -> R.string.backup_invalid_version
            BackupParseResult.Reason.CORRUPT -> R.string.backup_invalid_corrupt
            BackupParseResult.Reason.INCONSISTENT -> R.string.backup_invalid_inconsistent
        }
        BackupMessage.TooLarge -> R.string.backup_invalid_too_large
    },
)

@Composable
private fun RestoreConfirmSheet(summary: BackupSummary, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    OrbitSheet(onDismiss) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(R.string.backup_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            if (summary.exportedAt > 0) {
                Text(
                    stringResource(R.string.backup_confirm_exported, DateFormats.dateTime(summary.exportedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                stringResource(R.string.backup_confirm_contains),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = Spacing.xs),
            )
            val counts = listOf(
                stringResource(R.string.backup_confirm_tasks, summary.tasks),
                stringResource(R.string.backup_confirm_projects, summary.projects),
                stringResource(R.string.backup_confirm_notes, summary.notes),
                stringResource(R.string.backup_confirm_habits, summary.habits),
                stringResource(R.string.backup_confirm_goals, summary.goals),
            )
            if (isLargeFontScale()) {
                counts.forEach { CountWell(it, Modifier.fillMaxWidth()) }
            } else {
                counts.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        pair.forEach { CountWell(it, Modifier.weight(1f)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs)
                    .clip(RoundedCornerShape(16.dp))
                    .background(error.copy(alpha = 0.12f))
                    .padding(Spacing.sm + Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = error)
                Text(stringResource(R.string.backup_confirm_warning), style = MaterialTheme.typography.bodyMedium, color = error)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        if (isLargeFontScale()) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GradientButton(
                    stringResource(R.string.backup_confirm_action),
                    onConfirm,
                    Modifier.fillMaxWidth(),
                    brush = Brush.linearGradient(listOf(DangerRed, OrbitCoral)),
                )
                WellButton(stringResource(R.string.action_cancel), onDismiss, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WellButton(stringResource(R.string.action_cancel), onDismiss, Modifier.weight(1f))
                GradientButton(
                    stringResource(R.string.backup_confirm_action),
                    onConfirm,
                    Modifier.weight(1f),
                    brush = Brush.linearGradient(listOf(DangerRed, OrbitCoral)),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

@Composable
private fun CountWell(text: String, modifier: Modifier = Modifier) {
    OrbitWell(modifier) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.sm + Spacing.xs),
        )
    }
}
