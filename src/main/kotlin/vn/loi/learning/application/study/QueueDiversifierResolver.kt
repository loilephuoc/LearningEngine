package vn.loi.learning.application.study

import vn.loi.learning.domain.study.session.model.QueueDiversityPolicyType

/**
 * Ánh xạ lựa chọn diversity trong SessionPolicy sang QueueDiversifier
 * tương ứng.
 *
 * Resolver không:
 * - đọc repository;
 * - ordering candidate;
 * - áp dụng SessionPolicy limit;
 * - persist StudyQueue.
 */
class QueueDiversifierResolver(
    private val noOpDiversifier:
    QueueDiversifier =
        NoOpQueueDiversifier(),
    private val contentDiversityDiversifier:
    QueueDiversifier =
        ContentDiversityQueueDiversifier()
) {

    fun resolve(
        type: QueueDiversityPolicyType
    ): QueueDiversifier =
        when (type) {
            QueueDiversityPolicyType.NONE ->
                noOpDiversifier

            QueueDiversityPolicyType
                .CONTENT_DIVERSITY ->
                contentDiversityDiversifier
        }
}