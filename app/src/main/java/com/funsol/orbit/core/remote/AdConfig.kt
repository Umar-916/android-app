package com.funsol.orbit.core.remote

import com.funsol.orbit.core.ads.BannerPlacement
import com.funsol.orbit.core.ads.NativePlacement

object RemoteKeys {
    const val ADS_ENABLED = "ads_enabled"
    const val BANNER_ENABLED = "banner_enabled"
    const val BANNER_SCREENS = "banner_screens"
    const val NATIVE_LANGUAGE_ENABLED = "native_language_enabled"
    const val NATIVE_ONBOARDING_ENABLED = "native_onboarding_enabled"
    const val NATIVE_EXIT_ENABLED = "native_exit_enabled"
    const val NATIVE_HOME_ENABLED = "native_home_enabled"
    const val LANGUAGE_AD_TYPE = "language_ad_type"
    const val INTERSTITIAL_SPLASH_FIRST_RUN = "interstitial_splash_first_run"
    const val INTERSTITIAL_SPLASH_RETURNING = "interstitial_splash_returning"
    const val INTERSTITIAL_NAV_ENABLED = "interstitial_nav_enabled"
    const val INTERSTITIAL_NAV_EVERY_N = "interstitial_nav_every_n_actions"
    const val INTERSTITIAL_MIN_INTERVAL = "interstitial_min_interval_seconds"
    const val APP_OPEN_ENABLED = "app_open_enabled"
    const val APP_OPEN_MIN_BACKGROUND = "app_open_min_background_seconds"
    const val SPLASH_MAX_WAIT = "splash_max_wait_seconds"
    const val ONBOARDING_FLOW_FIRST_RUN = "onboarding_flow_first_run"
    const val ONBOARDING_FLOW_RETURNING = "onboarding_flow_returning"
    const val PREMIUM_CLOSE_MODE = "premium_close_mode"
    const val PREMIUM_VARIANT_INNER = "premium_variant_inner"
}

