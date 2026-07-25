package vn.loi.learning.desktop.ui.browser

/**
 * Bản thảo các thay đổi chưa lưu cho một Content trong Learning Browser.
 *
 * Được tạo khi người dùng bắt đầu edit, bị xóa khi Save hoặc Discard.
 * Không liên quan đến persistence — chỉ là transient presentation state.
 */
data class ContentDraftEdits(
    val contentId: String,
    val questionText: String,
    val answerText: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val exampleText: String,
    val exampleTranslation: String
)
