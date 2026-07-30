package vn.loi.learning.application

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.review.ReviewCommand
import vn.loi.learning.application.review.ReviewLearningItemUseCase
import vn.loi.learning.application.review.ReviewResult
import vn.loi.learning.application.session.ActiveStudySessionRecovery
import vn.loi.learning.application.session.ContinueGeneralStudyRequest
import vn.loi.learning.application.session.ContinueGeneralStudyUseCase
import vn.loi.learning.application.session.FinishStudySessionUseCase
import vn.loi.learning.application.session.GeneralStudyContinuationResult
import vn.loi.learning.application.session.GetNextSessionItemUseCase
import vn.loi.learning.application.session.GetStudyQueueProgressUseCase
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.ReviewSessionItemResult
import vn.loi.learning.application.session.RecoverActiveStudySessionUseCase
import vn.loi.learning.application.session.ReviewSessionItemUseCase
import vn.loi.learning.application.session.ReplayCompletedStudySessionRequest
import vn.loi.learning.application.session.ReplayCompletedStudySessionUseCase
import vn.loi.learning.application.session.LearnEntryReviewAvailability
import vn.loi.learning.application.session.LearnEntryReviewAvailabilityQuery
import vn.loi.learning.application.session.LearnEntryScope
import vn.loi.learning.application.session.StartLearnedItemsReviewRequest
import vn.loi.learning.application.session.StartLearnedItemsReviewResult
import vn.loi.learning.application.session.StartLearnedItemsReviewUseCase
import vn.loi.learning.application.session.CompletedStudySessionReplayResult
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StartStudySessionUseCase
import vn.loi.learning.application.session.StudyQueueProgress
import vn.loi.learning.application.session.StudyQueueService
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.application.session.UndoLatestSessionReviewResult
import vn.loi.learning.application.session.UndoLatestSessionReviewUseCase
import vn.loi.learning.application.study.GetNextLearningItemQuery
import vn.loi.learning.application.study.GetNextLearningItemUseCase
import vn.loi.learning.application.study.ContentLearningState
import vn.loi.learning.application.study.ContentLearningStateQueryService
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.application.study.StudyQueuePlanner
import vn.loi.learning.application.study.StudyQueuePlanningService
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.scheduling.Scheduler
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionCompletionSnapshot
import vn.loi.learning.domain.study.session.model.StudySession

