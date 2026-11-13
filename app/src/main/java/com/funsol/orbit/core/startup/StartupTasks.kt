package com.funsol.orbit.core.startup

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.funsol.orbit.core.ads.AdsStartup
import com.funsol.orbit.core.analytics.AnalyticsLogger
import com.funsol.orbit.core.remote.FirebaseBootstrap
import com.funsol.orbit.core.common.LanguageCatalog
import com.funsol.orbit.core.database.OrbitDatabase
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal const val CONSENT_STEP_TIMEOUT_MS = 5 * 60_000L
internal const val FIREBASE_STEP_TIMEOUT_MS = 3_000L
internal const val NETWORK_STEP_TIMEOUT_MS = 1_000L
internal const val REMOTE_CONFIG_STEP_TIMEOUT_MS = 8_000L
internal const val MOBILE_ADS_STEP_TIMEOUT_MS = 8_000L
internal const val PRELOAD_STEP_TIMEOUT_MS = 3_000L

internal fun buildStartupTasks(
    appContext: Context,
    store: SettingsStore,
    database: OrbitDatabase,
    reminders: ReminderScheduler,
    ads: AdsStartup,
    flow: OnboardingFlowHelper,
): List<StartupTask> {
    val runCounted = AtomicBoolean(false)
    return listOf(
        StartupTask(InitStep.LOAD_SETTINGS, critical = false) {
            store.settings.first()
            if (runCounted.compareAndSet(false, true)) store.incrementRunCount()
        },
        StartupTask(InitStep.WARM_DATABASE, critical = true) {
            withContext(Dispatchers.IO) { database.openHelper.writableDatabase.version }
        },
        StartupTask(InitStep.RECONCILE_REMINDERS, critical = false) {
            reminders.reconcileAll()
        },
        StartupTask(InitStep.LOCALE_READY, critical = false) {
            val settings = store.settings.first()
            val applied = AppCompatDelegate.getApplicationLocales()
            if (settings.languageChosen && applied.isEmpty && settings.languageCode != LanguageCatalog.suggested().code) {
                withContext(Dispatchers.Main) { LanguageCatalog.apply(settings.languageCode) }
            }
        },
        // Firebase first (before network/consent/remote config/ads), same order as the other Funsol apps.
        // Both calls are idempotent: FirebaseApp is only created when none exists and Analytics initialises once.
        StartupTask(InitStep.FIREBASE, critical = false, timeoutMs = FIREBASE_STEP_TIMEOUT_MS) {
            FirebaseBootstrap.initializeDefault(appContext)
            AnalyticsLogger.init(appContext)
        },
        StartupTask(InitStep.CHECK_NETWORK, critical = false, timeoutMs = NETWORK_STEP_TIMEOUT_MS) {
            ads.isOnline()
        },
        StartupTask(InitStep.CONSENT, critical = false, timeoutMs = CONSENT_STEP_TIMEOUT_MS) {
            ads.gatherConsent()
        },
        StartupTask(InitStep.REMOTE_CONFIG, critical = false, timeoutMs = REMOTE_CONFIG_STEP_TIMEOUT_MS) {
            ads.fetchRemoteConfig()
        },
        StartupTask(InitStep.MOBILE_ADS, critical = false, timeoutMs = MOBILE_ADS_STEP_TIMEOUT_MS) {
            ads.initializeSdk()
        },
        StartupTask(InitStep.PRELOAD_ADS, critical = false, timeoutMs = PRELOAD_STEP_TIMEOUT_MS) {
            ads.preload(splashInterstitialInFlow = flow.includesSplashInterstitial())
        },
    )
}
