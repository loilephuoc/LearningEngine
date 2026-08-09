package vn.loi.learning.android.packageexperience

import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.application.session.StartScopedStudyRequest
import vn.loi.learning.application.session.StudyContentScope
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import java.util.UUID
import vn.loi.learning.application.library.command.LibraryCommandResult
import vn.loi.learning.application.study.DailyStudyBudgetLimits
import vn.loi.learning.domain.study.session.model.SessionPolicy
import java.time.ZoneId

/**
 * Android presentation facade for Package Experience.
 * All query authority stays in Application layer.
 * No repository calls from UI — this facade owns the projection boundary.
 */
class AndroidPackageFacade(
    private val context: LearningApplicationContext,
    private val dailyLimits: () -> DailyStudyBudgetLimits = { DailyStudyBudgetLimits() },
    private val now: () -> Long = System::currentTimeMillis,
    private val zoneId: () -> ZoneId = ZoneId::systemDefault
) {

    private val learnerId = LearnerId("default-learner")

    /**
     * Open a package by canonical [InstalledPackageId].
     * - Header: from InstalledPackageSummary (NavigationTree authority).
     * - Content: from PackageContentBrowserQueryService (no archive parse, no media bytes).
     * - CTA: from active session query and scope.
     * Returns [AndroidPackageContentState] — never throws.
     */
    fun openPackage(id: InstalledPackageId, query: String = ""): AndroidPackageContentState = runCatching {
        val libraryId = requireNotNull(context.defaultLibraryId) { "Library is unavailable." }
        val tree = requireNotNull(context.libraryQuery).getNavigationTree(libraryId)
            ?: return AndroidPackageContentState.Failure("Library is unavailable.")

        val summary = tree.installedPackages.firstOrNull { it.id == id }
            ?: return AndroidPackageContentState.Failure("Package is no longer installed.")

        val header = AndroidPackageHeaderModel(
            packageId = summary.id.value,
            title = summary.name,
            version = summary.version,
            contentCount = summary.contentCount,
            state = summary.state.name,
            isActivePackage = tree.activePackageId == summary.id
        )

        val allItems = requireNotNull(context.packageBrowserQuery) { "Content browser is unavailable." }
            .getBrowserItemsForPackage(id)

        // Pass the already-loaded tree to avoid a second getNavigationTree call.
        val isActivePackage = tree.activePackageId == id
        val cta = resolveCtaWithTree(id, allItems.size, isActivePackage)

        if (allItems.isEmpty()) {
            return AndroidPackageContentState.Empty(header = header, cta = cta)
        }

        val visibleRows = applyQuery(allItems, query)
        AndroidPackageContentState.Content(
            header = header,
            cta = cta,
            allRows = allItems.map { it.toRow() },
            visibleRows = visibleRows,
            query = query
        )
    }.getOrElse { AndroidPackageContentState.Failure(it.message ?: "Package could not be opened.") }

    /**
     * Apply a package-local search query on pre-loaded items.
     * Unicode-safe — uses pre-computed NFKC-normalized searchableText from browser projection.
     * No repository call — operates on cached allRows.
     */
    fun applySearch(current: AndroidPackageContentState.Content, query: String): AndroidPackageContentState.Content {
        val allRows = current.allRows
        val visible = if (query.isBlank()) allRows else {
            val normalized = normalizeQuery(query)
            allRows.filter { row -> row.searchableText.contains(normalized) }
        }
        return current.copy(visibleRows = visible, query = query)
    }

    /**
     * Start a scoped Study session for this package.
     * Returns a session ID on success, or null if unavailable.
     */
    fun startPackage(id: InstalledPackageId): Result<String> = runCatching {
        val libraryId = requireNotNull(context.defaultLibraryId) { "Library is unavailable." }
        require(requireNotNull(context.libraryCommand) { "Library commands are unavailable." }
            .setActivePackage(libraryId, id) is LibraryCommandResult.Success) {
            "Package could not become active."
        }
        require(context.domainLibraryRepository?.findById(libraryId)?.activePackageId == id) {
            "Package selection could not be confirmed."
        }
        context.engine.getActiveSession(learnerId)
            ?.takeIf { it.installedPackageId != id }
            ?.let { context.engine.finishSession(it.id, Moment(now())) }
        val startedAt = Moment(now())
        val contentIds = context.packageContentQuery?.getContentsForPackage(id).orEmpty()
            .mapTo(linkedSetOf()) { vn.loi.learning.domain.content.model.ContentId(it.id) }
        val daily = requireNotNull(context.dailyStudyBudget) { "Daily Study budget is unavailable." }
            .execute(learnerId, dailyLimits(), startedAt, zoneId(), contentIds)
        require(daily.hasEligibleWork) {
            if (daily.targetsComplete) "Today's configured Study workload is complete."
            else "No eligible Study content is currently available."
        }
        val session = requireNotNull(context.scopedStudy) { "Scoped Study is unavailable." }
            .execute(
                StartScopedStudyRequest(
                    sessionId = SessionId(UUID.randomUUID().toString()),
                    learnerId = learnerId,
                    startedAt = startedAt,
                    scope = StudyContentScope.Package(id),
                    policy = SessionPolicy(daily.newRemainingToday, daily.reviewRemainingToday)
                )
            )
        session.id.value
    }

    /** Select this usable package for Study without creating or finishing a session. */
    fun selectLearningPackage(id: InstalledPackageId): Result<Unit> = runCatching {
        val libraryId = requireNotNull(context.defaultLibraryId) { "Library is unavailable." }
        require(requireNotNull(context.libraryCommand) { "Library commands are unavailable." }
            .setActivePackage(libraryId, id) is LibraryCommandResult.Success) {
            "Package could not become the learning package."
        }
        require(context.domainLibraryRepository?.findById(libraryId)?.activePackageId == id) {
            "Package selection could not be confirmed."
        }
    }

    /**
     * Resume an existing active session.
     * Returns the session ID if one exists, null otherwise.
     */
    fun resolveActiveSessionId(id: InstalledPackageId): String? = runCatching {
        val libraryId = context.defaultLibraryId ?: return null
        if (context.domainLibraryRepository?.findById(libraryId)?.activePackageId != id) return null
        val session = context.engine.getActiveSession(learnerId) ?: return null
        // Only offer Continue Learning if session belongs to this package
        if (session.installedPackageId == id) session.id.value else null
    }.getOrNull()

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun resolveCtaWithTree(id: InstalledPackageId, contentCount: Int, isActivePackage: Boolean): AndroidPackageCta {
        if (contentCount == 0) return AndroidPackageCta.NoContent
        val activeSessionId = resolveActiveSessionId(id)
        if (activeSessionId != null) return AndroidPackageCta.ContinueLearning(activeSessionId)
        return if (isActivePackage) AndroidPackageCta.ContinuePackage else AndroidPackageCta.StudyPackage
    }

    @Suppress("unused")
    private fun resolveCta(id: InstalledPackageId, contentCount: Int): AndroidPackageCta {
        if (contentCount == 0) return AndroidPackageCta.NoContent
        val activeSessionId = resolveActiveSessionId(id)
        if (activeSessionId != null) return AndroidPackageCta.ContinueLearning(activeSessionId)
        // Has content and package is active → offer Continue Package (scope exists)
        val isActive = runCatching {
            val libraryId = context.defaultLibraryId ?: return@runCatching false
            val tree = context.libraryQuery?.getNavigationTree(libraryId) ?: return@runCatching false
            tree.activePackageId?.let { it == id } ?: false
        }.getOrElse { false }
        return if (isActive) AndroidPackageCta.ContinuePackage else AndroidPackageCta.StudyPackage
    }

    private fun applyQuery(
        items: List<vn.loi.learning.application.contentpackaging.browser.PackageContentBrowserItem>,
        query: String
    ): List<AndroidPackageContentRow> {
        val rows = items.map { it.toRow() }
        if (query.isBlank()) return rows
        val normalized = normalizeQuery(query)
        return rows.filter { row -> row.searchableText.contains(normalized) }
    }

    private fun normalizeQuery(text: String): String {
        val normalized = java.text.Normalizer.normalize(text.trim(), java.text.Normalizer.Form.NFKC)
        return normalized.lowercase(java.util.Locale.ROOT)
    }
}
