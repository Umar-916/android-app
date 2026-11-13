package com.funsol.orbit.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.BuildConfig
import com.funsol.orbit.R
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.SectionHeader
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.TagPill
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(nav: OrbitNavigator) {
    val extras = Orbit.extras
    val guard = rememberClickGuard()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                stringResource(R.string.about_title),
                onBack = nav::back,
                accent = FeatureAccent.SETTINGS,
                icon = Icons.Rounded.Info,
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
                BrandHero()

                OrbitCard(Modifier.fillMaxWidth(), tint = extras.accent(FeatureAccent.SETTINGS), style = CardStyle.Tinted, elevation = CardElevation.Flat) {
                    Text(stringResource(if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) R.string.about_description else R.string.about_description_offline), style = MaterialTheme.typography.bodyMedium)
                }

                OrbitCard(Modifier.fillMaxWidth()) {
                    SectionHeader(stringResource(R.string.about_made_with))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        listOf(
                            R.string.about_tech_kotlin to FeatureAccent.BACKUP,
                            R.string.about_tech_compose to FeatureAccent.PROJECTS,
                            R.string.about_tech_room to FeatureAccent.HABITS,
                            R.string.about_tech_workmanager to FeatureAccent.CALENDAR,
                        ).forEach { (res, accent) ->
                            TagPill(stringResource(res), extras.accent(accent))
                        }
                    }
                }

                OrbitCard(
                    onClick = { guard.run { nav.navigate(AppDestination.Privacy) } },
                    tint = extras.accent(FeatureAccent.PRIVACY),
                    style = CardStyle.Tinted,
                    elevation = CardElevation.Flat,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = Spacing.touchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        IconTile(Icons.Rounded.Lock, FeatureAccent.PRIVACY, style = IconTileStyle.Tinted)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.about_privacy_title), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.about_privacy_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = extras.textMuted,
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = extras.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHero() {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .orbitGlow(shape, extras.actionGlow.copy(alpha = 0.28f), radius = 18.dp, offsetY = 8.dp)
            .clip(shape)
            .background(extras.heroGradient),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val rtl = layoutDirection == LayoutDirection.Rtl
            val c = Offset(if (rtl) size.width * 0.04f else size.width * 0.96f, size.height * 0.04f)
            listOf(70.dp to 0.16f, 112.dp to 0.11f, 156.dp to 0.07f).forEach { (r, a) ->
                drawCircle(Color.White.copy(alpha = a), radius = r.toPx(), center = c, style = Stroke(width = 1.2.dp.toPx()))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg, horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(72.dp)) {
                    val c = Offset(size.width / 2, size.height / 2)
                    val r = size.minDimension * 0.36f
                    drawCircle(Color.White, r, c, style = Stroke(width = size.minDimension * 0.06f))
                    drawCircle(Color.White.copy(alpha = 0.85f), r * 0.45f, c)
                    drawCircle(Color.White, r * 0.2f, Offset(c.x + r, c.y))
                }
            }
            Text(
                stringResource(R.string.app_brand),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs + Spacing.xxs),
            )
        }
    }
}
