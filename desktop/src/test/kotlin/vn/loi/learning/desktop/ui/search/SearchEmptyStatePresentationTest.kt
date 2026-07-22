package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchEmptyStatePresentationTest {
    @Test
    fun sourceEmptyStateDoesNotOfferMisleadingRecovery() {
        val presentation =
            presentSearchEmptyState(
                message = "No lessons are installed.",
                noun = "lessons",
                hasSourceItems = false,
                hasQuery = false,
                hasNonQueryRefinement = false
            )

        assertFalse(presentation.showClearQuery)
        assertFalse(presentation.showResetView)
        assertTrue(presentation.contentDescription.contains("No recovery action"))
    }

    @Test
    fun queryOnlyEmptyStateOffersClearSearch() {
        val presentation =
            presentSearchEmptyState(
                message = "No lessons match.",
                noun = "lessons",
                hasSourceItems = true,
                hasQuery = true,
                hasNonQueryRefinement = false
            )

        assertTrue(presentation.showClearQuery)
        assertFalse(presentation.showResetView)
        assertTrue(presentation.contentDescription.contains("Clear the search"))
    }

    @Test
    fun combinedEmptyStateOffersBothRecoveryActions() {
        val presentation =
            presentSearchEmptyState(
                message = "No review events match.",
                noun = "review events",
                hasSourceItems = true,
                hasQuery = true,
                hasNonQueryRefinement = true
            )

        assertTrue(presentation.showClearQuery)
        assertTrue(presentation.showResetView)
        assertTrue(presentation.resetViewDescription.contains("search, filter, and sort"))
    }
}
