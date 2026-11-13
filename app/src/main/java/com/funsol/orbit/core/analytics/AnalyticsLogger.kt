package com.funsol.orbit.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.funsol.orbit.BuildConfig
import com.funsol.orbit.core.common.tryCatch
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Single entry point for Firebase Analytics events.
 *
 * Every call is wrapped in [tryCatch] so analytics can never crash the app (for example when
 * google-services.json is not configured yet). Event names are normalised to Firebase rules:
 * lowercase, only a-z 0-9 and "_", at most 40 characters.
 * Never pass user content (task titles, note text, goal names) as an event name or parameter.
 */
object AnalyticsLogger {
    private const val TAG = "current_analytic"

    @Volatile
    private var firebaseAnalytics: FirebaseAnalytics? = null

    /** Set after the first init attempt (success OR failure) so a broken/missing Firebase setup is never retried per event. */
    @Volatile
    private var initAttempted = false

    /**
     * Initialises Firebase Analytics exactly once per process (double-checked, thread-safe).
     * Called from Application.onCreate; also invoked lazily by [analytics] as a safety net.
     * This never calls FirebaseApp.initializeApp: FirebaseApp is created by the google-services plugin or by
     * FirebaseBootstrap (Remote Config) and Analytics only reads it.
     */
    fun init(context: Context) {
        if (initAttempted) return
        synchronized(this) {
            if (initAttempted) return
            initAttempted = true
            tryCatch {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
                if (BuildConfig.DEBUG) Log.i(TAG, "FirebaseAnalytics initialised once")
            }
        }
    }

    private fun analytics(context: Context): FirebaseAnalytics? {
        if (!initAttempted) init(context)
        return firebaseAnalytics
    }

    private fun clean(value: String): String = value
        .trim()
        .lowercase()
        .replace("[^a-z0-9_]".toRegex(), "_")
        .take(40)

    fun logEvent(context: Context, value: String) {
        tryCatch {
            val name = clean(value)
            analytics(context)?.logEvent(name, Bundle().apply { putString(name, name) })
            if (BuildConfig.DEBUG) Log.i(TAG, "postAnalytic: $name")
        }
    }

    fun logEventWithBundle(context: Context, value: String, bundle: Bundle) {
        tryCatch {
            val name = clean(value)
            analytics(context)?.logEvent(name, bundle)
            if (BuildConfig.DEBUG) Log.i(TAG, "postAnalytic: $name $bundle")
        }
    }
}
