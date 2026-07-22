package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchRefinementActionTest {
    @Test
    fun activeRefinementsProduceIndependentActionsInVisualOrder() {
        val presentation = presentSearchRefinements(
            state = SearchRefinementState(hasQuery = true, hasFilter = true, hasSort = true),
            noun = "lessons"
        )

        assertEquals(
            listOf(
                SearchRefinementAction.CLEAR_QUERY,
                SearchRefinementAction.RESET_FILTER,
                SearchRefinementAction.RESET_SORT
            ),
            presentation.actions.map { it.action }
        )
        assertEquals(listOf("Clear search", "Clear filter", "Default sort"), presentation.actions.map { it.label })
    }

    @Test
    fun inactiveRefinementsDoNotOfferMisleadingActions() {
        val presentation = presentSearchRefinements(
            state = SearchRefinementState(hasQuery = false, hasFilter = true, hasSort = false),
            noun = "review history"
        )

        assertEquals(listOf(SearchRefinementAction.RESET_FILTER), presentation.actions.map { it.action })
    }
}
