package vn.loi.learning.infrastructure.persistence.repository

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.ContentSearchRepository
import vn.loi.learning.domain.content.search.model.ContentSearchQuery
import vn.loi.learning.domain.content.search.model.ContentSearchResult
import vn.loi.learning.domain.content.search.service.ContentMatcher
import vn.loi.learning.domain.content.search.service.ContentRankingService

class StoreBackedContentSearchRepository(
    private val contentRepository: ContentRepository,
    private val matcher: ContentMatcher = ContentMatcher(),
    private val ranking: ContentRankingService = ContentRankingService()
) : ContentSearchRepository {

    override fun search(
        query: ContentSearchQuery
    ): ContentSearchResult {
        val contents = contentRepository
            .findAll()
            .filter { content ->
                matcher.matches(
                    content = content,
                    query = query
                )
            }
            .sortedWith(
                compareByDescending<vn.loi.learning.domain.content.model.Content> { content ->
                    ranking.score(
                        content = content,
                        query = query
                    )
                }
                    .thenBy { content -> content.text.primaryText.lowercase() }
                    .thenBy { content -> content.id.value }
            )

        return ContentSearchResult(
            contents = contents
        )
    }
}


