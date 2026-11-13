package com.funsol.orbit.feature.premium

enum class CloseSide { START, END }

data class CloseControl(val visible: Boolean, val side: CloseSide, val backEnabled: Boolean)

class PremiumClosePolicy(
    private val homeReached: Boolean,
    mode: Int = DEFAULT_ONBOARDING_MODE,
    billingAvailable: Boolean = true,
) {
    private val effectiveMode: Int = when {
        homeReached -> HOME_MODE
        mode == 0 && !billingAvailable -> 1
        mode in 0..3 -> mode
        else -> DEFAULT_ONBOARDING_MODE
    }

    val delayMs: Long = if (effectiveMode == 2 || effectiveMode == 3) CLOSE_DELAY_MS else 0L

    fun control(delayElapsed: Boolean): CloseControl = when (effectiveMode) {
        HOME_MODE -> CloseControl(visible = true, side = CloseSide.START, backEnabled = true)
        0 -> CloseControl(visible = false, side = CloseSide.END, backEnabled = false)
        1 -> CloseControl(visible = true, side = CloseSide.END, backEnabled = true)
        2 -> CloseControl(visible = delayElapsed, side = CloseSide.END, backEnabled = delayElapsed)
        else -> CloseControl(visible = delayElapsed, side = CloseSide.START, backEnabled = delayElapsed)
    }

    companion object {
        const val CLOSE_DELAY_MS = 3_000L

        const val DEFAULT_ONBOARDING_MODE = 2
        private const val HOME_MODE = -1
    }
}
