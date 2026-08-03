package vn.loi.learning.application.session

import vn.loi.learning.domain.study.learning.model.LearningItemId
import vn.loi.learning.domain.study.memory.model.ReviewRating

data class CoverageReinforcementState(
    val reinforcementCount: Int = 0,
    val previousGap: Int? = null,
    val lastInsertionIndex: Int? = null,
    val deferred: Boolean = false,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    init {
        require(reinforcementCount >= 0)
        require(previousGap == null || previousGap > 0)
        require(lastInsertionIndex == null || lastInsertionIndex >= 0)
        require(schemaVersion == CURRENT_SCHEMA_VERSION)
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class CoverageReinforcementUndo(
    val learningItemId: LearningItemId,
    val previousState: CoverageReinforcementState?,
    val discardedTail: List<LearningItemId> = emptyList(),
    val completionTruncation: Boolean = false
)

data class CoverageReinforcementRequest(
    val rating: ReviewRating,
    val state: CoverageReinforcementState,
    val currentIndex: Int,
    val queueSize: Int
) {
    init {
        require(currentIndex in 0 until queueSize)
    }
}

sealed interface CoverageReinforcementDecision {
    data class Schedule(val gap: Int, val insertionIndex: Int) : CoverageReinforcementDecision {
        init {
            require(gap > 0)
            require(insertionIndex >= 0)
        }
    }

    data class Defer(val requiredGap: Int) : CoverageReinforcementDecision
    data object NotRequired : CoverageReinforcementDecision
    data object LimitReached : CoverageReinforcementDecision
}

fun interface CoverageReinforcementPolicy {
    fun decide(request: CoverageReinforcementRequest): CoverageReinforcementDecision

    companion object {
        val DEFAULT = CoverageReinforcementPolicy { request ->
            val gaps = when (request.rating) {
                ReviewRating.AGAIN -> listOf(2, 8, 20, 40)
                ReviewRating.HARD -> listOf(4, 12, 30)
                ReviewRating.GOOD, ReviewRating.EASY ->
                    return@CoverageReinforcementPolicy CoverageReinforcementDecision.NotRequired
            }
            val configuredGap = gaps.getOrNull(request.state.reinforcementCount)
                ?: return@CoverageReinforcementPolicy CoverageReinforcementDecision.LimitReached
            val gap = maxOf(configuredGap, (request.state.previousGap ?: 0) + 1)
            val insertionIndex = request.currentIndex + gap
            if (insertionIndex > request.queueSize) {
                CoverageReinforcementDecision.Defer(gap)
            } else {
                CoverageReinforcementDecision.Schedule(gap, insertionIndex)
            }
        }
    }
}
