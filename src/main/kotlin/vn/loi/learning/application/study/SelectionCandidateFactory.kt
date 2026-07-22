package vn.loi.learning.application.study

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.content.model.Content
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.selection.model.SelectionCandidate

/**
 * Application factory dựng SelectionCandidate từ dữ liệu persistence.
 *
 * Factory là nguồn sự thật duy nhất cho việc:
 * - resolve Content của LearningItem;
 * - resolve MemoryState của learner;
 * - biểu diễn item chưa từng học bằng MemoryState.new(...);
 * - loại LearningItem đang disabled;
 * - loại MemoryState đang SUSPENDED.
 *
 * Factory không:
 * - lưu MemoryState mới vào persistence;
 * - thay đổi LearningItem hoặc Content;
 * - thực hiện ordering;
 * - thực hiện session selection;
 * - thực hiện scheduling.
 */
class SelectionCandidateFactory(
    private val contentRepository: ContentRepository,
    private val memoryStateRepository: MemoryStateRepository
) {

    fun create(
        learningItem: LearningItem,
        learnerId: LearnerId,
        availableAt: Moment
    ): PreparedSelectionCandidate? {
        if (!learningItem.isEnabled) {
            return null
        }

        val content =
            contentRepository.findById(
                learningItem.contentId
            ) ?: return null

        val persistedMemoryState =
            memoryStateRepository.find(
                learnerId = learnerId,
                learningItemId = learningItem.id
            )

        return createResolved(
            learningItem = learningItem,
            content = content,
            persistedMemoryState = persistedMemoryState,
            learnerId = learnerId,
            availableAt = availableAt
        )
    }

    fun createResolved(
        learningItem: LearningItem,
        content: Content?,
        persistedMemoryState: MemoryState?,
        learnerId: LearnerId,
        availableAt: Moment
    ): PreparedSelectionCandidate? {
        if (!learningItem.isEnabled || content == null) return null

        if (
            persistedMemoryState?.stage ==
            LearningStage.SUSPENDED
        ) {
            return null
        }

        val effectiveMemoryState =
            persistedMemoryState
                ?: MemoryState.new(
                    learnerId = learnerId,
                    learningItemId = learningItem.id,
                    availableAt = availableAt
                )

        return PreparedSelectionCandidate(
            content = content,
            candidate =
                SelectionCandidate(
                    learningItem = learningItem,
                    memoryState = effectiveMemoryState
                ),
            hasPersistedMemoryState =
                persistedMemoryState != null
        )
    }
}

/**
 * SelectionCandidate đã được resolve cùng Content tương ứng.
 *
 * hasPersistedMemoryState cho biết MemoryState trong candidate:
 * - true: đã tồn tại trong repository;
 * - false: là trạng thái NEW hiệu lực được tạo bằng MemoryState.new(...).
 */
data class PreparedSelectionCandidate(
    val content: Content,
    val candidate: SelectionCandidate,
    val hasPersistedMemoryState: Boolean
)
