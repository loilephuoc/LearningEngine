package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.SearchEmptyStatePresentation
import vn.loi.learning.desktop.ui.search.SearchResultSummary
import vn.loi.learning.desktop.ui.search.presentSearchEmptyState

fun lessonBrowserSearchSummary(state: LessonBrowserUiState) =
    SearchResultSummary(
        visibleCount = state.visibleLessons.size,
        totalCount = state.lessons.size,
        query = state.query
    )

fun lessonBrowserEmptySearchMessage(state: LessonBrowserUiState): String =
    when {
        state.lessons.isEmpty() ->
            "This library has no learning content."

        state.query.isNotBlank() ->
            "No lessons match \"${state.query.trim()}\". Clear the search or change the filter."

        state.filter != LessonBrowserFilter.ALL ->
            "No lessons match the ${state.filter.label.lowercase()} filter."

        else ->
            "No lessons are available."
    }

fun lessonBrowserEmptySearchPresentation(
    state: LessonBrowserUiState
): SearchEmptyStatePresentation =
    presentSearchEmptyState(
        message = lessonBrowserEmptySearchMessage(state),
        noun = "lessons",
        hasSourceItems = state.lessons.isNotEmpty(),
        hasQuery = state.query.isNotBlank(),
        hasNonQueryRefinement =
            state.filter != LessonBrowserFilter.ALL ||
                state.sort != LessonBrowserSort.PACKAGE_ORDER
    )
