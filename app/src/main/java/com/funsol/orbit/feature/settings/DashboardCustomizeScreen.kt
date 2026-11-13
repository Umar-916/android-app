package com.funsol.orbit.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.MoreEvents
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.datastore.AppSettings
import com.funsol.orbit.core.datastore.HomeWidget
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.core.designsystem.orbitSwitchColors
import com.funsol.orbit.core.navigation.OrbitNavigator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

class DashboardCustomizeViewModel(private val store: SettingsStore) : ViewModel() {
    val settings: StateFlow<AppSettings> = store.settings
        .catch { emit(AppSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setVisible(widget: HomeWidget, visible: Boolean) {
        launchSafely { store.setWidgetVisible(widget, visible) }
    }

    fun reset() {
        launchSafely { store.resetWidgets() }
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
    }
}

@Composable
fun DashboardCustomizeScreen(nav: OrbitNavigator) {
    val viewModel: DashboardCustomizeViewModel = koinViewModel()
    val settings = viewModel.settings.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    PostAnalytic(MoreEvents.DASHBOARD_SCREEN_DISPLAY)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                stringResource(R.string.backup_customize_title),
                onBack = {
                    context.postAnalytic(MoreEvents.DASHBOARD_BACK_CLICK)
                    nav.back()
                },
                accent = FeatureAccent.HOME,
                icon = Icons.Rounded.Tune,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item(key = "intro", contentType = "intro") {
                    Text(
                        stringResource(R.string.backup_customize_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Orbit.extras.textMuted,
                        modifier = Modifier.padding(horizontal = Spacing.xs),
                    )
                }
                item(key = "widgets", contentType = "widgets") {
                    OrbitCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), tint = Orbit.extras.accent(FeatureAccent.HOME), style = CardStyle.Tinted) {
                        HomeWidget.entries.forEachIndexed { index, widget ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            WidgetRow(widget, settings.isVisible(widget)) {
                                context.postAnalytic(MoreEvents.DASHBOARD_WIDGET_TOGGLE)
                                viewModel.setVisible(widget, it)
                            }
                        }
                    }
                }
                item(key = "note", contentType = "note") {
                    Text(
                        stringResource(R.string.backup_customize_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Orbit.extras.textMuted,
                        modifier = Modifier.padding(horizontal = Spacing.xs),
                    )
                }
                item(key = "reset", contentType = "reset") {
                    WellButton(
                        stringResource(R.string.backup_customize_reset),
                        onClick = {
                            context.postAnalytic(MoreEvents.DASHBOARD_RESET_CLICK)
                            viewModel.reset()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = settings.hiddenWidgets.isNotEmpty(),
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(widget: HomeWidget, visible: Boolean, onChange: (Boolean) -> Unit) {
    val info = widgetInfo(widget)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = visible, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        IconTile(info.icon, info.accent, style = IconTileStyle.Tinted)
        Column(Modifier.weight(1f)) {
            Text(stringResource(info.name), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(info.description),
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
            )
        }
        Switch(checked = visible, onCheckedChange = null, colors = orbitSwitchColors())
    }
}

private data class WidgetInfo(val icon: ImageVector, val accent: FeatureAccent, val name: Int, val description: Int)

private fun widgetInfo(widget: HomeWidget): WidgetInfo = when (widget) {
    HomeWidget.PROGRESS -> WidgetInfo(Icons.Rounded.DonutLarge, FeatureAccent.HOME, R.string.backup_widget_progress, R.string.backup_widget_progress_desc)
    HomeWidget.TODAY -> WidgetInfo(Icons.Rounded.Today, FeatureAccent.TASKS, R.string.backup_widget_today, R.string.backup_widget_today_desc)
    HomeWidget.UPCOMING -> WidgetInfo(Icons.Rounded.Event, FeatureAccent.CALENDAR, R.string.backup_widget_upcoming, R.string.backup_widget_upcoming_desc)
    HomeWidget.OVERDUE -> WidgetInfo(Icons.Rounded.WarningAmber, FeatureAccent.GOALS, R.string.backup_widget_overdue, R.string.backup_widget_overdue_desc)
    HomeWidget.PROJECTS -> WidgetInfo(Icons.Rounded.Folder, FeatureAccent.PROJECTS, R.string.backup_widget_projects, R.string.backup_widget_projects_desc)
    HomeWidget.HABITS -> WidgetInfo(Icons.Rounded.Loop, FeatureAccent.HABITS, R.string.backup_widget_habits, R.string.backup_widget_habits_desc)
    HomeWidget.OVERVIEW -> WidgetInfo(Icons.Rounded.Insights, FeatureAccent.ANALYTICS, R.string.backup_widget_overview, R.string.backup_widget_overview_desc)
}
