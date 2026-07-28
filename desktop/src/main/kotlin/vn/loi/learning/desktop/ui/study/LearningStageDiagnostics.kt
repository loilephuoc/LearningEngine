package vn.loi.learning.desktop.ui.study

import vn.loi.learning.application.study.NextLearningItem
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment

data class LearningStageDiagnostics(
    val learningItemId: String,
    val contentId: String,
    val stage: LearningStage,
    val reviewCount: Int,
    val lastReviewedAt: Moment?,
    val hasPersistedMemoryState: Boolean,
    val selectionReason: LearningStageSelectionReason
)

enum class LearningStageSelectionReason {
    CURRENT_SESSION_QUEUE_ITEM
}

object LearningStageDiagnosticsResolver {
    fun resolve(item: NextLearningItem): LearningStageDiagnostics =
        LearningStageDiagnostics(
            learningItemId = item.learningItem.id.value,
            contentId = item.content.id.value,
            stage = item.learningStage,
            reviewCount = item.memoryState?.reviewCount ?: 0,
            lastReviewedAt = item.memoryState?.lastReviewedAt,
            hasPersistedMemoryState = item.hasPersistedMemoryState,
            selectionReason = LearningStageSelectionReason.CURRENT_SESSION_QUEUE_ITEM
        )
}
