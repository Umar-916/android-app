package com.funsol.orbit.core.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.AnalyticsEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitSheet
import com.funsol.orbit.core.designsystem.Spacing

private data class QuickAction(
    val label: Int,
    val icon: ImageVector,
    val accent: FeatureAccent,
    val destination: AppDestination,
    val event: String,
)

private val ACTIONS = listOf(
    QuickAction(R.string.dv3_new_task, Icons.Rounded.TaskAlt, FeatureAccent.TASKS, AppDestination.TaskEdit(), AnalyticsEvents.QUICK_ADD_TASK_CLICK),
    QuickAction(R.string.dv3_new_note, Icons.AutoMirrored.Rounded.StickyNote2, FeatureAccent.NOTES, AppDestination.NoteEdit(), AnalyticsEvents.QUICK_ADD_NOTE_CLICK),
    QuickAction(R.string.dv3_new_habit, Icons.Rounded.LocalFireDepartment, FeatureAccent.HABITS, AppDestination.HabitEdit(), AnalyticsEvents.QUICK_ADD_HABIT_CLICK),
    QuickAction(R.string.dv3_new_project, Icons.Rounded.FolderCopy, FeatureAccent.PROJECTS, AppDestination.ProjectEdit(), AnalyticsEvents.QUICK_ADD_PROJECT_CLICK),
    QuickAction(R.string.dv3_new_goal, Icons.Rounded.Flag, FeatureAccent.GOALS, AppDestination.GoalEdit(), AnalyticsEvents.QUICK_ADD_GOAL_CLICK),
    QuickAction(R.string.dv3_search, Icons.Rounded.Search, FeatureAccent.ANALYTICS, AppDestination.Search, AnalyticsEvents.QUICK_ADD_SEARCH_CLICK),
)

@Composable
fun QuickAddSheet(nav: OrbitNavigator, onDismiss: () -> Unit) {
    PostAnalytic(AnalyticsEvents.QUICK_ADD_SHEET_APPEAR)
    val context = LocalContext.current
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.72f).dp
    OrbitSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                stringResource(R.string.dv3_quick_add),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
            )
            Text(
                stringResource(R.string.dv3_quick_add_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
            )
            ACTIONS.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { action ->
                        QuickTile(action, Modifier.weight(1f)) {
                            context.postAnalytic(action.event)
                            onDismiss()
                            nav.navigate(action.destination)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(action: QuickAction, modifier: Modifier, onClick: () -> Unit) {
    val extras = Orbit.extras
    val accent = extras.accent(action.accent)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .heightIn(min = 88.dp)
            .clip(shape)
            .background(extras.tintedBrush(accent, 0.10f))
            .border(0.8.dp, accent.copy(alpha = 0.24f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        IconTile(action.icon, action.accent, size = 40.dp)
        Text(
            stringResource(action.label),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
