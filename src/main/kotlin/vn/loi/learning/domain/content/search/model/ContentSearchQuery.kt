package vn.loi.learning.domain.content.search.model

/**
 * Tiêu chí tìm kiếm Content.
 *
 * Vertical Slice 1.0: chỉ hỗ trợ keyword đơn giản.
 * Các tiêu chí khác sẽ được bổ sung sau mà không phá API.
 */
data class ContentSearchQuery(
    val keyword: String
) {

    init {
        require(keyword.isNotBlank()) {
            "Search keyword must not be blank."
        }
    }

    val normalizedKeyword: String
        get() = keyword.trim()

    val normalizedTerms: List<String>
        get() = normalizedKeyword
            .split(Regex("\\s+"))
            .filter { term -> term.isNotBlank() }

    val normalizedPhrase: String
        get() = normalizedTerms.joinToString(" ")
}


