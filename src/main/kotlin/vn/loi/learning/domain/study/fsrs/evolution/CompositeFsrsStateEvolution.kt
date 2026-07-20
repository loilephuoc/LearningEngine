package vn.loi.learning.domain.study.fsrs.evolution

import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.fsrs.model.FsrsStateTransition
import vn.loi.learning.domain.study.memory.evolution.DifficultyEvolution
import vn.loi.learning.domain.study.memory.evolution.StabilityEvolution
import vn.loi.learning.domain.study.memory.model.ReviewRating

/**
 * Implementation chuyển tiếp dùng các evolution service hiện tại
 * để tiến hóa toàn bộ FsrsState.
 *
 * Lớp này giúp migration sang FsrsStateEvolution mà chưa làm thay đổi
 * hành vi Difficulty và Stability của scheduler.
 *
 * Sau này có thể được thay bằng implementation FSRS đầy đủ mà không
 * ảnh hưởng đến TransitionRule.
 */
class CompositeFsrsStateEvolution(
    private val difficultyEvolution: DifficultyEvolution,
    private val stabilityEvolution: StabilityEvolution
) : FsrsStateEvolution {

    override fun evolve(
        current: FsrsState,
        rating: ReviewRating
    ): FsrsStateTransition {
        val nextDifficulty =
            difficultyEvolution.evolve(
                current = current.difficulty,
                rating = rating
            )

        val nextStability =
            stabilityEvolution.evolve(
                current = current.stability,
                rating = rating
            )

        return FsrsStateTransition(
            nextState =
                FsrsState(
                    difficulty = nextDifficulty,
                    stability = nextStability
                )
        )
    }
}