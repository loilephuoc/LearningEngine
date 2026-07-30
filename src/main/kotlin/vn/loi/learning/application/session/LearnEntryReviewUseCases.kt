package vn.loi.learning.application.session

import java.util.UUID
import vn.loi.learning.application.contentpackaging.InstalledPackageContentQueryService
import vn.loi.learning.application.port.LearningItemRepository
import vn.loi.learning.application.port.MemoryStateQuery
import vn.loi.learning.application.port.ReviewEventRepository
import vn.loi.learning.application.port.StudySessionRepository
import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.domain.library.model.InstalledPackageId
import vn.loi.learning.domain.study.learning.model.LearningItem
import vn.loi.learning.domain.study.memory.model.LearnerId
import vn.loi.learning.domain.study.memory.model.MemoryState
import vn.loi.learning.domain.study.memory.model.LearningStage
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.domain.study.session.model.SessionPolicy
import vn.loi.learning.domain.study.session.model.SessionStatus
import vn.loi.learning.domain.study.session.model.StudySession

data class LearnEntryScope(
    val learnerId: LearnerId,
    val installedPackageId: InstalledPackageId,
    val topicId: TopicId?,
    val includedContentIds: Set<ContentId> = emptySet()
)

sealed interface LatestCompletedSessionAvailability {
    data class Available(val sessionId: SessionId, val itemCount: Int) :
        LatestCompletedSessionAvailability

    data object Unavailable : LatestCompletedSessionAvailability
}

sealed interface LearnedItemsReviewAvailability {
    data class Available(val totalLearnedCount: Int, val sessionItemCount: Int) :
        LearnedItemsReviewAvailability

    data object Unavailable : LearnedItemsReviewAvailability
}

data class LearnEntryReviewAvailability(
    val latestCompletedSession: LatestCompletedSessionAvailability,
    val learnedItems: LearnedItemsReviewAvailability
)

class LearnEntryReviewAvailabilityQuery(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val learningItems: LearningItemRepository,
    private val memoryStates: MemoryStateQuery?,
    private val reviewEvents: ReviewEventRepository,
    private val packageContentQuerySupplier: (() -> InstalledPackageContentQueryService?)?
) {
    fun execute(
        scope: LearnEntryScope,
        now: Moment
    ): LearnEntryReviewAvailability {
        val scopedItems = resolveScopedItems(scope) ?: emptyList()
        val scopedIds = scopedItems.mapTo(linkedSetOf()) { it.id }
        val latest = sessions.findAll()
            .asSequence()
            .filter { it.matchesFinishedScope(scope) }
            .mapNotNull { session ->
                val queue = queues.get(session.id) ?: return@mapNotNull null
                val committedCount = queue.completedLearningItemIds.count {
                    it in session.reviewedItemIds && it in scopedIds
                }
                session.takeIf { committedCount > 0 }?.let { it to committedCount }
            }
            .sortedWith(
                compareByDescending<Pair<StudySession, Int>> {
                    it.first.finishedAt?.epochMillis ?: Long.MIN_VALUE
                }.thenByDescending { it.first.startedAt.epochMillis }
                    .thenByDescending { it.first.id.value }
            )
            .firstOrNull()
            ?.let { LatestCompletedSessionAvailability.Available(it.first.id, it.second) }
            ?: LatestCompletedSessionAvailability.Unavailable

        val learned = learnedItems(scope.learnerId, scopedItems, now)
        val learnedAvailability =
            if (learned.isNotEmpty()) {
                LearnedItemsReviewAvailability.Available(
                    totalLearnedCount = learned.size,
                    sessionItemCount = learned.size
                )
            } else {
                LearnedItemsReviewAvailability.Unavailable
            }
        return LearnEntryReviewAvailability(latest, learnedAvailability)
    }

    internal fun learnedItems(
        learnerId: LearnerId,
        scopedItems: List<LearningItem>,
        now: Moment
    ): List<LearningItem> {
        val states = memoryStates?.findAll(learnerId).orEmpty()
            .filter { it.reviewCount > 0 && it.lastReviewedAt != null }
            .associateBy { it.learningItemId }
        val events = reviewEvents.findAll(learnerId)
        val latestEventAt = linkedMapOf<vn.loi.learning.domain.study.learning.model.LearningItemId, Moment>()
        events.forEach { latestEventAt[it.learningItemId] = it.reviewedAt }
        return scopedItems
            .asSequence()
            .filter { it.isEnabled }
            .filter { it.id in states || it.id in latestEventAt }
            .filter { states[it.id]?.stage != LearningStage.SUSPENDED }
            .distinctBy { it.contentId }
            .sortedWith(
                compareBy<LearningItem>(
                    { states[it.id]?.isDue(now) != true },
                    { states[it.id]?.lastReviewedAt?.epochMillis ?: latestEventAt[it.id]?.epochMillis ?: Long.MAX_VALUE },
                    { it.id.value }
                )
            )
            .toList()
    }

    internal fun resolveScopedItems(scope: LearnEntryScope): List<LearningItem>? {
        val contentIds =
            if (scope.includedContentIds.isNotEmpty()) {
                scope.includedContentIds
            } else {
                val query = packageContentQuerySupplier?.invoke() ?: return null
                try {
                    query.getContentsForPackage(scope.installedPackageId)
                        .mapTo(linkedSetOf()) { ContentId(it.id) }
                } catch (_: IllegalArgumentException) {
                    return null
                }
            }
        return learningItems.findByContentIds(contentIds)
    }

    private fun StudySession.matchesFinishedScope(scope: LearnEntryScope): Boolean =
        status == SessionStatus.FINISHED &&
            learnerId == scope.learnerId &&
            installedPackageId == scope.installedPackageId &&
            topicId == scope.topicId &&
            (
                scope.includedContentIds.isEmpty() ||
                    includedContentIds == scope.includedContentIds
            )
}