class LearningEngine(
    private val contentRepository:
    ContentRepository,
    private val learningItemRepository:
    LearningItemRepository,
    private val memoryStateRepository:
    MemoryStateRepository,
    private val reviewEventRepository:
    ReviewEventRepository,
    private val memoryStateQuery:
    vn.loi.learning.application.port.MemoryStateQuery? = null,
    private val sessionRepository:
    StudySessionRepository,
    private val studyQueueService:
    StudyQueueService,
    private val transactionRunner:
    TransactionRunner,
    scheduler: Scheduler,
    private val packageContentQuerySupplier: (() -> vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService?)? = null,
    private val topicQueryServiceSupplier: (() -> vn.loi.learning.application.topic.TopicQueryService?)? = null,
    private val installedPackageRepository: vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null
) {

    private val reviewUseCase =
        ReviewLearningItemUseCase(
            memoryStateRepository =
                memoryStateRepository,
            reviewEventRepository =
                reviewEventRepository,
            scheduler =
                scheduler
        )

    private val getNextLearningItemUseCase =
        GetNextLearningItemUseCase(
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository,
            memoryStateRepository =
                memoryStateRepository
        )

    private val contentStageQueryService =
        vn.loi.learning.application.study.ContentStageQueryService(
            learningItemRepository = learningItemRepository,
            memoryStateRepository = memoryStateRepository
        )

    private val contentLearningStateQueryService =
        ContentLearningStateQueryService(
            learningItems = learningItemRepository,
            reviewEvents = reviewEventRepository
        )

    private val studyQueuePlanner =
        StudyQueuePlanner(
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository,
            memoryStateRepository =
                memoryStateRepository,
            contentLearningStateQuery = contentLearningStateQueryService
        )

    private val studyQueuePlanningService =
        StudyQueuePlanningService(
            planner =
                studyQueuePlanner,
            packageContentQuerySupplier = packageContentQuerySupplier,
            topicQueryServiceSupplier = topicQueryServiceSupplier
        )

    private val startSessionUseCase =
        StartStudySessionUseCase(
            sessionRepository =
                sessionRepository,
            studyQueuePlanningService =
                studyQueuePlanningService,
            studyQueueService =
                studyQueueService
        )

    private val continueGeneralStudyUseCase =
        ContinueGeneralStudyUseCase(
            sessions = sessionRepository,
            queues = studyQueueService,
            startSession = startSessionUseCase
        )

    private val replayCompletedStudySessionUseCase =
        ReplayCompletedStudySessionUseCase(
            sessions = sessionRepository,
            queues = studyQueueService
        )

    private val learnEntryReviewAvailabilityQuery =
        LearnEntryReviewAvailabilityQuery(
            sessions = sessionRepository,
            queues = studyQueueService,
            learningItems = learningItemRepository,
            memoryStates = memoryStateQuery,
            reviewEvents = reviewEventRepository,
            packageContentQuerySupplier = packageContentQuerySupplier
        )

    private val startLearnedItemsReviewUseCase =
        StartLearnedItemsReviewUseCase(
            sessions = sessionRepository,
            queues = studyQueueService,
            availability = learnEntryReviewAvailabilityQuery
        )

    private val getNextSessionItemUseCase =
        GetNextSessionItemUseCase(
            sessionRepository =
                sessionRepository,
            getNextLearningItemUseCase =
                getNextLearningItemUseCase,
            learningItemRepository =
                learningItemRepository,
            studyQueueService =
                studyQueueService,
            contentLearningStateQuery = contentLearningStateQueryService
        )

    private val reviewSessionItemUseCase =
        ReviewSessionItemUseCase(
            sessionRepository =
                sessionRepository,
            learningItemRepository =
                learningItemRepository,
            reviewLearningItemUseCase =
                reviewUseCase,
            transactionRunner =
                transactionRunner,
            studyQueueService =
                studyQueueService,
            contentLearningStateQuery = contentLearningStateQueryService
        )

    private val finishSessionUseCase =
        FinishStudySessionUseCase(
            sessionRepository =
                sessionRepository,
            studyQueueService =
                studyQueueService
        )

    private val undoLatestSessionReviewUseCase = UndoLatestSessionReviewUseCase(
        sessions = sessionRepository,
        queues = studyQueueService,
        memoryStates = memoryStateRepository,
        reviewEvents = reviewEventRepository,
        transactions = transactionRunner
    )

    private val getStudyQueueProgressUseCase =
        GetStudyQueueProgressUseCase(
            studyQueueService =
                studyQueueService
        )

    private val recoverActiveStudySessionUseCase =
        RecoverActiveStudySessionUseCase(
            sessionRepository =
                sessionRepository,
            studyQueueService =
                studyQueueService,
            finishStudySessionUseCase =
                finishSessionUseCase,
            installedPackageRepository =
                installedPackageRepository
        )

    fun registerContent(
        content: Content
    ) {
        contentRepository.save(
            content
        )
    }

    fun registerLearningItem(
        learningItem: LearningItem
    ) {
        require(
            contentRepository.findById(
                learningItem.contentId
            ) != null
        ) {
            "Cannot register LearningItem " +
                    "${learningItem.id}: " +
                    "Content " +
                    "${learningItem.contentId} " +
                    "does not exist."
        }

        learningItemRepository.save(
            learningItem
        )
    }

    fun getContent(
        contentId: ContentId
    ): Content? =
        contentRepository.findById(
            contentId
        )

    fun getAllContent(): List<Content> =
        contentRepository.findAll()

    fun getLearningItemsByContentId(
        contentId: ContentId
    ): List<LearningItem> =
        learningItemRepository
            .findByContentId(
                contentId
            )

    fun getLearningItemsByContentIds(
        contentIds: Set<ContentId>
    ): List<LearningItem> =
        learningItemRepository
            .findByContentIds(
                contentIds
            )

    fun getNextLearningItem(
        learnerId: LearnerId,
        now: Moment
    ): NextLearningItem? =
        getNextLearningItemUseCase.execute(
            GetNextLearningItemQuery(
                learnerId =
                    learnerId,
                now =
                    now
            )
        )

    fun review(
        command: ReviewCommand
    ): ReviewResult {
        requireLearningItemExists(
            command.learningItemId
        )

        return transactionRunner
            .runInTransaction {
                reviewUseCase.execute(
                    command
                )
            }
    }

    fun startSession(
        command: StartStudySessionCommand
    ): StudySession =
        startSessionUseCase.execute(
            command
        )

    fun continueGeneralStudy(
        request: ContinueGeneralStudyRequest
    ): GeneralStudyContinuationResult =
        continueGeneralStudyUseCase.execute(request)

    fun replayCompletedStudySession(
        request: ReplayCompletedStudySessionRequest
    ): CompletedStudySessionReplayResult =
        replayCompletedStudySessionUseCase.execute(request)

    fun getLearnEntryReviewAvailability(
        scope: LearnEntryScope,
        reviewLimit: Int,
        now: Moment
    ): LearnEntryReviewAvailability =
        learnEntryReviewAvailabilityQuery.execute(scope, reviewLimit, now)

    fun startLearnedItemsReview(
        request: StartLearnedItemsReviewRequest
    ): StartLearnedItemsReviewResult =
        startLearnedItemsReviewUseCase.execute(request)

    fun getNextSessionItem(
        sessionId: SessionId,
        now: Moment,
        includedContentIds: Set<ContentId> =
            emptySet()
    ): NextSessionItem? =
        getNextSessionItemUseCase.execute(
            sessionId =
                sessionId,
            now =
                now
        )

    fun reviewSessionItem(
        command: ReviewSessionItemCommand
    ): ReviewSessionItemResult {
        requireLearningItemExists(
            command.learningItemId
        )

        return reviewSessionItemUseCase
            .execute(
                command
            )
    }

    fun revealSessionItem(
        sessionId: SessionId,
        learningItemId: LearningItemId
    ): StudySession {
        val session = requireNotNull(sessionRepository.findById(sessionId)) {
            "Session $sessionId does not exist."
        }
        val revealed = session.revealCurrentItem(learningItemId)
        sessionRepository.save(revealed)
        return revealed
    }

    fun completeContentIntroduction(
        sessionId: SessionId,
        contentId: ContentId,
        learningItemId: LearningItemId? = null
    ): StudySession {
        val session = requireNotNull(sessionRepository.findById(sessionId)) {
            "Session $sessionId does not exist."
        }
        val updated =
            if (learningItemId == null) session.completeIntroduction(contentId)
            else session.completeIntroductionAndReveal(contentId, learningItemId)
        sessionRepository.save(updated)
        return updated
    }

    fun finishSession(
        sessionId: SessionId,
        finishedAt: Moment,
        completionSnapshot: SessionCompletionSnapshot? = null
    ): StudySession =
        finishSessionUseCase.execute(
            sessionId =
                sessionId,
            finishedAt =
                finishedAt,
            completionSnapshot =
                completionSnapshot
        )

    fun undoLatestSessionReview(sessionId: SessionId): UndoLatestSessionReviewResult =
        undoLatestSessionReviewUseCase.execute(sessionId)

    fun getSession(
        sessionId: SessionId
    ): StudySession? =
        sessionRepository.findById(
            sessionId
        )

    fun getActiveSession(
        learnerId: LearnerId
    ): StudySession? =
        sessionRepository.findActiveByLearner(
            learnerId
        )

    fun getLatestUndoableSession(learnerId: LearnerId): StudySession? {
        if (sessionRepository.findActiveByLearner(learnerId) != null) {
            return null
        }
        return sessionRepository.findLatestUndoableByLearner(learnerId)
    }

    fun recoverActiveSession(
        learnerId: LearnerId,
        recoveredAt: Moment
    ): ActiveStudySessionRecovery {
        sessionRepository.findActiveByLearner(learnerId)?.let { session ->
            val queue = studyQueueService.get(session.id)
            if (queue != null && !queue.isCompleted) {
                reviewSessionItemUseCase.resumePending(session.id)
            }
        }
        return recoverActiveStudySessionUseCase.execute(
            learnerId = learnerId,
            recoveredAt = recoveredAt
        )
    }

    fun recoverTopicSession(
        learnerId: LearnerId,
        topicId: TopicId,
        recoveredAt: Moment
    ): ActiveStudySessionRecovery {
        sessionRepository
            .findActiveByLearnerAndTopic(
                learnerId = learnerId,
                topicId = topicId
            )
            ?.let { session ->
                val queue =
                    studyQueueService.get(
                        session.id
                    )
                if (queue != null && !queue.isCompleted) {
                    reviewSessionItemUseCase.resumePending(
                        session.id
                    )
                }
            }

        return recoverActiveStudySessionUseCase.execute(
            learnerId = learnerId,
            recoveredAt = recoveredAt,
            topicId = topicId
        )
    }

    fun getStudyQueue(
        sessionId: SessionId
    ): StudyQueueSnapshot? =
        studyQueueService.get(
            sessionId
        )

    fun getStudyQueueProgress(
        sessionId: SessionId
    ): StudyQueueProgress? =
        getStudyQueueProgressUseCase.execute(
            sessionId
        )

    fun requireStudyQueueProgress(
        sessionId: SessionId
    ): StudyQueueProgress =
        getStudyQueueProgressUseCase.require(
            sessionId
        )

    fun getMemoryState(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState? =
        memoryStateRepository.find(
            learnerId =
                learnerId,
            learningItemId =
                learningItemId
        )

    fun getContentPresentationStage(
        learnerId: LearnerId,
        contentId: ContentId
    ): LearningStage =
        contentStageQueryService.resolveContentStage(
            learnerId = learnerId,
            contentId = contentId
        )

    fun getContentLearningState(
        learnerId: LearnerId,
        contentId: ContentId
    ): ContentLearningState =
        contentLearningStateQueryService.resolve(learnerId, contentId)

    fun getReviewHistory(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent> =
        reviewEventRepository.findAll(
            learnerId =
                learnerId,
            learningItemId =
                learningItemId
        )

    private fun requireLearningItemExists(
        learningItemId: LearningItemId
    ) {
        require(
            learningItemRepository.findById(
                learningItemId
            ) != null
        ) {
            "LearningItem $learningItemId " +
                    "does not exist."
        }
    }
}
