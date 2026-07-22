package vn.loi.learning.desktop.ui.search

data class SearchQueryGuidancePresentation(
    val placeholder: String,
    val supportingText: String,
    val contentDescription: String
)

fun presentSearchQueryGuidance(
    noun: String,
    examples: List<String>,
    query: String
): SearchQueryGuidancePresentation {
    val normalizedNoun = noun.trim().lowercase()
    require(normalizedNoun.isNotEmpty()) { "Search noun must not be blank." }

    val normalizedExamples = examples
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
    require(normalizedExamples.isNotEmpty()) { "At least one search example is required." }

    val normalizedQuery = query.trim()
    val exampleText = normalizedExamples.joinToString(", ")
    val supportingText = when {
        normalizedQuery.isEmpty() -> "Try $exampleText"
        normalizedQuery.length == 1 -> "Add another character for a more specific match."
        else -> "Searching $normalizedNoun for “$normalizedQuery”."
    }

    return SearchQueryGuidancePresentation(
        placeholder = "Try ${normalizedExamples.first()}",
        supportingText = supportingText,
        contentDescription = when {
            normalizedQuery.isEmpty() -> "Search guidance for $normalizedNoun. Try $exampleText."
            normalizedQuery.length == 1 -> "Search query has one character. Add another character for a more specific match."
            else -> "Searching $normalizedNoun for $normalizedQuery."
        }
    )
}