data class StartLearnedItemsReviewRequest(
    val scope: LearnEntryScope,
    val requestedAt: Moment
)

sealed interface StartLearnedItemsReviewResult {
    data class Accepted(val session: StudySession, val queue: StudyQueueSnapshot) :
        StartLearnedItemsReviewResult

    data object NoItems : StartLearnedItemsReviewResult

    data class Rejected(val reason: StartLearnedItemsReviewRejection) :
        StartLearnedItemsReviewResult
}

enum class StartLearnedItemsReviewRejection {
    INVALID_SCOPE,
    OTHER_ACTIVE_SESSION_EXISTS
}

class StartLearnedItemsReviewUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val availability: LearnEntryReviewAvailabilityQuery
) {
    fun execute(request: StartLearnedItemsReviewRequest): StartLearnedItemsReviewResult {
        sessions.findActiveByLearner(request.scope.learnerId)?.let {
            return StartLearnedItemsReviewResult.Rejected(
                StartLearnedItemsReviewRejection.OTHER_ACTIVE_SESSION_EXISTS
            )
        }
        val scopedItems =
            availability.resolveScopedItems(request.scope)
                ?: return StartLearnedItemsReviewResult.Rejected(
                    StartLearnedItemsReviewRejection.INVALID_SCOPE
                )
        val selected = availability.learnedItems(
            request.scope.learnerId,
            scopedItems,
            request.requestedAt
        )
        if (selected.isEmpty()) return StartLearnedItemsReviewResult.NoItems

        val sessionId = SessionId(UUID.randomUUID().toString())
        val policy =
            SessionPolicy(
                newItemLimit = 0,
                reviewItemLimit = selected.size,
                allowRepeatInSameSession = true
            )
        val session = StudySession.start(
            id = sessionId,
            learnerId = request.scope.learnerId,
            startedAt = request.requestedAt,
            policy = policy,
            includedContentIds = request.scope.includedContentIds,
            topicId = request.scope.topicId,
            installedPackageId = request.scope.installedPackageId
        )
        sessions.save(session)
        val queue =
            try {
                queues.create(
                    sessionId = sessionId,
                    createdAt = request.requestedAt,
                    learningItemIds = selected.map { it.id },
                    itemOrigins = selected.associate { it.id to SessionItemOrigin.REVIEW },
                    itemContentIds = selected.associate { it.id to it.contentId },
                    configuredReviewTarget = selected.size,
                    effectiveReviewWorkload = selected.map { it.contentId }.distinct().size
                )
            } catch (failure: RuntimeException) {
                sessions.deleteById(sessionId)
                throw failure
            }
        return StartLearnedItemsReviewResult.Accepted(session, queue)
    }
}
