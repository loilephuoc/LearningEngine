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

    fun validate(
        descriptor: PackageDescriptor
    ): PackageValidationReport {
        val issues =
            mutableListOf<PackageValidationIssue>()

        if (descriptor.format != SUPPORTED_FORMAT) {
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

        descriptor.minimumEngineVersion?.let {
                minimumVersion ->

            val parsedMinimumVersion =
                NumericPackageVersion.parseOrNull(
                    minimumVersion
                )

            if (parsedMinimumVersion == null) {
                issues +=
                    PackageValidationIssue(
                        code =
                            "INVALID_MINIMUM_ENGINE_VERSION",
                        message =
                            "Invalid minimum engine version: $minimumVersion.",
                        severity =
                            PackageValidationSeverity.ERROR
                    )
            } else if (
                engineVersion < parsedMinimumVersion
            ) {
                issues +=
                    PackageValidationIssue(
                        code =
                            "ENGINE_VERSION_TOO_OLD",
                        message =
                            "Package requires engine version $minimumVersion or newer, but current version is $currentEngineVersion.",
                        severity =
                            PackageValidationSeverity.ERROR
                    )
            }
        }

        descriptor.maximumEngineVersion?.let {
                maximumVersion ->

            val parsedMaximumVersion =
                NumericPackageVersion.parseOrNull(
                    maximumVersion
                )

            if (parsedMaximumVersion == null) {
                issues +=
                    PackageValidationIssue(
                        code =
                            "INVALID_MAXIMUM_ENGINE_VERSION",
                        message =
                            "Invalid maximum engine version: $maximumVersion.",
                        severity =
                            PackageValidationSeverity.ERROR
                    )
            } else if (
                engineVersion > parsedMaximumVersion
            ) {
                issues +=
                    PackageValidationIssue(
                        code =
                            "ENGINE_VERSION_TOO_NEW",
                        message =
                            "Package supports engine version $maximumVersion or older, but current version is $currentEngineVersion.",
                        severity =
                            PackageValidationSeverity.ERROR
                    )
            }
        }

        val minimumVersion =
            descriptor.minimumEngineVersion
                ?.let(
                    NumericPackageVersion::parseOrNull
                )

        val maximumVersion =
            descriptor.maximumEngineVersion
                ?.let(
                    NumericPackageVersion::parseOrNull
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
        }
    }

    companion object {

        const val SUPPORTED_FORMAT =
            "OPD3"

        const val CURRENT_ENGINE_VERSION =
            "1.0.0"
    }
}