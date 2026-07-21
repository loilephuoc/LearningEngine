package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.SearchResultSummary

fun lessonBrowserSearchSummary(state: LessonBrowserUiState)=SearchResultSummary(state.visibleLessons.size,state.lessons.size,state.query)
fun lessonBrowserEmptySearchMessage(state: LessonBrowserUiState): String = when {
 state.lessons.isEmpty()->"This library has no learning content."
 state.query.isNotBlank()->"No lessons match \"${state.query.trim()}\". Clear the search or change the filter."
 state.filter!=LessonBrowserFilter.ALL->"No lessons match the ${state.filter.label.lowercase()} filter."
 else->"No lessons are available."
}
