package vn.loi.learning.desktop.ui.search

data class SearchRefinementPresentation(
    val label: String,
    val contentDescription: String,
    val resetDescription: String
)

fun presentSearchRefinements(
    state: SearchRefinementState,
    noun: String
): SearchRefinementPresentation {
    if (state.isDefault) {
        return SearchRefinementPresentation(
            label = "Default view",
            contentDescription = "$noun use the default search, filter, and sort settings.",
            resetDescription = "Reset $noun search, filter, and sort. No refinements are active."
        )
    }

    val parts = buildList {
        if (state.hasQuery) add("search query")
        if (state.hasFilter) add("filter")
        if (state.hasSort) add("sort order")
    }
    val joined = when (parts.size) {
        0 -> ""
        1 -> parts.single()
        2 -> parts.joinToString(separator = " and ")
        else -> parts.dropLast(1).joinToString(separator = ", ") + " and " + parts.last()
    }
    val countLabel = if (state.activeCount == 1) "1 refinement" else "${state.activeCount} refinements"

    return SearchRefinementPresentation(
        label = "$countLabel active",
        contentDescription = "$countLabel active for $noun: $joined.",
        resetDescription = "Reset $noun $joined to the default view."
    )
}
