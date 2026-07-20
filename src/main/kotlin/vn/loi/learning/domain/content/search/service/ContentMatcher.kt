package vn.loi.learning.domain.content.search.service

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.search.model.ContentSearchQuery

/**
 * Domain service xác định một Content có phù hợp với truy vấn tìm kiếm hay không.
 */
class ContentMatcher {

    fun matches(
        content: Content,
        query: ContentSearchQuery
    ): Boolean {
        val searchableValues = buildList {
            add(content.text.primaryText)
            content.text.translatedText?.let(::add)
            content.metadata.group?.let(::add)
            content.metadata.section?.let(::add)
            content.metadata.lesson?.let(::add)
            addAll(content.customFields.fields.map { field -> field.value })
        }

        return query.normalizedTerms.all { term ->
            searchableValues.any { value ->
                value.contains(
                    other = term,
                    ignoreCase = true
                )
            }
        }
    }
}
