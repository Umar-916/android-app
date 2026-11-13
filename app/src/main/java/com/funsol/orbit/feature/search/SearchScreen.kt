package com.funsol.orbit.feature.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.SearchEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.DateFormats
import com.funsol.orbit.core.common.GoalMath
import com.funsol.orbit.core.designsystem.ErrorState
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.OrbitWell
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(nav: OrbitNavigator) {
    PostAnalytic(SearchEvents.SEARCH_SCREEN_DISPLAY)
    val context = LocalContext.current
    val viewModel: SearchViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf(viewModel.currentQuery) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(Unit) { if (viewModel.currentQuery != text) viewModel.onQueryChange(text) }

    val open: (AppDestination) -> Unit = { destination ->
        keyboard?.hide()
        nav.navigate(destination)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                OrbitTopBar(
                    stringResource(R.string.action_search),
                    onBack = {
                        context.postAnalytic(SearchEvents.SEARCH_BACK_CLICK)
                        keyboard?.hide()
                        nav.back()
                    },
                    accent = FeatureAccent.HOME,
                    icon = Icons.Rounded.Search,
                )
                SearchField(
                    text = text,
                    focusRequester = focusRequester,
                    onChange = {
                        text = it
                        viewModel.onQueryChange(it)
                    },
                    onClear = {
                        context.postAnalytic(SearchEvents.SEARCH_CLEAR_CLICK)
                        text = ""
                        viewModel.onQueryChange("")
                        focusRequester.requestFocus()
                    },
                    onSearch = {
                        context.postAnalytic(SearchEvents.SEARCH_SUBMIT_CLICK)
                        keyboard?.hide()
                    },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                when (state.phase) {
                    SearchPhase.IDLE -> SearchIdle(
                        suggestions = suggestions,
                        onPick = { picked ->
                            context.postAnalytic(SearchEvents.SEARCH_SUGGESTION_CLICK)
                            text = picked
                            viewModel.onQueryChange(picked)
                        },
                    )
                    SearchPhase.LOADING -> LinearProgressIndicator(
                        Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    SearchPhase.EMPTY -> SearchNoResults(state.query.trim())
                    SearchPhase.ERROR -> ErrorState(stringResource(R.string.error_generic), onRetry = {
                        context.postAnalytic(SearchEvents.SEARCH_RETRY_CLICK)
                        viewModel.retry()
                    })
                    SearchPhase.RESULTS -> ResultsList(state, open)
                }
            }
        }
    }
}

