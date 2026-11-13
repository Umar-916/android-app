package com.funsol.orbit.feature.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.launchSafely
import com.funsol.orbit.core.common.LanguageCatalog
import com.funsol.orbit.core.datastore.AppSettings
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.startup.FlowStepType
import com.funsol.orbit.core.startup.OnboardingFlowHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal fun selectedLanguageCode(settings: AppSettings?): String =
    if (settings != null && settings.languageChosen) settings.languageCode else LanguageCatalog.suggested().code

internal fun shouldApplyLocale(appliedTags: String, targetCode: String, suggestedCode: String): Boolean {
    val target = LanguageCatalog.byCode(targetCode).code
    return if (appliedTags.isBlank()) !target.equals(suggestedCode, ignoreCase = true) else !appliedTags.equals(target, ignoreCase = true)
}

data class LanguageUiState(
    val ready: Boolean = false,
    val selectedCode: String = LanguageCatalog.DEFAULT_CODE,
    val confirming: Boolean = false,
)

class LanguageViewModel(
    private val store: SettingsStore,
    private val flow: OnboardingFlowHelper,
) : ViewModel() {
    private val picked = MutableStateFlow<String?>(null)
    private val confirming = MutableStateFlow(false)

    val state: StateFlow<LanguageUiState> = combine(store.settings, picked, confirming) { settings, pick, busy ->
        LanguageUiState(ready = true, selectedCode = pick ?: selectedLanguageCode(settings), confirming = busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanguageUiState())

    fun select(code: String) {
        picked.value = LanguageCatalog.byCode(code).code
    }

    fun confirm(fromOnboarding: Boolean, onFinished: (AppDestination?) -> Unit) {
        if (confirming.value) return
        confirming.value = true
        viewModelScope.launchSafely(onError = { confirming.value = false }) {
            val settings = store.settings.first()
            val code = picked.value ?: selectedLanguageCode(settings)
            store.setLanguage(code)
            val next = if (fromOnboarding) flow.destinationAfter(FlowStepType.LANGUAGE) else null
            onFinished(next)
            val applied = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (shouldApplyLocale(applied, code, LanguageCatalog.suggested().code)) LanguageCatalog.apply(code)
            confirming.value = false
        }
    }
}
