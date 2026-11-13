package com.funsol.orbit.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.common.launchSafely
import com.funsol.orbit.core.datastore.AppSettings
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.datastore.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DialogState {
    data object None : DialogState
    data object Theme : DialogState
    data object RateUs : DialogState
}

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    val settings: StateFlow<AppSettings?> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _dialog = MutableStateFlow<DialogState>(DialogState.None)
    val dialog: StateFlow<DialogState> = _dialog.asStateFlow()

    fun showDialog(state: DialogState) {
        _dialog.update { state }
    }

    fun dismissDialog() {
        _dialog.update { DialogState.None }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launchSafely { store.setTheme(mode) }
        dismissDialog()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launchSafely { store.setNotificationsEnabled(enabled) }
    }
}
