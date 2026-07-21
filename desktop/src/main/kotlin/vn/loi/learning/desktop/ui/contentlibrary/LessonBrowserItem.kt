package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Presentation model cho một Content có thể học trong Lesson Browser.
 *
 * group, section và lesson được giữ nguyên từ ContentMetadata
 * để Presentation Layer có thể hiển thị đúng cấu trúc của package
 * mà không tạo thêm business model trong Desktop.
 */
data class LessonBrowserItem(
    val id: String,
    val title: String,
    val type: String,
    val group: String?,
    val section: String?,
    val lesson: String?,
    val primaryText: String,
    val translatedText: String?,
    val learningItemCount: Int
) {

    val hierarchyPath: String
        get() =
            listOfNotNull(
                group?.takeIf {
                    it.isNotBlank()
                },
                section?.takeIf {
                    it.isNotBlank()
                },
                lesson?.takeIf {
                    it.isNotBlank()
                }
            )
                .distinct()
                .joinToString(
                    separator = " → "
                )

    val hasHierarchy: Boolean
        get() =
            hierarchyPath.isNotBlank()
}