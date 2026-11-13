package com.funsol.orbit.core.ads

import android.app.Activity
import android.content.Context
import com.funsol.orbit.core.consent.ConsentManager
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.network.NetworkObserver
import com.funsol.orbit.core.remote.AdConfig
import com.funsol.orbit.core.remote.LanguageAdType
import com.funsol.orbit.core.remote.RemoteConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

interface AdsStartup {
    fun isOnline(): Boolean
    suspend fun isPremium(): Boolean

    suspend fun gatherConsent()

    suspend fun fetchRemoteConfig()

    suspend fun initializeSdk()

    suspend fun preload(splashInterstitialInFlow: Boolean)
}

interface SplashAds {
    fun isOnline(): Boolean
    suspend fun isPremium(): Boolean
    val splashAdState: StateFlow<SplashAdState>
    val consentFormShowing: StateFlow<Boolean>
    val config: StateFlow<AdConfig>

    fun showSplashAd(onDone: () -> Unit)
}

class AdsController(
    context: Context,
    private val scope: CoroutineScope,
    private val settings: SettingsStore,
    private val network: NetworkObserver,
    private val remote: RemoteConfigRepository,
    private val consent: ConsentManager,
    private val sdk: MobileAdsInitializer,
    private val gate: AdGate,
    private val ids: AdUnitIds,
    private val tracker: ActivityTracker,
    private val interstitial: InterstitialAdManager,
    private val appOpen: AppOpenAdManager,
    private val nativePool: NativeAdPool,
    private val banner: BannerAdManager,
    private val rectBanner: RectBannerAdManager,
    private val premium: PremiumState,
) : AdsStartup, SplashAds {
    private val appContext = context.applicationContext
    private val _splashAdState = MutableStateFlow(SplashAdState.NOT_APPLICABLE)
    override val splashAdState: StateFlow<SplashAdState> = _splashAdState.asStateFlow()
    override val consentFormShowing: StateFlow<Boolean> = consent.formShowing
    override val config: StateFlow<AdConfig> = remote.config

    @Volatile
    private var startupDone = false

    @Volatile
    private var splashPlacement = InterstitialPlacement.SPLASH_RETURNING
    private val recovering = AtomicBoolean(false)

    fun start() {
        if (!com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) return
        appOpen.start()
        scope.launch {
            premium.isPremium.collect { isPremium -> if (isPremium) clearAll() }
        }
        scope.launch {
            var wasOnline = network.isOnline()
            network.online.collect { now ->
                if (now && !wasOnline && startupDone) recover()
                wasOnline = now
            }
        }
    }

    override fun isOnline(): Boolean = network.isOnline()

    override suspend fun isPremium(): Boolean =
        !com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM || settings.settings.first().isPremium

    override suspend fun gatherConsent() {
        if (isPremium() || consent.canRequestAds.value || !network.isOnline()) return
        val activity = tracker.current() ?: return
        withContext(Dispatchers.Main) { consent.gather(activity) }
    }

    override suspend fun fetchRemoteConfig() {
        if (isPremium()) return
        remote.fetch(remote.config.value.splashMaxWaitSeconds * 1_000L)
    }

    override suspend fun initializeSdk() {
        if (isPremium() || !network.isOnline() || !consent.canRequestAds.value) return
        if (!remote.config.value.adsEnabled || ids.appId.isBlank()) return
        sdk.initialize(SDK_INIT_TIMEOUT_MS)
    }

    override suspend fun preload(splashInterstitialInFlow: Boolean) {
        if (!com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) {
            _splashAdState.value = SplashAdState.NOT_APPLICABLE
            startupDone = true
            return
        }
        try {
            preloadInternal(handleSplash = true, splashInterstitialInFlow = splashInterstitialInFlow)
        } finally {
            startupDone = true
        }
    }

    private suspend fun preloadInternal(handleSplash: Boolean, splashInterstitialInFlow: Boolean) {
        val firstRun = settings.settings.first().runCount <= 1
        if (handleSplash) {
            val placement = if (firstRun) InterstitialPlacement.SPLASH_FIRST_RUN else InterstitialPlacement.SPLASH_RETURNING
            splashPlacement = placement
            if (splashInterstitialInFlow && gate.allows(AdKind.INTERSTITIAL, placement)) {
                _splashAdState.value = SplashAdState.LOADING
                interstitial.preload(placement) { ok ->
                    _splashAdState.value = if (ok) SplashAdState.LOADED else SplashAdState.FAILED
                }
            } else {
                _splashAdState.value = SplashAdState.NOT_APPLICABLE
            }
        }
        if (gate.allows(AdKind.INTERSTITIAL, InterstitialPlacement.NAVIGATION)) interstitial.preload(InterstitialPlacement.NAVIGATION)

        if (firstRun) {
            preloadLanguageAd()
            nativePool.preload(NativePlacement.ONBOARDING)
        } else {
            nativePool.preload(NativePlacement.HOME)
            banner.ensureLoaded(bannerCardWidthDp(appContext.resources.configuration.screenWidthDp))
        }
        appOpen.preload()
    }

    private fun preloadLanguageAd() {
        if (remote.config.value.languageAdType == LanguageAdType.NATIVE) {
            nativePool.preload(NativePlacement.LANGUAGE)
        } else {
            rectBanner.ensureLoaded()
        }
    }

    private fun recover() {
        if (!recovering.compareAndSet(false, true)) return
        scope.launch {
            try {
                if (isPremium()) return@launch
                gatherConsent()
                fetchRemoteConfig()
                initializeSdk()
                banner.resetFailure()
                rectBanner.resetFailure()
                preloadInternal(handleSplash = false, splashInterstitialInFlow = false)
            } finally {
                recovering.set(false)
            }
        }
    }

    override fun showSplashAd(onDone: () -> Unit) {
        val activity = tracker.current()
        if (activity == null) {
            onDone()
            return
        }
        interstitial.showThen(activity, InterstitialAdManager.Trigger.SPLASH, splashPlacement, onDone)
    }

    fun onNaturalTransition(activity: Activity, onDone: () -> Unit = {}) {
        interstitial.showThen(activity, InterstitialAdManager.Trigger.NAVIGATION, InterstitialPlacement.NAVIGATION, onDone)
    }

    fun preloadNative(placement: NativePlacement) {
        if (com.funsol.orbit.core.common.BuildFeatureFlags.FIRST_BUILD_SHOW_ADS_AND_PREMIUM) nativePool.preload(placement)
    }

    fun suppressAppOpenOnce() = appOpen.suppressNext()

    private fun clearAll() {
        interstitial.clear()
        appOpen.clear()
        nativePool.clear()
        banner.clear()
        rectBanner.clear()
        _splashAdState.value = SplashAdState.NOT_APPLICABLE
    }

    private companion object {
        const val SDK_INIT_TIMEOUT_MS = 7_000L
    }
}
