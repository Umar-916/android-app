package com.funsol.orbit.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.funsol.orbit.R
import com.funsol.orbit.core.designsystem.CardElevation
import com.funsol.orbit.core.designsystem.CardStyle
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.orbitGlow
import com.funsol.orbit.core.navigation.OrbitNavigator

private data class PrivacySection(val icon: ImageVector, val title: Int, val body: Int)

private val SECTIONS = buildList {
    add(PrivacySection(Icons.Rounded.PhoneAndroid, R.string.privacy_stored_title, R.string.privacy_stored_body))
    add(PrivacySection(Icons.Rounded.PersonOff, R.string.privacy_account_title, R.string.privacy_account_body))
    if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) {
        add(PrivacySection(Icons.Rounded.VisibilityOff, R.string.privacy_tracking_title, R.string.privacy_tracking_body))
        add(PrivacySection(Icons.Rounded.WifiOff, R.string.privacy_network_title, R.string.privacy_network_body))
    }
}

private val SECTIONS_AFTER_PERMISSIONS = listOf(
    PrivacySection(Icons.Rounded.Save, R.string.privacy_backups_title, R.string.privacy_backups_body),
    PrivacySection(Icons.Rounded.CloudOff, R.string.privacy_cloud_title, R.string.privacy_cloud_body),
    PrivacySection(Icons.Rounded.DeleteOutline, R.string.privacy_delete_title, R.string.privacy_delete_body),
)

@Composable
fun PrivacyScreen(nav: OrbitNavigator) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                stringResource(R.string.privacy_title),
                onBack = nav::back,
                accent = FeatureAccent.PRIVACY,
                icon = Icons.Rounded.Lock,
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
                PrivacyHero()
                SECTIONS.forEachIndexed { i, s -> SectionCard(s, tinted = i % 2 == 0) }
                PermissionsCard()
                SECTIONS_AFTER_PERMISSIONS.forEachIndexed { i, s -> SectionCard(s, tinted = i % 2 == 1) }
            }
        }
    }
}

@Composable
private fun PrivacyHero() {
    val extras = Orbit.extras
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .orbitGlow(shape, extras.accent(FeatureAccent.PRIVACY).copy(alpha = 0.30f), radius = 16.dp, offsetY = 6.dp)
            .clip(shape)
            .background(extras.accentGradient(FeatureAccent.PRIVACY)),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val rtl = layoutDirection == LayoutDirection.Rtl
            val c = Offset(if (rtl) size.width * 0.05f else size.width * 0.95f, size.height * 0.1f)
            listOf(60.dp to 0.18f, 100.dp to 0.12f, 140.dp to 0.08f).forEach { (r, a) ->
                drawCircle(Color.White.copy(alpha = a), radius = r.toPx(), center = c, style = Stroke(width = 1.2.dp.toPx()))
            }
        }
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Text(
                stringResource(R.string.privacy_headline),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) R.string.privacy_intro else R.string.privacy_intro_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun SectionCard(section: PrivacySection, tinted: Boolean) {
    OrbitCard(
        Modifier.fillMaxWidth(),
        tint = Orbit.extras.accent(FeatureAccent.PRIVACY).takeIf { tinted },
        style = if (tinted) CardStyle.Tinted else CardStyle.Soft,
        elevation = if (tinted) CardElevation.Flat else CardElevation.Soft,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
            IconTile(section.icon, FeatureAccent.PRIVACY, style = IconTileStyle.Tinted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    stringResource(section.title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(section.body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                )
            }
        }
    }
}

@Composable
private fun PermissionsCard() {
    OrbitCard(Modifier.fillMaxWidth(), accent = Orbit.extras.accent(FeatureAccent.PRIVACY), tint = Orbit.extras.accent(FeatureAccent.PRIVACY), style = CardStyle.Tinted, elevation = CardElevation.Lifted) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
            IconTile(Icons.Rounded.Notifications, FeatureAccent.PRIVACY, style = IconTileStyle.Tinted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    stringResource(R.string.privacy_permissions_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) R.string.privacy_permissions_intro else R.string.privacy_permissions_intro_offline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                )
                PermissionItem(Icons.Rounded.Notifications, R.string.privacy_perm_notifications_title, R.string.privacy_perm_notifications_body)
                PermissionItem(Icons.Rounded.Restore, R.string.privacy_perm_boot_title, R.string.privacy_perm_boot_body)
            }
        }
    }
}

@Composable
private fun PermissionItem(icon: ImageVector, title: Int, body: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Orbit.extras.well)
            .padding(Spacing.sm + Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Orbit.extras.accent(FeatureAccent.PRIVACY))
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        }
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = Orbit.extras.textMuted)
    }
}
