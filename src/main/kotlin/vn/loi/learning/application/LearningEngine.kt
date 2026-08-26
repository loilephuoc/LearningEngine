package vn.loi.learning.application

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.application.port.TransactionRunner
import vn.loi.learning.application.port.ContinuousReviewIntentRepository
import vn.loi.learning.application.port.LearningTrajectoryRepository
import vn.loi.learning.application.continuousreview.ContinuousReviewIntent
import vn.loi.learning.application.continuousreview.ContinuousReviewRecoveryResult
import vn.loi.learning.application.continuousreview.ContinuousReviewService
import vn.loi.learning.application.continuousreview.RecoverContinuousReviewUseCase
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
import vn.loi.learning.application.session.LeaveActiveStudySessionUseCase
import vn.loi.learning.application.session.StartLearnedItemsReviewRequest
import vn.loi.learning.application.session.StartLearnedItemsReviewResult
import vn.loi.learning.application.session.StartLearnedItemsReviewUseCase
import vn.loi.learning.application.session.StartLatestCompletedNewItemsReviewRequest
import vn.loi.learning.application.session.StartLatestCompletedNewItemsReviewResult
import vn.loi.learning.application.session.StartLatestCompletedNewItemsReviewUseCase
import vn.loi.learning.application.session.StartDifficultItemsReviewRequest
import vn.loi.learning.application.session.StartDifficultItemsReviewResult
import vn.loi.learning.application.session.StartDifficultItemsReviewUseCase
import vn.loi.learning.application.session.StartContinuousSkimPracticeUseCase
import vn.loi.learning.application.session.StartContinuousSkimPracticeRequest
import vn.loi.learning.application.session.StartContinuousSkimPracticeResult
import vn.loi.learning.application.session.CompletedStudySessionReplayResult
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StartStudySessionUseCase
import vn.loi.learning.application.session.UpdateActiveStudySessionLimitsUseCase
import vn.loi.learning.application.session.StudyQueueProgress
import vn.loi.learning.application.session.StudyQueueService
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.application.session.UndoLatestSessionReviewResult
import vn.loi.learning.application.session.UndoLatestSessionReviewUseCase
import vn.loi.learning.application.session.CompletePracticeItemCommand
import vn.loi.learning.application.session.CompletePracticeItemResult
import vn.loi.learning.application.session.CompletePracticeItemUseCase
import vn.loi.learning.application.session.ManualRatingOverrideCommand
import vn.loi.learning.application.session.ManualRatingOverrideResult
import vn.loi.learning.application.session.ManualRatingOverrideUseCase
import vn.loi.learning.application.session.RatingInventory
import vn.loi.learning.application.session.RatingInventoryQuery
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
import vn.loi.learning.domain.study.session.model.SessionCompletionProvenance
import vn.loi.learning.domain.study.session.model.StudySession
import vn.loi.learning.application.recall.RecallExecutionEngine
import vn.loi.learning.application.recall.RecallExecutionRequest
import vn.loi.learning.application.recall.RecallExecutionResult
import vn.loi.learning.application.recall.RecallLearningExecutionBridge
import vn.loi.learning.application.recall.RecallLearningExecutionRequest
import vn.loi.learning.application.recall.RecallLearningExecutionResult
import vn.loi.learning.application.recall.RecallPlanFactory
import vn.loi.learning.application.recall.RecallPlanFactoryResult
import vn.loi.learning.application.recall.RecallPlanRequest
import vn.loi.learning.application.recall.ProductionRecallPlanRequest
import vn.loi.learning.application.recall.ProductionRecallPlanResolver
import vn.loi.learning.application.recall.ProductionRecallPlanResult

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
    private val installedPackageRepository: vn.loi.learning.domain.library.repository.InstalledPackageRepository? = null,
    private val continuousReviewIntentRepository: ContinuousReviewIntentRepository? = null,
    private val learningTrajectoryRepository: LearningTrajectoryRepository? = null
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

    private val updateActiveSessionLimitsUseCase = UpdateActiveStudySessionLimitsUseCase(
        sessions = sessionRepository,
        planning = studyQueuePlanningService,
        queues = studyQueueService,
        transactions = transactionRunner
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

    private val startLatestCompletedNewItemsReviewUseCase =
        StartLatestCompletedNewItemsReviewUseCase(
            sessions = sessionRepository,
            queues = studyQueueService,
            availability = learnEntryReviewAvailabilityQuery
        )

    private val startDifficultItemsReviewUseCase =
        StartDifficultItemsReviewUseCase(
            sessions = sessionRepository,
            queues = studyQueueService,
            availability = learnEntryReviewAvailabilityQuery
        )

    private val startContinuousSkimPracticeUseCase =
        StartContinuousSkimPracticeUseCase(sessionRepository, studyQueueService)

    private val leaveActiveStudySessionUseCase =
        LeaveActiveStudySessionUseCase(
            sessions = sessionRepository,
            queues = studyQueueService,
            transactions = transactionRunner
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
            contentLearningStateQuery = contentLearningStateQueryService,
            memoryStates = memoryStateRepository,
            trajectories = learningTrajectoryRepository
        )

    private val completePracticeItemUseCase = CompletePracticeItemUseCase(
        sessionRepository,
        studyQueueService,
        transactionRunner
    )

    private val manualRatingOverrideUseCase = ManualRatingOverrideUseCase(
        sessionRepository,
        learningItemRepository,
        contentLearningStateQueryService,
        reviewUseCase,
        transactionRunner,
        studyQueueService
    )

    private val recallPlanFactory = RecallPlanFactory()
    private val productionRecallPlanResolver = ProductionRecallPlanResolver(planFactory = recallPlanFactory)
    private val recallExecutionEngine = RecallExecutionEngine()
    private val recallLearningExecutionBridge = RecallLearningExecutionBridge(
        sessionRepository,
        learningItemRepository,
        reviewEventRepository,
        studyQueueService,
        reviewSessionItemUseCase,
        completePracticeItemUseCase,
        manualRatingOverrideUseCase
    )

    private val ratingInventoryQuery = RatingInventoryQuery(
        learningItemRepository,
        memoryStateQuery,
        reviewEventRepository,
        packageContentQuerySupplier
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
        transactions = transactionRunner,
        trajectories = learningTrajectoryRepository
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

    private val continuousReviewService =
        continuousReviewIntentRepository?.let { ContinuousReviewService(it, transactionRunner) }

    private val recoverContinuousReviewUseCase =
        continuousReviewService?.let {
            RecoverContinuousReviewUseCase(
                intentService = it,
                sessions = sessionRepository,
                recoverActive = recoverActiveStudySessionUseCase,
                continueGeneralStudy = continueGeneralStudyUseCase
            )
        }

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

    fun getContinuousReviewIntent(learnerId: LearnerId): ContinuousReviewIntent? =
        continuousReviewService?.query(learnerId)

    fun enableContinuousReview(
        learnerId: LearnerId,
        installedPackageId: vn.loi.learning.domain.library.model.InstalledPackageId,
        topicId: TopicId?,
        updatedAt: Moment
    ): ContinuousReviewIntent =
        requireNotNull(continuousReviewService) { "Continuous Review persistence is unavailable." }
            .enable(learnerId, installedPackageId, topicId, updatedAt)

    fun disableContinuousReview(learnerId: LearnerId, updatedAt: Moment): ContinuousReviewIntent? =
        continuousReviewService?.disable(learnerId, updatedAt)

    fun recoverContinuousReview(
        learnerId: LearnerId,
        installedPackageId: vn.loi.learning.domain.library.model.InstalledPackageId,
        topicId: TopicId?,
        recoveredAt: Moment
    ): ContinuousReviewRecoveryResult {
        sessionRepository.findActiveByLearner(learnerId)?.let { session ->
            val queue = studyQueueService.get(session.id)
            if (queue != null && !queue.isCompleted) {
                reviewSessionItemUseCase.resumePending(session.id)
            }
        }
        return requireNotNull(recoverContinuousReviewUseCase) {
            "Continuous Review persistence is unavailable."
        }.execute(
            vn.loi.learning.application.continuousreview.ContinuousReviewRecoveryRequest(
                learnerId = learnerId,
                installedPackageId = installedPackageId,
                topicId = topicId,
                recoveredAt = recoveredAt
            )
        )
    }

    fun replayCompletedStudySession(
        request: ReplayCompletedStudySessionRequest
    ): CompletedStudySessionReplayResult =
        replayCompletedStudySessionUseCase.execute(request)

    fun getLearnEntryReviewAvailability(
        scope: LearnEntryScope,
        now: Moment
    ): LearnEntryReviewAvailability =
        learnEntryReviewAvailabilityQuery.execute(scope, now)

    fun startLatestCompletedNewItemsReview(
        request: StartLatestCompletedNewItemsReviewRequest
    ): StartLatestCompletedNewItemsReviewResult =
        startLatestCompletedNewItemsReviewUseCase.execute(request)

    fun startDifficultItemsReview(
        request: StartDifficultItemsReviewRequest
    ): StartDifficultItemsReviewResult =
        startDifficultItemsReviewUseCase.execute(request)

    fun startContinuousSkimPractice(
        request: StartContinuousSkimPracticeRequest
    ): StartContinuousSkimPracticeResult = startContinuousSkimPracticeUseCase.execute(request)

    fun startLearnedItemsReview(
        request: StartLearnedItemsReviewRequest
    ): StartLearnedItemsReviewResult =
        startLearnedItemsReviewUseCase.execute(request)

    fun leaveActiveStudySession(
        learnerId: LearnerId,
        leftAt: Moment
    ): StudySession? =
        leaveActiveStudySessionUseCase.execute(learnerId, leftAt)

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

    fun advanceQuickReviewWithoutEvaluation(sessionId: SessionId): StudyQueueSnapshot =
        studyQueueService.advanceQuickReview(sessionId)

    fun completePracticeItem(command: CompletePracticeItemCommand): CompletePracticeItemResult =
        completePracticeItemUseCase.execute(command)

    fun createRecallPlan(request: RecallPlanRequest): RecallPlanFactoryResult =
        recallPlanFactory.create(request)

    fun createProductionRecallPlan(request: ProductionRecallPlanRequest): ProductionRecallPlanResult =
        productionRecallPlanResolver.resolve(
            if (request.difficultyProfile != null || learningTrajectoryRepository == null) request
            else {
                val trajectory = learningTrajectoryRepository.find(request.learnerId, request.content.id)
                if (trajectory == null) request else {
                    val profile = vn.loi.learning.domain.study.evidence.LearningDifficultyProfileCalculator(
                        vn.loi.learning.domain.study.evidence.EvidenceClock { request.generatedAt }
                    ).calculateDifficultyProfile(request.learnerId, trajectory)
                    request.copy(
                        difficultyProfile = profile,
                        learningRecommendation = vn.loi.learning.domain.study.evidence.AdaptiveLearningStrategy()
                            .calculateRecommendation(profile)
                    )
                }
            }
        )

    fun executeRecall(request: RecallExecutionRequest): RecallExecutionResult =
        recallExecutionEngine.execute(request)

    fun executeRecallLearning(request: RecallLearningExecutionRequest): RecallLearningExecutionResult =
        recallLearningExecutionBridge.execute(request)

    fun overridePracticeItemRating(
        command: ManualRatingOverrideCommand
    ): ManualRatingOverrideResult = manualRatingOverrideUseCase.execute(command)

    fun getRatingInventory(scope: LearnEntryScope): RatingInventory =
        ratingInventoryQuery.execute(scope)

    fun getPracticeProgress(sessionId: SessionId): vn.loi.learning.application.session.PracticeProgress? =
        studyQueueService.get(sessionId)?.practiceProgress

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
        completionSnapshot: SessionCompletionSnapshot? = null,
        completionProvenance: SessionCompletionProvenance = SessionCompletionProvenance.ORDINARY_SUCCESS
    ): StudySession =
        finishSessionUseCase.execute(
            sessionId =
                sessionId,
            finishedAt =
                finishedAt,
            completionSnapshot =
                completionSnapshot,
            completionProvenance = completionProvenance
        )

    fun undoLatestSessionReview(sessionId: SessionId): UndoLatestSessionReviewResult =
        undoLatestSessionReviewUseCase.execute(sessionId)

    fun getSession(
        sessionId: SessionId
    ): StudySession? =
        sessionRepository.findById(
            sessionId
        )

    fun updateActiveSessionLimits(sessionId: SessionId, newLimit: Int, reviewLimit: Int): StudySession =
        updateActiveSessionLimitsUseCase.execute(sessionId, newLimit, reviewLimit)

    fun getLearningTrajectory(learnerId: LearnerId, contentId: ContentId) =
        learningTrajectoryRepository?.find(learnerId, contentId)

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
