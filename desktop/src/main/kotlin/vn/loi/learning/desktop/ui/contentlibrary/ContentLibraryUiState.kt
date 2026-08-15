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

data class PackageIntegrityDialogState(
    val visible: Boolean = false,
    val packageId: String = "",
    val packageName: String = "",
    val scanning: Boolean = false,
    val report: vn.loi.learning.application.integrity.PackageIntegrityReport? = null,
    val error: String? = null
)

data class LibraryHealthPackageResult(
    val packageId: String,
    val packageName: String,
    val report: vn.loi.learning.application.integrity.PackageIntegrityReport? = null,
    val failure: String? = null
)

data class LibraryHealthPackageTarget(val packageId: String, val packageName: String)

data class LibraryHealthOverviewState(
    val scanning: Boolean = false,
    val scannedAt: java.time.Instant? = null,
    val results: List<LibraryHealthPackageResult> = emptyList(),
    val failure: String? = null
) {
    val hasResult: Boolean get() = scannedAt != null
    val healthyPackages: Int get() = results.count { it.report?.status == vn.loi.learning.application.integrity.IntegrityStatus.HEALTHY }
    val warningPackages: Int get() = results.count { it.report?.status == vn.loi.learning.application.integrity.IntegrityStatus.WARNINGS }
    val errorPackages: Int get() = results.count { it.report?.status == vn.loi.learning.application.integrity.IntegrityStatus.ERRORS }
    val failedPackages: Int get() = results.count { it.failure != null }
}

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
    val loadError: String? = null,
    val operation: ContentLibraryOperation = ContentLibraryOperation.Idle
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

sealed interface ContentLibraryOperation {
    data object Idle : ContentLibraryOperation
    data class Loading(val title: String, val phase: String) : ContentLibraryOperation
    data class Importing(
        val phase: String,
        val processed: Int = 0,
        val total: Int = 0,
        val committed: Boolean = false,
        val cancellationSignal: vn.loi.learning.application.contentpackaging.PackageImportCancellationSignal? = null
    ) : ContentLibraryOperation {
        val fraction: Float?
            get() = total.takeIf { it > 0 }?.let {
                val measured = processed.toFloat() / it.toFloat()
                if (committed) measured.coerceIn(0f, 1f) else measured.coerceIn(0f, 0.95f)
            }
    }
    data class Exporting(
        val packageName: String,
        val phase: String,
        val processed: Int = 0,
        val total: Int = 100
    ) : ContentLibraryOperation {
        val fraction: Float
            get() = if (total > 0) (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    }
}
