package com.funsol.orbit.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.startup.FlowStepType
import com.funsol.orbit.core.startup.OnboardingFlowHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val store: SettingsStore,
    private val flow: OnboardingFlowHelper,
) : ViewModel() {
    private var finishing = false

    fun finish(replay: Boolean, onFinished: (AppDestination?) -> Unit) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            try {
                store.setOnboardingDone(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            val destination: AppDestination? = if (replay) {
                null
            } else {
                try {
                    flow.destinationAfter(FlowStepType.ONBOARDING)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    AppDestination.Home
                }
            }
            onFinished(destination)
        }
    }
}
