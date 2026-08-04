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
data class AndroidLibrarySearchResult(val packageId: String, val packageName: String, val item: PackageContentBrowserItem)
data class AndroidItemDraft(val question: String, val answer: String, val pronunciation: String, val partOfSpeech: String, val example: String, val exampleTranslation: String)

sealed interface AndroidLibraryState {
    data object Loading : AndroidLibraryState
    data class Root(val tree: LibraryNavigationTree, val query: String = "", val results: List<AndroidLibrarySearchResult> = emptyList()) : AndroidLibraryState
    data class PackageBrowser(
        val pkg: InstalledPackageSummary,
        val allItems: List<PackageContentBrowserItem>,
        val visibleItems: List<PackageContentBrowserItem>,
        val criteria: AndroidLibraryCriteria,
        val selectedContentId: String? = null,
        val draft: AndroidItemDraft? = null,
        val message: String? = null
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

    fun openPackage(id: InstalledPackageId, criteria: AndroidLibraryCriteria = AndroidLibraryCriteria(), selectedId: String? = null): AndroidLibraryState = runCatching {
        val summary = requireNotNull(context.libraryQuery?.getPackageSummary(id)) { "Package is no longer installed." }
        val all = requireNotNull(context.packageBrowserQuery) { "Content browser is unavailable." }.getBrowserItemsForPackage(id)
        browser(summary, all, criteria, selectedId)
    }.getOrElse { AndroidLibraryState.Failed(it.message ?: "Package could not be opened.") }

    fun searchGlobal(queryText: String): AndroidLibraryState = runCatching {
        val root = loadRoot() as? AndroidLibraryState.Root ?: return@runCatching loadRoot()
        if (queryText.isBlank()) return@runCatching root.copy(query = queryText)
        val results = root.tree.installedPackages.flatMap { pkg ->
            val all = context.packageBrowserQuery?.getBrowserItemsForPackage(pkg.id).orEmpty()
            PackageContentBrowserProjectionPolicy.filterAndSort(all, queryText, null, BrowserMediaFilter.ALL, BrowserSortOption.ORIGINAL_ORDER)
                .map { AndroidLibrarySearchResult(pkg.id.value, pkg.name, it) }
        }.take(100)
        root.copy(query = queryText, results = results)
    }.getOrElse { AndroidLibraryState.Failed("Library search failed.") }

    fun applyCriteria(state: AndroidLibraryState.PackageBrowser, criteria: AndroidLibraryCriteria) =
        browser(state.pkg, state.allItems, criteria, state.selectedContentId)

    fun select(state: AndroidLibraryState.PackageBrowser, contentId: String?) = state.copy(selectedContentId = contentId, draft = null)
    fun beginEdit(state: AndroidLibraryState.PackageBrowser): AndroidLibraryState.PackageBrowser {
        val item=state.allItems.firstOrNull { it.contentId.value==state.selectedContentId } ?: return state
        return state.copy(draft=AndroidItemDraft(item.questionText,item.answerText,item.pronunciation,item.partOfSpeech,item.exampleText.orEmpty(),item.exampleTranslation.orEmpty()))
    }
    fun saveEdit(state: AndroidLibraryState.PackageBrowser, draft: AndroidItemDraft): AndroidLibraryState = runCatching {
        val id=state.allItems.first { it.contentId.value==state.selectedContentId }.contentId
        requireNotNull(context.contentBrowserEdit) { "Content editor is unavailable." }.updateTextFields(id,draft.question,draft.answer,draft.pronunciation,draft.partOfSpeech,draft.example,draft.exampleTranslation)
        openPackage(state.pkg.id,state.criteria)
    }.getOrElse { state.copy(draft=draft, message="Content validation failed; your draft was preserved.") }

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
