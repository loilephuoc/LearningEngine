package vn.loi.learning.desktop.ui.search

import java.util.Locale

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
        canonicalizeSearchText(query)
            .value
            .trim()
            .split(Regex("\\s+"))
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinctBy { term ->
                term.lowercase(Locale.ROOT)
            }

    return SearchQueryTerms(
        normalizedQuery = terms.joinToString(" "),
        terms = terms
    )
}
