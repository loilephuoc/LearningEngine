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
        val selectedNewIdentities = linkedSetOf<Any>()
        val selectedReviewIdentities = linkedSetOf<Any>()

        return buildList {
            orderedEntries.forEach { entry ->
                val identity = entry.contentId ?: entry.learningItemId
                if (entry.isNew) {
                    if (identity in selectedNewIdentities ||
                        selectedNewIdentities.size < policy.newItemLimit
                    ) {
                        add(entry)
                        selectedNewIdentities += identity
                    }
                } else {
                    if (identity in selectedReviewIdentities ||
                        selectedReviewIdentities.size < policy.reviewItemLimit
                    ) {
                        add(entry)
                        selectedReviewIdentities += identity
                    }
                }
            }
        }
    }
}
