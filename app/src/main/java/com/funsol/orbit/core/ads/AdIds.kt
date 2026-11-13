package com.funsol.orbit.core.ads

import android.content.Context
import com.funsol.orbit.BuildConfig
import com.funsol.orbit.R

object AdIds {
    private const val SAMPLE_PREFIX = "ca-app-pub-3940256099942544"
    const val SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val SAMPLE_BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val SAMPLE_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val SAMPLE_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    const val SAMPLE_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    const val RELEASE_BANNER = ""
    const val RELEASE_RECT_BANNER = ""
    const val RELEASE_INTERSTITIAL = ""
    const val RELEASE_NATIVE = ""
    const val RELEASE_APP_OPEN = ""

    fun forBuild(context: Context): AdUnitIds = AdUnitIds(
        appId = context.getString(R.string.admob_app_id).trim().let { if (!BuildConfig.DEBUG && (it.startsWith(SAMPLE_PREFIX) || !it.startsWith("ca-app-pub-"))) "" else it },
        banner = if (BuildConfig.DEBUG) SAMPLE_BANNER else releaseOnly(RELEASE_BANNER),
        rectBanner = if (BuildConfig.DEBUG) SAMPLE_BANNER else releaseOnly(RELEASE_RECT_BANNER).ifBlank { releaseOnly(RELEASE_BANNER) },
        native = if (BuildConfig.DEBUG) SAMPLE_NATIVE else releaseOnly(RELEASE_NATIVE),
        interstitial = if (BuildConfig.DEBUG) SAMPLE_INTERSTITIAL else releaseOnly(RELEASE_INTERSTITIAL),
        appOpen = if (BuildConfig.DEBUG) SAMPLE_APP_OPEN else releaseOnly(RELEASE_APP_OPEN),
    )

    private fun releaseOnly(id: String): String = id.trim().takeUnless { it.startsWith(SAMPLE_PREFIX) }.orEmpty()
}
