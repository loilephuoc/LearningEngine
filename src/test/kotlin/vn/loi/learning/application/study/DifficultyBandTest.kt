package vn.loi.learning.application.study

import kotlin.test.Test
import kotlin.test.assertEquals

class DifficultyBandTest {

    @Test
    fun `values below medium threshold are easy`() {
        assertEquals(
            expected =
                DifficultyBand.EASY,
            actual =
                DifficultyBand.from(
                    1.0
                )
        )

        assertEquals(
            expected =
                DifficultyBand.EASY,
            actual =
                DifficultyBand.from(
                    3.999999
                )
        )
    }

    @Test
    fun `medium threshold belongs to medium band`() {
        assertEquals(
            expected =
                DifficultyBand.MEDIUM,
            actual =
                DifficultyBand.from(
                    4.0
                )
        )
    }

    @Test
    fun `values below hard threshold are medium`() {
        assertEquals(
            expected =
                DifficultyBand.MEDIUM,
            actual =
                DifficultyBand.from(
                    6.999999
                )
        )
    }

    @Test
    fun `hard threshold belongs to hard band`() {
        assertEquals(
            expected =
                DifficultyBand.HARD,
            actual =
                DifficultyBand.from(
                    7.0
                )
        )
    }

    @Test
    fun `maximum domain difficulty is hard`() {
        assertEquals(
            expected =
                DifficultyBand.HARD,
            actual =
                DifficultyBand.from(
                    10.0
                )
        )
    }
}