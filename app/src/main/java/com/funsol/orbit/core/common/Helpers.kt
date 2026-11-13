package com.funsol.orbit.core.common

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.funsol.orbit.BuildConfig
import com.funsol.orbit.R
import kotlinx.coroutines.CancellationException

object AppConfig {
    const val FEEDBACK_EMAIL = ""

    const val PREMIUM_PRODUCT_ID = ""

    val playStoreUrl: String get() = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
    val marketUri: String get() = "market://details?id=${BuildConfig.APPLICATION_ID}"
}

inline fun tryCatch(block: () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
    }
}

class ClickGuard(private val windowMs: Long = 600L, private val clock: () -> Long = System::currentTimeMillis) {
    private var last = 0L
    fun run(action: () -> Unit): Boolean {
        val now = clock()
        if (now - last < windowMs) return false
        last = now
        action()
        return true
    }
}

@Composable
fun rememberClickGuard(windowMs: Long = 600L): ClickGuard = remember { ClickGuard(windowMs) }

@Composable
fun Modifier.debounceClickable(delayMillis: Long = 800L, role: Role? = Role.Button, onClick: () -> Unit): Modifier {
    val guard = remember(delayMillis) { ClickGuard(delayMillis) }
    return this.clickable(role = role) { guard.run(onClick) }
}

private object SystemBarsHolders {
    var count = 0
}

@Composable
fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        SystemBarsHolders.count++
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            SystemBarsHolders.count = (SystemBarsHolders.count - 1).coerceAtLeast(0)
            if (SystemBarsHolders.count == 0) controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

fun Context.shareAppLink() {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text, getString(R.string.app_name), AppConfig.playStoreUrl))
    }
    tryCatch {
        startActivity(Intent.createChooser(send, getString(R.string.share_app_chooser)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

fun Context.openInPlayStore() {
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.marketUri)).addFlags(flags))
    } catch (_: ActivityNotFoundException) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.playStoreUrl)).addFlags(flags))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_cannot_open_link, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.error_cannot_open_link, Toast.LENGTH_SHORT).show()
    }
}

fun Context.composeEmail(to: String, subject: String, body: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
