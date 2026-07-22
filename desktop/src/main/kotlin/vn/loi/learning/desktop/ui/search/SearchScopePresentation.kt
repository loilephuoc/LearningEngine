package vn.loi.learning.desktop.ui.search

data class SearchScopePresentation(
    val heading: String,
    val fields: List<String>,
    val activeSummary: String,
    val contentDescription: String
)

fun presentSearchScope(
    noun: String,
    fields: List<String>,
    query: String,
    filterLabel: String,
    sortLabel: String
): SearchScopePresentation {
    require(noun.isNotBlank()) { "noun must not be blank" }
    val normalizedFields = fields.map(String::trim).filter(String::isNotEmpty).distinct()
    require(normalizedFields.isNotEmpty()) { "fields must not be empty" }

    val normalizedQuery = query.trim()
    val querySummary =
        if (normalizedQuery.isEmpty()) "No text query"
        else "Query: $normalizedQuery"
    val activeSummary = "$querySummary · Filter: $filterLabel · Sort: $sortLabel"
    val fieldSummary = normalizedFields.joinToString(", ")

    return SearchScopePresentation(
        heading = "Search scope",
        fields = normalizedFields,
        activeSummary = activeSummary,
        contentDescription =
            "Search $noun across $fieldSummary. $querySummary. " +
                "Filter $filterLabel. Sort $sortLabel."
    )
}
