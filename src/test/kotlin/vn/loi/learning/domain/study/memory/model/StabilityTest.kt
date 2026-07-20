package vn.loi.learning.domain.study.memory.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StabilityTest {

    @Test
    fun `of creates stability with valid days`() {
        val stability =
            Stability.of(2.5)

        assertEquals(
            2.5,
            stability.days
        )
    }

    @Test
    fun `of accepts zero days`() {
        assertEquals(
            0.0,
            Stability.ZERO.days
        )
    }

    @Test
    fun `of rejects negative days`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Stability.of(-0.1)
            }

        assertEquals(
            "Stability days must not be negative.",
            exception.message
        )
    }

    @Test
    fun `of rejects non finite days`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Stability.of(Double.NaN)
            }

        assertEquals(
            "Stability days must be finite.",
            exception.message
        )
    }

    @Test
    fun `isZero returns true only for zero stability`() {
        assertTrue(
            Stability.ZERO.isZero()
        )

        assertFalse(
            Stability.of(0.1).isZero()
        )
    }

    @Test
    fun `isPositive returns true only for positive stability`() {
        assertFalse(
            Stability.ZERO.isPositive()
        )

        assertTrue(
            Stability.of(0.1).isPositive()
        )
    }

    @Test
    fun `multiplyBy returns multiplied stability`() {
        val stability =
            Stability.of(2.0)
                .multiplyBy(2.5)

        assertEquals(
            5.0,
            stability.days
        )
    }

    @Test
    fun `multiplyBy accepts zero multiplier`() {
        val stability =
            Stability.of(2.0)
                .multiplyBy(0.0)

        assertEquals(
            Stability.ZERO,
            stability
        )
    }

    @Test
    fun `multiplyBy rejects negative multiplier`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Stability.of(2.0)
                    .multiplyBy(-1.0)
            }

        assertEquals(
            "Stability multiplier must not be negative.",
            exception.message
        )
    }

    @Test
    fun `multiplyBy rejects non finite multiplier`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Stability.of(2.0)
                    .multiplyBy(Double.POSITIVE_INFINITY)
            }

        assertEquals(
            "Stability multiplier must be finite.",
            exception.message
        )
    }

    @Test
    fun `coerceAtLeast keeps current stability when already above minimum`() {
        val stability =
            Stability.of(2.0)
                .coerceAtLeast(
                    Stability.of(0.5)
                )

        assertEquals(
            2.0,
            stability.days
        )
    }

    @Test
    fun `coerceAtLeast returns minimum when current stability is lower`() {
        val stability =
            Stability.of(0.1)
                .coerceAtLeast(
                    Stability.of(0.5)
                )

        assertEquals(
            0.5,
            stability.days
        )
    }

    @Test
    fun `compareTo compares stability values`() {
        assertTrue(
            Stability.of(2.0) >
                    Stability.of(1.0)
        )

        assertTrue(
            Stability.of(1.0) <
                    Stability.of(2.0)
        )

        assertEquals(
            0,
            Stability.of(2.0)
                .compareTo(
                    Stability.of(2.0)
                )
        )
    }

    @Test
    fun `toTimeSpan converts days to milliseconds`() {
        val interval =
            Stability.of(1.5)
                .toTimeSpan()

        assertEquals(
            129_600_000L,
            interval.millis
        )
    }

    @Test
    fun `toTimeSpan preserves fractional day precision`() {
        val interval =
            Stability.of(0.5)
                .toTimeSpan()

        assertEquals(
            43_200_000L,
            interval.millis
        )
    }
}