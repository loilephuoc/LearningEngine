package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage

class PackageInstallationDependencyGuard(
    private val contentPackageRepository:
    ContentPackageRepository
) {

    fun ensureCanInstall(
        contentPackage: ContentPackage
    ) {
        val installedPackages =
            contentPackageRepository.findAll()

        val missingDependencies =
            contentPackage
                .descriptor
                .dependencies
                .filterNot { dependency ->
                    installedPackages.any { installedPackage ->
                        PackageDependencyVersionMatcher.matches(
                            contentPackage =
                                installedPackage,
                            dependency =
                                dependency
                        )
                    }
                }
                .toSet()

        if (missingDependencies.isNotEmpty()) {
            throw MissingPackageDependenciesException(
                packageId =
                    contentPackage.id,
                missingDependencies =
                    missingDependencies
            )
        }
    }
}