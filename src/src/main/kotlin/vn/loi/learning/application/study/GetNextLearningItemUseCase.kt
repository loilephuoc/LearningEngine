package vn.loi.learning.application.study

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment

class GetNextLearningItemUseCase(
    private val contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    private val memoryStateRepository: MemoryStateRepository
) {

    fun execute(
        query: GetNextLearningItemQuery
    ): NextLearningItem? {
        val candidates = learningItemRepository
            .findAllEnabled()
            .asSequence()

            // Không lấy lại chính LearningItem đã bị loại.
            .filterNot { item ->
                item.id in query.excludedItemIds
            }

            // Sibling Filter:
            // Không lấy LearningItem thuộc Content đã xuất hiện trong session.
            .filterNot { item ->
                item.contentId in query.excludedContentIds
            }

            .mapNotNull { item ->
                createCandidate(
                    item = item,
                    learnerId = query.learnerId
                )
            }
            .toList()

        if (query.includeReviewItems) {
            val dueReview = candidates
                .asSequence()
                .filter { candidate ->
                    val state = candidate.memoryState
                    state != null && state.isDue(query.now)
                }
                .minByOrNull { candidate ->
                    requireNotNull(candidate.memoryState)
                        .dueAt
                        .epochMillis
                }

            if (dueReview != null) {
                return dueReview.toResult()
            }
        }

        if (!query.includeNewItems) {
            return null
        }

        val newItem = candidates.firstOrNull {
            it.memoryState == null
        }

        return newItem?.toResult(
            effectiveDueAt = query.now
        )
    }

    private fun createCandidate(
        item: LearningItem,
        learnerId: LearnerId
    ): Candidate? {
        val content = contentRepository.findById(item.contentId)
            ?: return null

        val memoryState = memoryStateRepository.find(
            learnerId = learnerId,
            learningItemId = item.id
        )

        if (memoryState?.stage == LearningStage.SUSPENDED) {
            return null
        }

        return Candidate(
            content = content,
            learningItem = item,
            memoryState = memoryState
        )
    }

    private data class Candidate(
        val content: Content,
        val learningItem: LearningItem,
        val memoryState: MemoryState?
    ) {

        fun toResult(
            effectiveDueAt: Moment =
                requireNotNull(memoryState).dueAt
        ): NextLearningItem =
            NextLearningItem(
                content = content,
                learningItem = learningItem,
                memoryState = memoryState,
                effectiveDueAt = effectiveDueAt
            )
    }
}