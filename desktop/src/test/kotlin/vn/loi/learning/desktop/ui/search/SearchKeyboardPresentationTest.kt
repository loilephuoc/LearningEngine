package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchKeyboardPresentationTest {
    @Test
    fun presentationNamesFocusAndProgressiveEscapeRecovery() {
        val presentation = presentSearchKeyboardShortcuts("lessons")

        assertEquals("Ctrl+F search · Escape recover view", presentation.visibleLabel)
        assertTrue(presentation.contentDescription.contains("Control F"))
        assertTrue(presentation.contentDescription.contains("clear the search first"))
        assertTrue(presentation.contentDescription.contains("reset filters and sorting"))
        assertTrue(presentation.contentDescription.contains("lessons"))
    }

    @Test
    fun blankNounUsesStableFallback() {
        assertTrue(
            presentSearchKeyboardShortcuts(" ").contentDescription.contains("items")
        )
    }
}
