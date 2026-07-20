package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentLibraryRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryLearningItemRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageUninstallDependencyIntegrationTest {

    @Test
    fun `uninstall rejects removing required installed package`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val corePackage =
            contentPackage(
                id =
                    "core-english-1",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )

        val extensionPackage =
            contentPackage(
                id =
                    "vocabulary-extension",
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

        repositories.contentPackageRepository.save(
            corePackage
        )

        repositories.contentPackageRepository.save(
            extensionPackage
        )

        repositories.packageCatalogRepository.save(
            PackageCatalog(
                id =
                    catalogId,
                packageIds =
                    setOf(
                        corePackage.id,
                        extensionPackage.id
                    )
            )
        )

        val exception =
            assertFailsWith<PackageRequiredByInstalledPackagesException> {
                repositories
                    .uninstallUseCase()
                    .execute(
                        UninstallContentPackageCommand(
                            catalogId =
                                catalogId,
                            packageId =
                                corePackage.id
                        )
                    )
            }

        assertEquals(
            setOf(
                extensionPackage.id
            ),
            exception.dependentPackageIds
        )

        assertEquals(
            corePackage,
            repositories.contentPackageRepository.findById(
                corePackage.id
            )
        )

        assertEquals(
            setOf(
                corePackage.id,
                extensionPackage.id
            ),
            assertNotNull(
                repositories.packageCatalogRepository.findById(
                    catalogId
                )
            ).packageIds
        )
    }

    @Test
    fun `uninstall succeeds when compatible dependency alternative remains`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val removedPackage =
            contentPackage(
                id =
                    "core-english-1",
                name =
                    "Core English",
                version =
                    "1.0.0"
            )

        val alternativePackage =
            contentPackage(
                id =
                    "core-english-2",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )

        val extensionPackage =
            contentPackage(
                id =
                    "vocabulary-extension",
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

        repositories.contentPackageRepository.save(
            removedPackage
        )

        repositories.contentPackageRepository.save(
            alternativePackage
        )

        repositories.contentPackageRepository.save(
            extensionPackage
        )

        repositories.packageCatalogRepository.save(
            PackageCatalog(
                id =
                    catalogId,
                packageIds =
                    setOf(
                        removedPackage.id,
                        alternativePackage.id,
                        extensionPackage.id
                    )
            )
        )

        repositories
            .uninstallUseCase()
            .execute(
                UninstallContentPackageCommand(
                    catalogId =
                        catalogId,
                    packageId =
                        removedPackage.id
                )
            )

        assertEquals(
            null,
            repositories.contentPackageRepository.findById(
                removedPackage.id
            )
        )

        assertEquals(
            alternativePackage,
            repositories.contentPackageRepository.findById(
                alternativePackage.id
            )
        )

        assertEquals(
            extensionPackage,
            repositories.contentPackageRepository.findById(
                extensionPackage.id
            )
        )

        assertEquals(
            setOf(
                alternativePackage.id,
                extensionPackage.id
            ),
            assertNotNull(
                repositories.packageCatalogRepository.findById(
                    catalogId
                )
            ).packageIds
        )
    }

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

    private class Repositories {

        val contentLibraryRepository =
            InMemoryContentLibraryRepository()

        val contentRepository =
            InMemoryContentRepository()

        val learningItemRepository =
            InMemoryLearningItemRepository()

        val contentPackageRepository =
            InMemoryContentPackageRepository()

        val packageCatalogRepository =
            InMemoryPackageCatalogRepository()

        fun uninstallUseCase(): UninstallContentPackageUseCase =
            UninstallContentPackageUseCase(
                uninstallOperation =
                    PackageUninstallOperation(
                        contentLibraryRepository =
                            contentLibraryRepository,
                        contentRepository =
                            contentRepository,
                        learningItemRepository =
                            learningItemRepository,
                        contentPackageRepository =
                            contentPackageRepository,
                        packageCatalogRepository =
                            packageCatalogRepository
                    ),
                transactionRunner =
                    InMemoryTransactionRunner()
            )
    }
}