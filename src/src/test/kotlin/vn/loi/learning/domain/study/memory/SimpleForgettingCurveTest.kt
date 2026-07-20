package vn.loi.learning.domain.study.memory

import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.study.memory.model.Retrievability
import vn.loi.learning.domain.study.memory.model.Stability
import vn.loi.learning.domain.study.memory.model.TimeSpan

class SimpleForgettingCurveTest {

    private val forgettingCurve =
        SimpleForgettingCurve()

    @Test
    fun `zero elapsed time is fully retrievable`() {
        val result =
            forgettingCurve.calculate(
                stability = Stability.of(5.0),
                elapsedTime = TimeSpan.ZERO
            )

        assertEquals(
            Retrievability.FULLY_RETRIEVABLE,
            result
        )
    }

    @Test
    fun `retrievability decreases as elapsed time increases`() {
        val stability =
            Stability.of(5.0)

        val earlier =
            forgettingCurve.calculate(
                stability = stability,
                elapsedTime = TimeSpan.days(1)
            )

        val later =
            forgettingCurve.calculate(
                stability = stability,
                elapsedTime = TimeSpan.days(3)
            )

        assertTrue(earlier.value > later.value)
    }

    @Test
    fun `greater stability produces greater retrievability`() {
        val elapsedTime =
            TimeSpan.days(3)

        val lowerStability =
            forgettingCurve.calculate(
                stability = Stability.of(2.0),
                elapsedTime = elapsedTime
            )

        val greaterStability =
            forgettingCurve.calculate(
                stability = Stability.of(6.0),
                elapsedTime = elapsedTime
            )

        assertTrue(
            greaterStability.value >
                    lowerStability.value
        )
    }

    @Test
    fun `zero stability with zero elapsed time is fully retrievable`() {
        val result =
            forgettingCurve.calculate(
                stability = Stability.ZERO,
                elapsedTime = TimeSpan.ZERO
            )

        assertEquals(
            Retrievability.FULLY_RETRIEVABLE,
            result
        )
    }

    @Test
    fun `zero stability with positive elapsed time is forgotten`() {
        val result =
            forgettingCurve.calculate(
                stability = Stability.ZERO,
                elapsedTime = TimeSpan.days(1)
            )

        assertEquals(
            Retrievability.FORGOTTEN,
            result
        )
    }

    @Test
    fun `elapsed time equal to stability follows exponential decay`() {
        val result =
            forgettingCurve.calculate(
                stability = Stability.of(4.0),
                elapsedTime = TimeSpan.days(4)
            )

        assertEquals(
            expected = exp(-1.0),
            actual = result.value,
            absoluteTolerance = 1e-12
        )
    }
}