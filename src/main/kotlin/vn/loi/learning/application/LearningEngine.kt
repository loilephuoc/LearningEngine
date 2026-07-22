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
import vn.loi.learning.application.session.FinishStudySessionUseCase
import vn.loi.learning.application.session.GetNextSessionItemUseCase
import vn.loi.learning.application.session.GetStudyQueueProgressUseCase
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.ReviewSessionItemResult
import vn.loi.learning.application.session.RecoverActiveStudySessionUseCase
import vn.loi.learning.application.session.ReviewSessionItemUseCase
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StartStudySessionUseCase
import vn.loi.learning.application.session.StudyQueueProgress
import vn.loi.learning.application.session.StudyQueueService
import vn.loi.learning.application.session.StudyQueueSnapshot
import vn.loi.learning.application.study.GetNextLearningItemQuery
import vn.loi.learning.application.study.GetNextLearningItemUseCase
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.application.study.StudyQueuePlanner
import vn.loi.learning.application.study.StudyQueuePlanningService
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.scheduling.Scheduler
import vn.loi.learning.domain.study.session.model.SessionId
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
    private val sessionRepository:
    StudySessionRepository,
    private val studyQueueService:
    StudyQueueService,
    private val transactionRunner:
    TransactionRunner,
    scheduler: Scheduler
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

    private val studyQueuePlanner =
        StudyQueuePlanner(
            contentRepository =
                contentRepository,
            learningItemRepository =
                learningItemRepository,
            memoryStateRepository =
                memoryStateRepository
        )

    private val studyQueuePlanningService =
        StudyQueuePlanningService(
            planner =
                studyQueuePlanner
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

    private val getNextSessionItemUseCase =
        GetNextSessionItemUseCase(
            sessionRepository =
                sessionRepository,
            getNextLearningItemUseCase =
                getNextLearningItemUseCase,
            learningItemRepository =
                learningItemRepository,
            studyQueueService =
                studyQueueService
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
                studyQueueService
        )

    private val finishSessionUseCase =
        FinishStudySessionUseCase(
            sessionRepository =
                sessionRepository,
            studyQueueService =
                studyQueueService
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
                finishSessionUseCase
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

    fun finishSession(
        sessionId: SessionId,
        finishedAt: Moment
    ): StudySession =
        finishSessionUseCase.execute(
            sessionId =
                sessionId,
            finishedAt =
                finishedAt
        )

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
