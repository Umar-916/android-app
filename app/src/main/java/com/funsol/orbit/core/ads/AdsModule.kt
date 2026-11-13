package com.funsol.orbit.core.ads

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.funsol.orbit.core.consent.ConsentManager
import com.funsol.orbit.core.network.ConnectivityNetworkObserver
import com.funsol.orbit.core.network.NetworkObserver
import com.funsol.orbit.core.remote.FirebaseRemoteConfigRepository
import com.funsol.orbit.core.remote.RemoteConfigRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val AppScopeQualifier = named("orbit_app_scope")

val adsModule = module {
    single(AppScopeQualifier) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { ConnectivityNetworkObserver(androidContext()) } bind NetworkObserver::class
    single<RemoteConfigRepository> { FirebaseRemoteConfigRepository(androidContext(), get()) }
    single { ConsentManager(androidContext()) }

    single { AdIds.forBuild(androidContext()) }
    single { ActivityTracker() }
    single { PremiumState(get(), get(AppScopeQualifier)) }
    single { FullScreenAdState() }
    single { InterstitialCapping() }
    single { MobileAdsInitializer(androidContext(), get()) }
    single {
        val consent = get<ConsentManager>()
        val sdk = get<MobileAdsInitializer>()
        val remote = get<RemoteConfigRepository>()
        AdGate(
            premium = get<PremiumState>().isPremium,
            online = get<NetworkObserver>().online,
            canRequestAds = consent.canRequestAds,
            sdkReady = sdk.ready,
            config = remote.config,
            ids = get(),
        )
    }

    single { InterstitialAdManager(androidContext(), get(), get(), get(), get(), get()) }
    single { AppOpenAdManager(get(), get(), get(), get(), get(), get(), get(), get(AppScopeQualifier)) }
    single { NativeAdPool(get(), get()) }
    single { BannerAdManager(androidContext(), get(), get(), get()) }
    single { RectBannerAdManager(get(), get(), get()) }

    single {
        AdsController(
            context = androidContext(),
            scope = get(AppScopeQualifier),
            settings = get(),
            network = get(),
            remote = get(),
            consent = get(),
            sdk = get(),
            gate = get(),
            ids = get(),
            tracker = get(),
            interstitial = get(),
            appOpen = get(),
            nativePool = get(),
            banner = get(),
            rectBanner = get(),
            premium = get(),
        )
    } binds arrayOf(AdsStartup::class, SplashAds::class)
}

@Composable
fun rememberNaturalTransitionAd(): (onDone: () -> Unit) -> Unit {
    val controller = koinInject<AdsController>()
    val activity = LocalActivity.current
    return remember(controller, activity) {
        { onDone: () -> Unit ->
            if (activity == null) onDone() else controller.onNaturalTransition(activity, onDone)
        }
    }
}
