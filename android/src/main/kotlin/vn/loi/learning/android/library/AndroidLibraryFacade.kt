package vn.loi.learning.android.library

import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.query.*
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext

data class AndroidLibraryCriteria(
    val query: String = "",
    val media: BrowserMediaFilter = BrowserMediaFilter.ALL,
    val sort: BrowserSortOption = BrowserSortOption.ORIGINAL_ORDER,
    val lesson: String? = null
)

sealed interface AndroidLibraryState {
    data object Loading : AndroidLibraryState
    data class Root(val tree: LibraryNavigationTree) : AndroidLibraryState
    data class PackageBrowser(
        val pkg: InstalledPackageSummary,
        val allItems: List<PackageContentBrowserItem>,
        val visibleItems: List<PackageContentBrowserItem>,
        val criteria: AndroidLibraryCriteria,
        val selectedContentId: String? = null
    ) : AndroidLibraryState
    data class Failed(val message: String, val recoverable: Boolean = true) : AndroidLibraryState
}

/** Android presentation orchestration only; all query, filter and mutation authority is Application-owned. */
class AndroidLibraryFacade(private val context: LearningApplicationContext) {
    private val libraryId get() = context.defaultLibraryId ?: LibraryId("default-library")

    fun loadRoot(): AndroidLibraryState = runCatching {
        val query = requireNotNull(context.libraryQuery) { "Library query is unavailable." }
        query.getNavigationTree(libraryId)?.let(AndroidLibraryState::Root)
            ?: AndroidLibraryState.Failed("Library is unavailable.")
    }.getOrElse { AndroidLibraryState.Failed("Library could not be loaded.") }

    fun openPackage(id: InstalledPackageId, criteria: AndroidLibraryCriteria = AndroidLibraryCriteria()): AndroidLibraryState = runCatching {
        val summary = requireNotNull(context.libraryQuery?.getPackageSummary(id)) { "Package is no longer installed." }
        val all = requireNotNull(context.packageBrowserQuery) { "Content browser is unavailable." }.getBrowserItemsForPackage(id)
        browser(summary, all, criteria)
    }.getOrElse { AndroidLibraryState.Failed(it.message ?: "Package could not be opened.") }

    fun applyCriteria(state: AndroidLibraryState.PackageBrowser, criteria: AndroidLibraryCriteria) =
        browser(state.pkg, state.allItems, criteria, state.selectedContentId)

    fun select(state: AndroidLibraryState.PackageBrowser, contentId: String?) = state.copy(selectedContentId = contentId)

    fun createCollection(name: String): AndroidLibraryState = command {
        require(name.isNotBlank()) { "Collection name is required." }
        requireNotNull(context.libraryCommand).createCollection(libraryId, CollectionName(name))
    }

    fun renameCollection(id: CollectionId, name: String): AndroidLibraryState = command {
        requireNotNull(context.libraryCommand).renameCollection(libraryId, id, CollectionName(name))
    }

    fun deleteCollection(id: CollectionId): AndroidLibraryState = command {
        requireNotNull(context.libraryCommand).deleteCollection(libraryId, id)
    }

    fun attach(collectionId: CollectionId, packageId: InstalledPackageId): AndroidLibraryState = command {
        requireNotNull(context.libraryCommand).assignPackageToCollection(libraryId, collectionId, packageId)
    }

    fun detach(collectionId: CollectionId, packageId: InstalledPackageId): AndroidLibraryState = command {
        requireNotNull(context.libraryCommand).removePackageFromCollection(libraryId, collectionId, packageId)
    }

    private fun command(action: () -> LibraryCommandResult<*>): AndroidLibraryState = runCatching {
        when (val result = action()) {
            is LibraryCommandResult.Success -> loadRoot()
            is LibraryCommandResult.DuplicateCollection -> AndroidLibraryState.Failed("A collection with that name already exists.")
            is LibraryCommandResult.InvalidState -> AndroidLibraryState.Failed(result.message)
            is LibraryCommandResult.PersistenceFailure -> AndroidLibraryState.Failed("Library changes could not be saved.")
            else -> AndroidLibraryState.Failed("The library changed. Refresh and try again.")
        }
    }.getOrElse { AndroidLibraryState.Failed(it.message ?: "Library command failed.") }

    private fun browser(
        pkg: InstalledPackageSummary,
        all: List<PackageContentBrowserItem>,
        criteria: AndroidLibraryCriteria,
        selected: String? = null
    ) = AndroidLibraryState.PackageBrowser(
        pkg, all,
        PackageContentBrowserProjectionPolicy.filterAndSort(all, criteria.query, criteria.lesson, criteria.media, criteria.sort),
        criteria, selected?.takeIf { id -> all.any { it.contentId.value == id } }
    )
}
