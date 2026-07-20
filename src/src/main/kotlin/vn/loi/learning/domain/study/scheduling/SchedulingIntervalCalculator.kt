package vn.loi.learning.domain.study.scheduling

import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.memory.algorithm.IntervalCalculator
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

/**
 * Adapter giữa scheduling rule và IntervalCalculator.
 *
 * Lớp này chỉ chuyển các Value Object của scheduling thành FsrsState.
 * Nó không lựa chọn thuật toán và không chứa công thức.
 */
internal class SchedulingIntervalCalculator(
    private val intervalCalculator: IntervalCalculator
) {

    fun calculate(
        difficulty: Difficulty,
        stability: Stability,
        desiredRetention: DesiredRetention
    ): TimeSpan =
        intervalCalculator.calculate(
            state =
                FsrsState(
                    difficulty = difficulty,
                    stability = stability
                ),
            desiredRetention = desiredRetention
        )
}
