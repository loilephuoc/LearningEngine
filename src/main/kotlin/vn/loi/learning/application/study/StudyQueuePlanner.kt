package vn.loi.learning.application.study

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.selection.model.SelectionCandidate
import vn.loi.learning.domain.study.session.model.SessionId

/**
 * Lập kế hoạch thứ tự LearningItem có thể được học.
 *
 * Pipeline:
 *
 * candidate selection
 * -> QueueTransformationPipeline
 * -> StudyQueuePlanEntry
 *
 * QueueTransformationPipeline chịu trách nhiệm điều phối:
 *
 * StudyQueueStrategy
 * -> QueueDiversifier
 * -> QueueBalancer
 * -> final QueueDiversifier guard
 */
class StudyQueuePlanner(
    private val contentRepository: ContentRepository,
    private val learningItemRepository:
    LearningItemRepository,
    private val memoryStateRepository:
    MemoryStateRepository,
    private val selectionCandidateFactory:
    SelectionCandidateFactory =
        SelectionCandidateFactory(
            contentRepository =
                contentRepository,
            memoryStateRepository =
                memoryStateRepository
        ),
    private val defaultStrategy:
    StudyQueueStrategy =
        ReviewFirstStudyQueueStrategy(),
    private val defaultQueueDiversifier:
    QueueDiversifier =
        ContentDiversityQueueDiversifier(),
    private val defaultQueueBalancer:
    QueueBalancer =
        NoOpQueueBalancer(),
    private val transformationPipeline:
    QueueTransformationPipeline =
        QueueTransformationPipeline(),
    private val seededNewItemOrderer:
    SessionSeededNewItemOrderer =
        SessionSeededNewItemOrderer(),
    private val contentLearningStateQuery:
    ContentLearningStateQueryService? = null
) {

    /**
     * API tương thích sử dụng toàn bộ transformation mặc định.
     */
    fun plan(
        query: GetNextLearningItemQuery
    ): List<LearningItemId> =
        planEntries(
            query = query,
            strategy = defaultStrategy,
            queueDiversifier =
                defaultQueueDiversifier,
            queueBalancer =
                defaultQueueBalancer
        ).map { entry ->
            entry.learningItemId
        }

    /**
     * API tương thích sử dụng toàn bộ transformation mặc định.
     */
    fun planEntries(
        query: GetNextLearningItemQuery
    ): List<StudyQueuePlanEntry> =
        planEntries(
            query = query,
            strategy = defaultStrategy,
            queueDiversifier =
                defaultQueueDiversifier,
            queueBalancer =
                defaultQueueBalancer
        )

    /**
     * API tương thích cho phép chọn strategy.
     */
    fun planEntries(
        query: GetNextLearningItemQuery,
        strategy: StudyQueueStrategy
    ): List<StudyQueuePlanEntry> =
        planEntries(
            query = query,
            strategy = strategy,
            queueDiversifier =
                defaultQueueDiversifier,
            queueBalancer =
                defaultQueueBalancer
        )

    /**
     * API tương thích cho phép chọn strategy và diversity.
     */
    fun planEntries(
        query: GetNextLearningItemQuery,
        strategy: StudyQueueStrategy,
        queueDiversifier: QueueDiversifier
    ): List<StudyQueuePlanEntry> =
        planEntries(
            query = query,
            strategy = strategy,
            queueDiversifier =
                queueDiversifier,
            queueBalancer =
                defaultQueueBalancer
        )

    /**
     * Lập kế hoạch với đầy đủ transformation được cung cấp.
     */
    fun planEntries(
        query: GetNextLearningItemQuery,
        strategy: StudyQueueStrategy,
        queueDiversifier: QueueDiversifier,
        queueBalancer: QueueBalancer,
        sessionId: SessionId? = null
    ): List<StudyQueuePlanEntry> {
        val contentsById = contentRepository.findAll().associateBy { it.id }
        val memoryStatesByItemId =
            (memoryStateRepository as? vn.loi.learning.application.port.MemoryStateQuery)
                ?.findAll(query.learnerId)
                ?.associateBy { it.learningItemId }

        val enabledItems = learningItemRepository.findAllEnabled()
        val contentStates = contentLearningStateQuery?.resolveAll(
            query.learnerId,
            enabledItems.mapTo(linkedSetOf()) { it.contentId }
        ).orEmpty()
        val candidates =
            enabledItems
                .asSequence()
                .filterNot { learningItem ->
                    learningItem.id in
                            query.excludedItemIds
                }
                .filterNot { learningItem ->
                    learningItem.contentId in
                            query.excludedContentIds
                }
                .filter { learningItem ->
                    query.includedContentIds
                        .isEmpty() ||
                            learningItem.contentId in
                            query.includedContentIds
                }
                .mapNotNull { learningItem ->
                    if (memoryStatesByItemId == null) {
                        selectionCandidateFactory.create(
                            learningItem = learningItem,
                            learnerId = query.learnerId,
                            availableAt = query.now
                        )
                    } else {
                        selectionCandidateFactory.createResolved(
                            learningItem = learningItem,
                            content = contentsById[learningItem.contentId],
                            persistedMemoryState = memoryStatesByItemId[learningItem.id],
                            learnerId = query.learnerId,
                            availableAt = query.now
                        )
                    }
                }
                .map { preparedCandidate ->
                    preparedCandidate.candidate
                }
                .map { candidate ->
                    if (contentStates[candidate.contentId]?.isLearned == true &&
                        candidate.isNew
                    ) {
                        candidate.copy(
                            memoryState = candidate.memoryState.copy(
                                stage = vn.loi.learning.domain.study.memory.model.LearningStage.REVIEW
                            )
                        )
                    } else {
                        candidate
                    }
                }
                .filter { candidate ->
                    candidate.isIncludedBy(
                        query
                    )
                }
                .toList()

        val transformedCandidates =
            transformationPipeline.transform(
                candidates =
                    candidates,
                strategy =
                    strategy,
                queueDiversifier =
                    queueDiversifier,
                queueBalancer =
                    queueBalancer,
                postStrategyOrderer = { orderedCandidates ->
                    sessionId?.let {
                        seededNewItemOrderer.order(orderedCandidates, it)
                    } ?: orderedCandidates
                }
            )

        return transformedCandidates.map { candidate ->
            StudyQueuePlanEntry(
                learningItemId =
                    candidate.learningItemId,
                isNew =
                    candidate.isNew,
                contentId = candidate.contentId
            )
        }
    }

    private fun SelectionCandidate.isIncludedBy(
        query: GetNextLearningItemQuery
    ): Boolean {
        if (!isDueAt(query.now)) {
            return false
        }

        return if (isNew) {
            query.includeNewItems
        } else {
            query.includeReviewItems
        }
    }
}
