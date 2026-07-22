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
    val parsed = parseSearchQuery(query)
    if (text.isEmpty() || parsed.isBlank) {
        return SearchMatchPresentation(
            text = text,
            ranges = emptyList(),
            contentDescription = text
        )
    }

    val searchableText =
        canonicalizeSearchText(text)

    val ranges =
        parsed.terms
            .flatMap(searchableText::findRanges)
            .sortedWith(
                compareBy(
                    SearchMatchRange::start,
                    SearchMatchRange::endExclusive
                )
            )
            .mergeOverlappingRanges()

    val description =
        if (ranges.isEmpty()) {
            text
        } else {
            "$text. ${ranges.size} search " +
                if (ranges.size == 1) {
                    "match."
                } else {
                    "matches."
                }
        }

    return SearchMatchPresentation(
        text = text,
        ranges = ranges,
        contentDescription = description
    )
}

private fun List<SearchMatchRange>.mergeOverlappingRanges():
    List<SearchMatchRange> {
    if (isEmpty()) return emptyList()

    return buildList {
        for (range in this@mergeOverlappingRanges) {
            val previous = lastOrNull()

            if (
                previous == null ||
                range.start >= previous.endExclusive
            ) {
                add(range)
            } else {
                removeAt(lastIndex)
                add(
                    SearchMatchRange(
                        start = previous.start,
                        endExclusive =
                            maxOf(
                                previous.endExclusive,
                                range.endExclusive
                            )
                    )
                )
            }
        }
    }
}
