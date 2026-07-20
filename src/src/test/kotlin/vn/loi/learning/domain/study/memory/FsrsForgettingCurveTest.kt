package vn.loi.learning.domain.study.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

class FsrsForgettingCurveTest {

    private val curve =
        FsrsForgettingCurve()

    @Test
    fun `zero elapsed time is fully retrievable`() {
        val result =
            curve.calculate(
                stability = Stability.of(10.0),
                elapsedTime = TimeSpan.ZERO
            )

        assertEquals(
            expected = 1.0,
            actual = result.value,
            absoluteTolerance = TOLERANCE
        )
    }

    @Test
    fun `elapsed equal to stability produces ninety percent retrievability`() {
        val stability =
            Stability.of(10.0)

        val result =
            curve.calculate(
                stability = stability,
                elapsedTime = TimeSpan.days(10)
            )

        assertEquals(
            expected = 0.9,
            actual = result.value,
            absoluteTolerance = TOLERANCE
        )
    }

    @Test
    fun `zero stability is forgotten after positive elapsed time`() {
        val result =
            curve.calculate(
                stability = Stability.ZERO,
                elapsedTime = TimeSpan.days(1)
            )

        assertEquals(
            expected = 0.0,
            actual = result.value,
            absoluteTolerance = TOLERANCE
        )
    }

    private companion object {
        const val TOLERANCE: Double = 1e-12
    }
}