package vn.loi.learning.application.study

import java.time.Instant
import java.time.ZoneId
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment

data class DailyStudyBudgetLimits(val newPerDay: Int = 20, val reviewPerDay: Int = 100) {
    init {
        require(newPerDay in 1..999) { "New items per day must be between 1 and 999." }
        require(reviewPerDay in 1..999) { "Review items per day must be between 1 and 999." }
    }
}

data class DailyStudyBudgetSnapshot(
    val limits: DailyStudyBudgetLimits,
    val newCompletedToday: Int,
    val reviewCompletedToday: Int,
    val dueReviewCount: Int,
    val eligibleNewContentCount: Int
) {
    val newRemainingToday = (limits.newPerDay - newCompletedToday).coerceAtLeast(0)
    val reviewRemainingToday = (limits.reviewPerDay - reviewCompletedToday).coerceAtLeast(0)
    val hasEligibleWork =
        (newRemainingToday > 0 && eligibleNewContentCount > 0) ||
            (reviewRemainingToday > 0 && dueReviewCount > 0)
    val targetsComplete = newRemainingToday == 0 && reviewRemainingToday == 0
}

/** Learner-global daily accounting with eligibility restricted to the requested content scope. */
class DailyStudyBudgetQueryService(
    private val reviewEvents: ReviewEventRepository,
    private val memories: MemoryStateQuery,
    private val learningItems: LearningItemRepository
) {
    fun execute(
        learnerId: LearnerId,
        limits: DailyStudyBudgetLimits,
        at: Moment,
        zoneId: ZoneId,
        scopeContentIds: Set<ContentId>
    ): DailyStudyBudgetSnapshot {
        val date = Instant.ofEpochMilli(at.epochMillis).atZone(zoneId).toLocalDate()
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val items = learningItems.findAllEnabled()
        val contentByItem = items.associate { it.id to it.contentId }
        val events = reviewEvents.findAll(learnerId)
        val today = events.filter { it.reviewedAt.epochMillis in dayStart until dayEnd }
        val newCompleted = today.asSequence()
            .filter { it.stateBefore.reviewCount == 0 }
            .mapNotNull { contentByItem[it.learningItemId] }
            .distinct().count()
        val reviewCompleted = today.count { it.stateBefore.reviewCount > 0 }
        val introducedContent = events.asSequence().mapNotNull { contentByItem[it.learningItemId] }.toSet()
        val scopedItems = items.filter { it.contentId in scopeContentIds }
        val states = memories.findAll(learnerId).associateBy { it.learningItemId }
        val due = scopedItems.count { item ->
            states[item.id]?.let { it.reviewCount > 0 && it.stage != LearningStage.SUSPENDED && it.isDue(at) } == true
        }
        return DailyStudyBudgetSnapshot(
            limits = limits,
            newCompletedToday = newCompleted,
            reviewCompletedToday = reviewCompleted,
            dueReviewCount = due,
            eligibleNewContentCount = scopedItems.map { it.contentId }.distinct().count { it !in introducedContent }
        )
    }
}
