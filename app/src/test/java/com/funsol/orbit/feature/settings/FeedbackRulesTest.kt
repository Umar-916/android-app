package com.funsol.orbit.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackRulesTest {
    @Test
    fun eachTabHasEightItems() {
        FeedbackTab.entries.forEach { assertEquals(8, FeedbackRules.itemsFor(it).size) }
    }

    @Test
    fun toggleAddsAndRemoves() {
        val added = FeedbackRules.toggle(emptySet(), FeedbackItem.ISSUE_CRASH)
        assertEquals(setOf(FeedbackItem.ISSUE_CRASH), added)
        assertEquals(emptySet<FeedbackItem>(), FeedbackRules.toggle(added!!, FeedbackItem.ISSUE_CRASH))
    }

    @Test
    fun toggleRefusesFourthSelectionButStillAllowsRemoval() {
        val full = setOf(FeedbackItem.ISSUE_CRASH, FeedbackItem.ISSUE_NOTES, FeedbackItem.ISSUE_TASKS)
        assertNull(FeedbackRules.toggle(full, FeedbackItem.ISSUE_BACKUP))
        assertEquals(2, FeedbackRules.toggle(full, FeedbackItem.ISSUE_NOTES)!!.size)
    }

    @Test
    fun submitNeedsItemOrText() {
        assertFalse(FeedbackRules.canSubmit(emptySet(), "   "))
        assertTrue(FeedbackRules.canSubmit(emptySet(), "hello"))
        assertTrue(FeedbackRules.canSubmit(setOf(FeedbackItem.IDEA_DESIGN), ""))
    }

    @Test
    fun textIsClamped() {
        val long = "a".repeat(FeedbackRules.MAX_TEXT_LENGTH + 50)
        assertEquals(FeedbackRules.MAX_TEXT_LENGTH, FeedbackRules.clampText(long).length)
    }

    @Test
    fun bodyContainsOnlyProvidedParts() {
        val body = FeedbackRules.buildBody(
            userText = "  It crashes  ",
            itemsLine = "Topics: App crashes",
            footerLines = listOf("App version: 1.0", "Android version: 14", "Device: Test Phone"),
        )
        assertEquals(
            "It crashes\n\nTopics: App crashes\n\n---\nApp version: 1.0\nAndroid version: 14\nDevice: Test Phone",
            body,
        )
    }

    @Test
    fun bodyWithoutTextOrItemsStartsWithFooter() {
        val body = FeedbackRules.buildBody("", null, listOf("App version: 1.0"))
        assertEquals("---\nApp version: 1.0", body)
    }

    @Test
    fun viewModelClearsOnTabSwitch() {
        val vm = FeedbackViewModel()
        vm.toggle(FeedbackItem.ISSUE_CRASH)
        vm.setText("abc")
        vm.selectTab(FeedbackTab.SUGGESTIONS)
        val state = vm.state.value
        assertEquals(FeedbackTab.SUGGESTIONS, state.tab)
        assertTrue(state.selected.isEmpty())
        assertEquals("", state.text)
    }

    @Test
    fun viewModelCountsLimitHits() {
        val vm = FeedbackViewModel()
        listOf(FeedbackItem.ISSUE_CRASH, FeedbackItem.ISSUE_NOTES, FeedbackItem.ISSUE_TASKS, FeedbackItem.ISSUE_BACKUP)
            .forEach(vm::toggle)
        assertEquals(3, vm.state.value.selected.size)
        assertEquals(1, vm.state.value.limitHits)
    }
}
