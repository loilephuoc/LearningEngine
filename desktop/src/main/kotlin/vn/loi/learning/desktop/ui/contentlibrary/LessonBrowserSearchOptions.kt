package vn.loi.learning.desktop.ui.contentlibrary

import vn.loi.learning.desktop.ui.search.SearchOptionGroupPresentation
import vn.loi.learning.desktop.ui.search.SearchOptionKind
import vn.loi.learning.desktop.ui.search.presentSearchOptionGroup

fun lessonBrowserFilterPresentation(state: LessonBrowserUiState): SearchOptionGroupPresentation =
    presentSearchOptionGroup(
        noun = "lessons",
        kind = SearchOptionKind.FILTER,
        options = LessonBrowserFilter.entries.map { it.label },
        selectedLabel = state.filter.label
    )

fun lessonBrowserSortPresentation(state: LessonBrowserUiState): SearchOptionGroupPresentation =
    presentSearchOptionGroup(
        noun = "lessons",
        kind = SearchOptionKind.SORT,
        options = LessonBrowserSort.entries.map { it.label },
        selectedLabel = state.sort.label
    )
