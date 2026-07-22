package vn.loi.learning.desktop.ui.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchOptionPresentationTest {
    @Test
    fun selectedOptionAndGroupAreAnnounced() {
        val result = presentSearchOptionGroup(
            noun = "lessons",
            kind = SearchOptionKind.SORT,
            options = listOf("Package order", "Title", "Type"),
            selectedLabel = "Title"
        )

        assertEquals("Sort", result.heading)
        assertTrue(result.contentDescription.contains("Selected: Title"))
        assertTrue(result.contentDescription.contains("3 options"))
        assertEquals(1, result.options.count { it.selected })
        assertTrue(result.options.single { it.selected }.contentDescription.contains("selected sort"))
        assertTrue(result.options.first { !it.selected }.contentDescription.contains("Activate to select"))
    }

    @Test
    fun invalidSelectionIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            presentSearchOptionGroup("items", SearchOptionKind.FILTER, listOf("All"), "Missing")
        }
    }
}
