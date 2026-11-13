package com.funsol.orbit.feature.premium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumUiStateTest {
    @Test
    fun cannotPurchase_whenBillingUnavailable() {
        assertFalse(PremiumUiState(billingAvailable = false, price = "$1.99").canPurchase)
    }

    @Test
    fun cannotPurchase_withoutKnownPrice() {
        assertFalse(PremiumUiState(billingAvailable = true, price = null).canPurchase)
    }

    @Test
    fun cannotPurchase_whenAlreadyOwned() {
        assertFalse(PremiumUiState(billingAvailable = true, price = "$1.99", isPremium = true).canPurchase)
        assertFalse(PremiumUiState(billingAvailable = true, price = "$1.99", isPurchased = true).canPurchase)
    }

    @Test
    fun canPurchase_whenAvailablePricedAndNotOwned() {
        assertTrue(PremiumUiState(billingAvailable = true, price = "$1.99").canPurchase)
    }

    @Test
    fun trialOn_needsPurchasableOfferedAndSwitchedOn() {
        val base = PremiumUiState(billingAvailable = true, price = "$1.99", trialAvailable = true, trialEnabled = true)
        assertTrue(base.trialOn)
        assertFalse(base.copy(trialEnabled = false).trialOn)
        assertFalse(base.copy(trialAvailable = false).trialOn)
        assertFalse(base.copy(price = null).trialOn)
        assertFalse(base.copy(billingAvailable = false).trialOn)
    }

    @Test
    fun defaultConnectivity_isOnline() {
        assertTrue(AlwaysOnlineConnectivity.isOnline())
    }
}
