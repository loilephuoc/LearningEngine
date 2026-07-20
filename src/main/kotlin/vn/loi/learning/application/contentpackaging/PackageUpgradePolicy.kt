package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageDescriptor

class PackageUpgradePolicy {

    fun evaluate(
        installedDescriptor: PackageDescriptor,
        candidateDescriptor: PackageDescriptor
    ): PackageUpgradeDecision {
        if (
            installedDescriptor.name !=
            candidateDescriptor.name ||
            installedDescriptor.format !=
            candidateDescriptor.format
        ) {
            return PackageUpgradeDecision.DifferentPackage
        }

        val installedVersion =
            NumericPackageVersion.parseOrNull(
                installedDescriptor.version
            ) ?: return PackageUpgradeDecision.InvalidInstalledVersion

        val candidateVersion =
            NumericPackageVersion.parseOrNull(
                candidateDescriptor.version
            ) ?: return PackageUpgradeDecision.InvalidCandidateVersion

        return when {
            candidateVersion > installedVersion ->
                PackageUpgradeDecision.UpgradeAvailable

            candidateVersion == installedVersion ->
                PackageUpgradeDecision.SameVersion

            else ->
                PackageUpgradeDecision.Downgrade
        }
    }
}