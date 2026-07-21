package vn.loi.learning.domain.content.library.model

/**
 * Metadata nghiệp vụ dễ đọc của một Library Collection.
 *
 * Descriptor không chứa định danh, quan hệ package hoặc thông tin persistence.
 */
data class LibraryCollectionDescriptor(
    val name: String
) {

    init {
        require(name.isNotBlank()) {
            "Library collection name must not be blank."
        }
    }
}