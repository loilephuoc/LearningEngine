package vn.loi.learning.domain.study.memory.science

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import vn.loi.learning.domain.study.fsrs.model.DesiredRetention
import vn.loi.learning.domain.study.memory.SimpleForgettingCurve
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

class SimpleIntervalSolverTest {

    private val solver =
        SimpleIntervalSolver()

    private val forgettingCurve =
        SimpleForgettingCurve()

    @Test
    fun `produced interval reaches desired retention on forgetting curve`() {
        val stability =
            Stability.of(12.5)

        val desiredRetention =
            DesiredRetention(0.90)

        val interval =
            solver.solve(
                stability = stability,
                desiredRetention = desiredRetention
            )

        val actualRetrievability =
            forgettingCurve.calculate(
                stability = stability,
                elapsedTime = interval
            )

        assertEquals(
            desiredRetention.value,
            actualRetrievability.value,
            TOLERANCE
        )
    }

    @Test
    fun `full retention produces zero interval`() {
        val interval =
            solver.solve(
                stability = Stability.of(10.0),
                desiredRetention = DesiredRetention(1.0)
            )

        assertEquals(
            TimeSpan.ZERO,
            interval
        )
    }

    @Test
    fun `zero stability produces zero interval`() {
        val interval =
            solver.solve(
                stability = Stability.ZERO,
                desiredRetention = DesiredRetention.DEFAULT
            )

        assertEquals(
            TimeSpan.ZERO,
            interval
        )
    }

    @Test
    fun `zero desired retention is rejected for finite exponential interval`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            solver.solve(
                stability = Stability.of(10.0),
                desiredRetention = DesiredRetention(0.0)
            )
        }
    }

    companion object {
        private const val TOLERANCE =
            1e-9
    }
}