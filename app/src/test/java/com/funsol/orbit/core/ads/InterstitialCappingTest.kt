package com.funsol.orbit.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstitialCappingTest {
    private var now = 1_000_000L
    private val capping = InterstitialCapping { now }

    @Test
    fun freshProcess_needsOnlyTheActionCountForNavigation() {
        repeat(3) { capping.recordAction() }
        assertFalse(capping.canShowAfterAction(everyN = 4, minIntervalSeconds = 90))
        capping.recordAction()
        assertTrue(capping.canShowAfterAction(everyN = 4, minIntervalSeconds = 90))
    }

    @Test
    fun splash_needsOnlyTheInterval() {
        assertTrue(capping.canShowByInterval(90))
        capping.recordShown()
        assertFalse(capping.canShowByInterval(90))
        now += 90_000L
        assertTrue(capping.canShowByInterval(90))
    }

    @Test
    fun afterAShow_bothCountersReset() {
        repeat(4) { capping.recordAction() }
        capping.recordShown()
        assertEquals(0, capping.actions())
        repeat(4) { capping.recordAction() }
        assertFalse(capping.canShowAfterAction(4, 90))
        now += 89_999L
        assertFalse(capping.canShowAfterAction(4, 90))
        now += 1L
        assertTrue(capping.canShowAfterAction(4, 90))
    }

    @Test
    fun intervalZero_meansNoTimeLimit_andEveryNIsAtLeastOne() {
        capping.recordShown()
        assertTrue(capping.canShowByInterval(0))
        capping.recordAction()
        assertTrue(capping.canShowAfterAction(everyN = 0, minIntervalSeconds = 0))
    }
}
