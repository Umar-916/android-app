package com.funsol.orbit.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.funsol.orbit.R
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.GroupTitle
import com.funsol.orbit.core.designsystem.IconTile
import com.funsol.orbit.core.designsystem.IconTileStyle
import com.funsol.orbit.core.designsystem.Orbit
import com.funsol.orbit.core.designsystem.OrbitCard
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.designsystem.WellButton
import com.funsol.orbit.core.designsystem.isLargeFontScale
import com.funsol.orbit.core.designsystem.orbitSwitchColors
import com.funsol.orbit.core.notifications.NotificationHelper
import kotlinx.coroutines.delay

@Composable
internal fun Modifier.staggeredEntrance(index: Int, withScale: Boolean = false): Modifier {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val progress = remember { Animatable(if (animationsOff) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!animationsOff) {
            delay(index.coerceAtMost(4) * 30L)
            progress.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        }
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 10.dp.toPx()
        if (withScale) {
            val s = 0.97f + 0.03f * p
            scaleX = s
            scaleY = s
        }
    }
}

@Composable
internal fun SettingsGroup(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        GroupTitle(title)
        OrbitCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs, horizontal = 0.dp)) {
            content()
        }
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 14.dp + 36.dp + 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        thickness = 0.5.dp,
    )
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    accent: FeatureAccent,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
) {
    val valueBelow = value != null && isLargeFontScale()
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, accent, size = 36.dp, style = IconTileStyle.Soft)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Orbit.extras.textMuted)
            }
            if (valueBelow && value != null) {
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null && !valueBelow) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 128.dp),
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Orbit.extras.textMuted.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun NotificationRows(enabled: Boolean, onEnabledChange: (Boolean) -> Unit, onGuardedClick: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    var systemAllowed by remember { mutableStateOf(NotificationHelper.canPost(context)) }
    var denied by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { systemAllowed = NotificationHelper.canPost(context) }

    fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tryCatch { context.startActivity(intent) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        systemAllowed = NotificationHelper.canPost(context)
        denied = !granted
        if (granted) onEnabledChange(true)
    }

    fun onToggle(checked: Boolean) {
        when {
            !checked -> onEnabledChange(false)
            NotificationHelper.canPost(context) -> {
                denied = false
                onEnabledChange(true)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> {
                onEnabledChange(true)
                openSystemSettings()
            }
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = enabled, role = Role.Switch) { checked -> onGuardedClick { onToggle(checked) } }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(Icons.Rounded.Notifications, FeatureAccent.NOTES, size = 36.dp, style = IconTileStyle.Soft)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_reminders),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                stringResource(R.string.settings_reminders_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Orbit.extras.textMuted,
            )
        }
        Switch(checked = enabled, onCheckedChange = null, colors = orbitSwitchColors())
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            stringResource(if (systemAllowed) R.string.settings_notif_allowed else R.string.settings_notif_blocked),
            style = MaterialTheme.typography.bodyMedium,
            color = if (systemAllowed) Orbit.extras.textMuted else MaterialTheme.colorScheme.error,
        )
        if (!systemAllowed) {
            if (denied) {
                Text(
                    stringResource(R.string.settings_notif_denied_explain),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Orbit.extras.textMuted,
                )
            }
            WellButton(
                text = stringResource(R.string.settings_notif_open),
                onClick = { onGuardedClick(::openSystemSettings) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            stringResource(R.string.settings_reminders_note),
            style = MaterialTheme.typography.bodySmall,
            color = Orbit.extras.textMuted,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
    }
}
