package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonBrowserResultStatusTest {
    @Test
    fun filterMarksStatusAsRefined() {
        val state = LessonBrowserUiState(filter = LessonBrowserFilter.entries.first { it != LessonBrowserFilter.ALL })
        val result = lessonBrowserResultStatus(state)
        assertTrue(result.isFiltered)
        assertEquals("No lessons yet", result.label)
    }
}
