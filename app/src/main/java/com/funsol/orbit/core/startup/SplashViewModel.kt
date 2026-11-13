package com.funsol.orbit.core.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.ads.SplashAdState
import com.funsol.orbit.core.ads.SplashAds
import com.funsol.orbit.core.navigation.AppDestination
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class SplashUiState(
    val progress: Float = 0f,
    val isLoadingComplete: Boolean = false,
    val error: Boolean = false,
    val showingAd: Boolean = false,
    val destination: AppDestination? = null,
)

class SplashViewModel(
    private val initializer: InitializationHelper,
    private val flow: OnboardingFlowHelper,
    private val ads: SplashAds,
    private val appScope: CoroutineScope,
    private val tickMs: Long = TICK_MS,
) : ViewModel() {
    private val _state = MutableStateFlow(SplashUiState())
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        start()
    }

    fun retry() {
        _state.value = SplashUiState()
        start()
    }

    private fun start() {
        job?.cancel()
        appScope.launch { initializer.initialize() }
        job = viewModelScope.launch {
            var elapsed = 0L
            while (true) {
                delay(tickMs)
                val init = initializer.state.value
                if (init is InitState.Failed) {
                    _state.update { it.copy(error = true) }
                    return@launch
                }
                if (!ads.consentFormShowing.value) elapsed += tickMs

                val inputs = SplashInputs(
                    elapsedMs = elapsed,
                    initDone = init is InitState.Completed,
                    online = ads.isOnline(),
                    premium = ads.isPremium(),
                    adState = ads.splashAdState.value,
                )
                val cap = ads.config.value.splashHardCapMs
                if (!ads.consentFormShowing.value && SplashProgressRules.isFinished(inputs, cap)) {
                    finish(inputs.adState)
                    return@launch
                }
                val progress = SplashProgressRules.progress(_state.value.progress, inputs, cap)
                _state.update { it.copy(progress = progress) }
            }
        }
    }

    private suspend fun finish(adState: SplashAdState) {
        _state.update { it.copy(progress = 1f) }
        if (adState == SplashAdState.LOADED && !ads.isPremium() && flow.includesSplashInterstitial()) {
            _state.update { it.copy(showingAd = true) }
            val done = CompletableDeferred<Unit>()
            ads.showSplashAd { done.complete(Unit) }
            withTimeoutOrNull(AD_SAFETY_TIMEOUT_MS) { done.await() }
            _state.update { it.copy(showingAd = false) }
        }
        val destination = flow.destinationAfter(null)
        _state.value = SplashUiState(progress = 1f, isLoadingComplete = true, destination = destination)
    }

    companion object {
        const val TICK_MS = 150L

        const val AD_SAFETY_TIMEOUT_MS = 90_000L
    }
}
