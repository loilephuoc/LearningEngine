package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.ImportedPackageContent

class PackageWarningValidatorTest {

    private val validator = PackageWarningValidator()

    @Test
    fun `import warnings produce validation warnings without invalidating package`() {
        val report = validator.validate(
            ImportedPackageContent(
                contents = emptyList(),
                learningItems = emptyList(),
                warnings = listOf(
                    "Missing optional audio file.",
                    "Unknown optional metadata field."
                )
            )
        )

        assertTrue(report.isValid)
        assertTrue(report.hasWarnings)
        assertTrue(report.errors.isEmpty())
        assertEquals(
            listOf(
                "PACKAGE_IMPORT_WARNING",
                "PACKAGE_IMPORT_WARNING"
            ),
            report.warnings.map { issue -> issue.code }
        )
        assertEquals(
            listOf(
                "Missing optional audio file.",
                "Unknown optional metadata field."
            ),
            report.warnings.map { issue -> issue.message }
        )
    }

    @Test
    fun `no import warnings produces empty valid report`() {
        val report = validator.validate(
            ImportedPackageContent(
                contents = emptyList(),
                learningItems = emptyList()
            )
        )

        assertTrue(report.isValid)
        assertFalse(report.hasWarnings)
        assertTrue(report.issues.isEmpty())
    }
}
