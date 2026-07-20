package vn.loi.learning.domain.study.fsrs.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FsrsConfigurationTest {

    @Test
    fun `default configuration uses default FSRS parameters`() {
        val configuration =
            FsrsConfiguration.DEFAULT

        assertSame(
            FsrsParameters.DEFAULT,
            configuration.parameters
        )
    }

    @Test
    fun `default configuration uses default desired retention`() {
        val configuration =
            FsrsConfiguration.DEFAULT

        assertEquals(
            DesiredRetention.DEFAULT,
            configuration.desiredRetention
        )
    }

    @Test
    fun `configuration preserves supplied values`() {
        val parameters =
            FsrsParameters.of(
                DoubleArray(
                    FsrsParameters.PARAMETER_COUNT
                ) { index ->
                    index.toDouble() + 0.5
                }
            )

        val desiredRetention =
            DesiredRetention(0.95)

        val configuration =
            FsrsConfiguration(
                parameters = parameters,
                desiredRetention = desiredRetention
            )

        assertSame(
            parameters,
            configuration.parameters
        )

        assertEquals(
            desiredRetention,
            configuration.desiredRetention
        )
    }
}