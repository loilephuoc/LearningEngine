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
import vn.loi.learning.domain.study.session.model.SessionEvaluationPolicy
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy

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
    data class Available(val itemCount: Int) :
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
    private data class CachedLearnedSelection(
        val scope: LearnEntryScope,
        val capturedAtMillis: Long,
        val items: List<LearningItem>
    )

    private var cachedLearnedSelection: CachedLearnedSelection? = null

    private companion object {
        const val LEARNED_SELECTION_CACHE_TTL_MILLIS = 120_000L
    }
    fun execute(
        scope: LearnEntryScope,
        now: Moment
    ): LearnEntryReviewAvailability {
        val scopedItems = resolveScopedItems(scope) ?: emptyList()

        // Availability used to scan the complete memory-state store three times
        // and the review-event store twice (latest/difficult/learned).  Read each
        // source once and share the snapshots across all three calculations.
        val stateSnapshot = memoryStates?.findAll(scope.learnerId).orEmpty()
        val eventSnapshot = reviewEvents.findAll(scope.learnerId)

        val latestSelection = latestCompletedNewItems(scope, scopedItems, stateSnapshot)
        val latest = latestSelection
            ?.takeIf { it.second.isNotEmpty() }
            ?.let { LatestCompletedNewItemsAvailability.Available(it.first.id, it.second.size) }
            ?: LatestCompletedNewItemsAvailability.Unavailable

        val difficult = difficultItems(scope.learnerId, scopedItems, now, stateSnapshot, eventSnapshot)
        val difficultAvailability =
            if (difficult.isNotEmpty()) {
                DifficultItemsReviewAvailability.Available(difficult.size)
            } else {
                DifficultItemsReviewAvailability.Unavailable
            }

        val learned = learnedItems(scope.learnerId, scopedItems, now, stateSnapshot, eventSnapshot)
        cachedLearnedSelection = CachedLearnedSelection(scope, now.epochMillis, learned)
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
    ): Pair<StudySession, List<LearningItem>>? =
        latestCompletedNewItems(
            scope,
            scopedItems,
            memoryStates?.findAll(scope.learnerId).orEmpty()
        )

    private fun latestCompletedNewItems(
        scope: LearnEntryScope,
        scopedItems: List<LearningItem>,
        stateSnapshot: List<MemoryState>
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
        val suspendedIds = stateSnapshot
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
    ): List<LearningItem> =
        difficultItems(
            learnerId,
            scopedItems,
            now,
            memoryStates?.findAll(learnerId).orEmpty(),
            reviewEvents.findAll(learnerId)
        )

    private fun difficultItems(
        learnerId: LearnerId,
        scopedItems: List<LearningItem>,
        now: Moment,
        stateSnapshot: List<MemoryState>,
        eventSnapshot: List<vn.loi.learning.domain.study.memory.model.ReviewEvent>
    ): List<LearningItem> {
        val states = stateSnapshot.associateBy { it.learningItemId }
        val scopedById = scopedItems.associateBy { it.id }
        val latestByContent = linkedMapOf<ContentId, vn.loi.learning.domain.study.memory.model.ReviewEvent>()
        eventSnapshot.forEach { event ->
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
    ): List<LearningItem> =
        learnedItems(
            learnerId,
            scopedItems,
            now,
            memoryStates?.findAll(learnerId).orEmpty(),
            reviewEvents.findAll(learnerId)
        )

    private fun learnedItems(
        learnerId: LearnerId,
        scopedItems: List<LearningItem>,
        now: Moment,
        stateSnapshot: List<MemoryState>,
        eventSnapshot: List<vn.loi.learning.domain.study.memory.model.ReviewEvent>
    ): List<LearningItem> {
        val states = stateSnapshot
            .filter { it.reviewCount > 0 && it.lastReviewedAt != null }
            .associateBy { it.learningItemId }
        val events = eventSnapshot
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

    internal fun learnedItemsForStart(
        scope: LearnEntryScope,
        now: Moment
    ): List<LearningItem>? {
        cachedLearnedSelection
            ?.takeIf { cached ->
                cached.scope == scope &&
                        now.epochMillis >= cached.capturedAtMillis &&
                        now.epochMillis - cached.capturedAtMillis <= LEARNED_SELECTION_CACHE_TTL_MILLIS
            }
            ?.let { return it.items }

        val scopedItems = resolveScopedItems(scope) ?: return null
        val learned = learnedItems(scope.learnerId, scopedItems, now)
        cachedLearnedSelection = CachedLearnedSelection(scope, now.epochMillis, learned)
        return learned
    }

    internal fun resolveScopedItems(scope: LearnEntryScope): List<LearningItem>? {
        val contentIds =
            if (scope.includedContentIds.isNotEmpty()) {
                scope.includedContentIds
            } else {
                val query = packageContentQuerySupplier?.invoke() ?: return null
                try {
                    query.getContentIdsForPackage(scope.installedPackageId)
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
    val requestedAt: Moment
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
        val accepted = createFocusedPracticeSession(
            request.scope, request.requestedAt, selected, SessionItemOrigin.NEW,
            PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED,
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.LATEST_SESSION,
            sessions, queues
        )
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
        if (selected.isEmpty()) return StartDifficultItemsReviewResult.NoItems
        val accepted = createFocusedPracticeSession(
            request.scope, request.requestedAt, selected, SessionItemOrigin.REVIEW,
            PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP,
            vn.loi.learning.domain.study.session.model.FocusedPracticeKind.DIFFICULT,
            sessions, queues
        )
        return StartDifficultItemsReviewResult.Accepted(accepted.first, accepted.second)
    }
}

private fun createFocusedPracticeSession(
    scope: LearnEntryScope,
    requestedAt: Moment,
    selected: List<LearningItem>,
    origin: SessionItemOrigin,
    practiceLoopPolicy: PracticeLoopPolicy,
    focusedPracticeKind: vn.loi.learning.domain.study.session.model.FocusedPracticeKind,
    sessions: StudySessionRepository,
    queues: StudyQueueService
): Pair<StudySession, StudyQueueSnapshot> {
    val uuid = UUID.randomUUID()
    val sessionId = SessionId(uuid.toString())
    val session = StudySession.start(
        id = sessionId,
        learnerId = scope.learnerId,
        startedAt = requestedAt,
        policy = SessionPolicy(
            0,
            selected.size,
            allowRepeatInSameSession = true,
            evaluationPolicy = SessionEvaluationPolicy.PRACTICE_ONLY,
            practiceLoopPolicy = practiceLoopPolicy,
            focusedPracticeKind = focusedPracticeKind
        ),
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
            itemOrigins = selected.associate { it.id to origin },
            itemContentIds = selected.associate { it.id to it.contentId },
            configuredReviewTarget = selected.size,
            effectiveReviewWorkload = selected.map { it.contentId }.distinct().size,
            practiceSeed = uuid.mostSignificantBits xor uuid.leastSignificantBits,
            practiceLoopPolicy = practiceLoopPolicy
        )
    } catch (failure: RuntimeException) {
        sessions.deleteById(sessionId)
        throw failure
    }
    return session to queue
}

data class StartLearnedItemsReviewRequest(
    val scope: LearnEntryScope,
    val requestedAt: Moment,
    val practiceLoopPolicy: PracticeLoopPolicy = PracticeLoopPolicy.NONE
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
        // The chooser already calculated this exact learned-item selection.
        // Reuse that short-lived snapshot instead of rescanning package content,
        // memory states and the full review-event history after the click.
        val selected =
            availability.learnedItemsForStart(request.scope, request.requestedAt)
                ?: return StartLearnedItemsReviewResult.Rejected(
                    StartLearnedItemsReviewRejection.INVALID_SCOPE
                )
        if (selected.isEmpty()) return StartLearnedItemsReviewResult.NoItems

        if (request.practiceLoopPolicy != PracticeLoopPolicy.NONE) {
            val accepted = createFocusedPracticeSession(
                request.scope,
                request.requestedAt,
                selected,
                SessionItemOrigin.REVIEW,
                request.practiceLoopPolicy,
                vn.loi.learning.domain.study.session.model.FocusedPracticeKind.NONE,
                sessions,
                queues
            )
            return StartLearnedItemsReviewResult.Accepted(accepted.first, accepted.second)
        }

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
