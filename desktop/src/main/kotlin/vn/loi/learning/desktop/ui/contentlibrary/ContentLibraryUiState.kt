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
 * Tham chiếu package đang được gắn vào một collection.
 *
 * Presentation model chỉ chứa dữ liệu cần thiết cho UI
 * và không để Compose phụ thuộc trực tiếp vào Domain.
 */
data class ContentLibraryAttachedPackageItem(
    val id: String,
    val name: String,
    val version: String,
    val format: String
)

/**
 * Một collection logic thuộc Content Library.
 */
data class ContentLibraryCollectionItem(
    val id: String,
    val libraryId: String,
    val name: String,
    val attachedPackages:
    List<ContentLibraryAttachedPackageItem> =
        emptyList()
) {

    val packageCount: Int
        get() = attachedPackages.size
}

/**
 * Một library được hiển thị trong Library Browser.
 */
data class ContentLibraryItem(
    val id: String,
    val name: String,
    val contentCount: Int,
    val learningItemCount: Int,
    val collections: List<ContentLibraryCollectionItem> =
        emptyList()
) {

    val collectionCount: Int
        get() = collections.size
}

data class ContentLibraryImportResult(
    val discoveredPackageCount: Int,
    val importedPackageCount: Int,
    val importedLibraryCount: Int,
    val importedContentCount: Int,
    val importedLearningItemCount: Int,
    val failures: List<ContentLibraryImportFailure> = emptyList()
) {
    val failedPackageCount: Int
        get() = failures.size
}

data class ContentLibraryImportFailure(
    val source: String,
    val message: String
)

/**
 * Trạng thái hiển thị của Content Library.
 */
data class ContentLibraryUiState(
    val packages: List<ContentLibraryPackageItem> =
        emptyList(),
    val libraries: List<ContentLibraryItem> =
        emptyList(),
    val importMessage: String? = null,
    val importError: String? = null,
    val loadError: String? = null
) {

    val packageCount: Int
        get() = packages.size

    val libraryCount: Int
        get() = libraries.size

    val collectionCount: Int
        get() =
            libraries.sumOf { library ->
                library.collectionCount
            }

    val isEmpty: Boolean
        get() =
            packages.isEmpty() &&
                    libraries.isEmpty()
}