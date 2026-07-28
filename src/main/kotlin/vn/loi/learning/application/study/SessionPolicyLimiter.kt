package vn.loi.learning.application.study

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.session.model.SessionPolicy

/**
 * Áp dụng giới hạn NEW và REVIEW của SessionPolicy lên một kế hoạch
 * đã được sắp xếp.
 *
 * Limiter giữ nguyên thứ tự của orderedEntries.
 *
 * Điều này rất quan trọng vì thứ tự đã được quyết định bởi
 * StudyQueueStrategy trước đó. Limiter chỉ loại những entry vượt
 * quá quota tương ứng, không được phân nhóm hoặc sắp xếp lại.
 */
class SessionPolicyLimiter {

    fun apply(
        orderedEntries:
        List<StudyQueuePlanEntry>,
        policy: SessionPolicy
    ): List<LearningItemId> =
        applyEntries(orderedEntries, policy).map(StudyQueuePlanEntry::learningItemId)

    fun applyEntries(
        orderedEntries: List<StudyQueuePlanEntry>,
        policy: SessionPolicy
    ): List<StudyQueuePlanEntry> {
        var selectedNewItemCount = 0
        var selectedReviewItemCount = 0

        return buildList {
            orderedEntries.forEach { entry ->
                if (entry.isNew) {
                    if (
                        selectedNewItemCount <
                        policy.newItemLimit
                    ) {
                        add(entry)
                        selectedNewItemCount += 1
                    }
                } else {
                    if (
                        selectedReviewItemCount <
                        policy.reviewItemLimit
                    ) {
                        add(entry)
                        selectedReviewItemCount += 1
                    }
                }
            }
        }
    }
}
