package vn.loi.learning.desktop.ui.search

enum class SearchKeyboardKey {
    F,
    ESCAPE,
    OTHER
}

enum class SearchKeyboardAction {
    FOCUS_SEARCH,
    CLEAR_QUERY,
    RESET_VIEW,
    NONE
}

fun resolveSearchKeyboardAction(
    key: SearchKeyboardKey,
    isKeyDown: Boolean,
    controlPressed: Boolean,
    hasQuery: Boolean,
    hasNonQueryRefinement: Boolean
): SearchKeyboardAction {
    if (!isKeyDown) return SearchKeyboardAction.NONE

    if (controlPressed && key == SearchKeyboardKey.F) {
        return SearchKeyboardAction.FOCUS_SEARCH
    }

    if (!controlPressed && key == SearchKeyboardKey.ESCAPE) {
        return when {
            hasQuery -> SearchKeyboardAction.CLEAR_QUERY
            hasNonQueryRefinement -> SearchKeyboardAction.RESET_VIEW
            else -> SearchKeyboardAction.NONE
        }
    }

    return SearchKeyboardAction.NONE
}
