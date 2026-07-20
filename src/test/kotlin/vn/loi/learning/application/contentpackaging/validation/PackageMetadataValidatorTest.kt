package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageMetadataValidatorTest {

    private val validator = PackageMetadataValidator()

    @Test
    fun `valid OPD3 metadata produces valid report`() {
        val report = validator.validate(
            PackageDescriptor(
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD3"
            )
        )

        assertTrue(report.isValid)
        assertTrue(report.issues.isEmpty())
    }

    @Test
    fun `unsupported format produces error`() {
        val report = validator.validate(
            PackageDescriptor(
                name = "English Elementary",
                version = "1.0.0",
                format = "OPD2"
            )
        )

        assertEquals(
            listOf("UNSUPPORTED_PACKAGE_FORMAT"),
            report.errors.map { issue -> issue.code }
        )
    }

    @Test
    fun `non numeric version produces error`() {
        val report = validator.validate(
            PackageDescriptor(
                name = "English Elementary",
                version = "1.beta",
                format = "OPD3"
            )
        )

        assertEquals(
            listOf("INVALID_PACKAGE_VERSION"),
            report.errors.map { issue -> issue.code }
        )
    }

    @Test
    fun `multiple metadata failures are accumulated`() {
        val report = validator.validate(
            PackageDescriptor(
                name = "English Elementary",
                version = "version-one",
                format = "ZIP"
            )
        )

        assertEquals(
            listOf(
                "UNSUPPORTED_PACKAGE_FORMAT",
                "INVALID_PACKAGE_VERSION"
            ),
            report.errors.map { issue -> issue.code }
        )
    }
}
