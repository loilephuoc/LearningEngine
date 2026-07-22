package vn.loi.learning.desktop.ui.search

enum class SearchOptionKind(val label: String) {
    FILTER("Filter"),
    SORT("Sort")
}

data class SearchOptionPresentation(
    val label: String,
    val selected: Boolean,
    val contentDescription: String
)

data class SearchOptionGroupPresentation(
    val heading: String,
    val contentDescription: String,
    val options: List<SearchOptionPresentation>
)

fun presentSearchOptionGroup(
    noun: String,
    kind: SearchOptionKind,
    options: List<String>,
    selectedLabel: String
): SearchOptionGroupPresentation {
    val normalizedNoun = noun.trim().ifBlank { "items" }
    val normalizedOptions = options.map(String::trim).filter(String::isNotBlank).distinct()
    require(normalizedOptions.isNotEmpty()) { "Search option group must contain at least one option." }
    require(selectedLabel in normalizedOptions) { "Selected option must belong to the option group." }

    val presentations = normalizedOptions.map { label ->
        val selected = label == selectedLabel
        SearchOptionPresentation(
            label = label,
            selected = selected,
            contentDescription =
                if (selected) "$label, selected ${kind.label.lowercase()} for $normalizedNoun."
                else "$label, ${kind.label.lowercase()} $normalizedNoun. Activate to select."
        )
    }
    return SearchOptionGroupPresentation(
        heading = kind.label,
        contentDescription = "${kind.label} $normalizedNoun. Selected: $selectedLabel. ${presentations.size} options.",
        options = presentations
    )
}
