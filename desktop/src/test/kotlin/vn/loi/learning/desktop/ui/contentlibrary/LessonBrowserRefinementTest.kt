package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonBrowserRefinementTest {
    @Test
    fun defaultsAreNotRefined() {
        assertTrue(LessonBrowserUiState().refinementState().isDefault)
    }

    @Test
    fun nonDefaultControlsAreAnnounced() {
        val state = LessonBrowserUiState(
            query = "travel",
            filter = LessonBrowserFilter.WITH_TRANSLATION,
            sort = LessonBrowserSort.TITLE
        )

        assertEquals(3, state.refinementState().activeCount)
        assertTrue(lessonBrowserRefinementPresentation(state).resetDescription.contains("search query, filter and sort order"))
    }
}
