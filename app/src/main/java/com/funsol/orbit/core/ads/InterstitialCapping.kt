package com.funsol.orbit.core.ads

class InterstitialCapping(private val clock: () -> Long = { System.nanoTime() / 1_000_000L }) {
    private var lastShownAtMs: Long = NEVER
    private var actionsSinceShown: Int = 0

    @Synchronized
    fun recordAction() {
        if (actionsSinceShown < Int.MAX_VALUE) actionsSinceShown++
    }

    @Synchronized
    fun recordShown() {
        lastShownAtMs = clock()
        actionsSinceShown = 0
    }

    @Synchronized
    fun canShowAfterAction(everyN: Int, minIntervalSeconds: Int): Boolean =
        actionsSinceShown >= everyN.coerceAtLeast(1) && intervalElapsed(minIntervalSeconds)

    @Synchronized
    fun canShowByInterval(minIntervalSeconds: Int): Boolean = intervalElapsed(minIntervalSeconds)

    @Synchronized
    fun actions(): Int = actionsSinceShown

    private fun intervalElapsed(minIntervalSeconds: Int): Boolean {
        if (lastShownAtMs == NEVER) return true
        return clock() - lastShownAtMs >= minIntervalSeconds.coerceAtLeast(0) * 1_000L
    }

    private companion object {
        const val NEVER = Long.MIN_VALUE
    }
}
