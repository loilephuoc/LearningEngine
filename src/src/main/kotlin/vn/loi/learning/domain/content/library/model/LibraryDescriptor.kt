package vn.loi.learning.domain.content.library.model

/**
 * Thông tin mô tả dễ đọc của một Content Library.
 *
 * Descriptor chứa metadata nghiệp vụ của Library, tách khỏi:
 * - định danh ổn định;
 * - danh sách Content;
 * - thông tin kỹ thuật về file, package hoặc persistence.
 */
data class LibraryDescriptor(
    val name: String
) {

    init {
        require(name.isNotBlank()) {
            "Content library name must not be blank."
        }
    }
}
