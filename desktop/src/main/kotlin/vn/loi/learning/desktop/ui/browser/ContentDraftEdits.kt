package vn.loi.learning.desktop.ui.browser

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
