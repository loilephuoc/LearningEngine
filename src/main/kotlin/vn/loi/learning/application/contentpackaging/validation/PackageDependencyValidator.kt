package vn.loi.learning.application.contentpackaging.validation

import vn.loi.learning.application.contentpackaging.NumericPackageVersion
import vn.loi.learning.application.contentpackaging.PackageDependencyVersionMatcher
import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageDependencyValidator(
    private val contentPackageRepository:
    ContentPackageRepository
) {

    fun validate(
        descriptor: PackageDescriptor
    ): PackageValidationReport {
        val installedPackages =
            contentPackageRepository.findAll()

        val issues =
            descriptor.dependencies
                .sortedBy { dependency ->
                    dependency.packageName
                }
                .flatMap { dependency ->
                    validateDependency(
                        dependency =
                            dependency,
                        installedPackages =
                            installedPackages
                    )
                }

        return PackageValidationReport(
            issues =
                issues
        )
    }

    private fun validateDependency(
        dependency: PackageDependency,
        installedPackages: List<ContentPackage>
    ): List<PackageValidationIssue> {
        val declarationIssues =
            validateDeclaration(
                dependency
            )

        if (declarationIssues.isNotEmpty()) {
            return declarationIssues
        }

        val matchingPackages =
            installedPackages.filter { installedPackage ->
                installedPackage.name ==
                        dependency.packageName
            }

        if (matchingPackages.isEmpty()) {
            return listOf(
                PackageValidationIssue(
                    code =
                        "MISSING_PACKAGE_DEPENDENCY",
                    message =
                        "Required package is not installed: ${dependency.packageName}.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
            )
        }

        val compatiblePackageExists =
            matchingPackages.any { installedPackage ->
                PackageDependencyVersionMatcher.matches(
                    contentPackage =
                        installedPackage,
                    dependency =
                        dependency
                )
            }

        if (compatiblePackageExists) {
            return emptyList()
        }

        return listOf(
            PackageValidationIssue(
                code =
                    "INCOMPATIBLE_PACKAGE_DEPENDENCY",
                message =
                    dependencyVersionMessage(
                        dependency
                    ),
                severity =
                    PackageValidationSeverity.ERROR
            )
        )
    }

    private fun validateDeclaration(
        dependency: PackageDependency
    ): List<PackageValidationIssue> {
        val issues =
            mutableListOf<PackageValidationIssue>()

        val minimumVersion =
            dependency.minimumVersion?.let { version ->
                NumericPackageVersion.parseOrNull(
                    version
                )
            }

        val maximumVersion =
            dependency.maximumVersion?.let { version ->
                NumericPackageVersion.parseOrNull(
                    version
                )
            }

        if (
            dependency.minimumVersion != null &&
            minimumVersion == null
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "INVALID_DEPENDENCY_MINIMUM_VERSION",
                    message =
                        "Invalid minimum version for dependency ${dependency.packageName}: ${dependency.minimumVersion}.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }

        if (
            dependency.maximumVersion != null &&
            maximumVersion == null
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "INVALID_DEPENDENCY_MAXIMUM_VERSION",
                    message =
                        "Invalid maximum version for dependency ${dependency.packageName}: ${dependency.maximumVersion}.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }

        if (
            minimumVersion != null &&
            maximumVersion != null &&
            minimumVersion > maximumVersion
        ) {
            issues +=
                PackageValidationIssue(
                    code =
                        "INVALID_DEPENDENCY_VERSION_RANGE",
                    message =
                        "Dependency ${dependency.packageName} has a minimum version greater than its maximum version.",
                    severity =
                        PackageValidationSeverity.ERROR
                )
        }

        return issues
    }

    private fun dependencyVersionMessage(
        dependency: PackageDependency
    ): String {
        val versionRequirement =
            when {
                dependency.minimumVersion != null &&
                        dependency.maximumVersion != null ->
                    "between ${dependency.minimumVersion} and ${dependency.maximumVersion}"

                dependency.minimumVersion != null ->
                    "${dependency.minimumVersion} or newer"

                dependency.maximumVersion != null ->
                    "${dependency.maximumVersion} or older"

                else ->
                    "with a valid numeric version"
            }

        return "Installed package ${dependency.packageName} does not satisfy the required version $versionRequirement."
    }
}