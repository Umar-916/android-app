package com.funsol.orbit.core.startup

import com.funsol.orbit.core.datastore.AppSettings
import com.funsol.orbit.core.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFlowHelperTest {
    private val fresh = AppSettings()
    private fun next(runCount: Int, settings: AppSettings, after: FlowStepType?) =
        OnboardingFlowHelper.next(OnboardingFlowHelper.configFor(runCount), settings, after)

    @Test
    fun firstRun_walksLanguageOnboardingPremiumHome() {
        assertEquals(AppDestination.Language(fromOnboarding = true), next(1, fresh, null))
        assertEquals(AppDestination.Onboarding(replay = false), next(1, fresh.copy(languageChosen = true), FlowStepType.LANGUAGE))
        val done = fresh.copy(languageChosen = true, onboardingDone = true)
        assertEquals(AppDestination.Premium(variant = 1, fromOnboarding = true), next(1, done, FlowStepType.ONBOARDING))
        assertEquals(AppDestination.Home, next(1, done, FlowStepType.PREMIUM))
    }

    @Test
    fun returningUser_goesStraightHome() {
        val done = fresh.copy(languageChosen = true, onboardingDone = true)
        assertEquals(AppDestination.Home, next(2, done, null))
        assertEquals(AppDestination.Home, next(7, done, null))
    }

    @Test
    fun returningUser_whoQuitMidway_resumesAtMissingStep() {
        assertEquals(
            AppDestination.Onboarding(replay = false),
            next(2, fresh.copy(languageChosen = true), null),
        )
        assertEquals(AppDestination.Language(fromOnboarding = true), next(3, fresh, null))
    }

    @Test
    fun premiumIsSkippedForPremiumUsers() {
        val premium = fresh.copy(languageChosen = true, onboardingDone = true, isPremium = true)
        assertEquals(AppDestination.Home, next(1, premium, null))
        assertEquals(AppDestination.Home, next(1, premium, FlowStepType.ONBOARDING))
    }

    @Test
    fun disabledAndDuplicateStepsAreFiltered() {
        val config = FlowConfig(
            listOf(
                FlowStep(FlowStepType.LANGUAGE, enabled = false),
                FlowStep(FlowStepType.ONBOARDING),
                FlowStep(FlowStepType.ONBOARDING),
                FlowStep(FlowStepType.PREMIUM, premiumVariant = 2),
                FlowStep(FlowStepType.PREMIUM, premiumVariant = 3),
            ),
        )
        assertEquals(listOf(FlowStepType.ONBOARDING, FlowStepType.PREMIUM), OnboardingFlowHelper.normalize(config).map { it.type })
        assertEquals(AppDestination.Onboarding(replay = false), OnboardingFlowHelper.next(config, fresh, null))
        assertEquals(AppDestination.Premium(variant = 2, fromOnboarding = true), OnboardingFlowHelper.next(config, fresh, FlowStepType.ONBOARDING))
    }

    @Test
    fun exhaustedList_endsAtHome() {
        val config = FlowConfig(listOf(FlowStep(FlowStepType.ONBOARDING)))
        assertEquals(AppDestination.Home, OnboardingFlowHelper.next(config, fresh, FlowStepType.ONBOARDING))
        assertEquals(AppDestination.Home, OnboardingFlowHelper.next(FlowConfig(emptyList()), fresh, null))
    }

    @Test
    fun stepAfterIsNeverRepeated() {
        assertEquals(AppDestination.Home, next(1, fresh, FlowStepType.PREMIUM))
    }

    @Test
    fun splashInterstitialStep_isNeverADestination() {
        val config = FlowConfig(listOf(FlowStep(FlowStepType.SPLASH_INTERSTITIAL), FlowStep(FlowStepType.HOME)))
        assertEquals(AppDestination.Home, OnboardingFlowHelper.next(config, fresh, null))
        assertEquals(FlowStepType.SPLASH_INTERSTITIAL, OnboardingFlowHelper.defaultConfigFor(1).steps.first().type)
        assertEquals(AppDestination.Language(fromOnboarding = true), next(1, fresh, null))
    }

    @Test
    fun splashInterstitialStep_canBeSwitchedOffRemotely() {
        val json = """{"screens":[{"id":"SPLASH_INTERSTITIAL","enabled":false},{"id":"HOME"}]}"""
        val steps = OnboardingFlowHelper.normalize(OnboardingFlowHelper.configFor(2, json))
        assertEquals(listOf(FlowStepType.HOME), steps.map { it.type })
    }
}
