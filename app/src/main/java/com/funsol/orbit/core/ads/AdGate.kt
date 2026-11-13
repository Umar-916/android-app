package com.funsol.orbit.core.ads

import com.funsol.orbit.core.remote.AdConfig
import com.funsol.orbit.core.remote.LanguageAdType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

data class AdUnitIds(
    val appId: String,
    val banner: String,
    val native: String,
    val interstitial: String,
    val appOpen: String,
    val rectBanner: String = banner,
) {
    fun forKind(kind: AdKind): String = when (kind) {
        AdKind.BANNER -> banner
        AdKind.RECT_BANNER -> rectBanner
        AdKind.NATIVE -> native
        AdKind.INTERSTITIAL -> interstitial
        AdKind.APP_OPEN -> appOpen
    }
}

enum class AdDecision {
    ALLOWED,
    PREMIUM,
    DISABLED_REMOTE,
    NO_AD_UNIT,
    NO_CONSENT,
    OFFLINE,
    NOT_INITIALIZED,

    DISABLED_BUILD,
    ;

    val isHardBlock: Boolean get() = this == DISABLED_BUILD || this == PREMIUM || this == DISABLED_REMOTE || this == NO_AD_UNIT || this == NO_CONSENT
}

data class AdEnvironment(
    val premium: Boolean,
    val online: Boolean,
    val canRequestAds: Boolean,
    val sdkReady: Boolean,
)

object AdGateRules {
    fun decide(
        kind: AdKind,
        placementKey: String?,
        env: AdEnvironment,
        config: AdConfig,
        ids: AdUnitIds,
    ): AdDecision {
        if (env.premium) return AdDecision.PREMIUM
        if (!config.adsEnabled) return AdDecision.DISABLED_REMOTE
        if (ids.forKind(kind).isBlank() || ids.appId.isBlank()) return AdDecision.NO_AD_UNIT
        if (!remoteAllows(kind, placementKey, config)) return AdDecision.DISABLED_REMOTE
        if (!env.canRequestAds) return AdDecision.NO_CONSENT
        if (!env.online) return AdDecision.OFFLINE
        if (!env.sdkReady) return AdDecision.NOT_INITIALIZED
        return AdDecision.ALLOWED
    }

    private fun remoteAllows(kind: AdKind, placementKey: String?, config: AdConfig): Boolean = when (kind) {
        AdKind.BANNER -> config.bannerEnabled && (placementKey == null || placementKey.lowercase() in config.bannerScreens)
        AdKind.RECT_BANNER -> config.nativeLanguageEnabled && config.languageAdType == LanguageAdType.RECT
        AdKind.NATIVE -> NativePlacement.entries.firstOrNull { it.key == placementKey }
            ?.let { p -> config.nativeEnabled(p) && (p != NativePlacement.LANGUAGE || config.languageAdType == LanguageAdType.NATIVE) } ?: false
        AdKind.INTERSTITIAL -> when (placementKey) {
            InterstitialPlacement.SPLASH_FIRST_RUN -> config.splashInterstitialEnabled(firstRun = true)
            InterstitialPlacement.SPLASH_RETURNING -> config.splashInterstitialEnabled(firstRun = false)
            InterstitialPlacement.NAVIGATION -> config.interstitialNavEnabled
            else -> false
        }
        AdKind.APP_OPEN -> config.appOpenEnabled
    }
}

class AdGate(
    private val premium: StateFlow<Boolean>,
    private val online: StateFlow<Boolean>,
    private val canRequestAds: StateFlow<Boolean>,
    private val sdkReady: StateFlow<Boolean>,
    private val config: StateFlow<AdConfig>,
    private val ids: AdUnitIds,
) {
    private fun environment() = AdEnvironment(premium.value, online.value, canRequestAds.value, sdkReady.value)

    fun decision(kind: AdKind, placementKey: String? = null): AdDecision =
        if (!com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) AdDecision.DISABLED_BUILD
        else AdGateRules.decide(kind, placementKey, environment(), config.value, ids)

    fun allows(kind: AdKind, placementKey: String? = null): Boolean = decision(kind, placementKey) == AdDecision.ALLOWED

    fun decisionFlow(kind: AdKind, placementKey: String? = null): Flow<AdDecision> =
        if (!com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) kotlinx.coroutines.flow.flowOf(AdDecision.DISABLED_BUILD) else
        combine(premium, online, canRequestAds, sdkReady) { p, o, c, s -> AdEnvironment(p, o, c, s) }
            .combine(config) { env, cfg -> AdGateRules.decide(kind, placementKey, env, cfg, ids) }
            .distinctUntilChanged()

    val isPremium: Boolean get() = premium.value
}
