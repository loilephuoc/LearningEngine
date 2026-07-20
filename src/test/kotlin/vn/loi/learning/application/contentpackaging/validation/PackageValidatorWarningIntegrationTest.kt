package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.application.contentpackaging.ImportedPackageContent
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageValidatorWarningIntegrationTest {

    @Test
    fun `warning is preserved while package remains valid`() {
        val validator = PackageValidator()

        val report = validator.validate(
            descriptor = PackageDescriptor(
                name = "Demo",
                version = "1.0.0",
                format = "OPD3"
            ),
            importedContent = ImportedPackageContent(
                contents = emptyList(),
                learningItems = emptyList(),
                warnings = listOf(
                    "Missing optional audio.",
                    "Unknown optional metadata."
                )
            )
        )

        assertTrue(report.isValid)
        assertEquals(0, report.errors.size)
        assertEquals(2, report.warnings.size)
        assertEquals(
            listOf(
                "Missing optional audio.",
                "Unknown optional metadata."
            ),
            report.warnings.map { it.message }
        )
    }
}
