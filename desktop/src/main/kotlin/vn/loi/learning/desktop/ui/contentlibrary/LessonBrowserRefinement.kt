package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.SearchRefinementPresentation
import vn.loi.learning.desktop.ui.search.SearchRefinementState
import vn.loi.learning.desktop.ui.search.presentSearchRefinements

fun LessonBrowserUiState.refinementState(): SearchRefinementState =
    SearchRefinementState(
        hasQuery = query.isNotBlank(),
        hasFilter = filter != LessonBrowserFilter.ALL,
        hasSort = sort != LessonBrowserSort.PACKAGE_ORDER
    )

fun lessonBrowserRefinementPresentation(
    state: LessonBrowserUiState
): SearchRefinementPresentation =
    presentSearchRefinements(state.refinementState(), "lessons")
