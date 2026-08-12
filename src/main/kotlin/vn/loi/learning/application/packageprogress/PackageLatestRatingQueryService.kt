package vn.loi.learning.application.packageprogress

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.application.contentlibrary.LibraryContentItem

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

    internal fun executeAllFromSnapshot(
        installedPackageIds: Collection<InstalledPackageId>,
        learnerId: LearnerId,
        contentsByPackage: Map<InstalledPackageId, Result<List<LibraryContentItem>>>,
        learningItemsByContentId: Map<ContentId, List<LearningItem>>
    ): Map<InstalledPackageId, Result<PackageLatestRatingDistribution>> {
        if (installedPackageIds.isEmpty()) return emptyMap()
        val latestEventByItemId = linkedMapOf<LearningItemId, ReviewEvent>()
        reviewEventRepository.findAll(learnerId).forEach { event ->
            val current = latestEventByItemId[event.learningItemId]
            if (current == null || event.reviewedAt >= current.reviewedAt) {
                latestEventByItemId[event.learningItemId] = event
            }
        }
        val result = installedPackageIds.distinct().associateWith { installedPackageId ->
            contentsByPackage.getValue(installedPackageId).mapCatching { contents ->
                val itemIds = contents.asSequence()
                    .flatMap { content -> learningItemsByContentId[ContentId(content.id)].orEmpty().asSequence() }
                    .filter { it.isEnabled }
                    .mapTo(linkedSetOf()) { it.id }
                latestRatingDistributionFromLatestEvents(itemIds, latestEventByItemId)
            }
        }
        return result
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

private fun latestRatingDistributionFromLatestEvents(
    packageLearningItemIds: Set<LearningItemId>,
    latestEventByItemId: Map<LearningItemId, ReviewEvent>
): PackageLatestRatingDistribution {
    val ratings = packageLearningItemIds.mapNotNull(latestEventByItemId::get)
    return PackageLatestRatingDistribution(
        againCount = ratings.count { it.rating == ReviewRating.AGAIN },
        hardCount = ratings.count { it.rating == ReviewRating.HARD },
        goodCount = ratings.count { it.rating == ReviewRating.GOOD },
        easyCount = ratings.count { it.rating == ReviewRating.EASY }
    )
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
