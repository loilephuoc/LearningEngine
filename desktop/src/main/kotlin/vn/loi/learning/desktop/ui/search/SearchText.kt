package vn.loi.learning.desktop.ui.search

fun String.containsSearchQuery(
    query: String
): Boolean {
    val normalizedQuery = query.trim()

    return normalizedQuery.isBlank() ||
        contains(
            other = normalizedQuery,
            ignoreCase = true
        )
}
