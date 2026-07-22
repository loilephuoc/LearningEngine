package vn.loi.learning.desktop.ui.search

data class SearchRefinementPresentation(
    val label: String,
    val contentDescription: String,
    val resetDescription: String,
    val actions: List<SearchRefinementActionPresentation>
)

fun presentSearchRefinements(
    state: SearchRefinementState,
    noun: String
): SearchRefinementPresentation {
    val normalizedNoun = noun.trim().ifBlank { "items" }
    if (state.isDefault) {
        return SearchRefinementPresentation(
            label = "Default view",
            contentDescription = "$normalizedNoun use the default search, filter, and sort settings.",
            resetDescription = "Reset $normalizedNoun search, filter, and sort. No refinements are active.",
            actions = emptyList()
        )
    }

    val parts = buildList {
        if (state.hasQuery) add("search query")
        if (state.hasFilter) add("filter")
        if (state.hasSort) add("sort order")
    }
    val joined = joinRefinementNames(parts)
    val countLabel = if (state.activeCount == 1) "1 refinement" else "${state.activeCount} refinements"
    val actions = buildList {
        if (state.hasQuery) {
            add(
                SearchRefinementActionPresentation(
                    action = SearchRefinementAction.CLEAR_QUERY,
                    label = "Clear search",
                    contentDescription = "Clear the $normalizedNoun search query while preserving the current filter and sort order."
                )
            )
        }
        if (state.hasFilter) {
            add(
                SearchRefinementActionPresentation(
                    action = SearchRefinementAction.RESET_FILTER,
                    label = "Clear filter",
                    contentDescription = "Restore the default $normalizedNoun filter while preserving the search query and sort order."
                )
            )
        }
        if (state.hasSort) {
            add(
                SearchRefinementActionPresentation(
                    action = SearchRefinementAction.RESET_SORT,
                    label = "Default sort",
                    contentDescription = "Restore the default $normalizedNoun sort order while preserving the search query and filter."
                )
            )
        }
    }

    return SearchRefinementPresentation(
        label = "$countLabel active",
        contentDescription = "$countLabel active for $normalizedNoun: $joined. Each refinement can be removed independently.",
        resetDescription = "Reset $normalizedNoun $joined to the default view.",
        actions = actions
    )
}

private fun joinRefinementNames(parts: List<String>): String =
    when (parts.size) {
        0 -> ""
        1 -> parts.single()
        2 -> parts.joinToString(separator = " and ")
        else -> parts.dropLast(1).joinToString(separator = ", ") + " and " + parts.last()
    }
