package vn.loi.learning.application.session

data class PracticeReinforcementState(
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val previousGap: Int? = null,
    val lastInsertionIndex: Int? = null
) {
    init {
        require(againCount >= 0 && hardCount >= 0)
        require(previousGap == null || previousGap > 0)
        require(lastInsertionIndex == null || lastInsertionIndex >= 0)
    }
}

enum class PracticeFeedback { AGAIN_LIKE, HARD_LIKE, GOOD_LIKE }

object PracticeRecallFeedbackMapper {
    fun map(result: PracticeRecallResult): PracticeFeedback = when (result) {
        PracticeRecallResult.INCORRECT, PracticeRecallResult.REVEALED -> PracticeFeedback.AGAIN_LIKE
        PracticeRecallResult.ALMOST_CORRECT -> PracticeFeedback.HARD_LIKE
        PracticeRecallResult.CORRECT -> PracticeFeedback.GOOD_LIKE
    }
}

data class PracticeReinforcementDecision(val gap: Int, val nextState: PracticeReinforcementState)

class PracticeReinforcementPolicy(
    private val againGaps: List<Int> = listOf(2, 6, 15),
    private val hardGaps: List<Int> = listOf(4, 12)
) {
    init {
        require(againGaps.isNotEmpty() && hardGaps.isNotEmpty())
        require((againGaps + hardGaps).all { it > 0 })
        require(againGaps.zipWithNext().all { (a, b) -> b > a })
        require(hardGaps.zipWithNext().all { (a, b) -> b > a })
    }

    fun decide(feedback: PracticeFeedback, state: PracticeReinforcementState): PracticeReinforcementDecision? {
        val gaps = if (feedback == PracticeFeedback.AGAIN_LIKE) againGaps else hardGaps
        val count = if (feedback == PracticeFeedback.AGAIN_LIKE) state.againCount else state.hardCount
        if (feedback == PracticeFeedback.GOOD_LIKE || count >= gaps.size) return null
        val gap = gaps[count]
        return PracticeReinforcementDecision(
            gap,
            state.copy(
                againCount = state.againCount + if (feedback == PracticeFeedback.AGAIN_LIKE) 1 else 0,
                hardCount = state.hardCount + if (feedback == PracticeFeedback.HARD_LIKE) 1 else 0,
                previousGap = gap
            )
        )
    }

    companion object { val DEFAULT = PracticeReinforcementPolicy() }
}

data class PracticeMembershipUndo(
    val learningItemId: vn.loi.learning.domain.study.learning.model.LearningItemId,
    val previousMembership: List<vn.loi.learning.domain.study.learning.model.LearningItemId>,
    val previousQueue: List<vn.loi.learning.domain.study.learning.model.LearningItemId>,
    val previousIndex: Int,
    val previousRound: Int
)
