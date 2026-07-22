package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessonBrowserSearchScopeTest {
    @Test
    fun `lesson browser exposes every searchable lesson field`() {
        val result = lessonBrowserSearchScope(LessonBrowserUiState(query = "alpha"))

        assertEquals(listOf("Hierarchy", "Title", "Primary text", "Translation"), result.fields)
        assertTrue(result.activeSummary.startsWith("Query: alpha"))
        assertTrue(result.contentDescription.startsWith("Search lessons across"))
    }
}
