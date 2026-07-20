package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageValidationReportTest {

    @Test
    fun `empty report is valid`() {
        val report = PackageValidationReport()

        assertTrue(report.isValid)
        assertFalse(report.hasWarnings)
        assertTrue(report.errors.isEmpty())
        assertTrue(report.warnings.isEmpty())
    }

    @Test
    fun `error makes report invalid`() {
        val error = PackageValidationIssue(
            code = "INVALID_METADATA",
            message = "Package metadata is invalid.",
            severity = PackageValidationSeverity.ERROR
        )

        val report = PackageValidationReport(
            issues = listOf(error)
        )

        assertFalse(report.isValid)
        assertEquals(listOf(error), report.errors)
        assertTrue(report.warnings.isEmpty())
    }

    @Test
    fun `warning does not make report invalid`() {
        val warning = PackageValidationIssue(
            code = "EMPTY_LIBRARY",
            message = "Package contains an empty library.",
            severity = PackageValidationSeverity.WARNING
        )

        val report = PackageValidationReport(
            issues = listOf(warning)
        )

        assertTrue(report.isValid)
        assertTrue(report.hasWarnings)
        assertEquals(listOf(warning), report.warnings)
        assertTrue(report.errors.isEmpty())
    }
}
