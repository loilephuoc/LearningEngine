package vn.loi.learning.application.port

import vn.loi.learning.domain.content.search.model.ContentSearchQuery
import vn.loi.learning.domain.content.search.model.ContentSearchResult

/**
 * Port chuyên truy vấn Content.
 * CRUD và Search được tách riêng để dễ mở rộng.
 */
interface ContentSearchRepository {

    fun search(
        query: ContentSearchQuery
    ): ContentSearchResult
}
