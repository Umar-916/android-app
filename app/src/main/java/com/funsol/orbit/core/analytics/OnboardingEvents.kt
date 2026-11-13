package com.funsol.orbit.core.analytics

/** Analytics events for splash-adjacent onboarding flow: language selection and intro pages. */
object OnboardingEvents {

    // Language screen (also reused from Settings)
    const val LANGUAGE_SCREEN_DISPLAY = "language_screen_display"
    const val LANGUAGE_DONE_CLICK = "language_done_click"
    const val LANGUAGE_BACK_CLICK = "language_back_click"

    /** Prefix for per-language selection; append a sanitized language code (see [languageSelectEvent]). */
    const val LANGUAGE_SELECT_PREFIX = "language_select_"

    fun languageSelectEvent(code: String): String =
        (LANGUAGE_SELECT_PREFIX + code.lowercase().replace(Regex("[^a-z0-9]"), "_")).take(40)

    // Onboarding pager
    const val ONBOARDING_SCREEN_DISPLAY = "onboarding_screen_display"
    const val ONBOARDING_PAGE1_DISPLAY = "onboarding_page1_display"
    const val ONBOARDING_PAGE2_DISPLAY = "onboarding_page2_display"
    const val ONBOARDING_PAGE3_DISPLAY = "onboarding_page3_display"
    const val ONBOARDING_PAGE4_DISPLAY = "onboarding_page4_display"
    const val ONBOARDING_SKIP_CLICK = "onboarding_skip_click"
    const val ONBOARDING_NEXT_CLICK = "onboarding_next_click"
    const val ONBOARDING_GET_STARTED_CLICK = "onboarding_get_started_click"
}
