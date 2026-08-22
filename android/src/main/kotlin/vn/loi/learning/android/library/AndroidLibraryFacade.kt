package vn.loi.learning.android.library

import vn.loi.learning.application.contentpackaging.browser.*
import vn.loi.learning.application.contentpackaging.InstalledPackageItem
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.library.query.*
import vn.loi.learning.domain.library.model.*
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.application.session.*
import vn.loi.learning.domain.study.memory.model.*
import vn.loi.learning.domain.study.session.model.SessionId
import java.util.UUID
import vn.loi.learning.android.platform.AndroidStartupTrace

data class AndroidLibraryCriteria(
    val query: String = "",
    val media: BrowserMediaFilter = BrowserMediaFilter.ALL,
    val sort: BrowserSortOption = BrowserSortOption.ORIGINAL_ORDER,
    val lesson: String? = null
)
data class AndroidLibrarySearchResult(val packageId: String, val packageName: String, val item: PackageContentBrowserItem)
data class AndroidItemDraft(val question: String, val answer: String, val pronunciation: String, val partOfSpeech: String, val example: String, val exampleTranslation: String)

enum class AndroidLibraryFilter { ALL, COLLECTIONS, PACKAGES }

data class AndroidLibraryPackageItem(
    val packageId: String,
    val title: String,
    val version: String,
    val contentCount: Int,
    val status: String,
    val isUsable: Boolean,
    val isActivePackage: Boolean
)

data class AndroidLibraryCollectionItem(
    val collectionId: String,
    val title: String,
    val packageIds: Set<String>
) { val packageCount: Int get() = packageIds.size }

sealed interface AndroidLibraryState {
    data object Loading : AndroidLibraryState
    data class Root(
        val allPackages: List<AndroidLibraryPackageItem>,
        val allCollections: List<AndroidLibraryCollectionItem>,
        val packages: List<AndroidLibraryPackageItem> = allPackages,
        val collections: List<AndroidLibraryCollectionItem> = allCollections,
        val query: String = "",
        val filter: AndroidLibraryFilter = AndroidLibraryFilter.ALL,
        val selectedCollectionId: String? = null
    ) : AndroidLibraryState
    data class PackageBrowser(
        val pkg: InstalledPackageItem,
        val allItems: List<PackageContentBrowserItem>,
        val visibleItems: List<PackageContentBrowserItem>,
        val criteria: AndroidLibraryCriteria,
        val selectedContentId: String? = null,
        val draft: AndroidItemDraft? = null,
        val message: String? = null
    ) : AndroidLibraryState
    data class Lessons(val pkg: InstalledPackageItem, val lessons: List<LessonBrowserSummary>, val query: String = "") : AndroidLibraryState
    data class StudyStarted(val sessionId: String) : AndroidLibraryState
    data class Failed(val message: String, val recoverable: Boolean = true) : AndroidLibraryState
}

