package vn.loi.learning.domain.study.memory.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DifficultyTest {

    @Test
    fun `of creates difficulty inside valid range`() {
        val difficulty =
            Difficulty.of(6.5)

        assertEquals(
            6.5,
            difficulty.value
        )
    }

    @Test
    fun `of rejects value below minimum`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Difficulty.of(0.9)
            }

        assertEquals(
            "Difficulty must be between 1.0 and 10.0, but was 0.9.",
            exception.message
        )
    }

    @Test
    fun `of rejects value above maximum`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Difficulty.of(10.1)
            }

        assertEquals(
            "Difficulty must be between 1.0 and 10.0, but was 10.1.",
            exception.message
        )
    }

    @Test
    fun `of rejects non finite value`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Difficulty.of(Double.NaN)
            }

        assertEquals(
            "Difficulty must be finite.",
            exception.message
        )
    }

    @Test
    fun `increase returns increased difficulty`() {
        val difficulty =
            Difficulty.of(5.0)
                .increase(0.8)

        assertEquals(
            5.8,
            difficulty.value
        )
    }

    @Test
    fun `increase clamps at maximum`() {
        val difficulty =
            Difficulty.of(9.8)
                .increase(0.8)

        assertEquals(
            Difficulty.MAX_VALUE,
            difficulty.value
        )
    }

    @Test
    fun `decrease returns decreased difficulty`() {
        val difficulty =
            Difficulty.of(5.0)
                .decrease(0.5)

        assertEquals(
            4.5,
            difficulty.value
        )
    }

    @Test
    fun `decrease clamps at minimum`() {
        val difficulty =
            Difficulty.of(1.2)
                .decrease(0.5)

        assertEquals(
            Difficulty.MIN_VALUE,
            difficulty.value
        )
    }

    @Test
    fun `increase rejects negative delta`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Difficulty.of(5.0)
                    .increase(-0.1)
            }

        assertEquals(
            "Difficulty delta must not be negative.",
            exception.message
        )
    }

    @Test
    fun `decrease rejects non finite delta`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Difficulty.of(5.0)
                    .decrease(Double.POSITIVE_INFINITY)
            }

        assertEquals(
            "Difficulty delta must be finite.",
            exception.message
        )
    }

    @Test
    fun `clamped limits value to valid range`() {
        assertEquals(
            Difficulty.MIN_VALUE,
            Difficulty.clamped(-100.0).value
        )

        assertEquals(
            Difficulty.MAX_VALUE,
            Difficulty.clamped(100.0).value
        )
    }

    @Test
    fun `default difficulty uses default value`() {
        assertEquals(
            Difficulty.DEFAULT_VALUE,
            Difficulty.DEFAULT.value
        )
    }
}