@Composable
private fun SearchIllustration(modifier: Modifier = Modifier, noResults: Boolean = false) {
    val extras = Orbit.extras
    val accent = extras.accent(FeatureAccent.HOME)
    val partner = FeatureAccent.HOME.to
    val amber = extras.amber
    Canvas(modifier.size(120.dp).clearAndSetSemantics { }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(accent.copy(alpha = 0.06f), radius = r * 0.98f, center = c)
        drawCircle(accent.copy(alpha = 0.10f), radius = r * 0.74f, center = c)
        drawCircle(accent.copy(alpha = 0.16f), radius = r * 0.54f, center = c)
        drawCircle(
            color = accent.copy(alpha = 0.32f),
            radius = r * 0.90f,
            center = c,
            style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 7.dp.toPx()))),
        )
        val sat = r * 0.90f * 0.7071f
        drawCircle(partner, radius = 4.dp.toPx(), center = Offset(c.x + sat, c.y - sat))
        drawCircle(amber, radius = 3.dp.toPx(), center = Offset(c.x - sat, c.y + sat))

        val lensR = r * 0.25f
        val lens = Offset(c.x - r * 0.12f, c.y - r * 0.12f)
        drawCircle(Color.White.copy(alpha = 0.55f), radius = lensR, center = lens)
        val handleStart = Offset(lens.x + lensR * 0.72f, lens.y + lensR * 0.72f)
        drawLine(
            brush = Brush.linearGradient(listOf(accent, partner), start = handleStart, end = handleStart + Offset(r * 0.32f, r * 0.32f)),
            start = handleStart,
            end = handleStart + Offset(r * 0.30f, r * 0.30f),
            strokeWidth = r * 0.10f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            brush = Brush.linearGradient(listOf(accent, partner), start = lens - Offset(lensR, lensR), end = lens + Offset(lensR, lensR)),
            radius = lensR,
            center = lens,
            style = Stroke(width = r * 0.075f),
        )
        drawArc(
            color = Color.White.copy(alpha = 0.85f),
            startAngle = 195f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = Offset(lens.x - lensR * 0.62f, lens.y - lensR * 0.62f),
            size = Size(lensR * 1.24f, lensR * 1.24f),
            style = Stroke(width = r * 0.035f, cap = StrokeCap.Round),
        )
        if (noResults) {
            val d = lensR * 0.34f
            val stroke = r * 0.05f
            drawLine(accent, Offset(lens.x - d, lens.y - d), Offset(lens.x + d, lens.y + d), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(accent, Offset(lens.x + d, lens.y - d), Offset(lens.x - d, lens.y + d), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchIdle(suggestions: SearchSuggestionsState, onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SearchIllustration()
        val hasChips = suggestions.items.isNotEmpty()
        Text(
            stringResource(if (hasChips || !suggestions.loaded) R.string.search_idle_title else R.string.search_edu_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        if (hasChips) {
            Text(
                stringResource(R.string.search_idle_message),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.search_try_title),
                style = MaterialTheme.typography.labelLarge,
                color = Orbit.extras.sectionTitle,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                suggestions.items.forEach { suggestion ->
                    SuggestionChip(suggestion) { onPick(suggestion.text) }
                }
            }
        } else if (suggestions.loaded) {
            Text(
                stringResource(R.string.search_edu_message),
                style = MaterialTheme.typography.bodyMedium,
                color = Orbit.extras.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
    }
}

@Composable
private fun SuggestionChip(suggestion: SearchSuggestion, onClick: () -> Unit) {
    val accent = accentOf(suggestion.type)
    val accentColor = Orbit.extras.accent(accent)
    val shape = RoundedCornerShape(24.dp)
    val description = stringResource(R.string.search_suggestion_desc, suggestion.text)
    Row(
        Modifier
            .widthIn(max = 280.dp)
            .heightIn(min = Spacing.touchTarget)
            .clip(shape)
            .background(Orbit.extras.tinted(accentColor, 0.12f))
            .border(0.8.dp, accentColor.copy(alpha = 0.30f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(iconOf(suggestion.type), contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Text(
            suggestion.text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun SearchNoResults(query: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SearchIllustration(noResults = true)
        Text(
            stringResource(R.string.search_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Text(
            stringResource(R.string.search_empty_message, query),
            style = MaterialTheme.typography.bodyMedium,
            color = Orbit.extras.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Text(
            stringResource(R.string.search_empty_tip),
            style = MaterialTheme.typography.bodySmall,
            color = Orbit.extras.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

@Composable
private fun SearchField(
    text: String,
    focusRequester: FocusRequester,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
) {
    OrbitWell(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        TextField(
            value = text,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).focusRequester(focusRequester),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.action_clear))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
    }
}

private fun accentOf(type: SearchSectionType): FeatureAccent = when (type) {
    SearchSectionType.TASKS -> FeatureAccent.TASKS
    SearchSectionType.PROJECTS -> FeatureAccent.PROJECTS
    SearchSectionType.NOTES -> FeatureAccent.NOTES
    SearchSectionType.HABITS -> FeatureAccent.HABITS
    SearchSectionType.GOALS -> FeatureAccent.GOALS
}

private fun iconOf(type: SearchSectionType): ImageVector = when (type) {
    SearchSectionType.TASKS -> Icons.Rounded.CheckBox
    SearchSectionType.PROJECTS -> Icons.Rounded.Folder
    SearchSectionType.NOTES -> Icons.Rounded.StickyNote2
    SearchSectionType.HABITS -> Icons.Rounded.Loop
    SearchSectionType.GOALS -> Icons.Rounded.EmojiEvents
}

@Composable
private fun ResultsList(state: SearchUiState, open: (AppDestination) -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        state.sections.forEach { section ->
            val accent = accentOf(section.type)
            val icon = iconOf(section.type)
            item(key = "h_${section.type}", contentType = "header") {
                SectionTitle(section, icon, accent)
            }
            when (section.type) {
                SearchSectionType.TASKS -> rows(section.results.tasks.map { it.id }, "t", section.results.tasks.size) { i ->
                    val t = section.results.tasks[i]
                    val due = t.dueDate?.let { DateFormats.date(it) }
                    ResultRow(accent, t.title, due) { context.postAnalytic(SearchEvents.SEARCH_RESULT_TASK_CLICK); open(AppDestination.TaskEdit(taskId = t.id)) }
                }
                SearchSectionType.PROJECTS -> rows(section.results.projects.map { it.id }, "p", section.results.projects.size) { i ->
                    val p = section.results.projects[i]
                    ResultRow(accent, p.name, p.description.firstLineOrNull()) { context.postAnalytic(SearchEvents.SEARCH_RESULT_PROJECT_CLICK); open(AppDestination.ProjectDetail(p.id)) }
                }
                SearchSectionType.NOTES -> rows(section.results.notes.map { it.id }, "n", section.results.notes.size) { i ->
                    val n = section.results.notes[i]
                    val title = n.title.ifBlank { stringResource(R.string.search_untitled_note) }
                    ResultRow(accent, title, n.body.firstLineOrNull()) { context.postAnalytic(SearchEvents.SEARCH_RESULT_NOTE_CLICK); open(AppDestination.NoteEdit(noteId = n.id)) }
                }
                SearchSectionType.HABITS -> rows(section.results.habits.map { it.id }, "h", section.results.habits.size) { i ->
                    val h = section.results.habits[i]
                    ResultRow(accent, h.name, h.description.firstLineOrNull()) { context.postAnalytic(SearchEvents.SEARCH_RESULT_HABIT_CLICK); open(AppDestination.HabitDetail(h.id)) }
                }
                SearchSectionType.GOALS -> rows(section.results.goals.map { it.id }, "g", section.results.goals.size) { i ->
                    val g = section.results.goals[i]
                    val pct = stringResource(R.string.search_goal_percent, GoalMath.percent(g.currentValue, g.targetValue))
                    ResultRow(accent, g.title, pct) { context.postAnalytic(SearchEvents.SEARCH_RESULT_GOAL_CLICK); open(AppDestination.GoalEdit(goalId = g.id)) }
                }
            }
        }
    }
}

private fun LazyListScope.rows(
    ids: List<String>,
    prefix: String,
    count: Int,
    content: @Composable (Int) -> Unit,
) {
    items(count, key = { "${prefix}_${ids[it]}" }, contentType = { prefix }) { index -> content(index) }
}

@Composable
private fun SectionTitle(section: SearchSection, icon: ImageVector, accent: FeatureAccent) {
    val label = pluralStringResource(
        when (section.type) {
            SearchSectionType.TASKS -> R.plurals.search_section_tasks
            SearchSectionType.PROJECTS -> R.plurals.search_section_projects
            SearchSectionType.NOTES -> R.plurals.search_section_notes
            SearchSectionType.HABITS -> R.plurals.search_section_habits
            SearchSectionType.GOALS -> R.plurals.search_section_goals
        },
        section.count,
        section.count,
    )
    Row(
        Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        IconTile(icon, accent, size = 30.dp)
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = Orbit.extras.accentText(accent),
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun ResultRow(accent: FeatureAccent, title: String, subtitle: String?, onClick: () -> Unit) {
    OrbitCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm + Spacing.xs),
        accent = Orbit.extras.accent(accent),
    ) {
        Column(Modifier.heightIn(min = 32.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Orbit.extras.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun String.firstLineOrNull(): String? = lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