/** Android presentation orchestration only; all query, filter and mutation authority is Application-owned. */
class AndroidLibraryFacade(
    private val context: LearningApplicationContext,
    private val dailyLimits: () -> vn.loi.learning.application.study.DailyStudyBudgetLimits = {
        vn.loi.learning.application.study.DailyStudyBudgetLimits()
    },
    private val now: () -> Long = System::currentTimeMillis,
    private val zoneId: () -> java.time.ZoneId = java.time.ZoneId::systemDefault,
    private val onRootQuery: () -> Unit = {},
    private val onActivePackageChanged: () -> Unit = {}
) {
    private val libraryId get() = context.defaultLibraryId ?: LibraryId("default-library")

    fun loadRoot(): AndroidLibraryState = AndroidStartupTrace.measured("library_load_root_total") { runCatching {
        onRootQuery()
        val query = requireNotNull(context.libraryQuery) { "Truy vấn thư viện không khả dụng." }
        AndroidStartupTrace.measured("library_navigation_tree") {
            query.getNavigationTree(libraryId)
        }?.let { tree ->
            val packages = tree.installedPackages.map { pkg ->
                AndroidLibraryPackageItem(
                    packageId = pkg.id.value,
                    title = pkg.name,
                    version = pkg.version,
                    contentCount = pkg.contentCount,
                    status = pkg.state.name,
                    isUsable = pkg.state == PackageState.ACTIVE,
                    isActivePackage = pkg.id == tree.activePackageId
                )
            }
            val collections = tree.collections.map { node ->
                AndroidLibraryCollectionItem(
                    collectionId = node.collection.id.value,
                    title = node.collection.name,
                    packageIds = node.assignedPackages.mapTo(linkedSetOf()) { it.id.value }
                )
            }
            AndroidLibraryState.Root(packages, collections)
        }
            ?: AndroidLibraryState.Failed("Thư viện không khả dụng.")
    }.getOrElse { AndroidLibraryState.Failed("Library could not be loaded.") } }

    fun openPackage(id: InstalledPackageId, criteria: AndroidLibraryCriteria = AndroidLibraryCriteria(), selectedId: String? = null): AndroidLibraryState = AndroidStartupTrace.measured("library_open_package_total") { runCatching {
        val summary = AndroidStartupTrace.measured("library_package_summary") {
            requireNotNull(context.installedPackages.findById(id.value)) { "Gói không còn được cài đặt." }
        }
        val all = AndroidStartupTrace.measured("library_package_browser_items") {
            requireNotNull(context.packageBrowserQuery) { "Trình duyệt nội dung không khả dụng." }.getBrowserItemsForPackage(id)
        }
        browser(summary, all, criteria, selectedId)
    }.getOrElse { AndroidLibraryState.Failed(it.message ?: "Không thể mở gói.") } }

    fun searchRoot(
        root: AndroidLibraryState.Root,
        queryText: String = root.query,
        filter: AndroidLibraryFilter = root.filter,
        collectionId: String? = root.selectedCollectionId
    ): AndroidLibraryState = runCatching {
        val normalized = queryText.trim()
        val selectedPackageIds = collectionId?.let { id ->
            root.allCollections.firstOrNull { it.collectionId == id }?.packageIds ?: emptySet()
        }
        val packages = root.allPackages.filter { pkg ->
            (selectedPackageIds == null || pkg.packageId in selectedPackageIds) &&
                (normalized.isEmpty() || pkg.title.contains(normalized, ignoreCase = true) ||
                    pkg.packageId.contains(normalized, ignoreCase = true) || pkg.version.contains(normalized, ignoreCase = true))
        }.takeIf { filter != AndroidLibraryFilter.COLLECTIONS }.orEmpty()
        val collections = root.allCollections.filter { collection ->
            collectionId == null && (normalized.isEmpty() || collection.title.contains(normalized, ignoreCase = true))
        }.takeIf { filter != AndroidLibraryFilter.PACKAGES }.orEmpty()
        root.copy(packages = packages, collections = collections, query = queryText, filter = filter, selectedCollectionId = collectionId)
    }.getOrElse { AndroidLibraryState.Failed("Tìm kiếm thư viện thất bại.") }
    fun openLessons(packageId: InstalledPackageId, search: String = ""): AndroidLibraryState = runCatching {
        AndroidLibraryState.Lessons(requireNotNull(context.installedPackages.findById(packageId.value)), requireNotNull(context.lessonBrowser).query(packageId,search), search)
    }.getOrElse { AndroidLibraryState.Failed("Không thể tải bài học.") }
    fun selectLearningPackage(packageId: InstalledPackageId): AndroidLibraryState = command(onActivePackageChanged) {
        requireNotNull(context.libraryCommand) { "Lệnh thư viện không khả dụng." }
            .setActivePackage(libraryId, packageId)
    }
    fun startPackage(packageId: InstalledPackageId) = start(StudyContentScope.Package(packageId))
    fun startLesson(packageId: InstalledPackageId, lesson: String) = start(StudyContentScope.Lesson(packageId,lesson))
    fun startSelection(packageId: InstalledPackageId, ids: Set<vn.loi.learning.domain.content.model.ContentId>) = start(StudyContentScope.Selection(packageId,ids))
    private fun start(scope: StudyContentScope): AndroidLibraryState = runCatching {
        val packageId = when (scope) {
            is StudyContentScope.Package -> scope.packageId
            is StudyContentScope.Lesson -> scope.packageId
            is StudyContentScope.Selection -> scope.packageId
            is StudyContentScope.Collection -> null
        }
        if (packageId != null) {
            require(requireNotNull(context.libraryCommand) { "Lệnh thư viện không khả dụng." }
                .setActivePackage(libraryId, packageId) is vn.loi.learning.application.library.command.LibraryCommandResult.Success) {
                "Không thể đặt gói làm gói học chính."
            }
            require(context.domainLibraryRepository?.findById(libraryId)?.activePackageId == packageId) {
                "Không thể xác nhận lựa chọn gói."
            }
            runCatching(onActivePackageChanged)
            context.engine.getActiveSession(LearnerId("default-learner"))
                ?.takeIf { it.installedPackageId != packageId }
                ?.let { context.engine.finishSession(it.id, Moment(now())) }
        }
        val startedAt = Moment(now())
        val scopeContentIds = when (scope) {
            is StudyContentScope.Package -> context.packageContentQuery?.getContentsForPackage(scope.packageId).orEmpty()
            is StudyContentScope.Lesson -> context.packageContentQuery?.getContentsForPackage(scope.packageId).orEmpty()
                .filter { it.lesson == scope.lesson }
            is StudyContentScope.Selection -> context.packageContentQuery?.getContentsForPackage(scope.packageId).orEmpty()
                .filter { vn.loi.learning.domain.content.model.ContentId(it.id) in scope.contentIds }
            is StudyContentScope.Collection -> emptyList()
        }.mapTo(linkedSetOf()) { vn.loi.learning.domain.content.model.ContentId(it.id) }
        val daily = requireNotNull(context.dailyStudyBudget) { "Hạn mức học hằng ngày không khả dụng." }
            .execute(LearnerId("default-learner"), dailyLimits(), startedAt, zoneId(), scopeContentIds)
        require(daily.hasEligibleWork) {
            if (daily.targetsComplete) "Đã hoàn thành khối lượng học được cấu hình hôm nay."
            else "Hiện không có nội dung học phù hợp."
        }
        val session=requireNotNull(context.scopedStudy).execute(StartScopedStudyRequest(
            SessionId(UUID.randomUUID().toString()), LearnerId("default-learner"), startedAt, scope,
            vn.loi.learning.domain.study.session.model.SessionPolicy(daily.newRemainingToday, daily.reviewRemainingToday)
        ))
        AndroidLibraryState.StudyStarted(session.id.value)
    }.getOrElse { AndroidLibraryState.Failed(it.message ?: "Không thể bắt đầu học theo phạm vi này.") }

    fun applyCriteria(state: AndroidLibraryState.PackageBrowser, criteria: AndroidLibraryCriteria) =
        browser(state.pkg, state.allItems, criteria, state.selectedContentId)

    fun select(state: AndroidLibraryState.PackageBrowser, contentId: String?) = state.copy(selectedContentId = contentId, draft = null)
    fun beginEdit(state: AndroidLibraryState.PackageBrowser): AndroidLibraryState.PackageBrowser {
        val item=state.allItems.firstOrNull { it.contentId.value==state.selectedContentId } ?: return state
        return state.copy(draft=AndroidItemDraft(item.questionText,item.answerText,item.pronunciation,item.partOfSpeech,item.exampleText.orEmpty(),item.exampleTranslation.orEmpty()))
    }
    fun saveEdit(state: AndroidLibraryState.PackageBrowser, draft: AndroidItemDraft): AndroidLibraryState = runCatching {
        val id=state.allItems.first { it.contentId.value==state.selectedContentId }.contentId
        requireNotNull(context.contentBrowserEdit) { "Trình sửa nội dung không khả dụng." }.updateTextFields(id,draft.question,draft.answer,draft.pronunciation,draft.partOfSpeech,draft.example,draft.exampleTranslation)
        openPackage(InstalledPackageId(state.pkg.id),state.criteria,id.value)
    }.getOrElse { state.copy(draft=draft, message="Xác thực nội dung thất bại; bản nháp của bạn đã được giữ lại.") }

    fun createCollection(name: String): AndroidLibraryState = command {
        require(name.isNotBlank()) { "Cần nhập tên bộ sưu tập." }
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

    private fun command(
        onSuccess: () -> Unit = {},
        action: () -> LibraryCommandResult<*>
    ): AndroidLibraryState = runCatching {
        when (val result = action()) {
            is LibraryCommandResult.Success -> {
                runCatching(onSuccess)
                loadRoot()
            }
            is LibraryCommandResult.DuplicateCollection -> AndroidLibraryState.Failed("Đã có bộ sưu tập mang tên này.")
            is LibraryCommandResult.InvalidState -> AndroidLibraryState.Failed(result.message)
            is LibraryCommandResult.PersistenceFailure -> AndroidLibraryState.Failed("Library changes could not be saved.")
            else -> AndroidLibraryState.Failed("Thư viện đã thay đổi. Hãy làm mới và thử lại.")
        }
    }.getOrElse { AndroidLibraryState.Failed(it.message ?: "Thao tác thư viện thất bại.") }

    private fun browser(
        pkg: InstalledPackageItem,
        all: List<PackageContentBrowserItem>,
        criteria: AndroidLibraryCriteria,
        selected: String? = null
    ) = AndroidLibraryState.PackageBrowser(
        pkg, all,
        PackageContentBrowserProjectionPolicy.filterAndSort(all, criteria.query, criteria.lesson, criteria.media, criteria.sort),
        criteria, selected?.takeIf { id -> all.any { it.contentId.value == id } }
    )
}
