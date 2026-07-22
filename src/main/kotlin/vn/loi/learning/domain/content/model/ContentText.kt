package vn.loi.learning.domain.content.model

/**
 * Các thành phần văn bản của Content.
 *
 * Tên primaryText và translatedText được sử dụng thay vì en/vi
 * để Engine không bị khóa vào riêng cặp ngôn ngữ Anh - Việt.
 */
data class ContentText(
    val primaryText: String,
    val translatedText: String? = null,
    val pronunciation: String? = null,
    val exampleText: String? = null,
    val exampleTranslation: String? = null,
    val primaryFormat: ContentTextFormat = ContentTextFormat.PLAIN_TEXT,
    val translatedFormat: ContentTextFormat = ContentTextFormat.PLAIN_TEXT,
    val exampleFormat: ContentTextFormat = ContentTextFormat.PLAIN_TEXT,
    val exampleTranslationFormat: ContentTextFormat = ContentTextFormat.PLAIN_TEXT
) {

    init {
        require(primaryText.isNotBlank()) {
            "Content primary text must not be blank."
        }
    }
}
