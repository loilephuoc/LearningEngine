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
import vn.loi.learning.domain.study.memory.model.ReviewRating
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

sealed interface LatestCompletedNewItemsAvailability {
    data class Available(val sessionId: SessionId, val itemCount: Int) :
        LatestCompletedNewItemsAvailability

    data object Unavailable : LatestCompletedNewItemsAvailability
}

sealed interface DifficultItemsReviewAvailability {
    data class Available(val totalItemCount: Int, val sessionItemCount: Int) :
        DifficultItemsReviewAvailability

    data object Unavailable : DifficultItemsReviewAvailability
}

sealed interface LearnedItemsReviewAvailability {
    data class Available(val totalLearnedCount: Int, val sessionItemCount: Int) :
        LearnedItemsReviewAvailability

    data object Unavailable : LearnedItemsReviewAvailability
}

data class LearnEntryReviewAvailability(
    val latestCompletedNewItems: LatestCompletedNewItemsAvailability,
    val learnedItems: LearnedItemsReviewAvailability,
    val difficultItems: DifficultItemsReviewAvailability = DifficultItemsReviewAvailability.Unavailable
) {
    val latestCompletedSession: LatestCompletedNewItemsAvailability
        get() = latestCompletedNewItems
}

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
        now: Moment,
        reviewItemLimit: Int? = null
    ): LearnEntryReviewAvailability {
        val scopedItems = resolveScopedItems(scope) ?: emptyList()
        val latestSelection = latestCompletedNewItems(scope, scopedItems)
        val latest = latestSelection
            ?.takeIf { it.second.isNotEmpty() }
            ?.let { LatestCompletedNewItemsAvailability.Available(it.first.id, it.second.size) }
            ?: LatestCompletedNewItemsAvailability.Unavailable

        val difficult = difficultItems(scope.learnerId, scopedItems, now)
        val difficultSessionCount = reviewItemLimit?.let { difficult.size.coerceAtMost(it.coerceAtLeast(0)) }
            ?: difficult.size
        val difficultAvailability =
            if (difficultSessionCount > 0) {
                DifficultItemsReviewAvailability.Available(difficult.size, difficultSessionCount)
            } else {
                DifficultItemsReviewAvailability.Unavailable
            }

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
        return LearnEntryReviewAvailability(latest, learnedAvailability, difficultAvailability)
    }

    internal fun latestCompletedNewItems(
        scope: LearnEntryScope,
        scopedItems: List<LearningItem>
    ): Pair<StudySession, List<LearningItem>>? {
        val latestSession = sessions.findAll()
            .asSequence()
            .filter { it.matchesFinishedScope(scope) }
            .sortedWith(
                compareByDescending<StudySession> { it.finishedAt?.epochMillis ?: Long.MIN_VALUE }
                    .thenByDescending { it.startedAt.epochMillis }
                    .thenByDescending { it.id.value }
            )
            .firstOrNull()
            ?: return null
        val queue = queues.get(latestSession.id) ?: return latestSession to emptyList()
        val suspendedIds = memoryStates?.findAll(scope.learnerId).orEmpty()
            .filter { it.stage == LearningStage.SUSPENDED }
            .mapTo(hashSetOf()) { it.learningItemId }
        val scopedById = scopedItems
            .filter { it.isEnabled && it.id !in suspendedIds }
            .associateBy { it.id }
        val selected = queue.learningItemIds.asSequence()
            .filter { queue.itemOrigins[it] == SessionItemOrigin.NEW }
            .filter { it in queue.completedLearningItemIds && it in latestSession.reviewedItemIds }
            .mapNotNull(scopedById::get)
            .distinctBy { it.contentId }
            .toList()
        return latestSession to selected
    }

    internal fun difficultItems(
        learnerId: LearnerId,
        scopedItems: List<LearningItem>,
        now: Moment
    ): List<LearningItem> {
        val states = memoryStates?.findAll(learnerId).orEmpty().associateBy { it.learningItemId }
        val scopedById = scopedItems.associateBy { it.id }
        val latestByContent = linkedMapOf<ContentId, vn.loi.learning.domain.study.memory.model.ReviewEvent>()
        reviewEvents.findAll(learnerId).forEach { event ->
            scopedById[event.learningItemId]?.let { latestByContent[it.contentId] = event }
        }
        return latestByContent.entries.asSequence()
            .filter { it.value.rating == ReviewRating.AGAIN || it.value.rating == ReviewRating.HARD }
            .mapNotNull { (contentId, event) ->
                val representative = scopedItems.asSequence()
                    .filter { it.contentId == contentId && it.isEnabled }
                    .filter { states[it.id]?.stage != LearningStage.SUSPENDED }
                    .sortedBy { it.id.value }
                    .firstOrNull() ?: return@mapNotNull null
                DifficultSelection(representative, event.rating, event.reviewedAt, states[representative.id])
            }
            .sortedWith(
                compareBy<DifficultSelection>(
                    { if (it.rating == ReviewRating.AGAIN) 0 else 1 },
                    { it.state?.isDue(now) != true },
                    { it.reviewedAt.epochMillis },
                    { it.item.id.value }
                )
            )
            .map { it.item }
            .toList()
    }

    private data class DifficultSelection(
        val item: LearningItem,
        val rating: ReviewRating,
        val reviewedAt: Moment,
        val state: MemoryState?
    )

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

