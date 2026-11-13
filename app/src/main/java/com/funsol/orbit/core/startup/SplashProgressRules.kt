package com.funsol.orbit.core.startup

import com.funsol.orbit.core.ads.SplashAdState

data class SplashInputs(
    val elapsedMs: Long,
    val initDone: Boolean,
    val online: Boolean,
    val premium: Boolean,
    val adState: SplashAdState,
)

object SplashProgressRules {
    const val MIN_DISPLAY_MS = 2_200L
    const val MAX_WHILE_WAITING = 0.95f

    fun usesLongPath(online: Boolean, premium: Boolean): Boolean = online && !premium

    fun isFinished(inputs: SplashInputs, hardCapMs: Long): Boolean {
        if (inputs.elapsedMs >= hardCapMs) return true
        if (inputs.elapsedMs < MIN_DISPLAY_MS || !inputs.initDone) return false
        return if (usesLongPath(inputs.online, inputs.premium)) inputs.adState.isSettled else true
    }

    fun progress(previous: Float, inputs: SplashInputs, hardCapMs: Long): Float {
        val target = if (isFinished(inputs, hardCapMs)) {
            1f
        } else {
            val pathMs = if (usesLongPath(inputs.online, inputs.premium)) hardCapMs else MIN_DISPLAY_MS
            (inputs.elapsedMs / pathMs.toFloat()).coerceIn(0f, MAX_WHILE_WAITING)
        }
        return maxOf(previous, target)
    }
}
