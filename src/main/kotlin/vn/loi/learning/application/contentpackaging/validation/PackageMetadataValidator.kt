package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.NumericPackageVersion
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageMetadataValidator(
    private val currentEngineVersion: String =
        CURRENT_ENGINE_VERSION,
    private val supportedSchemaVersions: Set<Int> =
        setOf(
            PackageDescriptor.CURRENT_SCHEMA_VERSION
        )
) {

    init {
        require(
            supportedSchemaVersions.isNotEmpty()
        ) {
            "Supported schema versions must not be empty."
        }

        require(
            supportedSchemaVersions.all { schemaVersion ->
                schemaVersion > 0
            }
        ) {
            "Supported schema versions must be positive."
        }
    }

    fun validate(
        descriptor: PackageDescriptor
    ): PackageValidationReport {
        val issues =
            mutableListOf<PackageValidationIssue>()

        validateFormat(
            descriptor =
                descriptor,
            issues =
                issues
        )

        validatePackageVersion(
            descriptor =
                descriptor,
            issues =
                issues
        )

        validateSchemaVersion(
            descriptor =
                descriptor,
            issues =
                issues
        )

        validateEngineCompatibility(
            descriptor =
                descriptor,
            issues =
                issues
        )

        return PackageValidationReport(
            issues =
                issues
        )
    }

    private fun validateFormat(
        descriptor: PackageDescriptor,
        issues: MutableList<PackageValidationIssue>
    ) {
        if (
            !descriptor.format.equals(
                SUPPORTED_FORMAT,
                ignoreCase = true
            )
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "UNSUPPORTED_PACKAGE_FORMAT",
                    message =
                        "Unsupported package format: ${descriptor.format}. Expected $SUPPORTED_FORMAT.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }
    }

    private fun validatePackageVersion(
        descriptor: PackageDescriptor,
        issues: MutableList<PackageValidationIssue>
    ) {
        if (
            NumericPackageVersion.parseOrNull(
                descriptor.version
            ) == null
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "INVALID_PACKAGE_VERSION",
                    message =
                        "Invalid package version: ${descriptor.version}.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }
    }

    private fun validateSchemaVersion(
        descriptor: PackageDescriptor,
        issues: MutableList<PackageValidationIssue>
    ) {
        if (
            descriptor.schemaVersion !in
            supportedSchemaVersions
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "UNSUPPORTED_MANIFEST_SCHEMA_VERSION",
                    message =
                        "Unsupported manifest schema version: ${descriptor.schemaVersion}.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }
    }

    private fun validateEngineCompatibility(
        descriptor: PackageDescriptor,
        issues: MutableList<PackageValidationIssue>
    ) {
        val engineVersion =
            NumericPackageVersion.parseOrNull(
                currentEngineVersion
            )

        if (engineVersion == null) {
            issues +=
                PackageValidationIssue(
                    code =
                        "INVALID_ENGINE_VERSION",
                    message =
                        "Invalid current engine version: $currentEngineVersion.",
                    severity =
                        PackageValidationSeverity.ERROR
                )

            return
        }

        val minimumVersion =
            parseOptionalEngineVersion(
                value =
                    descriptor.minimumEngineVersion,
                invalidCode =
                    "INVALID_MINIMUM_ENGINE_VERSION",
                invalidLabel =
                    "minimum",
                issues =
                    issues
            )

        val maximumVersion =
            parseOptionalEngineVersion(
                value =
                    descriptor.maximumEngineVersion,
                invalidCode =
                    "INVALID_MAXIMUM_ENGINE_VERSION",
                invalidLabel =
                    "maximum",
                issues =
                    issues
            )

        if (
            minimumVersion != null &&
            maximumVersion != null &&
            minimumVersion > maximumVersion
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "INVALID_ENGINE_VERSION_RANGE",
                    message =
                        "Minimum engine version must not be greater than maximum engine version.",
                    severity =
                        PackageValidationSeverity.ERROR
                )

            return
        }

        if (
            minimumVersion != null &&
            engineVersion < minimumVersion
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "ENGINE_VERSION_TOO_OLD",
                    message =
                        "Package requires engine version ${descriptor.minimumEngineVersion} or newer, but current version is $currentEngineVersion.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }

        if (
            maximumVersion != null &&
            engineVersion > maximumVersion
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "ENGINE_VERSION_TOO_NEW",
                    message =
                        "Package supports engine version ${descriptor.maximumEngineVersion} or older, but current version is $currentEngineVersion.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }
    }

    private fun parseOptionalEngineVersion(
        value: String?,
        invalidCode: String,
        invalidLabel: String,
        issues: MutableList<PackageValidationIssue>
    ): NumericPackageVersion? {
        if (value == null) {
            return null
        }

        val parsedVersion =
            NumericPackageVersion.parseOrNull(
                value
            )

        if (parsedVersion == null) {
            issues +=
                PackageValidationIssue(
                    code =
                        invalidCode,
                    message =
                        "Invalid $invalidLabel engine version: $value.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }

        return parsedVersion
    }

    companion object {

        const val SUPPORTED_FORMAT =
            "OPD3"

        const val CURRENT_ENGINE_VERSION =
            "1.0.0"
    }
}