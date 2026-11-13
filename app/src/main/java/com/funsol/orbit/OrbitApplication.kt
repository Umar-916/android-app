package com.funsol.orbit

import android.app.Application
import com.funsol.orbit.core.ads.ActivityTracker
import com.funsol.orbit.core.ads.AdsController
import com.funsol.orbit.core.common.tryCatch
import com.funsol.orbit.core.di.appModules
import com.funsol.orbit.core.network.NetworkObserver
import com.funsol.orbit.core.notifications.NotificationHelper
import com.funsol.orbit.core.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

class OrbitApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@OrbitApplication)
            modules(appModules)
        }
        NotificationHelper.ensureChannels(this)
        startAdsLayer()
        appScope.launch {
            runCatching { get<ReminderScheduler>(ReminderScheduler::class.java).reconcileAll() }
        }
    }

    private fun startAdsLayer() {
        tryCatch { get<ActivityTracker>(ActivityTracker::class.java).register(this) }
        tryCatch { get<NetworkObserver>(NetworkObserver::class.java).start() }
        tryCatch { get<AdsController>(AdsController::class.java).start() }
    }
}
