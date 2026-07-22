package vn.loi.learning.desktop.ui.study

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import vn.loi.learning.application.session.ActiveStudySessionRecovery
import vn.loi.learning.application.session.LearningSessionProgress
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.UndoLatestSessionReviewResult
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEventId
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.infrastructure.LearningApplicationContext

class StudyFacade(
    private val applicationContext:
    LearningApplicationContext
) {

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

    private var studyTitle:
            String = DEFAULT_STUDY_TITLE

    private var lessonStudy:
            Boolean = false

    private var totalItems:
            Int = 0

    private var latestSchedulerFeedback:
            StudySchedulerFeedback? = null

    private var latestProgress: LearningSessionProgress? = null

    fun load(): StudyUiState {
        currentItem?.let { nextItem ->
            return toUiState(
                nextSessionItem = nextItem,
                answerRevealed = nextItem.session.answerRevealed
            )
        }

        if (activeSessionId == null) {
            restoreActiveSession()?.let { restoredState ->
                return restoredState
            }
        }

        return createIdleUiState()
    }

    private fun restoreActiveSession(): StudyUiState? {
        val nowMillis =
            System.currentTimeMillis()

        return when (
            val recovery =
                applicationContext
                    .engine
                    .recoverActiveSession(
                        learnerId = learnerId,
                        recoveredAt =
                            Moment(nowMillis)
                    )
        ) {
            ActiveStudySessionRecovery
                .NoActiveSession -> restoreLatestUndoableCompletion()

            is ActiveStudySessionRecovery
                .ClosedIncompleteSession -> {
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
                restoreResumableSession(
                    recovery = recovery,
                    nowMillis = nowMillis
                )
            }
        }
    }

    private fun restoreLatestUndoableCompletion(): StudyUiState? {
        val session = applicationContext.engine.getLatestUndoableSession(learnerId) ?: return null
        if (session.status != vn.loi.learning.domain.study.session.model.SessionStatus.FINISHED) return null
        val queue = applicationContext.engine.getStudyQueueProgress(session.id) ?: return null
        return restoreCompletedSession(
            ActiveStudySessionRecovery.ClosedIncompleteSession(
                session = session,
                reason = ActiveStudySessionRecovery.ClosedIncompleteSession.Reason.COMPLETED_QUEUE,
                queueProgress = queue
            )
        )
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
        includedContentIds = emptySet()
        lessonStudy = session.includedContentIds.isNotEmpty()
        studyTitle = resolveRestoredStudyTitle(session.includedContentIds)
        totalItems = requireNotNull(progress.totalItemCount)
        latestProgress = progress
        latestSchedulerFeedback = null
        return StudyUiState(
            sessionStarted = true,
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

        activeSessionId =
            session.id

        latestSession =
            session

        includedContentIds =
            session.includedContentIds

        lessonStudy =
            includedContentIds.isNotEmpty()

        studyTitle =
            resolveRestoredStudyTitle(
                includedContentIds
            )

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
        activeSessionId = null
        currentItem = null
        presentedAtMillis = null
        latestSession = null
        includedContentIds = emptySet()
        studyTitle = DEFAULT_STUDY_TITLE
        lessonStudy = false
        totalItems = 0
        latestSchedulerFeedback = null
        latestProgress = null
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
            ?: firstContent.displayName
    }

    fun startStudy(): StudyUiState {
        includedContentIds =
            emptySet()

        studyTitle =
            DEFAULT_STUDY_TITLE

        lessonStudy =
            false

        totalItems =
            0

        latestSchedulerFeedback =
            null
        latestProgress = null

        return startSession()
    }

    fun startLessonStudy(
        contentId: String
    ): StudyUiState {
        val resolvedContentId =
            ContentId(contentId)

        val selectedContent =
            requireNotNull(
                applicationContext
                    .engine
                    .getContent(
                        resolvedContentId
                    )
            ) {
                "Content $contentId does not exist."
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

        includedContentIds =
            lessonContent
                .map { content ->
                    content.id
                }
                .toSet()

        studyTitle =
            selectedMetadata.lesson
                ?: selectedContent.displayName

        lessonStudy =
            true

        latestSchedulerFeedback =
            null

        latestProgress = null

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

        return startSession()
    }

    private fun startSession(): StudyUiState {
        val nowMillis =
            System.currentTimeMillis()

        val now =
            Moment(nowMillis)

        val sessionId =
            SessionId(
                UUID.randomUUID()
                    .toString()
            )

        latestSession =
            applicationContext
                .engine
                .startSession(
                    StartStudySessionCommand(
                        sessionId = sessionId,
                        learnerId = learnerId,
                        startedAt = now,
                        includedContentIds =
                            includedContentIds
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

    fun review(
        rating: ReviewRating
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
                    formatDuration(
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

        return loadNextItem(
            sessionId = sessionId,
            now = reviewedAt,
            nowMillis = nowMillis,
            emptyMessage =
                "Study session completed."
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
                toUiState(requireNotNull(currentItem), result.session.answerRevealed)
                    .copy(message = "Latest rating undone.")
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
            schedulerFeedback =
                latestSchedulerFeedback,
            learningContent = learningContent,
            sessionProgress = progress,
            message =
                if (item.isNew) {
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

    private fun createIdleUiState(
        message: String =
            "Press Start Study"
    ): StudyUiState =
        StudyUiState(
            studyTitle = studyTitle,
            isLessonStudy = lessonStudy,
            totalItems = totalItems,
            sessionProgress = latestProgress,
            schedulerFeedback =
                latestSchedulerFeedback,
            message = message,
            workspaceState = ReviewWorkspaceState.Idle
        )

    private fun workspaceState(
        nextItem: NextSessionItem
    ): ReviewWorkspaceState =
        if (nextItem.session.answerRevealed) {
            ReviewWorkspaceState.AnswerRevealed
        } else {
            ReviewWorkspaceState.Question
        }

    private fun formatDuration(
        millis: Long
    ): String {
        val totalMinutes =
            millis / MILLIS_PER_MINUTE

        if (totalMinutes < MINUTES_PER_HOUR) {
            return "${totalMinutes.coerceAtLeast(1L)} min"
        }

        val totalHours =
            millis / MILLIS_PER_HOUR

        if (totalHours < HOURS_PER_DAY) {
            return "$totalHours h"
        }

        val days =
            millis.toDouble() /
                    MILLIS_PER_DAY.toDouble()

        return formatDays(days)
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
