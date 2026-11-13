package com.funsol.orbit.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class RateActionTest {
    @Test
    fun noRating_doesNothing() {
        assertEquals(RateAction.NONE, rateActionFor(0))
    }

    @Test
    fun oneToThreeStars_goToFeedbackStep() {
        (1..3).forEach { assertEquals(RateAction.FEEDBACK, rateActionFor(it)) }
    }

    @Test
    fun fourAndFiveStars_openTheStore() {
        assertEquals(RateAction.STORE, rateActionFor(4))
        assertEquals(RateAction.STORE, rateActionFor(5))
    }
}
