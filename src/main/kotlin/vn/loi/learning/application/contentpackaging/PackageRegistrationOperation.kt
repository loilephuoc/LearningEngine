package vn.loi.learning.application.contentpackaging

import vn.loi.learning.application.port.ContentPackageRepository
import vn.loi.learning.application.port.PackageCatalogRepository
import vn.loi.learning.domain.content.packaging.model.PackageCatalog

/**
 * Thực hiện phần ghi dữ liệu khi đăng ký package.
 *
 * Operation này không tự mở transaction.
 * Transaction boundary thuộc use case hoặc workflow gọi nó.
 */
class PackageRegistrationOperation(
    private val contentPackageRepository:
    ContentPackageRepository,
    private val packageCatalogRepository:
    PackageCatalogRepository,
    private val installationDependencyGuard:
    PackageInstallationDependencyGuard =
        PackageInstallationDependencyGuard(
            contentPackageRepository
        )
) {

    fun ensureCanRegister(
        command: RegisterContentPackageCommand
    ) {
        val existingPackage =
            contentPackageRepository.findById(
                command.contentPackage.id
            )

        if (
            existingPackage != null &&
            existingPackage != command.contentPackage
        ) {
            throw DuplicatePackageException(
                command.contentPackage.id
            )
        }

        if (existingPackage == null) {
            installationDependencyGuard.ensureCanInstall(
                command.contentPackage
            )
        }
    }

    fun execute(
        command: RegisterContentPackageCommand
    ): PackageCatalog {
        ensureCanRegister(
            command
        )

        val existingPackage =
            contentPackageRepository.findById(
                command.contentPackage.id
            )

        if (existingPackage == null) {
            contentPackageRepository.save(
                command.contentPackage
            )
        }

        val catalog =
            packageCatalogRepository.findById(
                command.catalogId
            ) ?: PackageCatalog(
                id =
                    command.catalogId
            )

        val updatedCatalog =
            catalog.register(
                command.contentPackage.id
            )

        if (updatedCatalog !== catalog) {
            packageCatalogRepository.save(
                updatedCatalog
            )
        }

        return updatedCatalog
    }
}