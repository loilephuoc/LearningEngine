package vn.loi.learning.desktop.ui.search

data class SearchKeyboardPresentation(
    val visibleLabel: String,
    val contentDescription: String
)

fun presentSearchKeyboardShortcuts(noun: String): SearchKeyboardPresentation {
    val normalizedNoun = noun.trim().ifBlank { "items" }
    return SearchKeyboardPresentation(
        visibleLabel = "Ctrl+F search · Escape recover view",
        contentDescription =
            "Search shortcuts for $normalizedNoun. " +
                "Press Control F to focus search. " +
                "Press Escape to clear the search first, then reset filters and sorting."
    )
}
