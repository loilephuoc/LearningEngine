package vn.loi.learning.domain.study.memory.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetrievabilityTest {

    @Test
    fun `valid probability value is accepted`() {
        val retrievability = Retrievability(0.75)

        assertEquals(0.75, retrievability.value)
    }

    @Test
    fun `lower boundary zero is accepted`() {
        val retrievability = Retrievability(0.0)

        assertEquals(0.0, retrievability.value)
    }

    @Test
    fun `upper boundary one is accepted`() {
        val retrievability = Retrievability(1.0)

        assertEquals(1.0, retrievability.value)
    }

    @Test
    fun `value below zero is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Retrievability(-0.01)
        }
    }

    @Test
    fun `value above one is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Retrievability(1.01)
        }
    }

    @Test
    fun `NaN is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Retrievability(Double.NaN)
        }
    }

    @Test
    fun `positive infinity is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Retrievability(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `negative infinity is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Retrievability(Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun `forgotten constant represents zero retrievability`() {
        assertEquals(
            0.0,
            Retrievability.FORGOTTEN.value
        )
    }

    @Test
    fun `fully retrievable constant represents maximum retrievability`() {
        assertEquals(
            1.0,
            Retrievability.FULLY_RETRIEVABLE.value
        )
    }
}