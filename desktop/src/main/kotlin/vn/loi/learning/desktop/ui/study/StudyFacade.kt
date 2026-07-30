package vn.loi.learning.desktop.ui.study

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import vn.loi.learning.application.session.ActiveStudySessionRecovery
import vn.loi.learning.application.learningexperience.ExperienceRotationContext
import vn.loi.learning.application.session.LearningSessionProgress
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.UndoLatestSessionReviewResult
import vn.loi.learning.application.learningstrategy.ProductBrainPlanner
import vn.loi.learning.application.scene.TypingRecallScene
import vn.loi.learning.application.session.bootstrap.SessionOverview
import vn.loi.learning.application.session.completion.SessionCompletionInput
import vn.loi.learning.application.session.completion.SessionCompletionPlan
import vn.loi.learning.application.session.completion.SessionSchedulingOutcome

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.infrastructure.LearningApplicationContext
import vn.loi.learning.application.packageprogress.StudyHeaderStatistics
import vn.loi.learning.application.packageprogress.StudyStatisticsScope
import vn.loi.learning.application.packageprogress.StudySessionProgressSource

class StudyFacade(
    private val applicationContext:
    LearningApplicationContext,
    private val productBrainPlanner: ProductBrainPlanner = ProductBrainPlanner(),
    private val sessionPolicyProvider: () -> SessionPolicy = { SessionPolicy() }
) {
    fun refreshHeaderStatistics(
        state: StudyUiState,
        previous: StudyHeaderStatisticsState = state.headerStatistics
    ): StudyUiState {
        val query = applicationContext.studyHeaderStatistics
            ?: return state.copy(
                headerStatistics = StudyHeaderStatisticsState.Unavailable(previous.lastKnownGood())
            )
        val scope = resolveStatisticsScope(state)
            ?: return state.copy(
                headerStatistics = StudyHeaderStatisticsState.Unavailable(previous.lastKnownGood())
            )
        val sessionSource = resolveSessionProgressSource()
            ?: return state.copy(
                headerStatistics = StudyHeaderStatisticsState.Unavailable(previous.lastKnownGood())
            )
        return runCatching { query.execute(scope, sessionSource, learnerId) }
            .fold(
                onSuccess = {
                    state.copy(headerStatistics = StudyHeaderStatisticsState.Available(it))
                },
                onFailure = {
                    state.copy(
                        headerStatistics = StudyHeaderStatisticsState.Unavailable(previous.lastKnownGood())
                    )
                }
            )
    }

    private fun resolveSessionProgressSource(): StudySessionProgressSource? {
        val session = latestSession
            ?: activeSessionId?.let(applicationContext.engine::getSession)
            ?: return null
        val queue = applicationContext.engine.getStudyQueueProgress(session.id)
            ?: return null
        return StudySessionProgressSource(
            sessionId = session.id.value,
            newConfiguredTarget = session.policy.newItemLimit,
            reviewConfiguredTarget = session.policy.reviewItemLimit,
            newEffectiveWorkload = queue.effectiveNewWorkload,
            reviewEffectiveWorkload = queue.effectiveReviewWorkload,
            newCompleted = session.newItemsReviewed,
            reviewCompleted = session.reviewItemsReviewed,
            remainingLearningItemIds = queue.remainingLearningItemIds.toSet(),
            remainingItemOrigins = queue.itemOrigins,
            remainingItemContentIds = queue.itemContentIds
        )
    }

    private fun resolveStatisticsScope(state: StudyUiState): StudyStatisticsScope? {
        if (state.isLessonStudy) {
            val contentIds = state.activeContentId?.let(::setOf).orEmpty()
            return StudyStatisticsScope("lesson:${state.activeContentId?.value.orEmpty()}", contentIds)
        }
        state.activeInstalledPackageId?.let { packageId ->
            val ids = applicationContext.packageContentQuery
                ?.getContentsForPackage(packageId)
                ?.mapTo(linkedSetOf()) { ContentId(it.id) }
                ?: return null
            return StudyStatisticsScope("package:${packageId.value}", ids)
        }
        val ids = latestSession?.includedContentIds ?: includedContentIds
        if (ids.isNotEmpty()) {
            return StudyStatisticsScope(
                "session:${latestSession?.id?.value ?: activeSessionId?.value.orEmpty()}",
                ids
            )
        }
        return null
    }


    private val learnerId =
        LearnerId("default-learner")

    private var activeSessionId:
            SessionId? = null

    private var currentItem:
            NextSessionItem? = null

    private var presentedAtMillis:
            Long? = null

    private var latestSession:
            StudySession? = null

    private var includedContentIds:
            Set<ContentId> = emptySet()

    private var activeTopicId:
            TopicId? = null

    private var activeInstalledPackageId:
            vn.loi.learning.domain.library.model.InstalledPackageId? = null

    private var studyTitle:
            String = DEFAULT_STUDY_TITLE

    private var lessonStudy:
            Boolean = false

    private var totalItems:
            Int = 0

    private var latestSchedulerFeedback:
            StudySchedulerFeedback? = null

    private var latestProgress: LearningSessionProgress? = null

    private var adaptiveUiState: StudyUiState? = null
    private var latestSchedulingOutcome: SessionSchedulingOutcome? = null
    private var completionPresentationDismissed: Boolean = false

    fun dismissCompletionPresentation(): StudyUiState {
        completionPresentationDismissed = true
        return createIdleUiState()
    }

    fun load(): StudyUiState {
        val canonicalPkg = resolveCanonicalActivePackageId()

        if (applicationContext.installedPackageRepository == null || applicationContext.defaultLibraryId == null) {
            adaptiveUiState?.let { return it }
            rehydrateCurrentItem()?.let { nextItem ->
                return toUiState(nextItem, nextItem.session.answerRevealed)
            }
            if (activeSessionId == null && !completionPresentationDismissed) {
                val recovery = applicationContext.engine.recoverActiveSession(
                    learnerId = learnerId,
                    recoveredAt = Moment(System.currentTimeMillis())
                )
                when (recovery) {
                    ActiveStudySessionRecovery.NoActiveSession -> restoreLatestUndoableCompletion()?.let { return it }
                    is ActiveStudySessionRecovery.ClosedIncompleteSession ->
                        return restoreCompletedSession(recovery)
                    is ActiveStudySessionRecovery.Resumable ->
                        return restoreResumableSession(recovery, System.currentTimeMillis())
                }
            }
            return createIdleUiState()
        }

        if (lessonStudy) {
            adaptiveUiState?.let { return it }
            rehydrateCurrentItem()?.let { nextItem ->
                return toUiState(
                    nextSessionItem = nextItem,
                    answerRevealed = nextItem.session.answerRevealed
                )
            }
            return createIdleUiState()
        }

        if (canonicalPkg == null) {
            clearActiveStudyState()
            purgeStaleSessionsIfNoActivePackage()
            return createNoActiveTopicUiState()
        }

        if (activeInstalledPackageId != canonicalPkg) {
            if (activeSessionId == null || completionPresentationDismissed) {
                clearActiveStudyState()
                activeInstalledPackageId = canonicalPkg
                activeTopicId = resolveActiveTopicIdForPackage(canonicalPkg)
            }
        }

        adaptiveUiState?.let { state ->
            val isCompatible = state.activeInstalledPackageId == canonicalPkg
            if (isCompatible) {
                return state
            } else {
                adaptiveUiState = null
            }
        }

        rehydrateCurrentItem()?.let { nextItem ->
            val currentPkgId = activeInstalledPackageId
            val isCompatible = currentPkgId == canonicalPkg
            if (isCompatible) {
                return toUiState(
                    nextSessionItem = nextItem,
                    answerRevealed = nextItem.session.answerRevealed
                )
            } else {
                currentItem = null
            }
        }

        if (activeSessionId == null && !completionPresentationDismissed) {
            restoreActiveSession(canonicalPkg)?.let { restoredState ->
                return restoredState
            }
        }

        return createIdleUiState()
    }

    fun enterStudy(): StudyUiState {
        val latestPolicy = sessionPolicyProvider()
        val activeSession =
            activeSessionId?.let(applicationContext.engine::getSession)
                ?: applicationContext.engine.getActiveSession(learnerId)
                ?: return load()
        if (activeSession.policy.goalFingerprint() == latestPolicy.goalFingerprint()) {
            return load()
        }
        return replaceStaleGoalSession(activeSession, latestPolicy)
    }

    fun enterLearnEntry(): StudyUiState {
        val loaded = enterStudy()
        return loaded.copy(
            learnEntryChooserVisible = true,
            learnEntryReviewAvailability =
                currentLearnEntryAvailability() ?: loaded.learnEntryReviewAvailability
        )
    }

    fun continueSelectedLearning(): StudyUiState {
        if (applicationContext.engine.getActiveSession(learnerId) != null) {
            return enterStudy().copy(learnEntryChooserVisible = false)
        }
        return continueGeneralStudyAfterCompletion()
            .copy(learnEntryChooserVisible = false)
    }

    private fun leaveActivePracticeSession(nowMillis: Long) {
        applicationContext.engine.leaveActiveStudySession(
            learnerId = learnerId,
            leftAt = Moment(nowMillis)
        )
        clearActiveStudyState()
        completionPresentationDismissed = true
    }

    private fun replaceStaleGoalSession(
        staleSession: StudySession,
        latestPolicy: SessionPolicy
    ): StudyUiState {
        val now = Moment(System.currentTimeMillis())
        applicationContext.engine.finishSession(
            sessionId = staleSession.id,
            finishedAt = if (now >= staleSession.startedAt) now else staleSession.startedAt
        )

        clearActiveStudyState()
        activeInstalledPackageId = staleSession.installedPackageId
        activeTopicId = staleSession.topicId
        includedContentIds = staleSession.includedContentIds
        lessonStudy = includedContentIds.isNotEmpty()
        studyTitle =
            resolveStudyTitleForSession(
                topicId = activeTopicId,
                packageId = activeInstalledPackageId,
                contentIds = includedContentIds
            )
        return startSession(latestPolicy)
    }

    private fun rehydrateCurrentItem(): NextSessionItem? {
        val current = currentItem ?: return null
        val learner = learnerId
        val itemId = current.item.learningItem.id
        val latestMemoryState = applicationContext.engine.getMemoryState(learner, itemId)
        val updatedItem = if (latestMemoryState != null) {
            current.item.copy(
                memoryState = latestMemoryState,
                hasPersistedMemoryState = true
            )
        } else {
            current.item
        }
        val updated = current.copy(item = updatedItem)
        currentItem = updated
        return updated
    }

    private fun resolveActiveTopicIdForPackage(packageId: vn.loi.learning.domain.library.model.InstalledPackageId): TopicId? {
        val instPkg = applicationContext.installedPackageRepository?.findById(packageId)
        if (instPkg != null) return instPkg.topicId
        val contentPkg = applicationContext.contentPackageRepository?.findById(vn.loi.learning.domain.content.packaging.model.PackageId(packageId.value))
        return contentPkg?.topicId
    }

    private fun purgeStaleSession(sessionId: SessionId) {
        applicationContext.studyQueueRepository?.deleteBySessionId(sessionId)
        applicationContext.studySessionRepository?.deleteById(sessionId)
    }

    private fun purgeStaleSessionsIfNoActivePackage() {
        val sessionRepo = applicationContext.studySessionRepository ?: return
        val queueRepo = applicationContext.studyQueueRepository
        sessionRepo.findAll()
            .filter { it.status == vn.loi.learning.domain.study.session.model.SessionStatus.ACTIVE }
            .filterNot { it.installedPackageId == null && it.includedContentIds.isNotEmpty() }
            .forEach { session ->
                queueRepo?.deleteBySessionId(session.id)
                sessionRepo.deleteById(session.id)
            }
    }

    private fun isRestorableGeneralSession(
        session: StudySession,
        canonicalPkg: vn.loi.learning.domain.library.model.InstalledPackageId,
        queue: vn.loi.learning.application.session.StudyQueueProgress?
    ): Boolean {
        val sessionPkgId = session.installedPackageId ?: return false
        if (sessionPkgId != canonicalPkg) return false

        val installedPackage = applicationContext.installedPackageRepository
            ?.findById(sessionPkgId)
            ?.takeIf { it.state == vn.loi.learning.domain.library.model.PackageState.ACTIVE }
            ?: return false
        if (session.topicId != installedPackage.topicId) return false
        if (session.startedAt.epochMillis < installedPackage.installedAt.toEpochMilli()) return false

        val ownedContentIds = applicationContext.packageContentQuery
            ?.getContentsForPackage(sessionPkgId)
            ?.mapTo(HashSet()) { ContentId(it.id) }
            ?: return false
        if (!ownedContentIds.containsAll(session.includedContentIds)) return false

        val queuedItemIds = buildSet {
            queue?.completedLearningItemIds?.let(::addAll)
            queue?.remainingLearningItemIds?.let(::addAll)
            session.currentLearningItemId?.let(::add)
        }
        return queuedItemIds.all { itemId ->
            applicationContext.learningItemRepository
                ?.findById(itemId)
                ?.contentId in ownedContentIds
        }
    }

    private fun isRestorableSession(
        session: StudySession,
        canonicalPkg: vn.loi.learning.domain.library.model.InstalledPackageId,
        queue: vn.loi.learning.application.session.StudyQueueProgress?
    ): Boolean {
        val canonicalTopicId = applicationContext.installedPackageRepository
            ?.findById(canonicalPkg)
            ?.topicId
        val isManualLessonScope = session.installedPackageId == null &&
            session.includedContentIds.isNotEmpty() &&
            session.topicId != canonicalTopicId
        val isPackageLessonScope = session.installedPackageId != null &&
            session.includedContentIds.isNotEmpty()
        val isExplicitLessonScope = isManualLessonScope || isPackageLessonScope
        if (!isExplicitLessonScope) {
            return isRestorableGeneralSession(session, canonicalPkg, queue)
        }

        val sessionPackage = session.installedPackageId?.let { packageId ->
            applicationContext.installedPackageRepository?.findById(packageId)
        }
        if (session.installedPackageId != null) {
            if (sessionPackage == null ||
                sessionPackage.state == vn.loi.learning.domain.library.model.PackageState.REMOVED ||
                sessionPackage.topicId != session.topicId ||
                session.startedAt.epochMillis < sessionPackage.installedAt.toEpochMilli()
            ) {
                return false
            }
        }

        val queuedItemIds = buildSet {
            queue?.completedLearningItemIds?.let(::addAll)
            queue?.remainingLearningItemIds?.let(::addAll)
            session.currentLearningItemId?.let(::add)
        }
        return queuedItemIds.all { itemId ->
            applicationContext.learningItemRepository
                ?.findById(itemId)
                ?.contentId in session.includedContentIds
        }
    }

    private fun restoreActiveSession(
        canonicalPkg: vn.loi.learning.domain.library.model.InstalledPackageId
    ): StudyUiState? {
        val nowMillis = System.currentTimeMillis()

        val recovery = applicationContext.engine.recoverActiveSession(
            learnerId = learnerId,
            recoveredAt = Moment(nowMillis)
        )

        return when (recovery) {
            ActiveStudySessionRecovery.NoActiveSession -> restoreLatestUndoableCompletion()

            is ActiveStudySessionRecovery.ClosedIncompleteSession -> {
                val session = recovery.session
                if (!isRestorableSession(session, canonicalPkg, recovery.queueProgress)) {
                    purgeStaleSession(session.id)
                    clearActiveStudyState()
                    return null
                }
                when (recovery.reason) {
                    ActiveStudySessionRecovery.ClosedIncompleteSession.Reason.MISSING_QUEUE -> {
                        clearActiveStudyState()
                        createIdleUiState(
                            message = "The previous study session could not be resumed because " +
                                "its saved queue was missing. Start a new session."
                        )
                    }
                    ActiveStudySessionRecovery.ClosedIncompleteSession.Reason.COMPLETED_QUEUE ->
                        restoreCompletedSession(recovery)
                }
            }

            is ActiveStudySessionRecovery.Resumable -> {
                val session = recovery.session
                if (!isRestorableSession(session, canonicalPkg, recovery.queueProgress)) {
                    purgeStaleSession(session.id)
                    clearActiveStudyState()
                    return null
                }
                restoreResumableSession(
                    recovery = recovery,
                    nowMillis = nowMillis
                )
            }
        }
    }

    private fun restoreLatestUndoableCompletion(): StudyUiState? {
        val canonicalPkg = resolveCanonicalActivePackageId()
        purgeLegacyUndoableCompletionsWithoutOwnership()
        val session = applicationContext.engine.getLatestUndoableSession(learnerId) ?: return null
        if (session.status != vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED) return null
        val queue = applicationContext.engine.getStudyQueueProgress(session.id)
        if (session.installedPackageId == null || session.topicId == null) {
            purgeStaleSession(session.id)
            return null
        }
        if (
            (canonicalPkg == null || session.installedPackageId != canonicalPkg)
        ) {
            return null
        }
        if (!isRestorableCompletedPackageSession(session, canonicalPkg, queue)) {
            purgeStaleSession(session.id)
            return null
        }
        queue ?: return null
        return restoreCompletedSession(
            ActiveStudySessionRecovery.ClosedIncompleteSession(
                session = session,
                reason = ActiveStudySessionRecovery.ClosedIncompleteSession.Reason.COMPLETED_QUEUE,
                queueProgress = queue
            )
        )
    }

    private fun purgeLegacyUndoableCompletionsWithoutOwnership() {
        applicationContext.studySessionRepository
            ?.findAll()
            .orEmpty()
            .filter { session ->
                session.learnerId == learnerId &&
                    session.status == vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED &&
                    session.undoableReview != null &&
                    (session.installedPackageId == null || session.topicId == null)
            }
            .forEach { session -> purgeStaleSession(session.id) }
    }

    private fun isRestorableCompletedPackageSession(
        session: StudySession,
        canonicalPkg: vn.loi.learning.domain.library.model.InstalledPackageId?,
        queue: vn.loi.learning.application.session.StudyQueueProgress?
    ): Boolean {
        val sessionPackageId = session.installedPackageId ?: return false
        if (session.topicId == null) return false
        val installedPackageRepository = applicationContext.installedPackageRepository
            ?: return false
        val installedPackage = installedPackageRepository.findById(sessionPackageId)
            ?.takeIf { it.state == vn.loi.learning.domain.library.model.PackageState.ACTIVE }
            ?: return false
        if (canonicalPkg == null || sessionPackageId != canonicalPkg) return false
        if (session.topicId != installedPackage.topicId) return false
        if (session.startedAt.epochMillis < installedPackage.installedAt.toEpochMilli()) return false

        val ownedContentIds = applicationContext.packageContentQuery
            ?.getContentsForPackage(sessionPackageId)
            ?.mapTo(HashSet()) { ContentId(it.id) }
            ?: return false
        val queuedLearningItemIds = buildSet {
            queue?.completedLearningItemIds?.let(::addAll)
            queue?.remainingLearningItemIds?.let(::addAll)
            queue?.currentLearningItemId?.let(::add)
            session.currentLearningItemId?.let(::add)
        }
        if (queue == null || queuedLearningItemIds.isEmpty()) return false
        return queuedLearningItemIds.all { learningItemId ->
            applicationContext.learningItemRepository
                ?.findById(learningItemId)
                ?.contentId in ownedContentIds
        }
    }

    private fun restoreCompletedSession(
        recovery: ActiveStudySessionRecovery.ClosedIncompleteSession
    ): StudyUiState {
        val session = recovery.session
        val progress = LearningSessionProgress.from(
            session,
            requireNotNull(recovery.queueProgress) {
                "Completed queue recovery requires queue progress."
            }
        )
        activeSessionId = null
        currentItem = null
        presentedAtMillis = null
        latestSession = session
        activeTopicId = session.topicId
        includedContentIds = session.includedContentIds
        activeInstalledPackageId = session.installedPackageId
        lessonStudy = session.includedContentIds.isNotEmpty()
        studyTitle = if (session.includedContentIds.isNotEmpty()) {
            resolveRestoredStudyTitle(session.includedContentIds)
        } else {
            resolveTopicTitle(session.topicId, session.installedPackageId)
                ?: resolveRestoredStudyTitle(session.includedContentIds)
        }
        totalItems = requireNotNull(progress.totalItemCount)
        latestProgress = progress
        latestSchedulerFeedback = null
        return StudyUiState(
            sessionStarted = true,
            activeInstalledPackageId = session.installedPackageId,
            activeContentId = session.includedContentIds.singleOrNull(),
            studyTitle = studyTitle,
            isLessonStudy = lessonStudy,
            reviewedCount = session.totalReviews,
            newItemsReviewed = session.newItemsReviewed,
            reviewItemsReviewed = session.reviewItemsReviewed,
            totalItems = totalItems,
            currentItemPosition = progress.completedItemCount,
            sessionCompleted = true,
            canUndo = session.undoableReview != null,
            sessionProgress = progress,
            sessionCompletion = session.completionSnapshot,
            learnEntryReviewAvailability = learnEntryAvailabilityFor(session),
            message = "The previous study session was complete and has been finalized.",
            workspaceState = ReviewWorkspaceState.Completed
        )
    }

    private fun restoreResumableSession(
        recovery: ActiveStudySessionRecovery.Resumable,
        nowMillis: Long
    ): StudyUiState {
        val session =
            recovery.session

        val canonicalPkg = resolveCanonicalActivePackageId()
        if (canonicalPkg != null && session.installedPackageId != null && session.installedPackageId != canonicalPkg) {
            return createIdleUiState()
        }

        activeSessionId =
            session.id
        activeTopicId =
            session.topicId
        activeInstalledPackageId =
            session.installedPackageId

        latestSession =
            session

        includedContentIds =
            session.includedContentIds

        lessonStudy =
            includedContentIds.isNotEmpty()

        studyTitle =
            if (includedContentIds.isNotEmpty()) {
                resolveRestoredStudyTitle(includedContentIds)
            } else {
                resolveTopicTitle(session.topicId, session.installedPackageId)
                    ?: resolveRestoredStudyTitle(includedContentIds)
            }

        totalItems =
            recovery
                .queueProgress
                .totalItemCount
        latestProgress = LearningSessionProgress.from(session, recovery.queueProgress)

        latestSchedulerFeedback =
            null

        val restored = loadNextItem(
            sessionId = session.id,
            now = Moment(nowMillis),
            nowMillis = nowMillis,
            emptyMessage =
                "Restored study session completed."
        )
        presentedAtMillis = session.currentItemPresentedAt?.epochMillis ?: nowMillis
        return if (recovery.answerRevealed && currentItem != null) {
            toUiState(requireNotNull(currentItem), answerRevealed = true)
        } else {
            restored
        }
    }

    private fun clearActiveStudyState() {
        adaptiveUiState = null
        activeSessionId = null
        currentItem = null
        presentedAtMillis = null
        latestSession = null
        activeTopicId = null
        activeInstalledPackageId = null
        includedContentIds = emptySet()
        studyTitle = DEFAULT_STUDY_TITLE
        lessonStudy = false
        totalItems = 0
        latestSchedulerFeedback = null
        latestProgress = null
        latestSchedulingOutcome = null
        completionPresentationDismissed = false
    }

    private fun resolveTopicTitle(
        topicId: TopicId?,
        packageId: vn.loi.learning.domain.library.model.InstalledPackageId?
    ): String? {
        if (packageId != null) {
            val instPkg = applicationContext.installedPackageRepository?.findById(packageId)
            if (instPkg != null) return instPkg.name.value
            val contentPkg = applicationContext.contentPackageRepository?.findById(
                vn.loi.learning.domain.content.packaging.model.PackageId(packageId.value)
            )
            if (contentPkg != null) return contentPkg.name
            val pkgItem = applicationContext.installedPackages.findById(packageId.value)
            if (pkgItem != null) return pkgItem.name
        }
        if (topicId != null) {
            val instPkg = applicationContext.installedPackageRepository?.findAll()
                ?.firstOrNull { it.topicId == topicId || it.id.value == topicId.value || it.packageId.value == topicId.value }
            if (instPkg != null) return instPkg.name.value

            val contentPkg = applicationContext.contentPackageRepository?.findAll()
                ?.firstOrNull { it.topicId == topicId || it.id.value == topicId.value }
            if (contentPkg != null) return contentPkg.name

            val pkgItem = applicationContext.installedPackages.findById(topicId.value)
            if (pkgItem != null) return pkgItem.name

            if (topicId.value.isNotBlank() && !topicId.value.startsWith("topic-")) {
                return topicId.value
            }
        }
        return null
    }

    private fun resolveStudyTitleForSession(
        topicId: TopicId?,
        packageId: vn.loi.learning.domain.library.model.InstalledPackageId?,
        contentIds: Set<ContentId> = emptySet()
    ): String {
        val topicName = resolveTopicTitle(topicId, packageId)
        if (topicName != null) {
            return topicName
        }
        if (contentIds.isNotEmpty()) {
            val restoredTitle = resolveRestoredStudyTitle(contentIds)
            if (restoredTitle != DEFAULT_STUDY_TITLE) {
                return restoredTitle
            }
        }
        return DEFAULT_STUDY_TITLE
    }

    private fun resolveRestoredStudyTitle(
        contentIds: Set<ContentId>
    ): String {
        if (contentIds.isEmpty()) {
            return DEFAULT_STUDY_TITLE
        }

        val firstContent =
            contentIds
                .asSequence()
                .mapNotNull { contentId ->
                    applicationContext
                        .engine
                        .getContent(
                            contentId
                        )
                }
                .firstOrNull()
                ?: return "Selected lesson"

        return firstContent.metadata.lesson
            ?: DEFAULT_STUDY_TITLE
    }

    fun startStudy(): StudyUiState {
        clearActiveStudyState()
        val canonicalPkg = resolveCanonicalActivePackageId()
            ?: return createNoActiveTopicUiState()

        activeInstalledPackageId = canonicalPkg
        activeTopicId = resolveActiveTopicIdForPackage(canonicalPkg)
        includedContentIds = emptySet()
        studyTitle = DEFAULT_STUDY_TITLE
        lessonStudy = false
        totalItems = 0
        latestSchedulerFeedback = null
        latestProgress = null
        latestSchedulingOutcome = null

        return startSession()
    }

    fun continueGeneralStudyAfterCompletion(): StudyUiState {
        val completedSession =
            latestSession
                ?.takeIf { session ->
                    !lessonStudy &&
                        session.status ==
                        vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED
                }
                ?: return startStudy()
        val packageId =
            completedSession.installedPackageId
                ?: return load().copy(
                    message = "The completed Study session has no general-study package scope."
                )
        val nowMillis = System.currentTimeMillis()

        return when (
            val continuation =
                applicationContext.engine.continueGeneralStudy(
                    vn.loi.learning.application.session.ContinueGeneralStudyRequest(
                        precedingSessionId = completedSession.id,
                        learnerId = learnerId,
                        requestedAt = Moment(nowMillis),
                        policy = sessionPolicyProvider(),
                        installedPackageId = packageId,
                        topicId = completedSession.topicId
                    )
                )
        ) {
            is vn.loi.learning.application.session.GeneralStudyContinuationResult.Accepted -> {
                clearActiveStudyState()
                latestSession = continuation.session
                activeSessionId = continuation.session.id
                activeInstalledPackageId = continuation.session.installedPackageId
                activeTopicId = continuation.session.topicId
                includedContentIds = emptySet()
                lessonStudy = false
                studyTitle =
                    resolveStudyTitleForSession(
                        continuation.session.topicId,
                        continuation.session.installedPackageId,
                        emptySet()
                    )
                totalItems = continuation.queue.totalItemCount
                latestProgress =
                    LearningSessionProgress.from(
                        continuation.session,
                        applicationContext.engine.requireStudyQueueProgress(
                            continuation.session.id
                        )
                    )
                loadNextItem(
                    sessionId = continuation.session.id,
                    now = Moment(nowMillis),
                    nowMillis = nowMillis,
                    emptyMessage = "No learning items available."
                )
            }

            vn.loi.learning.application.session.GeneralStudyContinuationResult.NoWork -> {
                clearActiveStudyState()
                completionPresentationDismissed = true
                createIdleUiState(
                    message =
                        "No learning items are currently available. " +
                            "Check back when a review is due."
                )
            }

            is vn.loi.learning.application.session.GeneralStudyContinuationResult.Rejected ->
                load().copy(
                    message =
                        "Unable to continue this Study session: " +
                            continuation.reason.name.lowercase().replace('_', ' ') +
                            "."
                )
        }
    }

    fun replayCompletedStudySession(): StudyUiState {
        val predecessor =
            latestSession
                ?.takeIf {
                    it.status == vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED
                }
                ?: return load().copy(message = "No completed Study session is available to replay.")
        return replayCompletedStudySession(predecessor.id)
    }

    fun replayLatestCompletedStudySession(): StudyUiState {
        val completionAvailability =
            latestSession
                ?.takeIf {
                    !completionPresentationDismissed &&
                        it.status ==
                            vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED
                }
                ?.let(::learnEntryAvailabilityFor)
        val predecessor =
            (completionAvailability ?: currentLearnEntryAvailability())?.latestCompletedSession
                as? vn.loi.learning.application.session.LatestCompletedSessionAvailability.Available
                ?: return load().copy(message = "No completed Study session is available to replay.")
        return replayCompletedStudySession(predecessor.sessionId)
    }

    private fun replayCompletedStudySession(
        predecessorSessionId: vn.loi.learning.domain.study.session.model.SessionId
    ): StudyUiState {
        val nowMillis = System.currentTimeMillis()
        leaveActivePracticeSession(nowMillis)

        return when (
            val replay =
                applicationContext.engine.replayCompletedStudySession(
                    vn.loi.learning.application.session.ReplayCompletedStudySessionRequest(
                        precedingSessionId = predecessorSessionId,
                        learnerId = learnerId,
                        requestedAt = Moment(nowMillis)
                    )
                )
        ) {
            is vn.loi.learning.application.session.CompletedStudySessionReplayResult.Accepted -> {
                clearActiveStudyState()
                latestSession = replay.session
                activeSessionId = replay.session.id
                activeInstalledPackageId = replay.session.installedPackageId
                activeTopicId = replay.session.topicId
                includedContentIds = replay.session.includedContentIds
                lessonStudy = includedContentIds.isNotEmpty()
                studyTitle =
                    resolveStudyTitleForSession(
                        replay.session.topicId,
                        replay.session.installedPackageId,
                        includedContentIds
                    )
                totalItems = replay.queue.totalItemCount
                latestProgress =
                    LearningSessionProgress.from(
                        replay.session,
                        applicationContext.engine.requireStudyQueueProgress(replay.session.id)
                    )
                loadNextItem(
                    sessionId = replay.session.id,
                    now = Moment(nowMillis),
                    nowMillis = nowMillis,
                    emptyMessage = "No items from the completed session remain available."
                )
            }

            vn.loi.learning.application.session.CompletedStudySessionReplayResult.NoItems ->
                load().copy(message = "The completed session has no items available to review again.")

            is vn.loi.learning.application.session.CompletedStudySessionReplayResult.Rejected ->
                load().copy(
                    message =
                        "Unable to review the completed session again: " +
                            replay.reason.name.lowercase().replace('_', ' ') +
                            "."
                )
        }
    }

    fun startLearnedItemsReview(): StudyUiState {
        val packageId =
            resolveCanonicalActivePackageId()
                ?: return createNoActiveTopicUiState()
        val nowMillis = System.currentTimeMillis()
        leaveActivePracticeSession(nowMillis)
        return when (
            val result = applicationContext.engine.startLearnedItemsReview(
                vn.loi.learning.application.session.StartLearnedItemsReviewRequest(
                    scope = vn.loi.learning.application.session.LearnEntryScope(
                        learnerId = learnerId,
                        installedPackageId = packageId,
                        topicId = resolveActiveTopicIdForPackage(packageId)
                    ),
                    requestedAt = Moment(nowMillis)
                )
            )
        ) {
            is vn.loi.learning.application.session.StartLearnedItemsReviewResult.Accepted -> {
                clearActiveStudyState()
                latestSession = result.session
                activeSessionId = result.session.id
                activeInstalledPackageId = result.session.installedPackageId
                activeTopicId = result.session.topicId
                includedContentIds = result.session.includedContentIds
                lessonStudy = false
                studyTitle = DEFAULT_STUDY_TITLE
                totalItems = result.queue.totalItemCount
                latestProgress = LearningSessionProgress.from(
                    result.session,
                    applicationContext.engine.requireStudyQueueProgress(result.session.id)
                )
                loadNextItem(
                    sessionId = result.session.id,
                    now = Moment(nowMillis),
                    nowMillis = nowMillis,
                    emptyMessage = "No learned items remain available."
                )
            }
            vn.loi.learning.application.session.StartLearnedItemsReviewResult.NoItems ->
                createIdleUiState(message = "Chưa có item đã học để ôn lại.")
            is vn.loi.learning.application.session.StartLearnedItemsReviewResult.Rejected ->
                createIdleUiState(
                    message = "Unable to start learned-items review: " +
                        result.reason.name.lowercase().replace('_', ' ') + "."
                )
        }
    }

    fun startLessonStudy(
        request: vn.loi.learning.application.session.StartPackageLessonStudyRequest
    ): StudyUiState {
        val installedPackageId = request.installedPackageId
        val contentId = request.contentId

        // Step 1: Validate InstalledPackage exists
        val defaultLibId = applicationContext.defaultLibraryId
        val libQuery = applicationContext.libraryQuery
        val navTree = if (libQuery != null && defaultLibId != null) {
            libQuery.getNavigationTree(defaultLibId)
        } else null

        val activeSummary = navTree?.activePackages?.firstOrNull { it.id == installedPackageId }
        val archivedSummary = navTree?.archivedPackages?.firstOrNull { it.id == installedPackageId }

        if (archivedSummary != null) {
            clearActiveStudyState()
            return StudyUiState(
                hasActiveSession = false,
                loadError = "Cannot start study: package '${archivedSummary.name}' is ARCHIVED.",
                failureKind = StudyFailureKind.PREPARATION,
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

        val allInstalled = applicationContext.installedPackages.query()
        val pkgItem = if (activeSummary != null) {
            allInstalled.firstOrNull { it.id == activeSummary.packageId.value }
                ?: applicationContext.installedPackages.findById(activeSummary.packageId.value)
        } else {
            allInstalled.firstOrNull { it.id == installedPackageId.value }
        }

        if (activeSummary == null && pkgItem == null) {
            clearActiveStudyState()
            return StudyUiState(
                hasActiveSession = false,
                loadError = "Package with id '${installedPackageId.value}' not found.",
                failureKind = StudyFailureKind.PREPARATION,
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

        // Step 2: Validate Package state is ACTIVE
        if (activeSummary == null && navTree != null) {
            clearActiveStudyState()
            return StudyUiState(
                hasActiveSession = false,
                loadError = "Package with id '${installedPackageId.value}' is not active in default library.",
                failureKind = StudyFailureKind.PREPARATION,
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

        // Step 3: Validate lesson content belongs strictly to this InstalledPackage
        val contentLibraryIds = mutableSetOf<vn.loi.learning.domain.content.library.model.ContentLibraryId>()
        if (pkgItem != null && pkgItem.libraryIds.isNotEmpty()) {
            contentLibraryIds.addAll(pkgItem.libraryIds.map { vn.loi.learning.domain.content.library.model.ContentLibraryId(it) })
        }
        contentLibraryIds.add(vn.loi.learning.domain.content.library.model.ContentLibraryId(installedPackageId.value))
        if (activeSummary != null) {
            contentLibraryIds.add(vn.loi.learning.domain.content.library.model.ContentLibraryId(activeSummary.packageId.value))
        }

        val packageContents = applicationContext.libraryContents.queryForLibraries(contentLibraryIds)
        val contentInPackage = packageContents.firstOrNull { it.id == contentId.value }
        if (contentInPackage == null) {
            clearActiveStudyState()
            val pkgName = activeSummary?.name ?: (pkgItem?.name ?: installedPackageId.value)
            return StudyUiState(
                hasActiveSession = false,
                loadError = "Lesson '${contentId.value}' does not belong to package '$pkgName' (${installedPackageId.value}).",
                failureKind = StudyFailureKind.PREPARATION,
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

        // Step 4: Validate content exists in engine and has enabled learning items
        val selectedContent = applicationContext.engine.getContent(contentId)
        if (selectedContent == null) {
            clearActiveStudyState()
            return StudyUiState(
                hasActiveSession = false,
                loadError = "Content '${contentId.value}' does not exist.",
                failureKind = StudyFailureKind.PREPARATION,
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

        val learningItems = applicationContext.engine.getLearningItemsByContentId(contentId)
            .filter { it.isEnabled }
        if (learningItems.isEmpty()) {
            clearActiveStudyState()
            return StudyUiState(
                hasActiveSession = false,
                loadError = "Lesson '${contentId.value}' has no enabled learning items available.",
                failureKind = StudyFailureKind.PREPARATION,
                workspaceState = ReviewWorkspaceState.RecoverableFailure
            )
        }

        // Step 5: Execute lesson study session with package provenance
        return prepareLessonStudy(contentId = contentId, targetPackageId = installedPackageId)
    }

    fun startLessonStudy(
        contentId: String
    ): StudyUiState {
        return prepareLessonStudy(contentId = ContentId(contentId), targetPackageId = null)
    }

    private fun prepareLessonStudy(
        contentId: ContentId,
        targetPackageId: vn.loi.learning.domain.library.model.InstalledPackageId?
    ): StudyUiState {
        if (targetPackageId != null) {
            val ownedContents = applicationContext.packageContentQuery?.getContentsForPackage(targetPackageId)
                ?.map { ContentId(it.id) }?.toSet() ?: emptySet()
            if (ownedContents.isNotEmpty() && !ownedContents.contains(contentId)) {
                clearActiveStudyState()
                return StudyUiState(
                    hasActiveSession = false,
                    loadError = "Content ${contentId.value} does not belong to package ${targetPackageId.value}.",
                    failureKind = StudyFailureKind.PREPARATION,
                    workspaceState = ReviewWorkspaceState.Idle
                )
            }
        }
        val selectedContent =
            requireNotNull(
                applicationContext
                    .engine
                    .getContent(
                        contentId
                    )
            ) {
                "Content ${contentId.value} does not exist."
            }

        val selectedMetadata =
            selectedContent.metadata

        val lessonContent =
            applicationContext
                .engine
                .getAllContent()
                .filter { candidate ->
                    candidate.metadata.source ==
                            selectedMetadata.source &&
                            candidate.metadata.group ==
                            selectedMetadata.group &&
                            candidate.metadata.section ==
                            selectedMetadata.section &&
                            candidate.metadata.lesson ==
                            selectedMetadata.lesson
                }
                .ifEmpty {
                    listOf(selectedContent)
                }

        val topicId =
            applicationContext
                .topics
                ?.requireByContentId(
                    contentId = contentId,
                    compatibleScopeContentIds =
                        lessonContent
                            .map { content ->
                                content.id
                            }
                            .toSet()
                )
                ?.id
                ?: TopicId.deriveForUnpackagedContent(
                    lessonContent
                        .minBy { content ->
                            content.id.value
                        }
                        .id
                )

        clearActiveStudyState()
        activeInstalledPackageId = targetPackageId

        includedContentIds =
            lessonContent
                .map { content ->
                    content.id
                }
                .toSet()

        studyTitle =
            selectedMetadata.lesson
                ?: resolveTopicTitle(topicId, targetPackageId)
                ?: DEFAULT_STUDY_TITLE

        lessonStudy =
            true

        latestSchedulerFeedback =
            null

        latestProgress = null
        latestSchedulingOutcome = null

        totalItems =
            lessonContent
                .sumOf { content ->
                    applicationContext
                        .engine
                        .getLearningItemsByContentId(
                            content.id
                        )
                        .count { learningItem ->
                            learningItem.isEnabled
                        }
                }

        activeTopicId = topicId

        restoreTopicSession(
            topicId
        )?.let { restored ->
            return restored
        }

        return startSession()
    }

    private fun restoreTopicSession(
        topicId: TopicId
    ): StudyUiState? {
        val nowMillis =
            System.currentTimeMillis()

        return when (
            val recovery =
                applicationContext
                    .engine
                    .recoverTopicSession(
                        learnerId = learnerId,
                        topicId = topicId,
                        recoveredAt = Moment(nowMillis)
                    )
        ) {
            ActiveStudySessionRecovery.NoActiveSession ->
                null

            is ActiveStudySessionRecovery.ClosedIncompleteSession ->
                null

            is ActiveStudySessionRecovery.Resumable ->
                restoreResumableSession(
                    recovery = recovery,
                    nowMillis = nowMillis
                )
        }
    }

    private fun startSession(
        policy: SessionPolicy = sessionPolicyProvider()
    ): StudyUiState {
        completionPresentationDismissed = false
        adaptiveUiState = null
        val nowMillis =
            System.currentTimeMillis()

        val now =
            Moment(nowMillis)

        val sessionId =
            SessionId(
                UUID.randomUUID()
                    .toString()
            )

        val canonicalPkg = resolveCanonicalActivePackageId()
        val targetPackageId = if (lessonStudy || includedContentIds.isNotEmpty()) {
            activeInstalledPackageId
        } else {
            canonicalPkg ?: activeInstalledPackageId
        }
        val isGeneralStudy = !lessonStudy && includedContentIds.isEmpty()
        if (isGeneralStudy && targetPackageId == null) {
            return createNoActiveTopicUiState()
        }

        if (targetPackageId != null && includedContentIds.isNotEmpty()) {
            val ownedContents = applicationContext.packageContentQuery?.getContentsForPackage(targetPackageId)
                ?.map { ContentId(it.id) }?.toSet() ?: emptySet()
            if (ownedContents.isNotEmpty() && !ownedContents.containsAll(includedContentIds)) {
                clearActiveStudyState()
                return StudyUiState(
                    hasActiveSession = false,
                    loadError = "Nội dung không thuộc về gói nội dung đã chọn.",
                    failureKind = StudyFailureKind.PREPARATION,
                    workspaceState = ReviewWorkspaceState.Idle
                )
            }
        }

        val targetTopicId = targetPackageId?.let { resolveActiveTopicIdForPackage(it) } ?: activeTopicId
        val packageContentIds = if (includedContentIds.isEmpty() && targetPackageId != null) {
            val queryContents = applicationContext.packageContentQuery?.getContentsForPackage(targetPackageId)
                ?.map { ContentId(it.id) }?.toSet() ?: emptySet()
            queryContents
        } else {
            includedContentIds
        }

        if (!lessonStudy || studyTitle == DEFAULT_STUDY_TITLE) {
            studyTitle = resolveStudyTitleForSession(targetTopicId, targetPackageId, packageContentIds)
        }

        latestSession =
            applicationContext
                .engine
                .startSession(
                    StartStudySessionCommand(
                        sessionId = sessionId,
                        learnerId = learnerId,
                        startedAt = now,
                        includedContentIds =
                            if (isGeneralStudy) emptySet() else packageContentIds,
                        topicId =
                            targetTopicId,
                        installedPackageId =
                            targetPackageId,
                        policy = policy
                    )
                )

        activeSessionId =
            sessionId

        totalItems =
            applicationContext
                .studyQueue
                .require(sessionId)
                .totalItemCount
        latestProgress = applicationContext.engine
            .requireStudyQueueProgress(sessionId)
            .let { LearningSessionProgress.from(requireNotNull(latestSession), it) }

        if (totalItems == 0) {
            purgeStaleSession(sessionId)
            activeSessionId = null
            latestSession = null
            latestProgress = null
            completionPresentationDismissed = true
            return createIdleUiState(
                message = "No learning items are currently available. Check back when a review is due."
            )
        }

        return loadNextItem(
            sessionId = sessionId,
            now = now,
            nowMillis = nowMillis,
            emptyMessage =
                if (lessonStudy) {
                    "No learning items available " +
                            "for this lesson."
                } else {
                    "No learning items available."
                }
        )
    }

    fun revealAnswer(): StudyUiState {
        val nextItem =
            currentItem
                ?: return createIdleUiState(
                    message =
                        "No active learning item."
                )

        ReviewWorkspaceStateMachine.dispatch(
            state = workspaceState(nextItem),
            action = ReviewWorkspaceAction.ShowAnswer
        )

        val revealedSession = applicationContext.engine.revealSessionItem(
            sessionId = requireNotNull(activeSessionId),
            learningItemId = nextItem.item.learningItem.id
        )
        currentItem = nextItem.copy(session = revealedSession)
        return toUiState(
            nextSessionItem = requireNotNull(currentItem),
            answerRevealed = true
        )
    }

    fun completeContentIntroduction(revealAnswer: Boolean): StudyUiState {
        val nextItem = currentItem
            ?: return createIdleUiState(message = "No active learning item.")
        val updatedSession = applicationContext.engine.completeContentIntroduction(
            sessionId = requireNotNull(activeSessionId),
            contentId = nextItem.item.content.id,
            learningItemId = nextItem.item.learningItem.id.takeIf { revealAnswer }
        )
        latestSession = updatedSession
        currentItem = nextItem.copy(session = updatedSession)
        return toUiState(requireNotNull(currentItem), answerRevealed = revealAnswer)
    }

    fun review(
        rating: ReviewRating
    ): StudyUiState =
        reviewInternal(
            rating = rating,
            completionPlan = null
        )

    private fun reviewInternal(
        rating: ReviewRating,
        completionPlan: SessionCompletionPlan?
    ): StudyUiState {
        val sessionId =
            activeSessionId
                ?: return createIdleUiState(
                    message =
                        "No active study session."
                )

        val nextItem =
            currentItem
                ?: return createIdleUiState(
                    message =
                        "No active learning item."
                )

        val reviewAction = ReviewWorkspaceAction.Rate(rating)
        val feedbackState = ReviewWorkspaceStateMachine.dispatch(
            state = workspaceState(nextItem),
            action = reviewAction
        )
        ReviewWorkspaceStateMachine.beginTransition(feedbackState)

        val nowMillis =
            System.currentTimeMillis()

        val reviewedAt =
            Moment(nowMillis)

        val responseTime =
            presentedAtMillis?.let {
                presentedAt ->
                TimeSpan(
                    (
                        nowMillis -
                            presentedAt
                    ).coerceAtLeast(0L)
                )
            }

        val reviewSessionItemResult =
            applicationContext
                .engine
                .reviewSessionItem(
                    ReviewSessionItemCommand(
                        sessionId = sessionId,
                        reviewEventId =
                            ReviewEventId(
                                UUID.randomUUID()
                                    .toString()
                            ),
                        learningItemId =
                            nextItem
                                .item
                                .learningItem
                                .id,
                        rating = rating,
                        reviewedAt = reviewedAt,
                        responseTime =
                            responseTime
                    )
                )

        latestSession =
            reviewSessionItemResult
                .session
        latestProgress = reviewSessionItemResult.progress ?: latestProgress

        val reviewResult =
            reviewSessionItemResult
                .reviewResult

        val previousState =
            reviewResult
                .reviewEvent
                .stateBefore

        val nextState =
            reviewResult
                .reviewEvent
                .stateAfter

        latestSchedulerFeedback =
            StudySchedulerFeedback(
                rating = rating.name,
                stageTransition =
                    formatStudyStageTransition(
                        beforeStage =
                            previousState.stage.name,
                        afterStage =
                            nextState.stage.name
                    ),
                scheduledInterval =
                    formatVietnameseReviewInterval(
                        reviewResult
                            .scheduledInterval
                            .millis
                    ),
                nextReviewAt =
                    formatMoment(
                        nextState
                            .dueAt
                            .epochMillis
                    ),
                difficultyBefore =
                    formatDecimal(
                        previousState
                            .difficulty
                    ),
                difficultyAfter =
                    formatDecimal(
                        nextState
                            .difficulty
                    ),
                stabilityBefore =
                    formatDays(
                        previousState
                            .stabilityDays
                    ),
                stabilityAfter =
                    formatDays(
                        nextState
                            .stabilityDays
                    ),
                reviewCount =
                    nextState
                        .reviewCount,
                lapseCount =
                        nextState
                            .lapseCount
            )

        latestSchedulingOutcome =
            productBrainPlanner.projectSchedulingOutcome(
                rating = rating,
                scheduledIntervalMillis = reviewResult.scheduledInterval.millis,
                nextReviewAtEpochMillis = nextState.dueAt.epochMillis
            )

        if (completionPlan != null) {
            val completionResult =
                productBrainPlanner.completeSession(
                    plan = completionPlan,
                    schedulingOutcome = requireNotNull(latestSchedulingOutcome),
                    sessionId = sessionId.value,
                    completedAtEpochMillis = nowMillis
                )
            val completionSnapshot = completionResult.toSnapshot()
            latestSession =
                applicationContext.engine.finishSession(
                    sessionId = sessionId,
                    finishedAt = reviewedAt,
                    completionSnapshot = completionSnapshot
                )
            activeSessionId = null
            currentItem = null
            presentedAtMillis = null
            adaptiveUiState = null
            val completedSession = requireNotNull(latestSession)
            return StudyUiState(
                sessionStarted = true,
                activeInstalledPackageId = completedSession.installedPackageId,
                activeContentId = completedSession.includedContentIds.singleOrNull(),
                studyTitle = studyTitle,
                isLessonStudy = lessonStudy,
                reviewedCount = completedSession.totalReviews,
                newItemsReviewed = completedSession.newItemsReviewed,
                reviewItemsReviewed = completedSession.reviewItemsReviewed,
                totalItems = totalItems,
                currentItemPosition = latestProgress?.completedItemCount ?: completedSession.totalReviews,
                sessionCompleted = true,
                canUndo = completedSession.undoableReview != null,
                sessionProgress = latestProgress,
                schedulerFeedback = latestSchedulerFeedback,
                sessionCompletion = completionSnapshot,
                learnEntryReviewAvailability = learnEntryAvailabilityFor(completedSession),
                message = "Learning session completed.",
                workspaceState = ReviewWorkspaceState.Completed
            )
        }

        return refreshHeaderStatistics(
            loadNextItem(
                sessionId = sessionId,
                now = reviewedAt,
                nowMillis = nowMillis,
                emptyMessage =
                    "Study session completed."
            )
        )
    }

    fun undoLatestReview(): StudyUiState {
        val sessionId = activeSessionId ?: latestSession?.id
            ?: return createIdleUiState(message = "There is no review to undo.")
        return when (val result = applicationContext.engine.undoLatestSessionReview(sessionId)) {
            UndoLatestSessionReviewResult.NothingToUndo ->
                load().copy(message = "There is no review to undo.")
            is UndoLatestSessionReviewResult.Undone -> {
                activeSessionId = sessionId
                latestSession = result.session
                latestProgress = result.progress
                latestSchedulerFeedback = null
                includedContentIds = result.session.includedContentIds
                currentItem = applicationContext.engine.getNextSessionItem(
                    sessionId,
                    result.session.currentItemPresentedAt ?: Moment(System.currentTimeMillis())
                )
                presentedAtMillis = result.session.currentItemPresentedAt?.epochMillis
                refreshHeaderStatistics(
                    toUiState(requireNotNull(currentItem), result.session.answerRevealed)
                        .copy(message = "Latest rating undone.")
                )
            }
        }
    }

    private fun loadNextItem(
        sessionId: SessionId,
        now: Moment,
        nowMillis: Long,
        emptyMessage: String
    ): StudyUiState {
        currentItem =
            applicationContext
                .engine
                .getNextSessionItem(
                    sessionId = sessionId,
                    now = now
                )

        val nextItem =
            currentItem

        if (nextItem != null) {
            latestProgress = nextItem.progress ?: latestProgress
            totalItems = latestProgress?.totalItemCount ?: totalItems
        }

        if (nextItem == null) {
            println("DEBUG_LOAD_NEXT_NULL: sessionId=$sessionId, engineContentCount=${applicationContext.engine.getAllContent().size}, queueProgress=${applicationContext.engine.getStudyQueueProgress(sessionId)}")
            val activeSession = applicationContext.engine.getSession(sessionId)
                ?: requireNotNull(latestSession)
            latestProgress = applicationContext.engine
                .getStudyQueueProgress(sessionId)
                ?.let { LearningSessionProgress.from(activeSession, it) }
                ?: latestProgress
            latestSession =
                applicationContext
                    .engine
                    .finishSession(
                        sessionId = sessionId,
                        finishedAt = now
                    )

            activeSessionId =
                null

            presentedAtMillis =
                null

            includedContentIds =
                emptySet()

            val completedSession =
                requireNotNull(
                    latestSession
                )

            return StudyUiState(
                sessionStarted = true,
                activeInstalledPackageId = completedSession.installedPackageId,
                activeContentId = completedSession.includedContentIds.singleOrNull(),
                studyTitle = studyTitle,
                isLessonStudy = lessonStudy,
                reviewedCount =
                    completedSession.totalReviews,
                newItemsReviewed =
                    completedSession
                        .newItemsReviewed,
                reviewItemsReviewed =
                    completedSession
                        .reviewItemsReviewed,
                totalItems =
                    totalItems,
                currentItemPosition =
                    latestProgress?.completedItemCount ?: completedSession.totalReviews,
                sessionCompleted = true,
                canUndo = completedSession.undoableReview != null,
                sessionProgress = latestProgress,
                schedulerFeedback =
                    latestSchedulerFeedback,
                learnEntryReviewAvailability = learnEntryAvailabilityFor(completedSession),
                message = emptyMessage,
                workspaceState = ReviewWorkspaceState.Completed
            )
        }

        presentedAtMillis =
            nowMillis

        return toUiState(
            nextSessionItem = nextItem,
            answerRevealed = false
        )
    }

    private fun toUiState(
        nextSessionItem: NextSessionItem,
        answerRevealed: Boolean
    ): StudyUiState {
        val item =
            nextSessionItem.item

        val learningContent = item.learningContent

        val reviewedCount = nextSessionItem.session.totalReviews
        val progress = nextSessionItem.progress ?: latestProgress
        latestProgress = progress

        val currentItemPosition =
            progress?.currentPosition ?: (reviewedCount + 1)

        return StudyUiState(
            hasActiveSession =
                activeSessionId != null,
            sessionStarted = true,
            topicId = activeTopicId?.value,
            activeInstalledPackageId = if (activeSessionId != null) latestSession?.installedPackageId ?: activeInstalledPackageId else null,
            activeContentId = if (activeSessionId != null) latestSession?.includedContentIds?.singleOrNull() ?: includedContentIds.singleOrNull() else null,
            studyTitle = studyTitle,
            isLessonStudy = lessonStudy,
            contentText =
                learningContent.question.textBlocks
                    .first()
                    .value,
            translationText =
                learningContent.answer.textBlocks
                    .lastOrNull()
                    ?.value
                    ?: "No translation available.",
            canRevealAnswer =
                !answerRevealed,
            canReview =
                answerRevealed,
            canUndo = nextSessionItem.session.undoableReview != null,
            reviewedCount =
                reviewedCount,
            newItemsReviewed =
                nextSessionItem
                    .session
                    .newItemsReviewed,
            reviewItemsReviewed =
                nextSessionItem
                    .session
                    .reviewItemsReviewed,
            totalItems =
                progress?.totalItemCount ?: totalItems,
            currentItemPosition =
                currentItemPosition,
            currentLearningItemId =
                item.learningItem.id.value,
            currentItemReviewContext = resolveCurrentStudyItemReviewContext(
                origin = nextSessionItem.origin,
                contentLearningState =
                    applicationContext.engine.getContentLearningState(
                        learnerId,
                        item.content.id
                    )
            ),
            contentIntroductionState =
                resolveContentIntroductionState(
                    origin = nextSessionItem.origin,
                    contentId = item.content.id,
                    introducedContentIds = nextSessionItem.session.introducedContentIds
                ),
            experienceRotationContext =
                ExperienceRotationContext.from(nextSessionItem),
            schedulerFeedback =
                latestSchedulerFeedback,
            learningContent = learningContent,
            domainContent = item.content,
            learningStage = item.learningStage,
            contentPresentationStage = applicationContext.engine.getContentPresentationStage(learnerId, item.content.id),
            learningStageDiagnostics = LearningStageDiagnosticsResolver.resolve(item),
            sessionProgress = progress,
            sessionOverview = productBrainPlanner.bootstrapSession(
                learnerId = learnerId.value,
                topicId = studyTitle,
                content = learningContent
            ),
            isSessionOverviewVisible = false,
            message =
                if (nextSessionItem.origin == vn.loi.learning.domain.study.session.model.SessionItemOrigin.NEW) {
                    "New learning item"
                } else {
                    "Review learning item"
                },
            workspaceState =
                if (answerRevealed) {
                    ReviewWorkspaceState.AnswerRevealed
                } else {
                    ReviewWorkspaceState.Question
                }
        )
    }

    fun bootstrapSessionOverview(topicId: String): StudyUiState {
        val current = adaptiveUiState ?: load()
        val selectedContentList = applicationContext.engine.getAllContent()
            .filter { content -> content.metadata.lesson == topicId || content.displayName == topicId }
        val overview = productBrainPlanner.bootstrapSession(
            learnerId = learnerId.value,
            topicId = topicId,
            itemCount = selectedContentList.size
        )
        return current.copy(
            studyTitle = topicId,
            sessionOverview = overview,
            isSessionOverviewVisible = true,
            message = "Session Overview Ready. Press Start Learning to begin."
        ).also { state ->
            adaptiveUiState = state
        }
    }

    fun startFirstScene(promptText: String, expectedAnswer: String): StudyUiState {
        val scene = productBrainPlanner.selectFirstScene(
            promptText = promptText,
            expectedAnswer = expectedAnswer,
            learnerId = learnerId.value
        )
        val current = adaptiveUiState ?: load()
        return current.copy(
            activeScene = scene,
            isSessionOverviewVisible = false,
            message = "Typing Recall Scene Active. Enter your response."
        ).also { state ->
            adaptiveUiState = state
        }
    }

    fun submitSceneAttempt(userAttempt: String, latencyMs: Long = 1000L): StudyUiState {
        val currentState = adaptiveUiState ?: load()
        val scene = currentState.activeScene as? TypingRecallScene
            ?: return currentState.copy(message = "No active scene to evaluate.")

        val result = scene.evaluate(userAttempt, latencyMs)
        val itemId = currentState.currentLearningItemId ?: "item-01"
        val evidence = scene.toEvidence(result, learnerId.value, itemId)
        productBrainPlanner.processEvidence(evidence)

        val currentOverview = currentState.sessionOverview
            ?: productBrainPlanner.bootstrapSession(
                learnerId = learnerId.value,
                topicId = currentState.studyTitle
            )

        val adaptiveOutcome = productBrainPlanner.evaluateAndAdapt(
            evidence = evidence,
            timeline = currentOverview.timeline,
            currentDifficulty = currentState.currentDifficultyLevel
        )

        val updatedOverview = currentOverview.copy(
            timeline = adaptiveOutcome.updatedTimeline
        )

        return currentState.copy(
            lastSceneResult = result,
            lastLearningEvidence = evidence,
            lastAdaptiveDecision = adaptiveOutcome.decision,
            lastDecisionTrace = adaptiveOutcome.trace,
            lastDecisionExplanation = adaptiveOutcome.explanation,
            isDecisionExplanationVisible = true,
            currentDifficultyLevel = adaptiveOutcome.newDifficultyLevel,
            sessionOverview = updatedOverview,
            message = "Adaptive Decision: ${adaptiveOutcome.decision.action} (${adaptiveOutcome.decision.rationale})"
        ).also { state ->
            adaptiveUiState = state
        }
    }

    fun completeAdaptiveSession(): StudyUiState {
        val current = adaptiveUiState ?: load()
        val overview = current.sessionOverview
        val sceneResult = current.lastSceneResult
        val evidence = current.lastLearningEvidence
        val decision = current.lastAdaptiveDecision
        val trace = current.lastDecisionTrace
        val explanation = current.lastDecisionExplanation

        val missingPrereqs = listOfNotNull(
            if (activeSessionId == null) "activeSessionId" else null,
            if (currentItem == null) "currentItem" else null,
            if (overview == null) "overview" else null,
            if (sceneResult == null) "sceneResult" else null,
            if (evidence == null) "evidence" else null,
            if (decision == null) "decision" else null,
            if (trace == null) "trace" else null,
            if (explanation == null) "explanation" else null
        )
        if (missingPrereqs.isNotEmpty()) {
            return current.copy(
                message = "Session completion needs a finished scene and adaptive decision. Missing: ${missingPrereqs.joinToString()}"
            )
        }

        val plan =
            productBrainPlanner.prepareSessionCompletion(
                SessionCompletionInput(
                    context = requireNotNull(overview).context,
                    goal = overview.goal,
                    sceneResult = requireNotNull(sceneResult),
                    evidence = requireNotNull(evidence),
                    decision = requireNotNull(decision),
                    decisionTrace = requireNotNull(trace),
                    decisionExplanation = requireNotNull(explanation),
                    timeline = overview.timeline,
                    finalDifficultyLevel = current.currentDifficultyLevel
                )
            )

        val isRevealed = currentItem?.session?.answerRevealed ?: true
        if (!isRevealed) {
            adaptiveUiState = null
            revealAnswer()
        }
        return reviewInternal(
            rating = plan.recommendedRating,
            completionPlan = plan
        )
    }

    fun toggleDecisionExplanationVisibility(): StudyUiState {
        val current = adaptiveUiState ?: load()
        return current.copy(
            isDecisionExplanationVisible = !current.isDecisionExplanationVisible
        ).also { state ->
            adaptiveUiState = state
        }
    }

    fun showDecisionExplanation(): StudyUiState {
        val current = adaptiveUiState ?: load()
        return current.copy(
            isDecisionExplanationVisible = true
        ).also { state ->
            adaptiveUiState = state
        }
    }

    fun hideDecisionExplanation(): StudyUiState {
        val current = adaptiveUiState ?: load()
        return current.copy(
            isDecisionExplanationVisible = false
        ).also { state ->
            adaptiveUiState = state
        }
    }






    private fun createNoActiveTopicUiState(): StudyUiState {
        clearActiveStudyState()
        return StudyUiState(
            topicId = null,
            activeInstalledPackageId = null,
            activeContentId = null,
            studyTitle = DEFAULT_STUDY_TITLE,
            isLessonStudy = false,
            totalItems = 0,
            sessionProgress = null,
            schedulerFeedback = null,
            message = "Chưa có chủ đề đang hoạt động\nHãy vào Thư viện và đặt một chủ đề làm Active trước khi bắt đầu học.",
            workspaceState = ReviewWorkspaceState.Idle
        )
    }

    internal fun resolveCanonicalActivePackageId(): vn.loi.learning.domain.library.model.InstalledPackageId? {
        val defaultLibId = applicationContext.defaultLibraryId
        val instPkgRepo = applicationContext.installedPackageRepository
        val navActiveId = if (defaultLibId != null) {
            applicationContext.libraryQuery?.getNavigationTree(defaultLibId)?.activePackageId
        } else null

        val validNavActiveId = navActiveId?.takeIf { id ->
            instPkgRepo?.findById(id)?.state == vn.loi.learning.domain.library.model.PackageState.ACTIVE
        }

        return validNavActiveId
            ?: instPkgRepo?.findAll()
                ?.firstOrNull { it.state == vn.loi.learning.domain.library.model.PackageState.ACTIVE }
                ?.id
    }

    private fun createIdleUiState(
        message: String =
            "Press Start Study"
    ): StudyUiState {
        val canonicalPkg = resolveCanonicalActivePackageId()
        if (canonicalPkg != null && activeInstalledPackageId != canonicalPkg) {
            activeInstalledPackageId = canonicalPkg
            activeTopicId = resolveActiveTopicIdForPackage(canonicalPkg)
            if (latestSession != null && latestSession?.installedPackageId != canonicalPkg) {
                clearActiveStudyState()
                activeInstalledPackageId = canonicalPkg
                activeTopicId = resolveActiveTopicIdForPackage(canonicalPkg)
            }
        } else if (canonicalPkg == null && activeSessionId == null && currentItem == null) {
            activeInstalledPackageId = null
            activeTopicId = null
        }
        val targetPkg = canonicalPkg ?: activeInstalledPackageId ?: latestSession?.installedPackageId
        val samePkgSession = targetPkg != null && latestSession?.installedPackageId == targetPkg
        val targetContent = if (samePkgSession) {
            includedContentIds.singleOrNull() ?: latestSession?.includedContentIds?.singleOrNull()
        } else {
            includedContentIds.singleOrNull()
        }
        val targetTitle = if (samePkgSession || canonicalPkg == null) studyTitle else DEFAULT_STUDY_TITLE

        val learnEntryAvailability =
            targetPkg?.let {
                applicationContext.engine.getLearnEntryReviewAvailability(
                    scope = vn.loi.learning.application.session.LearnEntryScope(
                        learnerId = learnerId,
                        installedPackageId = it,
                        topicId = activeTopicId
                    ),
                    now = Moment(System.currentTimeMillis())
                )
            }
        return StudyUiState(
            topicId = activeTopicId?.value,
            activeInstalledPackageId = targetPkg,
            activeContentId = targetContent,
            studyTitle = targetTitle,
            isLessonStudy = lessonStudy,
            totalItems = if (samePkgSession) totalItems else 0,
            sessionProgress = if (samePkgSession) latestProgress else null,
            schedulerFeedback = if (samePkgSession) latestSchedulerFeedback else null,
            learnEntryReviewAvailability = learnEntryAvailability,
            message = message,
            workspaceState = ReviewWorkspaceState.Idle
        )
    }

    private fun currentLearnEntryAvailability():
        vn.loi.learning.application.session.LearnEntryReviewAvailability? {
        val packageId = resolveCanonicalActivePackageId() ?: return null
        return applicationContext.engine.getLearnEntryReviewAvailability(
            scope = vn.loi.learning.application.session.LearnEntryScope(
                learnerId = learnerId,
                installedPackageId = packageId,
                topicId = resolveActiveTopicIdForPackage(packageId)
            ),
            now = Moment(System.currentTimeMillis())
        )
    }

    private fun learnEntryAvailabilityFor(
        session: StudySession
    ): vn.loi.learning.application.session.LearnEntryReviewAvailability? {
        val packageId = session.installedPackageId ?: return null
        return applicationContext.engine.getLearnEntryReviewAvailability(
            scope = vn.loi.learning.application.session.LearnEntryScope(
                learnerId = learnerId,
                installedPackageId = packageId,
                topicId = session.topicId,
                includedContentIds = session.includedContentIds
            ),
            now = Moment(System.currentTimeMillis())
        )
    }

    private fun vn.loi.learning.application.session.completion.SessionCompletionResult.toSnapshot() =
        SessionCompletionSnapshot(
            whatWasLearned = summary.whatWasLearned,
            overallOutcome = summary.overallOutcome,
            reflection = reflection.encouragement,
            reinforcement = reflection.reinforcement,
            whatHappensNext = summary.whatHappensNext,
            schedulingGuidance = schedulingOutcome.guidance,
            scheduledIntervalMillis = schedulingOutcome.scheduledIntervalMillis,
            nextReviewAtEpochMillis = schedulingOutcome.nextReviewAtEpochMillis
        )

    private fun workspaceState(
        nextItem: NextSessionItem
    ): ReviewWorkspaceState =
        if (nextItem.session.answerRevealed) {
            ReviewWorkspaceState.AnswerRevealed
        } else {
            ReviewWorkspaceState.Question
        }

    private fun formatMoment(
        epochMillis: Long
    ): String =
        DATE_TIME_FORMATTER
            .format(
                Instant
                    .ofEpochMilli(epochMillis)
                    .atZone(
                        ZoneId.systemDefault()
                    )
            )

    private fun formatDecimal(
        value: Double
    ): String =
        String.format(
            Locale.US,
            "%.2f",
            value
        )

    private fun formatDays(
        value: Double
    ): String =
        "${formatDecimal(value)} d"

    private companion object {

        const val DEFAULT_STUDY_TITLE =
            "All learning items"

        const val MILLIS_PER_MINUTE =
            60_000L

        const val MILLIS_PER_HOUR =
            3_600_000L

        const val MILLIS_PER_DAY =
            86_400_000L

        const val MINUTES_PER_HOUR =
            60L

        const val HOURS_PER_DAY =
            24L

        val DATE_TIME_FORMATTER:
                DateTimeFormatter =
            DateTimeFormatter.ofPattern(
                "yyyy-MM-dd HH:mm"
            )
    }
}

private fun StudyHeaderStatisticsState.lastKnownGood(): StudyHeaderStatistics? =
    when (this) {
        is StudyHeaderStatisticsState.Available -> value
        is StudyHeaderStatisticsState.Unavailable -> lastKnownGood
        StudyHeaderStatisticsState.Loading -> null
    }
