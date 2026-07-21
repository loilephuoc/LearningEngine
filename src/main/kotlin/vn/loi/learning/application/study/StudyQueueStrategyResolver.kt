package vn.loi.learning.application.study

import vn.loi.learning.domain.study.session.model.StudyQueueStrategyType

/**
 * Ánh xạ lựa chọn strategy trong SessionPolicy sang implementation
 * ordering tương ứng.
 *
 * Resolver không:
 * - đọc repository;
 * - lựa chọn candidate;
 * - áp dụng SessionPolicy limit;
 * - tạo hoặc persist StudyQueue.
 */
class StudyQueueStrategyResolver(
    private val reviewFirstStrategy:
    StudyQueueStrategy =
        ReviewFirstStudyQueueStrategy(),
    private val newFirstStrategy:
    StudyQueueStrategy =
        NewFirstStudyQueueStrategy(),
    private val interleavedStrategy:
    StudyQueueStrategy =
        InterleavedStudyQueueStrategy(),
    private val adaptiveStrategy:
    StudyQueueStrategy =
        AdaptiveStudyQueueStrategy()
) {

    fun resolve(
        type: StudyQueueStrategyType
    ): StudyQueueStrategy =
        when (type) {
            StudyQueueStrategyType.REVIEW_FIRST ->
                reviewFirstStrategy

            StudyQueueStrategyType.NEW_FIRST ->
                newFirstStrategy

            StudyQueueStrategyType.INTERLEAVED ->
                interleavedStrategy

            StudyQueueStrategyType.ADAPTIVE ->
                adaptiveStrategy
        }
}