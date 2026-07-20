package vn.loi.learning.domain.study.fsrs.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesiredRetentionTest {

    @Test
    fun `valid desired retention is accepted`() {
        val desiredRetention =
            DesiredRetention(0.87)

        assertEquals(
            0.87,
            desiredRetention.value
        )
    }

    @Test
    fun `lower boundary zero is accepted`() {
        val desiredRetention =
            DesiredRetention(0.0)

        assertEquals(
            0.0,
            desiredRetention.value
        )
    }

    @Test
    fun `upper boundary one is accepted`() {
        val desiredRetention =
            DesiredRetention(1.0)

        assertEquals(
            1.0,
            desiredRetention.value
        )
    }

    @Test
    fun `value below zero is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DesiredRetention(-0.01)
        }
    }

    @Test
    fun `value above one is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DesiredRetention(1.01)
        }
    }

    @Test
    fun `NaN is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DesiredRetention(Double.NaN)
        }
    }

    @Test
    fun `positive infinity is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DesiredRetention(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative infinity is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DesiredRetention(Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `low constant represents eighty percent retention`() {
        assertEquals(
            0.80,
            DesiredRetention.LOW.value
        )
    }

    @Test
    fun `default constant represents ninety percent retention`() {
        assertEquals(
            0.90,
            DesiredRetention.DEFAULT.value
        )
    }

    @Test
    fun `high constant represents ninety five percent retention`() {
        assertEquals(
            0.95,
            DesiredRetention.HIGH.value
        )
    }
}