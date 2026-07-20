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
import vn.loi.learning.application.session.FinishStudySessionUseCase
import vn.loi.learning.application.session.GetNextSessionItemUseCase
import vn.loi.learning.application.session.NextSessionItem
import vn.loi.learning.application.session.ReviewSessionItemCommand
import vn.loi.learning.application.session.ReviewSessionItemResult
import vn.loi.learning.application.session.ReviewSessionItemUseCase
import vn.loi.learning.application.session.StartStudySessionCommand
import vn.loi.learning.application.session.StartStudySessionUseCase
import vn.loi.learning.application.study.GetNextLearningItemQuery
import vn.loi.learning.application.study.GetNextLearningItemUseCase
import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.content.model.Content
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
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val memoryStateRepository: MemoryStateRepository,
    private val reviewEventRepository: ReviewEventRepository,
    private val sessionRepository: StudySessionRepository,
    private val transactionRunner: TransactionRunner,
    scheduler: Scheduler
) {

    private val reviewUseCase =
        ReviewLearningItemUseCase(
            memoryStateRepository = memoryStateRepository,
            reviewEventRepository = reviewEventRepository,
            scheduler = scheduler
        )

    private val getNextLearningItemUseCase =
        GetNextLearningItemUseCase(
            contentRepository = contentRepository,
            learningItemRepository = learningItemRepository,
            memoryStateRepository = memoryStateRepository
        )

    private val startSessionUseCase =
        StartStudySessionUseCase(
            sessionRepository = sessionRepository
        )

    private val getNextSessionItemUseCase =
        GetNextSessionItemUseCase(
            sessionRepository = sessionRepository,
            getNextLearningItemUseCase =
                getNextLearningItemUseCase
        )

    private val reviewSessionItemUseCase =
        ReviewSessionItemUseCase(
            sessionRepository = sessionRepository,
            learningItemRepository = learningItemRepository,
            reviewLearningItemUseCase = reviewUseCase,
            transactionRunner = transactionRunner
        )

    private val finishSessionUseCase =
        FinishStudySessionUseCase(
            sessionRepository = sessionRepository
        )

    fun registerContent(content: Content) {
        contentRepository.save(content)
    }

    fun registerLearningItem(learningItem: LearningItem) {
        require(
            contentRepository.findById(
                learningItem.contentId
            ) != null
        ) {
            "Cannot register LearningItem ${learningItem.id}: " +
                    "Content ${learningItem.contentId} does not exist."
        }

        learningItemRepository.save(learningItem)
    }

    fun getNextLearningItem(
        learnerId: LearnerId,
        now: Moment
    ): NextLearningItem? =
        getNextLearningItemUseCase.execute(
            GetNextLearningItemQuery(
                learnerId = learnerId,
                now = now
            )
        )

    fun review(
        command: ReviewCommand
    ): ReviewResult {
        requireLearningItemExists(command.learningItemId)

        return transactionRunner.runInTransaction {
            reviewUseCase.execute(command)
        }
    }

    fun startSession(
        command: StartStudySessionCommand
    ): StudySession =
        startSessionUseCase.execute(command)

    fun getNextSessionItem(
        sessionId: SessionId,
        now: Moment
    ): NextSessionItem? =
        getNextSessionItemUseCase.execute(
            sessionId = sessionId,
            now = now
        )

    fun reviewSessionItem(
        command: ReviewSessionItemCommand
    ): ReviewSessionItemResult {
        requireLearningItemExists(command.learningItemId)

        return reviewSessionItemUseCase.execute(command)
    }

    fun finishSession(
        sessionId: SessionId,
        finishedAt: Moment
    ): StudySession =
        finishSessionUseCase.execute(
            sessionId = sessionId,
            finishedAt = finishedAt
        )

    fun getSession(
        sessionId: SessionId
    ): StudySession? =
        sessionRepository.findById(sessionId)

    fun getMemoryState(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): MemoryState? =
        memoryStateRepository.find(
            learnerId = learnerId,
            learningItemId = learningItemId
        )

    fun getReviewHistory(
        learnerId: LearnerId,
        learningItemId: LearningItemId
    ): List<ReviewEvent> =
        reviewEventRepository.findAll(
            learnerId = learnerId,
            learningItemId = learningItemId
        )

    private fun requireLearningItemExists(
        learningItemId: LearningItemId
    ) {
        require(
            learningItemRepository.findById(
                learningItemId
            ) != null
        ) {
            "LearningItem $learningItemId does not exist."
        }
    }
}