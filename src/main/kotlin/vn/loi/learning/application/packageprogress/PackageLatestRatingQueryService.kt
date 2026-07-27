package vn.loi.learning.application.packageprogress

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating

class PackageLatestRatingQueryService(
    private val packageContentQuery: InstalledPackageContentQueryService,
    private val engine: LearningEngine,
    private val reviewEventRepository: ReviewEventRepository
) {
    fun executeAll(
        installedPackageIds: Collection<InstalledPackageId>,
        learnerId: LearnerId
    ): Map<InstalledPackageId, Result<PackageLatestRatingDistribution>> {
        if (installedPackageIds.isEmpty()) return emptyMap()
        val allEvents = reviewEventRepository.findAll(learnerId)
        return installedPackageIds.distinct().associateWith { installedPackageId ->
            runCatching { project(installedPackageId, allEvents) }
        }
    }

    private fun project(
        installedPackageId: InstalledPackageId,
        allEvents: List<ReviewEvent>
    ): PackageLatestRatingDistribution {
        val contentIds = packageContentQuery.getContentsForPackage(installedPackageId)
            .mapTo(hashSetOf()) { ContentId(it.id) }
        if (contentIds.isEmpty()) return PackageLatestRatingDistribution.EMPTY

        val itemIds = engine.getLearningItemsByContentIds(contentIds)
            .asSequence()
            .filter { it.isEnabled }
            .mapTo(hashSetOf()) { it.id }
        if (itemIds.isEmpty()) return PackageLatestRatingDistribution.EMPTY

        return latestRatingDistribution(itemIds, allEvents)
    }
}

internal fun latestRatingDistribution(
    packageLearningItemIds: Set<vn.loi.learning.domain.study.learning.model.LearningItemId>,
    eventsInAuthoritativeOrder: List<ReviewEvent>
): PackageLatestRatingDistribution {
    val latestByItem = linkedMapOf<vn.loi.learning.domain.study.learning.model.LearningItemId, ReviewEvent>()
    eventsInAuthoritativeOrder.forEach { event ->
        if (event.learningItemId in packageLearningItemIds) {
            val current = latestByItem[event.learningItemId]
            if (current == null || event.reviewedAt >= current.reviewedAt) {
                latestByItem[event.learningItemId] = event
            }
        }
    }
    return PackageLatestRatingDistribution(
        againCount = latestByItem.values.count { it.rating == ReviewRating.AGAIN },
        hardCount = latestByItem.values.count { it.rating == ReviewRating.HARD },
        goodCount = latestByItem.values.count { it.rating == ReviewRating.GOOD },
        easyCount = latestByItem.values.count { it.rating == ReviewRating.EASY }
    )
}
