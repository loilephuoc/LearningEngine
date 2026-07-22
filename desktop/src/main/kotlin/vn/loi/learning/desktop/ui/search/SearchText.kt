package vn.loi.learning.desktop.ui.search

fun String.containsSearchQuery(
    query: String
): Boolean {
    val parsed = parseSearchQuery(query)

    return parsed.isBlank ||
        parsed.terms.all { term ->
            contains(
                other = term,
                ignoreCase = true
            )
        }
}