data class StartLatestCompletedNewItemsReviewRequest(
    val scope: LearnEntryScope,
    val requestedAt: Moment
)

sealed interface StartLatestCompletedNewItemsReviewResult {
    data class Accepted(val session: StudySession, val queue: StudyQueueSnapshot) :
        StartLatestCompletedNewItemsReviewResult
    data object NoItems : StartLatestCompletedNewItemsReviewResult
    data class Rejected(val reason: StartFocusedReviewRejection) :
        StartLatestCompletedNewItemsReviewResult
}

data class StartDifficultItemsReviewRequest(
    val scope: LearnEntryScope,
    val requestedAt: Moment,
    val reviewItemLimit: Int
)

sealed interface StartDifficultItemsReviewResult {
    data class Accepted(val session: StudySession, val queue: StudyQueueSnapshot) :
        StartDifficultItemsReviewResult
    data object NoItems : StartDifficultItemsReviewResult
    data class Rejected(val reason: StartFocusedReviewRejection) : StartDifficultItemsReviewResult
}

enum class StartFocusedReviewRejection { INVALID_SCOPE, OTHER_ACTIVE_SESSION_EXISTS }

class StartLatestCompletedNewItemsReviewUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val availability: LearnEntryReviewAvailabilityQuery
) {
    fun execute(request: StartLatestCompletedNewItemsReviewRequest): StartLatestCompletedNewItemsReviewResult {
        if (sessions.findActiveByLearner(request.scope.learnerId) != null) {
            return StartLatestCompletedNewItemsReviewResult.Rejected(
                StartFocusedReviewRejection.OTHER_ACTIVE_SESSION_EXISTS
            )
        }
        val scoped = availability.resolveScopedItems(request.scope)
            ?: return StartLatestCompletedNewItemsReviewResult.Rejected(StartFocusedReviewRejection.INVALID_SCOPE)
        val selected = availability.latestCompletedNewItems(request.scope, scoped)?.second.orEmpty()
        if (selected.isEmpty()) return StartLatestCompletedNewItemsReviewResult.NoItems
        val accepted = createFocusedSession(request.scope, request.requestedAt, selected, sessions, queues)
        return StartLatestCompletedNewItemsReviewResult.Accepted(accepted.first, accepted.second)
    }
}

class StartDifficultItemsReviewUseCase(
    private val sessions: StudySessionRepository,
    private val queues: StudyQueueService,
    private val availability: LearnEntryReviewAvailabilityQuery
) {
    fun execute(request: StartDifficultItemsReviewRequest): StartDifficultItemsReviewResult {
        if (sessions.findActiveByLearner(request.scope.learnerId) != null) {
            return StartDifficultItemsReviewResult.Rejected(StartFocusedReviewRejection.OTHER_ACTIVE_SESSION_EXISTS)
        }
        val scoped = availability.resolveScopedItems(request.scope)
            ?: return StartDifficultItemsReviewResult.Rejected(StartFocusedReviewRejection.INVALID_SCOPE)
        val selected = availability.difficultItems(request.scope.learnerId, scoped, request.requestedAt)
            .take(request.reviewItemLimit.coerceAtLeast(0))
        if (selected.isEmpty()) return StartDifficultItemsReviewResult.NoItems
        val accepted = createFocusedSession(request.scope, request.requestedAt, selected, sessions, queues)
        return StartDifficultItemsReviewResult.Accepted(accepted.first, accepted.second)
    }
}

private fun createFocusedSession(
    scope: LearnEntryScope,
    requestedAt: Moment,
    selected: List<LearningItem>,
    sessions: StudySessionRepository,
    queues: StudyQueueService
): Pair<StudySession, StudyQueueSnapshot> {
    val sessionId = SessionId(UUID.randomUUID().toString())
    val session = StudySession.start(
        id = sessionId,
        learnerId = scope.learnerId,
        startedAt = requestedAt,
        policy = SessionPolicy(0, selected.size, allowRepeatInSameSession = true),
        includedContentIds = scope.includedContentIds,
        topicId = scope.topicId,
        installedPackageId = scope.installedPackageId
    )
    sessions.save(session)
    val queue = try {
        queues.create(
            sessionId = sessionId,
            createdAt = requestedAt,
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
    return session to queue
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
