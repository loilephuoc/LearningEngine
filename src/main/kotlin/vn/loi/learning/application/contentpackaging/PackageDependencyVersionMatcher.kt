package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageDependency

object PackageDependencyVersionMatcher {

    fun matches(
        contentPackage: ContentPackage,
        dependency: PackageDependency
    ): Boolean {
        if (
            contentPackage.name !=
            dependency.packageName
        ) {
            return false
        }

        return matchesVersion(
            installedVersion =
                contentPackage.version,
            dependency =
                dependency
        )
    }

    fun matchesVersion(
        installedVersion: String,
        dependency: PackageDependency
    ): Boolean {
        val parsedInstalledVersion =
            NumericPackageVersion.parseOrNull(
                installedVersion
            ) ?: return false

        val minimumVersion =
            parseOptionalVersion(
                dependency.minimumVersion
            ) ?: if (dependency.minimumVersion == null) {
                null
            } else {
                return false
            }

        val maximumVersion =
            parseOptionalVersion(
                dependency.maximumVersion
            ) ?: if (dependency.maximumVersion == null) {
                null
            } else {
                return false
            }

        if (
            minimumVersion != null &&
            maximumVersion != null &&
            minimumVersion > maximumVersion
        ) {
            return false
        }

        if (
            minimumVersion != null &&
            parsedInstalledVersion < minimumVersion
        ) {
            return false
        }

        if (
            maximumVersion != null &&
            parsedInstalledVersion > maximumVersion
        ) {
            return false
        }

        return true
    }

    private fun parseOptionalVersion(
        version: String?
    ): NumericPackageVersion? =
        version?.let(
            NumericPackageVersion::parseOrNull
        )
}