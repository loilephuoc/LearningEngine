package vn.loi.learning.application.session

import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.ReviewRating

data class RatingInventory(
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val neverReviewedCount: Int,
    val totalEligibleContentCount: Int
) {
    init {
        require(againCount + hardCount + goodCount + easyCount + neverReviewedCount == totalEligibleContentCount)
    }
}

/** Realtime content-level projection over committed evaluative events. */
class RatingInventoryQuery(
    private val learningItems: LearningItemRepository,
    private val memoryStates: MemoryStateQuery?,
    private val reviewEvents: ReviewEventRepository,
    private val packageContentQuerySupplier: (() -> InstalledPackageContentQueryService?)?
) {
    fun execute(scope: LearnEntryScope): RatingInventory {
        val contentIds = if (scope.includedContentIds.isNotEmpty()) {
            scope.includedContentIds
        } else {
            val query = requireNotNull(packageContentQuerySupplier?.invoke()) {
                "Installed package content query is unavailable."
            }
            query.getContentsForPackage(scope.installedPackageId).mapTo(linkedSetOf()) { ContentId(it.id) }
        }
        val suspended = memoryStates?.findAll(scope.learnerId).orEmpty()
            .filter { it.stage == LearningStage.SUSPENDED }
            .mapTo(hashSetOf()) { it.learningItemId }
        val representatives = learningItems.findByContentIds(contentIds)
            .asSequence()
            .filter { it.isEnabled && it.id !in suspended }
            .groupBy { it.contentId }
            .mapValues { (_, siblings) -> siblings.minBy { it.id.value } }
        val itemContent = learningItems.findByContentIds(representatives.keys)
            .associate { it.id to it.contentId }
        val latest = linkedMapOf<ContentId, ReviewRating>()
        reviewEvents.findAll(scope.learnerId).forEach { event ->
            itemContent[event.learningItemId]?.takeIf { it in representatives }?.let {
                latest[it] = event.rating
            }
        }
        fun count(rating: ReviewRating) = representatives.keys.count { latest[it] == rating }
        return RatingInventory(
            againCount = count(ReviewRating.AGAIN),
            hardCount = count(ReviewRating.HARD),
            goodCount = count(ReviewRating.GOOD),
            easyCount = count(ReviewRating.EASY),
            neverReviewedCount = representatives.keys.count { it !in latest },
            totalEligibleContentCount = representatives.size
        )
    }
}
