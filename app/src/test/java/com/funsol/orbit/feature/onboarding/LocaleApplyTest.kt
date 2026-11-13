package com.funsol.orbit.feature.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleApplyTest {
    @Test
    fun followingDevice_andChoosingDeviceLanguage_doesNotRecreate() {
        assertFalse(shouldApplyLocale(appliedTags = "", targetCode = "en", suggestedCode = "en"))
    }

    @Test
    fun followingDevice_andChoosingAnotherLanguage_applies() {
        assertTrue(shouldApplyLocale(appliedTags = "", targetCode = "ur", suggestedCode = "en"))
    }

    @Test
    fun alreadyApplied_sameLanguage_doesNotApplyAgain() {
        assertFalse(shouldApplyLocale(appliedTags = "ur", targetCode = "ur", suggestedCode = "en"))
        assertFalse(shouldApplyLocale(appliedTags = "pt-BR", targetCode = "pt-BR", suggestedCode = "en"))
    }

    @Test
    fun alreadyApplied_differentLanguage_applies() {
        assertTrue(shouldApplyLocale(appliedTags = "ur", targetCode = "es", suggestedCode = "en"))
    }
}
