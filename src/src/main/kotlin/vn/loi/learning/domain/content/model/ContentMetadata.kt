package vn.loi.learning.domain.content.model

/**
 * Thông tin dùng để tổ chức, tìm kiếm và phân loại Content.
 *
 * group, section và lesson được giữ ở dạng metadata để tương thích
 * về mặt khái niệm với kho dữ liệu hiện tại nhưng không khóa Domain
 * vào một cây thư mục cố định.
 */
data class ContentMetadata(
    val title: String? = null,
    val group: String? = null,
    val section: String? = null,
    val lesson: String? = null,
    val tags: Set<String> = emptySet(),
    val source: String? = null
) {

    init {
        require(title == null || title.isNotBlank()) {
            "Content title must not be blank."
        }

        require(group == null || group.isNotBlank()) {
            "Content group must not be blank."
        }

        require(section == null || section.isNotBlank()) {
            "Content section must not be blank."
        }

        require(lesson == null || lesson.isNotBlank()) {
            "Content lesson must not be blank."
        }

        require(source == null || source.isNotBlank()) {
            "Content source must not be blank."
        }

        require(tags.none { it.isBlank() }) {
            "Content tags must not contain blank values."
        }
    }
}