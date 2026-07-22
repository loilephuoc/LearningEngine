package vn.loi.learning.desktop.ui.search

data class SearchQueryTerms(
    val normalizedQuery: String,
    val terms: List<String>
) {
    val isBlank: Boolean
        get() = terms.isEmpty()

    val isMultiTerm: Boolean
        get() = terms.size > 1
}

fun parseSearchQuery(query: String): SearchQueryTerms {
    val terms =
        query
            .trim()
            .split(Regex("\\s+"))
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { it.lowercase() }

    return SearchQueryTerms(
        normalizedQuery = terms.joinToString(" "),
        terms = terms
    )
}
