package vn.loi.learning.desktop.ui.contentlibrary

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LessonBrowserEmptySearchPresentationTest {
    private val lesson =
        LessonBrowserItem(
            id = "lesson-1",
            title = "Greeting",
            type = "lesson",
            group = null,
            section = null,
            lesson = null,
            primaryText = "Hello",
            translatedText = "Xin chao",
            learningItemCount = 1
        )

    @Test
    fun filterOnlyEmptyStateOffersResetView() {
        val state =
            LessonBrowserUiState(
                lessons = listOf(lesson),
                filter = LessonBrowserFilter.WITHOUT_TRANSLATION
            )

        val presentation = lessonBrowserEmptySearchPresentation(state)

        assertFalse(presentation.showClearQuery)
        assertTrue(presentation.showResetView)
    }

    @Test
    fun emptyLibraryDoesNotOfferSearchRecovery() {
        val presentation =
            lessonBrowserEmptySearchPresentation(LessonBrowserUiState())

        assertFalse(presentation.showClearQuery)
        assertFalse(presentation.showResetView)
    }
}
