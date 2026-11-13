package com.funsol.orbit.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.activity.compose.LocalActivity
import com.funsol.orbit.core.consent.ConsentManager
import org.koin.compose.koinInject
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.BuildConfig
import com.funsol.orbit.R
import com.funsol.orbit.core.analytics.PostAnalytic
import com.funsol.orbit.core.analytics.SettingsEvents
import com.funsol.orbit.core.analytics.postAnalytic
import com.funsol.orbit.core.common.AppConfig
import com.funsol.orbit.core.common.LanguageCatalog
import com.funsol.orbit.core.common.rememberClickGuard
import com.funsol.orbit.core.common.shareAppLink
import com.funsol.orbit.core.datastore.ThemeMode
import com.funsol.orbit.core.designsystem.FeatureAccent
import com.funsol.orbit.core.designsystem.OrbitTopBar
import com.funsol.orbit.core.designsystem.Spacing
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.feature.premium.PremiumUpgradeCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(nav: OrbitNavigator) {
    PostAnalytic(SettingsEvents.SETTINGS_SCREEN_DISPLAY)
    val vm: SettingsViewModel = koinViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val dialog by vm.dialog.collectAsStateWithLifecycle()
    val guard = rememberClickGuard()
    val context = LocalContext.current
    val feedbackAvailable = AppConfig.FEEDBACK_EMAIL.isNotBlank()
    val consent = koinInject<ConsentManager>()
    val privacyOptionsRequired by consent.privacyOptionsRequired.collectAsStateWithLifecycle()
    val hostActivity = LocalActivity.current

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            OrbitTopBar(
                title = stringResource(R.string.settings_title),
                onBack = { guard.run(nav::back) },
                backEvent = SettingsEvents.SETTINGS_BACK_CLICK,
                accent = FeatureAccent.SETTINGS,
                scrolled = scrollState.value > 0,
                icon = Icons.Rounded.Settings,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            val current = settings
            if (current != null) {
                Column(
                    Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    var index = 0
                    if (!current.isPremium && com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) {
                        PremiumUpgradeCard(
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_PREMIUM_CARD_CLICK); guard.run { nav.navigate(AppDestination.Premium(variant = nextPremiumVariant(), fromOnboarding = false)) } },
                            modifier = Modifier.fillMaxWidth().staggeredEntrance(index++),
                        )
                    }

                    SettingsGroup(stringResource(R.string.settings_group_preferences), Modifier.staggeredEntrance(index++)) {
                        SettingsRow(
                            icon = Icons.Rounded.Language,
                            accent = FeatureAccent.PROJECTS,
                            title = stringResource(R.string.settings_language),
                            value = LanguageCatalog.byCode(current.languageCode).nativeName,
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_LANGUAGE_CLICK); guard.run { nav.navigate(AppDestination.Language(fromOnboarding = false)) } },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Palette,
                            accent = FeatureAccent.TASKS,
                            title = stringResource(R.string.settings_theme),
                            value = themeLabel(current.themeMode),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_THEME_CLICK); guard.run { vm.showDialog(DialogState.Theme) } },
                        )
                        SettingsDivider()
                        NotificationRows(
                            enabled = current.notificationsEnabled,
                            onEnabledChange = vm::setNotificationsEnabled,
                            onGuardedClick = { action -> guard.run(action) },
                        )
                    }

                    SettingsGroup(stringResource(R.string.settings_group_support), Modifier.staggeredEntrance(index++)) {
                        SettingsRow(
                            icon = Icons.Rounded.Star,
                            accent = FeatureAccent.NOTES,
                            title = stringResource(R.string.settings_rate),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_RATE_US_CLICK); guard.run { vm.showDialog(DialogState.RateUs) } },
                        )
                        if (feedbackAvailable) {
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Rounded.Feedback,
                                accent = FeatureAccent.HABITS,
                                title = stringResource(R.string.settings_feedback),
                                onClick = { context.postAnalytic(SettingsEvents.SETTINGS_FEEDBACK_CLICK); guard.run { nav.navigate(AppDestination.Feedback) } },
                            )
                        }
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Share,
                            accent = FeatureAccent.ANALYTICS,
                            title = stringResource(R.string.settings_share),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_SHARE_APP_CLICK); guard.run { context.shareAppLink() } },
                        )
                    }

                    SettingsGroup(stringResource(R.string.settings_group_data), Modifier.staggeredEntrance(index++)) {
                        SettingsRow(
                            icon = Icons.Rounded.Backup,
                            accent = FeatureAccent.BACKUP,
                            title = stringResource(R.string.settings_backup),
                            subtitle = stringResource(R.string.settings_backup_desc),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_BACKUP_CLICK); guard.run { nav.navigate(AppDestination.Backup) } },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Rounded.PrivacyTip,
                            accent = FeatureAccent.PRIVACY,
                            title = stringResource(R.string.settings_privacy),
                            subtitle = stringResource(R.string.settings_privacy_desc),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_PRIVACY_CLICK); guard.run { nav.navigate(AppDestination.Privacy) } },
                        )
                        if (privacyOptionsRequired && hostActivity != null) {
                            SettingsDivider()
                            SettingsRow(
                                icon = Icons.Rounded.Tune,
                                accent = FeatureAccent.PRIVACY,
                                title = stringResource(R.string.ads_privacy_options_title),
                                subtitle = stringResource(R.string.ads_privacy_options_subtitle),
                                onClick = { context.postAnalytic(SettingsEvents.SETTINGS_PRIVACY_OPTIONS_CLICK); guard.run { consent.showPrivacyOptions(hostActivity) } },
                            )
                        }
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Dashboard,
                            accent = FeatureAccent.HOME,
                            title = stringResource(R.string.settings_customize_dashboard),
                            subtitle = stringResource(R.string.settings_customize_dashboard_desc),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_CUSTOMIZE_DASHBOARD_CLICK); guard.run { nav.navigate(AppDestination.DashboardCustomize) } },
                        )
                    }

                    SettingsGroup(stringResource(R.string.settings_group_about), Modifier.staggeredEntrance(index++)) {
                        SettingsRow(
                            icon = Icons.Rounded.Replay,
                            accent = FeatureAccent.GOALS,
                            title = stringResource(R.string.settings_replay),
                            subtitle = stringResource(R.string.settings_replay_desc),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_REPLAY_ONBOARDING_CLICK); guard.run { nav.navigate(AppDestination.Onboarding(replay = true)) } },
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Info,
                            accent = FeatureAccent.SETTINGS,
                            title = stringResource(R.string.settings_about),
                            value = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                            onClick = { context.postAnalytic(SettingsEvents.SETTINGS_ABOUT_CLICK); guard.run { nav.navigate(AppDestination.About) } },
                        )
                    }
                }
            }
        }
    }

    when (dialog) {
        DialogState.None -> Unit
        DialogState.Theme -> ThemeDialog(
            current = settings?.themeMode ?: ThemeMode.SYSTEM,
            onApply = vm::setTheme,
            onDismiss = vm::dismissDialog,
        )
        DialogState.RateUs -> RateUsDialog(
            onDismiss = vm::dismissDialog,
            onOpenFeedback = if (feedbackAvailable) {
                { nav.navigate(AppDestination.Feedback) }
            } else {
                null
            },
        )
    }
}

@Composable
internal fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)

private var debugPremiumVariant = 0

private fun nextPremiumVariant(): Int {
    if (!com.funsol.orbit.BuildConfig.DEBUG) return 1
    debugPremiumVariant = debugPremiumVariant % 3 + 1
    return debugPremiumVariant
}
