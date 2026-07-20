package vn.loi.learning.domain.study.memory.algorithm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.fsrs.model.FsrsState
import vn.loi.learning.domain.study.memory.model.Difficulty
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.memory.science.IntervalSolver

class FsrsIntervalCalculatorTest {

    @Test
    fun `delegates stability and desired retention to interval solver`() {
        val expectedStability =
            Stability.of(12.5)

        val expectedRetention =
            DesiredRetention(0.92)

        val solver =
            RecordingIntervalSolver(
                result = TimeSpan.days(10)
            )

        val calculator =
            FsrsIntervalCalculator(
                intervalSolver = solver
            )

        val state =
            FsrsState(
                difficulty = Difficulty.of(7.0),
                stability = expectedStability
            )

        calculator.calculate(
            state = state,
            desiredRetention = expectedRetention
        )

        assertEquals(
            expectedStability,
            solver.receivedStability
        )

        assertEquals(
            expectedRetention,
            solver.receivedDesiredRetention
        )
    }

    @Test
    fun `returns interval produced by interval solver`() {
        val expectedInterval =
            TimeSpan.days(18)

        val solver =
            RecordingIntervalSolver(
                result = expectedInterval
            )

        val calculator =
            FsrsIntervalCalculator(
                intervalSolver = solver
            )

        val state =
            FsrsState(
                difficulty = Difficulty.DEFAULT,
                stability = Stability.of(20.0)
            )

        val actualInterval =
            calculator.calculate(
                state = state,
                desiredRetention = DesiredRetention.DEFAULT
            )

        assertEquals(
            expectedInterval,
            actualInterval
        )
    }

    @Test
    fun `calls interval solver exactly once`() {
        val solver =
            RecordingIntervalSolver(
                result = TimeSpan.days(7)
            )

        val calculator =
            FsrsIntervalCalculator(
                intervalSolver = solver
            )

        val state =
            FsrsState(
                difficulty = Difficulty.of(4.0),
                stability = Stability.of(8.0)
            )

        calculator.calculate(
            state = state,
            desiredRetention = DesiredRetention.HIGH
        )

        assertEquals(
            1,
            solver.callCount
        )
    }

    @Test
    fun `difficulty does not affect delegation to interval solver`() {
        val stability =
            Stability.of(15.0)

        val desiredRetention =
            DesiredRetention.DEFAULT

        val firstSolver =
            RecordingIntervalSolver(
                result = TimeSpan.days(11)
            )

        val secondSolver =
            RecordingIntervalSolver(
                result = TimeSpan.days(11)
            )

        val firstCalculator =
            FsrsIntervalCalculator(
                intervalSolver = firstSolver
            )

        val secondCalculator =
            FsrsIntervalCalculator(
                intervalSolver = secondSolver
            )

        firstCalculator.calculate(
            state = FsrsState(
                difficulty = Difficulty.of(2.0),
                stability = stability
            ),
            desiredRetention = desiredRetention
        )

        secondCalculator.calculate(
            state = FsrsState(
                difficulty = Difficulty.of(9.0),
                stability = stability
            ),
            desiredRetention = desiredRetention
        )

        assertEquals(
            firstSolver.receivedStability,
            secondSolver.receivedStability
        )

        assertEquals(
            firstSolver.receivedDesiredRetention,
            secondSolver.receivedDesiredRetention
        )
    }

    private class RecordingIntervalSolver(
        private val result: TimeSpan
    ) : IntervalSolver {

        var receivedStability: Stability? =
            null
            private set

        var receivedDesiredRetention: DesiredRetention? =
            null
            private set

        var callCount: Int =
            0
            private set

        override fun solve(
            stability: Stability,
            desiredRetention: DesiredRetention
        ): TimeSpan {
            receivedStability =
                stability

            receivedDesiredRetention =
                desiredRetention

            callCount += 1

            return result
        }
    }
}