data class AdConfig(
    val adsEnabled: Boolean = true,
    val bannerEnabled: Boolean = true,
    val bannerScreens: Set<String> = DEFAULT_BANNER_SCREENS,
    val nativeLanguageEnabled: Boolean = true,
    val nativeOnboardingEnabled: Boolean = true,
    val nativeExitEnabled: Boolean = true,
    val nativeHomeEnabled: Boolean = true,
    val languageAdType: String = LanguageAdType.RECT,
    val interstitialSplashFirstRun: Boolean = false,
    val interstitialSplashReturning: Boolean = true,
    val interstitialNavEnabled: Boolean = true,
    val interstitialNavEveryNActions: Int = 4,
    val interstitialMinIntervalSeconds: Int = 90,
    val appOpenEnabled: Boolean = false,
    val appOpenMinBackgroundSeconds: Int = 60,
    val splashMaxWaitSeconds: Int = 7,
    val onboardingFlowFirstRun: String = "",
    val onboardingFlowReturning: String = "",
    val premiumCloseMode: Int = 2,
    val premiumVariantInner: Int = 1,
) {
    fun bannerAllowedOn(placement: BannerPlacement): Boolean =
        bannerEnabled && placement.key in bannerScreens

    fun nativeEnabled(placement: NativePlacement): Boolean = when (placement) {
        NativePlacement.LANGUAGE -> nativeLanguageEnabled
        NativePlacement.ONBOARDING -> nativeOnboardingEnabled
        NativePlacement.EXIT -> nativeExitEnabled
        NativePlacement.HOME -> nativeHomeEnabled
    }

    fun splashInterstitialEnabled(firstRun: Boolean): Boolean =
        if (firstRun) interstitialSplashFirstRun else interstitialSplashReturning

    val splashHardCapMs: Long get() = (splashMaxWaitSeconds + SPLASH_CAP_EXTRA_SECONDS) * 1_000L

    fun flowJson(firstRun: Boolean): String = if (firstRun) onboardingFlowFirstRun else onboardingFlowReturning

    companion object {
        const val SPLASH_CAP_EXTRA_SECONDS = 3
        val DEFAULT_BANNER_SCREENS: Set<String> = setOf("home", "tasks", "calendar", "more")

        val DEFAULT = AdConfig()

        fun parse(lookup: (String) -> String?): AdConfig {
            val d = DEFAULT
            fun raw(key: String): String? = lookup(key)?.trim()?.takeIf { it.isNotEmpty() }
            fun bool(key: String, default: Boolean): Boolean = when (raw(key)?.lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> default
            }
            fun int(key: String, default: Int, range: IntRange): Int =
                (raw(key)?.let { it.toIntOrNull() ?: it.toDoubleOrNull()?.takeIf { d -> d.isFinite() }?.toInt() } ?: default).coerceIn(range)

            val screens = raw(RemoteKeys.BANNER_SCREENS)
                ?.split(',')
                ?.map { it.trim().lowercase() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: d.bannerScreens

            return AdConfig(
                adsEnabled = bool(RemoteKeys.ADS_ENABLED, d.adsEnabled),
                bannerEnabled = bool(RemoteKeys.BANNER_ENABLED, d.bannerEnabled),
                bannerScreens = screens,
                nativeLanguageEnabled = bool(RemoteKeys.NATIVE_LANGUAGE_ENABLED, d.nativeLanguageEnabled),
                nativeOnboardingEnabled = bool(RemoteKeys.NATIVE_ONBOARDING_ENABLED, d.nativeOnboardingEnabled),
                nativeExitEnabled = bool(RemoteKeys.NATIVE_EXIT_ENABLED, d.nativeExitEnabled),
                nativeHomeEnabled = bool(RemoteKeys.NATIVE_HOME_ENABLED, d.nativeHomeEnabled),
                languageAdType = LanguageAdType.normalize(raw(RemoteKeys.LANGUAGE_AD_TYPE)),
                interstitialSplashFirstRun = bool(RemoteKeys.INTERSTITIAL_SPLASH_FIRST_RUN, d.interstitialSplashFirstRun),
                interstitialSplashReturning = bool(RemoteKeys.INTERSTITIAL_SPLASH_RETURNING, d.interstitialSplashReturning),
                interstitialNavEnabled = bool(RemoteKeys.INTERSTITIAL_NAV_ENABLED, d.interstitialNavEnabled),
                interstitialNavEveryNActions = int(RemoteKeys.INTERSTITIAL_NAV_EVERY_N, d.interstitialNavEveryNActions, 1..50),
                interstitialMinIntervalSeconds = int(RemoteKeys.INTERSTITIAL_MIN_INTERVAL, d.interstitialMinIntervalSeconds, 0..3_600),
                appOpenEnabled = bool(RemoteKeys.APP_OPEN_ENABLED, d.appOpenEnabled),
                appOpenMinBackgroundSeconds = int(RemoteKeys.APP_OPEN_MIN_BACKGROUND, d.appOpenMinBackgroundSeconds, 0..86_400),
                splashMaxWaitSeconds = int(RemoteKeys.SPLASH_MAX_WAIT, d.splashMaxWaitSeconds, 2..15),
                onboardingFlowFirstRun = raw(RemoteKeys.ONBOARDING_FLOW_FIRST_RUN).orEmpty(),
                onboardingFlowReturning = raw(RemoteKeys.ONBOARDING_FLOW_RETURNING).orEmpty(),
                premiumCloseMode = int(RemoteKeys.PREMIUM_CLOSE_MODE, d.premiumCloseMode, 0..3),
                premiumVariantInner = int(RemoteKeys.PREMIUM_VARIANT_INNER, d.premiumVariantInner, 1..3),
            )
        }
    }
}

object LanguageAdType {
    const val RECT = "rect"
    const val NATIVE = "native"

    fun normalize(raw: String?): String = if (raw?.trim()?.equals(NATIVE, ignoreCase = true) == true) NATIVE else RECT
}
