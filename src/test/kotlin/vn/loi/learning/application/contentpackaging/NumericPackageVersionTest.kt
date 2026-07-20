package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumericPackageVersionTest {

    @Test
    fun `numeric version is parsed`() {
        assertEquals(
            "1.2.3",
            NumericPackageVersion
                .parse(
                    "1.2.3"
                )
                .toString()
        )
    }

    @Test
    fun `invalid version returns null`() {
        assertNull(
            NumericPackageVersion.parseOrNull(
                "1.2-beta"
            )
        )
    }

    @Test
    fun `invalid version is rejected by strict parser`() {
        assertFailsWith<InvalidPackageVersionException> {
            NumericPackageVersion.parse(
                "invalid"
            )
        }
    }

    @Test
    fun `missing components are compared as zero`() {
        assertEquals(
            NumericPackageVersion.parse(
                "1.0"
            ),
            NumericPackageVersion.parse(
                "1.0.0"
            )
        )

        assertTrue(
            NumericPackageVersion.parse(
                "1.1"
            ) >
                    NumericPackageVersion.parse(
                        "1.0.9"
                    )
        )
    }
}