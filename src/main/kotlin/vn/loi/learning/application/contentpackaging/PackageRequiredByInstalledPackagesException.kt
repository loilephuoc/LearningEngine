package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageId

class PackageRequiredByInstalledPackagesException(
    val packageId: PackageId,
    val dependentPackageIds: Set<PackageId>
) : IllegalStateException(
    buildMessage(
        packageId =
            packageId,
        dependentPackageIds =
            dependentPackageIds
    )
) {

    companion object {

        private fun buildMessage(
            packageId: PackageId,
            dependentPackageIds: Set<PackageId>
        ): String {
            val dependents =
                dependentPackageIds
                    .map(
                        PackageId::toString
                    )
                    .sorted()
                    .joinToString(
                        separator = ", "
                    )

            return "Package $packageId is required by installed packages: $dependents."
        }
    }
}