package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Presentation model cho Lesson Browser.
 *
 * Đại diện một Content (Lesson) trong Library Browser.
 * Chưa chứa navigation hay business logic.
 */
data class LessonBrowserItem(
    val id: String,
    val title: String,
    val type: String,
    val primaryText: String,
    val translatedText: String?,
    val learningItemCount: Int
)