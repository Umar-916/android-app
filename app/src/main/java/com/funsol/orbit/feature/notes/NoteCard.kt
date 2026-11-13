package com.funsol.orbit.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.NotesEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.TagPill

class NoteCardActions(
    val onOpen: (NoteItem) -> Unit,
    val onTogglePin: (NoteItem) -> Unit,
    val onToggleArchive: (NoteItem) -> Unit,
    val onMove: (NoteItem) -> Unit,
    val onDelete: (NoteItem) -> Unit,
)

private val NoteTints = listOf(
    FeatureAccent.NOTES,
    FeatureAccent.TASKS,
    FeatureAccent.PROJECTS,
    FeatureAccent.CALENDAR,
    FeatureAccent.HABITS,
    FeatureAccent.GOALS,
)

internal fun noteAccentFor(id: String): FeatureAccent = NoteTints[(id.hashCode() and Int.MAX_VALUE) % NoteTints.size]

@Composable
fun NoteCard(item: NoteItem, actions: NoteCardActions, modifier: Modifier = Modifier) {
    val untitled = stringResource(R.string.notes_untitled)
    val displayTitle = item.title.ifEmpty { untitled }
    val modified = remember(item.updatedAt) { DateFormats.dateTime(item.updatedAt) }
    val extras = Orbit.extras
    val accent = noteAccentFor(item.id)
    val tint = extras.accent(accent)
    var sheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    OrbitCard(
        onClick = { context.postAnalytic(NotesEvents.NOTES_CARD_CLICK); actions.onOpen(item) },
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        tint = tint,
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(tint.copy(alpha = if (extras.isDark) 0.08f else 0.06f))
                .padding(start = Spacing.md, top = Spacing.xs, end = Spacing.xs, bottom = Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.pinned) {
                    Box(
                        Modifier
                            .padding(end = Spacing.sm)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(extras.accentGradient(accent)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.notes_pinned),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(vertical = Spacing.sm),
                )
                IconButton(onClick = { context.postAnalytic(NotesEvents.NOTES_CARD_OPTIONS_CLICK); sheet = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.notes_card_options, displayTitle))
                }
            }
            Column(Modifier.padding(end = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (item.preview.isNotEmpty()) {
                    Text(
                        item.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    item.folderName?.let { name ->
                        TagPill(name, tint, Modifier.weight(1f, fill = false))
                    }
                    Text(
                        stringResource(R.string.notes_modified, modified),
                        style = MaterialTheme.typography.labelSmall,
                        color = extras.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    if (sheet) NoteActionsSheet(item, displayTitle, actions, onDismiss = { sheet = false })
}
