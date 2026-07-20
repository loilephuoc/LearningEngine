package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog

class PackageUpgradeOperation(
    private val contentPackageRepository:
    ContentPackageRepository,
    private val packageCatalogRepository:
    PackageCatalogRepository,
    private val installationDependencyGuard:
    PackageInstallationDependencyGuard =
        PackageInstallationDependencyGuard(
            contentPackageRepository
        ),
    private val removalDependencyGuard:
    PackageRemovalDependencyGuard =
        PackageRemovalDependencyGuard(
            contentPackageRepository
        )
) {

    fun execute(
        command: UpgradeContentPackageCommand
    ): PackageCatalog {
        val catalog =
            packageCatalogRepository.findById(
                command.catalogId
            ) ?: throw PackageNotInstalledException(
                command.currentPackageId
            )

        if (!catalog.contains(command.currentPackageId)) {
            throw PackageNotInstalledException(
                command.currentPackageId
            )
        }

        val currentPackage =
            contentPackageRepository.findById(
                command.currentPackageId
            ) ?: throw PackageNotInstalledException(
                command.currentPackageId
            )

        val replacementPackage =
            command.replacementPackage

        validateUpgrade(
            currentPackage =
                currentPackage,
            replacementPackage =
                replacementPackage
        )

        val existingReplacement =
            contentPackageRepository.findById(
                replacementPackage.id
            )

        if (
            existingReplacement != null &&
            existingReplacement != replacementPackage
        ) {
            throw DuplicatePackageException(
                replacementPackage.id
            )
        }

        installationDependencyGuard.ensureCanInstall(
            replacementPackage
        )

        removalDependencyGuard.ensureCanRemove(
            packageId =
                currentPackage.id,
            additionalPackages =
                listOf(
                    replacementPackage
                )
        )

        if (existingReplacement == null) {
            contentPackageRepository.save(
                replacementPackage
            )
        }

        val updatedCatalog =
            catalog
                .register(
                    replacementPackage.id
                )
                .remove(
                    currentPackage.id
                )

        packageCatalogRepository.save(
            updatedCatalog
        )

        contentPackageRepository.deleteById(
            currentPackage.id
        )

        return updatedCatalog
    }

    private fun validateUpgrade(
        currentPackage: ContentPackage,
        replacementPackage: ContentPackage
    ) {
        if (
            currentPackage.id ==
            replacementPackage.id
        ) {
            throw InvalidPackageUpgradeException(
                currentPackageId =
                    currentPackage.id,
                replacementPackageId =
                    replacementPackage.id,
                reason =
                    "replacement package must have a different ID"
            )
        }

        if (
            currentPackage.name !=
            replacementPackage.name
        ) {
            throw InvalidPackageUpgradeException(
                currentPackageId =
                    currentPackage.id,
                replacementPackageId =
                    replacementPackage.id,
                reason =
                    "package names do not match"
            )
        }

        val currentVersion =
            NumericPackageVersion.parseOrNull(
                currentPackage.version
            ) ?: throw InvalidPackageUpgradeException(
                currentPackageId =
                    currentPackage.id,
                replacementPackageId =
                    replacementPackage.id,
                reason =
                    "current package version is invalid"
            )

        val replacementVersion =
            NumericPackageVersion.parseOrNull(
                replacementPackage.version
            ) ?: throw InvalidPackageUpgradeException(
                currentPackageId =
                    currentPackage.id,
                replacementPackageId =
                    replacementPackage.id,
                reason =
                    "replacement package version is invalid"
            )

        if (
            replacementVersion <=
            currentVersion
        ) {
            throw InvalidPackageUpgradeException(
                currentPackageId =
                    currentPackage.id,
                replacementPackageId =
                    replacementPackage.id,
                reason =
                    "replacement version must be newer than the installed version"
            )
        }
    }
}