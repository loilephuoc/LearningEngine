package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LegacyPackageCandidateTest {

    @Test
    fun `candidate preserves JSON and media sources`() {
        val candidate =
            LegacyPackageCandidate(
                jsonSource = "C:/packages/2000Cau.json",
                mediaSource = "C:/packages/2000Cau.pkg"
            )

        assertEquals(
            "C:/packages/2000Cau.json",
            candidate.jsonSource
        )

        assertEquals(
            "C:/packages/2000Cau.pkg",
            candidate.mediaSource
        )
    }

    @Test
    fun `blank JSON source is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            LegacyPackageCandidate(
                jsonSource = "   ",
                mediaSource = "C:/packages/2000Cau.pkg"
            )
        }
    }

    @Test
    fun `blank media source is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            LegacyPackageCandidate(
                jsonSource = "C:/packages/2000Cau.json",
                mediaSource = "   "
            )
        }
    }
}
