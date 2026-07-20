package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageId

class PackageRemovalDependencyGuard(
    private val contentPackageRepository:
    ContentPackageRepository
) {

    fun ensureCanRemove(
        packageId: PackageId,
        additionalPackages: Collection<ContentPackage> =
            emptyList()
    ) {
        val installedPackages =
            (
                    contentPackageRepository.findAll() +
                            additionalPackages
                    )
                .associateBy { contentPackage ->
                    contentPackage.id
                }
                .values
                .toList()

        val targetPackage =
            installedPackages.firstOrNull { installedPackage ->
                installedPackage.id == packageId
            } ?: return

        val remainingPackages =
            installedPackages.filter { installedPackage ->
                installedPackage.id != packageId
            }

        val dependentPackageIds =
            remainingPackages
                .asSequence()
                .filter { installedPackage ->
                    requiresRemovedPackageWithoutAlternative(
                        installedPackage =
                            installedPackage,
                        removedPackage =
                            targetPackage,
                        remainingPackages =
                            remainingPackages
                    )
                }
                .map { installedPackage ->
                    installedPackage.id
                }
                .toSet()

        if (dependentPackageIds.isNotEmpty()) {
            throw PackageRequiredByInstalledPackagesException(
                packageId =
                    packageId,
                dependentPackageIds =
                    dependentPackageIds
            )
        }
    }

    private fun requiresRemovedPackageWithoutAlternative(
        installedPackage: ContentPackage,
        removedPackage: ContentPackage,
        remainingPackages: List<ContentPackage>
    ): Boolean =
        installedPackage
            .descriptor
            .dependencies
            .asSequence()
            .filter { dependency ->
                dependency.packageName ==
                        removedPackage.name
            }
            .any { dependency ->
                remainingPackages.none { candidatePackage ->
                    PackageDependencyVersionMatcher.matches(
                        contentPackage =
                            candidatePackage,
                        dependency =
                            dependency
                    )
                }
            }
}