package com.funsol.orbit.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.R
import com.funsol.orbit.core.ads.AdsController
import com.funsol.orbit.core.ads.NativeAdSlot
import com.funsol.orbit.core.ads.NativeAdStyle
import com.funsol.orbit.core.ads.NativePlacement
import com.funsol.orbit.core.ads.RectBannerAdSlot
import com.funsol.orbit.core.remote.LanguageAdType
import com.funsol.orbit.core.remote.RemoteConfigRepository
import org.koin.compose.koinInject
import com.funsol.orbit.core.common.AppLanguage
import com.funsol.orbit.core.common.LanguageCatalog
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GroupTitle
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Radius
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.core.startup.continueTo
import org.koin.compose.viewmodel.koinViewModel

private val LanguageRowMinHeight = 64.dp

@Composable
fun LanguageScreen(fromOnboarding: Boolean, nav: OrbitNavigator) {
    val vm: LanguageViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val guard = rememberClickGuard(1_000L)
    val selected = LanguageCatalog.byCode(state.selectedCode)
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(220)) }
    val ads = koinInject<AdsController>()
    LaunchedEffect(fromOnboarding) { if (fromOnboarding) ads.preloadNative(NativePlacement.ONBOARDING) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                title = stringResource(R.string.lang_screen_title),
                onBack = if (fromOnboarding) null else ({ nav.back() }),
                accent = FeatureAccent.SETTINGS,
                icon = Icons.Rounded.Language,
                actions = {
                    DonePill(
                        enabled = state.ready && !state.confirming,
                        onClick = {
                            guard.run {
                                vm.confirm(fromOnboarding) { next ->
                                    if (next != null) nav.continueTo(next) else nav.back()
                                }
                            }
                        },
                    )
                },
            )
        },
        bottomBar = { LanguageAdBar() },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().graphicsLayer { alpha = fade.value },
                contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "selected_title", contentType = "title") {
                    GroupTitle(stringResource(R.string.lang_selected_header))
                }
                item(key = "selected_card", contentType = "selected") {
                    LanguageRow(language = selected, selected = true, highlighted = true, onSelect = {})
                }
                item(key = "all_title", contentType = "title") {
                    GroupTitle(stringResource(R.string.lang_all_header), Modifier.padding(top = Spacing.sm))
                }
                items(LanguageCatalog.languages, key = { it.code }, contentType = { "language" }) { language ->
                    LanguageRow(
                        language = language,
                        selected = language.code == selected.code,
                        highlighted = false,
                        onSelect = { vm.select(language.code) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageAdBar() {
    val remote = koinInject<RemoteConfigRepository>()
    val config by remote.config.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        if (config.languageAdType == LanguageAdType.NATIVE) {
            NativeAdSlot(
                NativePlacement.LANGUAGE,
                modifier = Modifier.widthIn(max = 720.dp).padding(horizontal = Spacing.md),
                style = NativeAdStyle.Large,
                verticalPadding = Spacing.sm,
            )
        } else {
            RectBannerAdSlot()
        }
    }
}

@Composable
private fun DonePill(enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.button)
    Box(
        Modifier
            .padding(end = Spacing.sm)
            .minimumInteractiveComponentSize()
            .orbitGlow(shape, Orbit.extras.actionGlow, radius = 8.dp, offsetY = 3.dp)
            .clip(shape)
            .background(Orbit.extras.brandGradient)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.action_done), style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    highlighted: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(16.dp)
    val content = if (highlighted) Color.White else MaterialTheme.colorScheme.onSurface
    val selectedLabel = stringResource(R.string.lang_selected_state)
    val fill = when {
        highlighted -> extras.brandGradient
        selected -> extras.tintedBrush(MaterialTheme.colorScheme.primary, 0.10f)
        else -> extras.cardBrush
    }
    val border = when {
        highlighted -> BorderStroke(extras.cardStrokeWidth, Color.White.copy(alpha = 0.30f))
        selected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(extras.cardStrokeWidth, extras.cardStroke)
    }
    Row(
        modifier
            .fillMaxWidth()
            .then(if (highlighted) Modifier.orbitGlow(shape, extras.actionGlow, radius = 12.dp, offsetY = 4.dp) else Modifier)
            .clip(shape)
            .background(fill)
            .border(border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .semantics { if (selected) stateDescription = selectedLabel }
            .heightIn(min = LanguageRowMinHeight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (highlighted) Color.White.copy(alpha = 0.20f) else extras.well)
                .border(1.dp, if (highlighted) Color.White.copy(alpha = 0.55f) else extras.cardStroke, CircleShape)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text(language.flag, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            language.label,
            style = MaterialTheme.typography.titleSmall,
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        RadioMark(
            selected = selected,
            tint = if (highlighted) Color.White else MaterialTheme.colorScheme.primary,
            idle = if (highlighted) Color.White else extras.textMuted,
        )
    }
}

@Composable
private fun RadioMark(selected: Boolean, tint: Color, idle: Color) {
    Canvas(Modifier.size(22.dp).clearAndSetSemantics { }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val stroke = 1.8.dp.toPx()
        drawCircle(if (selected) tint else idle, radius = (size.minDimension - stroke) / 2f, center = c, style = Stroke(stroke))
        if (selected) drawCircle(tint, radius = size.minDimension * 0.24f, center = c)
    }
}
