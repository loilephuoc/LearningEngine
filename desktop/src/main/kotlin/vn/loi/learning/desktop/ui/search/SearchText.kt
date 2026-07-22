package vn.loi.learning.desktop.ui.search

fun String.containsSearchQuery(
    query: String
): Boolean {
    val parsed = parseSearchQuery(query)
    if (parsed.isBlank) return true

    val searchableText =
        canonicalizeSearchText(this)
            .value

    return parsed.terms.all { term ->
        searchableText.contains(
            other = term,
            ignoreCase = true
        )
    }
}
