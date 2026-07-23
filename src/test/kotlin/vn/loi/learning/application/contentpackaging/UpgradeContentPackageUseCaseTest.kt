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
import vn.loi.learning.domain.content.topic.model.TopicId
import vn.loi.learning.infrastructure.persistence.memory.InMemoryContentPackageRepository
import vn.loi.learning.infrastructure.persistence.memory.InMemoryPackageCatalogRepository
import vn.loi.learning.infrastructure.transaction.InMemoryTransactionRunner

class UpgradeContentPackageUseCaseTest {

    @Test
    fun `upgrades installed package and replaces catalog entry`() {
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
                    "1.0.0"
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
    fun `compatible package upgrade preserves installed topic identity`() {
        val repositories =
            Repositories()
        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )
        val currentPackage =
            contentPackage(
                id = "topic-release-1",
                name = "Original topic name",
                version = "1.0.0"
            ).copy(
                topicId =
                    TopicId(
                        "topic-durable"
                    )
            )
        val replacementPackage =
            contentPackage(
                id = "topic-release-2",
                name = "Original topic name",
                version = "2.0.0"
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentPackage
        )

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
            currentPackage.topicId,
            repositories
                .packageRepository
                .findById(
                    replacementPackage.id
                )
                ?.topicId
        )
    }

    @Test
    fun `upgrade rejects replacement with different package name`() {
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
                    "1.0.0"
            )

        val replacementPackage =
            contentPackage(
                id =
                    "different-package",
                name =
                    "Different Package",
                version =
                    "2.0.0"
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentPackage
        )

        assertFailsWith<InvalidPackageUpgradeException> {
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
    }

    @Test
    fun `upgrade rejects same or older version`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val currentPackage =
            contentPackage(
                id =
                    "core-english-2",
                name =
                    "Core English",
                version =
                    "2.0.0"
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentPackage
        )

        val sameVersionPackage =
            contentPackage(
                id =
                    "core-english-same",
                name =
                    "Core English",
                version =
                    "2.0.0"
            )

        val olderVersionPackage =
            contentPackage(
                id =
                    "core-english-old",
                name =
                    "Core English",
                version =
                    "1.9.0"
            )

        assertFailsWith<InvalidPackageUpgradeException> {
            repositories
                .useCase()
                .execute(
                    UpgradeContentPackageCommand(
                        catalogId =
                            catalogId,
                        currentPackageId =
                            currentPackage.id,
                        replacementPackage =
                            sameVersionPackage
                    )
                )
        }

        assertFailsWith<InvalidPackageUpgradeException> {
            repositories
                .useCase()
                .execute(
                    UpgradeContentPackageCommand(
                        catalogId =
                            catalogId,
                        currentPackageId =
                            currentPackage.id,
                        replacementPackage =
                            olderVersionPackage
                    )
                )
        }

        assertEquals(
            currentPackage,
            repositories.packageRepository.findById(
                currentPackage.id
            )
        )
    }

    @Test
    fun `upgrade rejects package that is not installed`() {
        val repositories =
            Repositories()

        val missingPackageId =
            PackageId(
                "missing-package"
            )

        assertFailsWith<PackageNotInstalledException> {
            repositories
                .useCase()
                .execute(
                    UpgradeContentPackageCommand(
                        catalogId =
                            PackageCatalogId(
                                "installed-packages"
                            ),
                        currentPackageId =
                            missingPackageId,
                        replacementPackage =
                            contentPackage(
                                id =
                                    "replacement-package",
                                name =
                                    "Core English",
                                version =
                                    "2.0.0"
                            )
                    )
                )
        }

        assertEquals(
            0,
            repositories.packageRepository.findAll().size
        )
    }

    @Test
    fun `upgrade rejects replacement with missing dependency before changing repositories`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val currentPackage =
            contentPackage(
                id =
                    "extension-1",
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
                    "extension-2",
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
            setOf(
                missingDependency
            ),
            exception.missingDependencies
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
    fun `upgrade rejects replacement that breaks dependent package without changing repositories`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val currentCorePackage =
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
                                "1.9.0"
                        )
                    )
            )

        val replacementCorePackage =
            contentPackage(
                id =
                    "core-english-2",
                name =
                    "Core English",
                version =
                    "2.0.0"
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentCorePackage
        )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                extensionPackage
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
                                currentCorePackage.id,
                            replacementPackage =
                                replacementCorePackage
                        )
                    )
            }

        assertEquals(
            currentCorePackage.id,
            exception.packageId
        )

        assertEquals(
            setOf(
                extensionPackage.id
            ),
            exception.dependentPackageIds
        )

        assertEquals(
            currentCorePackage,
            repositories.packageRepository.findById(
                currentCorePackage.id
            )
        )

        assertNull(
            repositories.packageRepository.findById(
                replacementCorePackage.id
            )
        )

        assertEquals(
            extensionPackage,
            repositories.packageRepository.findById(
                extensionPackage.id
            )
        )

        assertEquals(
            setOf(
                currentCorePackage.id,
                extensionPackage.id
            ),
            assertNotNull(
                repositories.catalogRepository.findById(
                    catalogId
                )
            ).packageIds
        )
    }

    @Test
    fun `upgrade keeps dependent packages valid with compatible newer version`() {
        val repositories =
            Repositories()

        val catalogId =
            PackageCatalogId(
                "installed-packages"
            )

        val currentCorePackage =
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
                                "3.0.0"
                        )
                    )
            )

        val replacementCorePackage =
            contentPackage(
                id =
                    "core-english-2",
                name =
                    "Core English",
                version =
                    "2.0.0"
            )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                currentCorePackage
        )

        repositories.install(
            catalogId =
                catalogId,
            contentPackage =
                extensionPackage
        )

        val updatedCatalog =
            repositories
                .useCase()
                .execute(
                    UpgradeContentPackageCommand(
                        catalogId =
                            catalogId,
                        currentPackageId =
                            currentCorePackage.id,
                        replacementPackage =
                            replacementCorePackage
                    )
                )

        assertEquals(
            setOf(
                replacementCorePackage.id,
                extensionPackage.id
            ),
            updatedCatalog.packageIds
        )

        assertEquals(
            extensionPackage,
            repositories.packageRepository.findById(
                extensionPackage.id
            )
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
}
