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
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.contentpackaging.browser.BrowserMediaFilter
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.learning.model.LearningMode
import java.time.ZoneId
import vn.loi.learning.android.platform.AndroidStartupTrace

import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.android.reminder.AndroidVocabularyReminderDifficultMarkers
import java.time.Instant

/**
 * Android presentation facade for Package Experience.
 * All query authority stays in Application layer.
 * No repository calls from UI — this facade owns the projection boundary.
 */
class AndroidPackageFacade(
    private val context: LearningApplicationContext,
    private val dailyLimits: () -> DailyStudyBudgetLimits = { DailyStudyBudgetLimits() },
    private val difficultMarkers: AndroidVocabularyReminderDifficultMarkers? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val zoneId: () -> ZoneId = ZoneId::systemDefault,
    private val onActivePackageChanged: () -> Unit = {}
) {
    fun saveQuickEdit(draft: AndroidPackageQuickEditDraft): Result<Unit> = runCatching {
        require(draft.question.isNotBlank()) { "Question is required." }
        require(draft.answer.isNotBlank()) { "Answer is required." }
        requireNotNull(context.contentBrowserEdit) { "Content editor is unavailable." }.updateTextFields(
            vn.loi.learning.domain.content.model.ContentId(draft.contentId),
            draft.question.trim(), draft.answer.trim(), draft.pronunciation.trim(), draft.partOfSpeech.trim(),
            draft.example.trim(), draft.translation.trim()
        )
    }

    private val learnerId = LearnerId("default-learner")

    /**
     * Open a package by canonical [InstalledPackageId].
     * - Header: from InstalledPackageSummary (NavigationTree authority).
     * - Content: from PackageContentBrowserQueryService (no archive parse, no media bytes).
     * - CTA: from active session query and scope.
     * Returns [AndroidPackageContentState] — never throws.
     */
    fun openPackage(
        id: InstalledPackageId,
        filterSpec: AndroidPackageFilterSpec = AndroidPackageFilterSpec()
    ): AndroidPackageContentState = AndroidStartupTrace.measured("package_detail_total") { runCatching {
        val libraryId = requireNotNull(context.defaultLibraryId) { "Library is unavailable." }
        val tree = AndroidStartupTrace.measured("package_detail_navigation_tree") {
            requireNotNull(context.libraryQuery).getNavigationTree(libraryId)
        }
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

        val allItems = AndroidStartupTrace.measured("package_detail_browser_items") {
            requireNotNull(context.packageBrowserQuery) { "Content browser is unavailable." }
                .getBrowserItemsForPackage(id)
        }

        // Pass the already-loaded tree to avoid a second getNavigationTree call.
        val isActivePackage = tree.activePackageId == id
        val cta = AndroidStartupTrace.measured("package_detail_cta") {
            resolveCtaWithTree(id, allItems.size, isActivePackage)
        }

        if (allItems.isEmpty()) {
            return AndroidPackageContentState.Empty(header = header, cta = cta)
        }

        val memoryRepository = context.memoryStateRepository
        val memoryStates = ((memoryRepository as? MemoryStateQuery)?.findAll(learnerId)
            ?: memoryRepository?.findAll().orEmpty().filter { it.learnerId == learnerId })
            .associateBy { it.learningItemId }
        val markedIds = difficultMarkers?.markedContentIds() ?: emptySet()
        val nowMillis = now()
        val currentZone = zoneId()
        val startOfToday = Instant.ofEpochMilli(nowMillis).atZone(currentZone).toLocalDate().atStartOfDay(currentZone).toInstant().toEpochMilli()

        val allRows = allItems.map { item ->
            // Full Review already defines MEANING_RECOGNITION as Android's authoritative mode.
            // Never fall back to an unrelated mode or infer mode from an identifier.
            val primaryItemId = item.learningItemIds.zip(item.learningModes)
                .firstOrNull { (_, mode) -> mode == LearningMode.MEANING_RECOGNITION }
                ?.first
            val memState = primaryItemId?.let { memoryStates[it] }
            val fsrsStatus = resolveFsrsStatus(memState, nowMillis, startOfToday)
            val isDue = memState?.let { state ->
                state.reviewCount > 0 && state.stage != LearningStage.SUSPENDED && state.dueAt.epochMillis <= nowMillis
            } == true
            val isOverdue = isDue && requireNotNull(memState).dueAt.epochMillis < startOfToday
            val isDifficult = item.contentId in markedIds
            item.toRow(
                fsrsStatus = fsrsStatus,
                fsrsStageFilter = resolveFsrsStageFilter(memState),
                isDue = isDue,
                isOverdue = isOverdue,
                isDifficult = isDifficult
            )
        }

        val availableLessons = allRows.map { it.lesson }.distinct().filter { it.isNotBlank() }
        val visibleRows = applyFilters(allRows, filterSpec)

        AndroidPackageContentState.Content(
            header = header,
            cta = cta,
            allRows = allRows,
            visibleRows = visibleRows,
            query = filterSpec.query,
            filterSpec = filterSpec,
            availableLessons = availableLessons
        )
    }.getOrElse { AndroidPackageContentState.Failure(it.message ?: "Package could not be opened.") } }

    /**
     * Backward-compatible openPackage with query string.
     */
    fun openPackage(id: InstalledPackageId, query: String): AndroidPackageContentState =
        openPackage(id, AndroidPackageFilterSpec(query = query))

    fun applyFilters(
        allRows: List<AndroidPackageContentRow>,
        spec: AndroidPackageFilterSpec
    ): List<AndroidPackageContentRow> {
        val queryNormalized = if (spec.query.isNotBlank()) normalizeQuery(spec.query) else ""
        return allRows.filter { row ->
            // 1. Search Query
            if (queryNormalized.isNotEmpty() && !row.searchableText.contains(queryNormalized)) {
                return@filter false
            }
            // 2. Lesson filter
            if (spec.selectedLesson != null && row.lesson != spec.selectedLesson) {
                return@filter false
            }
            // 3. Difficult filter
            if (spec.difficultOnly && !row.isDifficult) {
                return@filter false
            }
            // 4. Existing browser media semantics
            val mediaMatches = when (spec.mediaFilter) {
                BrowserMediaFilter.ALL -> true
                BrowserMediaFilter.HAS_IMAGE -> row.hasImage
                BrowserMediaFilter.MISSING_IMAGE -> !row.hasImage
                BrowserMediaFilter.HAS_AUDIO -> row.hasAudio
                BrowserMediaFilter.MISSING_AUDIO -> !row.hasAudio
            }
            if (!mediaMatches) return@filter false
            // 5. FSRS Filter
            when (spec.fsrsFilter) {
                AndroidFsrsFilter.ALL -> true
                AndroidFsrsFilter.NEW -> row.fsrsStageFilter == AndroidFsrsFilter.NEW
                AndroidFsrsFilter.LEARNING -> row.fsrsStageFilter == AndroidFsrsFilter.LEARNING
                AndroidFsrsFilter.REVIEW -> row.fsrsStageFilter == AndroidFsrsFilter.REVIEW
                AndroidFsrsFilter.DUE -> row.isDue
                AndroidFsrsFilter.OVERDUE -> row.isOverdue
            }
        }
    }

    fun toggleDifficult(contentId: ContentId): Boolean {
        return difficultMarkers?.toggle(contentId) ?: false
    }

    fun resolveFsrsStatus(
        memoryState: MemoryState?,
        nowMillis: Long = now(),
        startOfToday: Long = Instant.ofEpochMilli(nowMillis).atZone(zoneId()).toLocalDate().atStartOfDay(zoneId()).toInstant().toEpochMilli()
    ): AndroidContentFsrsStatus {
        if (memoryState == null) return AndroidContentFsrsStatus.NEW
        val isDue = memoryState.reviewCount > 0 && memoryState.stage != LearningStage.SUSPENDED &&
            memoryState.dueAt.epochMillis <= nowMillis
        if (isDue) {
            // OVERDUE means due before the start of the current local calendar day.
            return if (memoryState.dueAt.epochMillis < startOfToday) {
                AndroidContentFsrsStatus.OVERDUE
            } else {
                AndroidContentFsrsStatus.DUE
            }
        }
        return when (memoryState.stage) {
            LearningStage.NEW -> AndroidContentFsrsStatus.NEW
            LearningStage.LEARNING, LearningStage.RELEARNING -> AndroidContentFsrsStatus.LEARNING
            LearningStage.REVIEW, LearningStage.MASTERED -> AndroidContentFsrsStatus.REVIEW
            LearningStage.SUSPENDED -> AndroidContentFsrsStatus.REVIEW
        }
    }

    private fun resolveFsrsStageFilter(memoryState: MemoryState?): AndroidFsrsFilter = when (memoryState?.stage) {
        null, LearningStage.NEW -> AndroidFsrsFilter.NEW
        LearningStage.LEARNING, LearningStage.RELEARNING -> AndroidFsrsFilter.LEARNING
        LearningStage.REVIEW, LearningStage.MASTERED -> AndroidFsrsFilter.REVIEW
        LearningStage.SUSPENDED -> AndroidFsrsFilter.ALL
    }

    /**
     * Apply a package-local search query on pre-loaded items.
     * Unicode-safe — uses pre-computed NFKC-normalized searchableText from browser projection.
     * No repository call — operates on cached allRows.
     */
    fun applySearch(current: AndroidPackageContentState.Content, query: String): AndroidPackageContentState.Content {
        val newSpec = current.filterSpec.copy(query = query)
        val visible = applyFilters(current.allRows, newSpec)
        return current.copy(visibleRows = visible, query = query, filterSpec = newSpec)
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
        runCatching(onActivePackageChanged)
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
        runCatching(onActivePackageChanged)
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
