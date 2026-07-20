package vn.loi.learning.desktop.ui.contentlibrary

/**
 * Một package được hiển thị trong Content Library.
 *
 * Presentation model này không phụ thuộc trực tiếp
 * vào Domain model ContentPackage.
 */
data class ContentLibraryPackageItem(
    val id: String,
    val name: String,
    val version: String,
    val format: String,
    val libraryCount: Int
)

/**
 * Một library được hiển thị trong Library Browser.
 */
data class ContentLibraryItem(
    val id: String,
    val name: String,
    val contentCount: Int,
    val learningItemCount: Int
)

data class ContentLibraryImportResult(
    val importedPackageCount: Int,
    val importedLibraryCount: Int,
    val importedContentCount: Int,
    val importedLearningItemCount: Int
)

/**
 * Trạng thái hiển thị của Content Library.
 */
data class ContentLibraryUiState(
    val packages: List<ContentLibraryPackageItem> = emptyList(),
    val libraries: List<ContentLibraryItem> = emptyList(),
    val importMessage: String? = null,
    val importError: String? = null
) {

    val packageCount: Int
        get() = packages.size

    val libraryCount: Int
        get() = libraries.size

    val isEmpty: Boolean
        get() = packages.isEmpty() && libraries.isEmpty()
}