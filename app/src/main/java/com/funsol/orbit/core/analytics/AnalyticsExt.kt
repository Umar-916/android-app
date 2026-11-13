package com.funsol.orbit.core.analytics

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.funsol.orbit.core.common.tryCatch

/** Logs [text] as a Firebase event. Safe to call from anywhere with a Context. */
fun Context.postAnalytic(text: String) {
    tryCatch { AnalyticsLogger.logEvent(this, text) }
}

/** Logs [text] once when this composable first enters the composition (screen / dialog / sheet display). */
@Composable
fun PostAnalytic(text: String) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { context.postAnalytic(text) }
}

/**
 * Returns a click wrapper that logs [event] and then runs [action].
 * Use inside composables: `onClick = trackedClick(Events.X_CLICK) { vm.doIt() }`.
 */
@Composable
fun trackedClick(event: String, action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return {
        context.postAnalytic(event)
        action()
    }
}
