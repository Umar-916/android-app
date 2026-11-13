package com.funsol.orbit.feature.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumClosePolicyTest {
    @Test
    fun afterHome_backAlwaysWorks_closeAtStart_noDelay() {
        val policy = PremiumClosePolicy(homeReached = true, mode = 0)
        assertEquals(0L, policy.delayMs)
        assertEquals(CloseControl(visible = true, side = CloseSide.START, backEnabled = true), policy.control(delayElapsed = false))
    }

    @Test
    fun mode0_isHardPaywall_whenBillingWorks() {
        val control = PremiumClosePolicy(homeReached = false, mode = 0, billingAvailable = true).control(delayElapsed = true)
        assertFalse(control.visible)
        assertFalse(control.backEnabled)
    }

    @Test
    fun mode0_neverTrapsUser_whenBillingUnavailable() {
        val policy = PremiumClosePolicy(homeReached = false, mode = 0, billingAvailable = false)
        assertEquals(0L, policy.delayMs)
        val control = policy.control(delayElapsed = false)
        assertTrue(control.visible)
        assertTrue(control.backEnabled)
    }

    @Test
    fun mode1_closeAtEndImmediately() {
        val policy = PremiumClosePolicy(homeReached = false, mode = 1)
        assertEquals(0L, policy.delayMs)
        assertEquals(CloseControl(true, CloseSide.END, true), policy.control(false))
    }

    @Test
    fun mode2_closeAtEndAfterThreeSeconds() {
        val policy = PremiumClosePolicy(homeReached = false, mode = 2)
        assertEquals(3_000L, policy.delayMs)
        assertEquals(CloseControl(false, CloseSide.END, false), policy.control(false))
        assertEquals(CloseControl(true, CloseSide.END, true), policy.control(true))
    }

    @Test
    fun mode3_closeAtStartAfterThreeSeconds() {
        val policy = PremiumClosePolicy(homeReached = false, mode = 3)
        assertEquals(3_000L, policy.delayMs)
        assertEquals(CloseControl(false, CloseSide.START, false), policy.control(false))
        assertEquals(CloseControl(true, CloseSide.START, true), policy.control(true))
    }

    @Test
    fun defaultAndUnknownModes_useEndAfterDelay() {
        assertEquals(2, PremiumClosePolicy.DEFAULT_ONBOARDING_MODE)
        val unknown = PremiumClosePolicy(homeReached = false, mode = 42)
        assertEquals(CloseControl(true, CloseSide.END, true), unknown.control(true))
        assertEquals(3_000L, unknown.delayMs)
    }
}
