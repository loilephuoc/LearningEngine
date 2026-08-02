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
        val selectedIdentities = linkedSetOf<Any>()
        var selectedNewCount = 0
        var selectedReviewCount = 0

        return buildList {
            orderedEntries.forEach { entry ->
                val identity = entry.contentId ?: entry.learningItemId
                if (identity in selectedIdentities) return@forEach
                val withinQuota = if (entry.isNew) {
                    selectedNewCount < policy.newItemLimit
                } else {
                    selectedReviewCount < policy.reviewItemLimit
                }
                if (withinQuota) {
                    add(entry)
                    selectedIdentities += identity
                    if (entry.isNew) selectedNewCount++ else selectedReviewCount++
                }
            }
        }
    }
}
