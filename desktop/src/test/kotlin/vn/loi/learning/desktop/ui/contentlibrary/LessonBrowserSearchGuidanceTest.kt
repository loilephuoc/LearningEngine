package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals

class LessonBrowserSearchGuidanceTest {
    @Test
    fun `maps lesson search examples and query`() {
        val result = lessonBrowserSearchGuidance(LessonBrowserUiState(query = "travel"))

        assertEquals("Try lesson title", result.placeholder)
        assertEquals("Searching lessons for “travel”.", result.supportingText)
    }

    @Test
    fun `empty lesson query exposes all supported examples`() {
        val result = lessonBrowserSearchGuidance(LessonBrowserUiState())

        assertEquals("Try lesson title, hierarchy, translation", result.supportingText)
    }
}
