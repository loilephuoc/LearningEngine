package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PackageScanCandidateTest {

    @Test
    fun `candidate preserves source`() {
        val candidate =
            PackageScanCandidate(
                source = "C:/packages/english.opd3"
            )

        assertEquals(
            "C:/packages/english.opd3",
            candidate.source
        )
    }

    @Test
    fun `blank source is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            PackageScanCandidate(
                source = "   "
            )
        }
    }
}
