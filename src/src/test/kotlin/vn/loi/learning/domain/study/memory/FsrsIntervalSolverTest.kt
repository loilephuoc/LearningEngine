package vn.loi.learning.domain.study.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan
import vn.loi.learning.domain.study.memory.science.FsrsIntervalSolver

class FsrsIntervalSolverTest {

    private val solver =
        FsrsIntervalSolver()

    private val curve =
        FsrsForgettingCurve()

    @Test
    fun `solver and forgetting curve are mathematical inverses`() {
        val stability =
            Stability.of(10.0)

        val desiredRetention =
            DesiredRetention(0.85)

        val interval =
            solver.solve(
                stability = stability,
                desiredRetention = desiredRetention
            )

        val actualRetention =
            curve.calculate(
                stability = stability,
                elapsedTime = interval
            )

        assertEquals(
            expected = desiredRetention.value,
            actual = actualRetention.value,
            absoluteTolerance = TOLERANCE
        )
    }

    @Test
    fun `desired retention of ninety percent returns stability interval`() {
        val stability =
            Stability.of(10.0)

        val interval =
            solver.solve(
                stability = stability,
                desiredRetention = DesiredRetention.DEFAULT
            )

        assertEquals(
            expected = stability.days,
            actual = interval.toDays(),
            absoluteTolerance = TOLERANCE
        )
    }

    @Test
    fun `desired retention of one returns zero interval`() {
        val interval =
            solver.solve(
                stability = Stability.of(10.0),
                desiredRetention = DesiredRetention(1.0)
            )

        assertEquals(
            expected = TimeSpan.ZERO,
            actual = interval
        )
    }

    @Test
    fun `zero stability returns zero interval`() {
        val interval =
            solver.solve(
                stability = Stability.ZERO,
                desiredRetention = DesiredRetention.DEFAULT
            )

        assertEquals(
            expected = TimeSpan.ZERO,
            actual = interval
        )
    }

    @Test
    fun `desired retention of zero is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            solver.solve(
                stability = Stability.of(10.0),
                desiredRetention = DesiredRetention(0.0)
            )
        }
    }

    private companion object {
        const val TOLERANCE: Double = 1e-9
    }
}