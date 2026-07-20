package vn.loi.learning.application.contentpackaging

import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageId

class MissingPackageDependenciesException(
    val packageId: PackageId,
    val missingDependencies: Set<PackageDependency>
) : IllegalStateException(
    buildMessage(
        packageId =
            packageId,
        missingDependencies =
            missingDependencies
    )
) {

    companion object {

        private fun buildMessage(
            packageId: PackageId,
            missingDependencies: Set<PackageDependency>
        ): String {
            val dependencyDescriptions =
                missingDependencies
                    .map(
                        ::describeDependency
                    )
                    .sorted()
                    .joinToString(
                        separator = ", "
                    )

            return "Package $packageId has missing or incompatible dependencies: $dependencyDescriptions."
        }

        private fun describeDependency(
            dependency: PackageDependency
        ): String {
            val minimumVersion =
                dependency.minimumVersion
                    ?.let { version ->
                        " >= $version"
                    }
                    .orEmpty()

            val maximumVersion =
                dependency.maximumVersion
                    ?.let { version ->
                        " <= $version"
                    }
                    .orEmpty()

            return dependency.packageName +
                    minimumVersion +
                    maximumVersion
        }
    }
}