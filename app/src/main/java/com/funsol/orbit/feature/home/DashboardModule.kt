package com.funsol.orbit.feature.home

import com.funsol.orbit.feature.analytics.AnalyticsViewModel
import com.funsol.orbit.feature.search.SearchViewModel
import com.funsol.orbit.feature.settings.BackupViewModel
import com.funsol.orbit.feature.settings.DashboardCustomizeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dashboardFeatureModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { AnalyticsViewModel(get(), get(), get(), get()) }
    viewModel { BackupViewModel(androidContext(), get()) }
    viewModel { DashboardCustomizeViewModel(get()) }
}
