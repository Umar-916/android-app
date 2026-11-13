package com.funsol.orbit.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class BannerWidthTest {
    @Test
    fun cardWidth_isScreenMinusBothInsets() {
        assertEquals(336, bannerCardWidthDp(360))
        assertEquals(387, bannerCardWidthDp(411))
    }

    @Test
    fun cardWidth_neverBelowMinimum() {
        assertEquals(120, bannerCardWidthDp(100))
        assertEquals(120, bannerCardWidthDp(0))
    }

    @Test
    fun fullScreenSlot_isExclusiveAndReleasable() {
        val state = FullScreenAdState()
        assertEquals(true, state.tryEnter())
        assertEquals(false, state.tryEnter())
        state.leave()
        assertEquals(true, state.tryEnter())
    }
}
