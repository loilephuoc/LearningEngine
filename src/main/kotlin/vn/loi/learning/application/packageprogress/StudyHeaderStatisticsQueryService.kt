package vn.loi.learning.application.packageprogress

import vn.loi.learning.application.LearningEngine
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.memory.model.ReviewEvent
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.SessionItemOrigin

data class StudyStatisticsScope(val id: String, val contentIds: Set<ContentId>)

data class StudySessionProgressSource(
    val sessionId: String,
    val newConfiguredTarget: Int,
    val reviewConfiguredTarget: Int,
    val newCompleted: Int,
    val reviewCompleted: Int,
    val remainingLearningItemIds: Set<LearningItemId>,
    val remainingItemOrigins: Map<LearningItemId, SessionItemOrigin> = emptyMap()
)

data class StudySessionProgressStatistics(
    val sessionId: String,
    val newCompleted: Int,
    val newConfiguredTarget: Int,
    val newEffectiveWorkload: Int,
    val reviewRemaining: Int,
    val reviewConfiguredTarget: Int,
    val reviewEffectiveWorkload: Int
) {
    init {
        require(newCompleted in 0..newEffectiveWorkload)
        require(newEffectiveWorkload <= newConfiguredTarget)
        require(reviewRemaining in 0..reviewEffectiveWorkload)
        require(reviewEffectiveWorkload <= reviewConfiguredTarget)
    }
}

data class StudyPackageLearningStatistics(
    val scopeId: String,
    val calculatedAt: Moment,
    val totalLearned: Int,
    val dueCount: Int,
    val againCount: Int,
    val hardCount: Int,
    val goodCount: Int,
    val easyCount: Int,
    val nearestFutureDueAt: Moment?
) {
    init {
        require(totalLearned == againCount + hardCount + goodCount + easyCount)
        require(dueCount >= 0)
    }
}

data class StudyHeaderStatistics(
    val session: StudySessionProgressStatistics,
    val packageLearning: StudyPackageLearningStatistics
) {
    val scopeId get() = packageLearning.scopeId
    val calculatedAt get() = packageLearning.calculatedAt
    val total get() = packageLearning.totalLearned
    val newCompleted get() = session.newCompleted
    val newConfiguredTarget get() = session.newConfiguredTarget
    val newEffectiveWorkload get() = session.newEffectiveWorkload
    val reviewRemaining get() = session.reviewRemaining
    val reviewConfiguredTarget get() = session.reviewConfiguredTarget
    val reviewEffectiveWorkload get() = session.reviewEffectiveWorkload
    val dueCount get() = packageLearning.dueCount
    val againCount get() = packageLearning.againCount
    val hardCount get() = packageLearning.hardCount
    val goodCount get() = packageLearning.goodCount
    val easyCount get() = packageLearning.easyCount
    val nearestFutureDueAt get() = packageLearning.nearestFutureDueAt
}

class StudyHeaderStatisticsQueryService(
    private val engine: LearningEngine,
    private val memoryStateQuery: MemoryStateQuery,
    private val reviewEventRepository: ReviewEventRepository,
    private val clock: () -> Moment = { Moment(System.currentTimeMillis()) }
) {
    fun execute(
        scope: StudyStatisticsScope,
        session: StudySessionProgressSource,
        learnerId: LearnerId
    ): StudyHeaderStatistics {
        val at = clock()
        val scopeItemIds = engine.getLearningItemsByContentIds(scope.contentIds)
            .asSequence().filter { it.isEnabled }.map { it.id }.toSet()
        val states = memoryStateQuery.findAll(learnerId)
            .filter { it.learningItemId in scopeItemIds }.associateBy { it.learningItemId }
        val events = reviewEventRepository.findAll(learnerId)
            .filter { it.learningItemId in scopeItemIds }
        return projectStudyHeaderStatistics(scope.id, at, scopeItemIds, states, events, session)
    }
}

internal fun projectStudyHeaderStatistics(
    scopeId: String,
    at: Moment,
    itemIds: Set<LearningItemId>,
    statesByItem: Map<LearningItemId, MemoryState>,
    eventsInAuthoritativeOrder: List<ReviewEvent>,
    session: StudySessionProgressSource
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
    val remaining = session.remainingLearningItemIds intersect itemIds
    val remainingReview = remaining.count {
        when (session.remainingItemOrigins[it]) {
            SessionItemOrigin.NEW -> false
            SessionItemOrigin.REVIEW -> true
            null -> it in latestByItem
        }
    }
    val remainingNew = remaining.size - remainingReview
    val reviewedStates = latestByItem.keys.mapNotNull(statesByItem::get)
    val nearestFutureDueAt = reviewedStates.asSequence()
        .filterNot { it.stage == LearningStage.SUSPENDED }
        .map { it.dueAt }.filter { it > at }.minByOrNull { it.epochMillis }

    val packageLearning = StudyPackageLearningStatistics(
        scopeId = scopeId,
        calculatedAt = at,
        totalLearned = latestByItem.size,
        dueCount = reviewedStates.count { it.isDue(at) },
        againCount = latestByItem.values.count { it.rating == ReviewRating.AGAIN },
        hardCount = latestByItem.values.count { it.rating == ReviewRating.HARD },
        goodCount = latestByItem.values.count { it.rating == ReviewRating.GOOD },
        easyCount = latestByItem.values.count { it.rating == ReviewRating.EASY },
        nearestFutureDueAt = nearestFutureDueAt
    )
    val sessionProgress = StudySessionProgressStatistics(
        sessionId = session.sessionId,
        newCompleted = session.newCompleted,
        newConfiguredTarget = session.newConfiguredTarget,
        newEffectiveWorkload = (session.newCompleted + remainingNew)
            .coerceAtMost(session.newConfiguredTarget),
        reviewRemaining = remainingReview,
        reviewConfiguredTarget = session.reviewConfiguredTarget,
        reviewEffectiveWorkload = (session.reviewCompleted + remainingReview)
            .coerceAtMost(session.reviewConfiguredTarget)
    )
    return StudyHeaderStatistics(sessionProgress, packageLearning)
}
