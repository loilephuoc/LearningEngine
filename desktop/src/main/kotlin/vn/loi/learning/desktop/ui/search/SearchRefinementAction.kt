package vn.loi.learning.desktop.ui.search

enum class SearchRefinementAction {
    CLEAR_QUERY,
    RESET_FILTER,
    RESET_SORT
}

data class SearchRefinementActionPresentation(
    val action: SearchRefinementAction,
    val label: String,
    val contentDescription: String
)
