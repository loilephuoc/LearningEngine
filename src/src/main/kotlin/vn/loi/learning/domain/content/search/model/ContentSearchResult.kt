package vn.loi.learning.domain.content.search.model

import vn.loi.learning.domain.content.model.Content

/**
 * Kết quả tìm kiếm Content theo thứ tự phù hợp do search service quyết định.
 */
data class ContentSearchResult(
    val contents: List<Content> = emptyList()
) {

    val count: Int
        get() = contents.size

    val isEmpty: Boolean
        get() = contents.isEmpty()
}
