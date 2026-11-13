package com.funsol.orbit.core.startup

import com.funsol.orbit.core.ads.SplashAdState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashProgressRulesTest {
    private val cap = 10_000L

    private fun inputs(
        elapsed: Long,
        initDone: Boolean = true,
        online: Boolean = true,
        premium: Boolean = false,
        ad: SplashAdState = SplashAdState.NOT_APPLICABLE,
    ) = SplashInputs(elapsed, initDone, online, premium, ad)

    @Test
    fun longPath_onlyWhenOnlineAndNotPremium() {
        assertTrue(SplashProgressRules.usesLongPath(online = true, premium = false))
        assertFalse(SplashProgressRules.usesLongPath(online = false, premium = false))
        assertFalse(SplashProgressRules.usesLongPath(online = true, premium = true))
    }

    @Test
    fun neverFinishesBeforeTheMinimumDisplayTime() {
        assertFalse(SplashProgressRules.isFinished(inputs(2_000, online = false), cap))
        assertTrue(SplashProgressRules.isFinished(inputs(SplashProgressRules.MIN_DISPLAY_MS, online = false), cap))
    }

    @Test
    fun shortPath_finishesAsSoonAsLocalInitIsDone_evenIfAnAdIsStillLoading() {
        assertTrue(SplashProgressRules.isFinished(inputs(2_300, online = false, ad = SplashAdState.LOADING), cap))
        assertTrue(SplashProgressRules.isFinished(inputs(2_300, premium = true, ad = SplashAdState.LOADING), cap))
    }

    @Test
    fun longPath_waitsForTheSplashAdToSettle() {
        assertFalse(SplashProgressRules.isFinished(inputs(3_000, ad = SplashAdState.LOADING), cap))
        assertTrue(SplashProgressRules.isFinished(inputs(3_000, ad = SplashAdState.LOADED), cap))
        assertTrue(SplashProgressRules.isFinished(inputs(3_000, ad = SplashAdState.FAILED), cap))
        assertTrue(SplashProgressRules.isFinished(inputs(3_000, ad = SplashAdState.NOT_APPLICABLE), cap))
    }

    @Test
    fun waitsForInitialisation_untilTheHardCap() {
        assertFalse(SplashProgressRules.isFinished(inputs(5_000, initDone = false), cap))
        assertTrue(SplashProgressRules.isFinished(inputs(cap, initDone = false, ad = SplashAdState.LOADING), cap))
    }

    @Test
    fun hardCap_isMaxWaitPlusThreeSeconds() {
        assertEquals(10_000L, com.funsol.orbit.core.remote.AdConfig.DEFAULT.splashHardCapMs)
    }

    @Test
    fun progress_isMonotonic_cappedWhileWaiting_andJumpsToFullWhenFinished() {
        val waiting = inputs(9_000, ad = SplashAdState.LOADING)
        val p1 = SplashProgressRules.progress(0f, waiting, cap)
        assertTrue(p1 <= SplashProgressRules.MAX_WHILE_WAITING)
        assertEquals(p1, SplashProgressRules.progress(p1, inputs(1_000, ad = SplashAdState.LOADING), cap), 0f)
        assertEquals(1f, SplashProgressRules.progress(p1, inputs(3_000, ad = SplashAdState.LOADED), cap), 0f)
    }

    @Test
    fun shortPath_barIsPacedOverTheMinimumDisplayTime() {
        val half = SplashProgressRules.progress(0f, inputs(1_100, initDone = false, online = false), cap)
        assertEquals(0.5f, half, 0.001f)
    }
}
