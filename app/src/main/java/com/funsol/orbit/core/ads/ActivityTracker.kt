package com.funsol.orbit.core.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

class ActivityTracker : Application.ActivityLifecycleCallbacks {
    interface ForegroundListener {
        fun onEnteredForeground(backgroundedForMs: Long, isFirst: Boolean)

        fun onEnteredBackground() = Unit
    }

    private var currentRef: WeakReference<Activity>? = null
    private var startedCount = 0
    private var backgroundedAt = 0L
    private var firstForegroundDone = false
    private val listeners = CopyOnWriteArrayList<ForegroundListener>()
    private val destroyListeners = CopyOnWriteArrayList<(Activity) -> Unit>()

    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    fun current(): Activity? = currentRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

    fun addForegroundListener(listener: ForegroundListener) {
        listeners.addIfAbsent(listener)
    }

    fun addDestroyListener(listener: (Activity) -> Unit) {
        destroyListeners.add(listener)
    }

    private fun isAdActivity(activity: Activity): Boolean = activity.javaClass.name.contains("AdActivity", ignoreCase = true)

    private fun remember(activity: Activity) {
        if (!isAdActivity(activity)) currentRef = WeakReference(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = remember(activity)

    override fun onActivityStarted(activity: Activity) {
        remember(activity)
        startedCount++
        if (startedCount == 1) {
            val first = !firstForegroundDone
            firstForegroundDone = true
            val away = if (first || backgroundedAt == 0L) 0L else SystemClock.elapsedRealtime() - backgroundedAt
            listeners.forEach { it.onEnteredForeground(away, first) }
        }
    }

    override fun onActivityResumed(activity: Activity) = remember(activity)

    override fun onActivityStopped(activity: Activity) {
        startedCount = (startedCount - 1).coerceAtLeast(0)
        if (startedCount == 0) {
            backgroundedAt = SystemClock.elapsedRealtime()
            listeners.forEach { it.onEnteredBackground() }
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentRef?.get() === activity) currentRef = null
        destroyListeners.forEach { it(activity) }
    }

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
