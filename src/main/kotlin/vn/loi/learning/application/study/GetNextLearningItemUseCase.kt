package vn.loi.learning.application.study

import vn.loi.learning.application.port.ContentRepository
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateRepository
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.Moment

class GetNextLearningItemUseCase(
    contentRepository: ContentRepository,
    private val learningItemRepository: LearningItemRepository,
    memoryStateRepository: MemoryStateRepository,
    private val selectionCandidateFactory:
    SelectionCandidateFactory =
        SelectionCandidateFactory(
            contentRepository = contentRepository,
            memoryStateRepository = memoryStateRepository
        ),
    private val studyQueuePlanner:
    StudyQueuePlanner =
        StudyQueuePlanner(
            contentRepository = contentRepository,
            learningItemRepository =
                learningItemRepository,
            memoryStateRepository =
                memoryStateRepository,
            selectionCandidateFactory =
                selectionCandidateFactory
        )
) {

    fun execute(
        query: GetNextLearningItemQuery
    ): NextLearningItem? {
        val learningItemId =
            studyQueuePlanner
                .plan(query)
                .firstOrNull()
                ?: return null

        return getById(
            learnerId = query.learnerId,
            learningItemId = learningItemId,
            now = query.now
        )
    }

    /**
     * Dựng NextLearningItem từ một LearningItemId
     * đã được selection mechanism khác xác định trước.
     *
     * Capability này không chạy lại priority selection.
     * Nó chỉ resolve LearningItem, Content và MemoryState
     * cần thiết để tạo kết quả Application.
     *
     * Với item chưa từng học, MemoryState NEW hiệu lực được tạo
     * bằng MemoryState.new(...), nhưng không được lưu tại đây.
     */
    fun getById(
        learnerId: LearnerId,
        learningItemId: LearningItemId,
        now: Moment
    ): NextLearningItem? {
        val learningItem =
            learningItemRepository.findById(
                learningItemId
            ) ?: return null

        val preparedCandidate =
            selectionCandidateFactory.create(
                learningItem = learningItem,
                learnerId = learnerId,
                availableAt = now
            ) ?: return null

        val candidate =
            preparedCandidate.candidate

        return NextLearningItem(
            content = preparedCandidate.content,
            learningItem =
                candidate.learningItem,
            memoryState =
                candidate.memoryState,
            effectiveDueAt =
                candidate.memoryState.dueAt
        )
    }
}