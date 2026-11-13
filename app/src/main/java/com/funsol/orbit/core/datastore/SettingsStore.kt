package com.funsol.orbit.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HomeWidget { PROGRESS, TODAY, UPCOMING, OVERDUE, PROJECTS, HABITS, OVERVIEW }

data class AppSettings(
    val languageCode: String = SettingsStore.DEFAULT_LANGUAGE,
    val languageChosen: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val hiddenWidgets: Set<HomeWidget> = emptySet(),
    val runCount: Int = 0,
    val isPremium: Boolean = false,
    val ratePromptDone: Boolean = false,
    val quickAddHintDismissed: Boolean = false,
) {
    fun isVisible(widget: HomeWidget) = widget !in hiddenWidgets
}

class SettingsStore(private val context: Context) {
    private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "orbit_settings")

    val settings: Flow<AppSettings> = context.store.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { p ->
            AppSettings(
                languageCode = p[LANGUAGE] ?: DEFAULT_LANGUAGE,
                languageChosen = p[LANGUAGE_CHOSEN] ?: false,
                themeMode = p[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
                onboardingDone = p[ONBOARDING_DONE] ?: false,
                notificationsEnabled = p[NOTIFICATIONS] ?: true,
                runCount = p[RUN_COUNT] ?: 0,
                isPremium = p[PREMIUM] ?: false,
                ratePromptDone = p[RATE_DONE] ?: false,
                quickAddHintDismissed = p[QUICK_ADD_HINT] ?: false,
                hiddenWidgets = p[HIDDEN_WIDGETS].orEmpty()
                    .mapNotNull { name -> HomeWidget.entries.firstOrNull { it.name == name } }
                    .toSet(),
            )
        }

    suspend fun setLanguage(code: String) = context.store.edit {
        it[LANGUAGE] = code
        it[LANGUAGE_CHOSEN] = true
    }

    suspend fun setTheme(mode: ThemeMode) = context.store.edit { it[THEME] = mode.name }

    suspend fun setOnboardingDone(done: Boolean) = context.store.edit { it[ONBOARDING_DONE] = done }

    suspend fun setNotificationsEnabled(enabled: Boolean) = context.store.edit { it[NOTIFICATIONS] = enabled }

    suspend fun setWidgetVisible(widget: HomeWidget, visible: Boolean) = context.store.edit { p ->
        val current = p[HIDDEN_WIDGETS].orEmpty().toMutableSet()
        if (visible) current.remove(widget.name) else current.add(widget.name)
        p[HIDDEN_WIDGETS] = current
    }

    suspend fun resetWidgets() = context.store.edit { it[HIDDEN_WIDGETS] = emptySet() }

    suspend fun incrementRunCount() = context.store.edit { it[RUN_COUNT] = (it[RUN_COUNT] ?: 0) + 1 }

    suspend fun setPremium(value: Boolean) = context.store.edit { it[PREMIUM] = value }

    suspend fun setRatePromptDone(value: Boolean) = context.store.edit { it[RATE_DONE] = value }

    suspend fun setQuickAddHintDismissed(value: Boolean) = context.store.edit { it[QUICK_ADD_HINT] = value }

    companion object {
        const val DEFAULT_LANGUAGE = "en"
        private val LANGUAGE = stringPreferencesKey("language")
        private val LANGUAGE_CHOSEN = booleanPreferencesKey("language_chosen")
        private val THEME = stringPreferencesKey("theme")
        private val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private val HIDDEN_WIDGETS = stringSetPreferencesKey("hidden_widgets")
        private val RUN_COUNT = intPreferencesKey("run_count")
        private val PREMIUM = booleanPreferencesKey("premium_user")
        private val RATE_DONE = booleanPreferencesKey("rate_prompt_done")
        private val QUICK_ADD_HINT = booleanPreferencesKey("quick_add_hint_dismissed")
    }
}
