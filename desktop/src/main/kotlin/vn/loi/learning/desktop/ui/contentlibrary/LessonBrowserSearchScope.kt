package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.SearchScopePresentation
import vn.loi.learning.desktop.ui.search.presentSearchScope

fun lessonBrowserSearchScope(state: LessonBrowserUiState): SearchScopePresentation =
    presentSearchScope(
        noun = "lessons",
        fields = listOf("Hierarchy", "Title", "Primary text", "Translation"),
        query = state.query,
        filterLabel = state.filter.label,
        sortLabel = state.sort.label
    )
