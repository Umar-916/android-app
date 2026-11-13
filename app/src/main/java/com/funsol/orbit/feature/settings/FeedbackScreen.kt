package com.funsol.orbit.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.BuildConfig
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.SettingsEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.AppConfig
import com.funsol.orbit.core.common.composeEmail
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.designsystem.EmptyState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GradientButton
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.orbitChipColors
import com.funsol.orbit.core.navigation.OrbitNavigator
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedbackScreen(nav: OrbitNavigator) {
    PostAnalytic(SettingsEvents.FEEDBACK_SCREEN_DISPLAY)
    val vm: FeedbackViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val guard = rememberClickGuard()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val configured = AppConfig.FEEDBACK_EMAIL.isNotBlank()

    val limitMessage = stringResource(R.string.feedback_limit_reached, FeedbackRules.MAX_SELECTION)
    val noMailMessage = stringResource(R.string.feedback_no_mail_app)
    var handledLimitHits by rememberSaveable { mutableIntStateOf(state.limitHits) }
    LaunchedEffect(state.limitHits) {
        if (state.limitHits != handledLimitHits) {
            handledLimitHits = state.limitHits
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(limitMessage)
        }
    }

    fun send() {
        val itemLabels = state.items.filter { it in state.selected }.map { resources.getString(it.label) }
        val itemsLine = itemLabels.takeIf { it.isNotEmpty() }
            ?.let { resources.getString(R.string.feedback_body_topics, it.joinToString(", ")) }
        val footer = listOf(
            resources.getString(R.string.feedback_body_app, BuildConfig.VERSION_NAME),
            resources.getString(R.string.feedback_body_android, "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"),
            resources.getString(R.string.feedback_body_device, "${Build.MANUFACTURER} ${Build.MODEL}"),
        )
        val subject = resources.getString(
            if (state.tab == FeedbackTab.ISSUES) R.string.feedback_subject_issue else R.string.feedback_subject_suggestion,
        )
        val body = FeedbackRules.buildBody(state.text, itemsLine, footer)
        val opened = try {
            context.composeEmail(AppConfig.FEEDBACK_EMAIL, subject, body)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        if (!opened) scope.launch { snackbar.showSnackbar(noMailMessage) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                title = stringResource(R.string.feedback_title),
                onBack = { guard.run(nav::back) },
                backEvent = SettingsEvents.FEEDBACK_BACK_CLICK,
                accent = FeatureAccent.HABITS,
                icon = Icons.Rounded.Feedback,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (configured) {
                Box(
                    Modifier.fillMaxWidth().imePadding().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    GradientButton(
                        text = stringResource(R.string.feedback_submit),
                        onClick = {
                            context.postAnalytic(SettingsEvents.FEEDBACK_SUBMIT_CLICK)
                            guard.run(::send)
                        },
                        enabled = state.canSubmit,
                        icon = Icons.AutoMirrored.Rounded.Send,
                        modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (!configured) {
                EmptyState(
                    icon = Icons.Rounded.Feedback,
                    title = stringResource(R.string.feedback_unavailable_title),
                    message = stringResource(R.string.feedback_unavailable_message),
                    accent = FeatureAccent.HABITS,
                )
            } else {
                Column(
                    Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.section),
                ) {
                    FeedbackTabs(
                        selected = state.tab,
                        onSelect = { tab ->
                            context.postAnalytic(
                                if (tab == FeedbackTab.ISSUES) SettingsEvents.FEEDBACK_ISSUES_TAB_SELECT else SettingsEvents.FEEDBACK_SUGGESTIONS_TAB_SELECT,
                            )
                            vm.selectTab(tab)
                        },
                    )
                    DescriptionCard(tab = state.tab, text = state.text, onTextChange = vm::setText)
                    OrbitCard(Modifier.fillMaxWidth(), tint = Orbit.extras.accent(FeatureAccent.HABITS)) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                stringResource(R.string.feedback_select_label),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                stringResource(R.string.feedback_select_hint, FeedbackRules.MAX_SELECTION),
                                style = MaterialTheme.typography.bodySmall,
                                color = Orbit.extras.textMuted,
                            )
                            ItemGrid(
                                items = state.items,
                                selected = state.selected,
                                onToggle = { item ->
                                    context.postAnalytic(SettingsEvents.FEEDBACK_TOPIC_TOGGLE)
                                    vm.toggle(item)
                                },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.feedback_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Orbit.extras.textMuted,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackTabs(selected: FeedbackTab, onSelect: (FeedbackTab) -> Unit) {
    val shape = RoundedCornerShape(Radius.button)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Orbit.extras.well)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FeedbackTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .then(if (isSelected) Modifier.background(Orbit.extras.brandGradient) else Modifier)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(horizontal = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (tab == FeedbackTab.ISSUES) R.string.feedback_tab_issues else R.string.feedback_tab_suggestions),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DescriptionCard(tab: FeedbackTab, text: String, onTextChange: (String) -> Unit) {
    OrbitCard(Modifier.fillMaxWidth()) {
        Text(
            stringResource(if (tab == FeedbackTab.ISSUES) R.string.feedback_describe_issue else R.string.feedback_describe_idea),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(stringResource(R.string.feedback_hint), color = Orbit.extras.textMuted) },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(Radius.chip),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Orbit.extras.well,
                unfocusedContainerColor = Orbit.extras.well,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 125.dp),
        )
        Text(
            stringResource(R.string.feedback_counter, text.length, FeedbackRules.MAX_TEXT_LENGTH),
            style = MaterialTheme.typography.labelSmall,
            color = Orbit.extras.textMuted,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        )
    }
}

@Composable
private fun ItemGrid(items: List<FeedbackItem>, selected: Set<FeedbackItem>, onToggle: (FeedbackItem) -> Unit) {
    val perRow = if (isLargeFontScale()) 1 else 2
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(perRow).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item ->
                    val isSelected = item in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(item) },
                        label = {
                            Text(
                                stringResource(item.label),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        colors = orbitChipColors(),
                        shape = RoundedCornerShape(Radius.chip),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    )
                }
                if (rowItems.size < perRow) Box(Modifier.weight(1f))
            }
        }
    }
}
