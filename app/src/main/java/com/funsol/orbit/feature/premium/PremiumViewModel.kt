package com.funsol.orbit.feature.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.startup.FlowStepType
import com.funsol.orbit.core.startup.OnboardingFlowHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PremiumUiState(
    val billingAvailable: Boolean = false,
    val price: String? = null,
    val isPurchased: Boolean = false,
    val isPremium: Boolean = false,
    val offlineNotice: Boolean = false,
    val trialAvailable: Boolean = false,
    val trialEnabled: Boolean = true,
) {
    val trialOn: Boolean get() = canPurchase && trialAvailable && trialEnabled

    val canPurchase: Boolean get() = billingAvailable && price != null && !isPurchased && !isPremium
}

class PremiumViewModel(
    private val store: SettingsStore,
    private val billing: BillingProvider,
    private val flow: OnboardingFlowHelper,
    private val connectivity: ConnectivityProvider = AlwaysOnlineConnectivity,
) : ViewModel() {
    private val offlineNotice = MutableStateFlow(false)
    private val trialChoice = MutableStateFlow(true)

    val state: StateFlow<PremiumUiState> = combine(
        billing.price,
        billing.isPurchased,
        store.settings,
        offlineNotice,
        trialChoice,
    ) { price, purchased, settings, offline, trial ->
        PremiumUiState(
            billingAvailable = billing.isAvailable,
            price = price?.takeIf { it.isNotBlank() },
            isPurchased = purchased,
            isPremium = isPremiumUser(settings),
            offlineNotice = offline,
            trialAvailable = billing.supportsTrial,
            trialEnabled = trial,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PremiumUiState(billingAvailable = billing.isAvailable),
    )

    init {
        if (billing.isAvailable) billing.restore()
        if (billing.supportsTrial) billing.selectTrial(trialChoice.value)
    }

    fun toggleTrial() {
        if (!billing.supportsTrial) return
        val next = !trialChoice.value
        trialChoice.value = next
        billing.selectTrial(next)
    }

    fun restore() {
        if (billing.isAvailable) billing.restore()
    }

    fun purchase(activity: Activity) {
        if (!state.value.canPurchase) return
        if (!connectivity.isOnline()) {
            viewModelScope.launch {
                offlineNotice.value = true
                delay(OFFLINE_NOTICE_MS)
                offlineNotice.value = false
            }
            return
        }
        billing.purchase(activity)
    }

    fun confirmPurchase(onDone: () -> Unit) {
        viewModelScope.launch {
            store.setPremium(true)
            onDone()
        }
    }

    fun close(fromOnboarding: Boolean, onFinished: (AppDestination?) -> Unit) {
        viewModelScope.launch {
            onFinished(if (fromOnboarding) flow.destinationAfter(FlowStepType.PREMIUM) else null)
        }
    }

    private companion object {
        const val OFFLINE_NOTICE_MS = 3_500L
    }
}
