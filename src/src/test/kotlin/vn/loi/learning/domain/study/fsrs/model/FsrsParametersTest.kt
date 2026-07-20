package vn.loi.learning.domain.study.fsrs.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class FsrsParametersTest {

    @Test
    fun `default contains exactly 21 parameters`() {
        assertEquals(
            expected = FsrsParameters.PARAMETER_COUNT,
            actual = FsrsParameters.DEFAULT.size
        )
    }

    @Test
    fun `factory rejects an invalid parameter count`() {
        assertFailsWith<IllegalArgumentException> {
            FsrsParameters.of(DoubleArray(20))
        }

        assertFailsWith<IllegalArgumentException> {
            FsrsParameters.of(DoubleArray(22))
        }
    }

    @Test
    fun `factory defensively copies the source array`() {
        val source = DoubleArray(FsrsParameters.PARAMETER_COUNT) { index ->
            index.toDouble()
        }

        val parameters = FsrsParameters.of(source)
        val originalFirstValue = parameters[0]

        source[0] = 999.0

        assertNotEquals(
            illegal = 999.0,
            actual = parameters[0]
        )
        assertEquals(
            expected = originalFirstValue,
            actual = parameters[0]
        )
    }
}
