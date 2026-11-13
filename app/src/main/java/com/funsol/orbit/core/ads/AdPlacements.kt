package com.funsol.orbit.core.ads

enum class AdKind { BANNER, RECT_BANNER, NATIVE, INTERSTITIAL, APP_OPEN }

enum class NativePlacement(val key: String) {
    LANGUAGE("language"),
    ONBOARDING("onboarding"),
    EXIT("exit"),
    HOME("home"),
}

enum class BannerPlacement(val key: String) {
    HOME("home"),
    TASKS("tasks"),
    CALENDAR("calendar"),
    MORE("more"),
}

object InterstitialPlacement {
    const val SPLASH_FIRST_RUN = "splash_first_run"
    const val SPLASH_RETURNING = "splash_returning"
    const val NAVIGATION = "nav"
}

enum class NativeAdStyle { Large, Compact }
