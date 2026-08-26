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

data class DailyStudyBudgetLimits(
    val newPerDay: Int = 20,
    val reviewPerDay: Int = 100
) {
    init {
        require(newPerDay in 1..999) {
            "New items per day must be between 1 and 999."
        }

        require(reviewPerDay in 1..999) {
            "Review items per day must be between 1 and 999."
        }
    }
}

data class DailyStudyBudgetSnapshot(
    val limits: DailyStudyBudgetLimits,
    val newCompletedToday: Int,
    val reviewCompletedToday: Int,
    val dueReviewCount: Int,
    val eligibleNewContentCount: Int
) {
    val newRemainingToday =
        (limits.newPerDay - newCompletedToday)
            .coerceAtLeast(0)

    val reviewRemainingToday =
        (limits.reviewPerDay - reviewCompletedToday)
            .coerceAtLeast(0)

    val hasEligibleWork =
        (newRemainingToday > 0 &&
            eligibleNewContentCount > 0) ||
            (reviewRemainingToday > 0 &&
                dueReviewCount > 0)

    val targetsComplete =
        newRemainingToday == 0 &&
            reviewRemainingToday == 0
}

/**
 * Learner-global daily accounting with eligibility restricted
 * to the requested content scope.
 */
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

        val date =
            Instant.ofEpochMilli(at.epochMillis)
                .atZone(zoneId)
                .toLocalDate()

        val dayStart =
            date.atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()

        val dayEnd =
            date.plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()

        /*
         * Daily accounting needs review history globally for the learner,
         * but it does not need every LearningItem domain object.
         */
        val events =
            measured("review_events_find_all") {
                reviewEvents.findAll(
                    learnerId
                )
            }

        val today =
            measured("filter_today_events") {
                events.filter {
                    it.reviewedAt.epochMillis in
                        dayStart until dayEnd
                }
            }

        /*
         * Resolve LearningItem -> Content only for LearningItems that
         * actually occur in review history.
         */
        val reviewedLearningItemIds =
            measured("reviewed_learning_item_ids") {
                events.asSequence()
                    .map {
                        it.learningItemId
                    }
                    .toSet()
            }

        val contentByItem =
            measured("content_ids_by_reviewed_items") {
                learningItems
                    .findContentIdsByLearningItemIds(
                        reviewedLearningItemIds
                    )
            }

        val newCompleted =
            measured("new_completed") {
                today.asSequence()
                    .filter {
                        it.stateBefore.reviewCount == 0
                    }
                    .mapNotNull {
                        contentByItem[
                            it.learningItemId
                        ]
                    }
                    .distinct()
                    .count()
            }

        val reviewCompleted =
            measured("review_completed") {
                today.count {
                    it.stateBefore.reviewCount > 0
                }
            }

        val introducedContent =
            measured("introduced_content") {
                events.asSequence()
                    .mapNotNull {
                        contentByItem[
                            it.learningItemId
                        ]
                    }
                    .toSet()
            }

        /*
         * Only materialize LearningItems belonging to the requested
         * content scope instead of calling findAllEnabled().
         */
        val scopedItems =
            measured("find_scoped_items") {
                learningItems.findByContentIds(
                    scopeContentIds
                )
            }

        /*
         * findByContentIds() is not defined as enabled-only.
         * Preserve the old findAllEnabled() semantics explicitly.
         */
        val enabledScopedItems =
            measured("filter_enabled_scoped_items") {
                scopedItems.filter {
                    it.isEnabled
                }
            }

        val states =
            measured("memory_states_find_all") {
                memories.findAll(
                    learnerId
                ).associateBy {
                    it.learningItemId
                }
            }

        val due =
            measured("count_due") {
                enabledScopedItems.count { item ->
                    states[item.id]?.let {
                        it.reviewCount > 0 &&
                            it.stage !=
                            LearningStage.SUSPENDED &&
                            it.isDue(at)
                    } == true
                }
            }

        val eligibleNewContentCount =
            measured("eligible_new_content") {
                enabledScopedItems
                    .map {
                        it.contentId
                    }
                    .distinct()
                    .count {
                        it !in introducedContent
                    }
            }

        return DailyStudyBudgetSnapshot(
            limits = limits,
            newCompletedToday = newCompleted,
            reviewCompletedToday =
                reviewCompleted,
            dueReviewCount = due,
            eligibleNewContentCount =
                eligibleNewContentCount
        )
    }

    private inline fun <T> measured(
        phase: String,
        block: () -> T
    ): T {
        val started =
            System.nanoTime()

        return try {
            block()
        } finally {
            val elapsedMs =
                (
                    System.nanoTime() -
                        started
                    ) / 1_000_000

            println(
                "LearningEngineDailyBudget " +
                    "phase=$phase " +
                    "elapsedMs=$elapsedMs " +
                    "thread=${Thread.currentThread().name}"
            )
        }
    }
}