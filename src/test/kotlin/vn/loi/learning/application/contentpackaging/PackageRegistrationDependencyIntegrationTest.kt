package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageRegistrationDependencyIntegrationTest {

    @Test
    fun `registration rejects package when dependency is missing`() {
        val packageRepository =
            InMemoryContentPackageRepository()

        val catalogRepository =
            InMemoryPackageCatalogRepository()

        val extensionPackage =
            contentPackage(
                id =
                    "extension-package",
                name =
                    "Vocabulary Extension",
                version =
                    "1.0.0",
                dependencies =
                    setOf(
                        PackageDependency(
                            packageName =
                                "Core English",
                            minimumVersion =
                                "1.0.0"
                        )
                    )
            )

        val useCase =
            createUseCase(
                packageRepository =
                    packageRepository,
                catalogRepository =
                    catalogRepository
            )

        assertFailsWith<MissingPackageDependenciesException> {
            useCase.execute(
                RegisterContentPackageCommand(
                    catalogId =
                        PackageCatalogId(
                            "installed-packages"
                        ),
                    contentPackage =
                        extensionPackage
                )
            )
        }

        assertEquals(
            null,
            packageRepository.findById(
                extensionPackage.id
            )
        )

        assertEquals(
            null,
            catalogRepository.findById(
                PackageCatalogId(
                    "installed-packages"
                )
            )
        )
    }

    @Test
    fun `registration succeeds after compatible dependency is installed`() {
        val packageRepository =
            InMemoryContentPackageRepository()

        val catalogRepository =
            InMemoryPackageCatalogRepository()

        val useCase =
            createUseCase(
                packageRepository =
                    packageRepository,
                catalogRepository =
                    catalogRepository
            )

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val corePackage =
            contentPackage(
                id =
                    "core-package",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )

        val extensionPackage =
            contentPackage(
                id =
                    "extension-package",
                name =
                    "Vocabulary Extension",
                version =
                    "1.0.0",
                dependencies =
                    setOf(
                        PackageDependency(
                            packageName =
                                "Core English",
                            minimumVersion =
                                "1.0.0",
                            maximumVersion =
                                "2.0.0"
                        )
                    )
            )

        useCase.execute(
            RegisterContentPackageCommand(
                catalogId =
                    catalogId,
                contentPackage =
                    corePackage
            )
        )

        val catalog =
            useCase.execute(
                RegisterContentPackageCommand(
                    catalogId =
                        catalogId,
                    contentPackage =
                        extensionPackage
                )
            )

        assertEquals(
            extensionPackage,
            packageRepository.findById(
                extensionPackage.id
            )
        )

        assertEquals(
            setOf(
                corePackage.id,
                extensionPackage.id
            ),
            catalog.packageIds
        )
    }

    private fun createUseCase(
        packageRepository:
        InMemoryContentPackageRepository,
        catalogRepository:
        InMemoryPackageCatalogRepository
    ): RegisterContentPackageUseCase =
        RegisterContentPackageUseCase(
            registrationOperation =
                PackageRegistrationOperation(
                    contentPackageRepository =
                        packageRepository,
                    packageCatalogRepository =
                        catalogRepository
                ),
            transactionRunner =
                InMemoryTransactionRunner()
        )

    private fun contentPackage(
        id: String,
        name: String,
        version: String,
        dependencies: Set<PackageDependency> =
            emptySet()
    ): ContentPackage =
        ContentPackage(
            id =
                PackageId(
                    id
                ),
            descriptor =
                PackageDescriptor(
                    name =
                        name,
                    version =
                        version,
                    format =
                        "OPD3",
                    dependencies =
                        dependencies
                )
        )
}