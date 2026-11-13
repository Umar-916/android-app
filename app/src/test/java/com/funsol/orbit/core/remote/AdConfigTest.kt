package com.funsol.orbit.core.remote

import com.funsol.orbit.core.ads.BannerPlacement
import com.funsol.orbit.core.ads.NativePlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdConfigTest {
    private fun parse(vararg pairs: Pair<String, String?>): AdConfig {
        val values = pairs.toMap()
        return AdConfig.parse { key -> values[key] }
    }

    @Test
    fun emptyOrMissingValues_giveTheDocumentedDefaults() {
        assertEquals(AdConfig.DEFAULT, parse())
        val d = AdConfig.DEFAULT
        assertTrue(d.adsEnabled)
        assertFalse(d.interstitialSplashFirstRun)
        assertTrue(d.interstitialSplashReturning)
        assertEquals(4, d.interstitialNavEveryNActions)
        assertEquals(90, d.interstitialMinIntervalSeconds)
        assertFalse(d.appOpenEnabled)
        assertEquals(60, d.appOpenMinBackgroundSeconds)
        assertEquals(7, d.splashMaxWaitSeconds)
        assertEquals(2, d.premiumCloseMode)
        assertEquals(1, d.premiumVariantInner)
        assertEquals(10_000L, d.splashHardCapMs)
    }

    @Test
    fun blankStrings_areTreatedAsMissing() {
        val cfg = parse(RemoteKeys.ADS_ENABLED to "  ", RemoteKeys.INTERSTITIAL_NAV_EVERY_N to "")
        assertEquals(AdConfig.DEFAULT, cfg)
    }

    @Test
    fun validValues_areApplied() {
        val cfg = parse(
            RemoteKeys.ADS_ENABLED to "false",
            RemoteKeys.BANNER_SCREENS to " Home , tasks ,, ",
            RemoteKeys.NATIVE_EXIT_ENABLED to "0",
            RemoteKeys.INTERSTITIAL_NAV_EVERY_N to "6",
            RemoteKeys.INTERSTITIAL_MIN_INTERVAL to "120",
            RemoteKeys.APP_OPEN_ENABLED to "TRUE",
            RemoteKeys.SPLASH_MAX_WAIT to "9",
            RemoteKeys.PREMIUM_CLOSE_MODE to "3",
            RemoteKeys.PREMIUM_VARIANT_INNER to "2",
        )
        assertFalse(cfg.adsEnabled)
        assertEquals(setOf("home", "tasks"), cfg.bannerScreens)
        assertFalse(cfg.nativeExitEnabled)
        assertEquals(6, cfg.interstitialNavEveryNActions)
        assertEquals(120, cfg.interstitialMinIntervalSeconds)
        assertTrue(cfg.appOpenEnabled)
        assertEquals(9, cfg.splashMaxWaitSeconds)
        assertEquals(12_000L, cfg.splashHardCapMs)
        assertEquals(3, cfg.premiumCloseMode)
        assertEquals(2, cfg.premiumVariantInner)
    }

    @Test
    fun languageAdType_defaultsToRect_andAcceptsNativeOnly() {
        assertEquals(LanguageAdType.RECT, parse().languageAdType)
        assertEquals(LanguageAdType.NATIVE, parse(RemoteKeys.LANGUAGE_AD_TYPE to " Native ").languageAdType)
        assertEquals(LanguageAdType.RECT, parse(RemoteKeys.LANGUAGE_AD_TYPE to "rect").languageAdType)
        assertEquals(LanguageAdType.RECT, parse(RemoteKeys.LANGUAGE_AD_TYPE to "banana").languageAdType)
    }

    @Test
    fun malformedValues_fallBackToDefaults() {
        val cfg = parse(
            RemoteKeys.ADS_ENABLED to "maybe",
            RemoteKeys.INTERSTITIAL_NAV_EVERY_N to "many",
            RemoteKeys.SPLASH_MAX_WAIT to "NaN",
        )
        assertEquals(AdConfig.DEFAULT, cfg)
    }

    @Test
    fun numbers_areClampedToSaneRanges() {
        val cfg = parse(
            RemoteKeys.INTERSTITIAL_NAV_EVERY_N to "0",
            RemoteKeys.INTERSTITIAL_MIN_INTERVAL to "-5",
            RemoteKeys.SPLASH_MAX_WAIT to "600",
            RemoteKeys.PREMIUM_CLOSE_MODE to "9",
            RemoteKeys.PREMIUM_VARIANT_INNER to "0",
        )
        assertEquals(1, cfg.interstitialNavEveryNActions)
        assertEquals(0, cfg.interstitialMinIntervalSeconds)
        assertEquals(15, cfg.splashMaxWaitSeconds)
        assertEquals(3, cfg.premiumCloseMode)
        assertEquals(1, cfg.premiumVariantInner)
    }

    @Test
    fun placementHelpers_followTheFlags() {
        val cfg = parse(RemoteKeys.BANNER_SCREENS to "home", RemoteKeys.NATIVE_HOME_ENABLED to "false")
        assertTrue(cfg.bannerAllowedOn(BannerPlacement.HOME))
        assertFalse(cfg.bannerAllowedOn(BannerPlacement.TASKS))
        assertFalse(cfg.nativeEnabled(NativePlacement.HOME))
        assertTrue(cfg.nativeEnabled(NativePlacement.LANGUAGE))
        assertFalse(cfg.splashInterstitialEnabled(firstRun = true))
        assertTrue(cfg.splashInterstitialEnabled(firstRun = false))
        assertFalse(parse(RemoteKeys.BANNER_ENABLED to "false").bannerAllowedOn(BannerPlacement.HOME))
    }

    @Test
    fun flowJson_isSelectedByRun() {
        val cfg = parse(
            RemoteKeys.ONBOARDING_FLOW_FIRST_RUN to "{\"a\":1}",
            RemoteKeys.ONBOARDING_FLOW_RETURNING to "{\"b\":2}",
        )
        assertEquals("{\"a\":1}", cfg.flowJson(firstRun = true))
        assertEquals("{\"b\":2}", cfg.flowJson(firstRun = false))
    }
}
