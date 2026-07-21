package vn.loi.learning.application.study

import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

/**
 * Application service chuyển StudySession thành StudyQueuePlan.
 */
class StudyQueuePlanningService(
    private val planner: StudyQueuePlanner,
    private val policyLimiter:
    SessionPolicyLimiter =
        SessionPolicyLimiter(),
    private val strategyResolver:
    StudyQueueStrategyResolver =
        StudyQueueStrategyResolver(),
    private val queueDiversifierResolver:
    QueueDiversifierResolver =
        QueueDiversifierResolver(),
    private val queueBalancerResolver:
    QueueBalancerResolver =
        QueueBalancerResolver()
) {

    fun plan(
        session: StudySession
    ): StudyQueuePlan {
        require(
            session.status ==
                    SessionStatus.ACTIVE
        ) {
            "Cannot plan a study queue for a finished session."
        }

        val strategy =
            strategyResolver.resolve(
                session.policy.queueStrategy
            )

        val queueDiversifier =
            queueDiversifierResolver.resolve(
                session.policy
                    .queueDiversityPolicy
            )

        val queueBalancer =
            queueBalancerResolver.resolve(
                session.policy
                    .difficultyBalancePolicy
            )

        val orderedEntries =
            planner.planEntries(
                query =
                    GetNextLearningItemQuery(
                        learnerId =
                            session.learnerId,
                        now =
                            session.startedAt,
                        excludedItemIds =
                            session.reviewedItemIds,
                        excludedContentIds =
                            session.reviewedContentIds,
                        includedContentIds =
                            session.includedContentIds,
                        includeNewItems =
                            session.policy
                                .newItemLimit > 0,
                        includeReviewItems =
                            session.policy
                                .reviewItemLimit > 0
                    ),
                strategy =
                    strategy,
                queueDiversifier =
                    queueDiversifier,
                queueBalancer =
                    queueBalancer
            )

        val limitedItemIds =
            policyLimiter.apply(
                orderedEntries =
                    orderedEntries,
                policy =
                    session.policy
            )

        return StudyQueuePlan.create(
            sessionId =
                session.id,
            plannedAt =
                session.startedAt,
            learningItemIds =
                limitedItemIds
        )
    }
}