package com.funsol.orbit.core.ads

import com.funsol.orbit.core.remote.AdConfig
import com.funsol.orbit.core.remote.LanguageAdType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdGateRulesTest {
    private val ids = AdUnitIds("app", "banner", "native", "inter", "open")
    private val ok = AdEnvironment(premium = false, online = true, canRequestAds = true, sdkReady = true)

    private fun decide(
        kind: AdKind,
        placement: String? = null,
        env: AdEnvironment = ok,
        config: AdConfig = AdConfig.DEFAULT.copy(appOpenEnabled = true),
        unitIds: AdUnitIds = ids,
    ) = AdGateRules.decide(kind, placement, env, config, unitIds)

    @Test
    fun everythingGood_allowsEachKind() {
        assertEquals(AdDecision.ALLOWED, decide(AdKind.BANNER, "home"))
        assertEquals(AdDecision.ALLOWED, decide(AdKind.NATIVE, "exit"))
        assertEquals(AdDecision.ALLOWED, decide(AdKind.RECT_BANNER, "language"))
        assertEquals(AdDecision.ALLOWED, decide(AdKind.INTERSTITIAL, InterstitialPlacement.NAVIGATION))
        assertEquals(AdDecision.ALLOWED, decide(AdKind.APP_OPEN))
    }

    @Test
    fun premium_blocksEverythingFirst() {
        val env = ok.copy(premium = true, online = false, canRequestAds = false)
        AdKind.entries.forEach { assertEquals(AdDecision.PREMIUM, decide(it, "home", env)) }
    }

    @Test
    fun masterSwitchOff_disablesRemotely() {
        val cfg = AdConfig.DEFAULT.copy(adsEnabled = false)
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.NATIVE, "home", config = cfg))
    }

    @Test
    fun blankUnitId_orAppId_disablesThePlacement() {
        assertEquals(AdDecision.NO_AD_UNIT, decide(AdKind.BANNER, "home", unitIds = ids.copy(banner = "")))
        assertEquals(AdDecision.NO_AD_UNIT, decide(AdKind.NATIVE, "home", unitIds = ids.copy(appId = "")))
        assertEquals(AdDecision.ALLOWED, decide(AdKind.NATIVE, "home", unitIds = ids.copy(banner = "")))
    }

    @Test
    fun remoteFlags_areHonouredPerPlacement() {
        val cfg = AdConfig.DEFAULT.copy(
            bannerScreens = setOf("home"),
            nativeExitEnabled = false,
            interstitialNavEnabled = false,
            appOpenEnabled = false,
        )
        assertEquals(AdDecision.ALLOWED, decide(AdKind.BANNER, "home", config = cfg))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.BANNER, "tasks", config = cfg))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.NATIVE, "exit", config = cfg))
        assertEquals(AdDecision.ALLOWED, decide(AdKind.NATIVE, "home", config = cfg))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.INTERSTITIAL, InterstitialPlacement.NAVIGATION, config = cfg))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.APP_OPEN, config = cfg))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.NATIVE, "nope"))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.INTERSTITIAL, "nope"))
    }

    @Test
    fun languageAd_isRectByDefault_andNativeWhenConfigured() {
        assertEquals(AdDecision.ALLOWED, decide(AdKind.RECT_BANNER, "language"))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.NATIVE, "language"))
        val nativeCfg = AdConfig.DEFAULT.copy(languageAdType = LanguageAdType.NATIVE)
        assertEquals(AdDecision.ALLOWED, decide(AdKind.NATIVE, "language", config = nativeCfg))
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.RECT_BANNER, "language", config = nativeCfg))
        val off = AdConfig.DEFAULT.copy(nativeLanguageEnabled = false)
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.RECT_BANNER, "language", config = off))
        assertEquals(AdDecision.NO_AD_UNIT, decide(AdKind.RECT_BANNER, "language", unitIds = ids.copy(rectBanner = "")))
        assertEquals(AdDecision.NO_AD_UNIT, decide(AdKind.BANNER, "home", unitIds = ids.copy(banner = "")))
    }

    @Test
    fun splashInterstitial_followsFirstRunAndReturningFlags() {
        assertEquals(AdDecision.DISABLED_REMOTE, decide(AdKind.INTERSTITIAL, InterstitialPlacement.SPLASH_FIRST_RUN))
        assertEquals(AdDecision.ALLOWED, decide(AdKind.INTERSTITIAL, InterstitialPlacement.SPLASH_RETURNING))
    }

    @Test
    fun consentIsCheckedBeforeNetwork_andNetworkBeforeSdk() {
        assertEquals(AdDecision.NO_CONSENT, decide(AdKind.BANNER, "home", ok.copy(canRequestAds = false, online = false)))
        assertEquals(AdDecision.OFFLINE, decide(AdKind.BANNER, "home", ok.copy(online = false, sdkReady = false)))
        assertEquals(AdDecision.NOT_INITIALIZED, decide(AdKind.BANNER, "home", ok.copy(sdkReady = false)))
    }

    @Test
    fun hardBlocks_areDistinguishedFromTransientOnes() {
        assertTrue(AdDecision.PREMIUM.isHardBlock)
        assertTrue(AdDecision.NO_CONSENT.isHardBlock)
        assertTrue(AdDecision.DISABLED_REMOTE.isHardBlock)
        assertTrue(AdDecision.NO_AD_UNIT.isHardBlock)
        assertFalse(AdDecision.OFFLINE.isHardBlock)
        assertFalse(AdDecision.NOT_INITIALIZED.isHardBlock)
        assertFalse(AdDecision.ALLOWED.isHardBlock)
    }
}
