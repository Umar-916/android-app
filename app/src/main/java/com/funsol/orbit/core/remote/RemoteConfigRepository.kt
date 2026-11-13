package com.funsol.orbit.core.remote

import android.content.Context
import com.funsol.orbit.BuildConfig
import com.funsol.orbit.R
import com.funsol.orbit.core.network.NetworkObserver
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

interface RemoteConfigRepository {
    val config: StateFlow<AdConfig>

    val isConfigured: Boolean

    suspend fun fetch(timeoutMs: Long): Boolean
}

class StaticRemoteConfigRepository(initial: AdConfig = AdConfig.DEFAULT) : RemoteConfigRepository {
    private val state = MutableStateFlow(initial)
    override val config: StateFlow<AdConfig> = state.asStateFlow()
    override val isConfigured: Boolean = false
    override suspend fun fetch(timeoutMs: Long): Boolean = false

    fun set(config: AdConfig) {
        state.value = config
    }
}

class FirebaseRemoteConfigRepository(
    context: Context,
    private val network: NetworkObserver,
) : RemoteConfigRepository {
    private val appContext = context.applicationContext
    private val state = MutableStateFlow(AdConfig.DEFAULT)
    override val config: StateFlow<AdConfig> = state.asStateFlow()

    override val isConfigured: Boolean = RemoteConfigIds.isConfigured

    @Volatile
    private var remote: FirebaseRemoteConfig? = null

    override suspend fun fetch(timeoutMs: Long): Boolean {
        if (!isConfigured) return false
        return try {
            val rc = withContext(Dispatchers.Default) { ensureRemote() } ?: return false
            state.value = read(rc)
            if (!network.isOnline()) return false
            val fetched = withTimeoutOrNull(timeoutMs) { rc.fetchAndActivate().awaitBoolean() } == true
            if (fetched) state.value = read(rc)
            fetched
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            false
        }
    }

    private fun read(rc: FirebaseRemoteConfig): AdConfig = AdConfig.parse { key -> rc.getString(key) }

    private fun ensureRemote(): FirebaseRemoteConfig? {
        remote?.let { return it }
        return synchronized(this) {
            val existing = remote
            if (existing != null) {
                existing
            } else {
                val created = try {
                    FirebaseBootstrap.ensureInitialized(appContext)?.let { app -> configure(FirebaseRemoteConfig.getInstance(app)) }
                } catch (_: Throwable) {
                    null
                }
                remote = created
                created
            }
        }
    }

    private fun configure(rc: FirebaseRemoteConfig): FirebaseRemoteConfig {
        rc.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) DEBUG_FETCH_INTERVAL_S else RELEASE_FETCH_INTERVAL_S)
                .setFetchTimeoutInSeconds(FETCH_TIMEOUT_S)
                .build(),
        )
        rc.setDefaultsAsync(R.xml.remote_config_defaults)
        return rc
    }

    private companion object {
        const val DEBUG_FETCH_INTERVAL_S = 3L
        const val RELEASE_FETCH_INTERVAL_S = 12L * 60L * 60L
        const val FETCH_TIMEOUT_S = 7L
    }
}

internal object FirebaseBootstrap {
    @Synchronized
    fun ensureInitialized(context: Context): FirebaseApp? {
        if (RemoteConfigIds.API_KEY.isBlank() || RemoteConfigIds.APP_ID.isBlank() || RemoteConfigIds.PROJECT_ID.isBlank()) {
            return null
        }
        val existing = FirebaseApp.getApps(context).firstOrNull()
        if (existing != null) return existing
        val options = FirebaseOptions.Builder()
            .setApiKey(RemoteConfigIds.API_KEY)
            .setApplicationId(RemoteConfigIds.APP_ID)
            .setProjectId(RemoteConfigIds.PROJECT_ID)
            .build()
        return FirebaseApp.initializeApp(context, options)
    }
}

private suspend fun Task<Boolean>.awaitBoolean(): Boolean = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (cont.isActive) cont.resume(task.isSuccessful && task.result == true)
    }
}
