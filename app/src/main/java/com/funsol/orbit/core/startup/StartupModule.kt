package com.funsol.orbit.core.startup

import com.funsol.orbit.core.ads.AppScopeQualifier
import com.funsol.orbit.feature.onboarding.LanguageViewModel
import com.funsol.orbit.feature.onboarding.OnboardingViewModel
import com.funsol.orbit.feature.premium.BillingProvider
import com.funsol.orbit.feature.premium.PremiumViewModel
import com.funsol.orbit.feature.premium.UnavailableBillingProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startupFeatureModule = module {
    single { OnboardingFlowHelper(get(), get()) }
    single { InitializationHelper(buildStartupTasks(get(), get(), get(), get(), get())) }
    single<BillingProvider> { UnavailableBillingProvider() }

    viewModel { SplashViewModel(get(), get(), get(), get(AppScopeQualifier)) }
    viewModel { LanguageViewModel(get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { PremiumViewModel(get(), get(), get(), com.funsol.orbit.feature.premium.ConnectivityProvider { get<com.funsol.orbit.core.network.NetworkObserver>().isOnline() }) }
}
