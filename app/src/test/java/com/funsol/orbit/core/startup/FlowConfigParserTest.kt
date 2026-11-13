package com.funsol.orbit.core.startup

import com.funsol.orbit.core.datastore.AppSettings
import com.funsol.orbit.core.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowConfigParserTest {
    private val fallback = OnboardingFlowHelper.defaultConfigFor(1)

    @Test
    fun blankNullAndGarbage_useTheFallback() {
        assertSame(fallback, FlowConfigParser.parse(null, fallback))
        assertSame(fallback, FlowConfigParser.parse("", fallback))
        assertSame(fallback, FlowConfigParser.parse("   ", fallback))
        assertSame(fallback, FlowConfigParser.parse("not json", fallback))
        assertSame(fallback, FlowConfigParser.parse("[1,2,3]", fallback))
        assertSame(fallback, FlowConfigParser.parse("{\"screens\":\"oops\"}", fallback))
        assertSame(fallback, FlowConfigParser.parse("{\"screens\":[]}", fallback))
        assertSame(fallback, FlowConfigParser.parse("{\"screens\":[{\"id\":\"MARS\"},42,null]}", fallback))
    }

    @Test
    fun validJson_isParsed_withDefaultsForMissingFields() {
        val json = """{"screens":[{"id":"splash_interstitial","enabled":false},{"id":"LANGUAGE"},{"id":"PREMIUM","screenType":3},{"id":"HOME"}]}"""
        val cfg = FlowConfigParser.parse(json, fallback)
        assertEquals(
            listOf(
                FlowStep(FlowStepType.SPLASH_INTERSTITIAL, enabled = false),
                FlowStep(FlowStepType.LANGUAGE),
                FlowStep(FlowStepType.PREMIUM, premiumVariant = 3),
                FlowStep(FlowStepType.HOME),
            ),
            cfg.steps,
        )
    }

    @Test
    fun unknownEntriesAreSkipped_andScreenTypeIsClamped() {
        val json = """{"screens":[{"id":"MARS"},{"id":"PREMIUM","screenType":99},{"id":"ONBOARDING","enabled":"false"}]}"""
        val cfg = FlowConfigParser.parse(json, fallback)
        assertEquals(
            listOf(FlowStep(FlowStepType.PREMIUM, premiumVariant = 3), FlowStep(FlowStepType.ONBOARDING, enabled = false)),
            cfg.steps,
        )
    }

    @Test
    fun disablingAScreenRemotely_changesTheDestination() {
        val json = """{"screens":[{"id":"LANGUAGE"},{"id":"ONBOARDING","enabled":false},{"id":"PREMIUM","screenType":2},{"id":"HOME"}]}"""
        val cfg = OnboardingFlowHelper.configFor(1, json)
        val afterLanguage = OnboardingFlowHelper.next(cfg, AppSettings(languageChosen = true), FlowStepType.LANGUAGE)
        assertEquals(AppDestination.Premium(variant = 2, fromOnboarding = true), afterLanguage)
    }

    @Test
    fun invalidRemoteJson_keepsTheBuiltInFlow() {
        val cfg = OnboardingFlowHelper.configFor(2, "{broken")
        assertEquals(OnboardingFlowHelper.defaultConfigFor(2), cfg)
        assertTrue(cfg.steps.none { it.type == FlowStepType.PREMIUM })
    }
}
