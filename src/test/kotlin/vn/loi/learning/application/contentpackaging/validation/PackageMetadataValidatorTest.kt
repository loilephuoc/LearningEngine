package vn.loi.learning.application.contentpackaging.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageMetadataValidatorTest {

    private val validator =
        PackageMetadataValidator()

    @Test
    fun `valid OPD3 metadata produces valid report`() {
        val report =
            validator.validate(
                descriptor(
                    version =
                        "1.0.0"
                )
            )

        assertTrue(
            report.isValid
        )

        assertTrue(
            report.issues.isEmpty()
        )
    }

    @Test
    fun `package format comparison is case insensitive`() {
        val report =
            validator.validate(
                descriptor(
                    format =
                        "opd3"
                )
            )

        assertTrue(
            report.isValid
        )
    }

    @Test
    fun `unsupported format produces error`() {
        val report =
            validator.validate(
                descriptor(
                    format =
                        "OPD2"
                )
            )

        assertEquals(
            listOf(
                "UNSUPPORTED_PACKAGE_FORMAT"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `non numeric package version produces error`() {
        val report =
            validator.validate(
                descriptor(
                    version =
                        "1.beta"
                )
            )

        assertEquals(
            listOf(
                "INVALID_PACKAGE_VERSION"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `numeric package versions with different component counts are accepted`() {
        listOf(
            "1",
            "1.0",
            "1.0.0",
            "1.2.3.4"
        ).forEach { version ->
            val report =
                validator.validate(
                    descriptor(
                        version =
                            version
                    )
                )

            assertTrue(
                report.isValid,
                "Expected package version $version to be valid."
            )
        }
    }

    @Test
    fun `unsupported schema version produces error`() {
        val report =
            validator.validate(
                descriptor(
                    schemaVersion =
                        PackageDescriptor.CURRENT_SCHEMA_VERSION +
                                1
                )
            )

        assertEquals(
            listOf(
                "UNSUPPORTED_MANIFEST_SCHEMA_VERSION"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `configured supported schema version is accepted`() {
        val validator =
            PackageMetadataValidator(
                supportedSchemaVersions =
                    setOf(
                        1,
                        2
                    )
            )

        val report =
            validator.validate(
                descriptor(
                    schemaVersion =
                        2
                )
            )

        assertTrue(
            report.isValid
        )
    }

    @Test
    fun `empty supported schema configuration is rejected`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PackageMetadataValidator(
                    supportedSchemaVersions =
                        emptySet()
                )
            }

        assertEquals(
            "Supported schema versions must not be empty.",
            exception.message
        )
    }

    @Test
    fun `non positive supported schema configuration is rejected`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                PackageMetadataValidator(
                    supportedSchemaVersions =
                        setOf(
                            0,
                            1
                        )
                )
            }

        assertEquals(
            "Supported schema versions must be positive.",
            exception.message
        )
    }

    @Test
    fun `package requiring current engine version is accepted`() {
        val report =
            validator.validate(
                descriptor(
                    minimumEngineVersion =
                        "1.0.0"
                )
            )

        assertTrue(
            report.isValid
        )
    }

    @Test
    fun `package supporting current engine version as maximum is accepted`() {
        val report =
            validator.validate(
                descriptor(
                    maximumEngineVersion =
                        "1"
                )
            )

        assertTrue(
            report.isValid
        )
    }

    @Test
    fun `engine version below package minimum produces error`() {
        val validator =
            PackageMetadataValidator(
                currentEngineVersion =
                    "1.0.0"
            )

        val report =
            validator.validate(
                descriptor(
                    minimumEngineVersion =
                        "1.1.0"
                )
            )

        assertEquals(
            listOf(
                "ENGINE_VERSION_TOO_OLD"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `engine version above package maximum produces error`() {
        val validator =
            PackageMetadataValidator(
                currentEngineVersion =
                    "2.0.0"
            )

        val report =
            validator.validate(
                descriptor(
                    maximumEngineVersion =
                        "1.9.9"
                )
            )

        assertEquals(
            listOf(
                "ENGINE_VERSION_TOO_NEW"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `invalid current engine version produces error`() {
        val validator =
            PackageMetadataValidator(
                currentEngineVersion =
                    "development"
            )

        val report =
            validator.validate(
                descriptor()
            )

        assertEquals(
            listOf(
                "INVALID_ENGINE_VERSION"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `invalid minimum engine version produces error`() {
        val report =
            validator.validate(
                descriptor(
                    minimumEngineVersion =
                        "one"
                )
            )

        assertEquals(
            listOf(
                "INVALID_MINIMUM_ENGINE_VERSION"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `invalid maximum engine version produces error`() {
        val report =
            validator.validate(
                descriptor(
                    maximumEngineVersion =
                        "latest"
                )
            )

        assertEquals(
            listOf(
                "INVALID_MAXIMUM_ENGINE_VERSION"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `minimum engine version greater than maximum produces range error only`() {
        val report =
            validator.validate(
                descriptor(
                    minimumEngineVersion =
                        "3.0.0",
                    maximumEngineVersion =
                        "2.0.0"
                )
            )

        assertEquals(
            listOf(
                "INVALID_ENGINE_VERSION_RANGE"
            ),
            report.errorCodes()
        )
    }

    @Test
    fun `multiple independent metadata failures are accumulated`() {
        val report =
            validator.validate(
                descriptor(
                    version =
                        "version-one",
                    format =
                        "ZIP",
                    schemaVersion =
                        PackageDescriptor.CURRENT_SCHEMA_VERSION +
                                1,
                    minimumEngineVersion =
                        "invalid"
                )
            )

        assertEquals(
            listOf(
                "UNSUPPORTED_PACKAGE_FORMAT",
                "INVALID_PACKAGE_VERSION",
                "UNSUPPORTED_MANIFEST_SCHEMA_VERSION",
                "INVALID_MINIMUM_ENGINE_VERSION"
            ),
            report.errorCodes()
        )
    }

    private fun descriptor(
        name: String =
            "English Elementary",
        version: String =
            "1.0.0",
        format: String =
            "OPD3",
        schemaVersion: Int =
            PackageDescriptor.CURRENT_SCHEMA_VERSION,
        minimumEngineVersion: String? =
            null,
        maximumEngineVersion: String? =
            null
    ): PackageDescriptor =
        PackageDescriptor(
            name =
                name,
            version =
                version,
            format =
                format,
            schemaVersion =
                schemaVersion,
            minimumEngineVersion =
                minimumEngineVersion,
            maximumEngineVersion =
                maximumEngineVersion
        )

    private fun PackageValidationReport.errorCodes(): List<String> =
        errors.map { issue ->
            issue.code
        }
}