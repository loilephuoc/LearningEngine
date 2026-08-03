package vn.loi.learning.application.session

import vn.loi.learning.domain.content.model.ContentId
import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.Moment
import vn.loi.learning.domain.study.session.model.SessionId
import vn.loi.learning.domain.study.session.model.SessionItemOrigin
import vn.loi.learning.application.study.StudyQueueUnderfillReason
import vn.loi.learning.domain.study.memory.model.ReviewRating
import vn.loi.learning.domain.study.session.model.PracticeLoopPolicy

/**
 * Snapshot bất biến của thứ tự LearningItem trong một phiên học.
 *
 * Snapshot chỉ giữ:
 * - danh tính session;
 * - thời điểm tạo queue;
 * - thứ tự LearningItem;
 * - vị trí hiện tại.
 *
 * Snapshot không chứa:
 * - Content;
 * - MemoryState;
 * - dữ liệu scheduling;
 * - business rule lựa chọn item.
 *
 * Mọi thuộc tính điều hướng và tiến độ đều được suy ra từ
 * learningItemIds và currentIndex. Vì vậy các thuộc tính này
 * không cần được persist riêng.
 *
 * Mỗi thao tác chuyển queue đều trả về một snapshot mới.
 */
data class StudyQueueSnapshot(
    val sessionId: SessionId,
    val createdAt: Moment,
    val learningItemIds: List<LearningItemId>,
    val currentIndex: Int = 0,
    val itemOrigins: Map<LearningItemId, SessionItemOrigin> = emptyMap(),
    val itemContentIds: Map<LearningItemId, ContentId> = emptyMap(),
    val configuredNewTarget: Int = 0,
    val effectiveNewWorkload: Int = 0,
    val configuredReviewTarget: Int = 0,
    val effectiveReviewWorkload: Int = 0,
    val fixedPracticeMembership: List<LearningItemId> = emptyList(),
    val practiceSeed: Long? = null,
    val practiceRound: Int = 0,
    val practiceLoopPolicy: PracticeLoopPolicy = PracticeLoopPolicy.NONE,
    val practiceReinforcementStates: Map<LearningItemId, PracticeReinforcementState> = emptyMap(),
    val practiceMembershipUndo: PracticeMembershipUndo? = null,
    val coverageReinforcementStates: Map<LearningItemId, CoverageReinforcementState> = emptyMap(),
    val coverageReinforcementUndo: CoverageReinforcementUndo? = null
) {

    init {
        require(itemOrigins.keys.all { it in learningItemIds }) {
            "Study queue origins must reference queue LearningItemIds."
        }
        require(itemContentIds.keys.all { it in learningItemIds }) {
            "Study queue content identities must reference queue LearningItemIds."
        }
        require(effectiveNewWorkload in 0..configuredNewTarget)
        require(effectiveReviewWorkload in 0..configuredReviewTarget)

        require(currentIndex >= 0) {
            "Study queue current index must not be negative."
        }

        require(
            currentIndex <= learningItemIds.size
        ) {
            "Study queue current index must not exceed queue size."
        }
        require(fixedPracticeMembership.distinct().size == fixedPracticeMembership.size)
        require(
            fixedPracticeMembership.isEmpty() ||
                if (practiceLoopPolicy == PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP) {
                    fixedPracticeMembership.all { it in learningItemIds }
                } else fixedPracticeMembership.toSet() == learningItemIds.toSet()
        )
        val completedDynamicMembership = fixedPracticeMembership.isEmpty() &&
            practiceLoopPolicy == PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP
        require((practiceSeed == null) == (fixedPracticeMembership.isEmpty() && !completedDynamicMembership))
        if (fixedPracticeMembership.isEmpty() && !completedDynamicMembership) {
            require(practiceRound == 0)
            require(practiceLoopPolicy == PracticeLoopPolicy.NONE)
        } else {
            require(practiceRound > 0)
            require(practiceLoopPolicy != PracticeLoopPolicy.NONE)
        }
        require(practiceReinforcementStates.keys.all { it in fixedPracticeMembership })
        require(coverageReinforcementStates.keys.all { it in learningItemIds })
        require(coverageReinforcementUndo?.learningItemId?.let { it in learningItemIds } != false)
    }

    /**
     * Tổng số item ban đầu trong queue.
     */
    val totalItemCount: Int
        get() =
            learningItemIds.size

    /**
     * Số item đã đi qua.
     *
     * currentIndex cũng chính là số lượng item đã hoàn tất.
     */
    val completedItemCount: Int
        get() =
            currentIndex

    /**
     * Số item chưa hoàn tất, bao gồm item hiện tại.
     */
    val remainingItemCount: Int
        get() =
            totalItemCount -
                    completedItemCount

    /**
     * Queue không chứa item nào.
     */
    val isEmpty: Boolean
        get() =
            learningItemIds.isEmpty()

    val newUnderfillReason: StudyQueueUnderfillReason
        get() = if (effectiveNewWorkload < configuredNewTarget) {
            StudyQueueUnderfillReason.ELIGIBLE_INVENTORY_EXHAUSTED
        } else {
            StudyQueueUnderfillReason.NONE
        }

    val reviewUnderfillReason: StudyQueueUnderfillReason
        get() = if (effectiveReviewWorkload < configuredReviewTarget) {
            StudyQueueUnderfillReason.ELIGIBLE_INVENTORY_EXHAUSTED
        } else {
            StudyQueueUnderfillReason.NONE
        }

    /**
     * Queue đã đi hết toàn bộ item.
     *
     * Empty queue cũng được xem là completed.
     */
    val isCompleted: Boolean
        get() =
            currentIndex >=
                    learningItemIds.size

    /**
     * Queue đang ở vị trí đầu tiên.
     *
     * Empty queue cũng có currentIndex bằng 0, nên thuộc tính này
     * chỉ true khi queue thực sự có item.
     */
    val isAtStart: Boolean
        get() =
            !isEmpty &&
                    currentIndex == 0

    /**
     * Item hiện tại của queue.
     *
     * Trả về null khi queue đã completed hoặc queue rỗng.
     */
    val currentLearningItemId:
            LearningItemId?
        get() =
            learningItemIds
                .getOrNull(
                    currentIndex
                )

    val currentItemOrigin: SessionItemOrigin?
        get() = currentLearningItemId?.let(itemOrigins::get)

    fun originOf(learningItemId: LearningItemId): SessionItemOrigin? =
        itemOrigins[learningItemId]

    val currentContentId: ContentId?
        get() = currentLearningItemId?.let(itemContentIds::get)

    fun contentIdOf(learningItemId: LearningItemId): ContentId? =
        itemContentIds[learningItemId]

    /**
     * Item ngay trước vị trí hiện tại.
     *
     * Sau khi queue completed, đây là item cuối cùng đã hoàn tất.
     */
    val previousLearningItemId:
            LearningItemId?
        get() =
            learningItemIds
                .getOrNull(
                    currentIndex - 1
                )

    /**
     * Item ngay sau item hiện tại.
     *
     * Trả về null khi:
     * - queue rỗng;
     * - queue completed;
     * - item hiện tại là item cuối cùng.
     */
    val nextLearningItemId:
            LearningItemId?
        get() =
            peekNext(offset = 1)

    /**
     * True khi item hiện tại là item cuối cùng chưa hoàn tất.
     */
    val isLastItem: Boolean
        get() =
            !isCompleted &&
                    currentIndex ==
                    learningItemIds.lastIndex

    /**
     * Danh sách item đã hoàn tất.
     *
     * Thứ tự ban đầu của queue được giữ nguyên.
     */
    val completedLearningItemIds:
            List<LearningItemId>
        get() =
            learningItemIds
                .take(
                    completedItemCount
                )

    /**
     * Danh sách item chưa hoàn tất, bao gồm item hiện tại.
     *
     * Khi queue completed, danh sách này rỗng.
     */
    val remainingLearningItemIds:
            List<LearningItemId>
        get() =
            learningItemIds
                .drop(
                    currentIndex
                )

    /**
     * Danh sách item đứng sau item hiện tại.
     *
     * Không bao gồm item hiện tại.
     */
    val pendingLearningItemIds:
            List<LearningItemId>
        get() =
            if (isCompleted) {
                emptyList()
            } else {
                learningItemIds
                    .drop(
                        currentIndex + 1
                    )
            }

    /**
     * Tiến độ trong khoảng 0.0 đến 1.0.
     *
     * Empty queue được xem là hoàn tất nên có progress bằng 1.0.
     */
    val progress: Double
        get() =
            if (isEmpty) {
                1.0
            } else {
                completedItemCount
                    .toDouble() /
                        totalItemCount
                            .toDouble()
            }

    /**
     * Tiến độ phần trăm nguyên trong khoảng 0 đến 100.
     *
     * Phép chia dùng completedItemCount trước để tránh sai số
     * khiến queue completed trả về nhỏ hơn 100.
     */
    val percentComplete: Int
        get() =
            if (isEmpty) {
                100
            } else {
                completedItemCount *
                        100 /
                        totalItemCount
            }

    /**
     * Đọc item theo khoảng cách tương đối từ item hiện tại.
     *
     * offset = 0:
     * - trả về item hiện tại.
     *
     * offset = 1:
     * - trả về item kế tiếp.
     *
     * offset lớn hơn số item còn lại:
     * - trả về null.
     *
     * Không hỗ trợ offset âm vì việc đọc item trước đó đã có
     * previousLearningItemId và completedLearningItemIds.
     */
    fun peekNext(
        offset: Int = 1
    ): LearningItemId? {
        require(offset >= 0) {
            "Study queue peek offset must not be negative."
        }

        return learningItemIds
            .getOrNull(
                currentIndex + offset
            )
    }

    /**
     * Chuyển queue sang item kế tiếp.
     */
    fun advance(): StudyQueueSnapshot {
        require(!isCompleted) {
            "Cannot advance a completed study queue."
        }

        return copy(
            currentIndex =
                currentIndex + 1
        )
    }

    val practiceProgress: PracticeProgress?
        get() = if (fixedPracticeMembership.isEmpty()) null else PracticeProgress(
            round = practiceRound,
            position = minOf(currentIndex + 1, fixedPracticeMembership.size),
            membershipSize = fixedPracticeMembership.size
        )

    fun advancePractice(
        result: PracticeRecallResult = PracticeRecallResult.CORRECT,
        policy: PracticeReinforcementPolicy = PracticeReinforcementPolicy.DEFAULT
    ): StudyQueueSnapshot {
        require(practiceLoopPolicy != PracticeLoopPolicy.NONE) { "Queue is not a practice loop." }
        if (practiceLoopPolicy == PracticeLoopPolicy.LOOP_ADAPTIVE_FEEDBACK_SHUFFLED) {
            val current = requireNotNull(currentLearningItemId)
            val previous = practiceReinforcementStates[current] ?: PracticeReinforcementState()
            val decision = policy.decide(PracticeRecallFeedbackMapper.map(result), previous)
            if (decision != null) {
                val insertionIndex = minOf(currentIndex + decision.gap + 1, learningItemIds.size)
                val scheduled = learningItemIds.toMutableList().apply { add(insertionIndex, current) }
                return copy(
                    learningItemIds = scheduled,
                    currentIndex = currentIndex + 1,
                    practiceReinforcementStates = practiceReinforcementStates +
                        (current to decision.nextState.copy(lastInsertionIndex = insertionIndex))
                )
            }
        }
        if (practiceLoopPolicy == PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP &&
            fixedPracticeMembership.isEmpty()) {
            val completed = learningItemIds.take(currentIndex + 1)
            return copy(learningItemIds = completed, currentIndex = completed.size, practiceRound = 0,
                practiceLoopPolicy = PracticeLoopPolicy.NONE, practiceSeed = null,
                itemOrigins = itemOrigins.filterKeys { it in completed },
                itemContentIds = itemContentIds.filterKeys { it in completed },
                practiceMembershipUndo = null)
        }
        if (!isLastItem) return copy(currentIndex = currentIndex + 1)
        val nextRound = practiceRound + 1
        return copy(
            learningItemIds = PracticeRoundShuffler.shuffle(
                fixedPracticeMembership,
                requireNotNull(practiceSeed),
                nextRound,
                previousLast = currentLearningItemId,
                previousOrder = learningItemIds
            ),
            currentIndex = 0,
            practiceRound = nextRound,
            itemOrigins = if (practiceLoopPolicy == PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP) {
                itemOrigins.filterKeys { it in fixedPracticeMembership }
            } else itemOrigins,
            itemContentIds = if (practiceLoopPolicy == PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP) {
                itemContentIds.filterKeys { it in fixedPracticeMembership }
            } else itemContentIds,
            practiceMembershipUndo = null
        )
    }

    fun updateDifficultMembership(learningItemId: LearningItemId, rating: ReviewRating): StudyQueueSnapshot {
        require(practiceLoopPolicy == PracticeLoopPolicy.LOOP_DYNAMIC_DIFFICULT_MEMBERSHIP)
        require(currentLearningItemId == learningItemId)
        val eligible = rating == ReviewRating.AGAIN || rating == ReviewRating.HARD
        val nextMembership = if (eligible) {
            (fixedPracticeMembership + learningItemId).distinct()
        } else {
            fixedPracticeMembership - learningItemId
        }
        val pendingStart = currentIndex + 1
        val nextQueue = if (eligible) learningItemIds else
            learningItemIds.take(pendingStart) + learningItemIds.drop(pendingStart).filterNot { it == learningItemId }
        return copy(
            learningItemIds = nextQueue,
            fixedPracticeMembership = nextMembership,
            practiceMembershipUndo = PracticeMembershipUndo(
                learningItemId, fixedPracticeMembership, learningItemIds, currentIndex, practiceRound
            )
        )
    }

    fun restorePracticeMembershipUndo(learningItemId: LearningItemId): StudyQueueSnapshot {
        val undo = practiceMembershipUndo ?: return this
        require(undo.learningItemId == learningItemId)
        return copy(
            learningItemIds = undo.previousQueue,
            currentIndex = undo.previousIndex,
            fixedPracticeMembership = undo.previousMembership,
            practiceRound = undo.previousRound,
            practiceMembershipUndo = null
        )
    }

    /**
     * Advances a unique-coverage review pass and schedules one session-local reinforcement
     * occurrence without changing the coverage target. Once all target Content has been covered,
     * pending reinforcement occurrences are discarded and the pass completes immediately.
     */
    fun advanceCoverageReview(
        rating: ReviewRating,
        uniqueCoverageComplete: Boolean,
        policy: CoverageReinforcementPolicy = CoverageReinforcementPolicy.DEFAULT
    ): StudyQueueSnapshot {
        require(!isCompleted) { "Cannot advance a completed study queue." }
        if (uniqueCoverageComplete) {
            val current = requireNotNull(currentLearningItemId)
            val completedAttempts = learningItemIds.take(currentIndex + 1)
            return copy(
                learningItemIds = completedAttempts,
                currentIndex = completedAttempts.size,
                coverageReinforcementUndo = CoverageReinforcementUndo(
                    current,
                    coverageReinforcementStates[current],
                    learningItemIds.drop(currentIndex + 1),
                    completionTruncation = true
                )
            )
        }

        val current = requireNotNull(currentLearningItemId)
        val previousState = coverageReinforcementStates[current]
        val state = previousState ?: CoverageReinforcementState()
        val decision = policy.decide(
            CoverageReinforcementRequest(rating, state, currentIndex, learningItemIds.size)
        )
        val scheduled = learningItemIds.toMutableList()
        val nextState = when (decision) {
            is CoverageReinforcementDecision.Schedule -> {
                scheduled.add(decision.insertionIndex, current)
                state.copy(
                    reinforcementCount = state.reinforcementCount + 1,
                    previousGap = decision.gap,
                    lastInsertionIndex = decision.insertionIndex,
                    deferred = false
                )
            }
            is CoverageReinforcementDecision.Defer -> state.copy(deferred = true)
            CoverageReinforcementDecision.LimitReached,
            CoverageReinforcementDecision.NotRequired -> state.copy(deferred = false)
        }
        val nextStates =
            if (decision == CoverageReinforcementDecision.NotRequired && previousState == null) {
                coverageReinforcementStates
            } else {
                coverageReinforcementStates + (current to nextState)
            }
        return copy(
            learningItemIds = scheduled,
            currentIndex = currentIndex + 1,
            coverageReinforcementStates = nextStates,
            coverageReinforcementUndo = CoverageReinforcementUndo(current, previousState)
        )
    }

    fun rewind(expectedLearningItemId: LearningItemId): StudyQueueSnapshot {
        require(currentIndex > 0) { "Cannot rewind a study queue at its start." }
        require(previousLearningItemId == expectedLearningItemId) {
            "Only the latest completed study queue item can be restored."
        }
        return copy(currentIndex = currentIndex - 1)
    }

    fun rewindCoverageReview(expectedLearningItemId: LearningItemId): StudyQueueSnapshot {
        val rewound = rewind(expectedLearningItemId)
        val checkpoint = coverageReinforcementUndo ?: return rewound.removePendingCoverageRetry(
            expectedLearningItemId
        )
        require(checkpoint.learningItemId == expectedLearningItemId) {
            "Coverage reinforcement Undo checkpoint does not match the latest reviewed item."
        }
        val pendingRetryIndex =
            rewound.learningItemIds.indexOfFirstFrom(
                startIndex = rewound.currentIndex + 1,
                expected = expectedLearningItemId
            )
        val restoredStates = if (checkpoint.previousState == null) {
            rewound.coverageReinforcementStates - expectedLearningItemId
        } else {
            rewound.coverageReinforcementStates + (expectedLearningItemId to checkpoint.previousState)
        }
        if (checkpoint.completionTruncation) {
            return rewound.copy(
                learningItemIds = rewound.learningItemIds + checkpoint.discardedTail,
                coverageReinforcementStates = restoredStates,
                coverageReinforcementUndo = null
            )
        }
        return rewound.copy(
            learningItemIds =
                if (pendingRetryIndex < 0) rewound.learningItemIds else
                    rewound.learningItemIds.toMutableList().apply { removeAt(pendingRetryIndex) },
            coverageReinforcementStates = restoredStates,
            coverageReinforcementUndo = null
        )
    }

    companion object {

        fun create(
            sessionId: SessionId,
            createdAt: Moment,
            learningItemIds:
            List<LearningItemId>,
            itemOrigins: Map<LearningItemId, SessionItemOrigin> = emptyMap(),
            itemContentIds: Map<LearningItemId, ContentId> = emptyMap(),
            configuredNewTarget: Int = 0,
            effectiveNewWorkload: Int = 0,
            configuredReviewTarget: Int = 0,
            effectiveReviewWorkload: Int = 0,
            practiceSeed: Long? = null,
            practiceLoopPolicy: PracticeLoopPolicy = if (practiceSeed == null) PracticeLoopPolicy.NONE
                else PracticeLoopPolicy.LOOP_FIXED_MEMBERSHIP_SHUFFLED
        ): StudyQueueSnapshot {
            require(learningItemIds.distinct().size == learningItemIds.size) {
                "A newly planned Study queue must not contain duplicate LearningItemIds."
            }
            val fixedMembership = if (practiceSeed == null) emptyList() else learningItemIds.toList()
            val initialOrder = if (practiceSeed == null) learningItemIds.toList() else
                PracticeRoundShuffler.shuffle(fixedMembership, practiceSeed, 1)
            return StudyQueueSnapshot(
                sessionId = sessionId,
                createdAt = createdAt,
                learningItemIds =
                    initialOrder,
                currentIndex = 0,
                itemOrigins = itemOrigins.toMap(),
                itemContentIds = itemContentIds.toMap(),
                configuredNewTarget = configuredNewTarget,
                effectiveNewWorkload = effectiveNewWorkload,
                configuredReviewTarget = configuredReviewTarget,
                effectiveReviewWorkload = effectiveReviewWorkload,
                fixedPracticeMembership = fixedMembership,
                practiceSeed = practiceSeed,
                practiceRound = if (practiceSeed == null) 0 else 1,
                practiceLoopPolicy = practiceLoopPolicy
            )
        }
    }
}

private fun StudyQueueSnapshot.removePendingCoverageRetry(
    expectedLearningItemId: LearningItemId
): StudyQueueSnapshot {
    val pendingRetryIndex = learningItemIds.indexOfFirstFrom(
        startIndex = currentIndex + 1,
        expected = expectedLearningItemId
    )
    if (pendingRetryIndex < 0) return this
    return copy(
        learningItemIds = learningItemIds.toMutableList().apply { removeAt(pendingRetryIndex) }
    )
}

private fun List<LearningItemId>.indexOfFirstFrom(
    startIndex: Int,
    expected: LearningItemId
): Int {
    for (index in startIndex until size) {
        if (this[index] == expected) return index
    }
    return -1
}
