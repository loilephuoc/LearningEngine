package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonBrowserSearchOptionsTest {
    @Test
    fun activeLessonFilterAndSortAreNamed() {
        val state = lessonBrowserState().copy(
            filter = LessonBrowserFilter.WITH_TRANSLATION,
            sort = LessonBrowserSort.ITEM_COUNT
        )

        val filter = lessonBrowserFilterPresentation(state)
        val sort = lessonBrowserSortPresentation(state)

        assertEquals("With translation", filter.options.single { it.selected }.label)
        assertEquals("Item count", sort.options.single { it.selected }.label)
        assertTrue(filter.contentDescription.contains("review").not())
        assertTrue(sort.contentDescription.contains("lessons"))
    }

    private fun lessonBrowserState() = LessonBrowserUiState(
        libraryName = "Library",
        lessons = emptyList(),
        selectedLessonId = null
    )
}
