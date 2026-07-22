package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.SearchResultStatusPresentation
import vn.loi.learning.desktop.ui.search.presentSearchResultStatus

fun lessonBrowserResultStatus(
    state: LessonBrowserUiState
): SearchResultStatusPresentation =
    presentSearchResultStatus(
        summary = lessonBrowserSearchSummary(state),
        noun = "lessons",
        hasNonQueryRefinement =
            state.filter != LessonBrowserFilter.ALL ||
                state.sort != LessonBrowserSort.PACKAGE_ORDER
    )
