package vn.loi.learning.desktop.ui.reviewhistory

import vn.loi.learning.desktop.ui.search.containsSearchQuery

fun projectReviewHistory(
    items: List<ReviewHistoryItemUi>,
    query: String,
    filter: ReviewHistoryFilter,
    sort: ReviewHistorySort
): List<ReviewHistoryItemUi> {
    val filtered =
        items.filter { item ->
            filter.matches(item) &&
                listOf(
                    item.reviewedAt,
                    item.rating,
                    item.responseTime,
                    item.stability,
                    item.difficulty
                ).any { searchableText ->
                    searchableText.containsSearchQuery(query)
                }
        }

    return when (sort) {
        ReviewHistorySort.NEWEST ->
            filtered

        ReviewHistorySort.OLDEST ->
            filtered.asReversed()

        ReviewHistorySort.RATING ->
            filtered.sortedBy {
                it.rating.lowercase()
            }

        ReviewHistorySort.RESPONSE_TIME ->
            filtered.sortedBy {
                it.responseTime.lowercase()
            }
    }
}
