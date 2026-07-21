package vn.loi.learning.application.contentlibrary

/**
 * Dữ liệu LibraryCollection đã được chuẩn bị cho Presentation Layer.
 *
 * Query model công khai danh sách PackageId để Presentation có thể:
 * - hiển thị package đang được gắn;
 * - thực hiện thao tác detach theo đúng package;
 * - không cần truy cập trực tiếp Domain aggregate hoặc repository.
 */
data class LibraryCollectionItem(
    val id: String,
    val libraryId: String,
    val name: String,
    val packageIds: List<String>
) {

    val packageCount: Int
        get() = packageIds.size
}