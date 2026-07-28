package vn.loi.learning.application.packageprogress

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating

data class StudyStatisticsScope(
    val id: String,
    val contentIds: Set<ContentId>
)

data class StudyHeaderStatistics(
    val scopeId: String,
    val calculatedAt: Moment,
    val total: Int,
    val newCount: Int,
    val reviewCount: Int,
    val dueCount: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val nearestFutureDueAt: Moment?
) {
    init {
        require(
            listOf(total, newCount, reviewCount, dueCount, againCount, hardCount, goodCount, easyCount)
                .all { it >= 0 }
        )
        require(newCount + reviewCount == total)
        require(againCount + hardCount + goodCount + easyCount <= reviewCount)
    }
}

class StudyHeaderStatisticsQueryService(
    private val engine: LearningEngine,
    private val memoryStateQuery: MemoryStateQuery,
    private val reviewEventRepository: ReviewEventRepository,
    private val clock: () -> Moment = { Moment(System.currentTimeMillis()) }
) {
    fun execute(scope: StudyStatisticsScope, learnerId: LearnerId): StudyHeaderStatistics {
        val at = clock()
        val itemIds = engine.getLearningItemsByContentIds(scope.contentIds)
            .asSequence()
            .filter { it.isEnabled }
            .map { it.id }
            .toSet()
        val states = memoryStateQuery.findAll(learnerId)
            .filter { it.learningItemId in itemIds }
            .associateBy { it.learningItemId }
        val events = reviewEventRepository.findAll(learnerId)
            .filter { it.learningItemId in itemIds }
        return projectStudyHeaderStatistics(scope.id, at, itemIds, states, events)
    }
}

internal fun projectStudyHeaderStatistics(
    scopeId: String,
    at: Moment,
    itemIds: Set<LearningItemId>,
    statesByItem: Map<LearningItemId, MemoryState>,
    eventsInAuthoritativeOrder: List<ReviewEvent>
): StudyHeaderStatistics {
    val latestByItem = linkedMapOf<LearningItemId, ReviewEvent>()
    eventsInAuthoritativeOrder.forEach { event ->
        if (event.learningItemId in itemIds) {
            val current = latestByItem[event.learningItemId]
            if (current == null || event.reviewedAt >= current.reviewedAt) {
                latestByItem[event.learningItemId] = event
            }
        }
    }
    val reviewedIds = latestByItem.keys
    val reviewedStates = reviewedIds.mapNotNull(statesByItem::get)
    val nearestFutureDueAt = reviewedStates
        .asSequence()
        .filterNot { it.stage == vn.loi.learning.domain.study.memory.model.LearningStage.SUSPENDED }
        .map { it.dueAt }
        .filter { it > at }
        .minByOrNull { it.epochMillis }

    return StudyHeaderStatistics(
        scopeId = scopeId,
        calculatedAt = at,
        total = itemIds.size,
        newCount = itemIds.count { it !in reviewedIds },
        reviewCount = reviewedIds.size,
        dueCount = reviewedStates.count { it.isDue(at) },
        againCount = latestByItem.values.count { it.rating == ReviewRating.AGAIN },
        hardCount = latestByItem.values.count { it.rating == ReviewRating.HARD },
        goodCount = latestByItem.values.count { it.rating == ReviewRating.GOOD },
        easyCount = latestByItem.values.count { it.rating == ReviewRating.EASY },
        nearestFutureDueAt = nearestFutureDueAt
    )
}
