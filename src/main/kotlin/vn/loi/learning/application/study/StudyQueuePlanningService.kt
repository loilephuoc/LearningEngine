package vn.loi.learning.application.study

import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.topic.TopicQueryService
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
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
        QueueBalancerResolver(),
    private val packageContentQuerySupplier: (() -> InstalledPackageContentQueryService?)? = null,
    private val topicQueryServiceSupplier: (() -> TopicQueryService?)? = null
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

        val effectiveIncludedContentIds = resolveEffectiveIncludedContentIds(session)

        val initialEntries =
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
                            effectiveIncludedContentIds,
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
                    queueBalancer,
                sessionId = session.id
            )

        val orderedEntries = if (initialEntries.isEmpty() && (session.includedContentIds.isNotEmpty() || session.installedPackageId != null)) {
            planner.planEntries(
                query =
                    GetNextLearningItemQuery(
                        learnerId =
                            session.learnerId,
                        now =
                            vn.loi.learning.domain.study.memory.model.Moment(Long.MAX_VALUE / 2),
                        excludedItemIds =
                            session.reviewedItemIds,
                        excludedContentIds =
                            session.reviewedContentIds,
                        includedContentIds =
                            effectiveIncludedContentIds,
                        includeNewItems =
                            true,
                        includeReviewItems =
                            true
                    ),
                strategy =
                    strategy,
                queueDiversifier =
                    queueDiversifier,
                queueBalancer =
                    queueBalancer,
                sessionId = session.id
            )
        } else {
            initialEntries
        }

        val limitedEntries =
            policyLimiter.applyEntries(
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
                limitedEntries.map(StudyQueuePlanEntry::learningItemId),
            itemOrigins = limitedEntries.associate {
                it.learningItemId to
                    if (it.isNew) SessionItemOrigin.NEW else SessionItemOrigin.REVIEW
            },
            itemContentIds = limitedEntries.mapNotNull { entry ->
                entry.contentId?.let { entry.learningItemId to it }
            }.toMap(),
            configuredNewTarget = session.policy.newItemLimit,
            effectiveNewWorkload = limitedEntries
                .filter(StudyQueuePlanEntry::isNew)
                .map { it.contentId ?: it.learningItemId }
                .distinct()
                .size,
            configuredReviewTarget = session.policy.reviewItemLimit,
            effectiveReviewWorkload = limitedEntries
                .filterNot(StudyQueuePlanEntry::isNew)
                .map { it.contentId ?: it.learningItemId }
                .distinct()
                .size
        )
    }

    private fun resolveEffectiveIncludedContentIds(session: StudySession): Set<ContentId> {
        if (session.includedContentIds.isNotEmpty()) {
            return session.includedContentIds
        }
        val packageId = session.installedPackageId
        val packageQuery = packageContentQuerySupplier?.invoke()
        if (packageId != null && packageQuery != null) {
            val packageItems = try {
                packageQuery.getContentsForPackage(packageId)
            } catch (e: Exception) {
                emptyList()
            }
            if (packageItems.isNotEmpty()) {
                return packageItems.map { ContentId(it.id) }.toSet()
            } else {
                return setOf(ContentId("__NONE_AVAILABLE_FOR_PACKAGE__"))
            }
        }
        return emptySet()
    }
}
