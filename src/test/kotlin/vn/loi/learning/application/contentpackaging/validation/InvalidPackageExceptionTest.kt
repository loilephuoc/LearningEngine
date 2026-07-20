package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InvalidPackageExceptionTest {

    @Test
    fun `exception exposes validation report`() {
        val report = PackageValidationReport(
            issues = listOf(
                PackageValidationIssue(
                    code = "INVALID_METADATA",
                    message = "Metadata is invalid.",
                    severity = PackageValidationSeverity.ERROR
                ),
                PackageValidationIssue(
                    code = "MISSING_CONTENT",
                    message = "Content is missing.",
                    severity = PackageValidationSeverity.ERROR
                )
            )
        )

        val exception = InvalidPackageException(report)

        assertSame(report, exception.report)
        assertTrue(exception.message!!.contains("INVALID_METADATA"))
        assertTrue(exception.message!!.contains("MISSING_CONTENT"))
        assertEquals(2, exception.message!!.lines().size)
    }
}
