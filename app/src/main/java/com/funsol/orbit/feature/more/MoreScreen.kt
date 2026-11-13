package com.funsol.orbit.feature.more

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.common.ClickGuard
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.GroupTitle
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitAmber
import com.funsol.orbit.core.designsystem.OrbitCoral
import com.funsol.orbit.core.designsystem.OrbitCyan
import com.funsol.orbit.core.designsystem.OrbitIndigo
import com.funsol.orbit.core.designsystem.OrbitVioletSoft
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.feature.onboarding.rememberLoop
import com.funsol.orbit.feature.onboarding.rememberPulse
import com.funsol.orbit.feature.premium.drawOrbitMark
import com.funsol.orbit.feature.settings.SettingsDivider
import com.funsol.orbit.feature.settings.SettingsRow
import com.funsol.orbit.feature.settings.staggeredEntrance

private class MoreTile(
    val icon: ImageVector,
    val accent: FeatureAccent,
    val title: Int,
    val desc: Int,
    val destination: AppDestination,
)

private val moreTiles = listOf(
    MoreTile(Icons.Rounded.Folder, FeatureAccent.PROJECTS, R.string.more_projects_title, R.string.more_projects_desc, AppDestination.Projects),
    MoreTile(Icons.AutoMirrored.Rounded.StickyNote2, FeatureAccent.NOTES, R.string.more_notes_title, R.string.more_notes_desc, AppDestination.Notes),
    MoreTile(Icons.Rounded.Loop, FeatureAccent.HABITS, R.string.more_habits_title, R.string.more_habits_desc, AppDestination.Habits),
    MoreTile(Icons.Rounded.Flag, FeatureAccent.GOALS, R.string.more_goals_title, R.string.more_goals_desc, AppDestination.Goals),
    MoreTile(Icons.Rounded.BarChart, FeatureAccent.ANALYTICS, R.string.more_analytics_title, R.string.more_analytics_desc, AppDestination.Analytics),
    MoreTile(Icons.Rounded.Search, FeatureAccent.HOME, R.string.more_search_title, R.string.more_search_desc, AppDestination.Search),
)

private val heroShape = RoundedCornerShape(22.dp)

@Composable
fun MoreScreen(nav: OrbitNavigator) {
    val guard = rememberClickGuard()
    val singleColumn = isLargeFontScale()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                BrandHero(Modifier.staggeredEntrance(0, withScale = true))
                GroupTitle(stringResource(R.string.more_section_organize), Modifier.staggeredEntrance(1))
                val onOpen: (AppDestination) -> Unit = { destination -> guard.run { nav.navigate(destination) } }
                if (singleColumn) {
                    moreTiles.forEachIndexed { i, tile ->
                        FeatureTile(tile, onClick = { onOpen(tile.destination) }, modifier = Modifier.fillMaxWidth().staggeredEntrance(2 + i))
                    }
                } else {
                    moreTiles.chunked(2).forEachIndexed { row, pair ->
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            pair.forEachIndexed { col, tile ->
                                FeatureTile(
                                    tile,
                                    onClick = { onOpen(tile.destination) },
                                    modifier = Modifier.weight(1f).fillMaxHeight().staggeredEntrance(2 + row * 2 + col, withScale = true),
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                GroupTitle(stringResource(R.string.more_section_app), Modifier.staggeredEntrance(9))
                AppCard(nav, guard, Modifier.staggeredEntrance(10))
            }
        }
    }
}

@Composable
private fun BrandHero(modifier: Modifier = Modifier) {
    val extras = Orbit.extras
    Box(
        modifier
            .fillMaxWidth()
            .orbitGlow(heroShape, extras.actionGlow.copy(alpha = 0.22f), radius = 14.dp, offsetY = 5.dp)
            .clip(heroShape)
            .background(extras.heroGradient)
            .heightIn(min = 112.dp),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val rtl = layoutDirection == LayoutDirection.Rtl
            val center = Offset(if (rtl) size.width * 0.06f else size.width * 0.94f, size.height * 0.12f)
            listOf(44.dp, 74.dp, 104.dp, 138.dp).forEachIndexed { i, r ->
                drawCircle(
                    Color.White.copy(alpha = 0.16f - i * 0.025f),
                    radius = r.toPx(),
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            }
            drawCircle(Color.White.copy(alpha = 0.55f), radius = 3.5.dp.toPx(), center = Offset(center.x + (if (rtl) 1 else -1) * 74.dp.toPx(), center.y + 3.dp.toPx()))
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AnimatedOrbitMark(Modifier.size(88.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(R.string.app_brand),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AnimatedOrbitMark(modifier: Modifier = Modifier) {
    val loop = rememberLoop(12_000)
    val pulse = rememberPulse(2_200)
    val bob = rememberPulse(2_800)
    Canvas(modifier.clearAndSetSemantics { }) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawOrbitMark(
            c, r, loop.value, pulse.value,
            orbitColor = Color.White.copy(alpha = 0.38f),
            glow = OrbitVioletSoft,
            glowAlpha = 0.45f,
            discColors = listOf(Color.White, Color(0xFFE4DEFF)),
            markColor = OrbitIndigo,
            satellite = OrbitAmber,
        )
        val dy = (bob.value - 0.5f) * 3.dp.toPx()
        drawCircle(OrbitCoral, r * 0.07f, Offset(c.x - r * 0.88f, c.y - r * 0.50f + dy))
        drawCircle(OrbitCyan, r * 0.06f, Offset(c.x + r * 0.88f, c.y - r * 0.50f - dy))
        drawCircle(OrbitAmber, r * 0.05f, Offset(c.x, c.y + r * 0.92f + dy))
    }
}

@Composable
private fun FeatureTile(tile: MoreTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accentColor = Orbit.extras.accent(tile.accent)
    OrbitCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        tint = accentColor,
        style = CardStyle.Tinted,
        elevation = CardElevation.Flat,
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 92.dp)) {
            Icon(
                tile.icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.10f),
                modifier = Modifier.align(Alignment.BottomEnd).size(72.dp).offset(x = 14.dp, y = 14.dp),
            )
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                IconTile(tile.icon, tile.accent, style = com.funsol.orbit.core.designsystem.IconTileStyle.Tinted)
                Box(Modifier.height(2.dp))
                Text(
                    stringResource(tile.title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(tile.desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Orbit.extras.textMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AppCard(nav: OrbitNavigator, guard: ClickGuard, modifier: Modifier = Modifier) {
    OrbitCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
        SettingsRow(
            icon = Icons.Rounded.Settings,
            accent = FeatureAccent.SETTINGS,
            title = stringResource(R.string.more_settings_title),
            subtitle = stringResource(R.string.more_settings_desc),
            onClick = { guard.run { nav.navigate(AppDestination.Settings) } },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Rounded.Backup,
            accent = FeatureAccent.BACKUP,
            title = stringResource(R.string.more_backup_title),
            subtitle = stringResource(R.string.more_backup_desc),
            onClick = { guard.run { nav.navigate(AppDestination.Backup) } },
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Rounded.PrivacyTip,
            accent = FeatureAccent.PRIVACY,
            title = stringResource(R.string.more_privacy_title),
            subtitle = stringResource(R.string.more_privacy_desc),
            onClick = { guard.run { nav.navigate(AppDestination.Privacy) } },
        )
    }
}
