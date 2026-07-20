package vn.loi.learning.domain.content.search.service

import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.search.model.ContentSearchQuery

/**
 * Domain service tính độ phù hợp của Content với truy vấn tìm kiếm.
 *
 * Điểm cao hơn có độ ưu tiên cao hơn.
 */
class ContentRankingService {

    fun score(
        content: Content,
        query: ContentSearchQuery
    ): Int {
        val terms = query.normalizedTerms
        val phrase = query.normalizedPhrase

        return when {
            terms.size > 1 && content.text.primaryText.contains(phrase, ignoreCase = true) -> PRIMARY_TEXT_SCORE + PHRASE_MATCH_BONUS

            terms.any { term -> content.text.primaryText.contains(term, ignoreCase = true) } -> PRIMARY_TEXT_SCORE

            terms.size > 1 && content.text.translatedText?.contains(phrase, ignoreCase = true) == true -> TRANSLATED_TEXT_SCORE + PHRASE_MATCH_BONUS

            terms.any { term -> content.text.translatedText?.contains(term, ignoreCase = true) == true } -> TRANSLATED_TEXT_SCORE

            terms.size > 1 && content.metadata.group?.contains(phrase, ignoreCase = true) == true -> GROUP_SCORE + PHRASE_MATCH_BONUS

            terms.any { term -> content.metadata.group?.contains(term, ignoreCase = true) == true } -> GROUP_SCORE

            terms.size > 1 && content.metadata.section?.contains(phrase, ignoreCase = true) == true -> SECTION_SCORE + PHRASE_MATCH_BONUS

            terms.any { term -> content.metadata.section?.contains(term, ignoreCase = true) == true } -> SECTION_SCORE

            terms.size > 1 && content.metadata.lesson?.contains(phrase, ignoreCase = true) == true -> LESSON_SCORE + PHRASE_MATCH_BONUS

            terms.any { term -> content.metadata.lesson?.contains(term, ignoreCase = true) == true } -> LESSON_SCORE

            content.customFields.fields.any { field -> terms.size > 1 && field.value.contains(phrase, ignoreCase = true) } -> CUSTOM_FIELD_SCORE + PHRASE_MATCH_BONUS

            content.customFields.fields.any { field -> terms.any { term -> field.value.contains(term, ignoreCase = true) } } -> CUSTOM_FIELD_SCORE

            else -> NO_MATCH_SCORE
        }

    }

    private companion object {
        const val PRIMARY_TEXT_SCORE = 100
        const val PHRASE_MATCH_BONUS = 5
        const val TRANSLATED_TEXT_SCORE = 90
        const val GROUP_SCORE = 80
        const val SECTION_SCORE = 70
        const val LESSON_SCORE = 60
        const val CUSTOM_FIELD_SCORE = 50
        const val NO_MATCH_SCORE = 0
    }
}











