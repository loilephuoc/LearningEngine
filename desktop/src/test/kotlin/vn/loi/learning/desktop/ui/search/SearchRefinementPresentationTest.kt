package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchRefinementPresentationTest {
    @Test
    fun defaultStateHasNoActiveRefinements() {
        val state = SearchRefinementState(false, false, false)
        val presentation = presentSearchRefinements(state, "lessons")

        assertTrue(state.isDefault)
        assertEquals(0, state.activeCount)
        assertEquals("Default view", presentation.label)
        assertTrue(presentation.actions.isEmpty())
        assertTrue(presentation.resetDescription.contains("No refinements are active"))
    }

    @Test
    fun presentationNamesEveryActiveRefinement() {
        val state = SearchRefinementState(true, true, true)
        val presentation = presentSearchRefinements(state, "review history")

        assertFalse(state.isDefault)
        assertEquals(3, state.activeCount)
        assertEquals("3 refinements active", presentation.label)
        assertEquals(3, presentation.actions.size)
        assertTrue(presentation.contentDescription.contains("search query, filter and sort order"))
        assertTrue(presentation.contentDescription.contains("removed independently"))
    }
}
