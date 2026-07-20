package vn.loi.learning.application.contentpackaging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import vn.loi.learning.domain.content.packaging.model.ContentPackage
import vn.loi.learning.domain.content.packaging.model.PackageCatalog
import vn.loi.learning.domain.content.packaging.model.PackageCatalogId
import vn.loi.learning.domain.content.packaging.model.PackageDependency
import vn.loi.learning.domain.content.packaging.model.PackageDescriptor
import vn.loi.learning.domain.content.packaging.model.PackageId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class PackageUpgradeReplacementDependencyIntegrationTest {

    @Test
    fun `upgrades package when replacement dependencies are installed`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val corePackage =
            contentPackage(
                id =
                    "core-english",
                name =
                    "Core English",
                version =
                    "2.0.0"
            )

        val currentPackage =
            contentPackage(
                id =
                    "vocabulary-extension-1",
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

        val replacementPackage =
            contentPackage(
                id =
                    "vocabulary-extension-2",
                name =
                    "Vocabulary Extension",
                version =
                    "2.0.0",
                dependencies =
                    setOf(
                        PackageDependency(
                            packageName =
                                "Core English",
                            minimumVersion =
                                "2.0.0"
                        )
                    )
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                corePackage
        )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentPackage
        )

        val updatedCatalog =
            repositories
                .useCase()
                .execute(
                    UpgradeContentPackageCommand(
                        catalogId =
                            catalogId,
                        currentPackageId =
                            currentPackage.id,
                        replacementPackage =
                            replacementPackage
                    )
                )

        assertEquals(
            corePackage,
            repositories.packageRepository.findById(
                corePackage.id
            )
        )

        assertNull(
            repositories.packageRepository.findById(
                currentPackage.id
            )
        )

        assertEquals(
            replacementPackage,
            repositories.packageRepository.findById(
                replacementPackage.id
            )
        )

        assertEquals(
            setOf(
                corePackage.id,
                replacementPackage.id
            ),
            updatedCatalog.packageIds
        )

        assertEquals(
            updatedCatalog,
            repositories.catalogRepository.findById(
                catalogId
            )
        )
    }

    @Test
    fun `rejects upgrade when replacement dependency version is unavailable`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val corePackage =
            contentPackage(
                id =
                    "core-english",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )

        val currentPackage =
            contentPackage(
                id =
                    "vocabulary-extension-1",
                name =
                    "Vocabulary Extension",
                version =
                    "1.0.0"
            )

        val missingDependency =
            PackageDependency(
                packageName =
                    "Core English",
                minimumVersion =
                    "2.0.0"
            )

        val replacementPackage =
            contentPackage(
                id =
                    "vocabulary-extension-2",
                name =
                    "Vocabulary Extension",
                version =
                    "2.0.0",
                dependencies =
                    setOf(
                        missingDependency
                    )
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                corePackage
        )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentPackage
        )

        val exception =
            assertFailsWith<MissingPackageDependenciesException> {
                repositories
                    .useCase()
                    .execute(
                        UpgradeContentPackageCommand(
                            catalogId =
                                catalogId,
                            currentPackageId =
                                currentPackage.id,
                            replacementPackage =
                                replacementPackage
                        )
                    )
            }

        assertEquals(
            replacementPackage.id,
            exception.packageId
        )

        assertEquals(
            setOf(
                missingDependency
            ),
            exception.missingDependencies
        )

        assertEquals(
            corePackage,
            repositories.packageRepository.findById(
                corePackage.id
            )
        )

        assertEquals(
            currentPackage,
            repositories.packageRepository.findById(
                currentPackage.id
            )
        )

        assertNull(
            repositories.packageRepository.findById(
                replacementPackage.id
            )
        )

        assertEquals(
            setOf(
                corePackage.id,
                currentPackage.id
            ),
            assertNotNull(
                repositories.catalogRepository.findById(
                    catalogId
                )
            ).packageIds
        )
    }

    @Test
    fun `rejects upgrade when installed dependent does not accept replacement version`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val currentPackage =
            contentPackage(
                id =
                    "core-english-1",
                name =
                    "Core English",
                version =
                    "1.5.0"
            )

        val replacementPackage =
            contentPackage(
                id =
                    "core-english-2",
                name =
                    "Core English",
                version =
                    "2.0.0"
            )

        val dependentPackage =
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
                                "1.9.0"
                        )
                    )
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentPackage
        )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                dependentPackage
        )

        val exception =
            assertFailsWith<PackageRequiredByInstalledPackagesException> {
                repositories
                    .useCase()
                    .execute(
                        UpgradeContentPackageCommand(
                            catalogId =
                                catalogId,
                            currentPackageId =
                                currentPackage.id,
                            replacementPackage =
                                replacementPackage
                        )
                    )
            }

        assertEquals(
            currentPackage.id,
            exception.packageId
        )

        assertEquals(
            setOf(
                dependentPackage.id
            ),
            exception.dependentPackageIds
        )

        assertEquals(
            currentPackage,
            repositories.packageRepository.findById(
                currentPackage.id
            )
        )

        assertEquals(
            dependentPackage,
            repositories.packageRepository.findById(
                dependentPackage.id
            )
        )

        assertNull(
            repositories.packageRepository.findById(
                replacementPackage.id
            )
        )

        assertEquals(
            setOf(
                currentPackage.id,
                dependentPackage.id
            ),
            assertNotNull(
                repositories.catalogRepository.findById(
                    catalogId
                )
            ).packageIds
        )
    }

    private class Repositories {

        val packageRepository =
            InMemoryContentPackageRepository()

        val catalogRepository =
            InMemoryPackageCatalogRepository()

        fun install(
            catalogId: PackageCatalogId,
            contentPackage: ContentPackage
        ) {
            packageRepository.save(
                contentPackage
            )

            val catalog =
                catalogRepository.findById(
                    catalogId
                ) ?: PackageCatalog(
                    id =
                        catalogId
                )

            catalogRepository.save(
                catalog.register(
                    contentPackage.id
                )
            )
        }

        fun useCase(): UpgradeContentPackageUseCase =
            UpgradeContentPackageUseCase(
                upgradeOperation =
                    PackageUpgradeOperation(
                        contentPackageRepository =
                            packageRepository,
                        packageCatalogRepository =
                            catalogRepository
                    ),
                transactionRunner =
                    InMemoryTransactionRunner()
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
}