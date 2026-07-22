package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.SearchQueryGuidancePresentation
import vn.loi.learning.desktop.ui.search.presentSearchQueryGuidance

fun lessonBrowserSearchGuidance(state: LessonBrowserUiState): SearchQueryGuidancePresentation =
    presentSearchQueryGuidance(
        noun = "lessons",
        examples = listOf("lesson title", "hierarchy", "translation"),
        query = state.query
    )
