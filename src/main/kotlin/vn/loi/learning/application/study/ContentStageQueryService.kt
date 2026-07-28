package vn.loi.learning.application.study

import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState

class ContentStageQueryService(
    private val learningItemRepository: LearningItemRepository,
    private val memoryStateRepository: MemoryStateRepository
) {
    fun resolveContentStage(
        learnerId: LearnerId,
        contentId: ContentId
    ): LearningStage {
        val items = learningItemRepository.findByContentId(contentId)
        if (items.isEmpty()) return LearningStage.NEW

        val persistedMemoryStates = items.mapNotNull { item ->
            memoryStateRepository.find(learnerId, item.id)
        }

        if (persistedMemoryStates.isEmpty()) {
            return LearningStage.NEW
        }

        val selectedState = persistedMemoryStates.maxWithOrNull(
            compareBy<MemoryState> { state ->
                state.lastReviewedAt?.epochMillis ?: Long.MIN_VALUE
            }
            .thenBy { state -> state.reviewCount }
            .thenBy { state -> state.learningItemId.value }
        )

        return selectedState?.stage ?: LearningStage.NEW
    }
}
