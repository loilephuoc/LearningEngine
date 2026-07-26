package vn.loi.learning.desktop.ui.browser

import vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem

/**
 * Bản thảo các thay đổi chưa lưu cho một Content trong Learning Browser / Content Studio.
 *
 * Được tạo khi người dùng bắt đầu edit hoặc tạo mới, bị xóa khi Save hoặc Discard.
 * Hỗ trợ bản thảo cho cả văn bản lẫn các file phương tiện (ảnh và 4 khe audio).
 */
data class ContentDraftEdits(
    val contentId: String,
    val questionText: String = "",
    val answerText: String = "",
    val pronunciation: String = "",
    val partOfSpeech: String = "WORD",
    val exampleText: String = "",
    val exampleTranslation: String = "",
    val imageRef: String? = null,
    val questionAudioRef: String? = null,
    val answerAudioRef: String? = null,
    val exampleAudioRef: String? = null,
    val translationAudioRef: String? = null
)

/**
 * Extension method tạo [ContentDraftEdits] snapshot chuẩn từ [PackageContentBrowserItem].
 * Giữ nguyên tất cả dữ liệu văn bản và toàn bộ 5 khe media.
 */
fun PackageContentBrowserItem.toDraftEdits(): ContentDraftEdits {
    return ContentDraftEdits(
        contentId = contentId.value,
        questionText = questionText,
        answerText = answerText,
        pronunciation = pronunciation,
        partOfSpeech = partOfSpeech,
        exampleText = exampleText.orEmpty(),
        exampleTranslation = exampleTranslation.orEmpty(),
        imageRef = imageRef,
        questionAudioRef = questionAudioRef,
        answerAudioRef = answerAudioRef,
        exampleAudioRef = exampleAudioRef,
        translationAudioRef = translationAudioRef
    )
}
