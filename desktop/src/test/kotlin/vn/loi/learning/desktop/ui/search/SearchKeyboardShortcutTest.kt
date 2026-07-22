package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchKeyboardShortcutTest {
    @Test
    fun controlFFocusesSearch() {
        assertEquals(
            SearchKeyboardAction.FOCUS_SEARCH,
            resolveSearchKeyboardAction(
                key = SearchKeyboardKey.F,
                isKeyDown = true,
                controlPressed = true,
                hasQuery = false,
                hasNonQueryRefinement = false
            )
        )
    }

    @Test
    fun escapeClearsQueryBeforeResettingOtherRefinements() {
        assertEquals(
            SearchKeyboardAction.CLEAR_QUERY,
            resolveSearchKeyboardAction(
                key = SearchKeyboardKey.ESCAPE,
                isKeyDown = true,
                controlPressed = false,
                hasQuery = true,
                hasNonQueryRefinement = true
            )
        )
        assertEquals(
            SearchKeyboardAction.RESET_VIEW,
            resolveSearchKeyboardAction(
                key = SearchKeyboardKey.ESCAPE,
                isKeyDown = true,
                controlPressed = false,
                hasQuery = false,
                hasNonQueryRefinement = true
            )
        )
    }

    @Test
    fun irrelevantAndKeyUpEventsAreNotConsumed() {
        assertEquals(
            SearchKeyboardAction.NONE,
            resolveSearchKeyboardAction(
                key = SearchKeyboardKey.F,
                isKeyDown = false,
                controlPressed = true,
                hasQuery = true,
                hasNonQueryRefinement = true
            )
        )
        assertEquals(
            SearchKeyboardAction.NONE,
            resolveSearchKeyboardAction(
                key = SearchKeyboardKey.OTHER,
                isKeyDown = true,
                controlPressed = false,
                hasQuery = true,
                hasNonQueryRefinement = true
            )
        )
    }
}
