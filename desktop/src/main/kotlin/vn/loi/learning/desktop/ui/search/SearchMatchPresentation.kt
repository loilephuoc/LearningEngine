package vn.loi.learning.desktop.ui.search

data class SearchMatchRange(
    val start: Int,
    val endExclusive: Int
) {
    init {
        require(start >= 0) { "start must be non-negative" }
        require(endExclusive > start) { "endExclusive must be greater than start" }
    }
}

data class SearchMatchPresentation(
    val text: String,
    val ranges: List<SearchMatchRange>,
    val contentDescription: String
) {
    val hasMatches: Boolean
        get() = ranges.isNotEmpty()
}

fun presentSearchMatches(
    text: String,
    query: String
): SearchMatchPresentation {
    val normalizedQuery = query.trim()
    if (text.isEmpty() || normalizedQuery.isEmpty()) {
        return SearchMatchPresentation(
            text = text,
            ranges = emptyList(),
            contentDescription = text
        )
    }

    val ranges = buildList {
        var startIndex = 0
        while (startIndex <= text.length - normalizedQuery.length) {
            val matchIndex = text.indexOf(
                string = normalizedQuery,
                startIndex = startIndex,
                ignoreCase = true
            )
            if (matchIndex < 0) break
            add(SearchMatchRange(matchIndex, matchIndex + normalizedQuery.length))
            startIndex = matchIndex + normalizedQuery.length
        }
    }

    val description =
        if (ranges.isEmpty()) text
        else "$text. ${ranges.size} search ${if (ranges.size == 1) "match" else "matches"}."

    return SearchMatchPresentation(
        text = text,
        ranges = ranges,
        contentDescription = description
    )
}
