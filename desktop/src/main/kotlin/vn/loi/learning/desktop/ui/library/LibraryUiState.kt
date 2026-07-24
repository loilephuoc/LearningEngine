package vn.loi.learning.desktop.ui.library

import vn.loi.learning.application.library.query.CollectionNode
import vn.loi.learning.application.library.query.CollectionSummary
import vn.loi.learning.application.library.query.InstalledPackageSummary
import vn.loi.learning.application.library.query.LibraryNavigationTree
import vn.loi.learning.application.library.query.LibraryStatistics

/**
 * Trạng thái hiển thị (Presentation UI State) cho màn hình Desktop Library.
 */
sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Content(
        val tree: LibraryNavigationTree,
        val selectedSection: LibrarySection = LibrarySection.OVERVIEW
    ) : LibraryUiState {
        val activePackages: List<InstalledPackageSummary> get() = tree.activePackages
        val archivedPackages: List<InstalledPackageSummary> get() = tree.archivedPackages
        val installedPackages: List<InstalledPackageSummary> get() = tree.installedPackages
        val collections: List<CollectionNode> get() = tree.collections
        val deletedCollections: List<CollectionSummary> get() = tree.deletedCollections
        val statistics: LibraryStatistics get() = tree.statistics
        val activePackageId: vn.loi.learning.domain.library.model.InstalledPackageId? get() = tree.activePackageId
    }

    data class Empty(
        val message: String = "Library is empty. No installed packages or active collections found."
    ) : LibraryUiState

    data class Error(
        val message: String
    ) : LibraryUiState
}

enum class LibrarySection(val label: String) {
    OVERVIEW("Overview"),
    INSTALLED("Installed Packages"),
    ACTIVE("Active Packages"),
    ARCHIVED("Archived Packages"),
    COLLECTIONS("Collections"),
    DELETED("Deleted Collections")
}
