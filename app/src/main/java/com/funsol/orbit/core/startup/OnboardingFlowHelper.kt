package com.funsol.orbit.core.startup

import com.funsol.orbit.core.datastore.AppSettings
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.navigation.AppDestination
import com.funsol.orbit.core.navigation.OrbitNavigator
import com.funsol.orbit.core.remote.RemoteConfigRepository
import kotlinx.coroutines.flow.first

enum class FlowStepType { SPLASH_INTERSTITIAL, LANGUAGE, ONBOARDING, PREMIUM, HOME }

data class FlowStep(
    val type: FlowStepType,
    val enabled: Boolean = true,
    val premiumVariant: Int = 1,
)

data class FlowConfig(val steps: List<FlowStep>)

class OnboardingFlowHelper(
    private val store: SettingsStore,
    private val remote: RemoteConfigRepository,
) {
    suspend fun destinationAfter(current: FlowStepType?): AppDestination {
        val settings = store.settings.first()
        return next(currentConfig(settings.runCount), settings, current)
    }

    suspend fun includesSplashInterstitial(): Boolean {
        val runCount = store.settings.first().runCount
        return normalize(currentConfig(runCount)).any { it.type == FlowStepType.SPLASH_INTERSTITIAL }
    }

    private fun currentConfig(runCount: Int): FlowConfig =
        configFor(runCount, remote.config.value.flowJson(firstRun = runCount <= 1)).let { cfg ->
            if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) cfg
            else FlowConfig(cfg.steps.filter { it.type != FlowStepType.PREMIUM })
        }

    companion object {
        const val FIRST_RUN_PREMIUM_VARIANT = 1

        fun defaultConfigFor(runCount: Int): FlowConfig = if (runCount <= 1) {
            FlowConfig(
                listOf(
                    FlowStep(FlowStepType.SPLASH_INTERSTITIAL),
                    FlowStep(FlowStepType.LANGUAGE),
                    FlowStep(FlowStepType.ONBOARDING),
                    FlowStep(FlowStepType.PREMIUM, premiumVariant = FIRST_RUN_PREMIUM_VARIANT),
                    FlowStep(FlowStepType.HOME),
                ),
            )
        } else {
            FlowConfig(
                listOf(
                    FlowStep(FlowStepType.SPLASH_INTERSTITIAL),
                    FlowStep(FlowStepType.LANGUAGE),
                    FlowStep(FlowStepType.ONBOARDING),
                    FlowStep(FlowStepType.HOME),
                ),
            )
        }

        fun configFor(runCount: Int, json: String? = null): FlowConfig =
            FlowConfigParser.parse(json, defaultConfigFor(runCount))

        fun normalize(config: FlowConfig): List<FlowStep> =
            config.steps.filter { it.enabled }.distinctBy { it.type }

        fun isDone(step: FlowStep, settings: AppSettings): Boolean = when (step.type) {
            FlowStepType.SPLASH_INTERSTITIAL -> false
            FlowStepType.LANGUAGE -> settings.languageChosen
            FlowStepType.ONBOARDING -> settings.onboardingDone
            FlowStepType.PREMIUM -> settings.isPremium
            FlowStepType.HOME -> false
        }

        fun next(config: FlowConfig, settings: AppSettings, after: FlowStepType?): AppDestination {
            val steps = normalize(config)
            val position = steps.indexOfFirst { it.type == after }
            val remaining = when {
                after == null -> steps
                position >= 0 -> steps.drop(position + 1)
                else -> steps.filter { it.type.ordinal > after.ordinal }
            }
            val step = remaining.firstOrNull { candidate ->
                candidate.type != FlowStepType.SPLASH_INTERSTITIAL && !isDone(candidate, settings)
            }
            return when (step?.type) {
                FlowStepType.LANGUAGE -> AppDestination.Language(fromOnboarding = true)
                FlowStepType.ONBOARDING -> AppDestination.Onboarding(replay = false)
                FlowStepType.PREMIUM -> AppDestination.Premium(variant = step.premiumVariant, fromOnboarding = true)
                FlowStepType.HOME, FlowStepType.SPLASH_INTERSTITIAL, null -> AppDestination.Home
            }
        }
    }
}

fun OrbitNavigator.continueTo(destination: AppDestination) {
    if (destination == AppDestination.Home) enterHome() else replaceTop(destination)
}
